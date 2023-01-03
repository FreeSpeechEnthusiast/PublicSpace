package com.twitter.auth.customerauthtooling.api.services

trait DefaultInputValues {
  private[api] val DefaultDpProvider = "manual"
  private[api] val DefaultAutoDecider = true
  private[api] val DefaultUpdateOnDraft = false

  private[api] val DefaultIgnoreInvalid = true
  private[api] val DefaultIgnoreErrors = true
}
