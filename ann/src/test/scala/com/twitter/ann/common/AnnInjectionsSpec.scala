package com.twitter.ann.common

import java.lang
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class AnnInjectionsSpec extends FunSuite with MockitoSugar {
  test("test scala Long injection") {
    val num = 1L
    val bytes = AnnInjections.LongInjection(num)
    assert(AnnInjections.LongInjection.invert(bytes).get == num)
  }

  test("test scala Int injection") {
    val num = 1
    val bytes = AnnInjections.IntInjection(num)
    assert(AnnInjections.IntInjection.invert(bytes).get == num)
  }

  test("test scala String injection") {
    val word = "Hi"
    val bytes = AnnInjections.StringInjection(word)
    assert(AnnInjections.StringInjection.invert(bytes).get == word)
  }

  test("test java Long injection") {
    val num = new lang.Long(1L)
    val bytes = AnnInjections.JLongInjection(num)
    assert(AnnInjections.JLongInjection.invert(bytes).get == num)
  }

  test("test java Integer injection") {
    val num = new Integer(1)
    val bytes = AnnInjections.JIntInjection(num)
    assert(AnnInjections.JIntInjection.invert(bytes).get == num)
  }

  test("test java String injection") {
    val word = new java.lang.String("Hi")
    val bytes = AnnInjections.JStringInjection(word)
    assert(AnnInjections.JStringInjection.invert(bytes).get == word)
  }
}
