package jack.example

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import config.Settings
import services._
import http.Routes
import controllers._


import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import scala.util.{Failure, Success}


object Main extends App {
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "mini-netflix")
  implicit val ec: ExecutionContext = system.executionContext


  val cfg = Settings.load()


  // Services
  val users = new UserService
  val jwt = new JwtService(cfg.jwt)
  val storage = new StorageService(cfg.media) // REMOTE storage via HTTP HEAD checks
  val hls = new HlsService(storage)
  val signing = new SigningService(cfg.sign)
  val queue = new JobQueue


  // Controllers
  val authCtrl = new AuthController(users, jwt)
  val plCtrl = new PlaylistController(hls)
  val signCtrl = new SignController(signing)
  val jobsCtrl = new JobsController(queue)


  val routes = new Routes(authCtrl, plCtrl, signCtrl, jobsCtrl, cfg.corsAllowed).route


  val bindingFut = Http().newServerAt(cfg.host, cfg.port).bind(routes)
  bindingFut.onComplete {
    case Success(b) =>
      val addr = b.localAddress
      println(s"Server listening on http://${addr.getHostString}:${addr.getPort}")
    case Failure(ex) =>
      Console.err.println(s"Failed to bind: ${ex.getMessage}")
      system.terminate()
  }


  Await.result(system.whenTerminated, Duration.Inf)
}
