name := """speelsysteem-admin-panel"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

dockerRepository := Some("thomastoye")

dockerUpdateLatest := true

libraryDependencies ++= Seq(
  ws,
  guice,
  "com.ibm" %% "couchdb-scala" % "0.7.2",
  "io.lemonlabs" %% "scala-uri" % "0.4.16",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.1",

  "com.typesafe.play" %% "play-json" % "2.6.1",

  "org.webjars" %% "webjars-play" % "2.6.1",
  "org.webjars" % "bootstrap" % "3.3.6",
  "org.webjars" % "jquery" % "1.11.3",

  "com.adrianhurt" %% "play-bootstrap" % "1.2-P26-B3-RC2",

  "org.mockito" % "mockito-core" % "1.10.19",

  "org.scalatestplus.play" %% "scalatestplus-play" % "3.0.0" % Test,
  "de.leanovate.play-mockws" %% "play-mockws" % "2.6.0" % Test
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
