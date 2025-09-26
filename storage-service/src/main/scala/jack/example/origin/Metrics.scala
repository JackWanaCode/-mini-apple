package jack.example.origin

import java.util.concurrent.atomic.AtomicLong


class Counter { private val c = new AtomicLong(0L); def inc(): Unit = c.incrementAndGet(); def get: Long = c.get }


class Metrics {
  val uploads = new Counter
  val downloads = new Counter


  def snapshotJson(): String = s"""{"uploads":${uploads.get},"downloads":${downloads.get}}"""
}
