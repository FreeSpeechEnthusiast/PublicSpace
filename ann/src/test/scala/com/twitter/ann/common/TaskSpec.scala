package com.twitter.ann.common

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.util.Rng
import com.twitter.util.Await
import com.twitter.util.Awaitable
import com.twitter.util.Duration
import com.twitter.util.Future
import com.twitter.util.MockTimer
import com.twitter.util.Promise
import com.twitter.util.Time
import com.twitter.util.Timer
import com.twitter.util.logging.Logging
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite

class TaskSpec extends AnyFunSuite with Eventually {

  def await[A](a: Awaitable[A]): A = Await.result(a, 5.seconds)
  def mockTaskInterval: Duration = 1.hour

  abstract class MockTask(
    override val timer: Timer,
    override val statsReceiver: StatsReceiver = NullStatsReceiver,
    override val rng: Rng = Rng.threadLocal)
      extends Task
      with Logging {
    protected def taskInterval: Duration = mockTaskInterval
  }

  test("immediate start runs on set interval") {
    @volatile var counter: Int = 0
    Time.withCurrentTimeFrozen { ctl =>
      val timer = new MockTimer
      val task = new MockTask(timer) {
        def task(): Future[Unit] = {
          counter = counter + 1
          Future.Done
        }
      }

      timer.tick() // nothing happened before start
      assert(counter == 0)

      task.startImmediately(); timer.tick()
      assert(counter == 1)

      ctl.advance(mockTaskInterval - 1.second); timer.tick()
      assert(counter == 1)

      ctl.advance(1.seconds); timer.tick()
      assert(counter == 2)

      ctl.advance(mockTaskInterval); timer.tick()
      assert(counter == 3)

      await(task.close())
    }
  }

  test("task exports total metrics") {
    val imsr = new InMemoryStatsReceiver()
    Time.withCurrentTimeFrozen { ctl =>
      val timer = new MockTimer
      var p1 = new Promise[Unit]
      var p2 = new Promise[Unit]
      val task1 = new MockTask(timer, imsr.scope("task1")) {
        def task(): Future[Unit] = p1
      }
      val task2 = new MockTask(timer, imsr.scope("task2")) {
        def task(): Future[Unit] = p2
      }

      task1.startImmediately(); task2.startImmediately()
      assert(imsr.counter("task1", "total")() == 1)
      assert(imsr.counter("task1", "success")() == 0)
      assert(imsr.counter("task2", "total")() == 1)
      assert(imsr.counter("task2", "success")() == 0)

      p1.setDone()
      assert(imsr.counter("task1", "total")() == 1)
      assert(imsr.counter("task1", "success")() == 1)
      assert(imsr.counter("task2", "total")() == 1)
      assert(imsr.counter("task2", "success")() == 0)

      await(task1.close())
      await(task2.close())
    }
  }

  test("task exports latency metrics") {
    val imsr = new InMemoryStatsReceiver()
    Time.withCurrentTimeFrozen { ctl =>
      val timer = new MockTimer
      var p1 = new Promise[Unit]
      val task = new MockTask(timer, imsr) {
        def task(): Future[Unit] = p1
      }

      task.startImmediately(); timer.tick()
      ctl.advance(5.milliseconds)
      p1.setDone()
      assert(imsr.stats(Seq("latency_ms")).head == 5)

      await(task.close())
    }
  }

  test("jittered start is scheduled appropriately") {
    val imsr = new InMemoryStatsReceiver()
    Time.withCurrentTimeFrozen { ctl =>
      val timer = new MockTimer
      val rngSeed = 0L
      val rng = Rng(0)
      val jitterStartNs = Rng(0).nextLong(mockTaskInterval.inNanoseconds)
      assert(jitterStartNs != 0)
      val jitter = Duration.fromNanoseconds(jitterStartNs)
      val task = new MockTask(timer, imsr, rng) {
        def task(): Future[Unit] = Future.Done
      }

      task.jitteredStart()

      timer.tick()
      assert(imsr.counter("total")() == 0)

      ctl.advance(jitter); timer.tick() // will start here
      assert(imsr.counter("total")() == 1)

      ctl.advance(mockTaskInterval); timer.tick() // scheduled
      assert(imsr.counter("total")() == 2)

      await(task.close())
      ctl.advance(mockTaskInterval); timer.tick() // closed
      assert(imsr.counter("total")() == 2)
    }
  }

  test("task exports failure metrics") {
    val imsr = new InMemoryStatsReceiver()
    Time.withCurrentTimeFrozen { ctl =>
      val timer = new MockTimer
      val task = new MockTask(timer, imsr) {
        def task(): Future[Unit] = Future.exception(new Exception("boom"))
      }

      assert(imsr.counter("failures")() == 0)

      task.startImmediately(); timer.tick()
      assert(imsr.counter("failures")() == 1)
      assert(imsr.counter("failures", "java.lang.Exception")() == 1)

      await(task.close())
    }
  }

  test("closed task does not run") {
    @volatile var counter: Int = 0
    Time.withCurrentTimeFrozen { ctl =>
      val timer = new MockTimer
      val task = new MockTask(timer) {
        def task(): Future[Unit] = {
          counter = counter + 1
          Future.Done
        }
      }

      task.startImmediately(); timer.tick()
      assert(counter == 1)

      await(task.close())
      ctl.advance(mockTaskInterval); timer.tick()
      assert(counter == 1)

      await(task.close())
    }
  }
}
