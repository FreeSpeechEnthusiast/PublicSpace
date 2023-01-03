package com.twitter.auth.sso.models

import com.twitter.util.Time

case class SsoInfo(
  ssoId: String,
  ssoEmail: String,
  associationMethod: AssociationMethod,
  time: Time)
