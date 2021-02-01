import java.util.UUID

import domain.{GameEvent, GameState}
import eventsourcing.Entity.Id
import eventsourcing.Journal
import eventsourcing.Journal.JournalError
import eventsourcing.StateStore
import eventsourcing.StateStore.StoreError
import zio.{Has, IO, ULayer, ZLayer}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object Stores {

  val inMemoryJournal: ULayer[Has[Journal.Service[GameEvent]]] = ZLayer.succeed {
    new Journal.Service[GameEvent] {
      private val data =
        mutable.HashMap.empty[UUID, mutable.ArrayBuffer[GameEvent]].withDefault(_ => new ArrayBuffer[GameEvent]())
      override def saveEvents(events: Seq[GameEvent]): IO[JournalError, Unit] = IO.succeed {
        val buffer = data(events.head.entityId)
        buffer.appendAll(events)
        ()
      }

      override def listEvents(entityId: Id): IO[JournalError, Seq[GameEvent]] =
        IO.fromOption(data.get(entityId)).map(_.toSeq).mapError(_ => "Not found")
    }
  }

  val inMemoryStore: ULayer[Has[StateStore.Service[GameState]]] = ZLayer.succeed {
    new StateStore.Service[GameState] {
      private val data = mutable.HashMap.empty[UUID, GameState]
      override def get(id: Id): IO[StoreError, Option[GameState]] = IO.succeed(data.get(id))

      override def save(state: GameState): IO[StoreError, Unit] = IO.succeed(data.update(state.entityId, state))
    }

  }

}
