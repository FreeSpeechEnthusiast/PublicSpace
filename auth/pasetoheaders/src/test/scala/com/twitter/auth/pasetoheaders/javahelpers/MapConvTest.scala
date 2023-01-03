package com.twitter.auth.pasetoheaders.javahelpers

import MapConv._
import java.util
import org.junit.runner.RunWith
import org.scalatest.OneInstancePerTest
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class MapConvTest extends AnyFunSuite with OneInstancePerTest with Matchers {

  def getScalaImmutableMap[K, V](map: util.Map[K, V]): Map[K, V] = {
    map
  }

  def getJavaMutableMap[K, V](map: Map[K, V]): util.Map[K, V] = {
    map
  }

  test("implicit java mutable map to scala immutable map test") {
    val mutableMap = new util.HashMap[Long, String]()
    mutableMap.put(1L, "val1")
    mutableMap.put(2L, "val2")
    getScalaImmutableMap(mutableMap) mustEqual Map(1L -> "val1", 2L -> "val2")
    getScalaImmutableMap(mutableMap) must not equal Map(1L -> "val2", 2L -> "val2")
  }

  test("implicit immutable map to java mutable map test") {
    val mutableMap = new util.HashMap[Long, String]()
    mutableMap.put(1L, "val1")
    mutableMap.put(2L, "val2")
    getJavaMutableMap(Map(1L -> "val1", 2L -> "val2")) mustEqual mutableMap
    getJavaMutableMap(Map(1L -> "val2", 2L -> "val2")) must not equal mutableMap
  }
}
