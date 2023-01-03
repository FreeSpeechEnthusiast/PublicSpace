package com.twitter.auth.pasetoheaders.javahelpers

import java.util.Optional
import org.junit.runner.RunWith
import org.scalatest.matchers.must.Matchers
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.OneInstancePerTest
import org.scalatestplus.junit.JUnitRunner
import OptionConv._

@RunWith(classOf[JUnitRunner])
class OptionConvTest extends AnyFunSuite with OneInstancePerTest with Matchers {

  def optionToOptionalComparator[A](optScala: Option[A], optJava: Optional[A]): Boolean = {
    if (optScala.isDefined != optJava.isPresent) {
      // one is empty, another one is not empty
      false
    } else if (optScala.isEmpty && !optJava.isPresent) {
      // both are empty
      true
    } else {
      // both are not empty
      optScala.get == optJava.get()
    }
  }

  test("comparator test") {
    optionToOptionalComparator(Some(10L), Optional.of(10L)) mustEqual true
    optionToOptionalComparator(Some(11L), Optional.of(10L)) mustEqual false
    optionToOptionalComparator(Some(10L), Optional.of(11L)) mustEqual false
    optionToOptionalComparator(None, Optional.of(11L)) mustEqual false
    optionToOptionalComparator(None, Optional.empty) mustEqual true
    optionToOptionalComparator(Some(10L), Optional.empty[Long]) mustEqual false
  }

  test("implicit optionalToOption test") {
    optionToOptionalComparator(Optional.of(10L), Optional.of(10L)) mustEqual true
    optionToOptionalComparator(Optional.of(11L), Optional.of(10L)) mustEqual false
    optionToOptionalComparator(Optional.of(10L), Optional.of(11L)) mustEqual false
    optionToOptionalComparator(Optional.empty, Optional.of(11L)) mustEqual false
    optionToOptionalComparator(Optional.empty, Optional.empty) mustEqual true
    optionToOptionalComparator(Optional.of(10L), Optional.empty[Long]) mustEqual false
  }

  test("implicit optionToOptional test") {
    optionToOptionalComparator[Long](Some(10L), Some(10L)) mustEqual true
    optionToOptionalComparator[Long](Some(11L), Some(10L)) mustEqual false
    optionToOptionalComparator[Long](Some(10L), Some(11L)) mustEqual false
    optionToOptionalComparator[Long](None, Some(11L)) mustEqual false
    optionToOptionalComparator[Long](None, None) mustEqual true
    optionToOptionalComparator[Long](Some(10L), None) mustEqual false
  }

}
