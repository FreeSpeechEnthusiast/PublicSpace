package com.twitter.auth.customerauthtooling.cli.commands

import com.twitter.auth.customerauthtooling.thriftscala.AppliedAction
import com.twitter.auth.customerauthtooling.thriftscala.ApplyRouteRequest
import com.twitter.auth.customerauthtooling.thriftscala.ApplyRouteResponse
import com.twitter.auth.customerauthtooling.thriftscala.CustomerAuthToolingService
import com.twitter.auth.customerauthtooling.thriftscala.PartialRouteInfo
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
import picocli.CommandLine.ParseResult

@RunWith(classOf[JUnitRunner])
class ApplyRouteCommandTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with MockitoSugar
    with BeforeAndAfter
    with CommandTestFixtures {

  private val customerAuthToolingServiceMock = mock[CustomerAuthToolingService.MethodPerEndpoint]
  private val command = spy(new ApplyRouteCommand())
  command.customerAuthToolingService = customerAuthToolingServiceMock

  private val commandParseResultMock = mock[ParseResult]

  before {
    doReturn(testIdentifyingOptions).when(command).idOrPath
    doReturn(testCommonOptions).when(command).commonOptions
    doReturn(testNotRequiredForUpdateOptions).when(command).commonButNotRequiredForUpdateOptions
  }

  test("test ApplyRouteCommand when route doesn't exist") {
    doReturn(
      Future.value(
        ApplyRouteResponse(
          status = true,
          routeDraft = Some(
            RouteDraft(
              uuid = "test",
              expectedRouteId = testRouteId,
              action = Some(AppliedAction.Insert))))))
      .when(customerAuthToolingServiceMock).applyRoute(ApplyRouteRequest(
        routeInfo = PartialRouteInfo(
          domains = Some(testDomains.get()),
          authTypes = Some(testAuthTypes.get()),
          path = Some(testEndpoint),
          cluster = Some(testCluster),
          method = testMethod,
          projectId = Some(testProject),
        ),
        automaticDecider = None
      ))
    doReturn(commandParseResultMock).when(command).commandParseResult
    doReturn(false).when(commandParseResultMock).hasMatchedOption("id")
    doReturn(true).when(commandParseResultMock).hasMatchedOption("project")
    doReturn(true).when(commandParseResultMock).hasMatchedOption("method")
    doReturn(true).when(commandParseResultMock).hasMatchedOption("path")
    doReturn(true).when(commandParseResultMock).hasMatchedOption("cluster")
    doReturn(true).when(commandParseResultMock).hasMatchedOption("domains")
    doReturn(true).when(commandParseResultMock).hasMatchedOption("auth_types")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("dps")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("userRoles")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("fps")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("sps")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("decider")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("priority")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("tags")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("experiment_buckets")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("ua_tags")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("scopes")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("rate_limit")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("timeout_ms")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("ldap_owners")
    val stream = new java.io.ByteArrayOutputStream()
    Console.withOut(stream) {
      command.call()
    }
    stream.toString must include("Route was created")
  }

  test("test ApplyRouteCommand when route doesn't exist with ID input") {
    // mock user input
    testIdentifyingOptions.id = Some(testRouteId)
    testIdentifyingOptions.pathDetails.method = testMethod
    testIdentifyingOptions.pathDetails.routePath = testEndpoint
    testIdentifyingOptions.pathDetails.routeCluster = testCluster
    testNotRequiredForUpdateOptions.project = testProject
    doReturn(
      Future.value(
        ApplyRouteResponse(
          status = true,
          routeDraft = Some(
            RouteDraft(
              uuid = "test",
              expectedRouteId = testRouteId,
              action = Some(AppliedAction.Insert))))))
      .when(customerAuthToolingServiceMock).applyRoute(ApplyRouteRequest(
        routeInfo = PartialRouteInfo(
          domains = Some(testDomains.get()),
          authTypes = Some(testAuthTypes.get()),
          path = Some(testEndpoint),
          cluster = Some(testCluster),
          method = testMethod,
          id = Some(testRouteId),
          projectId = Some(testProject),
        ),
        automaticDecider = None
      ))
    doReturn(commandParseResultMock).when(command).commandParseResult
    doReturn(true).when(commandParseResultMock).hasMatchedOption("id")
    doReturn(true).when(commandParseResultMock).hasMatchedOption("project")
    doReturn(true).when(commandParseResultMock).hasMatchedOption("method")
    doReturn(true).when(commandParseResultMock).hasMatchedOption("path")
    doReturn(true).when(commandParseResultMock).hasMatchedOption("cluster")
    doReturn(true).when(commandParseResultMock).hasMatchedOption("domains")
    doReturn(true).when(commandParseResultMock).hasMatchedOption("auth_types")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("dps")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("userRoles")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("fps")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("sps")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("decider")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("priority")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("tags")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("experiment_buckets")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("ua_tags")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("scopes")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("rate_limit")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("timeout_ms")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("ldap_owners")
    val stream = new java.io.ByteArrayOutputStream()
    Console.withOut(stream) {
      command.call()
    }
    stream.toString must include("Route was created")
  }

  test("test ApplyRouteCommand when route already exists") {
    doReturn(
      Future.value(
        ApplyRouteResponse(
          status = true,
          routeDraft = Some(
            RouteDraft(
              uuid = "test",
              expectedRouteId = testRouteId,
              action = Some(AppliedAction.Update))))))
      .when(customerAuthToolingServiceMock).applyRoute(ApplyRouteRequest(
        routeInfo = PartialRouteInfo(
          domains = Some(testDomains.get()),
          authTypes = Some(testAuthTypes.get()),
          path = Some(testEndpoint),
          cluster = Some(testCluster),
          method = testMethod,
          projectId = Some(testProject),
        ),
        automaticDecider = None
      ))
    doReturn(commandParseResultMock).when(command).commandParseResult
    doReturn(false).when(commandParseResultMock).hasMatchedOption("id")
    doReturn(true).when(commandParseResultMock).hasMatchedOption("project")
    doReturn(true).when(commandParseResultMock).hasMatchedOption("method")
    doReturn(true).when(commandParseResultMock).hasMatchedOption("path")
    doReturn(true).when(commandParseResultMock).hasMatchedOption("cluster")
    doReturn(true).when(commandParseResultMock).hasMatchedOption("domains")
    doReturn(true).when(commandParseResultMock).hasMatchedOption("auth_types")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("dps")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("userRoles")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("fps")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("sps")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("decider")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("priority")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("tags")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("experiment_buckets")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("ua_tags")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("scopes")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("rate_limit")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("timeout_ms")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("ldap_owners")
    val stream = new java.io.ByteArrayOutputStream()
    Console.withOut(stream) {
      command.call()
    }
    stream.toString must include("Route was updated")
  }

  test("test ApplyRouteCommand when route already exists with ID input") {
    // mock user input
    testIdentifyingOptions.id = Some(testRouteId)
    testIdentifyingOptions.pathDetails = null
    testNotRequiredForUpdateOptions.project = testProject
    doReturn(
      Future.value(
        ApplyRouteResponse(
          status = true,
          routeDraft = Some(
            RouteDraft(
              uuid = "test",
              expectedRouteId = testRouteId,
              action = Some(AppliedAction.Update))))))
      .when(customerAuthToolingServiceMock).applyRoute(
        ApplyRouteRequest(
          routeInfo = PartialRouteInfo(
            domains = Some(testDomains.get()),
            id = Some(testRouteId),
            projectId = Some(testProject),
          ),
          automaticDecider = None
        ))
    doReturn(commandParseResultMock).when(command).commandParseResult
    doReturn(true).when(commandParseResultMock).hasMatchedOption("id")
    doReturn(true).when(commandParseResultMock).hasMatchedOption("project")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("method")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("path")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("cluster")
    doReturn(true).when(commandParseResultMock).hasMatchedOption("domains")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("auth_types")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("dps")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("userRoles")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("fps")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("sps")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("decider")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("priority")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("tags")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("experiment_buckets")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("ua_tags")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("scopes")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("rate_limit")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("timeout_ms")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("ldap_owners")
    val stream = new java.io.ByteArrayOutputStream()
    Console.withOut(stream) {
      command.call()
    }
    stream.toString must include("Route was updated")
  }

  test(
    "test ApplyRouteCommand when route already exists (overwrite auth_types, decider and scopes)") {
    // mock user input
    command.commonButNotRequiredForUpdateOptions.authTypes = testAnotherAuthTypes
    testCommonOptions.decider = Some(testAnotherDeciderValue)
    testCommonOptions.scopes = Some(testAnotherScopes)
    testIdentifyingOptions.id = Some(testRouteId)
    testIdentifyingOptions.pathDetails = null
    testNotRequiredForUpdateOptions.project = testProject
    doReturn(
      Future.value(
        ApplyRouteResponse(
          status = true,
          routeDraft = Some(
            RouteDraft(
              uuid = "test",
              expectedRouteId = testRouteId,
              action = Some(AppliedAction.Update))))))
      .when(customerAuthToolingServiceMock).applyRoute(ApplyRouteRequest(
        routeInfo = PartialRouteInfo(
          domains = Some(testDomains.get()),
          id = Some(testRouteId),
          decider = Some(testAnotherDeciderValue),
          authTypes = Some(testAnotherAuthTypes.get()),
          scopes = Some(testAnotherScopes.get()),
          projectId = Some(testProject),
        ),
        automaticDecider = None
      ))
    doReturn(commandParseResultMock).when(command).commandParseResult
    doReturn(true).when(commandParseResultMock).hasMatchedOption("id")
    doReturn(true).when(commandParseResultMock).hasMatchedOption("project")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("method")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("path")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("cluster")
    doReturn(true).when(commandParseResultMock).hasMatchedOption("domains")
    doReturn(true).when(commandParseResultMock).hasMatchedOption("auth_types")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("dps")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("userRoles")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("fps")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("sps")
    doReturn(true).when(commandParseResultMock).hasMatchedOption("decider")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("priority")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("tags")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("experiment_buckets")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("ua_tags")
    doReturn(true).when(commandParseResultMock).hasMatchedOption("scopes")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("rate_limit")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("timeout_ms")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("ldap_owners")
    val stream = new java.io.ByteArrayOutputStream()
    Console.withOut(stream) {
      command.call()
    }
    stream.toString must include("Route was updated")
  }

  test("test ApplyRouteCommand with unsuccessful update result") {
    // mock user input
    testIdentifyingOptions.id = Some(testRouteId)
    testIdentifyingOptions.pathDetails = null
    testNotRequiredForUpdateOptions.project = testProject
    doReturn(Future.value(ApplyRouteResponse(status = true, routeDraft = None)))
      .when(customerAuthToolingServiceMock).applyRoute(
        ApplyRouteRequest(
          routeInfo = PartialRouteInfo(
            id = Some(testRouteId),
            projectId = Some(testProject),
          ),
          automaticDecider = None
        ))
    doReturn(commandParseResultMock).when(command).commandParseResult
    doReturn(true).when(commandParseResultMock).hasMatchedOption("id")
    doReturn(true).when(commandParseResultMock).hasMatchedOption("project")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("method")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("path")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("cluster")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("domains")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("auth_types")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("dps")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("userRoles")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("fps")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("sps")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("decider")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("priority")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("tags")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("experiment_buckets")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("ua_tags")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("scopes")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("rate_limit")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("timeout_ms")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("ldap_owners")
    val stream = new java.io.ByteArrayOutputStream()
    Console.withOut(stream) {
      command.call()
    }
    stream.toString must include("Unable to apply the route")
  }

  test("test ApplyRouteCommand with untouched update result") {
    // mock user input
    testIdentifyingOptions.id = Some(testRouteId)
    testIdentifyingOptions.pathDetails = null
    testNotRequiredForUpdateOptions.project = testProject
    doReturn(
      Future.value(
        ApplyRouteResponse(
          status = true,
          routeDraft = Some(
            RouteDraft(
              uuid = "test",
              expectedRouteId = testRouteId,
              action = Some(AppliedAction.Nothing))))))
      .when(customerAuthToolingServiceMock).applyRoute(
        ApplyRouteRequest(
          routeInfo = PartialRouteInfo(
            id = Some(testRouteId),
            projectId = Some(testProject),
          ),
          automaticDecider = None
        ))
    doReturn(commandParseResultMock).when(command).commandParseResult
    doReturn(true).when(commandParseResultMock).hasMatchedOption("id")
    doReturn(true).when(commandParseResultMock).hasMatchedOption("project")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("method")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("path")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("cluster")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("domains")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("auth_types")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("dps")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("userRoles")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("fps")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("sps")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("decider")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("priority")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("tags")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("experiment_buckets")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("ua_tags")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("scopes")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("rate_limit")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("timeout_ms")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("ldap_owners")
    val stream = new java.io.ByteArrayOutputStream()
    Console.withOut(stream) {
      command.call()
    }
    stream.toString must include("Route was untouched")
  }

}
