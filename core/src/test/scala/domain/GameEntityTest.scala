package domain

import java.util.UUID

import domain.GameEvent._
import domain.GameState.GameFinished
import domain.TestHelpers.{GameStoreForTest, makeLayer}
import eventsourcing.StateEngine
import zio.test._

object GameEntityTest extends DefaultRunnableSpec {
  val id = UUID.randomUUID()
  val player1 = Player("j1")
  val player2 = Player("j2")
  val gameKind = GameKind.Chess
  val gameEngine = new StateEngine(GameEntity)
  val store = new GameStoreForTest()

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = suite("GameEntity")(
    testM("should consume events") {
      val events = Seq(
        GameHosted(id, gameKind, player1),
        GameJoined(id, player2),
        GameStarted(id),
        PlayerMoved(id, player1, Move("e2e3")),
        PlayerMoved(id, player2, Move("a7a5")),
        GameEnded(id, GameFinishStatus.Won(by = player1))
      )
      val finalState = gameEngine.processEvents(events)
      val store = new GameStoreForTest()

      assertM(finalState)(
        Assertion.equalTo(
          GameFinished(id, gameKind, player1, Set(player1, player2), GameFinishStatus.Won(by = player1))
        )
      ).provideLayer(makeLayer(store))
    },
    testM("should fail when invalid events are provided") {
      val events = Seq(
        GameHosted(id, gameKind, player1),
        PlayerMoved(id, player1, Move("e2e3"))
      )
      val error = gameEngine.processEvents(events).flip
      val store = new GameStoreForTest()
      assertM(error)(Assertion.containsString("State is not possible"))
        .provideLayer(makeLayer(store))
    }
  )

}
