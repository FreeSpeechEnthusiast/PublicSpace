package com.twitter.auth.httpcontext

import com.twitter.finagle.context.Contexts
import com.twitter.finagle.http.codec.context.LoadableHttpContext
import com.twitter.psec.common.context.S2SPrincipalTokenContextKey

class S2SPrincipalTokenHttpContext extends LoadableHttpContext {
  type ContextKeyType = String
  val key: Contexts.broadcast.Key[ContextKeyType] = S2SPrincipalTokenContextKey
}
