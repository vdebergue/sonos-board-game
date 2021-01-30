package domain

import java.util.UUID

import eventsourcing.{Entity, Event, State}

class GameEntity extends Entity[GameEvents, GameState] {
  import GameEvents._
  import GameState._

  // todo make an event ?
  def impossibleState = throw new IllegalStateException("")
  override def foldEvent(state: GameState, event: GameEvents): GameState = event match {
    case GameHosted(id, kind, host) => GameAvailable(id, kind, host, players = Set(host))
    case GameJoined(id, player) =>
      state match {
        case game: GameAvailable => game.copy(players = game.players + player)
        case _                   => impossibleState
      }
    case GameStarted(id) =>
      state match {
        case GameAvailable(id, kind, host, players) => GameInProgress(id, kind, host, players, kind.initBoard)
        case _                                      => impossibleState
      }
    case PlayerMoved(id, player, move) =>
      state match {
        case game: GameInProgress => game.copy(board = game.kind.updateBoard(move, game.board))
        case _                    => impossibleState
      }
    case GameEnded(id, status, winner) =>
      state match {
        case game: GameInProgress => GameFinished(id, game.kind, game.host, game.players, status, winner)
        case _                    => impossibleState
      }
  }

  override def zeroState: GameState = GameNotStarted
}

sealed trait GameEvents extends Event
case object GameEvents {
  case class GameHosted(entityId: UUID, kind: GameKind, host: Player) extends GameEvents
  case class GameJoined(entityId: UUID, player: Player) extends GameEvents
  case class GameStarted(entityId: UUID) extends GameEvents
  case class PlayerMoved(entityId: UUID, player: Player, move: Move) extends GameEvents
  case class GameEnded(entityId: UUID, status: GameFinishStatus, winner: Option[Player]) extends GameEvents
}

sealed trait GameState extends State
case object GameState {
  case object GameNotStarted extends GameState {
    // this is the zero state and is not stored in db, so entityId is not important here
    val entityId = UUID.fromString("00000000-0000-0000-0000-000000000000")
  }

  case class GameAvailable(
      entityId: UUID,
      kind: GameKind,
      host: Player,
      players: Set[Player]
  ) extends GameState

  case class GameInProgress(
      entityId: UUID,
      kind: GameKind,
      host: Player,
      players: Set[Player],
      board: Board
  ) extends GameState

  case class GameFinished(
      entityId: UUID,
      kind: GameKind,
      host: Player,
      players: Set[Player],
      status: GameFinishStatus,
      winner: Option[Player]
  ) extends GameState
}
