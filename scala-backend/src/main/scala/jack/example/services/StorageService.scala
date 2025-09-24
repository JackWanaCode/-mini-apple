package jack.example.services

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.StatusCodes._
//import akka.http.scaladsl.unmarshalling.Unmarshal
//import akka.http.scaladsl.model.Uri
import scala.concurrent.{ExecutionContext, Future}
import jack.example.config.MediaCfg


/** Remote storage backed by HTTP. We only need HEAD checks for availability. */
class StorageService(media: MediaCfg)(implicit system: ActorSystem[_], ec: ExecutionContext) {
  private val http = Http()


  /** Public path clients should use (edges proxy this). */
  def publicIndexPath(videoId: String, rendition: String): String =
    s"${media.publicHlsBase}/$videoId/$rendition/index.m3u8"


  /** Full origin URL used by the backend to check availability. */
  def originIndexUrl(videoId: String, rendition: String): String =
    s"${media.originBaseUrl}${publicIndexPath(videoId, rendition)}"


  /** HEAD the remote index.m3u8 to see if a rendition exists. */
  def renditionExists(videoId: String, rendition: String): Future[Boolean] = {
    val req = HttpRequest(method = HttpMethods.HEAD, uri = originIndexUrl(videoId, rendition))
    http.singleRequest(req).map { resp =>
      resp.discardEntityBytes()
      resp.status match {
        case OK | NoContent | PartialContent => true
        case _ => false
      }
    }.recover { case _ => false }
  }


  def guessCodecs(r: String): String = r match {
    case "1080p" | "720p" | "480p" => "avc1.640028,mp4a.40.2"
    case _ => "avc1.4d401f,mp4a.40.2"
  }
}
