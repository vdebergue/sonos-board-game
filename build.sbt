name := "sonos-board-game"

ThisBuild / scalaVersion := "2.13.4"

val zioVersion = "1.0.4"
val calibanVersion = "0.9.4"

val scalaChess = RootProject(uri("https://github.com/ornicar/scalachess.git"))

lazy val core = (project in file("core"))
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-test" % zioVersion % "test",
      "dev.zio" %% "zio-test-sbt" % zioVersion % "test",
      "dev.zio" %% "zio-test-magnolia" % zioVersion % "test"
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
  .dependsOn(core, scalaChess)
