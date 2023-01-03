package com.twitter.auth.ellverification

import com.google.inject.Module
import com.twitter.auth.authenticationtype.thriftscala.AuthenticationType
import com.twitter.finatra.mtls.app.Mtls
import com.twitter.inject.annotations.Flags
import com.twitter.inject.app.App
import com.twitter.inject.modules.StatsReceiverModule
import com.twitter.server.AdminHttpServer
import com.twitter.server.Stats
import com.twitter.tfe.core.routingng.AnnotatedDataPermission
import com.twitter.tfe.core.routingng.Enforced
import com.twitter.tfe.core.routingng.NgRoute
import com.twitter.tfe.core.routingng.RouteFlags
import com.twitter.util.logging.Logger

object EllVerificationAppMain extends EllVerificationApp

class EllVerificationApp extends App with Mtls with Stats with AdminHttpServer {

  override val modules: Seq[Module] = Seq(
    StatsReceiverModule
  )

  flag[Int]("check_frequency", 10, "Run the checks every X mins.")

  private[this] val RESTRICTED_SESSION_AUTH_TYPE = AuthenticationType.RestrictedSession
  private[this] val RESTRICTED_OAUTH2_SESSION_AUTH_TYPE = AuthenticationType.RestrictedOauth2Session
  private[this] val FAIL_OPEN_DATA_PERMISSION_FLAG = RouteFlags.Flag.FAIL_OPEN_DATA_PERMISSIONS
  private[this] val ELL_SCOPE = "email.lite.login"

  private[this] val log = Logger("EllVerificationAppLogger")

  private[this] val scoped = statsReceiver.scope("ell_verification")
  private[this] val routeIdToNgRouteMappingCounter =
    scoped.counter("route_id_to_ngroute_mapping_counter")
  private[this] val ellRoutesCounter =
    scoped.counter("ell_auth_type_annotated_routes")
  private[this] val ellRoutesWithFailOpenDataPermissionFlag =
    scoped.counter("ell_routes_with_fail_open_data_permission_flag")
  private[this] val ellRoutesWithOptionalDPs = scoped.counter("ell_routes_with_optional_dps")
  private[this] val routeAccessibleByELLScope =
    scoped.counter("route_accessible_by_ell_scope")

  def filterNGRoutesByELLAuthType(
    routeIdToNgRouteMapping: Map[String, NgRoute]
  ): Map[String, NgRoute] = {
    routeIdToNgRouteMapping.filter(r =>
      r._2.routeAuthTypes.contains(RESTRICTED_SESSION_AUTH_TYPE) ||
        r._2.routeAuthTypes.contains(RESTRICTED_OAUTH2_SESSION_AUTH_TYPE))
  }

  def filterELLNGRoutesByFailOpenDataPermissionFlag(
    routeIdToNgRouteMapping: Map[String, NgRoute]
  ): Map[String, NgRoute] = {
    routeIdToNgRouteMapping.filter(r => r._2.routeFlags.contains(FAIL_OPEN_DATA_PERMISSION_FLAG))
  }

  def filterELLNGRoutesByOptionalDataPermissions(
    routeIdToNgRouteMapping: Map[String, NgRoute]
  ): Map[String, NgRoute] = {
    routeIdToNgRouteMapping.filter { r => containsOptionalDPs(r._2.dataPermissions) }
  }

  def containsOptionalDPs(dps: Set[AnnotatedDataPermission]): Boolean = {
    dps.exists(dp => dp.state != Enforced)
  }

  def filterAccessibleNGRoutesByELLScope(
    routeIdToNgRouteMapping: Map[String, NgRoute]
  ): Map[String, NgRoute] = {
    val ellDPIds: Set[Long] = Policies.getDataPermissionsByScope(ELL_SCOPE)

    routeIdToNgRouteMapping.filter { r =>
      val routeDpIds = r._2.dataPermissions.map(dp => dp.id)
      //If we have one list subset of another list that means the route is accessible
      // ell scope. E.g. ELL scope: 1,2,3 => Route1: 1,2 => Route2: 1,2,3,4 = >Route: 3,5
      // Route1 & Router 2 are accessible through ng-route

      ellDPIds.forall(routeDpIds.contains) || routeDpIds.forall(ellDPIds.contains)
    }
  }

  override protected def run(): Unit = {
    val checkFrequency: Int = injector.instance[Int](Flags.named("check_frequency"))
    // wait for Configbus client to finish first load
    Thread.sleep(10 * 1000)

    while (true) {
      var allValid = true

      // 1. Load NgRoutes
      log.info("\n=============================")
      log.info("\n=============================")
      log.info("Step 1: Load Route Id to NgRoute Mapping.")
      val routeIdToNgRouteMapping: Map[String, NgRoute] = Policies.getRouteIdToNgRouteMapping
      routeIdToNgRouteMappingCounter.incr(routeIdToNgRouteMapping.keySet.size)
      log.info("OK.")

      // 2. Filter routes by ELL auth Types
      val ellRouteIdToNgRouteMapping = filterNGRoutesByELLAuthType(routeIdToNgRouteMapping)
      ellRoutesCounter.incr(ellRouteIdToNgRouteMapping.keySet.size)

      log.info("\n=============================")
      log.info("\n=============================")
      log.info("Step 2: Print ELL Supported Routes")
      ellRouteIdToNgRouteMapping.foreach { r =>
        log.info("RouteId => " + r._2.id)
      }

      // 3. Find  ELL routes with Fail open data permission flag
      log.info("\n=============================")
      log.info("\n=============================")
      log.info("Step 4: Find ELL Routes with Fail open DP flag")

      val dpFailOpenRouteIdToNgRouteMapping = filterELLNGRoutesByFailOpenDataPermissionFlag(
        ellRouteIdToNgRouteMapping)
      ellRoutesWithFailOpenDataPermissionFlag.incr(dpFailOpenRouteIdToNgRouteMapping.keySet.size)

      dpFailOpenRouteIdToNgRouteMapping.foreach { r =>
        allValid = false
        log.info("RouteId => " + r._2.id)
      }

      // 5. Find ELL routes with Optional DPs
      log.info("\n=============================")
      log.info("\n=============================")
      log.info("Step5: Find ELL Routes with Optional DPs")

      val optionalDPsELLNgRouteMapping = filterELLNGRoutesByOptionalDataPermissions(
        ellRouteIdToNgRouteMapping)
      ellRoutesWithOptionalDPs.incr(optionalDPsELLNgRouteMapping.keySet.size)

      optionalDPsELLNgRouteMapping.foreach { r =>
        allValid = false
        log.info("RouteId => " + r._2.id)
      }

      //6. Find Routes accessible through ELL scope
      log.info("\n=============================")
      log.info("\n=============================")
      log.info("Step6: Find Routes accessible through ELL scope")

      val accessibleNgRoutesByELLScope = filterAccessibleNGRoutesByELLScope(
        ellRouteIdToNgRouteMapping)
      routeAccessibleByELLScope.incr(accessibleNgRoutesByELLScope.keySet.size)

      accessibleNgRoutesByELLScope.foreach { r =>
        log.info("RouteId => " + r._2.id)
      }

      if (!allValid) {
        log.info("FAILED.")
      } else {
        log.info("OK.")
      }

      // IMPORTANT: The cron job has to be long-lived to get stats subscribed
      // https://confluence.twitter.biz/pages/viewpage.action?spaceKey=OBSERVE&title=Automatic+Observability
      // checks every 10 mins
      log.info("Sleeping for 10 mins, current Time -  " + System.currentTimeMillis())
      Thread.sleep(checkFrequency * 60 * 1000)
      log.info("Woke up after for 10 mins, current Time -  " + System.currentTimeMillis())
      if (!allValid) {
        // exitOnError("Invalid ELL Routes detected. Exiting the job to reset stats ...")
      }
    }
  }

}
