package com.twitter.auth.customerauthtooling.api.components

import com.twitter.auth.policykeeper.api.logger.JsonLogger
import com.twitter.cim.pacman.plugin.tferoute.models.CreateOrUpdateRouteRequest
import com.twitter.cim.pacman.plugin.tferoute.models.ProvisioningResponse
import com.twitter.auth.customerauthtooling.api.models.PartialRouteInformation
import com.twitter.auth.customerauthtooling.api.models.RouteAuthType
import com.twitter.auth.customerauthtooling.api.models.RouteDraft
import com.twitter.auth.customerauthtooling.api.models.RouteInformation
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.logging.Logger
import com.twitter.tfe.core.routingng.RawDestination
import com.twitter.tfe.core.routingng.RawRoute
import com.twitter.util.Await
import com.twitter.util.Future
import org.junit.runner.RunWith
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import com.twitter.auth.authenticationtype.thriftscala.{AuthenticationType => TAuthenticationType}
import com.twitter.auth.customerauthtooling.api.models.BatchRouteDraft
import com.twitter.auth.customerauthtooling.api.models.RouteAppliedAction
import com.twitter.tfe.core.routingng.RawRoutePackage
import com.twitter.tfe.core.routingng.RawRouteWithResourceInformation

@RunWith(classOf[JUnitRunner])
class PacmanRouteDrafterServiceTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with MockitoSugar
    with BeforeAndAfter {

  private val routeInformationService = mock[RouteInformationService]

  private val pacmanNgRouteStorageService = mock[PacmanNgRouteStorageService]

  protected val statsReceiver = new InMemoryStatsReceiver
  protected val logger: Logger = Logger.get()
  protected val jsonLogger: JsonLogger = JsonLogger(logger)

  private val pacmanRouteDrafterService =
    PacmanRouteDrafterService(
      ngRouteStorage = pacmanNgRouteStorageService,
      routeInformationService = routeInformationService,
      statsReceiver = statsReceiver,
      logger = jsonLogger
    )

  private val testProject = "project"

  private val routeInfo =
    RouteInformation(
      path = "/endpoint",
      domains = Set("api.twitter.com"),
      isNgRoute = true,
      cluster = "test",
      projectId = Some(testProject))

  private val testRawRoute = RawRoute(
    id = "GET/endpoint->cluster:test",
    vhosts = Set("api.twitter.com"),
    normalizedPath = "/endpoint",
    requestMethod = "GET",
    userRoles = Set(),
    decider = None,
    rateLimitsMap = Map("default" -> Some(0)),
    dataPermissions = Set(),
    featurePermissions = Set(),
    subscriptionPermissions = Set(),
    routeAuthTypes = Set("UNKNOWN"),
    routeFlags = Set(),
    cluster = "test",
    destination = RawDestination(cluster = Some("test")),
    lifeCycle = "draft",
    timeoutMs = Some(5000),
    ldapOwners = Set("customerauthtooling", "authplatform"),
    description = Some(""),
    tags = Set("customerauthtools-generated"),
    requestCategory = "API",
    priority = 0,
    experimentBucket = None,
    experimentBuckets = Set(),
    uaTags = Set(),
    scopes = Set(),
  )

  private val testRoutePackage = RawRoutePackage(
    route = testRawRoute,
    routeState = "draft",
    editsApproved = true,
    markedForDeletion = false,
    readyForReview = false,
    author = "test",
    approvers = Set(),
    lastEdited = "",
    historicalMetadata = Set(),
    versionID = 1
  )

  private val anotherRouteInfo =
    RouteInformation(
      path = "/endpoint/1",
      domains = Set("api.twitter.com"),
      isNgRoute = true,
      cluster = "test",
      projectId = Some(testProject))

  private val testAnotherRawRoute = RawRoute(
    id = "GET/endpoint/1->cluster:test",
    vhosts = Set("api.twitter.com"),
    normalizedPath = "/endpoint/1",
    requestMethod = "GET",
    userRoles = Set(),
    decider = None,
    rateLimitsMap = Map("default" -> Some(0)),
    dataPermissions = Set(),
    featurePermissions = Set(),
    subscriptionPermissions = Set(),
    routeAuthTypes = Set("UNKNOWN"),
    routeFlags = Set(),
    cluster = "test",
    destination = RawDestination(cluster = Some("test")),
    lifeCycle = "draft",
    timeoutMs = Some(5000),
    ldapOwners = Set("customerauthtooling", "authplatform"),
    description = Some(""),
    tags = Set("customerauthtools-generated"),
    requestCategory = "API",
    priority = 0,
    experimentBucket = None,
    experimentBuckets = Set(),
    uaTags = Set(),
    scopes = Set()
  )

  private val testAnotherRoutePackage = RawRoutePackage(
    route = testAnotherRawRoute,
    routeState = "draft",
    editsApproved = true,
    markedForDeletion = false,
    readyForReview = false,
    author = "test",
    approvers = Set(),
    lastEdited = "",
    historicalMetadata = Set(),
    versionID = 1
  )

  before {}

  test("test pacmanRouteDrafterService draft create request") {
    when(
      pacmanNgRouteStorageService
        .createRoute(
          CreateOrUpdateRouteRequest(
            route = testRawRoute.copy(id = ""),
            project = testProject,
            requestReason = "",
            selfManagedRoute = false)))
      .thenReturn(Future.value(ProvisioningResponse(status = "ok", uuid = "1")))
    Await.result(
      pacmanRouteDrafterService
        .draftRoute(route = routeInfo, automaticDecider = false)) mustBe Some(
      RouteDraft(
        uuid = "1",
        expectedRouteId = routeInfo.expectedNgRouteId,
        action = Some(RouteAppliedAction.Insert)))
  }

  test("test pacmanRouteDrafterService draft create request with automatic decider") {
    when(
      pacmanNgRouteStorageService
        .createRoute(
          CreateOrUpdateRouteRequest(
            route = testRawRoute.copy(decider = Some("project-get-endpoint"), id = ""),
            project = testProject,
            requestReason = "",
            selfManagedRoute = false)))
      .thenReturn(Future.value(ProvisioningResponse(status = "ok", uuid = "1")))
    Await.result(
      pacmanRouteDrafterService
        .draftRoute(route = routeInfo, automaticDecider = true)) mustBe Some(
      RouteDraft(
        uuid = "1",
        expectedRouteId = routeInfo.expectedNgRouteId,
        action = Some(RouteAppliedAction.Insert)))
  }

  test("test pacmanRouteDrafterService draft update request") {
    when(
      pacmanNgRouteStorageService
        .updateRoute(
          CreateOrUpdateRouteRequest(
            route = testRawRoute.copy(id = routeInfo.expectedNgRouteId),
            project = testProject,
            requestReason = "",
            selfManagedRoute = false)))
      .thenReturn(Future.value(ProvisioningResponse(status = "ok", uuid = "1")))
    Await.result(
      pacmanRouteDrafterService
        .draftRoute(
          route = routeInfo.copy(id = Some(routeInfo.expectedNgRouteId)),
          automaticDecider = false,
          update = true)) mustBe Some(
      RouteDraft(
        uuid = "1",
        expectedRouteId = routeInfo.expectedNgRouteId,
        action = Some(RouteAppliedAction.Update)))
  }

  test("test pacmanRouteDrafterService apply request, incomplete information") {
    Await.result(
      pacmanRouteDrafterService
        .applyRoute(
          route = PartialRouteInformation(
            authTypes = Some(List(RouteAuthType.fromThrift(TAuthenticationType.Oauth1))),
          ),
          automaticDecider = false
        )) mustBe None
    statsReceiver.counters(
      List(
        pacmanRouteDrafterService.Scope,
        pacmanRouteDrafterService.ImportStoppedDuringValidation)) mustEqual 1L
  }

  test("test pacmanRouteDrafterService apply request, pacman failed during retrieval") {
    when(
      pacmanNgRouteStorageService
        .getRoutesByIds(routeIds = Set(routeInfo.expectedNgRouteId)))
      .thenReturn(Future.exception(new Exception))
    Await.result(
      pacmanRouteDrafterService
        .applyRoute(
          route = PartialRouteInformation(
            id = Some(routeInfo.expectedNgRouteId),
            authTypes = Some(List(RouteAuthType.fromThrift(TAuthenticationType.Oauth1))),
          ),
          automaticDecider = false
        )) mustBe None
    statsReceiver.counters(
      List(
        pacmanRouteDrafterService.Scope,
        pacmanRouteDrafterService.UnableToLoadRoutes)) mustEqual 1L
  }

  test("test pacmanRouteDrafterService apply request, route exists, ID provided") {
    when(
      pacmanNgRouteStorageService
        .getRoutesByIds(routeIds = Set(routeInfo.expectedNgRouteId)))
      .thenReturn(
        Future.value(
          Seq(
            RawRouteWithResourceInformation(
              routePackage = testRoutePackage,
              projectName = Some(testProject)))))
    when(
      pacmanNgRouteStorageService
        .updateRoute(
          CreateOrUpdateRouteRequest(
            route =
              testRawRoute.copy(id = routeInfo.expectedNgRouteId, routeAuthTypes = Set("OAUTH_1")),
            project = testProject,
            requestReason = "",
            selfManagedRoute = false)))
      .thenReturn(Future.value(ProvisioningResponse(status = "ok", uuid = "1")))
    Await.result(
      pacmanRouteDrafterService
        .applyRoute(
          route = PartialRouteInformation(
            id = Some(routeInfo.expectedNgRouteId),
            authTypes = Some(List(RouteAuthType.fromThrift(TAuthenticationType.Oauth1))),
          ),
          automaticDecider = false
        )) mustBe Some(
      RouteDraft(
        uuid = "1",
        expectedRouteId = routeInfo.expectedNgRouteId,
        action = Some(RouteAppliedAction.Update)))
    statsReceiver.counters(
      List(pacmanRouteDrafterService.Scope, pacmanRouteDrafterService.ImportFinished)) mustEqual 1L
  }

  test(
    "test pacmanRouteDrafterService apply request, route exists, path, method, and cluster provided") {
    when(
      pacmanNgRouteStorageService
        .getRoutesByIds(routeIds = Set(routeInfo.expectedNgRouteId)))
      .thenReturn(
        Future.value(
          Seq(
            RawRouteWithResourceInformation(
              routePackage = testRoutePackage,
              projectName = Some(testProject)))))
    when(
      pacmanNgRouteStorageService
        .updateRoute(
          CreateOrUpdateRouteRequest(
            route =
              testRawRoute.copy(id = routeInfo.expectedNgRouteId, routeAuthTypes = Set("OAUTH_1")),
            project = testProject,
            requestReason = "",
            selfManagedRoute = false)))
      .thenReturn(Future.value(ProvisioningResponse(status = "ok", uuid = "1")))
    Await.result(
      pacmanRouteDrafterService
        .applyRoute(
          route = PartialRouteInformation(
            path = Some(routeInfo.path),
            cluster = Some(routeInfo.cluster),
            method = Some(routeInfo.method),
            authTypes = Some(List(RouteAuthType.fromThrift(TAuthenticationType.Oauth1))),
          ),
          automaticDecider = false
        )) mustBe Some(
      RouteDraft(
        uuid = "1",
        expectedRouteId = routeInfo.expectedNgRouteId,
        action = Some(RouteAppliedAction.Update)))
    statsReceiver.counters(
      List(pacmanRouteDrafterService.Scope, pacmanRouteDrafterService.ImportFinished)) mustEqual 1L
  }

  test(
    "test pacmanRouteDrafterService apply request, route doesn't exist, path, some parameters are not provided") {
    when(
      pacmanNgRouteStorageService
        .getRoutesByIds(routeIds = Set(routeInfo.expectedNgRouteId)))
      .thenReturn(Future.value(Seq()))
    when(
      pacmanNgRouteStorageService
        .createRoute(
          CreateOrUpdateRouteRequest(
            route =
              testRawRoute.copy(id = routeInfo.expectedNgRouteId, routeAuthTypes = Set("OAUTH_1")),
            project = testProject,
            requestReason = "",
            selfManagedRoute = false)))
      .thenReturn(Future.value(ProvisioningResponse(status = "ok", uuid = "1")))
    Await.result(
      pacmanRouteDrafterService
        .applyRoute(
          route = PartialRouteInformation(
            path = Some(routeInfo.path),
            cluster = Some(routeInfo.cluster),
            method = Some(routeInfo.method),
            authTypes = Some(List(RouteAuthType.fromThrift(TAuthenticationType.Oauth1))),
          ),
          automaticDecider = false
        )) mustBe None
    statsReceiver.counters(
      List(pacmanRouteDrafterService.Scope, pacmanRouteDrafterService.ImportStopped)) mustEqual 1L
  }

  test(
    "test pacmanRouteDrafterService apply request, route doesn't exist, all parameters are provided") {
    when(
      pacmanNgRouteStorageService
        .getRoutesByIds(routeIds = Set(routeInfo.expectedNgRouteId)))
      .thenReturn(Future.value(Seq()))
    when(
      pacmanNgRouteStorageService
        .createRoute(
          CreateOrUpdateRouteRequest(
            route = testRawRoute.copy(id = "", routeAuthTypes = Set("OAUTH_1")),
            project = testProject,
            requestReason = "",
            selfManagedRoute = false
          )))
      .thenReturn(Future.value(ProvisioningResponse(status = "ok", uuid = "1")))
    Await.result(
      pacmanRouteDrafterService
        .applyRoute(
          route = PartialRouteInformation(
            projectId = Some(testProject),
            path = Some(routeInfo.path),
            cluster = Some(routeInfo.cluster),
            method = Some(routeInfo.method),
            domains = Some(routeInfo.domains),
            authTypes = Some(List(RouteAuthType.fromThrift(TAuthenticationType.Oauth1)))
          ),
          automaticDecider = false
        )) mustBe Some(
      RouteDraft(
        uuid = "1",
        expectedRouteId = routeInfo.expectedNgRouteId,
        action = Some(RouteAppliedAction.Insert)))
    statsReceiver.counters(
      List(pacmanRouteDrafterService.Scope, pacmanRouteDrafterService.ImportFinished)) mustEqual 1L
  }

  test(
    "test pacmanRouteDrafterService apply request, route doesn't exist, project is not provided") {
    when(
      pacmanNgRouteStorageService
        .getRoutesByIds(routeIds = Set(routeInfo.expectedNgRouteId)))
      .thenReturn(Future.value(Seq()))
    when(
      pacmanNgRouteStorageService
        .createRoute(
          CreateOrUpdateRouteRequest(
            route = testRawRoute.copy(id = "", routeAuthTypes = Set("OAUTH_1")),
            project = testProject,
            requestReason = "",
            selfManagedRoute = false
          )))
      .thenReturn(Future.value(ProvisioningResponse(status = "ok", uuid = "1")))
    Await.result(
      pacmanRouteDrafterService
        .applyRoute(
          route = PartialRouteInformation(
            path = Some(routeInfo.path),
            cluster = Some(routeInfo.cluster),
            method = Some(routeInfo.method),
            domains = Some(routeInfo.domains),
            authTypes = Some(List(RouteAuthType.fromThrift(TAuthenticationType.Oauth1)))
          ),
          automaticDecider = false
        )) mustBe None
    statsReceiver.counters(
      List(pacmanRouteDrafterService.Scope, pacmanRouteDrafterService.ImportStopped)) mustEqual 1L
  }

  test(
    "test pacmanRouteDrafterService batch apply request, incomplete information, ignoreInvalid = false") {
    Await.result(
      pacmanRouteDrafterService
        .applyRoutes(
          routes = Set(
            PartialRouteInformation(
              authTypes = Some(List(RouteAuthType.fromThrift(TAuthenticationType.Oauth1))),
            ),
            PartialRouteInformation(
              id = Some(anotherRouteInfo.expectedNgRouteId),
              authTypes = Some(List(RouteAuthType.fromThrift(TAuthenticationType.Oauth1))),
            )
          ),
          automaticDecider = false,
          ignoreErrors = false,
          ignoreInvalid = false
        )) mustBe BatchRouteDraft.Empty.copy(ignoredInvalid = 1, wasStopped = true)
    statsReceiver.counters(
      List(
        pacmanRouteDrafterService.Scope,
        pacmanRouteDrafterService.ImportStoppedDuringValidation)) mustEqual 1L
  }

  test(
    "test pacmanRouteDrafterService batch apply request, incomplete information, ignoreInvalid = true") {
    when(
      pacmanNgRouteStorageService
        .getRoutesByIds(routeIds = Set(anotherRouteInfo.expectedNgRouteId)))
      .thenReturn(
        Future.value(
          Seq(
            RawRouteWithResourceInformation(
              routePackage = testAnotherRoutePackage,
              projectName = Some(testProject)))))
    when(
      pacmanNgRouteStorageService
        .updateRoute(CreateOrUpdateRouteRequest(
          route = testAnotherRawRoute
            .copy(id = anotherRouteInfo.expectedNgRouteId, routeAuthTypes = Set("OAUTH_1")),
          project = testProject,
          requestReason = "",
          selfManagedRoute = false
        )))
      .thenReturn(Future.value(ProvisioningResponse(status = "ok", uuid = "2")))
    Await.result(
      pacmanRouteDrafterService
        .applyRoutes(
          routes = Set(
            PartialRouteInformation(
              authTypes = Some(List(RouteAuthType.fromThrift(TAuthenticationType.Oauth1))),
            ),
            PartialRouteInformation(
              id = Some(anotherRouteInfo.expectedNgRouteId),
              authTypes = Some(List(RouteAuthType.fromThrift(TAuthenticationType.Oauth1))),
            )
          ),
          automaticDecider = false,
          ignoreErrors = false,
          ignoreInvalid = true
        )) mustBe
      BatchRouteDraft(
        updated = 1,
        inserted = 0,
        ignoredInvalid = 1,
        ignoredDueToErrors = 0,
        unchanged = 0,
        routeDrafts = Some(
          Set(
            RouteDraft(
              uuid = "2",
              expectedRouteId = anotherRouteInfo.expectedNgRouteId,
              action = Some(RouteAppliedAction.Update)))),
        wasStopped = false,
        errors = Some(Seq()),
        warnings = Some(Seq()),
        messages = Some(Seq())
      )
    statsReceiver.counters(
      List(pacmanRouteDrafterService.Scope, pacmanRouteDrafterService.ImportFinished)) mustEqual 1L
  }

  test("test pacmanRouteDrafterService batch apply request, both routes exist") {
    when(
      pacmanNgRouteStorageService
        .getRoutesByIds(routeIds =
          Set(routeInfo.expectedNgRouteId, anotherRouteInfo.expectedNgRouteId)))
      .thenReturn(Future.value(Seq(
        RawRouteWithResourceInformation(
          routePackage = testRoutePackage,
          projectName = Some(testProject)),
        RawRouteWithResourceInformation(
          routePackage = testAnotherRoutePackage,
          projectName = Some(testProject))
      )))
    when(
      pacmanNgRouteStorageService
        .updateRoute(
          CreateOrUpdateRouteRequest(
            route = testRawRoute
              .copy(id = routeInfo.expectedNgRouteId, routeAuthTypes = Set("OAUTH_1")),
            project = testProject,
            requestReason = "",
            selfManagedRoute = false
          )))
      .thenReturn(Future.value(ProvisioningResponse(status = "ok", uuid = "1")))
    when(
      pacmanNgRouteStorageService
        .updateRoute(CreateOrUpdateRouteRequest(
          route = testAnotherRawRoute
            .copy(id = anotherRouteInfo.expectedNgRouteId, routeAuthTypes = Set("OAUTH_1")),
          project = testProject,
          requestReason = "",
          selfManagedRoute = false
        )))
      .thenReturn(Future.value(ProvisioningResponse(status = "ok", uuid = "2")))
    val result = Await.result(
      pacmanRouteDrafterService
        .applyRoutes(
          routes = Set(
            PartialRouteInformation(
              id = Some(routeInfo.expectedNgRouteId),
              authTypes = Some(List(RouteAuthType.fromThrift(TAuthenticationType.Oauth1))),
            ),
            PartialRouteInformation(
              id = Some(anotherRouteInfo.expectedNgRouteId),
              authTypes = Some(List(RouteAuthType.fromThrift(TAuthenticationType.Oauth1))),
            )
          ),
          automaticDecider = false,
          ignoreErrors = true,
          ignoreInvalid = true
        ))
    result mustBe
      BatchRouteDraft(
        updated = 2,
        inserted = 0,
        ignoredInvalid = 0,
        ignoredDueToErrors = 0,
        unchanged = 0,
        routeDrafts = Some(
          Set(
            RouteDraft(
              uuid = "1",
              expectedRouteId = routeInfo.expectedNgRouteId,
              action = Some(RouteAppliedAction.Update)),
            RouteDraft(
              uuid = "2",
              expectedRouteId = anotherRouteInfo.expectedNgRouteId,
              action = Some(RouteAppliedAction.Update))
          )),
        wasStopped = false,
        errors = Some(Seq()),
        warnings = Some(Seq()),
        messages = Some(Seq()),
      )
    statsReceiver.counters(
      List(pacmanRouteDrafterService.Scope, pacmanRouteDrafterService.ImportFinished)) mustEqual 1L
  }

  test("test pacmanRouteDrafterService batch apply request, one route exists and one doesn't") {
    when(
      pacmanNgRouteStorageService
        .getRoutesByIds(routeIds =
          Set(routeInfo.expectedNgRouteId, anotherRouteInfo.expectedNgRouteId)))
      .thenReturn(
        Future.value(
          Seq(
            RawRouteWithResourceInformation(
              routePackage = testAnotherRoutePackage,
              projectName = Some(testProject)))))
    when(
      pacmanNgRouteStorageService
        .createRoute(
          CreateOrUpdateRouteRequest(
            route = testRawRoute
              .copy(id = "", routeAuthTypes = Set("OAUTH_1")),
            project = testProject,
            requestReason = "",
            selfManagedRoute = false
          )))
      .thenReturn(Future.value(ProvisioningResponse(status = "ok", uuid = "1")))
    when(
      pacmanNgRouteStorageService
        .updateRoute(CreateOrUpdateRouteRequest(
          route = testAnotherRawRoute
            .copy(id = anotherRouteInfo.expectedNgRouteId, routeAuthTypes = Set("OAUTH_1")),
          project = testProject,
          requestReason = "",
          selfManagedRoute = false
        )))
      .thenReturn(Future.value(ProvisioningResponse(status = "ok", uuid = "2")))
    val result = Await.result(
      pacmanRouteDrafterService
        .applyRoutes(
          routes = Set(
            PartialRouteInformation(
              projectId = Some(testProject),
              path = Some(routeInfo.path),
              cluster = Some(routeInfo.cluster),
              method = Some(routeInfo.method),
              domains = Some(routeInfo.domains),
              authTypes = Some(List(RouteAuthType.fromThrift(TAuthenticationType.Oauth1))),
            ),
            PartialRouteInformation(
              id = Some(anotherRouteInfo.expectedNgRouteId),
              authTypes = Some(List(RouteAuthType.fromThrift(TAuthenticationType.Oauth1))),
            )
          ),
          automaticDecider = false,
          ignoreErrors = true,
          ignoreInvalid = true
        ))
    result mustBe
      BatchRouteDraft(
        updated = 1,
        inserted = 1,
        ignoredInvalid = 0,
        ignoredDueToErrors = 0,
        unchanged = 0,
        routeDrafts = Some(
          Set(
            RouteDraft(
              uuid = "1",
              expectedRouteId = routeInfo.expectedNgRouteId,
              action = Some(RouteAppliedAction.Insert)),
            RouteDraft(
              uuid = "2",
              expectedRouteId = anotherRouteInfo.expectedNgRouteId,
              action = Some(RouteAppliedAction.Update))
          )),
        wasStopped = false,
        errors = Some(Seq()),
        warnings = Some(Seq()),
        messages = Some(Seq()),
      )
    statsReceiver.counters(
      List(pacmanRouteDrafterService.Scope, pacmanRouteDrafterService.ImportFinished)) mustEqual 1L
  }

  test(
    "test pacmanRouteDrafterService batch apply request, one route exists and one doesn't, project is not specified in the input") {
    when(
      pacmanNgRouteStorageService
        .getRoutesByIds(routeIds =
          Set(routeInfo.expectedNgRouteId, anotherRouteInfo.expectedNgRouteId)))
      .thenReturn(
        Future.value(
          Seq(
            RawRouteWithResourceInformation(
              routePackage = testAnotherRoutePackage,
              projectName = Some(testProject)))))
    when(
      pacmanNgRouteStorageService
        .updateRoute(CreateOrUpdateRouteRequest(
          route = testAnotherRawRoute
            .copy(id = anotherRouteInfo.expectedNgRouteId, routeAuthTypes = Set("OAUTH_1")),
          project = testProject,
          requestReason = "",
          selfManagedRoute = false
        )))
      .thenReturn(Future.value(ProvisioningResponse(status = "ok", uuid = "2")))
    val result = Await.result(
      pacmanRouteDrafterService
        .applyRoutes(
          routes = Set(
            PartialRouteInformation(
              path = Some(routeInfo.path),
              cluster = Some(routeInfo.cluster),
              method = Some(routeInfo.method),
              domains = Some(routeInfo.domains),
              authTypes = Some(List(RouteAuthType.fromThrift(TAuthenticationType.Oauth1))),
            ),
            PartialRouteInformation(
              id = Some(anotherRouteInfo.expectedNgRouteId),
              authTypes = Some(List(RouteAuthType.fromThrift(TAuthenticationType.Oauth1))),
            )
          ),
          automaticDecider = false,
          ignoreErrors = true,
          ignoreInvalid = true
        ))
    result mustBe
      BatchRouteDraft(
        updated = 1,
        inserted = 0,
        ignoredInvalid = 1,
        ignoredDueToErrors = 0,
        unchanged = 0,
        routeDrafts = Some(
          Set(
            RouteDraft(
              uuid = "2",
              expectedRouteId = anotherRouteInfo.expectedNgRouteId,
              action = Some(RouteAppliedAction.Update))
          )),
        wasStopped = false,
        errors = Some(Seq()),
        warnings = Some(Seq(
          "Unable to make a new route (GET/endpoint->cluster:test). Some required parameters are missing: auth_types, domains, path, cluster or project")),
        messages = Some(Seq()),
      )
    statsReceiver.counters(
      List(pacmanRouteDrafterService.Scope, pacmanRouteDrafterService.ImportFinished)) mustEqual 1L
  }

  test(
    "test pacmanRouteDrafterService batch apply request, both routes exist and one is not changed") {
    when(
      pacmanNgRouteStorageService
        .getRoutesByIds(routeIds =
          Set(routeInfo.expectedNgRouteId, anotherRouteInfo.expectedNgRouteId)))
      .thenReturn(Future.value(Seq(
        RawRouteWithResourceInformation(
          routePackage = testRoutePackage,
          projectName = Some(testProject)),
        RawRouteWithResourceInformation(
          routePackage = testAnotherRoutePackage,
          projectName = Some(testProject))
      )))
    when(
      pacmanNgRouteStorageService
        .updateRoute(
          CreateOrUpdateRouteRequest(
            route = testRawRoute
              .copy(id = routeInfo.expectedNgRouteId, routeAuthTypes = Set("OAUTH_1")),
            project = testProject,
            requestReason = "",
            selfManagedRoute = false
          )))
      .thenReturn(Future.value(ProvisioningResponse(status = "ok", uuid = "1")))
    when(
      pacmanNgRouteStorageService
        .updateRoute(CreateOrUpdateRouteRequest(
          route = testAnotherRawRoute
            .copy(id = anotherRouteInfo.expectedNgRouteId, routeAuthTypes = Set("OAUTH_1")),
          project = testProject,
          requestReason = "",
          selfManagedRoute = false
        )))
      .thenReturn(Future.value(ProvisioningResponse(status = "ok", uuid = "2")))
    val result = Await.result(
      pacmanRouteDrafterService
        .applyRoutes(
          routes = Set(
            PartialRouteInformation(
              id = Some(routeInfo.expectedNgRouteId),
              authTypes = Some(List(RouteAuthType.fromThrift(TAuthenticationType.Oauth1))),
            ),
            PartialRouteInformation(
              id = Some(anotherRouteInfo.expectedNgRouteId),
              authTypes = Some(List(RouteAuthType.fromThrift(TAuthenticationType.Unknown))),
            )
          ),
          automaticDecider = false,
          ignoreErrors = true,
          ignoreInvalid = true
        ))
    result mustBe
      BatchRouteDraft(
        updated = 1,
        inserted = 0,
        ignoredInvalid = 0,
        ignoredDueToErrors = 0,
        unchanged = 1,
        routeDrafts = Some(
          Set(
            RouteDraft(
              uuid = "1",
              expectedRouteId = routeInfo.expectedNgRouteId,
              action = Some(RouteAppliedAction.Update)),
            RouteDraft(
              uuid = "n/a",
              expectedRouteId = anotherRouteInfo.expectedNgRouteId,
              action = Some(RouteAppliedAction.Nothing))
          )),
        wasStopped = false,
        errors = Some(Seq()),
        warnings = Some(Seq()),
        messages = Some(Seq()),
      )
    statsReceiver.counters(
      List(pacmanRouteDrafterService.Scope, pacmanRouteDrafterService.ImportFinished)) mustEqual 1L
  }
}
