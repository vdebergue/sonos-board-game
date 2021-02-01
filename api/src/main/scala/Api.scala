import caliban.GraphQL.graphQL
import caliban.RootResolver
import domain.GameState

object Api {

  def getAvailableGames(): List[GameState.GameAvailable] = Nil

  case class Queries(availableGames: List[GameState.GameAvailable])

  val queries = Queries(getAvailableGames())
  val api = graphQL(RootResolver(queries))

  val interpreter = api.interpreter
}
