package jack.example.services

import scala.collection.mutable

final case class EncodeJob(videoId: String, sourcePath: String, renditions: List[String])


class JobQueue {
  private val q = new mutable.Queue[EncodeJob]()
  def enqueue(job: EncodeJob): Unit = synchronized { q.enqueue(job) }
  def drain(max: Int): List[EncodeJob] = synchronized { q.dequeueAll(_ => q.nonEmpty).take(max).toList }
}
