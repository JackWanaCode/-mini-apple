package jack.example.controllers

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import jack.example.services.{UserService, JwtService}


final case class AuthReq(email: String, password: String)
final case class TokenRes(token: String)
final case class UserRes(id: Long, email: String)

class AuthController(users: UserService, jwt: JwtService) {
  val routes: Route = pathPrefix("api" / "auth") {
    concat(
      path("register") {
        post {
          entity(as[AuthReq]) { r =>
            users.register(r.email, r.password) match {
              case Left(err) => complete(409 -> Map("error" -> err))
              case Right(u) => complete(UserRes(u.id, u.email))
            }
          }
        }
      },
      path("login") {
        post {
          entity(as[AuthReq]) { r =>
            users.login(r.email, r.password) match {
              case Left(err) => complete(401 -> Map("error" -> err))
              case Right(u) => complete(TokenRes(jwt.issue(u.id, u.email)))
            }
          }
        }
      }
    )
  }
}
