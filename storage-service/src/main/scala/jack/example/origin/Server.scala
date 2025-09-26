package jack.example.origin

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Scheduler}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{`Cache-Control`, CacheDirectives, ETag, HttpChallenges}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import akka.stream.scaladsl.{FileIO, Source}
import akka.stream.{Materializer, SystemMaterializer}
import com.typesafe.config.ConfigFactory


import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import java.security.MessageDigest
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


object Server {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "mini-netflix-origin")
    implicit val ec: ExecutionContext = system.executionContext
    implicit val mat: Materializer = SystemMaterializer(system).materializer


    val cfg = ConfigFactory.load()
    val host = cfg.getString("origin.bind-host")
    val port = cfg.getInt("origin.bind-port")
    val storageRoot = Paths.get(cfg.getString("origin.storage-root")).toAbsolutePath.normalize()
    val cacheCtl = cfg.getString("origin.cache-control")
    val hmacSecretOpt = if (cfg.hasPath("origin.hmac-secret") && !cfg.getString("origin.hmac-secret").trim.isEmpty) Some(cfg.getString("origin.hmac-secret").getBytes("UTF-8")) else None


    Files.createDirectories(storageRoot)


    val storage = new Storage(storageRoot)
    val auth = new TokenAuth(hmacSecretOpt)


    val routes = new Api(storage, auth, cacheCtl).routes


    Http().newServerAt(host, port).bind(routes).onComplete {
      case Success(binding) =>
        system.log.info(s"Origin listening on http://${binding.localAddress.getHostString}:${binding.localAddress.getPort}")
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint", ex)
        system.terminate()
    }
  }
}
