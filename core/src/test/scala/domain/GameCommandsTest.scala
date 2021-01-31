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
      val app = for {
        events <- GameCommands.hostGame(id, player1, gameKind)
      } yield assert(events)(Assertion.equalTo(Seq(GameHosted(id, gameKind, player1))))
      val store = new GameStoreForTest()

      app.provideLayer(makeLayer(store))
    },
    testM("should be able to join game") {
      val store = new GameStoreForTest()
      store.data = Map(id -> GameState.GameAvailable(id, gameKind, player1, Set(player1)))
      val app = for {
        events <- GameCommands.joinGame(id, player2)
      } yield assert(events)(Assertion.equalTo(Seq(GameJoined(id, player2))))
      app.provideLayer(makeLayer(store))
    },
    testM("should not be able to join a full game") {
      val store = new GameStoreForTest()
      store.data = Map(id -> GameState.GameAvailable(id, gameKind, player1, Set(player1, player2)))

      val app = for {
        error <- GameCommands.joinGame(id, Player("j3")).flip
      } yield assert(error)(Assertion.containsString("Game is already full"))

      app.provideLayer(makeLayer(store))
    }
  )
}
