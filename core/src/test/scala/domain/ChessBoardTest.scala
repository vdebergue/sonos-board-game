package domain

import cats.data.Validated
import zio.test._

object ChessBoardTest extends DefaultRunnableSpec {
  val p1 = Player("j1")
  val p2 = Player("j2")
  val players = Set(p1, p2)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = suite("ChessBoard")(
    test("should init a game") {
      val board = ChessBoard.init(players)
      assert(board)(
        Assertion.hasField("colors", (g: ChessBoard) => g.playersColors, Assertion.hasSize(Assertion.equalTo(2))) &&
          Assertion.hasField(
            "game",
            (g: ChessBoard) => g.game.turns,
            Assertion.equalTo(0)
          )
      )
    },
    test("should validate moves") {
      val board = ChessBoard.init(players)
      val m1 = board.checkMove(p1, Move("foo"))
      assert(m1)(Assertion.equalTo(Validated.Invalid("Invalid move")))

      val m2 = board.checkMove(p1, Move("e2e3"))
      assert(m2)(Assertion.equalTo(Validated.Valid(())))

      val m3 = board.checkMove(p1, Move("e2e7"))
      assert(m3)(Assertion.equalTo(Validated.Invalid("Piece on e2 cannot move to e7")))
    },
    test("should tell when a game is over") {
      val board = ChessBoard.init(players)
      assert(board.isFinished())(Assertion.equalTo(None))

      val matedBoard =
        chess.Game(None, Some(chess.format.FEN("2bqk2r/r2p1Q1p/p2P4/1p2p3/1P3p2/1BN2P2/PP2KP1P/R6R b k - 0 18")))
      val board2 = ChessBoard(matedBoard, Map(p1 -> chess.Color.Black, p2 -> chess.Color.White))
      assert(board2.isFinished())(Assertion.equalTo(Some(GameFinishStatus.Won(p2))))
    }
  )
}
