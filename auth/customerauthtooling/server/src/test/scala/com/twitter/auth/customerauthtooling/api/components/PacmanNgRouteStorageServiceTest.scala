package com.twitter.auth.customerauthtooling.api.components

import com.twitter.cim.pacman.plugin.tferoute.models.CreateOrUpdateRouteRequest
import com.twitter.cim.pacman.plugin.tferoute.models.DeleteRouteRequest
import com.twitter.cim.pacman.plugin.tferoute.models.ProvisioningResponse
import com.twitter.cim.pacman.plugin.tferoute.models.SearchRoutesRequest
import com.twitter.cim.pacman.thriftscala.PacmanException
import com.twitter.cim.pacman.thriftscala.PacmanServiceErrorCode
import com.twitter.finagle.util.DefaultTimer.Implicit
import com.twitter.kite.clients.KiteClient
import com.twitter.kite.clients.routestore.Routestore
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
import com.twitter.conversions.DurationOps._
import com.twitter.tfe.core.routingng.RawRoutePackage
import com.twitter.tfe.core.routingng.RawRouteWithResourceInformation

@RunWith(classOf[JUnitRunner])
class PacmanNgRouteStorageServiceTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with MockitoSugar
    with BeforeAndAfter {

  private val kiteClient = mock[KiteClient]

  private val routeStore = mock[Routestore]

  private val pacmanNgRouteStorageService =
    PacmanNgRouteStorageService(kiteClient = kiteClient, maxTimeoutSeconds = 3.seconds)

  private val testProject = "project"

  private val testRoute = RawRoute(
    id = "routeId",
    vhosts = Set("api.twitter.com"),
    normalizedPath = "",
    requestMethod = "",
    userRoles = Set(),
    decider = None,
    rateLimitsMap = Map("default" -> Some(-1)),
    dataPermissions = Set(),
    featurePermissions = Set(),
    subscriptionPermissions = Set(),
    routeAuthTypes = Set(),
    routeFlags = Set(),
    cluster = "",
    destination = RawDestination(),
    lifeCycle = "production",
    timeoutMs = Some(5000),
    ldapOwners = Set("customerauthtooling", "authplatform"),
    description = Some(""),
    tags = Set(),
    requestCategory = "API",
    priority = 0,
    experimentBucket = None,
    experimentBuckets = Set(),
    uaTags = Set(),
    scopes = Set()
  )

  private val testRoutePackage = RawRoutePackage(
    route = testRoute,
    routeState = "production",
    editsApproved = true,
    markedForDeletion = false,
    readyForReview = false,
    author = "test",
    approvers = Set(),
    lastEdited = "",
    historicalMetadata = Set(),
    versionID = 1
  )

  before {
    when(kiteClient.routestore).thenReturn(routeStore)
  }

  test("test pacmanNgRouteStorageService with successful response") {
    val createRouteRequest = CreateOrUpdateRouteRequest(
      route = testRoute,
      selfManagedRoute = false,
      project = testProject,
      requestReason = "")
    when(routeStore.createRoute(createRouteRequest))
      .thenReturn(Future.value(ProvisioningResponse("ok", "routeId")))
    Await.result(
      pacmanNgRouteStorageService.createRoute(createRouteRequest)) mustBe ProvisioningResponse(
      "ok",
      "routeId")
  }

  test("test pacmanNgRouteStorageService with too slow response") {
    val createRouteRequest = CreateOrUpdateRouteRequest(
      route = testRoute,
      selfManagedRoute = false,
      project = testProject,
      requestReason = "")
    when(routeStore.createRoute(createRouteRequest))
      .thenReturn(Future.sleep(4.seconds).map(_ => ProvisioningResponse("ok", "routeId")))
    intercept[PacmanTimeoutException] {
      Await.result(pacmanNgRouteStorageService.createRoute(createRouteRequest))
    }
  }

  test("test pacmanNgRouteStorageService with slow response") {
    val createRouteRequest = CreateOrUpdateRouteRequest(
      route = testRoute,
      selfManagedRoute = false,
      project = testProject,
      requestReason = "")
    when(routeStore.createRoute(createRouteRequest))
      .thenReturn(Future.sleep(500.milliseconds).map(_ => ProvisioningResponse("ok", "routeId")))
    Await.result(
      pacmanNgRouteStorageService.createRoute(createRouteRequest)) mustBe ProvisioningResponse(
      "ok",
      "routeId")
  }

  test("test pacmanNgRouteStorageService with already exists exception") {
    val createRouteRequest = CreateOrUpdateRouteRequest(
      route = testRoute,
      selfManagedRoute = false,
      project = testProject,
      requestReason = "")
    when(routeStore.createRoute(createRouteRequest))
      .thenReturn(
        Future.exception(
          new PacmanException(
            errorCode = Some(PacmanServiceErrorCode.ProvisioningConflict),
            message = Some("the route already exists"))))
    Await.result(
      pacmanNgRouteStorageService.createRoute(createRouteRequest)) mustBe ProvisioningResponse(
      "skipped",
      "")
  }

  test("test pacmanNgRouteStorageService with unexpected end of input exception") {
    val createRouteRequest = CreateOrUpdateRouteRequest(
      route = testRoute,
      selfManagedRoute = false,
      project = testProject,
      requestReason = "")
    when(routeStore.createRoute(createRouteRequest))
      .thenReturn(
        Future.exception(
          new PacmanException(
            errorCode = Some(PacmanServiceErrorCode.InternalServerError),
            message = Some("Unexpected end-of-input exception"))))
    Await.result(
      pacmanNgRouteStorageService.createRoute(createRouteRequest)) mustBe ProvisioningResponse(
      "skipped",
      "")
  }

  test("test pacmanNgRouteStorageService with unknown internal server error exception") {
    val createRouteRequest = CreateOrUpdateRouteRequest(
      route = testRoute,
      selfManagedRoute = false,
      project = testProject,
      requestReason = "")
    when(routeStore.createRoute(createRouteRequest))
      .thenReturn(
        Future.exception(
          new PacmanException(
            errorCode = Some(PacmanServiceErrorCode.InternalServerError),
            message = Some("unknown exception"))))
    intercept[PacmanException] {
      Await.result(pacmanNgRouteStorageService.createRoute(createRouteRequest))
    }
  }

  test(
    "test pacmanNgRouteStorageService with already in flight exception (retry with exponental backoff)") {
    val createRouteRequest = CreateOrUpdateRouteRequest(
      route = testRoute,
      selfManagedRoute = false,
      project = testProject,
      requestReason = "")
    when(routeStore.createRoute(createRouteRequest))
      .thenReturn(
        Future.exception(
          new PacmanException(
            errorCode = Some(PacmanServiceErrorCode.ProvisioningConflict),
            message = Some("the route is already in flight"))))
      .thenReturn(
        Future.exception(
          new PacmanException(
            errorCode = Some(PacmanServiceErrorCode.ProvisioningConflict),
            message = Some("the route is already in flight"))))
      // return successful result on 3rd attempt
      .thenReturn(Future.value(ProvisioningResponse("ok", "routeId")))

    Await.result(
      pacmanNgRouteStorageService.createRoute(createRouteRequest)) mustBe ProvisioningResponse(
      "ok",
      "routeId")
  }

  test("test pacmanNgRouteStorageService with successful response, updateRoute") {
    val updateRouteRequest = CreateOrUpdateRouteRequest(
      route = testRoute,
      selfManagedRoute = false,
      project = testProject,
      requestReason = "")
    when(routeStore.updateRoute(updateRouteRequest))
      .thenReturn(Future.value(ProvisioningResponse("ok", "routeId")))
    Await.result(
      pacmanNgRouteStorageService.updateRoute(updateRouteRequest)) mustBe ProvisioningResponse(
      "ok",
      "routeId")
  }

  test("test pacmanNgRouteStorageService with successful response, deleteRoute") {
    val deleteRouteRequest = DeleteRouteRequest(
      routeID = testRoute.id,
      routeState = "OK",
      selfManagedRoute = false,
      project = testProject,
      deletionReason = "")
    when(routeStore.deleteRoute(deleteRouteRequest))
      .thenReturn(Future.value(ProvisioningResponse("ok", "routeId")))
    Await.result(
      pacmanNgRouteStorageService.deleteRoute(deleteRouteRequest)) mustBe ProvisioningResponse(
      "ok",
      "routeId")
  }

  test("test pacmanNgRouteStorageService with successful response, getRoutesByProjects") {
    when(
      routeStore.searchRoutesWithResourceInformation(
        SearchRoutesRequest(projects = Some(Seq(testProject)))))
      .thenReturn(
        Future.value(
          Seq(
            RawRouteWithResourceInformation(
              routePackage = testRoutePackage,
              projectName = Some(testProject)))))
    Await.result(pacmanNgRouteStorageService.getRoutesByProjects(Set(testProject))) mustBe Seq(
      RawRouteWithResourceInformation(
        routePackage = testRoutePackage,
        projectName = Some(testProject)))
  }

  test("test pacmanNgRouteStorageService with successful response, getRoutesByIds") {
    when(
      routeStore.searchRoutesWithResourceInformation(
        SearchRoutesRequest(routeIds = Some(Set(testRoute.id)))))
      .thenReturn(
        Future.value(
          Seq(
            RawRouteWithResourceInformation(
              routePackage = testRoutePackage,
              projectName = Some(testProject)))))
    Await.result(pacmanNgRouteStorageService.getRoutesByIds(Set(testRoute.id))) mustBe Seq(
      RawRouteWithResourceInformation(
        routePackage = testRoutePackage,
        projectName = Some(testProject)))
  }
}
