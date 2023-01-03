package com.twitter.auth.common

trait DataSerializer[T] {

  val SerializationAlgorithm: String

  // In order to generate consistent PASETO tokens, the data obj
  // needs to be serialized into a string in a consistent manner
  def serializeData(data: T): String
}
