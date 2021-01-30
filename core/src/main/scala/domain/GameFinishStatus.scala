package domain

sealed trait GameFinishStatus
case object GameFinishStatus {
  case object Won extends GameFinishStatus
  case object Draw extends GameFinishStatus
}
