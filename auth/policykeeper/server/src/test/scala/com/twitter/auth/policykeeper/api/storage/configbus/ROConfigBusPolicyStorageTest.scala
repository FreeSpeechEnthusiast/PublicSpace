package com.twitter.auth.policykeeper.api.storage.configbus

import com.twitter.auth.policykeeper.api.logger.JsonLogger
import com.twitter.auth.policykeeper.api.storage.StorageTestBase
import com.twitter.auth.policykeeper.api.storage.common.PolicyId
import com.twitter.auth.policykeeper.api.storage.configbus.parser.PolicyDatabaseFile
import com.twitter.auth.policykeeper.api.storage.configbus.parser.PolicyDatabaseConfig
import com.twitter.auth.policykeeper.api.storage.configbus.parser.PolicyDatabaseParser
import com.twitter.configbus.client.file.PollingConfigSourceBuilder
import com.twitter.configbus.subscriber.ConfigbusSubscriber
import com.twitter.configbus.subscriber.Subscription
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.file.Files
import com.twitter.conversions.DurationOps._
import com.twitter.util.Await
import org.scalatest.time.Seconds
import org.scalatest.time.Span
import com.twitter.util.TimeoutException
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ROConfigBusPolicyStorageTest extends StorageTestBase {

  private val tempConfPath: File = Files.createTempDirectory("testconfigbus").toFile

  private val locationFolder = PolicyDatabaseConfig.ConfigBusStorageDir
  private val dbFolder1: File =
    new File(tempConfPath.getAbsolutePath + "/" + locationFolder + "/1")
  private val dbFolder2: File =
    new File(tempConfPath.getAbsolutePath + "/" + locationFolder + "/2")

  private[this] val configBusDatabaseFile = "policies.yaml"

  override implicit val patienceConfig = PatienceConfig(timeout = Span(2, Seconds))

  before {
    statsReceiver.clear()
    configBusStorage.underlyingStorage.clear()
    dbFolder1.mkdirs()
    dbFolder2.mkdirs()
  }

  after {
    new File(dbFolder1, configBusDatabaseFile).delete()
    new File(dbFolder2, configBusDatabaseFile).delete()
    dbFolder1.delete()
    dbFolder2.delete()
  }

  def mockConfigData(parentPath: File, fileName: String, str: String): Unit = {
    val path = new File(parentPath, fileName)
    val stream = new PrintStream(new FileOutputStream(path))
    stream.print(str)
    stream.close()
  }

  def configBusDBSubscription: Subscription[Map[String, PolicyDatabaseFile]] = {
    val fakeConfigSource = PollingConfigSourceBuilder()
      .basePath(tempConfPath.getAbsolutePath)
      .pollPeriod(20.milliseconds)
      .versionFilePath(None)
      .build()
    val configSubscriber: ConfigbusSubscriber =
      new ConfigbusSubscriber(
        statsReceiver = statsReceiver,
        configSource = fakeConfigSource,
        rootPath = "./")
    configSubscriber
      .subscribeAndPublishDirectory(
        path = locationFolder,
        parser = PolicyDatabaseParser,
        initialValue = Map.empty[String, PolicyDatabaseFile],
        defaultValue = None,
        glob = Some("*.yaml"))
  }

  protected val configBusStorage =
    ROConfigBusPolicyStorage(
      configBusDBSubscription,
      statsReceiver = statsReceiver,
      logger = JsonLogger(logger))

  test("test getPolicyById through configbus") {

    mockConfigData(
      dbFolder1,
      configBusDatabaseFile,
      """policies:
        |  - policy_id: policy1
        |    decider: 
        |    data_providers: 
        |    eligibility_criteria: 
        |    rules:
        |      - expression: exp
        |        action:
        |          action_needed: false
        |          api_error_code: 
        |        priority: 0
        |    name: testPolicy
        |    description: ''
        |""".stripMargin
    )

    eventually {

      Await.result(configBusStorage.getPolicyById(PolicyId(testPolicyId))) mustBe Some(testPolicy)

      statsReceiver.counters(
        List(configBusStorage.Scope, configBusStorage.DataReloaded)) mustEqual 1L
      configBusStorage.loadedPoliciesCount.get() mustBe 1

    }

  }

  test("test policy with bouncer settings loading through configbus") {

    mockConfigData(
      dbFolder1,
      configBusDatabaseFile,
      """policies:
        |-   policy_id: policy1
        |    rules:
        |      - expression: exp
        |        action:
        |          action_needed: true
        |          bouncer_settings:
        |            target:
        |              target_type: session
        |              user_id: "{{auth.userId}}"
        |              session_hash: "{{auth.sessionHash}}"
        |              feature: auth_challenge
        |            location: /account/access
        |            experience: FullOptional
        |            template_ids:
        |              - module_auth_challenge
        |            template_mapping:
        |              tslaAuthChallengeData:
        |                token: "{{access_token.token}}"
        |                tokenKind: "{{access_token.tokenKind}}"
        |              redirectUrl: "{{input.redirect_after_verify}}"
        |            referring_tags:
        |              - TSLA
        |              - MODULE
        |        fallback_action:
        |          action_needed: true
        |          api_error_code: 214
        |        priority: 0
        |    name: testPolicy
        |    description: ''
        |""".stripMargin
    )

    eventually {

      Await.result(configBusStorage.getPolicyById(PolicyId(testPolicyId))) mustBe Some(
        testBouncerPolicy)

      statsReceiver.counters(
        List(configBusStorage.Scope, configBusStorage.DataReloaded)) mustEqual 1L
      configBusStorage.loadedPoliciesCount.get() mustBe 1

    }

  }

  test("test empty policy db through configbus") {

    mockConfigData(
      dbFolder1,
      configBusDatabaseFile,
      """policies:
        |""".stripMargin
    )

    eventually {

      Await.result(configBusStorage.getPolicyById(PolicyId(testPolicyId))) mustBe None

      statsReceiver.counters(
        List(configBusStorage.Scope, configBusStorage.DataReloaded)) mustEqual 1L
      configBusStorage.loadedPoliciesCount.get() mustBe 0

    }

  }

  test("test getPoliciesByIds through configbus") {

    mockConfigData(
      dbFolder1,
      configBusDatabaseFile,
      """policies:
        |  - policy_id: policy1
        |    decider: 
        |    data_providers: 
        |    eligibility_criteria: 
        |    rules:
        |      - expression: exp
        |        action:
        |          action_needed: false
        |          api_error_code: 
        |    name: testPolicy
        |    description: ''
        |  - policy_id: policy2
        |    decider: 
        |    data_providers: 
        |    eligibility_criteria: 
        |    rules:
        |      - expression: otherExp
        |        action:
        |          action_needed: false
        |          api_error_code: 
        |        priority: 0
        |    name: testOtherPolicy
        |    description: ''
        |""".stripMargin
    )

    eventually {

      Await.result(
        configBusStorage.getPoliciesByIds(
          Seq(
            PolicyId(testPolicyId),
            PolicyId(testOtherPolicyId)))) must contain theSameElementsAs Seq(
        testPolicy,
        testOtherPolicy)

      statsReceiver.counters(
        List(configBusStorage.Scope, configBusStorage.DataReloaded)) mustEqual 1L
      configBusStorage.loadedPoliciesCount.get() mustBe 2

    }

  }

  test("test getPoliciesByIds through configbus (multiple folders)") {

    mockConfigData(
      dbFolder1,
      configBusDatabaseFile,
      """policies:
        |  - policy_id: policy1
        |    decider: 
        |    data_providers: 
        |    eligibility_criteria: 
        |    rules:
        |      - expression: exp
        |        action:
        |          action_needed: false
        |          api_error_code: 
        |    name: testPolicy
        |    description: ''
        |""".stripMargin
    )

    mockConfigData(
      dbFolder2,
      configBusDatabaseFile,
      """policies:
        |  - policy_id: policy2
        |    decider: 
        |    data_providers: 
        |    eligibility_criteria: 
        |    rules:
        |      - expression: otherExp
        |        action:
        |          action_needed: false
        |          api_error_code: 
        |        priority: 0
        |    name: testOtherPolicy
        |    description: ''
        |""".stripMargin
    )

    eventually {
      Await
        .result(
          configBusStorage.getPoliciesByIds(
            Seq(
              PolicyId(testPolicyId),
              PolicyId(testOtherPolicyId)))) must contain theSameElementsAs Seq(
        testPolicy,
        testOtherPolicy)

      statsReceiver.counters(
        List(configBusStorage.Scope, configBusStorage.DataReloaded)) mustEqual 1L
      configBusStorage.loadedPoliciesCount.get() mustBe 2

    }

  }

  test("test dynamic database replacement using configbus") {

    mockConfigData(
      dbFolder1,
      configBusDatabaseFile,
      """policies:
        |  - policy_id: policy1
        |    decider: 
        |    data_providers: 
        |    eligibility_criteria: 
        |    rules:
        |      - expression: exp
        |        action:
        |          action_needed: false
        |          api_error_code: 
        |        priority: 0
        |    name: testPolicy
        |    description: ''
        |""".stripMargin
    )

    eventually {
      Await.result(configBusStorage.getPolicyById(PolicyId(testPolicyId))) mustBe Some(testPolicy)
    }

    mockConfigData(
      dbFolder1,
      configBusDatabaseFile,
      """policies:
        |  - policy_id: policy2
        |    decider: 
        |    data_providers: 
        |    eligibility_criteria: 
        |    rules:
        |      - expression: otherExp
        |        action:
        |          action_needed: false
        |          api_error_code: 
        |        priority: 0
        |    name: testOtherPolicy
        |    description: ''
        |""".stripMargin
    )

    eventually {
      Await.result(configBusStorage.getPolicyById(PolicyId(testOtherPolicyId))) mustBe Some(
        testOtherPolicy)

      statsReceiver.counters(
        List(configBusStorage.Scope, configBusStorage.DataReloaded)) mustEqual 2L

      configBusStorage.loadedPoliciesCount.get() mustBe 1
    }

  }

  test(
    "test dynamic database replacement using configbus (multiple folders, update in two folders)") {

    mockConfigData(
      dbFolder1,
      configBusDatabaseFile,
      """policies:
        |  - policy_id: policy1
        |    decider: 
        |    data_providers: 
        |    eligibility_criteria: 
        |    rules:
        |      - expression: exp
        |        action:
        |          action_needed: false
        |          api_error_code: 
        |        priority: 0
        |    name: testPolicy
        |    description: ''
        |""".stripMargin
    )

    eventually {
      Await.result(configBusStorage.getPolicyById(PolicyId(testPolicyId))) mustBe Some(testPolicy)
    }

    mockConfigData(
      dbFolder1,
      configBusDatabaseFile,
      """policies:
        |  - policy_id: policy2
        |    decider: 
        |    data_providers: 
        |    eligibility_criteria: 
        |    rules:
        |      - expression: otherExp
        |        action:
        |          action_needed: false
        |          api_error_code: 
        |        priority: 0
        |    name: testOtherPolicy
        |    description: ''
        |""".stripMargin
    )

    mockConfigData(
      dbFolder2,
      configBusDatabaseFile,
      """policies:
        |  - policy_id: policy4
        |    decider: 
        |    data_providers: 
        |    eligibility_criteria: 
        |    rules:
        |      - expression: oneMoreExp
        |        action:
        |          action_needed: false
        |          api_error_code: 
        |        priority: 0
        |    name: testOneMorePolicy
        |    description: ''
        |""".stripMargin
    )

    eventually {
      Await.result(
        configBusStorage.getPoliciesByIds(
          Seq(
            PolicyId(testOtherPolicyId),
            PolicyId(testOneMorePolicyId)))) must contain theSameElementsAs Seq(
        testOtherPolicy,
        testOneMorePolicy)

      statsReceiver.counters(
        List(configBusStorage.Scope, configBusStorage.DataReloaded)) mustEqual 2L

      configBusStorage.loadedPoliciesCount.get() mustBe 2
    }

  }

  test(
    "test dynamic database replacement using configbus (multiple folders, making sure that other folders are not lost during update)") {

    mockConfigData(
      dbFolder1,
      configBusDatabaseFile,
      """policies:
        |  - policy_id: policy1
        |    decider: 
        |    data_providers: 
        |    eligibility_criteria: 
        |    rules:
        |      - expression: exp
        |        action:
        |          action_needed: false
        |          api_error_code: 
        |        priority: 0
        |    name: testPolicy
        |    description: ''
        |""".stripMargin
    )

    eventually {
      Await.result(configBusStorage.getPolicyById(PolicyId(testPolicyId))) mustBe Some(testPolicy)
    }

    mockConfigData(
      dbFolder2,
      configBusDatabaseFile,
      """policies:
        |  - policy_id: policy4
        |    decider: 
        |    data_providers: 
        |    eligibility_criteria: 
        |    rules:
        |      - expression: oneMoreExp
        |        action:
        |          action_needed: false
        |          api_error_code: 
        |        priority: 0
        |    name: testOneMorePolicy
        |    description: ''
        |""".stripMargin
    )

    eventually {
      Await.result(
        configBusStorage.getPoliciesByIds(
          Seq(
            PolicyId(testPolicyId),
            PolicyId(testOneMorePolicyId)))) must contain theSameElementsAs Seq(
        testPolicy,
        testOneMorePolicy)

      statsReceiver.counters(
        List(configBusStorage.Scope, configBusStorage.DataReloaded)) mustEqual 2L

      configBusStorage.loadedPoliciesCount.get() mustBe 2
    }

  }

  test("test broken policy file through configbus") {

    mockConfigData(
      dbFolder1,
      configBusDatabaseFile,
      """policies:
        |    policy_id: policy1 <- intentionally incorrect
        |    decider: 
        |    data_providers: 
        |    eligibility_criteria: 
        |    rules:
        |      - expression: exp
        |        action:
        |          action_needed: false
        |          api_error_code: 
        |        priority: 0
        |    name: testPolicy
        |    description: ''
        |""".stripMargin
    )

    intercept[TimeoutException] {
      Await.result(configBusDBSubscription.firstLoadCompleted, 1.seconds)
    }

    Await.result(configBusStorage.getPolicyById(PolicyId(testPolicyId))) mustBe None

    configBusStorage.loadedPoliciesCount.get() mustBe 0

  }

  test("test import policy with invalid policy identifier through configbus") {

    mockConfigData(
      dbFolder1,
      configBusDatabaseFile,
      """policies:
        |  - policy_id: policy1
        |    decider: 
        |    data_providers: 
        |    eligibility_criteria: 
        |    rules:
        |      - expression: exp
        |        action:
        |          action_needed: false
        |          api_error_code: 
        |        priority: 0
        |    name: testPolicy
        |    description: ''
        |  - policy_id: p o l i c y
        |    decider: 
        |    data_providers: 
        |    eligibility_criteria: 
        |    rules:
        |      - expression: exp
        |        action:
        |          action_needed: false
        |          api_error_code: 
        |        priority: 0
        |    name: testPolicy
        |    description: ''
        |""".stripMargin
    )

    eventually {

      Await.result(configBusStorage.getPolicyById(PolicyId(testPolicyId))) mustBe Some(testPolicy)
      Await.result(configBusStorage.getPolicyById(PolicyId(invalidPolicyId))) mustBe None

      statsReceiver.counters(
        List(configBusStorage.Scope, configBusStorage.DataReloaded)) mustEqual 1L

      statsReceiver.counters(
        List(configBusStorage.Scope, configBusStorage.WrongPolicyId)) mustEqual 1L

      configBusStorage.loadedPoliciesCount.get() mustBe 1
    }

  }
}
