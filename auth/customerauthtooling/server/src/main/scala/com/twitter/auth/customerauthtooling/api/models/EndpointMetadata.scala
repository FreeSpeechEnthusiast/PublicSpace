package com.twitter.auth.customerauthtooling.api.models

import com.twitter.auth.customerauthtooling.thriftscala.{EndpointMetadata => TEndpointMetadata}

final case class EndpointMetadata(
  suppliedDataPermissions: Option[Seq[DataPermission]] = None,
  private val foundInTfeOverride: Option[Boolean] = None,
  private val isInternalEndpointOverride: Option[Boolean] = None,
  private val isNgRouteOverride: Option[Boolean] = None,
  private val requiresAuthOverride: Option[Boolean] = None,
  private val isAppOnlyOrGuestOverride: Option[Boolean] = None,
  private val oauth1OrSessionOverride: Option[Boolean] = None,
  private val alreadyAdoptedDpsOverride: Option[Boolean] = None) {

  def getFoundInTfeOverride: Option[AdoptionParameterValue[AdoptionParameter.FoundInTfe]] = {
    foundInTfeOverride match {
      case Some(v) => Some(AdoptionParameterValue[AdoptionParameter.FoundInTfe](v))
      case None => None
    }
  }

  def getIsInternalEndpointOverride: Option[
    AdoptionParameterValue[AdoptionParameter.IsInternalEndpoint]
  ] = {
    isInternalEndpointOverride match {
      case Some(v) => Some(AdoptionParameterValue[AdoptionParameter.IsInternalEndpoint](v))
      case None => None
    }
  }

  def getIsNgRouteOverride: Option[AdoptionParameterValue[AdoptionParameter.IsNgRoute]] = {
    isNgRouteOverride match {
      case Some(v) => Some(AdoptionParameterValue[AdoptionParameter.IsNgRoute](v))
      case None => None
    }
  }

  def getRequiresAuthOverride: Option[AdoptionParameterValue[AdoptionParameter.RequiresAuth]] = {
    requiresAuthOverride match {
      case Some(v) => Some(AdoptionParameterValue[AdoptionParameter.RequiresAuth](v))
      case None => None
    }
  }

  def getIsAppOnlyOrGuestOverride: Option[
    AdoptionParameterValue[AdoptionParameter.IsAppOnlyOrGuest]
  ] = {
    isAppOnlyOrGuestOverride match {
      case Some(v) => Some(AdoptionParameterValue[AdoptionParameter.IsAppOnlyOrGuest](v))
      case None => None
    }
  }

  def getOauth1OrSessionOverride: Option[
    AdoptionParameterValue[AdoptionParameter.Oauth1OrSession]
  ] = {
    oauth1OrSessionOverride match {
      case Some(v) => Some(AdoptionParameterValue[AdoptionParameter.Oauth1OrSession](v))
      case None => None
    }
  }

  def getAlreadyAdoptedDpsOverride: Option[
    AdoptionParameterValue[AdoptionParameter.AlreadyAdoptedDps]
  ] = {
    alreadyAdoptedDpsOverride match {
      case Some(v) => Some(AdoptionParameterValue[AdoptionParameter.AlreadyAdoptedDps](v))
      case None => None
    }
  }
}

object EndpointMetadata {
  def fromThrift(thrift: TEndpointMetadata): EndpointMetadata = {
    EndpointMetadata(
      suppliedDataPermissions =
        thrift.suppliedDps.map(_.map(dpId => DataPermission(dataPermissionId = dpId)).toList),
      foundInTfeOverride = thrift.foundInTfeOverride,
      isInternalEndpointOverride = thrift.isInternalEndpointOverride,
      isNgRouteOverride = thrift.isNgRouteOverride,
      requiresAuthOverride = thrift.requiresAuthOverride,
      isAppOnlyOrGuestOverride = thrift.isAppOnlyOrGuestOverride,
      oauth1OrSessionOverride = thrift.oauth1OrSessionOverride,
      alreadyAdoptedDpsOverride = thrift.alreadyAdoptedDpsOverride
    )
  }
}
