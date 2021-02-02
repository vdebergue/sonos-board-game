package domain

import cats.data.Validated

case class ChessBoard(
    game: chess.Game,
    playersColors: Map[Player, chess.Color]
) extends Board {

  override def isFinished(): Option[GameFinishStatus] = {
    if (game.situation.`end`) {
      val status = game.situation.winner
        .map { color =>
          val winner = playersColors.toVector.find(_._2 == color).get._1
          GameFinishStatus.Won(winner)
        }
        .getOrElse(GameFinishStatus.Draw)
      Some(status)
    } else None
  }
  override def repr: String = s"${game.board.visual}"
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
    val colors = Map(
      playersVector(0) -> chess.Color.Black,
      playersVector(1) -> chess.Color.White
    )
    ChessBoard(game, colors)
  }
}
