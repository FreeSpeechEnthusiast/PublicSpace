package com.twitter.auth.sso.builder

import com.twitter.conversions.DurationOps._
import com.google.api.client.auth.openidconnect.IdTokenVerifier
import com.google.api.client.googleapis.auth.oauth2.{GoogleIdTokenVerifier, GooglePublicKeysManager}
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.apache.v2.ApacheHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.twitter.auth.sso.client.{
  AppleIdProviderClient,
  GoogleSsoProviderClient,
  IdTokenIntegrityClient,
  SsoProviderClient
}
import com.twitter.auth.sso.models.SsoProvider
import com.twitter.auth.sso.service.IdTokenIntegrityService
import com.twitter.finagle.client.Transporter
import com.twitter.finagle.service.RetryBudget
import com.twitter.finagle.Http
import com.twitter.finagle.stats.{NullStatsReceiver, StatsReceiver}
import com.twitter.util.jackson.ScalaObjectMapper
import com.twitter.util.security.Credentials
import java.io.File
import java.net.InetSocketAddress
import org.apache.http.HttpHost
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.impl.client.{
  BasicCredentialsProvider,
  DefaultHttpRequestRetryHandler,
  ProxyAuthenticationStrategy
}
import org.apache.http.impl.conn.DefaultProxyRoutePlanner
import scala.collection.JavaConverters._

object IdTokenIntegrityServiceBuilder {

  final val TssProxyCredentialsPath = "/var/lib/tss/keys/single-sign-on/proxy.yml"

  final val HttpProxyUsername = "username"
  final val HttpProxyPassword = "password"
  final val HttpProxyAddress = "httpproxy-sso.local.twitter.com"
  final val HttpProxyPort = 3128

  // Including the Google Playground audience to facilitate testing.
  // You can generate test id-tokens via https://developers.google.com/oauthplayground/
  final val GoogleTestAudience: String = "407408718192.apps.googleusercontent.com"

  // This is the Twitter client id for calling Google. In the future, we could considering making this
  // loaded via a config file.
  final val GoogleTwitterAndroidAudience: String =
    "49625052041-g0p3rurlhsc5aqfrns946ik1qdmehpej.apps.googleusercontent.com"
  final val GoogleTwitterIOsDevAudience: String =
    "213005704923-phsv4k7krff7msmjiq4297juf0jc2pnf.apps.googleusercontent.com"
  final val GoogleTwitterIOsProdAudience: String =
    "638150425128-l5vqrme56bqgum2unv5151c5569dllud.apps.googleusercontent.com"

  final val GoogleTwitterWebAudience: String =
    "49625052041-kgt0hghf445lmcmhijv46b715m2mpbct.apps.googleusercontent.com"
  final val GoogleAudiences = Seq(
    GoogleTestAudience,
    GoogleTwitterIOsDevAudience,
    GoogleTwitterIOsProdAudience,
    GoogleTwitterAndroidAudience,
    GoogleTwitterWebAudience).asJavaCollection

  final val AppleIdIssuer = "https://appleid.apple.com"

  final val AppleIdWebAudience = "com.twitter.twitter.siwa"
  final val AppleIdIosProdAudience = "com.atebits.Tweetie2"
  final val AppleIdIosInAppPurchaseAudience = "com.twitter.twitter.iap"
  final val AppleIdIosTestFlightAudience = "com.twitter.twitter.beta.testflight"
  final val AppleIdIosBetaAudience = "com.twitter.twttr"

  final val AllIosAudiences = Seq(
    AppleIdIosProdAudience,
    AppleIdIosInAppPurchaseAudience,
    AppleIdIosTestFlightAudience,
    AppleIdIosBetaAudience
  )

  // Mac IDs are just iOS' prefixed with maccatalyst.
  final val AllMacAudiences = AllIosAudiences.map { audience => s"maccatalyst.{$audience}" }
  final val AppleIdAudiences =
    (Seq(AppleIdWebAudience) ++ AllMacAudiences ++ AllIosAudiences).asJavaCollection
  final val AppleIdHostname = "appleid.apple.com"

  private def buildHttpTransport(
    proxyHost: String,
    proxyPort: Int,
    proxyCredentialsUserName: String,
    proxyCredentialsPassword: String
  ): HttpTransport = {
    val credentialsProvider = new BasicCredentialsProvider
    val proxyHostDetails = new HttpHost(proxyHost, proxyPort)
    val proxyRoutePlanner = new DefaultProxyRoutePlanner(proxyHostDetails)

    credentialsProvider.setCredentials(
      new AuthScope(proxyHost, proxyPort),
      new UsernamePasswordCredentials(
        proxyCredentialsUserName,
        proxyCredentialsPassword
      )
    )

    val mHttpClient = ApacheHttpTransport.newDefaultHttpClientBuilder
      .setRoutePlanner(proxyRoutePlanner)
      .setRetryHandler(new DefaultHttpRequestRetryHandler)
      .setProxyAuthenticationStrategy(ProxyAuthenticationStrategy.INSTANCE)
      .setDefaultCredentialsProvider(credentialsProvider)
      .build()

    new ApacheHttpTransport(mHttpClient)
  }

  private def buildGoogleSsoProviderClient(
    proxyHost: String,
    proxyPort: Int,
    proxyCredentialsUserName: String,
    proxyCredentialsPassword: String
  ): GoogleSsoProviderClient = {
    val transport = buildHttpTransport(
      proxyHost = proxyHost,
      proxyPort = proxyPort,
      proxyCredentialsUserName = proxyCredentialsUserName,
      proxyCredentialsPassword = proxyCredentialsPassword
    )

    val googlePublicKeysManager =
      new GooglePublicKeysManager(transport, JacksonFactory.getDefaultInstance())
    val googleIdTokenVerifier =
      new GoogleIdTokenVerifier.Builder(googlePublicKeysManager)
        .setAudience(GoogleAudiences)
        .build()

    new GoogleSsoProviderClient(googleIdTokenVerifier)
  }

  private def buildAppleIdProviderClient(
    scalaObjectMapper: ScalaObjectMapper,
    proxyHost: String,
    proxyPort: Int,
    proxyCredentialsUserName: String,
    proxyCredentialsPassword: String
  ): AppleIdProviderClient = {
    val transport = buildHttpTransport(
      proxyHost = proxyHost,
      proxyPort = proxyPort,
      proxyCredentialsUserName = proxyCredentialsUserName,
      proxyCredentialsPassword = proxyCredentialsPassword
    )

    val client =
      Http.client.withSessionQualifier.noFailFast.withTransport
        .connectTimeout(250.milliseconds)
        .withRequestTimeout(1.second)
        .withRetryBudget(RetryBudget())
        .withTransport.tls(AppleIdHostname).configured(
          Transporter
            .HttpProxy(
              Some(new InetSocketAddress(proxyHost, proxyPort)),
              Some(
                Transporter.Credentials(
                  proxyCredentialsUserName,
                  proxyCredentialsPassword
                )
              )
            )).newService(s"${AppleIdHostname}:443", "apple_id_client")

    val idTokenVerifier =
      new IdTokenVerifier.Builder()
        .setAudience(AppleIdAudiences)
        .setIssuer(AppleIdIssuer)
        .build()

    new AppleIdProviderClient(client, scalaObjectMapper, idTokenVerifier)
  }

  def buildIdTokenIntegrityService(
    scalaObjectMapper: ScalaObjectMapper,
    secretFileName: String = TssProxyCredentialsPath,
    statsReceiver: StatsReceiver = NullStatsReceiver
  ): IdTokenIntegrityService = {
    val credentials = Credentials(new File(secretFileName))
    val proxyCredentialsUserName = credentials(HttpProxyUsername)
    val proxyCredentialsPassword = credentials(HttpProxyPassword)

    val googleProvider = buildGoogleSsoProviderClient(
      proxyHost = HttpProxyAddress,
      proxyPort = HttpProxyPort,
      proxyCredentialsUserName = proxyCredentialsUserName,
      proxyCredentialsPassword = proxyCredentialsPassword
    )

    val appleIdProvider = buildAppleIdProviderClient(
      scalaObjectMapper = scalaObjectMapper,
      proxyHost = HttpProxyAddress,
      proxyPort = HttpProxyPort,
      proxyCredentialsUserName = proxyCredentialsUserName,
      proxyCredentialsPassword = proxyCredentialsPassword
    )

    val ssoProviders: Map[SsoProvider, SsoProviderClient] =
      Map(SsoProvider.Google -> googleProvider, SsoProvider.Apple -> appleIdProvider)
    val idTokenIntegrityClient = new IdTokenIntegrityClient(ssoProviders)
    new IdTokenIntegrityService(
      idTokenIntegrityClient = idTokenIntegrityClient,
      statsReceiver = statsReceiver
    )
  }
}
