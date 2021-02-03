package persistence

import java.util.UUID

import domain.{GameAvailable, GameEvent, GameState}
import eventsourcing.Entity.Id
import eventsourcing.Journal.JournalError
import eventsourcing.{Journal, StateStore}
import eventsourcing.StateStore.StoreError
import persistence.Mongo.Mongo
import reactivemongo.api.bson.MacroOptions.\/
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.{
  BSONDocument,
  BSONDocumentHandler,
  BSONDocumentReader,
  BSONHandler,
  BSONString,
  BSONWriter,
  MacroConfiguration,
  MacroOptions,
  Macros
}
import reactivemongo.api.{AsyncDriver, DB}
import zio._

object Mongo {

  type Mongo = Has[DB]

  val connection: Task[reactivemongo.api.DB] = ZIO.fromFuture { implicit ec =>
    val driver = new AsyncDriver()
    val connection = driver.connect(List("localhost"))
    connection.flatMap(_.database("boardgame"))
  }

  val layer: ZLayer[Any, Throwable, Mongo] = ZLayer.fromEffect(connection)
}

object MongoFormats {
  import domain._
  val documentDiscriminator = MacroConfiguration.defaultDiscriminator
  implicit val boardWriter = BSONWriter[Board](board => BSONString(board.repr))
  implicit val gameInProgressHandler: BSONDocumentHandler[GameInProgress] = {

    val writer = Macros
      .writer[GameInProgress]
      .afterWrite(_ ++ BSONDocument(documentDiscriminator -> GameInProgress.getClass.getCanonicalName))

    val reader = BSONDocumentReader.from { doc =>
      for {
        kind <- doc.getAsTry[GameKind]("kind")
        boardStr <- doc.getAsTry[String]("board")
        entityId <- doc.getAsTry[UUID]("entityId")
        host <- doc.getAsTry[Player]("host")
        players <- doc.getAsTry[Set[Player]]("players")
        board <- kind.readBoard(boardStr)
      } yield GameInProgress(entityId, kind, host, players, board)
    }
    BSONDocumentHandler.provided(reader, writer)
  }
  implicit val stateHandler: BSONDocumentHandler[GameState] = {
    type Opts = MacroOptions.UnionType[GameNotStarted.type \/ GameAvailable \/ GameInProgress \/ GameFinished]
      with MacroOptions.AutomaticMaterialization
    Macros.handlerOpts[GameState, Opts]
  }

  implicit val move: BSONDocumentHandler[Move] = Macros.handler[Move]
  implicit val player: BSONDocumentHandler[Player] = Macros.handler[Player]
  implicit val gameKind: BSONDocumentHandler[GameKind] = {
    type Opts = MacroOptions.AutomaticMaterialization
    Macros.handlerOpts[GameKind, Opts]
  }

  implicit val gameFinishStatus: BSONHandler[GameFinishStatus] = {
    type Opts = MacroOptions.UnionType[Draw.type \/ Won] with MacroOptions.AutomaticMaterialization
    Macros.handlerOpts[GameFinishStatus, Opts]
  }
  implicit val eventHandler: BSONDocumentHandler[GameEvent] = {
    type Opts = MacroOptions.UnionType[GameHosted \/ GameJoined \/ GameStarted \/ PlayerMoved \/ GameEnded]
      with MacroOptions.AutomaticMaterialization
    Macros.handlerOpts[GameEvent, Opts]
  }
}

object GameStateMongoStore {
  import MongoFormats._

  def query: ZLayer[Mongo, Nothing, GameStateQuery.GameStateQuery] = ZLayer.fromFunction { db: Mongo =>
    val collection = db.get[DB].collection[BSONCollection]("games")
    new GameStateQuery.Service {
      override def listAllGames(): IO[StoreError, Seq[GameState]] = ZIO
        .fromFuture { implicit ec =>
          collection.find(BSONDocument()).cursor[GameState]().collect[Vector]()
        }
        .mapError(_.getMessage)

      override def listAvailableGames(): IO[StoreError, Seq[GameAvailable]] = ZIO
        .fromFuture { implicit ec =>
          collection
            .find(BSONDocument(MongoFormats.documentDiscriminator -> GameAvailable.getClass.getCanonicalName))
            .cursor[GameState]()
            .collect[Vector]()
            .map(_.collect { case g: GameAvailable => g })
        }
        .mapError(_.getMessage)
    }
  }

  def store: ZLayer[Mongo, Nothing, Has[StateStore.Service[GameState]]] = ZLayer.fromFunction { db: Mongo =>
    val collection = db.get[DB].collection[BSONCollection]("games")

    new StateStore.Service[GameState] {
      def entityQuery(id: UUID) = BSONDocument("entityId" -> id)

      override def get(id: Id): IO[StoreError, Option[GameState]] = ZIO
        .fromFuture { implicit ec =>
          collection.find(entityQuery(id)).cursor[GameState]().headOption
        }
        .mapError(_.getMessage)

      override def save(state: GameState): IO[StoreError, Unit] = ZIO
        .fromFuture { implicit ec =>
          collection
            .update(true)
            .one(entityQuery(state.entityId), state, upsert = true)
            .map(_ => ())
        }
        .mapError(_.getMessage)
    }
  }
}

object GameEventMongoJournal {
  import MongoFormats._
  def journal: ZLayer[Mongo, Nothing, Journal.Journal[GameEvent]] = ZLayer.fromFunction { db: Mongo =>
    val collection = db.get[DB].collection[BSONCollection]("events")
    new Journal.Service[GameEvent] {
      override def saveEvents(events: Seq[GameEvent]): IO[JournalError, Unit] = ZIO
        .fromFuture { implicit ec =>
          collection.insert.many(events)
        }
        .map(_ => ())
        .mapError(_.getMessage)

      override def listEvents(entityId: Id): IO[JournalError, Seq[GameEvent]] = ZIO
        .fromFuture { implicit ec =>
          collection.find(BSONDocument()).cursor[GameEvent]().collect[Vector]()
        }
        .mapError(_.getMessage)
    }
  }
}
