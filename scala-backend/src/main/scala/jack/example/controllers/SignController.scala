package jack.example.controllers

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import jack.example.services.SigningService


final case class SignReq(path: String)
final case class SignRes(sig: String, exp: Long)


class SignController(sign: SigningService) {
  val routes: Route = path("api" / "v1" / "sign") {
    post {
      entity(as[SignReq]) { r =>
        val now = java.time.Instant.now().getEpochSecond
        val (sig, exp) = sign.signPath(r.path, now)
        complete(SignRes(sig, exp))
      }
    }
  }
}
