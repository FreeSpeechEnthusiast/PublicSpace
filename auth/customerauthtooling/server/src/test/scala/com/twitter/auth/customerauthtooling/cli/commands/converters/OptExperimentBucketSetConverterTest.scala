package com.twitter.auth.customerauthtooling.cli.commands.converters

import com.twitter.auth.customerauthtooling.thriftscala.ExperimentBucket
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class OptExperimentBucketSetConverterTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter {

  private val converter = new OptExperimentBucketSetConverter()
  test("test OptExperimentBucketSetConverter with empty value") {
    converter.convert("") mustBe Some(ExperimentBucketSetWrapper(Set()))
  }

  test("test OptExperimentBucketSetConverter with incorrect input #1") {
    intercept[InvalidInputException] {
      converter.convert(",")
    }
  }

  test("test OptExperimentBucketSetConverter with incorrect input #2") {
    intercept[InvalidInputException] {
      converter.convert("a,b,c")
    }
  }

  test("test OptExperimentBucketSetConverter with incorrect input #3") {
    intercept[InvalidInputException] {
      converter.convert("a:b,b")
    }
  }

  test("test OptExperimentBucketSetConverter with valid input #1") {
    converter.convert("b:a") mustBe Some(
      ExperimentBucketSetWrapper(Set(ExperimentBucket(key = "a", bucket = "b"))))
  }

  test("test OptExperimentBucketSetConverter with valid input #2") {
    converter.convert("b:a,c:d") mustBe Some(
      ExperimentBucketSetWrapper(
        Set(ExperimentBucket(key = "a", bucket = "b"), ExperimentBucket(key = "d", bucket = "c"))))
  }

}
