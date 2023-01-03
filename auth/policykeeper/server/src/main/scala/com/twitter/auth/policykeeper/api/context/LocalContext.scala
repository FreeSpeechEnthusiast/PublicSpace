package com.twitter.auth.policykeeper.api.context

import com.twitter.finagle.context.Contexts

case class LocalContext(
  var clientApplicationId: Option[Long] = None,
  var userId: Option[Long] = None,
  var sessionHash: Option[String] = None,
  var guestToken: Option[Long] = None,
  var authenticatedUserId: Option[Long] = None)

object LocalContext {
  val localContextKey = new Contexts.local.Key[LocalContext]

  def getUserId: Option[Long] = {
    Contexts.local.get(localContextKey).flatMap { context =>
      context.userId
    }
  }

  def getSessionHash: Option[String] = {
    Contexts.local.get(localContextKey).flatMap { context =>
      context.sessionHash
    }
  }

  def getAuthenticatedUserId: Option[Long] = {
    Contexts.local.get(localContextKey).flatMap { context =>
      context.authenticatedUserId
    }
  }

  def getClientApplicationId: Option[Long] = {
    Contexts.local.get(localContextKey).flatMap { context =>
      context.clientApplicationId
    }
  }

  def getGuestToken: Option[Long] = {
    Contexts.local.get(localContextKey).flatMap { context =>
      context.guestToken
    }
  }

  def getLocalContext: Option[LocalContext] = {
    Contexts.local.get(localContextKey)
  }

  def writeToLocalContexts[R](
    clientApplicationId: Option[Long] = None,
    userId: Option[Long] = None,
    sessionHash: Option[String] = None,
    guestToken: Option[Long] = None,
    authenticatedUserId: Option[Long] = None
  )(
    fn: => R
  ): R = {
    Contexts.local.let(
      localContextKey,
      getLocalContext match {
        case Some(oc) =>
          clientApplicationId.foreach(newValue => oc.clientApplicationId = Some(newValue))
          userId.foreach(newValue => oc.userId = Some(newValue))
          sessionHash.foreach(newValue => oc.sessionHash = Some(newValue))
          guestToken.foreach(newValue => oc.guestToken = Some(newValue))
          authenticatedUserId.foreach(newValue => oc.authenticatedUserId = Some(newValue))
          oc
        case None =>
          LocalContext(
            clientApplicationId = clientApplicationId,
            userId = userId,
            sessionHash = sessionHash,
            guestToken = guestToken,
            authenticatedUserId = authenticatedUserId
          )
      }
    )(fn)
  }
}
