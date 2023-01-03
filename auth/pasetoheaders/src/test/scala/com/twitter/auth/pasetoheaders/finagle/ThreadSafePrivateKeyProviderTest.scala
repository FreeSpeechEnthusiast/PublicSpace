package com.twitter.auth.pasetoheaders.finagle

import java.security.PrivateKey
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ThreadSafePrivateKeyProviderTest extends PrivateKeyProviderTest {

  override protected def buildStorage(): UpdatableKeyStorage[PrivateKey] =
    ThreadSafeUpdatableKeyStorage[PrivateKey]()

}
