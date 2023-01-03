package com.twitter.auth.policykeeper.api.evaluationengine.scalastaticengine

import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInputParameterName
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ScalaStaticExpressionTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter {

  test("test ScalaStaticExpression (valid, 0 params)") {
    val expression = ScalaStaticExpression("class()")
    expression.isValid mustBe true
    expression.expressionClass mustBe Some("class")
    expression.requiredInput() mustBe Set()
  }

  test("test ScalaStaticExpression (valid, 0 params with spaces)") {
    val expression = ScalaStaticExpression(" class  (   ) ")
    expression.isValid mustBe true
    expression.expressionClass mustBe Some("class")
    expression.requiredInput() mustBe Set()
  }

  test("test ScalaStaticExpression (valid, 1 param)") {
    val expression = ScalaStaticExpression("class(ns.var1)")
    expression.isValid mustBe true
    expression.expressionClass mustBe Some("class")
    expression.requiredInput() mustBe Set(ExpressionInputParameterName("var1", "ns"))
  }

  test("test ScalaStaticExpression (valid, 1 param default namespace)") {
    val expression = ScalaStaticExpression("class(var1)")
    expression.isValid mustBe true
    expression.expressionClass mustBe Some("class")
    expression.requiredInput() mustBe Set(
      ExpressionInputParameterName("var1", ExpressionInputParameterName.DefaultNamespace))
  }

  test("test ScalaStaticExpression (valid, 3 params)") {
    val expression = ScalaStaticExpression("class(ns.var1,ns2.var1,ns3.var3)")
    expression.isValid mustBe true
    expression.expressionClass mustBe Some("class")
    expression.requiredInput() mustBe Set(
      ExpressionInputParameterName("var1", "ns"),
      ExpressionInputParameterName("var1", "ns2"),
      ExpressionInputParameterName("var3", "ns3")
    )
  }

  test("test ScalaStaticExpression (valid, 1 param multiple times)") {
    val expression = ScalaStaticExpression("class(ns.var1,ns.var1,ns.var1)")
    expression.isValid mustBe true
    expression.expressionClass mustBe Some("class")
    expression.requiredInput() mustBe Set(
      ExpressionInputParameterName("var1", "ns"),
    )
    expression.positionedArgs mustBe Seq(
      ExpressionInputParameterName("var1", "ns"),
      ExpressionInputParameterName("var1", "ns"),
      ExpressionInputParameterName("var1", "ns"))
  }

  test("test ScalaStaticExpression (valid, 3 params with spaces)") {
    val expression = ScalaStaticExpression("  class (    ns.var1,ns2.var1,   ns3.var3  ) ")
    expression.isValid mustBe true
    expression.expressionClass mustBe Some("class")
    expression.requiredInput() mustBe Set(
      ExpressionInputParameterName("var1", "ns"),
      ExpressionInputParameterName("var1", "ns2"),
      ExpressionInputParameterName("var3", "ns3")
    )
  }

  test("test ScalaStaticExpression (invalid, 3 params with beginning comma )") {
    val expression = ScalaStaticExpression("class(,ns.var1,ns2.var1,ns3.var3)")
    expression.isValid mustBe false
    expression.expressionClass mustBe None
    expression.requiredInput() mustBe Set()
  }

  test("test ScalaStaticExpression (invalid, 3 params with ending comma )") {
    val expression = ScalaStaticExpression("class(ns.var1,ns2.var1,ns3.var3,)")
    expression.isValid mustBe false
    expression.expressionClass mustBe None
    expression.requiredInput() mustBe Set()
  }

  test("test ScalaStaticExpression (invalid, 3 params with missing begin parenthesis)") {
    val expression = ScalaStaticExpression("class ns.var1,ns2.var1,ns3.var3)")
    expression.isValid mustBe false
    expression.expressionClass mustBe None
    expression.requiredInput() mustBe Set()
  }

  test("test ScalaStaticExpression (invalid, 3 params with missing end parenthesis)") {
    val expression = ScalaStaticExpression("class(ns.var1,ns2.var1,ns3.var3")
    expression.isValid mustBe false
    expression.expressionClass mustBe None
    expression.requiredInput() mustBe Set()
  }

  test("test ScalaStaticExpression (invalid, missing comma)") {
    val expression = ScalaStaticExpression("class(ns.var1 ns2.var1,ns3.var3")
    expression.isValid mustBe false
    expression.expressionClass mustBe None
    expression.requiredInput() mustBe Set()
  }

  test("test ScalaStaticExpression (invalid, wrong class name)") {
    val expression = ScalaStaticExpression("cla#ss(ns.var1 ns2.var1,ns3.var3")
    expression.isValid mustBe false
    expression.expressionClass mustBe None
    expression.requiredInput() mustBe Set()
  }
}
