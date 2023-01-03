package com.twitter.auth.pasetoheaders.s2s.models

import scala.annotation.meta.field
import scala.annotation.meta.getter
import scala.annotation.meta.param

/** Custom annotation is required to get JsonProperty work correctly with Scala case classes */
object JacksonAnnotations {
  type JsonProperty = com.fasterxml.jackson.annotation.JsonProperty @param @field @getter
}
