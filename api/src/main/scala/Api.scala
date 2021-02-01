import caliban.GraphQL.graphQL
import caliban.schema.Schema
import caliban.{GraphQL, RootResolver}
import zio.RIO

object Api {
  import GraphQLSchema._

  val queries = Queries[Resolvers.Env](availableGames = RIO.succeed(Nil), allGames = RIO.succeed(Nil))

  // hack around derivation that does not work with ZIO of type Resolvers.Env
  implicit val querySchema =
    (Schema.gen[Queries[Any]]).asInstanceOf[Schema[Resolvers.Env, Queries[Resolvers.Env]]]
  implicit val mutationSchema =
    (Schema.gen[Mutations[Any]]).asInstanceOf[Schema[Resolvers.Env, Mutations[Resolvers.Env]]]

  val api: GraphQL[Resolvers.Env] = graphQL(RootResolver(queries, Resolvers.mutations))
}
