package com.twitter.auth.customerauthtooling.cli.commands

import com.twitter.auth.customerauthtooling.thriftscala.AdoptionRequirement
import com.twitter.auth.customerauthtooling.thriftscala.AdoptionStatus
import com.twitter.auth.customerauthtooling.thriftscala.CheckAdoptionStatusRequest
import com.twitter.auth.customerauthtooling.thriftscala.CheckAdoptionStatusResponse
import com.twitter.auth.customerauthtooling.thriftscala.CustomerAuthToolingService
import com.twitter.auth.customerauthtooling.thriftscala.EndpointInfo
import com.twitter.auth.customerauthtooling.thriftscala.EndpointMetadata
import com.twitter.auth.customerauthtooling.thriftscala.RequestMethod
import com.twitter.util.Future
import org.junit.runner.RunWith
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AdoptionCheckCommandTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with MockitoSugar
    with BeforeAndAfter {

  private val command = new AdoptionCheckCommand()
  private val customerAuthToolingService = mock[CustomerAuthToolingService.MethodPerEndpoint]
  private val authToolServiceResult = CheckAdoptionStatusResponse(adoptionStatus = AdoptionStatus(
    requirement = AdoptionRequirement.RequiredCustomerAuthAndNgRoutesAdoption,
    foundInTfe = Some(true),
    isInternalEndpoint = Some(false),
    isNgRoute = Some(false),
    requiresAuth = Some(true),
    isAppOnlyOrGuest = Some(false),
    oauth1OrSession = Some(false),
    alreadyAdoptedDps = Some(false)
  ))
  command.setCustomerAuthToolingService(customerAuthToolingService)

  test("test AdoptionCheckCommand with default options") {
    // mock automatic results
    when(
      customerAuthToolingService
        .checkAdoptionStatus(request = CheckAdoptionStatusRequest(endpointInfo =
          EndpointInfo(url = "", method = Some(RequestMethod.Get), metadata = None))))
      .thenReturn(Future.value(authToolServiceResult))
    // mock overridden results
    when(
      customerAuthToolingService
        .checkAdoptionStatus(request = CheckAdoptionStatusRequest(endpointInfo = EndpointInfo(
          url = "",
          method = Some(RequestMethod.Get),
          metadata = Some(EndpointMetadata(
            suppliedDps = None,
            foundInTfeOverride = None,
            isInternalEndpointOverride = None,
            isNgRouteOverride = None,
            requiresAuthOverride = None,
            isAppOnlyOrGuestOverride = None,
            oauth1OrSessionOverride = None,
            alreadyAdoptedDpsOverride = None
          ))
        ))))
      .thenReturn(Future.value(authToolServiceResult))
    val stream = new java.io.ByteArrayOutputStream()
    Console.withOut(stream) {
      command.call()
    }
    stream.toString must include("Customer Auth Data Permissions and NgRoute adoption is required")
  }

}
