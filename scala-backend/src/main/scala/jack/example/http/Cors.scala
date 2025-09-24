package jack.example.http

import akka.http.scaladsl.model.HttpMethods.{GET, OPTIONS, POST}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.model.headers.{`Access-Control-Allow-Credentials`, `Access-Control-Allow-Headers`, `Access-Control-Allow-Methods`, `Access-Control-Allow-Origin`}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

object Cors {
  def route(inner: Route, allowedOrigins: List[String]): Route = {
    val allowOrigin = `Access-Control-Allow-Origin`.*
    val allowCreds = `Access-Control-Allow-Credentials`(true)
    val allowHeaders = `Access-Control-Allow-Headers`("Authorization", "Content-Type", "Accept")
    val allowMethods = `Access-Control-Allow-Methods`(GET, POST, OPTIONS)


    respondWithHeaders(allowOrigin, allowCreds, allowHeaders, allowMethods) {
      options { complete(HttpResponse(StatusCodes.OK)) } ~ inner
    }
  }
}
