package com.twitter.auth.pasetoheaders.javahelpers

import com.twitter.auth.pasetoheaders.javahelpers.SetConv._
import java.util
import org.junit.runner.RunWith
import org.scalatest.OneInstancePerTest
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SetConvTest extends AnyFunSuite with OneInstancePerTest with Matchers {

  def getScalaImmutableSet[K](set: util.Set[K]): collection.Set[K] = {
    set
  }

  def getJavaMutableSet[K](set: collection.Set[K]): util.Set[K] = {
    set
  }

  test("implicit java mutable set to scala immutable set test") {
    val mutableSet = new util.HashSet[Long]()
    mutableSet.add(1L)
    mutableSet.add(2L)
    getScalaImmutableSet(mutableSet) mustEqual Set(1L, 2L)
    getScalaImmutableSet(mutableSet) must not equal Set(1L, 2L, 3L)
  }

  test("implicit immutable set to java mutable set test") {
    val mutableSet = new util.HashSet[Long]()
    mutableSet.add(1L)
    mutableSet.add(2L)
    getJavaMutableSet(Set(1L, 2L)) mustEqual mutableSet
    getJavaMutableSet(Set(1L, 2L, 3L)) must not equal mutableSet
  }
}
