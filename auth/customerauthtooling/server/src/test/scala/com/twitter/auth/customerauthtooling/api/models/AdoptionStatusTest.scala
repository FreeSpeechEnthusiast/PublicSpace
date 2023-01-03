package com.twitter.auth.customerauthtooling.api.models

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AdoptionStatusTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter {

  private val emptyAdoptionStatus = AdoptionStatus()
  private val ngRouteAdoptionStatus = AdoptionStatus(
    foundInTfe = Some(AdoptionParameterValue[AdoptionParameter.FoundInTfe](underlying = true)),
    isInternalEndpoint =
      Some(AdoptionParameterValue[AdoptionParameter.IsInternalEndpoint](underlying = false)),
    isNgRoute = Some(AdoptionParameterValue[AdoptionParameter.IsNgRoute](underlying = true)),
    requiresAuth = Some(AdoptionParameterValue[AdoptionParameter.RequiresAuth](underlying = true)),
    isAppOnlyOrGuest =
      Some(AdoptionParameterValue[AdoptionParameter.IsAppOnlyOrGuest](underlying = false)),
    oauth1OrSession =
      Some(AdoptionParameterValue[AdoptionParameter.Oauth1OrSession](underlying = false)),
    alreadyAdoptedDps =
      Some(AdoptionParameterValue[AdoptionParameter.AlreadyAdoptedDps](underlying = false)),
  )
  private val internalRouteAdoptionStatus = ngRouteAdoptionStatus.copy(
    isInternalEndpoint =
      Some(AdoptionParameterValue[AdoptionParameter.IsInternalEndpoint](underlying = true))
  )
  private val nonNgRouteAdoptionStatus = ngRouteAdoptionStatus.copy(
    isNgRoute = Some(AdoptionParameterValue[AdoptionParameter.IsNgRoute](underlying = false))
  )
  private val nonNgRouteNonAuthAdoptionStatus = nonNgRouteAdoptionStatus.copy(
    requiresAuth = Some(AdoptionParameterValue[AdoptionParameter.RequiresAuth](underlying = false)))
  private val partiallyEmptyAdoptionStatus = ngRouteAdoptionStatus.copy(foundInTfe = None)

  test("test empty AdoptionStatus adoptionRequired") {
    emptyAdoptionStatus.adoptionRequired mustBe AdoptionRequirement.UnableToDetermine
  }

  test("test partially empty AdoptionStatus adoptionRequired") {
    partiallyEmptyAdoptionStatus.adoptionRequired mustBe AdoptionRequirement.UnableToDetermine
  }

  test("test ngroute endpoint AdoptionStatus adoptionRequired") {
    ngRouteAdoptionStatus.adoptionRequired mustBe AdoptionRequirement.Required
  }

  test("test non ngroute endpoint AdoptionStatus adoptionRequired") {
    nonNgRouteAdoptionStatus.adoptionRequired mustBe AdoptionRequirement.RequiredCustomerAuthAndNgRoutesAdoption
  }

  test("test non ngroute non auth endpoint AdoptionStatus adoptionRequired") {
    nonNgRouteNonAuthAdoptionStatus.adoptionRequired mustBe AdoptionRequirement.RequiredNgRoutesAdoptionOnly
  }

  test("test internal auth endpoint AdoptionStatus adoptionRequired") {
    internalRouteAdoptionStatus.adoptionRequired mustBe AdoptionRequirement.NotRequired
  }

}
