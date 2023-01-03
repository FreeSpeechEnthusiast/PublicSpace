package com.twitter.auth.customerauthtooling.cli.commands

import com.twitter.auth.customerauthtooling.thriftscala.DraftRouteResponse
import com.twitter.auth.customerauthtooling.thriftscala.RouteDraft
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
class NewRouteCommandTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with MockitoSugar
    with BeforeAndAfter
    with CommandTestFixtures {

  private val command = spy(new NewRouteCommand())

  before {
    doReturn(testRequiredOptions).when(command).commonRequiredOptions
    doReturn(testPathDetails).when(command).identifyingOptions
  }

  test("test NewRouteCommand with success result") {
    doReturn(
      Future.value(
        DraftRouteResponse(
          status = true,
          routeDraft = Some(RouteDraft(uuid = "test", expectedRouteId = testRouteId)))))
      .when(command).draftRouteResult(
        project = testProject,
        domains = testDomains,
        authTypes = testAuthTypes,
        path = testEndpoint,
        cluster = testCluster,
        method = testMethod)
    val stream = new java.io.ByteArrayOutputStream()
    Console.withOut(stream) {
      command.call()
    }
    stream.toString must include("Route is created")
  }

  test("test NewRouteCommand with unsuccessful result") {
    doReturn(Future.value(DraftRouteResponse(status = false, routeDraft = None)))
      .when(command).draftRouteResult(
        project = testProject,
        domains = testDomains,
        authTypes = testAuthTypes,
        path = testEndpoint,
        cluster = testCluster,
        method = testMethod)
    val stream = new java.io.ByteArrayOutputStream()
    Console.withOut(stream) {
      command.call()
    }
    stream.toString must include("Unable to draft")
  }

}
