import GraphQLSchema.{Mutations, Queries}
import domain.{GameEntity, GameEvent, GameState}
import eventsourcing.{CommandEngine, Journal, StateStore}
import persistence.GameStateQuery

object Resolvers {

  type Env = Journal.Journal[GameEvent] with StateStore.StateStore[GameState] with GameStateQuery.GameStateQuery

  val engine = new CommandEngine(GameEntity)
  // Using exception here because caliban handles natively errors of type Throwable
  case class ResolverError(message: String) extends Throwable
  def errorToThrowable(msg: String): Throwable = ResolverError(msg)
  val mutations: Mutations[Env] = Mutations(
    hostGame = (cmd) => engine.handleCommand(cmd).mapError(errorToThrowable),
    joinGame = (cmd) => engine.handleCommand(cmd).mapError(errorToThrowable),
    startGame = (cmd) => engine.handleCommand(cmd).mapError(errorToThrowable),
    sendMove = (cmd) => engine.handleCommand(cmd).mapError(errorToThrowable)
  )

  val queries = Queries[Env](
    availableGames = GameStateQuery.listAvailableGames().mapError(errorToThrowable),
    allGames = GameStateQuery.listAllGames().mapError(errorToThrowable)
  )
}
