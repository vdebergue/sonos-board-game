package domain

import java.util.UUID

import domain.GameEvent.{GameHosted, GameJoined, GameStarted, PlayerMoved}
import eventsourcing.{Command, CommandEngine, Entity, Event, State}
import zio.{UIO, ZIO}

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
      status: GameFinishStatus
  ) extends GameState
}

sealed trait GameEvent extends Event
case object GameEvent {
  case class GameHosted(entityId: UUID, kind: GameKind, host: Player) extends GameEvent
  case class GameJoined(entityId: UUID, player: Player) extends GameEvent
  case class GameStarted(entityId: UUID) extends GameEvent
  case class PlayerMoved(entityId: UUID, player: Player, move: Move) extends GameEvent
  case class GameEnded(entityId: UUID, status: GameFinishStatus) extends GameEvent
}

sealed trait GameCommand extends Command {
  def entityId: Entity.Id
}
object GameCommands {

  type CommandError = String
  type CommandResult = ZIO[Any, CommandError, Seq[GameEvent]]

  case class HostGame(entityId: Entity.Id, player: Player, kind: GameKind) extends GameCommand
  case class JoinGame(entityId: Entity.Id, player: Player) extends GameCommand
  case class StartGame(entityId: Entity.Id, player: Player) extends GameCommand
  case class SendMove(entityId: Entity.Id, player: Player, move: Move) extends GameCommand

  private val commandNotHandled = ZIO.fail("command not supported with current state")
  import GameState._
  def handler(command: GameCommand, state: GameState): CommandResult = {
    state match {
      case GameNotStarted =>
        command match {
          case HostGame(id, player, kind) => UIO(Seq(GameHosted(id, kind, player)))
          case _                          => commandNotHandled
        }

      case game: GameAvailable =>
        command match {
          case JoinGame(_, player) =>
            if (game.players.contains(player)) {
              ZIO.fail("Player is already in the game")
            } else if (game.players.size >= game.kind.numPlayers.`end`) {
              ZIO.fail("Game is already full")
            } else {
              UIO(Seq(GameJoined(game.entityId, player)))
            }
          case StartGame(_, player) =>
            if (player != game.host) ZIO.fail(s"Only game host can start a game")
            else if (!game.kind.numPlayers.contains(game.players.size)) {
              ZIO.fail(s"Wrong number of players, this game requires ${game.kind.numPlayers}")
            } else {
              UIO(Seq(GameStarted(game.entityId)))
            }
          case _ => commandNotHandled
        }

      case game: GameInProgress =>
        command match {
          case SendMove(_, player, move) =>
            val board = game.board
            if (board.isMoveValid(player, move)) {
              val newBoard = board.updateBoard(player, move)
              val finishedEvent: Seq[GameEvent] =
                newBoard.isFinished().map(status => GameEvent.GameEnded(game.entityId, status)).toSeq
              UIO(Seq(PlayerMoved(game.entityId, player, move)) ++ finishedEvent)
            } else {
              ZIO.fail("Invalid move")
            }
          case _ => commandNotHandled

        }
      case _: GameFinished => commandNotHandled
    }
  }
}

object GameEntity extends Entity[GameEvent, GameState, GameCommand] {
  import GameEvent._
  import GameState._

  // todo make an event ?
  def impossibleState = throw new IllegalStateException("State is not possible")
  override def foldEvent(state: GameState, event: GameEvent): GameState = event match {
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
        case game: GameInProgress => game.copy(board = game.board.updateBoard(player, move))
        case _                    => impossibleState
      }
    case GameEnded(id, status) =>
      state match {
        case game: GameInProgress => GameFinished(id, game.kind, game.host, game.players, status)
        case _                    => impossibleState
      }
  }

  override def zeroState: GameState = GameNotStarted

  override def commandHandler(
      command: GameCommand,
      state: GameState
  ): ZIO[Any, CommandEngine.CommandError, Seq[GameEvent]] = GameCommands.handler(command, state)
}
