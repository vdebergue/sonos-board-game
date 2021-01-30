name := "sonos-board-game"

ThisBuild / scalaVersion := "2.13.4"

lazy val core = (project in file("core"))
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "1.0.4"
    )
  )
