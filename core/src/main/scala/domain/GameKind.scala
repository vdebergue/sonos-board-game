package domain

import cats.data.Validated

sealed trait GameKind {
  def numPlayers: Range
  def initBoard(players: Set[Player]): Board
}
case object GameKind {
  case object Chess extends GameKind {
    override val numPlayers: Range = Range.inclusive(2, 2)
    override def initBoard(players: Set[Player]): Board = ChessBoard.init(players)
  }
}

trait Board {
  def isFinished(): Option[GameFinishStatus]
  def repr: String
  def updateBoard(player: Player, move: Move): Board
  def checkMove(player: Player, move: Move): Validated[String, Unit]
}
