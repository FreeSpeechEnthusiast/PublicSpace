package com.twitter.auth.pasetoheaders.javahelpers

import scala.collection.JavaConverters
import scala.collection.immutable.Map
import java.util
import scala.language.implicitConversions

object MapConv {
  implicit def convertJavaMutableToScalaImmutable[K, V](
    m: util.Map[K, V]
  ): Map[K, V] =
    JavaConverters
      .mapAsScalaMap(m)
      .toMap
  implicit def convertScalaImmutableToJavaMutable[K, V](
    m: Map[K, V]
  ): util.Map[K, V] =
    JavaConverters
      .mapAsJavaMap(m)
}
