package domain

sealed trait GameKind {
  def numPlayers: Range
  def initBoard: Board
  def updateBoard(move: Move, board: Board): Board
  def isMoveValid(move: Move, board: Board): Boolean
}
case object GameKind {
  case object Chess extends GameKind {
    override val numPlayers: Range = Range.inclusive(2, 2)
    override val initBoard: Board = ChessBoard.init
    // todo
    override def updateBoard(move: Move, board: Board): Board = board
    override def isMoveValid(move: Move, board: Board): Boolean = true
  }
}

trait Board {
  def isFinished(): Option[GameFinishStatus]
}
case class ChessBoard() extends Board {
  // TODO
  override def isFinished(): Option[GameFinishStatus] = None
}
object ChessBoard {
  val init: ChessBoard = ChessBoard()
}
