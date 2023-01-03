package com.twitter.auth.customerauthtooling.api.models

import com.twitter.auth.customerauthtooling.thriftscala.{
  DataPermissionAnnotation => TDataPermission
}
import com.twitter.tfe.core.routingng.RawDataPermissionAnnotation

final case class DataPermission(dataPermissionId: Long, state: Option[String] = Some("enforced")) {
  def toThrift: TDataPermission = {
    TDataPermission(id = dataPermissionId, state = state)
  }
  def toRawDataPermissionAnnotation: RawDataPermissionAnnotation = {
    RawDataPermissionAnnotation(
      id = dataPermissionId,
      state = state.getOrElse("enforced"),
      testing = false)
  }
}
