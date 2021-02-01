import caliban.{GraphQLInterpreter, Http4sAdapter}
import cats.effect.{Blocker, ContextShift}
import org.http4s.{HttpRoutes, StaticFile}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.CORS
import zio.blocking.Blocking
import zio.console.putStrLn
import zio.{App, ExitCode, Task, URIO, ZEnv, ZIO}
import zio.interop.catz._
import zio.interop.catz.implicits._

object Main extends App {

  // TODO: make it compatible with sbt run
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val dsl = Http4sDsl[Task]
    import dsl._
    ZIO
      .runtime[ZEnv]
      .flatMap { implicit runtime =>
        for {
          interpreter <- Api.interpreter
          blocker <- ZIO.access[Blocking](_.get.blockingExecutor.asEC).map(Blocker.liftExecutionContext)
          _ <- BlazeServerBuilder[Task](runtime.platform.executor.asEC)
            .bindHttp(8080, "localhost")
            .withHttpApp(
              Router(
                "/" -> HttpRoutes.of[Task] { case GET -> Root =>
                  StaticFile
                    .fromResource[Task]("graphiql.html", blocker)(taskEffectInstance, zioContextShift)
                    .getOrElseF(NotFound("no file found"))
                },
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
      .exitCode
  }
}
