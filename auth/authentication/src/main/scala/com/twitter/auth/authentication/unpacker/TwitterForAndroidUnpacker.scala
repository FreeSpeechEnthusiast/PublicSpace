package com.twitter.auth.authentication.unpacker

import com.twitter.decider.Feature
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.joauth.OAuthParams.OAuthParamsHelper
import com.twitter.joauth.Unpacker.KeyValueCallback
import com.twitter.joauth._
import com.twitter.joauth.keyvalue.KeyValueHandler.TransformingKeyValueHandler
import com.twitter.joauth.keyvalue.KeyValueParser.{HeaderKeyValueParser, QueryKeyValueParser}
import com.twitter.joauth.keyvalue._

object TwitterForAndroidUnpacker {
  private val allowFloatingPointTimestamps = true
  private val helper = UnpackerHelper(allowFloatingPointTimestamps)

  def apply(statsReceiver: StatsReceiver, useNewRequestBodyParser: Feature) =
    new TwitterForAndroidUnpacker(
      helper = helper,
      normalizer = Normalizer.getStandardNormalizer,
      queryParser = QueryKeyValueParser,
      headerParser = HeaderKeyValueParser,
      shouldAllowOAuth2IfNetworkLocal = true,
      shouldAllowOAuth2SessionWithoutAuthTypeHeader = true,
      shouldAllowWebAuthMultiUserIdHeader = true,
      useNewRequestBodyParser = useNewRequestBodyParser,
      statsReceiver = statsReceiver
    )
}

/**
 * Twitter For Android clients prior to version 3.7.0 do not properly url encode their body
 * A big portion of this is borrowed from JOAuth's Unpacker
 */
class TwitterForAndroidUnpacker(
  helper: OAuthParamsHelper,
  normalizer: Normalizer,
  queryParser: KeyValueParser,
  headerParser: KeyValueParser,
  shouldAllowOAuth2IfNetworkLocal: Boolean,
  shouldAllowOAuth2SessionWithoutAuthTypeHeader: Boolean,
  shouldAllowWebAuthMultiUserIdHeader: Boolean,
  useNewRequestBodyParser: Feature,
  statsReceiver: StatsReceiver)
    extends TwitterRequestUnpacker(
      helper,
      normalizer,
      queryParser,
      headerParser,
      PassbirdKeyValueCallback,
      NonNormalizingCallback,
      PassbirdKeyValueCallback,
      (Request, OAuth1Params) => false,
      shouldAllowOAuth2IfNetworkLocal,
      shouldAllowOAuth2SessionWithoutAuthTypeHeader,
      shouldAllowWebAuthMultiUserIdHeader,
      useNewRequestBodyParser,
      statsReceiver = statsReceiver
    )

object NonNormalizingCallback extends KeyValueCallback {
  def invoke(kvHandler: KeyValueHandler) = new UrlEncodingKeyValueHandler(kvHandler)
}

object UrlEncodingTransformer extends Transformer {
  override def transform(s: String): String = UrlCodec.encode(s)
}

class UrlEncodingKeyValueHandler(kvHandler: KeyValueHandler)
    extends TransformingKeyValueHandler(kvHandler, UrlEncodingTransformer, UrlEncodingTransformer)
