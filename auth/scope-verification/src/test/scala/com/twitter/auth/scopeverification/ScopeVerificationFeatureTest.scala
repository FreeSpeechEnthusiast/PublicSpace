package com.twitter.auth.scopeverification

import com.twitter.finatra.mtls.EmbeddedMtlsApp
import com.twitter.inject.Test

class ScopeVerificationFeatureTest extends Test {

  def app = new EmbeddedMtlsApp(new ScopeVerification)

}
