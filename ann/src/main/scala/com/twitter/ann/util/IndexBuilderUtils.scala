package com.twitter.ann.util

import com.twitter.ann.common.{Appendable, EntityEmbedding}
import com.twitter.concurrent.AsyncStream
import com.twitter.logging.Logger
import com.twitter.util.Future
import java.util.concurrent.atomic.AtomicInteger

object IndexBuilderUtils {
  val Log = Logger.apply()

  def addToIndex[T](
    appendable: Appendable[T, _, _],
    embeddings: Seq[EntityEmbedding[T]],
    concurrencyLevel: Int
  ): Future[Int] = {
    val count = new AtomicInteger()
    // Async stream allows us to procss at most concurrencLevel futures at a time.
    // I am using the Future.unit.before to deal with an AsyncSteam GC issue:
    // https://groups.google.com/a/twitter.com/forum/#!search/AsyncStream$20GC$20issue/finaglers/0GS6YmEKeBs/oMnGBZBqCQAJ
    Future.Unit.before {
      val stream = AsyncStream.fromSeq(embeddings)
      val appendStream = stream.mapConcurrent(concurrencyLevel) { annEmbedding =>
        val processed = count.incrementAndGet()
        if (processed % 10000 == 0) {
          Log.info(s"Performed $processed updates")
        }
        appendable.append(annEmbedding)
      }
      appendStream.size
    }
  }
}
