package com.twitter.auth.customerauthtooling.cli.commands

import com.twitter.auth.customerauthtooling.thriftscala.CustomerAuthToolingService
import com.twitter.auth.customerauthtooling.thriftscala.DraftRouteRequest
import com.twitter.auth.customerauthtooling.thriftscala.DraftRouteResponse
import com.twitter.auth.customerauthtooling.thriftscala.GetRoutesByRouteIdsResponse
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
class UpdateRouteCommandTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with MockitoSugar
    with BeforeAndAfter
    with CommandTestFixtures {

  private val customerAuthToolingServiceMock = mock[CustomerAuthToolingService.MethodPerEndpoint]
  private val command = spy(new UpdateRouteCommand())
  command.customerAuthToolingService = customerAuthToolingServiceMock

  private val commandParseResultMock = mock[ParseResult]

  before {
    doReturn(testIdentifyingOptions).when(command).idOrPath
    doReturn(testCommonOptions).when(command).commonOptions
    doReturn(testNotRequiredForUpdateOptions).when(command).commonButNotRequiredForUpdateOptions
  }

  test("test UpdateRouteCommand when route doesn't exist") {
    doReturn(
      Future.value(
        GetRoutesByRouteIdsResponse(
          status = true,
          routes = None
        )))
      .when(command).getExistingRoute(
        id = null,
        pathOpt = Some(testEndpoint),
        clusterOpt = Some(testCluster),
        methodOpt = testMethod)
    val stream = new java.io.ByteArrayOutputStream()
    Console.withOut(stream) {
      command.call()
    }
    stream.toString must include("Route doesn't exist")
  }

  test("test UpdateRouteCommand when route already exists") {
    doReturn(
      Future.value(
        GetRoutesByRouteIdsResponse(
          status = true,
          routes = Some(Set(testRoute))
        )))
      .when(command).getExistingRoute(
        id = null,
        pathOpt = Some(testEndpoint),
        clusterOpt = Some(testCluster),
        methodOpt = testMethod)
    doReturn(
      Future.value(
        DraftRouteResponse(
          status = true,
          routeDraft = Some(RouteDraft(uuid = "test", expectedRouteId = testRouteId)))))
      .when(customerAuthToolingServiceMock).draftRoute(request = DraftRouteRequest(
        routeInfo = testRoute,
        automaticDecider = None,
        update = Some(true)
      ))
    doReturn(commandParseResultMock).when(command).commandParseResult
    doReturn(false).when(commandParseResultMock).hasMatchedOption("project")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("method")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("path")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("cluster")
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
    stream.toString must include("Route is updated")
  }

  test("test UpdateRouteCommand when route already exists with ID input") {
    // mock user input
    testIdentifyingOptions.id = Some(testRouteId)
    testIdentifyingOptions.pathDetails = null
    doReturn(
      Future.value(
        GetRoutesByRouteIdsResponse(
          status = true,
          routes = Some(Set(testRoute))
        )))
      .when(command).getExistingRoute(
        id = Some(testRouteId),
        pathOpt = None,
        clusterOpt = None,
        methodOpt = None)
    doReturn(
      Future.value(
        DraftRouteResponse(
          status = true,
          routeDraft = Some(RouteDraft(uuid = "test", expectedRouteId = testRouteId)))))
      .when(customerAuthToolingServiceMock).draftRoute(request = DraftRouteRequest(
        routeInfo = testRoute,
        automaticDecider = None,
        update = Some(true)
      ))
    doReturn(commandParseResultMock).when(command).commandParseResult
    doReturn(false).when(commandParseResultMock).hasMatchedOption("project")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("method")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("path")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("cluster")
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
    stream.toString must include("Route is updated")
  }

  test(
    "test UpdateRouteCommand when route already exists (overwrite auth_types, decider and scopes, no auth_types and domains supplied)") {
    doReturn(
      Future.value(
        GetRoutesByRouteIdsResponse(
          status = true,
          routes = Some(Set(testRoute))
        )))
      .when(command).getExistingRoute(
        id = null,
        pathOpt = Some(testEndpoint),
        clusterOpt = Some(testCluster),
        methodOpt = testMethod)
    doReturn(commandParseResultMock).when(command).commandParseResult
    // emulate that auth_types and decider are specified by user
    doReturn(false).when(commandParseResultMock).hasMatchedOption("project")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("method")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("path")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("cluster")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("domains")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("auth_types")
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
    // mock user input
    command.commonButNotRequiredForUpdateOptions.authTypes = null
    command.commonButNotRequiredForUpdateOptions.domains = null
    testCommonOptions.decider = Some(testAnotherDeciderValue)
    testCommonOptions.scopes = Some(testAnotherScopes)
    doReturn(
      Future.value(
        DraftRouteResponse(
          status = true,
          routeDraft = Some(RouteDraft(uuid = "test", expectedRouteId = testRouteId)))))
      .when(customerAuthToolingServiceMock).draftRoute(request = DraftRouteRequest(
        routeInfo = testRoute.copy(
          authTypes = Some(testAuthTypes.get()),
          decider = Some(testAnotherDeciderValue),
          scopes = Some(testAnotherScopes.get())
        ),
        automaticDecider = None,
        update = Some(true)
      ))
    val stream = new java.io.ByteArrayOutputStream()
    Console.withOut(stream) {
      command.call()
    }
    stream.toString must include("Route is updated")
  }

  test(
    "test UpdateRouteCommand when route already exists (overwrite auth_types, decider and scopes)") {
    doReturn(
      Future.value(
        GetRoutesByRouteIdsResponse(
          status = true,
          routes = Some(Set(testRoute))
        )))
      .when(command).getExistingRoute(
        id = null,
        pathOpt = Some(testEndpoint),
        clusterOpt = Some(testCluster),
        methodOpt = testMethod)
    doReturn(commandParseResultMock).when(command).commandParseResult
    // emulate that auth_types and decider are specified by user
    doReturn(false).when(commandParseResultMock).hasMatchedOption("project")
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
    // mock user input
    command.commonButNotRequiredForUpdateOptions.authTypes = testAnotherAuthTypes
    testCommonOptions.decider = Some(testAnotherDeciderValue)
    testCommonOptions.scopes = Some(testAnotherScopes)
    doReturn(
      Future.value(
        DraftRouteResponse(
          status = true,
          routeDraft = Some(RouteDraft(uuid = "test", expectedRouteId = testRouteId)))))
      .when(customerAuthToolingServiceMock).draftRoute(request = DraftRouteRequest(
        routeInfo = testRoute.copy(
          authTypes = Some(testAnotherAuthTypes.get()),
          decider = Some(testAnotherDeciderValue),
          scopes = Some(testAnotherScopes.get())
        ),
        automaticDecider = None,
        update = Some(true)
      ))
    val stream = new java.io.ByteArrayOutputStream()
    Console.withOut(stream) {
      command.call()
    }
    stream.toString must include("Route is updated")
  }

  test("test UpdateRouteCommand with unsuccessful update result") {
    doReturn(
      Future.value(
        GetRoutesByRouteIdsResponse(
          status = true,
          routes = Some(Set(testRoute))
        )))
      .when(command).getExistingRoute(
        id = null,
        pathOpt = Some(testEndpoint),
        clusterOpt = Some(testCluster),
        methodOpt = testMethod)
    doReturn(Future.value(DraftRouteResponse(status = false, routeDraft = None)))
      .when(customerAuthToolingServiceMock).draftRoute(request = DraftRouteRequest(
        routeInfo = testRoute,
        automaticDecider = None,
        update = Some(true)
      ))
    doReturn(commandParseResultMock).when(command).commandParseResult
    doReturn(false).when(commandParseResultMock).hasMatchedOption("project")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("method")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("path")
    doReturn(false).when(commandParseResultMock).hasMatchedOption("cluster")
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
    stream.toString must include("Unable to update")
  }

}
