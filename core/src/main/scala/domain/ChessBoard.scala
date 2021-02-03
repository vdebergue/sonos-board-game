package domain

import cats.data.Validated
import io.circe.Decoder.Result

import scala.util.Try

case class ChessBoard(
    game: chess.Game,
    playersColors: Map[chess.Color, Player]
) extends Board {

  override def isFinished(): Option[GameFinishStatus] = {
    if (game.situation.`end`) {
      val status = game.situation.winner
        .map { color =>
          val winner = playersColors(color)
          Won(winner)
        }
        .getOrElse(Draw)
      Some(status)
    } else None
  }

  override def repr: String = ChessBoard.jsonEncoder.apply(this).noSpaces

  override def updateBoard(player: Player, move: Move): Board = {
    val chessMove = chess.format.Uci.Move.apply(move.value).getOrElse(sys.error("Invalid move"))
    val result = game.apply(chessMove)
    result match {
      case Validated.Valid((gameUpdated, _)) => this.copy(game = gameUpdated)
      case Validated.Invalid(e)              => sys.error(e)
    }
  }
  override def checkMove(player: Player, move: Move): Validated[String, Unit] = {
    val chessMove = chess.format.Uci.Move.apply(move.value)
    Validated
      .fromOption(chessMove, "Invalid move")
      .flatMap(chessMove => game.apply(chessMove))
      .map(_ => ())
  }
}

object ChessBoard {
  def init(players: Set[Player]): ChessBoard = {
    assert(players.sizeIs == 2)
    val game = chess.Game(chess.variant.Standard)
    val playersVector = players.toVector
    val colors: Map[chess.Color, Player] = Map(
      chess.Color.Black -> playersVector(0),
      chess.Color.White -> playersVector(1)
    )
    ChessBoard(game, colors)
  }

  import io.circe._, io.circe.generic.auto._, io.circe.syntax._
  implicit val colorEncoder = KeyEncoder[String].contramap[chess.Color](_.name)
  implicit val colorDecoder = KeyDecoder[String].map(str => chess.Color.fromName(str).get)
  implicit val mapEncoder = Encoder[Map[chess.Color, Player]]
  implicit val jsonEncoder = new Encoder[ChessBoard] {
    override def apply(c: ChessBoard): Json = Json.obj(
      "players" -> c.playersColors.asJson,
      "game" -> Json.fromString(c.game.board.visual)
    )
  }
  implicit val jsonDecoder = new Decoder[ChessBoard] {
    override def apply(c: HCursor): Result[ChessBoard] = for {
      players <- c.downField("players").as[Map[chess.Color, Player]]
      gameStr <- c.downField("game").as[String]
      game <- Try {
        val board = chess.format.Visual << gameStr
        chess.Game(board)
      }.toEither.left.map(err => DecodingFailure.fromThrowable(err, c.history))
    } yield ChessBoard(game, players)
  }

}
