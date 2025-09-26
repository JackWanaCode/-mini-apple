package jack.example.origin

import akka.http.javadsl.model.headers.ContentDisposition
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{ByteRange, ContentDispositionTypes, Range, RangeUnits, `Content-Range`}
import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString

import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import java.security.MessageDigest
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try


sealed trait StorageError
case object NotFound extends StorageError

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.HttpCharsets
import akka.http.scaladsl.model.MediaType

import akka.stream.Materializer

// Custom content types (Akka 10.5.x doesn't predefine these)
object CustomContentTypes {
  // HLS transport stream segments
  val `video/MP2T`: ContentType.Binary =
    ContentType(MediaType.customBinary("video", "MP2T", MediaType.NotCompressible))

  // HLS playlist (UTF-8)
  val `application/vnd.apple.mpegurl`: ContentType.WithFixedCharset =
    ContentType(
      MediaType.customWithFixedCharset(
        "application",
        "vnd.apple.mpegurl",
        HttpCharsets.`UTF-8`
      )
    )

  // MPEG-DASH MPD manifest (UTF-8)
  val `application/dash+xml`: ContentType.WithFixedCharset =
    ContentType(
      MediaType.customWithFixedCharset(
        "application",
        "dash+xml",
        HttpCharsets.`UTF-8`
      )
    )

  // CMAF/fMP4 segment (often served as octet-stream)
  val `video/iso.segment`: ContentType.Binary =
    ContentType(MediaType.customBinary("video", "iso.segment", MediaType.NotCompressible))

  // MP4 container (binary, no charset)
  val `video/mp4`: ContentType.Binary =
    ContentType(MediaType.customBinary("video", "mp4", MediaType.NotCompressible))
}

final case class AssetMeta(
                            path: Path,
                            size: Long,
                            etag: String,
                            contentType: ContentType
                          ) {
  def toJson: String = s"""{"path":"${path.toString}","size":$size,"etag":"$etag","contentType":"${contentType.value}"}"""
}

class Storage(root: Path)(implicit mat: Materializer, ec: ExecutionContext) {


  private val meter = new Metrics


  import CustomContentTypes._

  private val videoContentTypes: Map[String, ContentType] = Map(
    ".m3u8" -> `application/vnd.apple.mpegurl`,
    ".mpd"  -> `application/dash+xml`,
    ".ts"   -> `video/MP2T`,
    ".m4s"  -> `video/iso.segment`,         // or ContentTypes.`application/octet-stream`
    ".mp4"  -> `video/mp4`     // this one *is* predefined
  )


  private def contentTypeFor(filename: String): ContentType =
    videoContentTypes.collectFirst { case (ext, ct) if filename.endsWith(ext) => ct }
      .getOrElse(ContentTypes.`application/octet-stream`)


  private def resolve(videoId: String, variant: String, filename: String): Path =
    root.resolve(Paths.get(videoId, variant, filename)).normalize()


  private def ensureParent(p: Path): Unit = Files.createDirectories(p.getParent)


  private def etagFor(p: Path): String = {
    val md = MessageDigest.getInstance("MD5")
    val ch = Files.newInputStream(p)
    val buf = new Array[Byte](8192)
    var read = 0
    try {
      while ({ read = ch.read(buf); read != -1 }) md.update(buf, 0, read)
    } finally ch.close()
    md.digest().map("%02x".format(_)).mkString
  }

  def saveAsset(videoId: String, variant: String, filename: String, entity: RequestEntity): Future[AssetMeta] = {
    val path = resolve(videoId, variant, filename)
    ensureParent(path)
    val ct = contentTypeFor(filename)


    meter.uploads.inc()


    entity.withoutSizeLimit().dataBytes.runWith(FileIO.toPath(path, options = Set(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE))).flatMap { ioRes =>
      if (ioRes.wasSuccessful) Future.successful(AssetMeta(path, Files.size(path), etagFor(path), ct))
      else Future.failed(ioRes.getError)
    }
  }


  def assetMeta(videoId: String, variant: String, filename: String): Future[Option[AssetMeta]] = Future {
    val p = resolve(videoId, variant, filename)
    if (Files.exists(p)) Some(AssetMeta(p, Files.size(p), etagFor(p), contentTypeFor(filename)))
    else None
  }

  /**
   * Streams an asset with support for HTTP Range requests.
   */
  def streamAsset(
                   videoId: String,
                   variant: String,
                   filename: String,
                   rangeHeader: Option[Range]
                 ): Future[Either[StorageError, (ResponseEntity, AssetMeta, StatusCode, List[HttpHeader])]] = {
    val p = resolve(videoId, variant, filename)
    if (!Files.exists(p)) return Future.successful(Left(NotFound))

    val size = Files.size(p)
    val meta = AssetMeta(p, size, etagFor(p), contentTypeFor(filename))

    meter.downloads.inc()

    rangeHeader match {
      // Match only on byte ranges
      case Some(Range(RangeUnits.Bytes, Seq(ByteRange.Slice(start, endOpt)))) =>
        val startIdx = start
        val endIdx   = endOpt
        val clamped  = Math.min(endIdx, size - 1)
        val count    = clamped - startIdx + 1

        val src    = FileIO.fromPath(p, chunkSize = 1024 * 1024).drop(startIdx).take(count)
        val entity = HttpEntity(meta.contentType, count, src)
        val hdrs   = List(`Content-Range`(ContentRange(startIdx, clamped, size)))

        Future.successful(Right((entity, meta, StatusCodes.PartialContent, hdrs)))

      // No/unsupported range: return whole file
      case _ =>
        val src    = FileIO.fromPath(p, chunkSize = 1024 * 1024)
        val entity = HttpEntity(meta.contentType, size, src)
        Future.successful(Right((entity, meta, StatusCodes.OK, Nil)))
    }
  }

  // Basic in-process metrics (replace with Prometheus if desired)
  def metricsSnapshotJson(): String = meter.snapshotJson()
}


