package jack.example.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import jack.example.controllers._


class Routes(auth: AuthController, playlist: PlaylistController, sign: SignController, jobs: JobsController, allowed: List[String]) {
  val route: Route = Cors.route(
    inner = concat(
      path("healthz") { get { complete("ok") } },
      auth.routes,
      playlist.routes,
      sign.routes,
      jobs.routes
    ),
    allowedOrigins = allowed
  )
}