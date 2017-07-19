name := """admin-panel"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

dockerRepository := Some("thomastoye")

dockerUpdateLatest := true

libraryDependencies ++= Seq(
  cache,
  ws,
  "com.ibm" %% "couchdb-scala" % "0.7.2",
  "io.lemonlabs" %% "scala-uri" % "0.4.16",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.1",

  "org.webjars" %% "webjars-play" % "2.5.0",
  "org.webjars" % "bootstrap" % "3.3.6",
  "com.adrianhurt" %% "play-bootstrap" % "1.0-P25-B3",

  "org.mockito" % "mockito-core" % "1.10.19",

  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "de.leanovate.play-mockws" %% "play-mockws" % "2.5.1" % Test
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
