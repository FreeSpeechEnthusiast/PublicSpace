package com.twitter.auth.customerauthtooling.cli.commands

import com.twitter.auth.customerauthtooling.api.models.RouteInformation
import picocli.CommandLine.IDefaultValueProvider
import picocli.CommandLine.Model

/**
 * This class adds dynamic values from RouteInformation.Defaults to Commands
 * due to the fact that dynamic values are not allowed in annotations
 */
class RouteDefaultParametersProvider extends IDefaultValueProvider {
  override def defaultValue(argSpec: Model.ArgSpec): String = {
    argSpec.paramLabel() match {
      case "<auth_types>" => RouteInformation.Defaults.AuthTypes.mkString(",")
      case "<user_roles>" => RouteInformation.Defaults.UserRoles.mkString(",")
      case "<flags>" => RouteInformation.Defaults.RouteFlags.mkString(",")
      case "<fps>" => RouteInformation.Defaults.FeaturePermissions.mkString(",")
      case "<sps>" => RouteInformation.Defaults.SubscriptionPermissions.mkString(",")
      case "<tags>" => RouteInformation.Defaults.Tags.mkString(",")
      case "<ua_tags>" => RouteInformation.Defaults.UaTags.mkString(",")
      case "<scopes>" => RouteInformation.Defaults.Scopes.mkString(",")
      case "<ldap_owners>" => RouteInformation.Defaults.LdapOwners.mkString(",")
      case "<priority>" => RouteInformation.Defaults.Priority.toString
      case "<timeout_ms>" => RouteInformation.Defaults.TimeoutMs.toString
      case "<experiment_buckets>" =>
        RouteInformation.Defaults.ExperimentBuckets.map(e => e.bucket + ":" + e.key).mkString(",")
      case "<method>" => RouteInformation.Defaults.Method.toString
      case "<rate_limit>" => RouteInformation.Defaults.RateLimit.toString
      // fallback to annotation
      case _ => argSpec.defaultValue()
    }
  }
}
