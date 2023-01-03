package com.twitter.ann.common

import com.twitter.util.Return
import com.twitter.util.Throw
import com.twitter.util.Try
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class MemoizedInEpochsSpec extends AnyFunSuite with Matchers {
  private def makeMemoize(): MemoizedInEpochs[Int, String] = {
    var count = -1
    new MemoizedInEpochs[Int, String]({ x =>
      count += 1
      Return(s"${x}:${count}")
    })
  }

  test("Repetitive function calls are memoized") {
    val memoize = makeMemoize()

    memoize.epoch(Seq(0, 1)) should be(Seq("0:0", "1:1"))
    memoize.epoch(Seq(0, 1)) should be(Seq("0:0", "1:1"))
    memoize.currentEpochKeys should be(Set(0, 1))
  }

  test("New keys are memoized, previous are reused") {
    val memoize = makeMemoize()

    memoize.epoch(Seq(0)) should be(Seq("0:0"))
    memoize.epoch(Seq(0, 1)) should be(Seq("0:0", "1:1"))
    memoize.epoch(Seq(1)) should be(Seq("1:1"))
    memoize.currentEpochKeys should be(Set(1))
  }

  test("Values aren't reused over an epoch") {
    val memoize = makeMemoize()

    memoize.epoch(Seq(0)) should be(Seq("0:0"))
    memoize.epoch(Seq(1)) should be(Seq("1:1"))
    memoize.epoch(Seq(0)) should be(Seq("0:2"))
    memoize.currentEpochKeys should be(Set(0))
  }

  test("Throwing skip memoizing") {
    val memoize = new MemoizedInEpochs[Int, String]({ x =>
      Throw(new IllegalArgumentException())
    })

    memoize.epoch(Seq(0)) should be(Seq.empty)
    memoize.currentEpochKeys should be(Set.empty)
  }

  test("Partial throws skip thrown keys") {
    val memoize = new MemoizedInEpochs[Int, String]({
      case x if x % 2 == 0 => Try(x.toString)
      case _ => Throw(new IllegalArgumentException())
    })

    memoize.epoch(Seq(0, 1, 2)) should be(Seq("0", "2"))
    memoize.currentEpochKeys should be(Set(0, 2))
  }
}
