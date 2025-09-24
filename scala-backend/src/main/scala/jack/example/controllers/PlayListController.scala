package jack.example.controllers

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import jack.example.services.HlsService
import akka.http.scaladsl.model.ContentTypes
import scala.concurrent.ExecutionContext


class PlaylistController(hls: HlsService)(implicit ec: ExecutionContext) {
  val routes: Route = pathPrefix("api" / "v1" / "videos") {
    path(Segment / "master.m3u8") { videoId =>
      get {
        onSuccess(hls.masterM3U8F(videoId)) { m3u8 =>
          complete(akka.http.scaladsl.model.HttpEntity(ContentTypes.`text/plain(UTF-8)`, m3u8))
        }
      }
    }
  }
}
