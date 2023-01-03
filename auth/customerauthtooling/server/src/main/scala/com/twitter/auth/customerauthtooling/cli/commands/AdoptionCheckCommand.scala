package com.twitter.auth.customerauthtooling.cli.commands

import com.twitter.auth.customerauthtooling.thriftscala.CheckAdoptionStatusRequest
import com.twitter.auth.customerauthtooling.thriftscala.CheckAdoptionStatusResponse
import com.twitter.auth.customerauthtooling.thriftscala.EndpointInfo
import com.twitter.auth.customerauthtooling.thriftscala.EndpointMetadata
import com.twitter.auth.customerauthtooling.thriftscala.RequestMethod
import com.twitter.auth.customerauthtooling.thriftscala.AdoptionRequirement
import com.twitter.auth.customerauthtooling.thriftscala.AdoptionStatus
import com.twitter.auth.customerauthtooling.cli.commands.converters.OptLongSetConverter
import com.twitter.auth.customerauthtooling.cli.commands.converters.LongSetWrapper
import com.twitter.auth.customerauthtooling.cli.commands.converters.OptBooleanConverter
import com.twitter.auth.customerauthtooling.cli.commands.converters.OptRequestMethodConverter
import com.twitter.util.Await
import com.twitter.util.Future
import picocli.CommandLine.Command
import picocli.CommandLine.Help.Ansi
import picocli.CommandLine.{Option => CommandLineOption}

@Command(
  name = "adoptioncheck",
  description = Array("Check an endpoint for custom auth adoption"),
  mixinStandardHelpOptions = true)
class AdoptionCheckCommand extends BaseCustomerAuthToolingCommand {

  @CommandLineOption(
    names = Array("--endpoint"),
    required = true,
    description = Array("Endpoint (default: " + defaultValueMacro + ")"),
    defaultValue = "")
  var endpoint: String = ""

  @CommandLineOption(
    names = Array("--method"),
    required = true,
    description = Array("Method (default: " + defaultValueMacro + ")"),
    converter = Array(classOf[OptRequestMethodConverter]),
    defaultValue = "GET")
  var method: Option[RequestMethod] = Some(RequestMethod.Get)

  @CommandLineOption(
    names = Array("--dps"),
    description = Array("Supplied data permissions (default: " + defaultValueMacro + ")"),
    converter = Array(classOf[OptLongSetConverter]),
    defaultValue = "")
  var suppliedDps: Option[LongSetWrapper] = None

  @CommandLineOption(
    names = Array("--override_found_in_tfe"),
    description = Array(
      "Allows to set the foundInTfeOverride checker override (default: " + defaultValueMacro + ")"),
    converter = Array(classOf[OptBooleanConverter]),
    defaultValue = "")
  var foundInTfeOverride: Option[Boolean] = None

  @CommandLineOption(
    names = Array("--override_is_internal"),
    description = Array(
      "Allows to set the isInternalEndpointOverride checker override (default: " + defaultValueMacro + ")"),
    converter = Array(classOf[OptBooleanConverter]),
    defaultValue = "")
  var isInternalEndpointOverride: Option[Boolean] = None

  @CommandLineOption(
    names = Array("--override_is_ng_route"),
    description = Array(
      "Allows to set the isNgRouteOverride checker override (default: " + defaultValueMacro + ")"),
    converter = Array(classOf[OptBooleanConverter]),
    defaultValue = "")
  var isNgRouteOverride: Option[Boolean] = None

  @CommandLineOption(
    names = Array("--override_requires_auth"),
    description = Array(
      "Allows to set the requiresAuthOverride checker override (default: " + defaultValueMacro + ")"),
    converter = Array(classOf[OptBooleanConverter]),
    defaultValue = "")
  var requiresAuthOverride: Option[Boolean] = None

  @CommandLineOption(
    names = Array("--override_is_app_only_or_guest"),
    description = Array(
      "Allows to set the isAppOnlyOrGuestOverride checker override (default: " + defaultValueMacro + ")"),
    converter = Array(classOf[OptBooleanConverter]),
    defaultValue = "")
  var isAppOnlyOrGuestOverride: Option[Boolean] = None

  @CommandLineOption(
    names = Array("--override_is_oauth1_or_session"),
    description = Array(
      "Allows to set the oauth1OrSessionOverride checker override (default: " + defaultValueMacro + ")"),
    converter = Array(classOf[OptBooleanConverter]),
    defaultValue = "")
  var oauth1OrSessionOverride: Option[Boolean] = None

  @CommandLineOption(
    names = Array("--override_already_adopted_dps"),
    description = Array(
      "Allows to set the alreadyAdoptedDpsOverride checker override (default: " + defaultValueMacro + ")"),
    converter = Array(classOf[OptBooleanConverter]),
    defaultValue = "")
  var alreadyAdoptedDpsOverride: Option[Boolean] = None

  private def printAdoptionCheckerResults(status: AdoptionStatus): Unit = {
    println(s"Found in TFE: ${status.foundInTfe}")
    println(s"Is Internal Endpoint: ${status.isInternalEndpoint}")
    println(s"Is NgRoute: ${status.isNgRoute}")
    println(s"Requires Auth: ${status.requiresAuth}")
    println(s"Is AppOnly or Guest: ${status.isAppOnlyOrGuest}")
    println(s"Oauth1 or Session: ${status.oauth1OrSession}")
    println(s"Already Adopted DPs: ${status.alreadyAdoptedDps}")
  }

  private def printMissingInformation(status: AdoptionStatus): Unit = {
    if (status.foundInTfe.isEmpty)
      println(
        s"Found in TFE: unable to determine, please provide override using --override_found_in_tfe")
    if (status.isInternalEndpoint.isEmpty)
      println(
        s"Is Internal Endpoint: unable to determine, please provide override using --override_is_internal")
    if (status.isNgRoute.isEmpty)
      println(
        s"Is NgRoute: unable to determine, please provide override using --override_is_ng_route")
    if (status.requiresAuth.isEmpty)
      println(
        s"Requires Auth: unable to determine, please provide override using --override_requires_auth")
    if (status.isAppOnlyOrGuest.isEmpty)
      println(
        s"Is AppOnly or Guest: unable to determine, please provide override using --override_is_app_only_or_guest")
    if (status.oauth1OrSession.isEmpty)
      println(
        s"Oauth1 or Session: unable to determine, please provide override using --override_is_oauth1_or_session")
    if (status.alreadyAdoptedDps.isEmpty)
      println(
        s"Already Adopted DPs: unable to determine, please provide override using --override_already_adopted_dps")
  }

  private def automaticallyDetectedResult(): Future[CheckAdoptionStatusResponse] = {
    customerAuthToolingService
      .checkAdoptionStatus(request = CheckAdoptionStatusRequest(endpointInfo =
        EndpointInfo(url = endpoint, method = method, metadata = None)))
  }

  private def resultWithOverrides(): Future[CheckAdoptionStatusResponse] = {
    customerAuthToolingService
      .checkAdoptionStatus(request = CheckAdoptionStatusRequest(endpointInfo = EndpointInfo(
        url = endpoint,
        method = method,
        metadata = Some(
          EndpointMetadata(
            suppliedDps = suppliedDps.map(_.get()),
            foundInTfeOverride = foundInTfeOverride,
            isInternalEndpointOverride = isInternalEndpointOverride,
            isNgRouteOverride = isNgRouteOverride,
            requiresAuthOverride = requiresAuthOverride,
            isAppOnlyOrGuestOverride = isAppOnlyOrGuestOverride,
            oauth1OrSessionOverride = oauth1OrSessionOverride,
            alreadyAdoptedDpsOverride = alreadyAdoptedDpsOverride
          ))
      )))
  }

  override def call(): Unit = {
    Await.result {
      Future
        .collect(List(automaticallyDetectedResult(), resultWithOverrides())).map {
          case Seq(automaticallyDetectedResultResponse, resultWithOverridesResponse) =>
            println("We automatically detected following properties of your endpoint:")
            printAdoptionCheckerResults(automaticallyDetectedResultResponse.adoptionStatus)
            println(
              "If something detected incorrectly you can set override. See `adoptioncheck --help` for options")
            println("The result below is based on automatic detection and provided overrides")
            resultWithOverridesResponse.adoptionStatus.requirement match {
              case AdoptionRequirement.RequiredCustomerAuthAndNgRoutesAdoption =>
                println(s"Customer Auth Data Permissions and NgRoute adoption is required")
              case AdoptionRequirement.RequiredNgRoutesAdoptionOnly =>
                println(s"NgRoute adoption only is required")
              case AdoptionRequirement.Required =>
                println(s"Customer Auth Data Permissions adoption is required")
              case AdoptionRequirement.NotRequired =>
                println(s"No actions required")
              case AdoptionRequirement.UnableToDetermine =>
                println(s"Unable to determine")
                printMissingInformation(resultWithOverridesResponse.adoptionStatus)
              case _ => println(s"Unexpected result")
            }
        }.rescue {
          case e: Exception =>
            println(Ansi.AUTO.string("@|bold,red Warning! Exception received!|@"))
            println(Ansi.AUTO.string("@|italic,yellow " + e.getMessage + "|@"))
            Future.Unit
        }
    }
  }

}
