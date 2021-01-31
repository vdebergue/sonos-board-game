package domain

import java.util.UUID

import domain.GameEvent.{GameHosted, GameJoined, GameStarted, PlayerMoved}
import domain.GameState.{GameAvailable, GameInProgress}
import eventsourcing.StateStore.StateStore
import eventsourcing.{Entity, Event, State, StateStore}
import zio.{IO, UIO, ZIO}

object GameCommands {
  type CommandError = String
  type CommandResult = ZIO[StateStore[GameState], CommandError, Seq[GameEvent]]

  def hostGame(id: UUID, player: Player, kind: GameKind): CommandResult = {
    UIO(Seq(GameHosted(id, kind, player)))
  }

  def joinGame(id: UUID, player: Player): CommandResult = withGame[GameAvailable](id) { game =>
    if (game.players.contains(player)) {
      ZIO.fail("Player is already in the game")
    } else if (game.players.size >= game.kind.numPlayers.`end`) {
      ZIO.fail("Game is already full")
    } else {
      UIO(Seq(GameJoined(id, player)))
    }
  }

  def startGame(id: UUID, player: Player): CommandResult = withGame[GameAvailable](id) { game =>
    if (player != game.host) ZIO.fail(s"Only game host can start a game")
    else if (!game.kind.numPlayers.contains(game.players.size)) {
      ZIO.fail(s"Wrong number of players, this game requires ${game.kind.numPlayers}")
    } else {
      UIO(Seq(GameStarted(id)))
    }
  }

  def sendMove(id: UUID, player: Player, move: Move): CommandResult = withGame[GameInProgress](id) { game =>
    if (game.kind.isMoveValid(move, game.board)) {
      val newBoard = game.kind.updateBoard(move, game.board)
      val finishedEvent: Seq[GameEvent] = newBoard.isFinished().map(status => GameEvent.GameEnded(id, status)).toSeq
      UIO(Seq(PlayerMoved(id, player, move)) ++ finishedEvent)
    } else {
      ZIO.fail("Invalid move")
    }
  }

  private def withGame[S <: GameState: Manifest](
      id: UUID
  )(f: S => IO[CommandError, Seq[GameEvent]]): CommandResult = {
    StateStore.get[GameState](id).mapError(error => s"Store Error: $error").flatMap {
      case None       => ZIO.fail(s"Game not found")
      case Some(g: S) => f(g)
      case g          => ZIO.fail(s"Invalid game state: ${g}")
    }
  }
}

object GameEntity extends Entity[GameEvent, GameState] {
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
        case game: GameInProgress => game.copy(board = game.kind.updateBoard(move, game.board))
        case _                    => impossibleState
      }
    case GameEnded(id, status) =>
      state match {
        case game: GameInProgress => GameFinished(id, game.kind, game.host, game.players, status)
        case _                    => impossibleState
      }
  }

  override def zeroState: GameState = GameNotStarted
}

sealed trait GameEvent extends Event
case object GameEvent {
  case class GameHosted(entityId: UUID, kind: GameKind, host: Player) extends GameEvent
  case class GameJoined(entityId: UUID, player: Player) extends GameEvent
  case class GameStarted(entityId: UUID) extends GameEvent
  case class PlayerMoved(entityId: UUID, player: Player, move: Move) extends GameEvent
  case class GameEnded(entityId: UUID, status: GameFinishStatus) extends GameEvent
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
      status: GameFinishStatus
  ) extends GameState
}
