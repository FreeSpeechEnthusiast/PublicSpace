package com.twitter.auth.customerauthtooling.api.models

private[api] case class AdoptionParameterValue[T <: AdoptionParameter](
  private val underlying: Boolean) {

  def toBoolean: Boolean =
    underlying
}
