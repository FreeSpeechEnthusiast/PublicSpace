package com.twitter.auth.policy

import com.twitter.auth.authorization.RouteIdToScopeConfig
import com.twitter.auth.authorization.RouteIdToScopeMapping
import com.twitter.configbus.subscriber.ConfigbusSubscriber
import com.twitter.finagle.stats.NullStatsReceiver
import org.junit.Assert
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class RouteIdToScopeConfigSpec
    extends AnyFunSuite
    with OneInstancePerTest
    with MockitoSugar
    with Matchers
    with BeforeAndAfter {

  private val fileName = "testMappings"
  private var source: MockConfigSource = _
  private var configbusSubscriber: ConfigbusSubscriber = _
  private var routeIdToScopeConfig: RouteIdToScopeConfig = _

  before {
    source = new MockConfigSource
    configbusSubscriber = new ConfigbusSubscriber(NullStatsReceiver, source, "")
    routeIdToScopeConfig = new RouteIdToScopeConfig(configbusSubscriber, NullStatsReceiver)
  }

  private val originalMappings = Map(
    "GET/2/tweets/{id}->cluster:des_apiservice_get_2_tweets_id_prod" -> Set(
      "tweet.read",
      "users.read"),
    "GET/2/spaces/{id}->cluster:des_apiservice_staging1" -> Set(
      "users.read",
      "tweet.read",
      "space.read")
  )

  private val testMappings: String =
    """
      {
      |  "mapping":
      |    {
      |      "GET/2/tweets/{id}->cluster:des_apiservice_get_2_tweets_id_prod": ["tweet.read", "users.read"],
      |      "GET/2/spaces/{id}->cluster:des_apiservice_staging1": ["users.read", "tweet.read", "space.read"]
      |    }
      |}
    """.stripMargin

  private val testMappingsWithInvalidJson: String =
    """
      {
    |  "mapping":
    |    {
    |      "GET/2/tweets/{id}->cluster:des_apiservice_get_2_tweets_id_prod: ["tweet.read", "users.read"],
    |      "GET/2/spaces/{id}->cluster:des_apiservice_staging1": ["users.read", "tweet.read", "space.read"
    |    }
    |}
    """.stripMargin

  private val testMappingsWithIncorrectHeading: String =
    """
    {
    |   "GET/2/tweets/{id}->cluster:des_apiservice_get_2_tweets_id_prod": ["tweet.read", "users.read"],
    |   "GET/2/spaces/{id}->cluster:des_apiservice_staging1": ["users.read", "tweet.read", "space.read"]
    |}
    """.stripMargin

  test("Return the original mappings if file is present") {
    source.setFileContents(fileName, testMappings)
    val configVar = routeIdToScopeConfig.watch(fileName)
    Assert.assertEquals(RouteIdToScopeMapping(originalMappings), configVar.sample())
  }

  test("Return empty config if file is not present") {
    val configVar = routeIdToScopeConfig.watch(fileName)
    Assert.assertEquals(RouteIdToScopeMapping(Map()), configVar.sample())
  }

  test("Updates in file gets reflected") {
    val configVar = routeIdToScopeConfig.watch(fileName)
    Assert.assertEquals(RouteIdToScopeMapping(Map()), configVar.sample())
    source.setFileContents(fileName, testMappings)
    Assert.assertEquals(RouteIdToScopeMapping(originalMappings), configVar.sample())
  }

  test("Does not update mappings if the file is updated incorrectly") {
    source.setFileContents(fileName, testMappings)
    val configVar = routeIdToScopeConfig.watch(fileName)
    Assert.assertEquals(RouteIdToScopeMapping(originalMappings), configVar.sample())

    // var does not change with invalid configs commited
    source.setFileContents(fileName, testMappingsWithInvalidJson)
    Assert.assertEquals(RouteIdToScopeMapping(originalMappings), configVar.sample())
    source.setFileContents(fileName, testMappingsWithIncorrectHeading)
    Assert.assertEquals(RouteIdToScopeMapping(originalMappings), configVar.sample())
  }
}
