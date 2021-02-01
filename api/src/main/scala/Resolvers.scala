import GraphQLSchema.Mutations
import domain.{GameEntity, GameEvent, GameState}
import eventsourcing.{CommandEngine, Journal, StateStore}

object Resolvers {

  type Env = Journal.Journal[GameEvent] with StateStore.StateStore[GameState]

  val engine = new CommandEngine(GameEntity)
  // Using exception here because caliban handles natively errors of type Throwable
  def errorToThrowable(msg: String): Throwable = new RuntimeException(msg)
  val mutations: Mutations[Env] = Mutations(
    hostGame = (cmd) => engine.handleCommand(cmd).mapError(errorToThrowable),
    joinGame = (cmd) => engine.handleCommand(cmd).mapError(errorToThrowable),
    startGame = (cmd) => engine.handleCommand(cmd).mapError(errorToThrowable),
    sendMove = (cmd) => engine.handleCommand(cmd).mapError(errorToThrowable)
  )
}