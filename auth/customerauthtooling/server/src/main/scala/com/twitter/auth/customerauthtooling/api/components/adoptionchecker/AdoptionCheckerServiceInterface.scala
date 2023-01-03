package com.twitter.auth.customerauthtooling.api.components.adoptionchecker

import com.twitter.auth.customerauthtooling.api.models.AdoptionStatus
import com.twitter.auth.customerauthtooling.api.models.EndpointInfo
import com.twitter.util.Future

trait AdoptionCheckerServiceInterface {
  def checkAdoptionStatus(
    endpoint: EndpointInfo
  ): Future[AdoptionStatus]
}
