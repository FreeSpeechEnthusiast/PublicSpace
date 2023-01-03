package com.twitter.auth.pasetoheaders.thriftconversion

import com.twitter.auth.pasetoheaders.models._
import com.twitter.auth.authenforcement.thriftscala.{
  DataPermissionDecisions => ThriftDataPermissionDecisions
}
import com.twitter.auth.authenforcement.thriftscala.{
  FeaturePermissionDecisions => ThriftFeaturePermissionDecisions
}
import com.twitter.auth.authenforcement.thriftscala.{
  SubscriptionPermissionDecisions => ThriftSubscriptionPermissionDecisions
}
import com.twitter.auth.authenforcement.thriftscala.{Passport => ThriftPassport}
import com.twitter.auth.authenforcement.thriftscala.Principal._

object FromThriftPassport {
  import com.twitter.auth.pasetoheaders.javahelpers.OptionConv._
  import com.twitter.auth.pasetoheaders.javahelpers.SetConv._

  private def convertOptionThriftModel[T, P](
    optThriftModel: Option[T],
    convertor: T => P
  ): Option[P] = {
    optThriftModel match {
      case Some(t) => Some(convertor(t))
      case None => None
    }
  }

  private def convertPassport(
    thriftPassport: ThriftPassport
  ): Option[Passports.Passport] = {
    thriftPassport.principals.nonEmpty match {
      case false => None
      case true =>
        val principals = PrincipalsAggregator()
        thriftPassport.principals.foreach {
          case v: UserPrincipal => principals.userId = Some(v.userPrincipal.userId)
          case v: AuthenticatedUserPrincipal =>
            principals.authenticatedUserId = Some(v.authenticatedUserPrincipal.userId)
          case v: ClientApplicationPrincipal =>
            principals.clientApplicationId = Some(v.clientApplicationPrincipal.clientApplicationId)
          case v: SessionPrincipal =>
            principals.sessionHash = Some(v.sessionPrincipal.sessionHash)
          case v: EmployeePrincipal => principals.ldapAccount = v.employeePrincipal.ldapAccount
          case v: GuestPrincipal => principals.guestToken = Some(v.guestPrincipal.guestToken)
        }
        val dpd =
          convertOptionThriftModel[ThriftDataPermissionDecisions, DataPermissionDecisions](
            thriftPassport.dataPermissionDecisions,
            t =>
              new DataPermissionDecisions(
                t.allowedDataPermissionIds,
                t.rejectedDataPermissionIds
              ))
        val fpd =
          convertOptionThriftModel[ThriftFeaturePermissionDecisions, FeaturePermissionDecisions](
            thriftPassport.featurePermissionDecisions,
            t =>
              new FeaturePermissionDecisions(
                t.allowedFeaturePermissions,
                t.rejectedFeaturePermissions
              ))
        val spd =
          convertOptionThriftModel[
            ThriftSubscriptionPermissionDecisions,
            SubscriptionPermissionDecisions](
            thriftPassport.subscriptionPermissionDecisions,
            t =>
              new SubscriptionPermissionDecisions(
                t.allowedSubscriptionPermissions,
                t.rejectedSubscriptionPermissions
              )
          )
        principals.ldapAccount match {
          case None =>
            Some(
              new Passports.CustomerPassport(
                thriftPassport.passportId,
                principals.userId,
                principals.authenticatedUserId,
                principals.guestToken,
                principals.clientApplicationId,
                principals.sessionHash,
                dpd,
                fpd,
                spd
              )
            )
          case Some(ldapAccount: String) =>
            Some(
              new Passports.EmployeePassport(
                thriftPassport.passportId,
                principals.userId,
                principals.authenticatedUserId,
                principals.clientApplicationId,
                principals.sessionHash,
                ldapAccount,
                dpd,
                fpd,
                spd
              )
            )
        }
    }
  }

  /**
   * Converts thrift passport into pojo passport (member of Passports.Passport)
   * If such conversion is not possible returns None
   *
   * @param thriftPassport
   * @return
   */
  def toPojo(thriftPassport: ThriftPassport): Option[Passports.Passport] = {
    convertPassport(thriftPassport)
  }

}

private case class PrincipalsAggregator(
  var userId: Option[Long] = None,
  var authenticatedUserId: Option[Long] = None,
  var guestToken: Option[Long] = None,
  var clientApplicationId: Option[Long] = None,
  var sessionHash: Option[String] = None,
  var ldapAccount: Option[String] = None)
