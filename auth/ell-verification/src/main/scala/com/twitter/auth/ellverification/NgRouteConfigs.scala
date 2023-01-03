package com.twitter.auth.ellverification

import com.twitter.configbus.subscriber.ConfigbusSubscriber
import com.twitter.configbus.subscriber.Subscription
import com.twitter.configbus.client.ConfigSource
import com.twitter.configbus.client.file.PollingConfigSourceBuilder
import com.twitter.finagle.util.DefaultTimer
import com.twitter.tfe.core.decider.StubTfeDecider
import com.twitter.tfe.core.host_aliases.HostAliases
import com.twitter.tfe.core.routingng.NgRoute
import com.twitter.tfe.core.routingng.RouteConfig
import com.twitter.tfe.core.routingng.RouteConfigParser
import com.twitter.util.Await
import com.twitter.util.Duration

object NgRouteConfigs extends ConfigUtils {

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
        CanaryRoutesPath,
        Some("*.json"),
        new RouteConfigParser(),
        Map.empty[String, RouteConfig],
        None)

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
