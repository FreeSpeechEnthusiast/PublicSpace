package com.twitter.auth.customerauthtooling.api.services

import com.twitter.auth.customerauthtooling.thriftscala.CheckAdoptionStatusResponse
import com.twitter.auth.customerauthtooling.thriftscala.CustomerAuthToolingService.CheckAdoptionStatus
import com.twitter.auth.customerauthtooling.api.components.adoptionchecker.AdoptionCheckerServiceInterface
import com.twitter.auth.customerauthtooling.api.models.EndpointInfo
import com.twitter.scrooge.Request
import com.twitter.scrooge.Response
import com.twitter.util.Future
import com.twitter.util.logging.Logging
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdoptionCheckerThriftService @Inject() (
  adoptionCheckerService: AdoptionCheckerServiceInterface)
    extends CheckAdoptionStatus.ReqRepServicePerEndpointServiceType
    with Logging {

  def apply(
    thriftRequest: Request[CheckAdoptionStatus.Args]
  ): Future[Response[CheckAdoptionStatusResponse]] = {

    val request = thriftRequest.args.request

    adoptionCheckerService
      .checkAdoptionStatus(endpoint = EndpointInfo.fromThrift(request.endpointInfo)).map(
        _.toThrift).map(status => Response(CheckAdoptionStatusResponse(adoptionStatus = status)))

  }
}
