package domain

import java.util.UUID

import domain.GameEvent.{GameHosted, GameJoined}
import domain.TestHelpers.{GameStoreForTest, makeLayer}
import zio.test._

object GameCommandsTest extends DefaultRunnableSpec {
  val player1 = Player("j1")
  val player2 = Player("j2")
  val gameKind = GameKind.Chess
  val id = UUID.randomUUID()

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = suite("GameCommands")(
    testM("should be able to hostGame") {
      val command = GameCommands.HostGame(id, player1, gameKind)
      val result = GameCommands.handler(command, GameState.GameNotStarted)
      assertM(result)(Assertion.equalTo(Seq(GameHosted(id, gameKind, player1))))
    },
    testM("should be able to join game") {
      val command = GameCommands.JoinGame(id, player2)
      val state = GameState.GameAvailable(id, gameKind, player1, Set(player1))
      val result = GameCommands.handler(command, state)
      assertM(result)(Assertion.equalTo(Seq(GameJoined(id, player2))))
    },
    testM("should not be able to join a full game") {
      val command = GameCommands.JoinGame(id, Player("j3"))
      val state = GameState.GameAvailable(id, gameKind, player1, Set(player1, player2))
      val result = GameCommands.handler(command, state)
      assertM(result.flip)(Assertion.containsString("Game is already full"))
    }
  )
}
