package com.twitter.auth.policykeeper.api.evaluationengine

import reflect.runtime.universe._
import scala.reflect.ClassTag
import scala.reflect.classTag

final case class ExpressionInputTypeCastException() extends Exception
final case class UnknownInputTypeException() extends Exception
final case class UnknownTargetTypeException() extends Exception

final case class TypeCaster[T: TypeTag: ClassTag]() {

  /**
   * Supported types declaration
   */
  val StringTag = classTag[String]
  val IntTag = classTag[Int]
  val LongTag = classTag[Long]
  val BoolTag = classTag[Boolean]

  /**
   * Casts underlying value to boolean
   *
   * @return
   * @throws ExpressionInputTypeCastException
   * @throws UnknownInputTypeException
   */
  private def toBoolean(valueForCasting: Any): Boolean = {
    valueForCasting match {
      case strValue: String =>
        strValue match {
          case "true" => true
          case "false" => false
          case _ => throw ExpressionInputTypeCastException()
        }
      case intValue: Int =>
        intValue match {
          case 1 => true
          case 0 => false
          case _ => throw ExpressionInputTypeCastException()
        }
      case longValue: Long =>
        longValue match {
          case 1L => true
          case 0L => false
          case _ => throw ExpressionInputTypeCastException()
        }
      case boolValue: Boolean =>
        boolValue
      case _ => throw UnknownInputTypeException()
    }
  }

  /**
   * Casts underlying value to integer
   *
   * @return
   * @throws ExpressionInputTypeCastException
   * @throws UnknownInputTypeException
   */
  private def toInt(valueForCasting: Any): Int = {
    valueForCasting match {
      case strValue: String =>
        strValue.toInt
      case intValue: Int =>
        intValue
      case longValue: Long => longValue.toInt
      case boolValue: Boolean =>
        boolValue match {
          case true => 1
          case false => 0
        }
      case _ => throw UnknownInputTypeException()
    }
  }

  /**
   * Casts underlying value to long
   *
   * @return
   * @throws ExpressionInputTypeCastException
   * @throws UnknownInputTypeException
   */
  private def toLong(valueForCasting: Any): Long = {
    valueForCasting match {
      case strValue: String =>
        strValue.toLong
      case intValue: Int =>
        intValue.toLong
      case longValue: Long => longValue
      case boolValue: Boolean =>
        boolValue match {
          case true => 1L
          case false => 0L
        }
      case _ => throw UnknownInputTypeException()
    }
  }

  private def toString(valueForCasting: Any): String = {
    valueForCasting match {
      case strValue: String =>
        strValue
      case intValue: Int =>
        intValue.toString
      case longValue: Long => longValue.toString
      case boolValue: Boolean =>
        boolValue match {
          case true => "true"
          case false => "false"
        }
      case _ => throw UnknownInputTypeException()
    }
  }

  def apply(valueForCasting: Any): T = {
    try {
      classTag[T] match {
        case BoolTag =>
          toBoolean(valueForCasting).asInstanceOf[T]
        case StringTag =>
          toString(valueForCasting).asInstanceOf[T]
        case IntTag =>
          toInt(valueForCasting).asInstanceOf[T]
        case LongTag =>
          toLong(valueForCasting).asInstanceOf[T]
        case _ => throw UnknownTargetTypeException()
      }
    } catch {
      case _: Exception => throw ExpressionInputTypeCastException()
    }
  }
}
