package com.twitter.auth.scopeverification

import com.google.inject.Module
import com.twitter.auth.authenticationtype.thriftscala.AuthenticationType
import com.twitter.finatra.mtls.app.Mtls
import com.twitter.inject.annotations.Flags
import com.twitter.inject.app.App
import com.twitter.inject.modules.StatsReceiverModule
import com.twitter.tfe.core.routingng.NgRoute
import com.twitter.server.AdminHttpServer
import com.twitter.server.Stats
import com.twitter.tfe.core.routingng.RouteFlags
import com.twitter.util.logging.Logger

object ScopeVerificationMain extends ScopeVerification

class ScopeVerification extends App with Mtls with Stats with AdminHttpServer {

  flag[Int]("check_frequency", 10, "Run the checks every X mins.")

  private[this] val log = Logger("ScopeVerificationLogger")

  private[this] val scoped = statsReceiver.scope("scope_verification")
  // step 1
  private[this] val routeIdToNgRouteMappingCounter =
    scoped.counter("route_id_to_ngroute_mapping_counter")
  // step 2
  private[this] val routeIdToScopeMappingCounter =
    scoped.counter("route_id_to_scope_mapping_counter")
  // step 3
  private[this] val invalidRouteIdCounter = scoped.counter("invalid_route_id_counter")
  // step 4
  private[this] val routeIdToAnnotatedDataPermissionMappingCounter =
    scoped.counter("route_id_to_annotated_data_permission_mapping_counter")
  // step 5
  private[this] val invalidScopeCounter = scoped.counter("invalid_scope_counter")
  // step 6
  private[this] val routeIdToDataPermissionsCounter =
    scoped.counter("route_id_to_data_permissions_counter")
  private[this] val invalidScopeToDataPermissionCounter =
    scoped.counter("invalid_scope_to_data_permissions_counter")
  // step 7
  private[this] val invalidDataPermissionsCounter =
    scoped.counter("invalid_data_permissions_counter")
  // step 8
  private[this] val totalOauth2NgRouteCounter =
    scoped.counter("total_oauth2_ngroute_counter")
  private[this] val invalidOauth2NgRouteCounter =
    scoped.counter("invalid_oauth2_ngroute_counter")
  private[this] val invalidOAuth2ScopeEnforcementPolicyCounter =
    scoped.counter("invalid_oauth2_scope_enforcement_policy_counter")
  private[this] val invalidOAuth2DataPermissionPolicyCounter =
    scoped.counter("invalid_oauth2_data_permission_policy_counter")

  override val modules: Seq[Module] = Seq(
    StatsReceiverModule
  )

  override protected def run(): Unit = {
    val checkFrequency: Int = injector.instance[Int](Flags.named("check_frequency"))
    // wait for Configbus client to finish first load
    Thread.sleep(10 * 1000)
    while (true) {
      var allValid = true

      // 1. Load NgRoutes
      log.info("\n=============================")
      log.info("Step 1: Load Route Id to NgRoute Mapping.")
      val routeIdToNgRouteMapping: Map[String, NgRoute] = Policies.getRouteIdToNgRouteMapping
      routeIdToNgRouteMappingCounter.incr(routeIdToNgRouteMapping.keySet.size)
      log.info(s"OK. Successfully loaded ${routeIdToNgRouteMapping.keySet.size} NgRoutes.")

      // 2. Load Route ID to Scope Mappings
      log.info("\n=============================")
      log.info("Step 2: Load Route Id to Scope Mappings.")
      val routeIdToScopeMapping: Map[String, Set[String]] = Policies.getRouteIdToScopeMapping
      routeIdToScopeMappingCounter.incr(routeIdToScopeMapping.keySet.size)
      log.info(
        s"OK. Successfully loaded ${routeIdToScopeMapping.keySet.size} route id to scope mappings.")

      // 3. Verify Route ID to Scope Mappings
      log.info("\n=============================")
      log.info("Step 3: Verify Route Id to Scope Mappings.")
      var invalidRouteIdCount = 0
      routeIdToScopeMapping.keySet.foreach { routeId =>
        routeIdToNgRouteMapping.get(routeId) match {
          case Some(route) =>
            log.debug(s"Step 3: Verified Route Id: ${route.id}.")
          case _ =>
            invalidRouteIdCount += 1
            allValid = false
            invalidRouteIdCounter.incr()
            log.error(s"Step 3: Found INVALID Route Id: $routeId.")
        }
      }
      if (invalidRouteIdCount > 0) {
        log.info(s"FAILED. Found $invalidRouteIdCount invalid routes.")
      } else {
        log.info("OK.")
      }

      // 4. Route ID -> NgRoutes -> Annotated DPs
      log.info("\n=============================")
      log.info("Step 4: Load Route Id to Annotated Data Permissions Mapping.")
      val routeIdToAnnotatedDataPermissionMapping: Map[String, Set[Long]] =
        routeIdToScopeMapping.keySet.map { routeId =>
          val dpSet = routeIdToNgRouteMapping.get(routeId) match {
            case Some(ngRoute) =>
              // find annotated DPs which are NOT in testing mode
              ngRoute.dataPermissions.filter(!_.testing).map(_.id).toSet
            case _ =>
              Set[Long]()
          }
          routeId -> dpSet
        }.toMap
      routeIdToAnnotatedDataPermissionMappingCounter.incr(
        routeIdToAnnotatedDataPermissionMapping.keySet.size)
      log.info("OK.")

      // 5. Verify Scopes
      log.info("\n=============================")
      log.info("Step 5: Verify Scopes")
      var invalidScopeCount = 0
      routeIdToScopeMapping.foreach { pair =>
        pair._2.foreach { scope =>
          Policies.getAuthorizationScopeByScope(scope) match {
            case Some(a) =>
              log.debug(s"Step 4: Verified Scope: ${a.name}.")
            case _ =>
              invalidScopeCount += 1
              allValid = false
              invalidScopeCounter.incr()
              log.error(s"Step 4: Found INVALID Scope: $scope from Route ID: ${pair._1}.")
          }
        }
      }
      if (invalidScopeCount > 0) {
        log.info(s"FAILED. Found $invalidScopeCount invalid scopes.")
      } else {
        log.info("OK.")
      }

      // 6. Route ID -> Scopes -> DPs
      log.info("\n=============================")
      log.info("Step 6: Load Route Scope to Data Permission Mapping.")
      var invalidScopeToDataPermissionCount = 0
      val routeIdToDataPermissions: Map[String, Set[Long]] = routeIdToScopeMapping.map { pair =>
        val dpSet = scala.collection.mutable.Set[Long]()
        pair._2.map { scope =>
          val dps = Policies.getDataPermissionsByScope(scope)
          if (dps.isEmpty) {
            // TODO: Enable this check after we deprecate Scope Enforcement
            // invalidScopeToDataPermissionCount += 1
            // allValid = false
            invalidScopeToDataPermissionCounter.incr()
            log.debug(s"Step 6: Found empty Scope to Data Permission Mapping: $scope.")
          }
          dpSet ++= dps
        }
        pair._1 -> dpSet.toSet
      }.toMap
      routeIdToDataPermissionsCounter.incr(routeIdToDataPermissions.keySet.size)
      if (invalidScopeToDataPermissionCount > 0) {
        log.info(
          s"FAILED. Found $invalidScopeToDataPermissionCount routes with invalid scope to Data Permission mapping.")
      } else {
        log.info("OK.")
      }

      // 7. Compare Data Permissions between Route ID -> Scopes -> DPs vs. Route ID -> NgRoutes -> Annotated DPs
      log.info("\n=============================")
      log.info(
        "Step 7: Compare Data Permissions between Route ID -> Scopes -> DPs vs. Route ID -> NgRoutes -> Annotated DPs.")
      var invalidDataPermissionComparisonCount = 0
      routeIdToAnnotatedDataPermissionMapping.foreach { pair =>
        val routeId = pair._1
        val annotatedDataPermissions = pair._2
        val dataPermissionsFromScopes = routeIdToDataPermissions.getOrElse(routeId, Set[Long]())
        val dpDiffSet = annotatedDataPermissions -- dataPermissionsFromScopes
        if (dpDiffSet.nonEmpty) {
          invalidDataPermissionComparisonCount += 1
          allValid = false
          invalidDataPermissionsCounter.incr()
          log.error(
            s"Step 7: Found Data Permission Mismatch Route ID: $routeId, Mismatched Data Permissions: ${dpDiffSet
              .toString()}.")
        }
      }
      if (invalidDataPermissionComparisonCount > 0) {
        log.info(
          s"FAILED. Found $invalidDataPermissionComparisonCount routes with Data Permission mismatch.")
      } else {
        log.info("OK.")
      }

      // 8. Verify NgRoutes with OAuth2 Annotated
      log.info("\n=============================")
      log.info("Step 8: Verify NgRoutes with OAuth2 auth type.")
      var totalOAuth2RouteCount: Int = 0
      var invalidOAuth2RouteCount: Int = 0
      routeIdToNgRouteMapping.values.foreach { ngRoute =>
        if (ngRoute.routeAuthTypes.contains(AuthenticationType.Oauth2)) {
          totalOAuth2RouteCount += 1
          val routeId = ngRoute.id
          // NgRoutes require scope enforcement in TFE
          if (ngRoute.routeFlags.contains(RouteFlags.Flag.ALLOW_V2_NG_ROUTE_SCOPE_ENFORCEMENT)) {
            routeIdToScopeMapping.get(routeId) match {
              case None =>
                invalidOAuth2ScopeEnforcementPolicyCounter.incr()
                log.error(
                  s"Step 8: Found Route Id: $routeId with OAUTH_2 and ALLOW_V2_NG_ROUTE_SCOPE_ENFORCEMENT with NO scope policy.")
                invalidOAuth2RouteCount += 1
                allValid = false
              case Some(scopes) if scopes.isEmpty =>
                invalidOAuth2ScopeEnforcementPolicyCounter.incr()
                log.error(
                  s"Step 8: Found Route Id: $routeId with OAUTH_2 and ALLOW_V2_NG_ROUTE_SCOPE_ENFORCEMENT with EMPTY scope policy.")
                invalidOAuth2RouteCount += 1
                allValid = false
              case _ =>
            }
          }
          // NgRoutes require DP enforcement in TFE
          else {
            if (ngRoute.dataPermissions.isEmpty) {
              invalidOAuth2DataPermissionPolicyCounter.incr()
              log.error(
                s"Step 8: Found Route Id: $routeId with OAUTH_2 with NO Data Permission policy.")
              invalidOAuth2RouteCount += 1
              allValid = false
            }
          }
        }
      }
      totalOauth2NgRouteCounter.incr(totalOAuth2RouteCount)
      invalidOauth2NgRouteCounter.incr(invalidOAuth2RouteCount)
      if (invalidOAuth2RouteCount > 0) {
        log.info(
          s"FAILED. Found $totalOAuth2RouteCount routes with OAuth2 annotated. Found $invalidOAuth2RouteCount routes with invalid policy.")
      } else {
        log.info(
          s"OK. Successfully verified $totalOAuth2RouteCount NgRoutes with OAuth2 annotated.")
      }

      // IMPORTANT: The cron job has to be long-lived to get stats subscribed
      // https://confluence.twitter.biz/pages/viewpage.action?spaceKey=OBSERVE&title=Automatic+Observability
      // checks every 10 mins
      Thread.sleep(checkFrequency * 60 * 1000)
      if (!allValid) {
        exitOnError("Invalid Customer Auth policy detected. Exiting the job to reset stats ...")
      }
    }
  }
}
