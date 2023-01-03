package com.twitter.auth.customerauthtooling.cli.commands

import com.twitter.auth.authenticationtype.thriftscala.AuthenticationType
import com.twitter.auth.customerauthtooling.thriftscala.AppliedAction
import com.twitter.auth.customerauthtooling.thriftscala.ApplyRoutesRequest
import com.twitter.auth.customerauthtooling.thriftscala.ApplyRoutesResponse
import com.twitter.auth.customerauthtooling.thriftscala.BatchRouteDraft
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

@RunWith(classOf[JUnitRunner])
class BatchApplyRouteCommandTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with MockitoSugar
    with BeforeAndAfter
    with CommandTestFixtures {

  private val customerAuthToolingServiceMock = mock[CustomerAuthToolingService.MethodPerEndpoint]
  private val command = spy(new BatchApplyRouteCommand())
  command.customerAuthToolingService = customerAuthToolingServiceMock
  command.file = "auth/customerauthtooling/server/src/test/resources/routes.csv"

  test("test BatchApplyRouteCommand when file doesn't exist") {
    val stream = new java.io.ByteArrayOutputStream()
    command.file = "nonexisting.csv"
    Console.withOut(stream) {
      command.call()
    }
    stream.toString must include("Unable to load")
  }

  private val testRoute1 = PartialRouteInfo(
    projectId = Some(testProject),
    path = Some("/tfetestservice/customerauthtooling"),
    domains = Some(Set("api.twitter.com")),
    cluster = Some("tfe_test_service_mtls"),
    method = None,
    authTypes = Some(Set(AuthenticationType.Oauth2)),
    requiredDps = None,
    userRoles = None,
    routeFlags = None,
    featurePermissions = None,
    subscriptionPermissions = None,
    decider = None,
    priority = None,
    tags = None,
    experimentBuckets = None,
    uaTags = None,
    scopes = Some(Set.empty[String]),
    rateLimit = Some(0),
    timeoutMs = None,
    ldapOwners = Some(Set("customerauthtooling", "authplatform"))
  )

  private val testRoute2 = PartialRouteInfo(
    projectId = Some(testProject),
    path = Some("/tfetestservice/customerauthtooling/1"),
    domains = Some(Set("api.twitter.com")),
    cluster = Some("tfe_test_service_mtls"),
    method = None,
    authTypes = Some(Set(AuthenticationType.Oauth2)),
    requiredDps = None,
    userRoles = None,
    routeFlags = None,
    featurePermissions = None,
    subscriptionPermissions = None,
    decider = None,
    priority = None,
    tags = None,
    experimentBuckets = None,
    uaTags = None,
    scopes = Some(Set("users.read", "tweet.read")),
    rateLimit = Some(44),
    timeoutMs = None,
    ldapOwners = Some(Set("customerauthtooling", "authplatform"))
  )

  private val testRoute3 = PartialRouteInfo(
    projectId = Some(testProject),
    path = Some("/tfetestservice/customerauthtooling/2"),
    domains = Some(Set("api.twitter.com")),
    cluster = Some("tfe_test_service_mtls"),
    method = None,
    authTypes = Some(Set(AuthenticationType.Oauth2)),
    requiredDps = None,
    userRoles = None,
    routeFlags = None,
    featurePermissions = None,
    subscriptionPermissions = None,
    decider = None,
    priority = None,
    tags = None,
    experimentBuckets = None,
    uaTags = None,
    scopes = Some(Set("users.read")),
    rateLimit = Some(55),
    timeoutMs = None,
    ldapOwners = Some(Set("customerauthtooling", "authplatform"))
  )

  test("test BatchApplyRouteCommand when all routes don't exist") {
    // mock cli input
    command.ignoreErrors = true
    doReturn(
      Future.value(ApplyRoutesResponse(
        status = true,
        batchRouteDraft = Some(BatchRouteDraft(
          updated = 0,
          inserted = 3,
          ignoredInvalid = 0,
          ignoredDueToErrors = 0,
          unchanged = 0,
          routeDrafts = Some(Set(
            RouteDraft(
              uuid = "test",
              expectedRouteId =
                "GET/tfetestservice/customerauthtooling->cluster:tfe_test_service_mtls",
              action = Some(AppliedAction.Insert)),
            RouteDraft(
              uuid = "test1",
              expectedRouteId =
                "GET/tfetestservice/customerauthtooling/1->cluster:tfe_test_service_mtls",
              action = Some(AppliedAction.Insert)),
            RouteDraft(
              uuid = "test2",
              expectedRouteId =
                "GET/tfetestservice/customerauthtooling/2->cluster:tfe_test_service_mtls",
              action = Some(AppliedAction.Insert))
          )),
          wasStopped = false,
          errors = Some(Seq()),
          warnings = Some(Seq()),
          messages = Some(Seq()),
        ))
      )))
      .when(customerAuthToolingServiceMock).applyRoutes(
        ApplyRoutesRequest(
          routes = Set(
            testRoute1,
            testRoute2,
            testRoute3
          ),
          automaticDecider = None,
          ignoreInvalid = Some(true),
          ignoreErrors = Some(true)
        ))
    val stream = new java.io.ByteArrayOutputStream()
    Console.withOut(stream) {
      command.call()
    }
    stream.toString must include("Route was created")
    stream.toString mustNot include("Route was updated")
    stream.toString must include("Created: 3")
    stream.toString must include("Updated: 0")
    stream.toString must include("Unchanged: 0")
  }

  test("test BatchApplyRouteCommand when all routes exist") {
    // mock cli input
    command.ignoreErrors = true
    doReturn(
      Future.value(ApplyRoutesResponse(
        status = true,
        batchRouteDraft = Some(BatchRouteDraft(
          updated = 3,
          inserted = 0,
          ignoredInvalid = 0,
          ignoredDueToErrors = 0,
          unchanged = 0,
          routeDrafts = Some(Set(
            RouteDraft(
              uuid = "test",
              expectedRouteId =
                "GET/tfetestservice/customerauthtooling->cluster:tfe_test_service_mtls",
              action = Some(AppliedAction.Update)),
            RouteDraft(
              uuid = "test1",
              expectedRouteId =
                "GET/tfetestservice/customerauthtooling/1->cluster:tfe_test_service_mtls",
              action = Some(AppliedAction.Update)),
            RouteDraft(
              uuid = "test2",
              expectedRouteId =
                "GET/tfetestservice/customerauthtooling/2->cluster:tfe_test_service_mtls",
              action = Some(AppliedAction.Update))
          )),
          wasStopped = false,
          errors = Some(Seq()),
          warnings = Some(Seq()),
          messages = Some(Seq()),
        ))
      )))
      .when(customerAuthToolingServiceMock).applyRoutes(
        ApplyRoutesRequest(
          routes = Set(
            testRoute1,
            testRoute2,
            testRoute3
          ),
          automaticDecider = None,
          ignoreInvalid = Some(true),
          ignoreErrors = Some(true)
        ))
    val stream = new java.io.ByteArrayOutputStream()
    Console.withOut(stream) {
      command.call()
    }
    stream.toString mustNot include("Route was created")
    stream.toString must include("Route was updated")
    stream.toString must include("Created: 0")
    stream.toString must include("Updated: 3")
    stream.toString must include("Unchanged: 0")
  }

  test("test BatchApplyRouteCommand when all some routes exist") {
    // mock cli input
    command.ignoreErrors = true
    doReturn(
      Future.value(ApplyRoutesResponse(
        status = true,
        batchRouteDraft = Some(BatchRouteDraft(
          updated = 2,
          inserted = 1,
          ignoredInvalid = 0,
          ignoredDueToErrors = 0,
          unchanged = 0,
          routeDrafts = Some(Set(
            RouteDraft(
              uuid = "test",
              expectedRouteId =
                "GET/tfetestservice/customerauthtooling->cluster:tfe_test_service_mtls",
              action = Some(AppliedAction.Update)),
            RouteDraft(
              uuid = "test1",
              expectedRouteId =
                "GET/tfetestservice/customerauthtooling/1->cluster:tfe_test_service_mtls",
              action = Some(AppliedAction.Update)),
            RouteDraft(
              uuid = "test2",
              expectedRouteId =
                "GET/tfetestservice/customerauthtooling/2->cluster:tfe_test_service_mtls",
              action = Some(AppliedAction.Insert))
          )),
          wasStopped = false,
          errors = Some(Seq()),
          warnings = Some(Seq()),
          messages = Some(Seq()),
        ))
      )))
      .when(customerAuthToolingServiceMock).applyRoutes(
        ApplyRoutesRequest(
          routes = Set(
            testRoute1,
            testRoute2,
            testRoute3
          ),
          automaticDecider = None,
          ignoreInvalid = Some(true),
          ignoreErrors = Some(true)
        ))
    val stream = new java.io.ByteArrayOutputStream()
    Console.withOut(stream) {
      command.call()
    }
    stream.toString must include("Route was created")
    stream.toString must include("Route was updated")
    stream.toString must include("Created: 1")
    stream.toString must include("Updated: 2")
    stream.toString must include("Unchanged: 0")
  }

  test("test BatchApplyRouteCommand when all routes exist and project is not specified") {
    // mock cli input
    command.file = "auth/customerauthtooling/server/src/test/resources/routes_no_proj.csv"
    command.ignoreErrors = true
    doReturn(
      Future.value(ApplyRoutesResponse(
        status = true,
        batchRouteDraft = Some(BatchRouteDraft(
          updated = 2,
          inserted = 0,
          ignoredInvalid = 0,
          ignoredDueToErrors = 0,
          unchanged = 0,
          routeDrafts = Some(Set(
            RouteDraft(
              uuid = "test",
              expectedRouteId =
                "GET/tfetestservice/customerauthtooling->cluster:tfe_test_service_mtls",
              action = Some(AppliedAction.Update)),
            RouteDraft(
              uuid = "test1",
              expectedRouteId =
                "GET/tfetestservice/customerauthtooling/1->cluster:tfe_test_service_mtls",
              action = Some(AppliedAction.Update))
          )),
          wasStopped = false,
          errors = Some(Seq()),
          warnings = Some(Seq()),
          messages = Some(Seq()),
        ))
      )))
      .when(customerAuthToolingServiceMock).applyRoutes(ApplyRoutesRequest(
        routes = Set(
          testRoute1.copy(scopes = None, projectId = None),
          testRoute2.copy(scopes = None, projectId = None)
        ),
        automaticDecider = None,
        ignoreInvalid = Some(true),
        ignoreErrors = Some(true)
      ))
    val stream = new java.io.ByteArrayOutputStream()
    Console.withOut(stream) {
      command.call()
    }
    stream.toString mustNot include("Route was created")
    stream.toString must include("Route was updated")
    stream.toString must include("Created: 0")
    stream.toString must include("Updated: 2")
    stream.toString must include("Unchanged: 0")
  }

  test("test BatchApplyRouteCommand when some routes aren't exist and project is not specified") {
    // mock cli input
    command.file = "auth/customerauthtooling/server/src/test/resources/routes_no_proj.csv"
    command.ignoreErrors = true
    doReturn(
      Future.value(ApplyRoutesResponse(
        status = true,
        batchRouteDraft = Some(BatchRouteDraft(
          updated = 1,
          inserted = 0,
          ignoredInvalid = 1,
          ignoredDueToErrors = 0,
          unchanged = 0,
          routeDrafts = Some(
            Set(
              RouteDraft(
                uuid = "test1",
                expectedRouteId =
                  "GET/tfetestservice/customerauthtooling/1->cluster:tfe_test_service_mtls",
                action = Some(AppliedAction.Update))
            )),
          wasStopped = false,
          errors = Some(Seq()),
          warnings = Some(Seq(
            "Unable to make a new route (GET/tfetestservice/customerauthtooling->cluster:tfe_test_service_mtls). Some required parameters are missing: auth_types, domains, path, cluster or project")),
          messages = Some(Seq()),
        ))
      )))
      .when(customerAuthToolingServiceMock).applyRoutes(ApplyRoutesRequest(
        routes = Set(
          testRoute1.copy(scopes = None, projectId = None),
          testRoute2.copy(scopes = None, projectId = None)
        ),
        automaticDecider = None,
        ignoreInvalid = Some(true),
        ignoreErrors = Some(true)
      ))
    val stream = new java.io.ByteArrayOutputStream()
    Console.withOut(stream) {
      command.call()
    }
    stream.toString mustNot include("Route was created")
    stream.toString must include("Route was updated")
    stream.toString must include("Created: 0")
    stream.toString must include("Updated: 1")
    stream.toString must include("Unchanged: 0")
  }
}
