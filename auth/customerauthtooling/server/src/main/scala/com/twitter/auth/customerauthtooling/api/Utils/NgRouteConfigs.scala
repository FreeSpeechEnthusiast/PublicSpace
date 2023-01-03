package com.twitter.auth.customerauthtooling.api.Utils

import com.twitter.configbus.subscriber.ConfigbusSubscriber
import com.twitter.configbus.subscriber.Subscription
import com.twitter.configbus.client.ConfigSource
import com.twitter.configbus.client.file.PollingConfigSourceBuilder
import com.twitter.finagle.stats.DefaultStatsReceiver
import com.twitter.finagle.util.DefaultTimer
import com.twitter.tfe.core.decider.StubTfeDecider
import com.twitter.tfe.core.host_aliases.HostAliases
import com.twitter.tfe.core.routingng.NgRoute
import com.twitter.tfe.core.routingng.RouteConfig
import com.twitter.tfe.core.routingng.RouteConfigParser
import com.twitter.util.Await
import com.twitter.util.Duration

object NgRouteConfigs {

  val Env = "prod"
  //val ConfigRoot = "/usr/local/config"
  val ConfigRoot = "/Users/akashp/workspace/config"
  //val RoutingConfigRoot = "/usr/local/config-routing"
  val RoutingConfigRoot = "/Users/akashp/workspace/config-routing"
  val CanaryRoutesPath = "/routing/tfe/ngroutes/canary"
  val ProductionRoutesPath = "/routing/tfe/ngroutes/production"
  val ScopeToDPMappingPath = "auth/oauth2/scope_to_dp_mapping.json"
  val ScopesPath = "auth/oauth2/scopes.json"
  val StatsReceiver = DefaultStatsReceiver.scope("configs")

  private val decider = new StubTfeDecider
  private val hostAliasSubscription = Subscription.Constant(HostAliases.EMPTY)

  private val configSource: ConfigSource =
    PollingConfigSourceBuilder
      .get()
      .pollPeriod(Duration.fromSeconds(30))
      .timer(DefaultTimer)
      .statsReceiver(StatsReceiver)
      .basePath(RoutingConfigRoot)
      .build()

  private val subscriber: ConfigbusSubscriber =
    new ConfigbusSubscriber(StatsReceiver, configSource, "")

  private val productionSub: Subscription[Map[String, RouteConfig]] = {
    val sub = subscriber
      .subscribeAndPublishDirectory(
        ProductionRoutesPath,
        Some("*.json"),
        new RouteConfigParser(),
        Map.empty[String, RouteConfig],
        None)

    Await.result(sub.firstLoadCompleted, Duration.fromSeconds(10))
    sub
  }

  private val canarySub: Subscription[Map[String, RouteConfig]] = {
    val sub = subscriber
      .subscribeAndPublishDirectory(
        path = CanaryRoutesPath,
        glob = Some("*.json"),
        parser = new RouteConfigParser(),
        initialValue = Map.empty[String, RouteConfig],
        defaultValue = None)

    Await.result(sub.firstLoadCompleted, Duration.fromSeconds(10))
    sub
  }

  def getRouteIdToNgRouteMapping: Map[String, NgRoute] = {
    val routeIdToNgRouteMapping = scala.collection.mutable.Map[String, NgRoute]()
    productionSub.latest.map { pair =>
      pair._2.routes.map { route =>
        routeIdToNgRouteMapping += (route.id -> route)
      }
    }
    canarySub.latest.map { pair =>
      pair._2.routes.map { route =>
        routeIdToNgRouteMapping += (route.id -> route)
      }
    }
    routeIdToNgRouteMapping.toMap
  }
}
