package com.twitter.auth.customerauthtooling.api.models

import com.twitter.auth.customerauthtooling.thriftscala.{AppliedAction => TRouteAppliedAction}

object RouteAppliedAction extends Enumeration {
  type RouteAppliedAction = Value
  val Insert, Update, Delete, Nothing, Error = Value

  def toThrift(value: RouteAppliedAction): TRouteAppliedAction = {
    value match {
      case Insert => TRouteAppliedAction.Insert
      case Update => TRouteAppliedAction.Update
      case Delete => TRouteAppliedAction.Delete
      case Nothing => TRouteAppliedAction.Nothing
      case Error => TRouteAppliedAction.Error
      // map unrecognized methods to nothing
      case _ => TRouteAppliedAction.Nothing
    }
  }

  def fromThrift(thrift: TRouteAppliedAction): RouteAppliedAction = {
    thrift match {
      case TRouteAppliedAction.Insert => RouteAppliedAction.Insert
      case TRouteAppliedAction.Update => RouteAppliedAction.Update
      case TRouteAppliedAction.Delete => RouteAppliedAction.Delete
      case TRouteAppliedAction.Nothing => RouteAppliedAction.Nothing
      case TRouteAppliedAction.Error => RouteAppliedAction.Error
      // map unrecognized methods to nothing
      case _ => RouteAppliedAction.Nothing
    }
  }
}
