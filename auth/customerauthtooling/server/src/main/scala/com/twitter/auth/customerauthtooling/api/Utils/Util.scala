package com.twitter.auth.customerauthtooling.api.Utils

import com.fasterxml.jackson.databind.JsonNode
import com.twitter.auth.customerauthtooling.api.models.RecommenderRoute
import com.twitter.auth.customerauthtooling.api.models.RouteStats
import com.twitter.finagle.Http
import com.twitter.finagle.http.RequestBuilder
import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule
import com.twitter.logging.Logger
import com.twitter.tfe.core.routingng.NgRoute
import com.twitter.tfe.core.routingng.RouteFlags
import com.twitter.util.Future
import java.io.Reader
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import scala.collection.JavaConverters._
import scala.collection.mutable

object Util {
  // Convert a path with format /a/:b/c to /a/{b}/c
  def normalizePath(path: String): String = {
    path
      .split('/')
      .map(segment =>
        if (segment.startsWith(":")) {
          s"{${segment.slice(1, segment.length)}}"
        } else {
          segment
        }).mkString("/")
  }

  // Some segments are improperly named like version_id
  def normalizePathSegments(path: String): String = {
    path
      .replaceAll("version_id", "version")
      .replaceAll("version_number", "version")
  }

  // Convert a String with format "[a, b, c]" to Seq["a", "b", "c"]
  def stringArrayToSeq(stringArray: String): Seq[String] = {
    stringArray
      .slice(1, stringArray.length - 1)
      .split(", ")
      .filter(s => s != "")
  }

  // Read csv from a given reader
  def readCsvFile(reader: Reader): Seq[CSVRecord] = {
    new CSVParser(reader, CSVFormat.DEFAULT.withHeader()).getRecords.asScala
  }

  // NGRoutes defines a decider for each endpoint, this function builds a unique decider name
  // for a path with format /a/{b}/c -> ads-api-method-a-b-c
  def deciderForRoute(method: String, path: String): String = {
    s"ads-api-${method.toLowerCase()}-${path.split('/').filter(_ != "").mkString("-")}"
      .replaceAll("\\{", "")
      .replaceAll("}", "")
  }

  // An abstract representation of an endpoint path.
  //
  // When comparing routes, its important to compare static segments by content and location
  // and dynamic segments by location only.
  //
  // /foo/{bar}/baz -> Seq(StaticSegment, DynamicSegment, StaticSegment)
  sealed trait Segment

  // Track the location of the dynamic segment in the path.
  case class DynamicSegment(position: Long) extends Segment

  // Track the name and location of the static segment in the path.
  case class StaticSegment(position: Long, name: String) extends Segment

  // Convert a path with format /foo/{bar}/baz/{bleh} to a Seq[Segment]
  def toSegments(path: String): Seq[Segment] = {
    path
      .split("/").filter(_ != "").zipWithIndex.map {
        case (segment, i) =>
          // is a dynamic segment
          if (segment.startsWith("{") && segment.endsWith("}")) {
            DynamicSegment(position = i)
          } else {
            StaticSegment(position = i, name = segment)
          }
      }.toSeq
  }

  // Fetch the registry file for a service and parse it as JSON
  def getRegistryJson(
    host: String,
    path: String
  )(
    implicit log: Logger
  ): Future[JsonNode] = {
    Http
      .newClient(host + ":80")
      .toService(RequestBuilder().url("http://" + host + path).buildGet())
      .map(res => ScalaObjectMapperModule.objectMapper.reader.readTree(res.contentString))
      .onFailure { e =>
        log.error(s"$e")
        System.exit(1)
      }
  }

  // Get routes listed in the data permission recommender output
  def getDPMaps(records: Seq[CSVRecord]): mutable.Map[Long, String] = {
    val map = scala.collection.mutable.Map[Long, String]()
    records.foreach(record => {
      val dataPermissionIds: Long = record.get("dp_id").toLong
      val dpName: String = record.get("dp_name")
      map += dataPermissionIds -> dpName
    })
    map
  }

  // Get routes listed in the data permission recommender output
  def getScopeToDPMap(records: Seq[CSVRecord]): mutable.Map[String, Seq[Long]] = {
    val map = scala.collection.mutable.Map[String, Seq[Long]]()
    records.foreach(record => {
      val dataPermissionIds = stringArrayToSeq(record.get("dps")).map(_.toLong)
      val scopeName: String = record.get("scope_name")
      map += scopeName -> dataPermissionIds
    })
    map
  }

  def getRecommenderRoutes(records: Seq[CSVRecord]): Seq[RecommenderRoute] = {
    records.map(record => {
      val pathWithSlash = normalizePathSegments(record.get("normalized_http_path"))
      val path = pathWithSlash.dropRight(1)

      val method = record.get("http_method")
      val dataPermissionIds: Set[Long] = stringArrayToSeq(record.get("dp_id")).map(_.toLong).toSet
      val clusters: Seq[String] = stringArrayToSeq(record.get("tfe_http_cluster"))
      val dataClassifications: Seq[String] =
        stringArrayToSeq(record.get("dp_object_classification"))

      val (cluster, timeout) = if (clusters.contains("ads_api_mtls")) {
        (Some("ads_api_mtls"), 5000)
      } else if (clusters.contains("ads_api_15s")) {
        (Some("ads_api_mtls"), 15000)
      } else if (clusters.contains("advertiser_api_mtls")) {
        (Some("advertiser_api_mtls"), 5000)
      } else {
        (None, 5000)
      }

      RecommenderRoute(path, method, dataPermissionIds, dataClassifications, cluster, timeout)
    })
  }

  def isHighRiskRoute(route: RecommenderRoute): Boolean = {
    route.dataClassifications.contains("Cornerstone") || route.dataClassifications.contains("High")
  }

  def dpIDToNameMap(dpIds: Seq[Long], dpMap: mutable.Map[Long, String]): Map[Long, String] = {
    dpIds.map(d => (d, dpMap.getOrElse(d, ""))).toMap
  }

  def generateNGRouteStats(
    dprMap: Map[String, RecommenderRoute],
    ngrMap: Map[String, NgRoute],
    scopeToDPMap: mutable.Map[String, Seq[Long]],
    routeStatsMap: mutable.Map[String, RouteStats]
  ): Future[Boolean] = {

    // 1. Generate route stat entry
    ngrMap.foreach { t =>
      val matchedNGRoute = t._2
      routeStatsMap +=
        (matchedNGRoute.id -> RouteStats(
          id = matchedNGRoute.id,
          isHighRiskRoute = false,
          enforcedDPs = false,
          annotatedFPs = -1,
          anyFailOpenFlags = false,
          annotatedDPs = -1,
          missingDpsAsPerRecommender = Seq(),
          failOpenFlags = Set(),
          supportedAuthTypes = Set(),
          accessibleByScopes = Set(),
          annotatedDPIds = Seq()
        ))
    }
    // 2. populate stats
    populateStats(dprMap, ngrMap, scopeToDPMap, routeStatsMap)

    // 3. print stats
    print("routeStatsMap")
    printRouteStatsMap(routeStatsMap)
    Future.True
  }

  def printRouteStatsMap(routeStatsMap: mutable.Map[String, RouteStats]) = {
    // @formatter:off
    println(
      s"id//,isHighRiskRoute//,enforcedDPs//,annotatedFPs//,anyFailOpenFlags//,annotatedDPs//,accessibleByScopes//,missingDpsAsPerRecommender//,failOpenFlags//,supportedAuthTypes//,accessibleByScopes"
    )
    routeStatsMap.foreach(t => println(s"${t._2.id}//, ${t._2.isHighRiskRoute}//, ${t._2.enforcedDPs}//, ${t._2.accessibleByScopes.mkString(",")} //, ${t._2.annotatedFPs}//, ${t._2.anyFailOpenFlags}//, ${t._2.annotatedDPs}//, ${t._2.missingDpsAsPerRecommender.mkString(",")}//, ${t._2.failOpenFlags.mkString(",")}//, ${t._2.supportedAuthTypes.mkString(",")}"))
    // @formatter:on
  }

  def populateStats(
    dprMap: Map[String, RecommenderRoute],
    ngrMap: Map[String, NgRoute],
    scopeToDPMap: mutable.Map[String, Seq[Long]],
    routeStatsMap: mutable.Map[String, RouteStats]
  ) = {

    var dps = scala.collection.mutable.Set[Long]()
    var enforcedDps = scala.collection.mutable.Set[Long]()

    ngrMap.foreach { t =>
      val matchedNGRoute = t._2
      val key = matchedNGRoute.requestMethod.name + "->" + matchedNGRoute.normalizedPath
      val matchedDPRecommenderRouteKey = dprMap.contains(key)
      matchedDPRecommenderRouteKey && isHighRiskRoute(dprMap(key))
      // populate high risk route stat
      routeStatsMap(matchedNGRoute.id).isHighRiskRoute =
        matchedDPRecommenderRouteKey && isHighRiskRoute(dprMap(key))
      // populate annotated dps
      routeStatsMap(matchedNGRoute.id).annotatedDPs = matchedNGRoute.dataPermissions.size
      // populate annotated fps
      routeStatsMap(matchedNGRoute.id).annotatedFPs = matchedNGRoute.featurePermissions.size
      // populate any fail open route flag
      routeStatsMap(matchedNGRoute.id).anyFailOpenFlags = anyFailOpenFlag(matchedNGRoute)
      // populate enforced DP flag
      routeStatsMap(matchedNGRoute.id).enforcedDPs =
        !matchedNGRoute.routeFlags.contains(RouteFlags.Flag.FAIL_OPEN_DATA_PERMISSIONS)
      // annotate dps
      routeStatsMap(matchedNGRoute.id).annotatedDPIds =
        matchedNGRoute.dataPermissions.map(t => t.id).toList.sorted
      // populate fail open flags
      routeStatsMap(matchedNGRoute.id).failOpenFlags = matchedNGRoute.routeFlags.map(t => t.name())
      // populate auth type flags
      routeStatsMap(matchedNGRoute.id).failOpenFlags =
        matchedNGRoute.routeAuthTypes.map(t => t.name)
      // populateDPDiff
      populateDPDiff(ngrMap, dprMap, routeStatsMap)
      // populateAccessibleByScopes
      populateAccessibleByScopes(ngrMap, scopeToDPMap, routeStatsMap)

      // populate enforced DPs & Dps list at the TFE level
      if (matchedNGRoute.routeFlags.contains(RouteFlags.Flag.FAIL_OPEN_DATA_PERMISSIONS)) {
        dps ++== matchedNGRoute.dataPermissions.map(t => t.id)
      } else {
        enforcedDps ++== matchedNGRoute.dataPermissions.map(t => t.id)
      }
    }
  }

  def anyFailOpenFlag(route: NgRoute) = {
    route.routeFlags.contains(RouteFlags.Flag.FAIL_OPEN_DATA_PERMISSIONS) ||
    route.routeFlags.contains(RouteFlags.Flag.FAIL_OPEN_AUTHENTICATION) ||
    route.routeFlags.contains(RouteFlags.Flag.FAIL_OPEN_SUBSCRIPTION_PERMISSIONS) ||
    route.routeFlags.contains(RouteFlags.Flag.FAIL_OPEN_FEATURE_PERMISSIONS)
  }

  def populateDPDiff(
    ngrMap: Map[String, NgRoute],
    dprMap: Map[String, RecommenderRoute],
    routeStatsMap: mutable.Map[String, RouteStats]
  ) = {
    ngrMap.foreach { t =>
      val matchedNgRoute: NgRoute = t._2
      val ngrDPs: Set[Long] = matchedNgRoute.dataPermissions.map(t => t.id)
      val keyInDPRMap = matchedNgRoute.requestMethod.name + "->" + matchedNgRoute.normalizedPath
      val dprDPs: Set[Long] = if (dprMap.contains(keyInDPRMap)) {
        dprMap(keyInDPRMap).dataPermissionIds
      } else {
        Set()
      }
      routeStatsMap(matchedNgRoute.id).missingDpsAsPerRecommender = dprDPs.diff(ngrDPs).toSeq.sorted
    }
  }

  def populateAccessibleByScopes(
    ngrMap: Map[String, NgRoute],
    scopeToDPMap: mutable.Map[String, Seq[Long]],
    routeStatsMap: mutable.Map[String, RouteStats]
  ) =
    ngrMap.foreach { t =>
      val matchedNgRoute: NgRoute = t._2
      val ngrDPs: Set[Long] = matchedNgRoute.dataPermissions.map(t => t.id)
      routeStatsMap(matchedNgRoute.id).accessibleByScopes = if (ngrDPs.isEmpty) {
        Set("Open For All")
      } else if (matchedNgRoute.routeFlags.contains(
          RouteFlags.Flag.ALLOW_V2_NG_ROUTE_SCOPE_ENFORCEMENT)) {
        Set("Scope Enforcement Protected Route")
      } else {
        scopeToDPMap.filter(sp => ngrDPs.subsetOf(sp._2.toSet)).keySet.toSet
      }
    }

  def printAdoptionStats(
    dprMap: Map[String, RecommenderRoute],
    ngrMap: Map[String, NgRoute]
  ) = {

    val matchedNGRoutes: Map[String, NgRoute] =
      ngrMap.filter { t =>
        val matchedNGRoute = t._2
        val matchedDPRecommenderRouteKey =
          dprMap.contains(matchedNGRoute.requestMethod.name + "->" + matchedNGRoute.normalizedPath)
        matchedDPRecommenderRouteKey
      }

    val highRiskNGRoutes: Map[String, NgRoute] =
      ngrMap.filter { t =>
        val matchedNGRoute = t._2
        val key = matchedNGRoute.requestMethod.name + "->" + matchedNGRoute.normalizedPath
        val matchedDPRecommenderRouteKey = dprMap.contains(key)
        matchedDPRecommenderRouteKey && isHighRiskRoute(dprMap(key))
      }

    var mismatchedDPRoutesCount = 0
    matchedNGRoutes.foreach { t =>
      val matchedNgRoute: NgRoute = t._2

      val ngrDPs: Set[Long] = matchedNgRoute.dataPermissions.map(t => t.id)
      val keyInDPRMap = matchedNgRoute.requestMethod.name + "->" + matchedNgRoute.normalizedPath
      val dprDPs: Set[Long] = dprMap(keyInDPRMap).dataPermissionIds
      if (!ngrDPs.equals(dprDPs)) {
        mismatchedDPRoutesCount = mismatchedDPRoutesCount + 1
      }
    }

    matchedNGRoutes.foreach { t =>
      val matchedNgRoute: NgRoute = t._2

      val ngrDPs: Set[Long] = matchedNgRoute.dataPermissions.map(t => t.id)
      val keyInDPRMap = matchedNgRoute.requestMethod.name + "->" + matchedNgRoute.normalizedPath
      val dprDPs: Set[Long] = dprMap(keyInDPRMap).dataPermissionIds
      if (!ngrDPs.equals(dprDPs)) {
        mismatchedDPRoutesCount = mismatchedDPRoutesCount + 1
      }
    }

    print("\n=============================")
    println("Mismatched DP Routes counts - " + mismatchedDPRoutesCount)
    println("Total Routes on NG Routes - " + highRiskNGRoutes.size)
    println("Total High Risk Routes on NG Routes - " + highRiskNGRoutes.size)
    println("Total Fail Close Routes - " + highRiskNGRoutes.size)

  }

}
