package com.twitter.auth.httpcontext

import com.twitter.auth.context.AuthPasetoContextKey
import com.twitter.finagle.context.Contexts
import com.twitter.finagle.http.codec.context.LoadableHttpContext

class AuthPasetoHttpContext extends LoadableHttpContext {
  type ContextKeyType = String
  val key: Contexts.broadcast.Key[ContextKeyType] = AuthPasetoContextKey
}
