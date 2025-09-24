package jack.example.controllers

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import jack.example.services.{JobQueue, EncodeJob}

final case class JobReq(videoId: String, sourcePath: String, renditions: List[String])


class JobsController(queue: JobQueue) {
  val routes: Route = path("api" / "v1" / "jobs" / "encode") {
    post {
      entity(as[JobReq]) { r =>
        queue.enqueue(EncodeJob(r.videoId, r.sourcePath, r.renditions))
        complete(202 -> Map("enqueued" -> true))
      }
    }
  }
}
