package com.twitter.auth.sso.client

import com.twitter.auth.sso.models.AssociationMethod
import com.twitter.auth.sso.models.SsoInfo
import com.twitter.util.Time
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SsoInfoEncryptorSpec extends AnyFunSuite with Matchers {

  // This must be at least 256 bits or AesCompactThriftStructEncryptor throws SecretKeyTooShortException
  val testSecret: String = "1234567890abcdefghijklmnopqrstuv"

  def mkSsoInfo(): SsoInfo = SsoInfo(
    ssoId = "test-sso-id",
    ssoEmail = "test-email",
    associationMethod = AssociationMethod.Login,
    time = Time.now
  )

  val client: SsoInfoEncryptor = SsoInfoEncryptor(testSecret)

  test("Encrypts and decrypts successfully") {
    Time.withCurrentTimeFrozen { _ =>
      val testSsoInfo = mkSsoInfo()
      val encrypted = client.encrypt(testSsoInfo)
      // This ssoIdHash should be consistent as long as testSecret remains the same.
      encrypted.ssoIdHash must be("5jZddqn1NyOVbJrdon0wt0/g6ensBLvgYgEG9L4W9tA=")
      client.decrypt(encrypted) must equal(testSsoInfo)
    }
  }

  test("Hashes ssoEmail successfully") {
    Time.withCurrentTimeFrozen { _ =>
      val email = "sdeepu@twitter.com"
      val hashedValue = client.hashSsoEmail(email)
      // Test email is hashed
      hashedValue must be("Xi1rhaeXe9UKG1bKSFcP6A2B85mw9VCUaSNsoYsewnM=")

    }
  }

}
