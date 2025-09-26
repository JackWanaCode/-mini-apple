package jack.example.origin


import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}


class Api(storage: Storage, auth: TokenAuth, cacheControlValue: String) {


  // Build a Cache-Control header from configured value (Akka HTTP 10.5.x-friendly)
  private val cacheHdr: HttpHeader = RawHeader("Cache-Control", cacheControlValue)
  private val commonHeaders: List[HttpHeader] = List(cacheHdr)


  // Keep helper as requested: returns a Route, uses directive composition internally
  private def withOptionalAuth(inner: => Route): Route =
    if (!auth.isEnabled) inner
    else {
      (optionalHeaderValueByName("X-Edge-Token") & parameter("token".?)).tapply {
        case (hdrTok, qTok) =>
          val supplied = hdrTok.orElse(qTok)
          if (supplied.exists(auth.validate)) inner
          else complete(StatusCodes.Unauthorized -> "invalid or missing token")
      }
    }

  val routes: Route = handleRejections(RejectionHandler.default) {
    handleExceptions(ExceptionHandlers.all) {
      concat(
        path("healthz") {
          get { complete("ok") }
        },


        path("metrics") {
          get {
            val json = storage.metricsSnapshotJson()
            complete(HttpEntity(ContentTypes.`application/json`, json))
          }
        },


        pathPrefix("v1" / "videos" / Segment) { videoId =>
          concat(
            // POST /v1/videos/{videoId}/assets/{variant}/{filename}
            path("assets" / Segment / Segment) { (variant, filename) =>
              post {
                withOptionalAuth {
                  extractRequestEntity { entity =>
                    if (entity.isKnownEmpty()) complete(StatusCodes.BadRequest -> "empty body")
                    else onSuccess(storage.saveAsset(videoId, variant, filename, entity)) { meta =>
                      complete(StatusCodes.Created -> meta.toJson)
                    }
                  }
                }
              }
            },

            // HEAD/GET /v1/videos/{videoId}/assets/{variant}/{filename}
            path("assets" / Segment / Segment) { (variant, filename) =>
              concat(
                head {
                  withOptionalAuth {
                    onSuccess(storage.assetMeta(videoId, variant, filename)) {
                      case Some(meta) =>
                        val headers: Seq[HttpHeader] = commonHeaders :+ ETag(meta.etag)
                        respondWithHeaders(headers) {
                          complete(StatusCodes.OK)
                        }
                      case None => complete(StatusCodes.NotFound)
                    }
                  }
                },
                get {
                  withOptionalAuth {
                    optionalHeaderValueByType[Range](()) { rangeHeader =>
                      onSuccess(storage.streamAsset(videoId, variant, filename, rangeHeader)) {
                        case Right((entity, meta, status, extraHeaders)) =>
                          val headers: Seq[HttpHeader] = (commonHeaders :+ ETag(meta.etag)) ++ extraHeaders
                          respondWithHeaders(headers) {
                            complete(HttpResponse(status = status, entity = entity))
                          }
                        case Left(NotFound) => complete(StatusCodes.NotFound)
                      }
                    }
                  }
                }
              )
            }
          )
        }
      )
    }
  }
}

object ExceptionHandlers {
  val all = akka.http.scaladsl.server.ExceptionHandler {
    case ex: Throwable =>
      extractUri { uri =>
        complete(HttpResponse(StatusCodes.InternalServerError, entity = s"error: ${ex.getMessage}"))
      }
  }
}