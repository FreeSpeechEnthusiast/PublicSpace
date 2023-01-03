package com.twitter.auth.policykeeper.api.evaluationengine

import com.twitter.auth.policykeeper.api.evaluationengine.ExpressionInputParameterName.DefaultNamespace
import scala.util.matching.Regex

final case class ExpressionInputParameterName(name: String, namespace: String = DefaultNamespace) {
  override def toString: String = namespace + "." + name
}

object ExpressionInputParameterName {

  private[api] val Name = "name"
  private[api] val Namespace = "namespace"
  private[api] val DefaultNamespace = "default"

  protected val inputParameterNameMatchPattern: Regex =
    "^\\s*([a-zA-Z0-9_]+)(\\.([a-zA-Z0-9_]+))?\\s*$".r(Namespace, "", Name)

  def fromString(rawParameterName: String): Option[ExpressionInputParameterName] = {
    rawParameterName match {
      case inputParameterNameMatchPattern(namespace: String, _, name: String) =>
        Some(ExpressionInputParameterName(name = name, namespace = namespace))
      case inputParameterNameMatchPattern(name: String, _, _) =>
        Some(ExpressionInputParameterName(name = name, namespace = DefaultNamespace))
      case _ => None
    }
  }

}

object InputValueTypeCasters {
  val toLongCaster: TypeCaster[Long] = TypeCaster[Long]
  val toIntCaster: TypeCaster[Int] = TypeCaster[Int]
  val toBooleanCaster: TypeCaster[Boolean] = TypeCaster[Boolean]
  val toStringCaster: TypeCaster[String] = TypeCaster[String]
}

case class ExpressionInputParameterValue(underlyingValue: Any) {

  /**
   * Casts underlying value to string
   *
   * @return
   * @throws ExpressionInputTypeCastException
   * @throws UnknownInputTypeException
   */
  override def toString: String = {
    InputValueTypeCasters.toStringCaster(underlyingValue)
  }

  /**
   * Casts underlying value to boolean
   *
   * @return
   * @throws ExpressionInputTypeCastException
   * @throws UnknownInputTypeException
   */
  def toBoolean: Boolean = {
    InputValueTypeCasters.toBooleanCaster(underlyingValue)
  }

  /**
   * Casts underlying value to integer
   *
   * @return
   * @throws ExpressionInputTypeCastException
   * @throws UnknownInputTypeException
   */
  def toInt: Int = {
    InputValueTypeCasters.toIntCaster(underlyingValue)
  }

  /**
   * Casts underlying value to long
   *
   * @return
   * @throws ExpressionInputTypeCastException
   * @throws UnknownInputTypeException
   */
  def toLong: Long = {
    InputValueTypeCasters.toLongCaster(underlyingValue)
  }
}

case class ExpressionInput(
  underlyingMap: Map[ExpressionInputParameterName, ExpressionInputParameterValue])
    extends Map[ExpressionInputParameterName, ExpressionInputParameterValue] {
  override def +[V1 >: ExpressionInputParameterValue](
    kv: (ExpressionInputParameterName, V1)
  ): Map[ExpressionInputParameterName, V1] = underlyingMap + kv

  override def get(key: ExpressionInputParameterName): Option[ExpressionInputParameterValue] =
    underlyingMap.get(key)

  override def iterator: Iterator[
    (ExpressionInputParameterName, ExpressionInputParameterValue)
  ] = underlyingMap.iterator

  override def -(
    key: ExpressionInputParameterName
  ): Map[ExpressionInputParameterName, ExpressionInputParameterValue] = underlyingMap - key
}
