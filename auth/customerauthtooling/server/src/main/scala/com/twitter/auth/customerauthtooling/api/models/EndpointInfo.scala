package com.twitter.auth.customerauthtooling.api.models

import com.twitter.auth.customerauthtooling.thriftscala.{RequestMethod => TRequestMethod}
import com.twitter.auth.customerauthtooling.api.models.RequestMethod.RequestMethod
import com.twitter.auth.customerauthtooling.thriftscala.{EndpointInfo => TEndpointInfo}

final case class EndpointInfo(
  url: String,
  method: Option[RequestMethod] = Some(RequestMethod.Get),
  metadata: Option[EndpointMetadata] = None)

object EndpointInfo {
  def fromThrift(thrift: TEndpointInfo): EndpointInfo = {
    EndpointInfo(
      url = thrift.url,
      method = Some(RequestMethod.fromThrift(thrift.method.getOrElse(TRequestMethod.Get))),
      metadata = thrift.metadata.map(EndpointMetadata.fromThrift)
    )
  }
}
