package com.twitter.auth.authentication.authenticator

import com.twitter.accounts.util.CryptoUtils
import com.twitter.auth.authenforcement.thriftscala.AuthenticatedUserPrincipal
import com.twitter.auth.authenforcement.thriftscala.Principal
import com.twitter.auth.authenforcement.thriftscala.ServiceClientPrincipal
import com.twitter.auth.authenforcement.thriftscala.SessionPrincipal
import com.twitter.auth.authenforcement.thriftscala.UserPrincipal
import com.twitter.auth.authentication.models.ActAsUserParams
import com.twitter.auth.authentication.models.RequestParams
import com.twitter.decider.Decider
import com.twitter.decider.Feature
import com.twitter.finagle.stats.InMemoryStatsReceiver
import org.junit.runner.RunWith
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.must.Matchers
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class AuthenticatorLibrarySpec
    extends AnyFunSuite
    with OneInstancePerTest
    with MockitoSugar
    with Matchers
    with BeforeAndAfterEach {

  import com.twitter.auth.authentication.CommonFixtures._

  private[this] var statsReceiver = new InMemoryStatsReceiver
  override def beforeEach(): Unit = {
    statsReceiver = new InMemoryStatsReceiver
  }

  case class DumbRequestParams(
    passportId: String,
    actAsUserParams: Option[ActAsUserParams])
      extends RequestParams

  private[this] val decider = mock[Decider]
  mkMockDeciders(
    Seq(
      ("use_new_authn_filter_oauth2session", true)
    ))

  def mkMockDeciders(featureNames: Seq[(String, Boolean)]) = {
    featureNames.foreach { featureName =>
      val mockFeature = mock[Feature]
      when(decider.feature(featureName._1)) thenReturn mockFeature
      when(mockFeature.isAvailable) thenReturn featureName._2
    }
  }

  test("create principals with user only") {
    val principalSet = AuthenticatorLibrary
      .createPrincipals(None, Some(UserId), None, None, None, None, None, statsReceiver)
    principalSet mustBe Set(
      Principal.UserPrincipal(UserPrincipal(UserId)),
      Principal.AuthenticatedUserPrincipal(AuthenticatedUserPrincipal(UserId))
    )
  }

  test("create principals with for app only request") {
    val principalSet = AuthenticatorLibrary
      .createPrincipals(
        None,
        Some(UserId),
        Some(ScopesAppOnly),
        None,
        None,
        None,
        None,
        statsReceiver)
    principalSet mustBe Set(
      Principal.UserPrincipal(UserPrincipal(UserId)),
      Principal.AuthenticatedUserPrincipal(AuthenticatedUserPrincipal(UserId)),
      Principal.SessionPrincipal(SessionPrincipal("", Some(ScopesAppOnly)))
    )
  }

  test("create principals for client credential request") {
    val principalSet = AuthenticatorLibrary
      .createPrincipals(
        None,
        None,
        Some(ScopesClientCredential),
        None,
        None,
        None,
        Some(ClientId),
        statsReceiver
      )
    principalSet mustBe Set(
      Principal.ServiceClientPrincipal(ServiceClientPrincipal(ClientId)),
      Principal.SessionPrincipal(SessionPrincipal("", Some(ScopesClientCredential)))
    )
  }

  test("create principals with for guest auth request") {
    val principalSet = AuthenticatorLibrary
      .createPrincipals(
        None,
        Some(UserId),
        Some(ScopesGuest),
        None,
        None,
        None,
        None,
        statsReceiver)
    principalSet mustBe Set(
      Principal.UserPrincipal(UserPrincipal(UserId)),
      Principal.AuthenticatedUserPrincipal(AuthenticatedUserPrincipal(UserId)),
      Principal.SessionPrincipal(SessionPrincipal("", Some(ScopesGuest)))
    )
  }

  test("create principals with token/scope only") {
    val principalSet = AuthenticatorLibrary
      .createPrincipals(
        Some(ValidOAuth1AccessToken.tokenHash),
        None,
        Some(ScopesOAuth1),
        None,
        None,
        None,
        None,
        statsReceiver)
    principalSet mustBe Set(
      Principal
        .SessionPrincipal(
          SessionPrincipal(
            CryptoUtils
              .hash(ValidOAuth1AccessToken.token),
            Some(ScopesOAuth1))))
  }

  test("create principals with all params") {
    val principalSet = AuthenticatorLibrary
      .createPrincipals(
        Some(ValidOAuth1AccessToken.tokenHash),
        Some(UserId),
        Some(ScopesOAuth1),
        None,
        None,
        None,
        None,
        statsReceiver)
    principalSet mustBe Set(
      Principal
        .SessionPrincipal(
          SessionPrincipal(
            CryptoUtils
              .hash(ValidOAuth1AccessToken.token),
            Some(ScopesOAuth1))),
      Principal.UserPrincipal(UserPrincipal(UserId)),
      Principal.AuthenticatedUserPrincipal(AuthenticatedUserPrincipal(UserId))
    )
  }
}
