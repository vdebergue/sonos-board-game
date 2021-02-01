import caliban.Http4sAdapter
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
                "/api/graphql" -> CORS(Http4sAdapter.makeHttpService(interpreter)),
                "/ws/graphql" -> CORS(Http4sAdapter.makeWebSocketService(interpreter))
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
}
