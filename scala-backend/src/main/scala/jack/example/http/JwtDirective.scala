package jack.example.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{AuthorizationFailedRejection, Directive1}
import jack.example.services.JwtService


object JwtDirective {
  def authenticate(jwt: JwtService): Directive1[String] = {
    optionalHeaderValueByName("Authorization").flatMap {
      case Some(h) if h.startsWith("Bearer ") =>
        val token = h.stripPrefix("Bearer ")
        jwt.verify(token) match {
          case Some(claim) => provide(claim.subject.getOrElse(""))
          case None => reject(AuthorizationFailedRejection)
        }
      case _ => reject(AuthorizationFailedRejection)
    }
  }
}
