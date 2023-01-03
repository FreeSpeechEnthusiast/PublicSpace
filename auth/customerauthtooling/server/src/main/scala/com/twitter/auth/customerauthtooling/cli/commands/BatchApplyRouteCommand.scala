package com.twitter.auth.customerauthtooling.cli.commands

import com.twitter.auth.customerauthtooling.thriftscala.AppliedAction
import com.twitter.auth.customerauthtooling.thriftscala.ApplyRoutesRequest
import com.twitter.auth.customerauthtooling.thriftscala.PartialRouteInfo
import com.twitter.auth.customerauthtooling.thriftscala.RouteDraft
import com.twitter.auth.customerauthtooling.thriftscala.BatchRouteDraft
import com.twitter.auth.customerauthtooling.cli.commands.converters.AuthTypeSetConverter
import com.twitter.auth.customerauthtooling.cli.commands.converters.BooleanConverter
import com.twitter.auth.customerauthtooling.cli.commands.converters.OptExperimentBucketSetConverter
import com.twitter.auth.customerauthtooling.cli.commands.converters.OptIntConverter
import com.twitter.auth.customerauthtooling.cli.commands.converters.OptLongSetConverter
import com.twitter.auth.customerauthtooling.cli.commands.converters.OptRequestMethodConverter
import com.twitter.auth.customerauthtooling.cli.commands.converters.OptStringConverter
import com.twitter.auth.customerauthtooling.cli.commands.converters.OptStringSetConverter
import com.twitter.auth.customerauthtooling.cli.commands.converters.StringSetConverter
import com.twitter.util.Await
import com.twitter.util.Future
import com.twitter.util.Return
import com.twitter.util.Throw
import com.twitter.util.Try
import java.io.File
import java.io.FileReader
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import picocli.CommandLine.Command
import picocli.CommandLine.Help.Ansi
import picocli.CommandLine.{Option => CommandLineOption}
import scala.collection.JavaConverters._

@Command(
  name = "apply",
  description = Array("Apply changes to routes from CSV file and create missing routes"),
  mixinStandardHelpOptions = true)
class BatchApplyRouteCommand
    extends BaseCustomerAuthToolingCommand
    with RouteBuilder
    with AutomaticDeciderOption {

  @CommandLineOption(
    names = Array("--file"),
    required = true,
    description = Array("Path to a CSV file with route parameters"))
  var file: String = _

  @CommandLineOption(
    names = Array("--ignoreInvalid"),
    required = false,
    description = Array("Ignore invalid records"),
    defaultValue = "true",
    converter = Array(classOf[BooleanConverter]))
  var ignoreInvalid: Boolean = true

  @CommandLineOption(
    names = Array("--ignoreErrors"),
    required = false,
    description = Array("Ignore server side errors"),
    defaultValue = "true",
    converter = Array(classOf[BooleanConverter]))
  var ignoreErrors: Boolean = true

  private val optLongSetConverter = new OptLongSetConverter()
  private val optStringSetConverter = new OptStringSetConverter()
  private val authTypeSetConverter = new AuthTypeSetConverter()
  private val optRequestMethodConverter = new OptRequestMethodConverter()
  private val optStringConverter = new OptStringConverter()
  private val optIntConverter = new OptIntConverter()
  private val optExperimentBucketSetConverter = new OptExperimentBucketSetConverter()
  private val stringSetConverter = new StringSetConverter()

  private case class RouteInformationFromCSVRow(
    rowId: Long,
    routeIdOpt: Option[String],
    rawCSVData: Option[CSVRecord])

  private def loadRoutesFromCSV() = {
    val reader = new FileReader(new File(file))
    val parser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader()).getRecords.asScala
    parser
      .map { r =>
        val routeIdOpt = Try {
          routeIdBasedOnProvidedIdentifiers(
            id = Try {
              r.get("id")
            }.toOption,
            path = Try {
              r.get("path")
            }.toOption,
            cluster = Try {
              r.get("cluster")
            }.toOption,
            method = Try {
              optRequestMethodConverter.convert(r.get("method"))
            }.toOption.flatten
          )
        }.toOption
        RouteInformationFromCSVRow(
          rowId = r.getRecordNumber,
          routeIdOpt = routeIdOpt,
          rawCSVData =
            if (routeIdOpt.isEmpty) None
            else {
              Some(r)
            }
        )
      }
  }

  private def newThriftRouteFromCSVRecord(csvRecord: CSVRecord): Option[PartialRouteInfo] = {
    Try {
      buildPartialRouteFromOptions(
        id = Try { csvRecord.get("id") }.toOption,
        domains = Try { stringSetConverter.convert(csvRecord.get("domains")) }.toOption,
        authTypes = Try { authTypeSetConverter.convert(csvRecord.get("auth_types")) }.toOption,
        path = Try { csvRecord.get("path") }.toOption,
        cluster = Try { csvRecord.get("cluster") }.toOption,
        project = Try { csvRecord.get("project") }.toOption,
        method = Try {
          optRequestMethodConverter.convert(csvRecord.get("method"))
        }.toOption.flatten,
        dps = Try {
          optLongSetConverter.convert(csvRecord.get("dps"))
        }.toOption.flatten,
        userRoles = Try {
          optStringSetConverter.convert(csvRecord.get("user_roles"))
        }.toOption.flatten,
        routeFlags = Try {
          optStringSetConverter.convert(csvRecord.get("flags"))
        }.toOption.flatten,
        featurePermissions = Try {
          optStringSetConverter.convert(csvRecord.get("fps"))
        }.toOption.flatten,
        subscriptionPermissions = Try {
          optStringSetConverter.convert(csvRecord.get("sps"))
        }.toOption.flatten,
        routeTags = Try {
          optStringSetConverter.convert(csvRecord.get("tags"))
        }.toOption.flatten,
        uaTags = Try {
          optStringSetConverter.convert(csvRecord.get("ua_tags"))
        }.toOption.flatten,
        scopes = Try {
          optStringSetConverter.convert(csvRecord.get("scopes"))
        }.toOption.flatten,
        decider = Try {
          optStringConverter.convert(csvRecord.get("decider"))
        }.toOption.flatten,
        ldapOwners = Try {
          optStringSetConverter.convert(csvRecord.get("ldap_owners"))
        }.toOption.flatten,
        priority = Try {
          optIntConverter.convert(csvRecord.get("priority"))
        }.toOption.flatten,
        rateLimit = Try {
          optIntConverter.convert(csvRecord.get("rate_limit"))
        }.toOption.flatten,
        timeoutMs = Try {
          optIntConverter.convert(csvRecord.get("timeout_ms"))
        }.toOption.flatten,
        experimentBuckets = Try {
          optExperimentBucketSetConverter.convert(csvRecord.get("experiment_buckets"))
        }.toOption.flatten,
      )
    }.toOption
  }

  override def call(): Unit = {
    Await.result {
      // load csv file
      val loadedRouteInfo = Try {
        loadRoutesFromCSV()
      } match {
        case Throw(e) =>
          println(Ansi.AUTO.string("@|bold,red Unable to load provided CSV file!|@"))
          println(Ansi.AUTO.string("@|italic,yellow " + e.getMessage + "|@"))
          return
        case Return(r) => r
      }
      val invalidRecords =
        loadedRouteInfo.filter(r => r.routeIdOpt.isEmpty || r.rawCSVData.isEmpty)
      val hasInvalid = invalidRecords.nonEmpty
      if (hasInvalid) {
        println(Ansi.AUTO.string("@|bold,red Some records are invalid!|@"))
      }
      invalidRecords.foreach {
        case RouteInformationFromCSVRow(rowId, None, _) =>
          println(s"Row $rowId is invalid, 'id' or 'path' with 'cluster' must be provided")
        case RouteInformationFromCSVRow(rowId, _, None) =>
          println(s"Row $rowId is invalid, incomplete or invalid data provided")
        case _ =>
      }
      if (hasInvalid && !ignoreInvalid) {
        return
      } else {
        // collect valid records and transform them to Map(routeId -> routeInfo)
        val routesMap = loadedRouteInfo.collect {
          case RouteInformationFromCSVRow(_, Some(routeId), Some(routeInfo)) => (routeId, routeInfo)
        }.toMap
        if (routesMap.isEmpty) {
          println(Ansi.AUTO.string("@|bold,red No valid routes were found in the file!|@"))
          return
        }
        customerAuthToolingService
          .applyRoutes(ApplyRoutesRequest(
            routes = routesMap
            // transform to thrift
              .collect {
                case (_, csvRecord) =>
                  newThriftRouteFromCSVRecord(csvRecord) match {
                    case Some(partialRouteInfo) => Some(partialRouteInfo)
                    case _ => None
                  }
              }
              // filter transformed
              .collect { case Some(partialRouteInfo) => partialRouteInfo }.toSet,
            automaticDecider = automaticDecider,
            ignoreInvalid = Option(ignoreInvalid),
            ignoreErrors = Option(ignoreErrors)
          )).map {
            _.batchRouteDraft match {
              case None =>
              case Some(
                    BatchRouteDraft(
                      updated,
                      inserted,
                      ignoredInvalid,
                      ignoredDueToErrors,
                      unchanged,
                      routeDraftsOpt,
                      wasStopped,
                      errorsOpt,
                      warningsOpt,
                      messagesOpt)) =>
                println(
                  Ansi.AUTO.string(
                    "@|yellow,bold Invalid: " + (ignoredInvalid + invalidRecords.size) + "|@"))
                println(Ansi.AUTO.string("@|yellow,bold Errors: " + ignoredDueToErrors + "|@"))
                println(Ansi.AUTO.string("@|green,bold Updated: " + updated + "|@"))
                println(Ansi.AUTO.string("@|green,bold Created: " + inserted + "|@"))
                println(Ansi.AUTO.string("@|green,bold Unchanged: " + unchanged + "|@"))
                routeDraftsOpt match {
                  case Some(routeDrafts) =>
                    routeDrafts.foreach {
                      case RouteDraft(uuid, routeId, Some(action))
                          if action == AppliedAction.Update =>
                        println(s"Route was updated, uuid is: ${uuid}, route id is ${routeId}")
                      case RouteDraft(uuid, routeId, Some(action))
                          if action == AppliedAction.Insert =>
                        println(s"Route was created, uuid is: ${uuid}, route id is ${routeId}")
                      case RouteDraft(uuid, routeId, Some(action))
                          if action == AppliedAction.Nothing =>
                        println(s"Route was untouched, uuid is: ${uuid}, route id is ${routeId}")
                    }
                  case None =>
                  // operation failed
                }
                errorsOpt.getOrElse(Seq()).foreach { msg =>
                  println(Ansi.AUTO.string("@|red,bold " + msg + "|@"))
                }
                warningsOpt.getOrElse(Seq()).foreach { msg =>
                  println(Ansi.AUTO.string("@|yellow,bold " + msg + "|@"))
                }
                messagesOpt.getOrElse(Seq()).foreach { msg =>
                  println(Ansi.AUTO.string(msg))
                }
              case _ =>
                println(Ansi.AUTO.string("@|bold,red Unable to apply the batch!|@"))
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
}
