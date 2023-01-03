package com.twitter.auth.customerauthtooling.api.models

import com.twitter.auth.customerauthtooling.thriftscala.{AdoptionStatus => TAdoptionStatus}

/**
 * @param foundInTfe True if exists in TFE
 * @param isInternalEndpoint True if internal/twoffice/twitter.biz
 * @param isNgRoute True if exists in ng routes
 * @param requiresAuth True if require authentication
 * @param isAppOnlyOrGuest True if there is no notion of a logged in user
 * @param oauth1OrSession True if requires auth1 or session auth types
 * @param alreadyAdoptedDps True if adopted to PDP customer auth
 */
private[api] final case class AdoptionStatus(
  foundInTfe: Option[AdoptionParameterValue[AdoptionParameter.FoundInTfe]] = None,
  isInternalEndpoint: Option[AdoptionParameterValue[AdoptionParameter.IsInternalEndpoint]] = None,
  isNgRoute: Option[AdoptionParameterValue[AdoptionParameter.IsNgRoute]] = None,
  requiresAuth: Option[AdoptionParameterValue[AdoptionParameter.RequiresAuth]] = None,
  isAppOnlyOrGuest: Option[AdoptionParameterValue[AdoptionParameter.IsAppOnlyOrGuest]] = None,
  oauth1OrSession: Option[AdoptionParameterValue[AdoptionParameter.Oauth1OrSession]] = None,
  alreadyAdoptedDps: Option[AdoptionParameterValue[AdoptionParameter.AlreadyAdoptedDps]] = None) {

  /**
   * Get PDP customer auth adoption state based on gathered information
   *
   * The logic reflects current requirements
   * and based on https://docs.google.com/document/d/1SJmVOfXp6cByBnI4GErnNYQNmahIUWqCOz8xULEs_TI/edit#
   *
   * @return adoption requirements
   */
  lazy val adoptionRequired: AdoptionRequirement.Value = {
    (
      foundInTfe,
      isInternalEndpoint,
      isNgRoute,
      requiresAuth,
      isAppOnlyOrGuest,
      oauth1OrSession,
      alreadyAdoptedDps) match {
      case (
            Some(foundInTfe),
            Some(isInternalEndpoint),
            Some(isNgRoute),
            Some(requiresAuth),
            Some(isAppOnlyOrGuest),
            _, // oauth1OrSession currently doesn't matter
            Some(alreadyAdoptedDps)) =>
        if (alreadyAdoptedDps.toBoolean || isInternalEndpoint.toBoolean || !foundInTfe.toBoolean) {
          AdoptionRequirement.NotRequired
        } else if (isAppOnlyOrGuest.toBoolean || !requiresAuth.toBoolean) {
          if (isNgRoute.toBoolean) {
            AdoptionRequirement.NotRequired
          } else {
            AdoptionRequirement.RequiredNgRoutesAdoptionOnly
          }
        } else if (isNgRoute.toBoolean) {
          AdoptionRequirement.Required
        } else {
          AdoptionRequirement.RequiredCustomerAuthAndNgRoutesAdoption
        }
      case _ =>
        // not enough information provided to decide
        AdoptionRequirement.UnableToDetermine
    }
  }

  def toThrift: TAdoptionStatus = {
    TAdoptionStatus(
      requirement = AdoptionRequirement.toThrift(adoptionRequired),
      foundInTfe = foundInTfe.map(_.toBoolean),
      isInternalEndpoint = isInternalEndpoint.map(_.toBoolean),
      isNgRoute = isNgRoute.map(_.toBoolean),
      requiresAuth = requiresAuth.map(_.toBoolean),
      isAppOnlyOrGuest = isAppOnlyOrGuest.map(_.toBoolean),
      oauth1OrSession = oauth1OrSession.map(_.toBoolean),
      alreadyAdoptedDps = alreadyAdoptedDps.map(_.toBoolean),
    )
  }
}
