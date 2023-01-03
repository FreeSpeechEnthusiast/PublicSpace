package com.twitter.auth.policykeeper.api.enforcement

import com.twitter.auth.policykeeper.api.storage.common.PolicyMappingUtils
import com.twitter.auth.policykeeper.thriftscala.RequestInformation
import com.twitter.auth.policykeeper.thriftscala.RouteInformation
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Uri
import com.twitter.tfe.core.routingng.NgRoute

object RouteInformationHandler {

  private def findPolicyRelatedTags(routeTags: Set[String]): Option[Set[String]] = {
    val matchedTags = routeTags
      .flatMap { tag =>
        tag match {
          case PolicyMappingUtils.StaticMatchPattern(_) => Some(tag)
          case _ => None
        }
      }
    if (matchedTags.isEmpty)
      None
    else
      Some(matchedTags)
  }

  /**
   * Extracts route information for policykeeper service from TFE request context
   *
   * @param request
   * @return
   */
  def collectRouteInformationFromRequest(
    request: Request,
    ngRoute: Option[NgRoute]
  ): RouteInformation = {
    RouteInformation(
      isNgRoute = ngRoute.isDefined,
      routeTags = ngRoute match {
        case Some(ngRoute) => findPolicyRelatedTags(ngRoute.tags)
        case _ => None
      },
      requestInformation = Some(
        RequestInformation(
          path = request.path,
          host = request.host,
          method = request.method.toString(),
          queryParams = Some(Map() ++ Uri.fromRequest(request).params), //transform to regular map
          bodyParams = Some(Map() ++ request.params) //transform to regular map
        ))
    )
  }
}
