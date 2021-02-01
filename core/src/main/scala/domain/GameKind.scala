package domain

sealed trait GameKind {
  def numPlayers: Range
  def initBoard: Board

}
case object GameKind {
  case object Chess extends GameKind {
    override val numPlayers: Range = Range.inclusive(2, 2)
    override val initBoard: Board = ChessBoard.init
  }
}

trait Board {
  def isFinished(): Option[GameFinishStatus]
  def repr: String
  def updateBoard(player: Player, move: Move): Board
  def isMoveValid(player: Player, move: Move): Boolean
}
case class ChessBoard() extends Board {
  // TODO game logic
  override def isFinished(): Option[GameFinishStatus] = None
  override def repr: String = "serialize board"
  override def updateBoard(player: Player, move: Move): Board = this
  override def isMoveValid(player: Player, move: Move): Boolean = true
}
object ChessBoard {
  val init: ChessBoard = ChessBoard()
}
