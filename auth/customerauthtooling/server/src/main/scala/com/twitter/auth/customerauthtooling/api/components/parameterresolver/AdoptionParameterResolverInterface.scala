package com.twitter.auth.customerauthtooling.api.components.parameterresolver

import com.twitter.auth.customerauthtooling.api.models.AdoptionParameter
import com.twitter.auth.customerauthtooling.api.models.AdoptionParameterValue
import com.twitter.auth.customerauthtooling.api.models.EndpointInfo
import com.twitter.util.Future

trait AdoptionParameterResolverInterface[T <: AdoptionParameter] {
  protected def check(endpoint: EndpointInfo): Future[Option[AdoptionParameterValue[T]]]

  final def checkWithOverride(
    endpoint: EndpointInfo,
    overrideValue: Option[AdoptionParameterValue[T]] = None
  ): Future[Option[AdoptionParameterValue[T]]] = {
    overrideValue match {
      case Some(o) => Future.value(Some(o))
      case None => check(endpoint)
    }
  }
}
