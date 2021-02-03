package domain

import cats.data.Validated

import scala.util.Try

sealed trait GameKind {
  def numPlayers: Range
  def initBoard(players: Set[Player]): Board
  def readBoard(boardStr: String): Try[Board]
}
case object Chess extends GameKind {
  override val numPlayers: Range = Range.inclusive(2, 2)
  override def initBoard(players: Set[Player]): Board = ChessBoard.init(players)
  override def readBoard(boardStr: String): Try[Board] = {
    import io.circe.parser._
    val result = for {
      json <- parse(boardStr)
      obj <- ChessBoard.jsonDecoder.decodeJson(json)
    } yield obj
    result.toTry
  }
}

trait Board {
  def isFinished(): Option[GameFinishStatus]
  def repr: String
  def updateBoard(player: Player, move: Move): Board
  def checkMove(player: Player, move: Move): Validated[String, Unit]
}
