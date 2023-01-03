package com.twitter.auth.customerauthtooling.api.services

//import com.twitter.auth.customerauthtooling.api.Utils.Policies
//import com.twitter.auth.customerauthtooling.api.Utils.Util
//import com.twitter.auth.customerauthtooling.api.models.RecommenderRoute
//import com.twitter.auth.customerauthtooling.api.models.RouteStats
import com.twitter.inject.annotations.Flag
import com.twitter.auth.customerauthtooling.thriftscala.AdoptionStatsResponse
import com.twitter.auth.customerauthtooling.thriftscala.CustomerAuthToolingService.GenerateAdoptionStats
import com.twitter.scrooge.Request
import com.twitter.scrooge.Response
//import com.twitter.tfe.core.routingng.NgRoute
import com.twitter.util.Future
import com.twitter.util.logging.Logging
//import java.io.FileReader
import javax.inject.Inject
import javax.inject.Singleton
//import scala.collection.mutable

@Singleton
class AdoptionStatsService @Inject() (
  @Flag("dp.recommender.file.path") dpRecommenderFilePath: String,
  @Flag("dps.list.file.path") dpsListFilePath: String,
  @Flag("scope.to.dps.file.path") scopeToDpsFilePath: String)
    extends GenerateAdoptionStats.ReqRepServicePerEndpointServiceType
    with Logging {

  /*
  //TODO: Temporary disabled because it haven't worked yet
  //TODO: Move this to read from config bus and create/inject reader classes
  val dpRecommenderCSVRecords = Util.readCsvFile(new FileReader(dpRecommenderFilePath))
  val scopeToDPCSVRecords = Util.readCsvFile(new FileReader(scopeToDpsFilePath))

  val recommenderRoutes: Seq[RecommenderRoute] =
    Util.getRecommenderRoutes(dpRecommenderCSVRecords)
  val recommenderRoutesMap: Map[String, RecommenderRoute] =
    recommenderRoutes.map(r => (r.method + "->" + r.path, r)).toMap
  val dpCsvRecords = Util.readCsvFile(new FileReader(dpsListFilePath))
  val routeIdToNgRouteMapping: Map[String, NgRoute] = Policies.getRouteIdToNgRouteMapping
   */

  def apply(
    thriftRequest: Request[GenerateAdoptionStats.Args]
  ): Future[Response[AdoptionStatsResponse]] = {
    /*
    val request = thriftRequest.args.request

    if (request.shouldReportNGRouteStats) {
      Util
        .generateNGRouteStats(
          dprMap = recommenderRoutesMap,
          ngrMap = routeIdToNgRouteMapping,
          scopeToDPMap = Util.getScopeToDPMap(scopeToDPCSVRecords),
          routeStatsMap = mutable.Map[String, RouteStats]()
        ).map(_ => Util.printAdoptionStats(recommenderRoutesMap, routeIdToNgRouteMapping))
    } else {
      Util.printAdoptionStats(recommenderRoutesMap, routeIdToNgRouteMapping)
    }
     */

    val response = AdoptionStatsResponse(isDone = true)
    Future.value(Response(response))
  }

}
