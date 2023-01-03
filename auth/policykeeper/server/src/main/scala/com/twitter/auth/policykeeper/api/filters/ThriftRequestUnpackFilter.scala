package com.twitter.auth.policykeeper.api.filters

import com.google.inject.Inject
import com.google.inject.Singleton
import com.twitter.auth.pasetoheaders.javahelpers.OptionConv._
import com.twitter.auth.pasetoheaders.models.Passports.CustomerPassport
import com.twitter.auth.passport_tools.PassportRetriever
import com.twitter.auth.policykeeper.api.context.LocalContext
import com.twitter.finagle.Filter
import com.twitter.finagle.Filter.TypeAgnostic
import com.twitter.finagle.Service
import com.twitter.scrooge.{Request => ThriftRequest}

/**
 * Extracts data from finagle broadcast context into local context
 * so it can be used in data providers
 */
@Singleton
class ThriftRequestUnpackFilter @Inject() (passportRetriever: PassportRetriever)
    extends TypeAgnostic {

  def toFilter[T, U]: Filter[T, U, T, U] = (request: T, service: Service[T, U]) => {
    request match {
      case _: ThriftRequest[_] =>
        passportRetriever.passport match {
          case Some(p: CustomerPassport) =>
            // handles both customer and employee passports
            // because employee passport is a child class of customer passport
            LocalContext.writeToLocalContexts(
              clientApplicationId = p.getClientApplicationId,
              userId = p.getUserId,
              sessionHash = p.getSessionHash,
              authenticatedUserId = p.getAuthenticatedUserId,
              guestToken = p.getGuestToken
            ) {
              service(request)
            }
          case _ =>
            // No passport found
            service(request)
        }

      case _ =>
        service(request)
    }
  }

}
