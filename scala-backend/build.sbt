ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.15"

lazy val akkaVersion = "2.8.5"
lazy val akkaHttpVersion = "10.5.3"

lazy val root = (project in file(".")).settings(
  name := "akka-mini-netflix",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    // JSON
    "io.circe" %% "circe-core" % "0.14.7",
    "io.circe" %% "circe-generic" % "0.14.7",
    "de.heikoseeberger" %% "akka-http-circe" % "1.39.2",
    // JWT
    "com.github.jwt-scala" %% "jwt-circe" % "10.0.1",
    // Password hashing
    "org.mindrot" % "jbcrypt" % "0.4",
    // Config
    "com.typesafe" % "config" % "1.4.3"
  )
)