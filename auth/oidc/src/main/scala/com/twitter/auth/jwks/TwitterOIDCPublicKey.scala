package com.twitter.auth.jwks

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * JWKs config for Twitter OIDC
 */
@JsonIgnoreProperties(ignoreUnknown = true)
case class TwitterOIDCPublicKey(
  alg: String,
  n: String,
  kid: String,
  kty: String,
  use: String,
  e: String)
