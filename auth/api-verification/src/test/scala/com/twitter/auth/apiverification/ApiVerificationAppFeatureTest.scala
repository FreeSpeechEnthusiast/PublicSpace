package com.twitter.auth.apiverification

import com.twitter.finatra.mtls.EmbeddedMtlsApp
import com.twitter.inject.Test

class ApiVerificationAppFeatureTest extends Test {

  def app = new EmbeddedMtlsApp(new ApiVerificationApp)

  test("ApiVerificationApp#print help") {
    // help always terminates with a non-zero exit-code.
    intercept[Exception] {
      app.main("help" -> "true")
    }
  }
}
