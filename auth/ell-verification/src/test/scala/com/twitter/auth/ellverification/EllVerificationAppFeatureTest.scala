package com.twitter.auth.ellverification

import com.twitter.finatra.mtls.EmbeddedMtlsApp
import com.twitter.inject.Test

class EllVerificationAppFeatureTest extends Test {
  def app = new EmbeddedMtlsApp(new EllVerificationApp)
}
