package com.twitter.auth.customerauthtooling.cli.commands

import com.twitter.auth.customerauthtooling.cli.commands.converters.ExperimentBucketSetWrapper
import com.twitter.auth.customerauthtooling.cli.commands.converters.LongSetWrapper
import com.twitter.auth.customerauthtooling.cli.commands.converters.OptExperimentBucketSetConverter
import com.twitter.auth.customerauthtooling.cli.commands.converters.OptIntConverter
import com.twitter.auth.customerauthtooling.cli.commands.converters.OptLongSetConverter
import com.twitter.auth.customerauthtooling.cli.commands.converters.OptStringConverter
import com.twitter.auth.customerauthtooling.cli.commands.converters.OptStringSetConverter
import com.twitter.auth.customerauthtooling.cli.commands.converters.StringSetWrapper
import picocli.CommandLine.{Option => CommandLineOption}

class CommonOptions extends DefaultValue with AutomaticDeciderOption {
  @CommandLineOption(
    names = Array("--dps"),
    description = Array("Required data permissions (default: " + defaultValueMacro + ")"),
    converter = Array(classOf[OptLongSetConverter]),
    defaultValue = "")
  var dps: Option[LongSetWrapper] = None

  @CommandLineOption(
    names = Array("--user_roles"),
    description = Array("Allowed user roles (default: " + defaultValueMacro + ")"),
    converter = Array(classOf[OptStringSetConverter]),
    paramLabel = "<user_roles>")
  var userRoles: Option[StringSetWrapper] = None

  @CommandLineOption(
    names = Array("--flags"),
    description = Array("Route flags (default: " + defaultValueMacro + ")"),
    converter = Array(classOf[OptStringSetConverter]),
    paramLabel = "<flags>")
  var routeFlags: Option[StringSetWrapper] = None

  @CommandLineOption(
    names = Array("--fps"),
    description = Array("Allowed feature permissions (default: " + defaultValueMacro + ")"),
    converter = Array(classOf[OptStringSetConverter]),
    paramLabel = "<fps>")
  var featurePermissions: Option[StringSetWrapper] = None

  @CommandLineOption(
    names = Array("--sps"),
    description = Array("Allowed subscription permissions (default: " + defaultValueMacro + ")"),
    converter = Array(classOf[OptStringSetConverter]),
    paramLabel = "<sps>")
  var subscriptionPermissions: Option[StringSetWrapper] = None

  @CommandLineOption(
    names = Array("--tags"),
    description = Array("Route tags (default: " + defaultValueMacro + ")"),
    converter = Array(classOf[OptStringSetConverter]),
    paramLabel = "<tags>")
  var routeTags: Option[StringSetWrapper] = None

  @CommandLineOption(
    names = Array("--ua_tags"),
    description = Array("Route UA tags (default: " + defaultValueMacro + ")"),
    converter = Array(classOf[OptStringSetConverter]),
    paramLabel = "<ua_tags>")
  var uaTags: Option[StringSetWrapper] = None

  @CommandLineOption(
    names = Array("--scopes"),
    description = Array("Route scopes (default: " + defaultValueMacro + ")"),
    converter = Array(classOf[OptStringSetConverter]),
    paramLabel = "<scopes>")
  var scopes: Option[StringSetWrapper] = None

  @CommandLineOption(
    names = Array("--decider"),
    description = Array("Route decider (default: " + defaultValueMacro + ")"),
    converter = Array(classOf[OptStringConverter]),
    defaultValue = "")
  var decider: Option[String] = None

  @CommandLineOption(
    names = Array("--ldap_owners"),
    description = Array("Route LDAP owners (default: " + defaultValueMacro + ")"),
    converter = Array(classOf[OptStringSetConverter]),
    paramLabel = "<ldap_owners>")
  var ldapOwners: Option[StringSetWrapper] = None

  @CommandLineOption(
    names = Array("--priority"),
    description = Array("Route priority (default: " + defaultValueMacro + ")"),
    converter = Array(classOf[OptIntConverter]),
    paramLabel = "<priority>")
  var priority: Option[Int] = None

  @CommandLineOption(
    names = Array("--rate_limit"),
    description = Array(
      "Route rate limit, use 0 if you need 'unlimited' (default: " + defaultValueMacro + ")"),
    paramLabel = "<rate_limit>")
  var rateLimit: Int = _

  @CommandLineOption(
    names = Array("--timeout_ms"),
    description = Array("Route request timeout, ms (default: " + defaultValueMacro + ")"),
    converter = Array(classOf[OptIntConverter]),
    paramLabel = "<timeout_ms>")
  var timeoutMs: Option[Int] = None

  @CommandLineOption(
    names = Array("--experiment_buckets"),
    description = Array("Route experiment buckets (default: " + defaultValueMacro + ")"),
    converter = Array(classOf[OptExperimentBucketSetConverter]),
    paramLabel = "<experiment_buckets>")
  var experimentBuckets: Option[ExperimentBucketSetWrapper] = None
}
