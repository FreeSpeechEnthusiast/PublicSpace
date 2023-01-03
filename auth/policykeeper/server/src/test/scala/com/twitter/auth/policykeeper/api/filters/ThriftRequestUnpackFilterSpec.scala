package com.twitter.auth.policykeeper.api.filters

import com.twitter.auth.policykeeper.api.context.LocalContext
import com.twitter.auth.policykeeper.thriftscala.PolicyKeeperService.VerifyPolicies
import com.twitter.auth.policykeeper.thriftscala.VerifyPoliciesRequest
import com.twitter.finagle.Service
import com.twitter.util.Await
import com.twitter.util.Future
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import com.twitter.scrooge.{Request => ThriftRequest}
import com.twitter.auth.authenforcement.thriftscala.AuthenticatedUserPrincipal
import com.twitter.auth.authenforcement.thriftscala.ClientApplicationPrincipal
import com.twitter.auth.authenforcement.thriftscala.EmployeePrincipal
import com.twitter.auth.authenforcement.thriftscala.GuestPrincipal
import com.twitter.auth.authenforcement.thriftscala.Passport
import com.twitter.auth.authenforcement.thriftscala.Principal
import com.twitter.auth.authenforcement.thriftscala.UserPrincipal
import com.twitter.auth.authenforcement.thriftscala.SessionPrincipal
import com.twitter.auth.pasetoheaders.thriftconversion.FromThriftPassport
import com.twitter.auth.passport_tools.PassportRetriever
import com.twitter.auth.passporttype.thriftscala.PassportType
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class ThriftRequestUnpackFilterSpec
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with MockitoSugar
    with BeforeAndAfter {

  val testUserId = Some(1L)
  val testAuthUserId = Some(2L)
  val testClientAppId = Some(3L)
  val testSessionHash = Some("abc")
  val testGuestToken = Some(4L)
  val testPassportId = "100"
  val testLdap = Some("jack")

  private val testPassport = Passport(
    passportId = testPassportId,
    principals = Set(
      Principal.UserPrincipal(UserPrincipal(testUserId.get)),
      Principal.AuthenticatedUserPrincipal(
        AuthenticatedUserPrincipal(testAuthUserId.get)
      ),
      Principal.ClientApplicationPrincipal(
        ClientApplicationPrincipal(testClientAppId.get)
      ),
      Principal.GuestPrincipal(GuestPrincipal(testGuestToken.get)),
      Principal.SessionPrincipal(SessionPrincipal(testSessionHash.get))
    ),
    dataPermissionDecisions = None,
    featurePermissionDecisions = None,
    subscriptionPermissionDecisions = None,
    passportType = Some(PassportType.UserRequest)
  )

  private val testEmployeePassport = testPassport.copy(principals =
    testPassport.principals ++ Set(Principal.EmployeePrincipal(EmployeePrincipal(testLdap))))

  private val passportRetriever = mock[PassportRetriever]

  private val filter =
    new ThriftRequestUnpackFilter(passportRetriever = passportRetriever)

  private val fakeThriftService = Service
    .mk[
      ThriftRequest[VerifyPolicies.Args],
      Tuple5[Option[Long], Option[Long], Option[Long], Option[String], Option[Long]]] { _ =>
      Future.value(
        (
          LocalContext.getUserId,
          LocalContext.getAuthenticatedUserId,
          LocalContext.getClientApplicationId,
          LocalContext.getSessionHash,
          LocalContext.getGuestToken
        )
      )
    }

  test("RequestUnpackFilter thrift test with customer passport") {
    when(passportRetriever.passport).thenReturn(FromThriftPassport.toPojo(testPassport))
    Await.result(
      filter
        .andThen(fakeThriftService).apply(request = ThriftRequest(
          VerifyPolicies.Args(VerifyPoliciesRequest(
            policyIds = Set()
          ))))) mustEqual (
      (
        testUserId,
        testAuthUserId,
        testClientAppId,
        testSessionHash,
        testGuestToken
      )
    )
  }

  test("RequestUnpackFilter thrift test with employee passport") {
    when(passportRetriever.passport).thenReturn(FromThriftPassport.toPojo(testEmployeePassport))
    Await.result(
      filter
        .andThen(fakeThriftService).apply(request = ThriftRequest(
          VerifyPolicies.Args(VerifyPoliciesRequest(
            policyIds = Set()
          ))))) mustEqual (
      (
        testUserId,
        testAuthUserId,
        testClientAppId,
        testSessionHash,
        None
      )
    )
  }

}
