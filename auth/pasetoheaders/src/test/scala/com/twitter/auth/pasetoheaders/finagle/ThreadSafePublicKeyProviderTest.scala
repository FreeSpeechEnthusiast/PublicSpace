package com.twitter.auth.pasetoheaders.finagle

import java.security.PublicKey
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ThreadSafePublicKeyProviderTest extends PublicKeyProviderTest {

  override protected def buildStorage(): UpdatableKeyStorage[PublicKey] =
    ThreadSafeUpdatableKeyStorage[PublicKey]()

}
