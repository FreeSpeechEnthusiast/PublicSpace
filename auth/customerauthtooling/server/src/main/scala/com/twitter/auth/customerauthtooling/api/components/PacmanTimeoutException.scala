package com.twitter.auth.customerauthtooling.api.components

import com.twitter.util.Duration

final case class PacmanTimeoutException(
  expectedTimeout: Duration)
    extends Exception
