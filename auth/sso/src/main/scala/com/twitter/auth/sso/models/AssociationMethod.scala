package com.twitter.auth.sso.models

import com.twitter.auth.sso.thriftscala.{AssociationMethod => TAssociationMethod}

case object AssociationMethodDoesNotExist extends Exception

sealed trait AssociationMethod
object AssociationMethod {
  case object Signup extends AssociationMethod
  case object Login extends AssociationMethod

  def toThrift(m: AssociationMethod): TAssociationMethod = {
    m match {
      case Signup => TAssociationMethod.Signup
      case Login => TAssociationMethod.Login
    }
  }

  def fromThrift(t: TAssociationMethod): AssociationMethod = {
    t match {
      case TAssociationMethod.Signup => AssociationMethod.Signup
      case TAssociationMethod.Login => AssociationMethod.Login
      case _ => throw AssociationMethodDoesNotExist
    }
  }
}
