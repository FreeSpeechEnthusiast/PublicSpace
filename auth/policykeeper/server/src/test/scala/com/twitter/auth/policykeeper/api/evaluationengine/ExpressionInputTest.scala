package com.twitter.auth.policykeeper.api.evaluationengine

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ExpressionInputTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter {

  test("test input parameter name parser (valid, 1 param)") {
    ExpressionInputParameterName
      .fromString("abc.def") mustBe Some(ExpressionInputParameterName("def", "abc"))
  }

  test("test input parameter name parser (valid, 1 param with spaces)") {
    ExpressionInputParameterName
      .fromString(" abc.def ") mustBe Some(ExpressionInputParameterName("def", "abc"))
  }

  test("test input parameter name parser (invalid, with space after namespace)") {
    ExpressionInputParameterName
      .fromString("abc .def") mustBe None
  }

  test("test input parameter name parser (invalid, with space before var name)") {
    ExpressionInputParameterName
      .fromString("abc. def") mustBe None
  }

  test("test input parameter name parser (valid, default namespace)") {
    ExpressionInputParameterName
      .fromString("abc") mustBe Some(
      ExpressionInputParameterName("abc", ExpressionInputParameterName.DefaultNamespace))
  }

  test("test input parameter name parser (valid, default namespace with spaces)") {
    ExpressionInputParameterName
      .fromString("   abc ") mustBe Some(
      ExpressionInputParameterName("abc", ExpressionInputParameterName.DefaultNamespace))
  }

}
