package com.twitter.auth.customerauthtooling.api.models

import com.twitter.auth.customerauthtooling.thriftscala.{
  AdoptionRequirement => TAdoptionRequirement
}

object AdoptionRequirement extends Enumeration {
  type AdoptionRequirement = Value
  val RequiredCustomerAuthAndNgRoutesAdoption, RequiredNgRoutesAdoptionOnly, Required, NotRequired,
    UnableToDetermine = Value

  def toThrift(value: AdoptionRequirement): TAdoptionRequirement = {
    value match {
      case RequiredCustomerAuthAndNgRoutesAdoption =>
        TAdoptionRequirement.RequiredCustomerAuthAndNgRoutesAdoption
      case RequiredNgRoutesAdoptionOnly => TAdoptionRequirement.RequiredNgRoutesAdoptionOnly
      case Required => TAdoptionRequirement.Required
      case NotRequired => TAdoptionRequirement.NotRequired
      case UnableToDetermine => TAdoptionRequirement.UnableToDetermine
      // map unrecognized methods to get
      case _ => TAdoptionRequirement.UnableToDetermine
    }
  }
}
