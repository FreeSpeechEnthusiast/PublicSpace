package com.twitter.auth.policy

import com.twitter.configbus.client.{ConfigSnapshot, ConfigSource}
import com.twitter.configbus.subscriber.ConfigbusSubscriber
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.io.Buf
import com.twitter.util.{Activity, Future, Time, Updatable, Var}
import org.junit.runner.RunWith
import org.scalatest.{OneInstancePerTest, BeforeAndAfter}
import org.scalatest.matchers.must.Matchers
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.junit.Assert
import scala.collection.mutable

@RunWith(classOf[JUnitRunner])
class ScopeToDPMappingConfigSpec
    extends AnyFunSuite
    with OneInstancePerTest
    with MockitoSugar
    with Matchers
    with BeforeAndAfter {

  private val fileName = "testMapping"
  private var source: MockConfigSource = _
  private var configbusSubscriber: ConfigbusSubscriber = _
  private var scopeToDPMappingConfig: ScopeToDPMappingConfig = _

  before {
    source = new MockConfigSource
    configbusSubscriber = new ConfigbusSubscriber(NullStatsReceiver, source, "")
    scopeToDPMappingConfig = new ScopeToDPMappingConfig(configbusSubscriber, NullStatsReceiver)
  }

  private val originalMappings = Map("read_scope" -> Set(1, 2), "read_write_scope" -> Set(3))
  private val mappings: String =
    """
      {
      |  "mapping":
      |    {
      |      "read_scope": [1, 2],
      |      "read_write_scope": [3]
      |    }
      |}
    """.stripMargin

  private val mappingsWithIncorrectJson: String =
    """
      {
      |  "mapping":
      |    {
      |      "read_scope": [1, 2,
      |      "read_write_scope": [3]
      |    }
      |}
    """.stripMargin

  private val mappingsWithIncorrectHeading: String =
    """
      {
      |   "read_scope": [1, 2],
      |   "read_write_scope": [3]
      |}
    """.stripMargin

  test("Return the original config if file is present") {
    source.setFileContents(fileName, mappings)
    Assert.assertEquals(
      ScopeToDPMapping(originalMappings),
      scopeToDPMappingConfig.watch(fileName).sample())
  }

  test("Return empty config if file is not present") {
    Assert.assertEquals(ScopeToDPMapping(Map()), scopeToDPMappingConfig.watch(fileName).sample())
  }

  test("Updates in file gets reflected") {
    val configVar = scopeToDPMappingConfig.watch(fileName)
    Assert.assertEquals(ScopeToDPMapping(Map()), configVar.sample())
    source.setFileContents(fileName, mappings)
    Assert.assertEquals(ScopeToDPMapping(originalMappings), configVar.sample())
  }

  test("Does not update mapping if the file is updated incorrectly") {
    source.setFileContents(fileName, mappings)
    val configVar = scopeToDPMappingConfig.watch(fileName)
    Assert.assertEquals(ScopeToDPMapping(originalMappings), configVar.sample())

    // Var does not change with invalid configs committed
    source.setFileContents(fileName, mappingsWithIncorrectJson)
    Assert.assertEquals(ScopeToDPMapping(originalMappings), configVar.sample())
    source.setFileContents(fileName, mappingsWithIncorrectHeading)
    Assert.assertEquals(ScopeToDPMapping(originalMappings), configVar.sample())
  }
}

class MockConfigSource extends ConfigSource {
  private val perPathVars =
    mutable.Map[
      String,
      Var[Activity.State[ConfigSnapshot[Buf]]] with Updatable[
        Activity.State[ConfigSnapshot[Buf]]
      ]]()

  def setFileContents(path: String, content: String) = {
    val buf: Buf = Buf.Utf8(content)
    val v = perPathVars.getOrElseUpdate(path, Var(Activity.Pending))
    v.update(Activity.Ok(ConfigSnapshot(None, buf)))
  }

  override def subscribe(path: String): Activity[ConfigSnapshot[Buf]] = synchronized {
    val v = perPathVars.getOrElseUpdate(path, Var(Activity.Pending))
    Activity(v)
  }

  override def subscribeRecursive(
    parent: String,
    glob: Option[String]
  ): Activity[ConfigSnapshot[Map[String, Buf]]] = ???

  override def close(deadline: Time): Future[Unit] = Future.Unit
}
