package com.twitter.auth.pasetoheaders.javahelpers

import java.util.Optional
import java.util
import scala.language.implicitConversions

object OptionConv {
  implicit def optionalSetToOptionSet[K, M](
    opt: Optional[util.Set[K]]
  )(
    implicit c: Optional[util.Set[K]] => Option[util.Set[K]],
    sc: util.Set[K] => collection.Set[M]
  ): Option[collection.Set[M]] = c(opt) match {
    case Some(value) => Some(sc(value))
    case None => None
  }
  implicit def optionSetToOptionalSet[K, M](
    opt: Option[collection.Set[K]]
  )(
    implicit c: Option[collection.Set[K]] => Optional[collection.Set[K]],
    sc: collection.Set[K] => util.Set[M]
  ): Optional[util.Set[M]] = {
    c(opt).isPresent match {
      case true => Optional.of(sc(opt.get))
      case false => Optional.empty[util.Set[M]]
    }
  }
  implicit def optionalMapToOptionMap[K, V](
    opt: Optional[util.Map[K, V]]
  )(
    implicit c: Optional[util.Map[K, V]] => Option[util.Map[K, V]],
    mc: util.Map[K, V] => Map[K, V]
  ): Option[Map[K, V]] = c(opt) match {
    case Some(value) => Some(mc(value))
    case None => None
  }
  implicit def optionMapToOptionalMap[K, V](
    opt: Option[Map[K, V]]
  )(
    implicit c: Option[Map[K, V]] => Optional[Map[K, V]],
    mc: Map[K, V] => util.Map[K, V]
  ): Optional[util.Map[K, V]] = {
    c(opt).isPresent match {
      case true => Optional.of(mc(opt.get))
      case false => Optional.empty[util.Map[K, V]]
    }
  }
  implicit def optionLongToOptionalJavaLong(
    opt: Option[Long]
  )(
    implicit c: Long => java.lang.Long
  ): Optional[java.lang.Long] = opt match {
    case Some(value) => Some(c(value))
    case None => Optional.empty[java.lang.Long]
  }
  implicit def optionalJavaLongToOptionLong(
    opt: Optional[java.lang.Long]
  )(
    implicit c: java.lang.Long => Long
  ): Option[Long] =
    opt.isPresent match {
      case true => Some(c(opt.get()))
      case false => None
    }
  implicit def optionToOptional[A](opt: Option[A]): Optional[A] = opt match {
    case Some(value) => Optional.of(value)
    case None => Optional.empty[A]
  }
  implicit def optionalToOption[A](opt: Optional[A]): Option[A] = opt.isPresent match {
    case true => Some(opt.get())
    case false => None
  }
}
