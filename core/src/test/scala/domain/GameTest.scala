package domain

import java.util.UUID

import domain.GameEvent.GameHosted
import eventsourcing.Entity.Id
import eventsourcing.StateStore
import zio.{Has, IO, Layer, ULayer, ZLayer}
import zio.test._

object GameTest extends DefaultRunnableSpec {
  val player1 = Player("j1")
  val player2 = Player("j2")
  val gameKind = GameKind.Chess
  val id = UUID.randomUUID()

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = suite("GameTest")(
    testM("should be able to hostGame") {
      val app = for {
        events <- GameCommands.hostGame(id, player1, gameKind)
      } yield assert(events)(Assertion.equalTo(Seq(GameHosted(id, gameKind, player1))))
      val store = new GameStoreForTest()
      val layer: Layer[Nothing, StateStore.StateStore[GameState]] = ZLayer.succeed(store)
      app.provideLayer(layer)
    }
  )
}

class GameStoreForTest extends StateStore.Service[GameState] {
  private var data: Map[Id, GameState] = Map.empty
  override def get(id: Id): IO[StateStore.StoreError, Option[GameState]] = IO(data.get(id)).mapError(_.toString)
  override def save(state: GameState): IO[StateStore.StoreError, Unit] =
    IO { data = data.updated(state.entityId, state) }.mapError(_.toString)
}
