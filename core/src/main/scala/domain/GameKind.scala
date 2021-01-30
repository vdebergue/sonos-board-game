package domain

sealed trait GameKind {
  def numPlayers: Range
  def initBoard: Board
  def updateBoard(move: Move, board: Board): Board
}
case object GameKind {
  case object Chess extends GameKind {
    override val numPlayers: Range = Range.inclusive(2, 2)
    override val initBoard: Board = ChessBoard.init
    // todo
    override def updateBoard(move: Move, board: Board): Board = board
  }
}

trait Board
case class ChessBoard() extends Board
object ChessBoard {
  val init: ChessBoard = ChessBoard()
}
