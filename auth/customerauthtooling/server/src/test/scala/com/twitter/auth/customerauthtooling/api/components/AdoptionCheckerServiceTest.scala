package com.twitter.auth.customerauthtooling.api.components

import com.twitter.auth.customerauthtooling.api.models.AdoptionParameter
import com.twitter.auth.customerauthtooling.api.models.AdoptionParameterValue
import com.twitter.auth.customerauthtooling.api.models.AdoptionStatus
import com.twitter.auth.customerauthtooling.api.models.EndpointInfo
import com.twitter.auth.customerauthtooling.api.models.EndpointMetadata
import com.twitter.util.Await
import com.twitter.util.Future
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.when
import com.twitter.conversions.DurationOps._
import com.twitter.auth.customerauthtooling.api.components.parameterresolver.AdoptionParameterResolverInterface
import com.twitter.finagle.util.DefaultTimer.Implicit

@RunWith(classOf[JUnitRunner])
class AdoptionCheckerServiceTest
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with MockitoSugar
    with BeforeAndAfter {

  private val foundInTfeResolver =
    mock[AdoptionParameterResolverInterface[AdoptionParameter.FoundInTfe]]
  private val isInternalEndpointResolver =
    mock[AdoptionParameterResolverInterface[AdoptionParameter.IsInternalEndpoint]]
  private val isNgRouteResolver =
    mock[AdoptionParameterResolverInterface[AdoptionParameter.IsNgRoute]]
  private val requiresAuthResolver =
    mock[AdoptionParameterResolverInterface[AdoptionParameter.RequiresAuth]]
  private val isAppOnlyOrGuestResolver =
    mock[AdoptionParameterResolverInterface[AdoptionParameter.IsAppOnlyOrGuest]]
  private val oauth1OrSessionResolver =
    mock[AdoptionParameterResolverInterface[AdoptionParameter.Oauth1OrSession]]
  private val alreadyAdoptedDpsResolver =
    mock[AdoptionParameterResolverInterface[AdoptionParameter.AlreadyAdoptedDps]]

  private val adoptioncheckerservice =
    AdoptionCheckerService(
      foundInTfeResolver = foundInTfeResolver,
      isInternalEndpointResolver = isInternalEndpointResolver,
      isNgRouteResolver = isNgRouteResolver,
      requiresAuthResolver = requiresAuthResolver,
      isAppOnlyOrGuestResolver = isAppOnlyOrGuestResolver,
      oauth1OrSessionResolver = oauth1OrSessionResolver,
      alreadyAdoptedDpsResolver = alreadyAdoptedDpsResolver
    )

  private val testUrl = "/endpoint"

  private val testMetadata = EndpointMetadata()

  private val testMetadataWithOverrides = testMetadata.copy(
    foundInTfeOverride = Some(true),
    isInternalEndpointOverride = Some(true),
    isNgRouteOverride = Some(true),
    requiresAuthOverride = Some(true),
    isAppOnlyOrGuestOverride = Some(true),
    oauth1OrSessionOverride = Some(true),
    alreadyAdoptedDpsOverride = Some(true)
  )

  private val testMetadataWithAnotherOverrides =
    testMetadataWithOverrides.copy(foundInTfeOverride = Some(false))

  test("test adoptioncheckerservice without overrides and non determined resolvers") {
    val testEndpointInfo = EndpointInfo(url = testUrl, metadata = Some(testMetadata))

    when(foundInTfeResolver.checkWithOverride(testEndpointInfo, None))
      .thenReturn(Future.value(None))
    when(isInternalEndpointResolver.checkWithOverride(testEndpointInfo, None))
      .thenReturn(Future.value(None))
    when(isNgRouteResolver.checkWithOverride(testEndpointInfo, None)).thenReturn(Future.value(None))
    when(requiresAuthResolver.checkWithOverride(testEndpointInfo, None))
      .thenReturn(Future.value(None))
    when(isAppOnlyOrGuestResolver.checkWithOverride(testEndpointInfo, None))
      .thenReturn(Future.value(None))
    when(oauth1OrSessionResolver.checkWithOverride(testEndpointInfo, None))
      .thenReturn(Future.value(None))
    when(alreadyAdoptedDpsResolver.checkWithOverride(testEndpointInfo, None))
      .thenReturn(Future.value(None))

    Await.result(
      adoptioncheckerservice.checkAdoptionStatus(
        endpoint = EndpointInfo(url = testUrl, metadata = Some(testMetadata)))
    ) mustBe AdoptionStatus(
      foundInTfe = None,
      isInternalEndpoint = None,
      isNgRoute = None,
      requiresAuth = None,
      isAppOnlyOrGuest = None,
      oauth1OrSession = None,
      alreadyAdoptedDps = None)
  }

  test("test adoption checker service with overrides and non defined resolvers") {
    val testEndpointInfo = EndpointInfo(url = testUrl, metadata = Some(testMetadataWithOverrides))

    when(
      foundInTfeResolver
        .checkWithOverride(testEndpointInfo, testMetadataWithOverrides.getFoundInTfeOverride))
      .thenReturn(
        Future.value(Some(AdoptionParameterValue[AdoptionParameter.FoundInTfe](underlying = true))))
    when(isInternalEndpointResolver
      .checkWithOverride(testEndpointInfo, testMetadataWithOverrides.getIsInternalEndpointOverride))
      .thenReturn(
        Future.value(
          Some(AdoptionParameterValue[AdoptionParameter.IsInternalEndpoint](underlying = true))))
    when(
      isNgRouteResolver
        .checkWithOverride(testEndpointInfo, testMetadataWithOverrides.getIsNgRouteOverride))
      .thenReturn(
        Future.value(Some(AdoptionParameterValue[AdoptionParameter.IsNgRoute](underlying = true))))
    when(
      requiresAuthResolver
        .checkWithOverride(testEndpointInfo, testMetadataWithOverrides.getRequiresAuthOverride))
      .thenReturn(
        Future.value(
          Some(AdoptionParameterValue[AdoptionParameter.RequiresAuth](underlying = true))))
    when(
      isAppOnlyOrGuestResolver
        .checkWithOverride(testEndpointInfo, testMetadataWithOverrides.getIsAppOnlyOrGuestOverride))
      .thenReturn(
        Future.value(
          Some(AdoptionParameterValue[AdoptionParameter.IsAppOnlyOrGuest](underlying = true))))
    when(
      oauth1OrSessionResolver
        .checkWithOverride(testEndpointInfo, testMetadataWithOverrides.getOauth1OrSessionOverride))
      .thenReturn(
        Future.value(
          Some(AdoptionParameterValue[AdoptionParameter.Oauth1OrSession](underlying = true))))
    when(alreadyAdoptedDpsResolver
      .checkWithOverride(testEndpointInfo, testMetadataWithOverrides.getAlreadyAdoptedDpsOverride))
      .thenReturn(
        Future.value(
          Some(AdoptionParameterValue[AdoptionParameter.AlreadyAdoptedDps](underlying = true))))

    Await.result(
      adoptioncheckerservice.checkAdoptionStatus(endpoint = testEndpointInfo)
    ) mustBe AdoptionStatus(
      foundInTfe = Some(AdoptionParameterValue[AdoptionParameter.FoundInTfe](underlying = true)),
      isInternalEndpoint =
        Some(AdoptionParameterValue[AdoptionParameter.IsInternalEndpoint](underlying = true)),
      isNgRoute = Some(AdoptionParameterValue[AdoptionParameter.IsNgRoute](underlying = true)),
      requiresAuth =
        Some(AdoptionParameterValue[AdoptionParameter.RequiresAuth](underlying = true)),
      isAppOnlyOrGuest =
        Some(AdoptionParameterValue[AdoptionParameter.IsAppOnlyOrGuest](underlying = true)),
      oauth1OrSession =
        Some(AdoptionParameterValue[AdoptionParameter.Oauth1OrSession](underlying = true)),
      alreadyAdoptedDps =
        Some(AdoptionParameterValue[AdoptionParameter.AlreadyAdoptedDps](underlying = true)),
    )
  }

  test(
    "test adoption checker service with overrides and non defined resolvers (different order, making sure futures are returned in order)") {
    val testEndpointInfo = EndpointInfo(
      url = testUrl,
      metadata = Some(testMetadataWithAnotherOverrides)
    )

    when(foundInTfeResolver
      .checkWithOverride(testEndpointInfo, testMetadataWithAnotherOverrides.getFoundInTfeOverride))
      .thenReturn(
        Future
          .value(
            Some(AdoptionParameterValue[AdoptionParameter.FoundInTfe](underlying = false))).delayed(
            1.second))
    when(
      isInternalEndpointResolver.checkWithOverride(
        testEndpointInfo,
        testMetadataWithAnotherOverrides.getIsInternalEndpointOverride))
      .thenReturn(
        Future.value(
          Some(AdoptionParameterValue[AdoptionParameter.IsInternalEndpoint](underlying = true))))
    when(
      isNgRouteResolver
        .checkWithOverride(testEndpointInfo, testMetadataWithAnotherOverrides.getIsNgRouteOverride))
      .thenReturn(
        Future.value(Some(AdoptionParameterValue[AdoptionParameter.IsNgRoute](underlying = true))))
    when(
      requiresAuthResolver.checkWithOverride(
        testEndpointInfo,
        testMetadataWithAnotherOverrides.getRequiresAuthOverride))
      .thenReturn(
        Future.value(
          Some(AdoptionParameterValue[AdoptionParameter.RequiresAuth](underlying = true))))
    when(
      isAppOnlyOrGuestResolver.checkWithOverride(
        testEndpointInfo,
        testMetadataWithAnotherOverrides.getIsAppOnlyOrGuestOverride))
      .thenReturn(
        Future.value(
          Some(AdoptionParameterValue[AdoptionParameter.IsAppOnlyOrGuest](underlying = true))))
    when(
      oauth1OrSessionResolver.checkWithOverride(
        testEndpointInfo,
        testMetadataWithAnotherOverrides.getOauth1OrSessionOverride))
      .thenReturn(
        Future.value(
          Some(AdoptionParameterValue[AdoptionParameter.Oauth1OrSession](underlying = true))))
    when(
      alreadyAdoptedDpsResolver.checkWithOverride(
        testEndpointInfo,
        testMetadataWithAnotherOverrides.getAlreadyAdoptedDpsOverride))
      .thenReturn(
        Future.value(
          Some(AdoptionParameterValue[AdoptionParameter.AlreadyAdoptedDps](underlying = true))))

    Await.result(
      adoptioncheckerservice.checkAdoptionStatus(endpoint = testEndpointInfo)
    ) mustBe AdoptionStatus(
      foundInTfe = Some(AdoptionParameterValue[AdoptionParameter.FoundInTfe](underlying = false)),
      isInternalEndpoint =
        Some(AdoptionParameterValue[AdoptionParameter.IsInternalEndpoint](underlying = true)),
      isNgRoute = Some(AdoptionParameterValue[AdoptionParameter.IsNgRoute](underlying = true)),
      requiresAuth =
        Some(AdoptionParameterValue[AdoptionParameter.RequiresAuth](underlying = true)),
      isAppOnlyOrGuest =
        Some(AdoptionParameterValue[AdoptionParameter.IsAppOnlyOrGuest](underlying = true)),
      oauth1OrSession =
        Some(AdoptionParameterValue[AdoptionParameter.Oauth1OrSession](underlying = true)),
      alreadyAdoptedDps =
        Some(AdoptionParameterValue[AdoptionParameter.AlreadyAdoptedDps](underlying = true)),
    )
  }

  test("test adoption checker service with overrides and non defined resolvers (partial failure)") {
    val testEndpointInfo = EndpointInfo(
      url = testUrl,
      metadata = Some(testMetadataWithAnotherOverrides)
    )

    when(foundInTfeResolver
      .checkWithOverride(testEndpointInfo, testMetadataWithAnotherOverrides.getFoundInTfeOverride))
      .thenReturn(
        Future
          .exception(new Exception()))
    when(
      isInternalEndpointResolver.checkWithOverride(
        testEndpointInfo,
        testMetadataWithAnotherOverrides.getIsInternalEndpointOverride))
      .thenReturn(
        Future.value(
          Some(AdoptionParameterValue[AdoptionParameter.IsInternalEndpoint](underlying = true))))
    when(
      isNgRouteResolver
        .checkWithOverride(testEndpointInfo, testMetadataWithAnotherOverrides.getIsNgRouteOverride))
      .thenReturn(
        Future.value(Some(AdoptionParameterValue[AdoptionParameter.IsNgRoute](underlying = true))))
    when(
      requiresAuthResolver.checkWithOverride(
        testEndpointInfo,
        testMetadataWithAnotherOverrides.getRequiresAuthOverride))
      .thenReturn(
        Future.value(
          Some(AdoptionParameterValue[AdoptionParameter.RequiresAuth](underlying = true))))
    when(
      isAppOnlyOrGuestResolver.checkWithOverride(
        testEndpointInfo,
        testMetadataWithAnotherOverrides.getIsAppOnlyOrGuestOverride))
      .thenReturn(
        Future.value(
          Some(AdoptionParameterValue[AdoptionParameter.IsAppOnlyOrGuest](underlying = true))))
    when(
      oauth1OrSessionResolver.checkWithOverride(
        testEndpointInfo,
        testMetadataWithAnotherOverrides.getOauth1OrSessionOverride))
      .thenReturn(
        Future.value(
          Some(AdoptionParameterValue[AdoptionParameter.Oauth1OrSession](underlying = true))))
    when(
      alreadyAdoptedDpsResolver.checkWithOverride(
        testEndpointInfo,
        testMetadataWithAnotherOverrides.getAlreadyAdoptedDpsOverride))
      .thenReturn(
        Future.value(
          Some(AdoptionParameterValue[AdoptionParameter.AlreadyAdoptedDps](underlying = true))))

    Await.result(
      adoptioncheckerservice.checkAdoptionStatus(testEndpointInfo)
    ) mustBe AdoptionStatus()
  }

}
