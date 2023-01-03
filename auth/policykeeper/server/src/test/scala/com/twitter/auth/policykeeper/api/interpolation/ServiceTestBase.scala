package com.twitter.auth.policykeeper.api.interpolation

import com.twitter.auth.policykeeper.thriftscala.BouncerSettings
import com.twitter.auth.policykeeper.thriftscala.BouncerTargetSettings
import com.twitter.auth.policykeeper.thriftscala.DeepStringMap
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
trait ServiceTestBase
    extends AnyFunSuite
    with OneInstancePerTest
    with Matchers
    with Eventually
    with BeforeAndAfter {

  protected val testBouncerSettings = BouncerSettings(
    location = Some("/account/access?feature=auth_challenge&session={{auth.sessionHash}}"),
    errorMessage = None,
    deepLink = None,
    experience = Some("FullOptional"),
    templateIds = Seq("module_auth_challenge"),
    templateMapping = Some(
      Map(
        "redirectUrl" -> DeepStringMap(
          isMap = false,
          stringVal = Some("{{input.url}}"),
          mapVal = None),
        "tslaAuthChallengeData" -> DeepStringMap(
          isMap = true,
          stringVal = None,
          mapVal = Some(Map(
            "token" -> DeepStringMap(
              isMap = false,
              stringVal = Some("{{access_token.token}}"),
              mapVal = None),
            "tokenKind" -> DeepStringMap(
              isMap = false,
              stringVal = Some("{{access_token.tokenKind}}"),
              mapVal = None)
          ))
        )
      )),
    referringTags = Some(Set("TSLA", "MODULE")),
    target = BouncerTargetSettings(
      targetType = "session",
      userId = Some("{{userId}}"),
      sessionHash = Some("{{auth.sessionHash}}"),
      feature = Some("auth_challenge")
    )
  )

}
