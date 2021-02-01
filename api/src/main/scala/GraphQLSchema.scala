import caliban.schema.Schema
import domain.{Board, GameCommands, GameState}
import zio.RIO

object GraphQLSchema {
  implicit val boardSchema = Schema.stringSchema.contramap[Board](_.repr)

  case class Queries[E](
      availableGames: RIO[E, List[GameState.GameAvailable]],
      allGames: RIO[E, List[GameState]]
  )
  case class Mutations[E](
      hostGame: GameCommands.HostGame => RIO[E, GameState],
      joinGame: GameCommands.JoinGame => RIO[E, GameState],
      startGame: GameCommands.StartGame => RIO[E, GameState],
      sendMove: GameCommands.SendMove => RIO[E, GameState]
  )
}
