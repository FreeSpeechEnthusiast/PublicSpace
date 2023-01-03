package com.twitter.auth.customerauthtooling.api.models

import com.twitter.auth.customerauthtooling.thriftscala.{RouteDraft => TRouteDraft}
import com.twitter.auth.customerauthtooling.api.models.RouteAppliedAction.RouteAppliedAction

case class RouteDraft(
  uuid: String,
  expectedRouteId: String,
  action: Option[RouteAppliedAction]) {
  def toThrift: TRouteDraft = {
    TRouteDraft(
      uuid = uuid,
      expectedRouteId = expectedRouteId,
      action = action.map(RouteAppliedAction.toThrift))
  }
}
