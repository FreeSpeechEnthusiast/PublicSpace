package com.twitter.auth.authorizationscope

import com.twitter.configbus.subscriber.ConfigbusSubscriber
import com.twitter.finagle.stats.NullStatsReceiver
import org.junit.Assert
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class ScopesConfigSpec
    extends AnyFunSuite
    with OneInstancePerTest
    with MockitoSugar
    with Matchers
    with BeforeAndAfter {

  private val fileName = "testMapping"
  private var source: MockConfigSource = _
  private var configbusSubscriber: ConfigbusSubscriber = _
  private var scopesConfig: AuthorizationScopesConfig = _

  before {
    source = new MockConfigSource
    configbusSubscriber = new ConfigbusSubscriber(NullStatsReceiver, source, "")
    scopesConfig = new AuthorizationScopesConfig(configbusSubscriber, NullStatsReceiver)
  }

  val read_0 = AuthorizationScope(
    0,
    "read",
    "legacy",
    Some("Things this App can view ..."),
    None,
    "internal",
    Set("data-products"),
    Set(),
    Set(),
    Set("OAUTH_2")
  )

  val read_write_1 = AuthorizationScope(
    1,
    "read_write",
    "legacy",
    None,
    Some("Things this App can do ..."),
    "production",
    Set("data-products"))

  val fine_grained_1 = AuthorizationScope(
    2,
    "fine_grained_1",
    "vnext",
    Some("Things this App can view ..."),
    Some("Things this App can do ..."),
    "production",
    Set("data-products"))

  val fine_grained_2 =
    AuthorizationScope(3, "fine_grained_2", "vnext", None, None, "production", Set("data-products"))

  private val scopesObjectList = List(read_0, read_write_1, fine_grained_1, fine_grained_2)

  private val scopesString: String =
    """
      |{
      |   "scopes_list": [
      |    {
      |      "id": 0,
      |      "name": "read",
      |      "internal_group": "legacy",
      |      "app_can_view_description": "Things this App can view ...",
      |      "state": "internal",
      |      "ldap_owners": ["data-products"],
      |      "applicable_auth_types": ["OAUTH_2"]
      |    },
      |    {
      |      "id": 1,
      |      "name": "read_write",
      |      "internal_group": "legacy",
      |      "app_can_do_description": "Things this App can do ...",
      |      "state": "production",
      |      "ldap_owners": ["data-products"]
      |    },
      |    {
      |      "id": 2,
      |      "name": "fine_grained_1",
      |      "internal_group": "vnext",
      |      "app_can_view_description": "Things this App can view ...",
      |      "app_can_do_description": "Things this App can do ...",
      |      "state": "production",
      |      "ldap_owners": ["data-products"]
      |    },
      |    {
      |      "id": 3,
      |      "name": "fine_grained_2",
      |      "internal_group": "vnext",
      |      "state": "production",
      |      "ldap_owners": ["data-products"]
      |    }
      |  ]
      | }
    """.stripMargin

  private val scopesStringWithUnknown: String =
    """
      |{
      |   "scopes_list": [
      |    {
      |      "id": 0,
      |      "name": "read",
      |      "internal_group": "legacy",
      |      "app_can_view_description": "Things this App can view ...",
      |      "dummmy": "123",
      |      "state": "internal",
      |      "ldap_owners": ["data-products"],
      |      "applicable_auth_types": ["OAUTH_2"]
      |    },
      |    {
      |      "id": 1,
      |      "name": "read_write",
      |      "internal_group": "legacy",
      |      "app_can_do_description": "Things this App can do ...",
      |      "dummmy": "123",
      |      "state": "production",
      |      "ldap_owners": ["data-products"]
      |    },
      |    {
      |      "id": 2,
      |      "name": "fine_grained_1",
      |      "internal_group": "vnext",
      |      "app_can_view_description": "Things this App can view ...",
      |      "app_can_do_description": "Things this App can do ...",
      |      "dummmy": "123",
      |      "state": "production",
      |      "ldap_owners": ["data-products"]
      |    },
      |    {
      |      "id": 3,
      |      "name": "fine_grained_2",
      |      "internal_group": "vnext",
      |      "state": "production",
      |      "ldap_owners": ["data-products"]
      |    }
      |  ]
      | }
    """.stripMargin

  private val scopesStringWithIncorrectJson: String =
    """
      |{
      |   "scopes_list": [
      |    {
      |      "id": 7,
      |      "name": "read",
      |      "internal_group": "legacy",

      |    },
      |    {
      |      "id": 8,
      |      "name": "read_write",
      |      "internal_group": "legacy",
      |      "user_description": "ui text",
      |      "state": "production",
      |      "ldap_owners": ["data-products"]
      |    }
      |  ]
      | }
    """.stripMargin

  private val scopesStringWithIncorrectHeading: String =
    """
      |    {
      |      "id": 5,
      |      "name": "read",
      |      "internal_group": "legacy",
      |      "user_description": "ui text",
      |      "state": "internal",
      |      "ldap_owners": ["data-products"],
      |      "applicable_auth_types": ["OAUTH_2"]
      |    },
      |    {
      |      "id": 6,
      |      "name": "read_write",
      |      "internal_group": "legacy",
      |      "user_description": "ui text",
      |      "state": "production",
      |      "ldap_owners": ["data-products"]
      |    }
    """.stripMargin

  test("Return the original config if file is present") {
    source.setFileContents(fileName, scopesString)
    Assert.assertEquals(
      AuthorizationScopesMap(scopesObjectList),
      scopesConfig.watch(fileName).sample())
  }

  test("Return the original config if file has unknown fields") {
    source.setFileContents(fileName, scopesStringWithUnknown)
    Assert.assertEquals(
      AuthorizationScopesMap(scopesObjectList),
      scopesConfig.watch(fileName).sample())
  }

  test("Return empty config if file is not present") {
    Assert.assertEquals(AuthorizationScopesMap(List()), scopesConfig.watch(fileName).sample())
  }

  test("Updates in file gets reflected") {
    val configVar = scopesConfig.watch(fileName)
    Assert.assertEquals(AuthorizationScopesMap(List()), configVar.sample())
    source.setFileContents(fileName, scopesString)
    Assert.assertEquals(AuthorizationScopesMap(scopesObjectList), configVar.sample())
  }

  test("Does not update mapping if the file is updated incorrectly") {
    source.setFileContents(fileName, scopesString)
    val configVar = scopesConfig.watch(fileName)
    Assert.assertEquals(AuthorizationScopesMap(scopesObjectList), configVar.sample())

    // Var does not change with invalid configs committed
    source.setFileContents(fileName, scopesStringWithIncorrectJson)
    Assert.assertEquals(AuthorizationScopesMap(scopesObjectList), configVar.sample())
    source.setFileContents(fileName, scopesStringWithIncorrectHeading)
    Assert.assertEquals(AuthorizationScopesMap(scopesObjectList), configVar.sample())
  }

}
