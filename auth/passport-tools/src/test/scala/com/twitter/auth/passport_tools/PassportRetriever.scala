package com.twitter.auth.passport_tools

import com.twitter.auth.context.AuthPasetoContextKey
import com.twitter.auth.pasetoheaders.encryption.ExtractedClaims
import com.twitter.auth.pasetoheaders.models.Passports.CustomerPassport
import com.twitter.auth.pasetoheaders.passport.PassportExtractor
import com.twitter.finagle.context.Contexts
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.logging.Logger
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatestplus.junit.JUnitRunner
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import com.twitter.auth.pasetoheaders.javahelpers.OptionConv._
import com.twitter.auth.pasetoheaders.models.Passports

@RunWith(classOf[JUnitRunner])
class PassportRetrieverTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with MockitoSugar
    with BeforeAndAfter {

  protected val statsReceiver = new InMemoryStatsReceiver
  protected val logger = Logger.get()

  before {
    statsReceiver.clear
  }

  // inject mocks
  val extractor = mock[PassportExtractor]
  val passportRetrieverMock =
    new PassportRetriever(
      logger = logger,
      stats = statsReceiver,
      passportExtractor = Some(extractor))

  private val testToken =
    "token"

  val testUserId = 1L
  val testAuthUserId = 2L
  val testGuestToken = 4L
  val testClientAppId = 3L
  val testSessionHash = "abc"
  val testPassportId = "100"

  private val pojoPassport = new CustomerPassport(
    testPassportId,
    Some(testUserId),
    Some(testAuthUserId),
    Some(testGuestToken),
    Some(testClientAppId),
    Some(testSessionHash),
    None,
    None,
    None)

  val pojoPassportClaims: ExtractedClaims[
    Passports.Passport
  ] = mock[ExtractedClaims[Passports.Passport]]

  test("test PassportRetriever within with paseto Passport") {
    Contexts.broadcast.let(AuthPasetoContextKey, testToken) {
      when(extractor.extractClaims(testToken, true)) thenReturn Some(pojoPassportClaims)
      when(pojoPassportClaims.getEnclosedEntity) thenReturn
        pojoPassport
          .asInstanceOf[Passports.Passport]
      passportRetrieverMock.passport mustBe Some(pojoPassport)
    }
  }

  test("test PassportRetriever without broadcasted data") {
    passportRetrieverMock.passport mustBe None
  }

}
