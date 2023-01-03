package com.twitter.auth.pasetoheaders.thriftconversion

import com.twitter.auth.authenforcement.thriftscala.AuthenticatedUserPrincipal
import com.twitter.auth.authenforcement.thriftscala.ClientApplicationPrincipal
import com.twitter.auth.authenforcement.thriftscala.DataPermissionDecisions
import com.twitter.auth.authenforcement.thriftscala.EmployeePrincipal
import com.twitter.auth.authenforcement.thriftscala.FeaturePermissionDecisions
import com.twitter.auth.authenforcement.thriftscala.GuestPrincipal
import com.twitter.auth.authenforcement.thriftscala.Passport
import com.twitter.auth.authenforcement.thriftscala.Principal
import com.twitter.auth.authenforcement.thriftscala.SubscriptionPermissionDecisions
import com.twitter.auth.authenforcement.thriftscala.UserPrincipal
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.matchers.must.Matchers
import com.twitter.auth.passporttype.thriftscala.PassportType
import org.junit.Assert.assertEquals

@RunWith(classOf[JUnitRunner])
class FromThriftPassportTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with BeforeAndAfter {

  val testUserId = 1
  val testClientAppId = 2
  val testAuthUserId = 3
  val testGuestToken = 4
  val testPassportId = "100"
  val testEmployeeLdap = "usr"

  private val guestPrincipals = Set(
    Principal.UserPrincipal(UserPrincipal(testUserId)),
    Principal.AuthenticatedUserPrincipal(
      AuthenticatedUserPrincipal(testAuthUserId)
    ),
    Principal.ClientApplicationPrincipal(
      ClientApplicationPrincipal(testClientAppId)
    ),
    Principal.GuestPrincipal(GuestPrincipal(testGuestToken))
  )

  private val guestPassport = Passport(
    passportId = testPassportId,
    principals = guestPrincipals.toSet,
    dataPermissionDecisions = None,
    featurePermissionDecisions = None,
    subscriptionPermissionDecisions = None,
    passportType = Some(PassportType.UserRequest)
  )

  private val completePassport = Passport(
    passportId = testPassportId,
    principals = guestPrincipals.toSet,
    dataPermissionDecisions = Some(
      DataPermissionDecisions(
        allowedDataPermissionIds = Some(Set(1L, 2L)),
        rejectedDataPermissionIds = Some(Set(3L, 4L))
      )),
    featurePermissionDecisions = Some(
      FeaturePermissionDecisions(
        allowedFeaturePermissions = Some(Set("a", "b")),
        rejectedFeaturePermissions = Some(Set("d", "e"))
      )
    ),
    subscriptionPermissionDecisions = Some(
      SubscriptionPermissionDecisions(
        allowedSubscriptionPermissions = Some(Set("f", "g")),
        rejectedSubscriptionPermissions = Some(Set("h", "i"))
      )
    ),
    passportType = Some(PassportType.UserRequest)
  )

  val employeePassport =
    completePassport.copy(principals = completePassport.principals ++
      Set(Principal.EmployeePrincipal(EmployeePrincipal(Some(testEmployeeLdap)))))

  test("test guest passport to pojo conversion") {
    FromThriftPassport.toPojo(guestPassport) match {
      case Some(pojo) =>
        assertEquals(
          "CustomerPassport ==> [pid= 100, userId=1, authenticatedUserId=3, guestId=4, clientApplicationId=2, sessionHash=None, DPD=None, FPS=None, SPS=None]",
          pojo.toString
        )
      case _ => fail()
    }
  }

  test("test complete passport to pojo conversion") {
    FromThriftPassport.toPojo(completePassport) match {
      case Some(pojo) =>
        assertEquals(
          "CustomerPassport ==> [pid= 100, userId=1, authenticatedUserId=3, guestId=4, clientApplicationId=2, sessionHash=None, DPD=DataPermissionDecisions ==> [id allowed: [1, 2], id rejected: [3, 4]], FPS=FeaturePermissionDecisions ==> [allowed: [a, b], rejected: [d, e]], SPS=SubscriptionPermissionDecisions ==> [allowed: [f, g], rejected: [h, i]]]",
          pojo.toString
        )
      case _ => fail()
    }
  }

  test("test employee passport to pojo conversion") {
    FromThriftPassport.toPojo(employeePassport) match {
      case Some(pojo) =>
        assertEquals(
          "EmployeePassport ==> [pid= 100, userId=1, authenticatedUserId=3, guestId=None, clientApplicationId=2, sessionHash=None, employeeId=usr, DPD=DataPermissionDecisions ==> [id allowed: [1, 2], id rejected: [3, 4]], FPS=FeaturePermissionDecisions ==> [allowed: [a, b], rejected: [d, e]], SPS=SubscriptionPermissionDecisions ==> [allowed: [f, g], rejected: [h, i]]]",
          pojo.toString
        )
      case _ => fail()
    }
  }

}
