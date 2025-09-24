package jack.example.services

import jack.example.models.Encoding
import scala.concurrent.{ExecutionContext, Future}


class HlsService(storage: StorageService)(implicit ec: ExecutionContext) {
  private val candidates = List(
    ("1080p", 5500000),
    ("720p", 3200000),
    ("480p", 1600000)
  )


  def discoverEncodingsF(videoId: String): Future[List[Encoding]] = {
    Future.traverse(candidates) { case (r, bw) =>
      storage.renditionExists(videoId, r).map { exists =>
        if (exists) Some(
          Encoding(videoId, r, bw, storage.guessCodecs(r), storage.publicIndexPath(videoId, r), ready = true)
        ) else None
      }
    }.map(_.flatten)
  }


  def masterM3U8F(videoId: String): Future[String] =
    discoverEncodingsF(videoId).map { encs =>
      val header = "#EXTM3U\n#EXT-X-VERSION:3\n"
      val body = encs.map { e =>
        s"#EXT-X-STREAM-INF:BANDWIDTH=${e.bandwidth},CODECS=\"${e.codecs}\"${e.path}"
      }.mkString
      header + body
    }
}
