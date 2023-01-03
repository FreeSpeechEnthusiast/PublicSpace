package com.twitter.auth.models

import com.twitter.flightauth.thriftscala.{GuestToken => TGuestToken}

case class GuestToken(
  clientApplicationId: Long,
  id: Long,
  createdAt: Long,
  lastUsedAt: Long) {
  // The GuestToken has sensitive PII. To avoid accidentally writing it in a log or error
  // message, we are redacting the toString method.
  override def toString: String =
    s"GuestToken(<all fields redacted, id: ${this.id}>)"
}

object GuestToken {
  def fromThrift(gt: TGuestToken): GuestToken = {
    GuestToken(
      clientApplicationId = gt.clientApplicationId,
      id = gt.id,
      createdAt = gt.createdAt,
      lastUsedAt = gt.lastUsedAt
    )
  }
}
