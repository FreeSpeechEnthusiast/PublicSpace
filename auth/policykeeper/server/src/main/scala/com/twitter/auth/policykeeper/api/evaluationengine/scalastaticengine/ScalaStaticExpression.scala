package com.twitter.auth.policykeeper.api.evaluationengine.scalastaticengine

import com.twitter.auth.policykeeper.api.evaluationengine.Expression
import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInputParameterName
import scala.util.matching.Regex

/**
 * This expression language defines very simple expression language:
 * policy.rule.expression="ScalaClassName(namespace1.arg1,namespace2.arg2...)"
 *
 * @param underlyingExpression
 */
final case class ScalaStaticExpression(underlyingExpression: String)
    extends Expression(underlyingExpression = underlyingExpression) {

  private val Args = "args"
  private val ClassName = "className"

  protected val staticLanguagePattern: Regex =
    "^\\s*([a-zA-Z0-9_]+)\\s*\\(((\\s*[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)?\\s*,?)*[^,\\s])?\\s*\\)\\s*$"
      .r(
        ClassName,
        Args,
        "",
        ""
      )

  private val matcher = staticLanguagePattern.findAllMatchIn(underlyingExpression)

  private val hasMatches = matcher.nonEmpty

  private val evaluationInfo: Option[ScalaStaticExpressionEvaluationInfo] = matcher.collect {
    case r => Map(ClassName -> r.group(ClassName), Args -> r.group(Args))
  }.toSeq match {
    case Seq(m) =>
      Some(
        ScalaStaticExpressionEvaluationInfo(
          m(ClassName),
          m(Args) match {
            case null => Seq.empty[ExpressionInputParameterName] //expression without arguments
            case args: String => //expression with arguments
              (
                args.split(',').map {
                  ExpressionInputParameterName.fromString
                } collect {
                  case Some(p) => p
                }
              ).toSeq
          }
        )
      )
    case _ => None
  }

  val expressionClass: Option[String] = evaluationInfo match {
    case Some(i) => Some(i.className)
    case _ => None
  }

  val positionedArgs: Seq[ExpressionInputParameterName] = evaluationInfo match {
    case Some(i) => i.args
    case _ => Seq()
  }

  override def requiredInput(): Set[ExpressionInputParameterName] = positionedArgs.toSet

  override def isValid: Boolean = {
    hasMatches
  }
}

case class ScalaStaticExpressionEvaluationInfo(
  className: String,
  args: Seq[ExpressionInputParameterName])
