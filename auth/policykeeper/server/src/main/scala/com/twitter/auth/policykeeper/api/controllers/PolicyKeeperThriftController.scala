package com.twitter.auth.policykeeper.api.controllers

import com.twitter.auth.policykeeper.api.services.internal.VerifyPoliciesService
import com.twitter.auth.policykeeper.api.services.internal.VerifyRoutePoliciesService
import com.twitter.auth.policykeeper.thriftscala.PolicyKeeperService
import com.twitter.auth.policykeeper.thriftscala.PolicyKeeperService.VerifyPolicies
import com.twitter.auth.policykeeper.thriftscala.PolicyKeeperService.VerifyRoutePolicies
import com.twitter.decider.Decider
import com.twitter.finatra.thrift.Controller
import javax.inject.Inject

class PolicyKeeperThriftController @Inject() (
  decider: Decider,
  verifyPoliciesService: VerifyPoliciesService,
  verifyRoutePoliciesService: VerifyRoutePoliciesService)
    extends Controller(PolicyKeeperService) {

  handle(VerifyPolicies).withService(
    verifyPoliciesService
  )

  handle(VerifyRoutePolicies).withService(
    verifyRoutePoliciesService
  )

}
