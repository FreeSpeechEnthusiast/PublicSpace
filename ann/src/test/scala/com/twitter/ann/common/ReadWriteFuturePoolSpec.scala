package com.twitter.ann.common

import com.twitter.util.{Await, Future, FuturePool}
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class ReadWriteFuturePoolSpec extends FunSuite with MockitoSugar {
  test("test ReadWriteFuturePool with separate future pool for read and write") {
    val readWriteFuturePool =
      ReadWriteFuturePool(FuturePool.immediatePool, FuturePool.immediatePool)
    val readValue = readWriteFuturePool.read(true)
    val writeValue = readWriteFuturePool.write(false)
    assert(Await.result(readValue))
    assert(!Await.result(writeValue))
  }

  test("test ReadWriteFuturePool with common pool for read and write") {
    val readWriteFuturePool = ReadWriteFuturePool(FuturePool.immediatePool)
    val readValue = readWriteFuturePool.read(true)
    val writeValue = readWriteFuturePool.write(false)
    assert(Await.result(readValue))
    assert(!Await.result(writeValue))
  }

  test("test ReadWriteFuturePoolANN with common pool for read and write") {
    val readResult = Future.value("read")
    val writeResult = Future.value("write")
    val readPool = new FuturePool {
      override def apply[T](f: => T): Future[T] = readResult.asInstanceOf[Future[T]]
    }
    val writePool = new FuturePool {
      override def apply[T](f: => T): Future[T] = writeResult.asInstanceOf[Future[T]]
    }

    val readWriteFuturePool = new ReadWriteFuturePoolANN(readPool, writePool)
    assert(readResult == readWriteFuturePool.read("ignored"))
    assert(writeResult == readWriteFuturePool.write("ignored"))
  }
}
