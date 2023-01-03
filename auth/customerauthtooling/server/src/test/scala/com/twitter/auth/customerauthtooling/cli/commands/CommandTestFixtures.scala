package com.twitter.auth.customerauthtooling.cli.commands

import com.twitter.auth.authenticationtype.thriftscala.AuthenticationType
import com.twitter.auth.customerauthtooling.thriftscala.RouteInfo
import com.twitter.auth.customerauthtooling.thriftscala.RequestMethod
import com.twitter.auth.customerauthtooling.cli.commands.converters.AuthTypeSetWrapper
import com.twitter.auth.customerauthtooling.cli.commands.converters.StringSetWrapper

trait CommandTestFixtures {

  protected val testProject = "test"
  protected val testDomains: StringSetWrapper = StringSetWrapper(Set("api.twitter.com"))
  protected val testAuthTypes: AuthTypeSetWrapper = AuthTypeSetWrapper(
    Set.empty[AuthenticationType])
  protected val testAnotherAuthTypes: AuthTypeSetWrapper = AuthTypeSetWrapper(
    Set(AuthenticationType.Oauth1))
  protected val testAnotherScopes: StringSetWrapper = StringSetWrapper(Set("anotherScope"))
  protected val testAnotherDeciderValue = "anotherDecider"
  protected val testMethod: Option[RequestMethod] = Some(RequestMethod.Get)
  protected val testEndpoint = "/endpoint"
  protected val testCluster = "test"

  protected val testRequiredOptions = new CommonRequiredOptions()
  testRequiredOptions.project = testProject
  testRequiredOptions.domains = testDomains
  testRequiredOptions.authTypes = testAuthTypes

  protected val testNotRequiredForUpdateOptions = new CommonButNotRequiredForUpdateOptions()
  testNotRequiredForUpdateOptions.project = testProject
  testNotRequiredForUpdateOptions.domains = testDomains
  testNotRequiredForUpdateOptions.authTypes = testAuthTypes

  protected val testPathDetails = new IdentifyingPathDetails()
  testPathDetails.method = testMethod
  testPathDetails.routePath = "/endpoint"
  testPathDetails.routeCluster = "test"

  protected val testIdentifyingOptions = new IdOrPathIdentifiers()
  testIdentifyingOptions.pathDetails = testPathDetails
  testIdentifyingOptions.id = null

  protected val testCommonOptions = new CommonOptions()
  testCommonOptions.automaticDecider = None
  testCommonOptions.dps = None
  testCommonOptions.userRoles = None
  testCommonOptions.routeFlags = None
  testCommonOptions.featurePermissions = None
  testCommonOptions.subscriptionPermissions = None
  testCommonOptions.routeTags = None
  testCommonOptions.uaTags = None
  testCommonOptions.scopes = None
  testCommonOptions.decider = None
  testCommonOptions.ldapOwners = Some(StringSetWrapper(Set("authplatform")))
  testCommonOptions.priority = None
  testCommonOptions.rateLimit = 10
  testCommonOptions.timeoutMs = Some(10)
  testCommonOptions.experimentBuckets = None

  protected val testRouteId = "GET/endpoint->cluster:test"

  protected val testRoute: RouteInfo = RouteInfo(
    path = "/endpoint",
    domains = testDomains.get(),
    cluster = "test",
    method = Some(RequestMethod.Get),
    authTypes = Some(testAuthTypes.get()),
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
    scopes = None,
    rateLimit = Some(10),
    timeoutMs = Some(10),
    ldapOwners = Some(Set("authplatform")),
    id = Some(testRouteId),
    projectId = Some(testProject),
  )

}
