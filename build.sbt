name := "sonos-board-game"

ThisBuild / scalaVersion := "2.13.4"

val zioVersion = "1.0.4"
val calibanVersion = "0.9.4"
val circeVersion = "0.12.3"

val scalaChess = RootProject(uri("https://github.com/ornicar/scalachess.git"))

lazy val core = (project in file("core"))
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-test" % zioVersion % "test",
      "dev.zio" %% "zio-test-sbt" % zioVersion % "test",
      "dev.zio" %% "zio-test-magnolia" % zioVersion % "test",
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
  .dependsOn(scalaChess)

lazy val api = (project in file("api"))
  .settings(
    libraryDependencies ++= Seq(
      "com.github.ghostdogpr" %% "caliban" % calibanVersion,
      "com.github.ghostdogpr" %% "caliban-http4s" % calibanVersion,
      "ch.qos.logback" % "logback-classic" % "1.2.3"
    )
  )
  .dependsOn(core, scalaChess, persistence)

lazy val persistence = (project in file("persistence"))
  .settings(
    libraryDependencies ++= Seq(
      "org.reactivemongo" %% "reactivemongo" % "1.0.2"
    )
  )
  .dependsOn(core)
