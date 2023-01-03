package com.twitter.auth.sso.store

import com.twitter.auth.sso.client.SsoInfoEncryptor
import com.twitter.auth.sso.client.StratoSsoInfoDeleter
import com.twitter.auth.sso.client.StratoSsoInfoFetcher
import com.twitter.auth.sso.client.StratoSsoInfoForUserScanner
import com.twitter.auth.sso.client.StratoSsoInfoWriter
import com.twitter.auth.sso.client.StratoSsoUsersForSsoIdScanner
import com.twitter.auth.sso.models.AssociationMethod
import com.twitter.auth.sso.models.Email
import com.twitter.auth.sso.models.SsoId
import com.twitter.auth.sso.models.SsoInfo
import com.twitter.auth.sso.models.SsoProvider
import com.twitter.auth.sso.models.UserId
import com.twitter.auth.sso.thriftscala.{SsoInfo => TSsoInfo}
import com.twitter.stitch.Stitch
import com.twitter.util.Await
import com.twitter.util.Time
import org.junit.runner.RunWith
import org.mockito.Matchers.any
import org.mockito.Matchers.{eq => objEq}
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class AssociationStoreSpec
    extends AnyFunSuite
    with Matchers
    with MockitoSugar
    with BeforeAndAfterEach {

  def stitchAwait[E](s: Stitch[E]): E = {
    Await.result(Stitch.run(s))
  }

  // This must be at least 256 bits or AesCompactThriftStructEncryptor throws SecretKeyTooShortException
  val testSecret: String = "1234567890abcdefghijklmnopqrstuv"
  val encryptor: SsoInfoEncryptor = SsoInfoEncryptor(testSecret)

  val testUserId: UserId = 1L
  val otherUserId: UserId = 2L
  val testSsoId: SsoId = "ssoId"
  val otherSsoId: SsoId = "ssoId"
  val encryptedSsoId = encryptor.hashSsoId(testSsoId)
  val testSsoEmail: Email = "test@test.com"
  val testProvider: SsoProvider = SsoProvider.Test
  val testAssociationMethod: AssociationMethod = AssociationMethod.Login

  val stratoSsoInfoWriter: StratoSsoInfoWriter = mock[StratoSsoInfoWriter]
  val stratoSsoInfoDeleter: StratoSsoInfoDeleter = mock[StratoSsoInfoDeleter]
  val stratoSsoInfoForUserScanner: StratoSsoInfoForUserScanner = mock[StratoSsoInfoForUserScanner]
  val stratoSsoUsersForSsoIdScanner: StratoSsoUsersForSsoIdScanner =
    mock[StratoSsoUsersForSsoIdScanner]
  val stratoSsoInfoFetcher: StratoSsoInfoFetcher = mock[StratoSsoInfoFetcher]

  val writer = new AssociationWriter(
    encryptor = encryptor,
    stratoSsoInfoWriter = stratoSsoInfoWriter,
    stratoSsoInfoDeleter = stratoSsoInfoDeleter
  )

  val reader = new AssociationReader(
    encryptor = encryptor,
    stratoSsoInfoForUserScanner = stratoSsoInfoForUserScanner,
    stratoSsoUsersForSsoIdScanner = stratoSsoUsersForSsoIdScanner,
    stratoSsoInfoFetcher = stratoSsoInfoFetcher
  )

  override def beforeEach(): Unit = {
    reset(
      stratoSsoInfoWriter,
      stratoSsoInfoDeleter,
      stratoSsoInfoForUserScanner,
      stratoSsoUsersForSsoIdScanner,
      stratoSsoInfoFetcher)
  }

  test("AssociationWriter#writeAssociation writes association") {
    Time.withCurrentTimeFrozen { _ =>
      when(
        stratoSsoInfoWriter
          .write(objEq(testUserId), objEq(testProvider), any[TSsoInfo])) thenReturn Stitch.Unit

      stitchAwait(
        writer.writeAssociation(
          testUserId,
          testSsoId,
          testSsoEmail,
          testProvider,
          testAssociationMethod)
      )

      verify(stratoSsoInfoWriter).write(objEq(testUserId), objEq(testProvider), any[TSsoInfo])
    }
  }

  test("AssociationReader#getSsoAccounts returns single sso id") {
    val info = SsoInfo(
      time = Time.now,
      ssoId = testSsoId,
      ssoEmail = testSsoEmail,
      associationMethod = testAssociationMethod
    )

    when(stratoSsoInfoForUserScanner.scan(testUserId)) thenReturn Stitch.value(
      Seq(encryptor.encrypt(info)))
    stitchAwait(reader.getSsoAccounts(testUserId)) must contain(testSsoId)
  }

  test("AssociationReader#getSsoAccounts returns multiple sso ids") {
    val info1 = SsoInfo(
      time = Time.now,
      ssoId = testSsoId,
      ssoEmail = testSsoEmail,
      associationMethod = testAssociationMethod
    )

    val info2 = SsoInfo(
      time = Time.now,
      ssoId = otherSsoId,
      ssoEmail = testSsoEmail,
      associationMethod = testAssociationMethod
    )

    val encrypted: Seq[TSsoInfo] = Seq(info1, info2).map(encryptor.encrypt)
    when(stratoSsoInfoForUserScanner.scan(testUserId)) thenReturn Stitch.value(encrypted)
    stitchAwait(reader.getSsoAccounts(testUserId)) must contain theSameElementsAs (Seq(
      testSsoId,
      otherSsoId))
  }

  test(
    "AssociationReader#getAccountForSsoId throws TooManyUserForSsoId if there is more than one UserId for ssoId") {
    when(stratoSsoUsersForSsoIdScanner.scan(encryptedSsoId)) thenReturn Stitch.value(
      Seq(testUserId, otherUserId))
    intercept[TooManyUserForSsoId] {
      stitchAwait(reader.getAccountForSsoId(testSsoId))
    }
  }

  test("AssociationReader#getAccountForSsoId returns empty if no user exists") {
    when(stratoSsoUsersForSsoIdScanner.scan(encryptedSsoId)) thenReturn Stitch.value(Seq.empty)
    stitchAwait(reader.getAccountForSsoId(testSsoId)) must be('empty)
  }

  test("AssociationReader#getAccountForSsoId returns user is it exists") {
    when(stratoSsoUsersForSsoIdScanner.scan(encryptedSsoId)) thenReturn Stitch.value(
      Seq(testUserId))
    stitchAwait(reader.getAccountForSsoId(testSsoId)) must contain(testUserId)
  }

  test("AssociationReader#getInfo returns ssoInfo") {
    val info = SsoInfo(
      time = Time.now,
      ssoId = testSsoId,
      ssoEmail = testSsoEmail,
      associationMethod = testAssociationMethod
    )

    when(stratoSsoInfoFetcher.fetch(testUserId, testProvider)) thenReturn Stitch.value(
      Some(encryptor.encrypt(info)))

    stitchAwait(reader.getInfo(testUserId, testProvider)) must contain(info)
  }

  test("AssociationReader#getSsoInfo return sso info") {
    val info1 = SsoInfo(
      time = Time.now,
      ssoId = testSsoId,
      ssoEmail = testSsoEmail,
      associationMethod = testAssociationMethod
    )

    val encrypted: Seq[TSsoInfo] = Seq(info1).map(encryptor.encrypt)
    when(stratoSsoInfoForUserScanner.scan(testUserId)) thenReturn Stitch.value(encrypted)
    stitchAwait(reader.getSsoInfo(testUserId)) must contain theSameElementsAs (Seq(info1))
  }

}
