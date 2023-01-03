package com.twitter.auth.customerauthtooling.api.components

import com.twitter.auth.customerauthtooling.api.components.adoptionchecker.AdoptionCheckerServiceInterface
import com.twitter.auth.customerauthtooling.api.components.parameterresolver.AdoptionParameterResolverInterface
import com.twitter.auth.customerauthtooling.api.models.AdoptionParameter
import com.twitter.auth.customerauthtooling.api.models.AdoptionParameterValue
import com.twitter.auth.customerauthtooling.api.models.AdoptionStatus
import com.twitter.auth.customerauthtooling.api.models.EndpointInfo
import com.twitter.auth.customerauthtooling.api.models.EndpointMetadata
import com.twitter.util.Future
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
final case class AdoptionCheckerService @Inject() (
  foundInTfeResolver: AdoptionParameterResolverInterface[AdoptionParameter.FoundInTfe],
  isInternalEndpointResolver: AdoptionParameterResolverInterface[
    AdoptionParameter.IsInternalEndpoint
  ],
  isNgRouteResolver: AdoptionParameterResolverInterface[AdoptionParameter.IsNgRoute],
  requiresAuthResolver: AdoptionParameterResolverInterface[AdoptionParameter.RequiresAuth],
  isAppOnlyOrGuestResolver: AdoptionParameterResolverInterface[AdoptionParameter.IsAppOnlyOrGuest],
  oauth1OrSessionResolver: AdoptionParameterResolverInterface[AdoptionParameter.Oauth1OrSession],
  alreadyAdoptedDpsResolver: AdoptionParameterResolverInterface[
    AdoptionParameter.AlreadyAdoptedDps
  ],
) extends AdoptionCheckerServiceInterface {
  override def checkAdoptionStatus(endpoint: EndpointInfo): Future[AdoptionStatus] = {
    val metadata = endpoint.metadata.getOrElse(EndpointMetadata())
    // gather information from all checkers
    Future
      .collect(
        List(
          foundInTfeResolver.checkWithOverride(endpoint, metadata.getFoundInTfeOverride),
          isInternalEndpointResolver
            .checkWithOverride(endpoint, metadata.getIsInternalEndpointOverride),
          isNgRouteResolver.checkWithOverride(endpoint, metadata.getIsNgRouteOverride),
          requiresAuthResolver.checkWithOverride(endpoint, metadata.getRequiresAuthOverride),
          isAppOnlyOrGuestResolver
            .checkWithOverride(endpoint, metadata.getIsAppOnlyOrGuestOverride),
          oauth1OrSessionResolver.checkWithOverride(endpoint, metadata.getOauth1OrSessionOverride),
          alreadyAdoptedDpsResolver
            .checkWithOverride(endpoint, metadata.getAlreadyAdoptedDpsOverride)
        )).map {
        // matching checkers' results in order
        case Seq(a, b, c, d, e, f, g) =>
          AdoptionStatus(
            foundInTfe =
              a.map(_.asInstanceOf[AdoptionParameterValue[AdoptionParameter.FoundInTfe]]),
            isInternalEndpoint =
              b.map(_.asInstanceOf[AdoptionParameterValue[AdoptionParameter.IsInternalEndpoint]]),
            isNgRoute = c.map(_.asInstanceOf[AdoptionParameterValue[AdoptionParameter.IsNgRoute]]),
            requiresAuth =
              d.map(_.asInstanceOf[AdoptionParameterValue[AdoptionParameter.RequiresAuth]]),
            isAppOnlyOrGuest =
              e.map(_.asInstanceOf[AdoptionParameterValue[AdoptionParameter.IsAppOnlyOrGuest]]),
            oauth1OrSession =
              f.map(_.asInstanceOf[AdoptionParameterValue[AdoptionParameter.Oauth1OrSession]]),
            alreadyAdoptedDps =
              g.map(_.asInstanceOf[AdoptionParameterValue[AdoptionParameter.AlreadyAdoptedDps]]),
          )
        case _ =>
          // unexpected unless there is a mismatch of input/output
          // TODO: add measuring / logging
          AdoptionStatus()
      }.rescue {
        case _: Exception =>
          // TODO: add measuring / logging
          Future.value(AdoptionStatus())
      }
  }
}
