import caliban.{CalibanError, GraphQLInterpreter, Http4sAdapter}
import cats.data.Kleisli
import cats.effect.Blocker
import org.http4s.StaticFile
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import zio.blocking.Blocking
import zio.console.putStrLn
import zio.interop.catz._
import zio._

object Main extends App {

  type Env = Resolvers.Env with ZEnv
  type F[A] = RIO[Env, A]

  val customLayer: ZLayer[Any, Throwable, Resolvers.Env] = Stores.inMemoryJournal ++ Stores.inMemoryStore

  // TODO: make it compatible with sbt run
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val dsl = Http4sDsl[Task]
    ZIO
      .runtime[Env]
      .flatMap { implicit runtime =>
        for {
          interpreter <- Api.api.interpreter
          blocker <- ZIO.access[Blocking](_.get.blockingExecutor.asEC).map(Blocker.liftExecutionContext)
          _ <- BlazeServerBuilder[F](runtime.platform.executor.asEC)
            .bindHttp(8080, "localhost")
            .withHttpApp(
              Router[F](
                "/" -> Kleisli.liftF(
                  StaticFile
                    .fromResource[F]("graphiql.html", blocker) //(taskEffectInstance, zioContextShift)
                ),
                "/api/graphql" -> CORS(Http4sAdapter.makeHttpService(withErrorReturn(interpreter))),
                "/ws/graphql" -> CORS(Http4sAdapter.makeWebSocketService(withErrorReturn(interpreter)))
              ).orNotFound
            )
            .resource
            .toManagedZIO
            .useForever
            .foldCauseM(err => putStrLn(err.prettyPrint).as(ExitCode.failure), _ => ZIO.succeed(ExitCode.success))
        } yield ()
      }
      .provideCustomLayer(customLayer)
      .exitCode
  }

  def withErrorReturn[R](
      interpreter: GraphQLInterpreter[R, CalibanError]
  ): GraphQLInterpreter[R, CalibanError] = {
    import caliban.ResponseValue.ObjectValue
    import caliban.CalibanError.{ExecutionError, ValidationError, ParsingError}
    import caliban.Value.StringValue

    interpreter.mapError {
      case err @ ExecutionError(_, _, _, Some(exampleError: Resolvers.ResolverError), _) =>
        err.copy(extensions = Some(ObjectValue(List(("details", StringValue(exampleError.message))))))
      case err: ExecutionError =>
        err.copy(extensions = Some(ObjectValue(List(("errorCode", StringValue("EXECUTION_ERROR"))))))
      case err: ValidationError =>
        err.copy(extensions = Some(ObjectValue(List(("errorCode", StringValue("VALIDATION_ERROR"))))))
      case err: ParsingError =>
        err.copy(extensions = Some(ObjectValue(List(("errorCode", StringValue("PARSING_ERROR"))))))
    }
  }
}
