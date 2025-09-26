ThisBuild / version := "0.1.0-SNAPSHOT"

name := "mini-netflix-origin"


scalaVersion := "2.13.14"


libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % "2.8.5",
  "com.typesafe.akka" %% "akka-stream" % "2.8.5",
  "com.typesafe.akka" %% "akka-http" % "10.5.3",
  "ch.qos.logback" % "logback-classic" % "1.5.6",
  "com.typesafe" % "config" % "1.4.3",
  "org.mindrot" % "jbcrypt" % "0.4" // optional (if you want hashed API keys)
)


Compile / run / fork := true
