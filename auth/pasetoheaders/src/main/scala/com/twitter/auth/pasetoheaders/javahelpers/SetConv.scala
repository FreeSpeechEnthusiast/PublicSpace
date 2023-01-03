package com.twitter.auth.pasetoheaders.javahelpers

import java.util
import scala.collection.JavaConverters
import scala.language.implicitConversions

object SetConv {
  implicit def convertJavaMutableToScalaImmutable[K, M](
    s: util.Set[K]
  )(
    implicit c: K => M
  ): collection.Set[M] =
    JavaConverters
      .asScalaSet(s)
      .map(c)
      .toSet
  implicit def convertScalaImmutableToJavaMutable[K, M](
    s: collection.Set[K]
  )(
    implicit c: K => M
  ): util.Set[M] =
    JavaConverters
      .setAsJavaSet(s.map(c))
}
