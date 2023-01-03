package com.twitter.auth.policykeeper.api

import com.twitter.auth.policykeeper.thriftscala.RouteInformation
import com.twitter.auth.policykeeper.thriftscala.VerifyPoliciesRequest
import com.twitter.auth.policykeeper.thriftscala.VerifyRoutePoliciesRequest

trait PolicyKeeperWarmupFixtures {

  def verifyPoliciesRequest: VerifyPoliciesRequest =
    VerifyPoliciesRequest(
      policyIds = Set()
    )

  def verifyRoutePoliciesRequest: VerifyRoutePoliciesRequest =
    VerifyRoutePoliciesRequest(
      routeInformation = RouteInformation(
        isNgRoute = true,
        routeTags = None
      )
    )

}
