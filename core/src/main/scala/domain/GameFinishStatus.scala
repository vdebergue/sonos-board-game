package domain

sealed trait GameFinishStatus
case object GameFinishStatus {
  case class Won(by: Player) extends GameFinishStatus
  case object Draw extends GameFinishStatus
}
