package com.twitter.auth.sso.client

import com.twitter.auth.sso.models.SsoProviderInfo
import com.fasterxml.jackson.databind.{ObjectMapper => JacksonObjectMapper}
import com.fasterxml.jackson.module.scala.{ScalaObjectMapper => JacksonScalaObjectMapper}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import com.google.api.client.auth.openidconnect.IdToken
import com.google.api.client.auth.openidconnect.IdTokenVerifier
import com.twitter.util.jackson.ScalaObjectMapper
import org.mockito.Mockito.when
import com.twitter.finagle.Service
import com.twitter.finagle.http
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.mockito.ArgumentMatchers.any
import org.scalatestplus.mockito.MockitoSugar
import com.twitter.util.Await
import com.twitter.util.Future
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AppleIdProviderClientSpec extends AnyFunSuite with Matchers with MockitoSugar {
  val service = mock[Service[http.Request, http.Response]]
  val underlying = new JacksonObjectMapper with JacksonScalaObjectMapper()

  val mapper = new ScalaObjectMapper(underlying)
  mapper.registerModule(DefaultScalaModule)

  val verifier = mock[IdTokenVerifier]
  val response = http.Response()
  response.contentString =
    """
  {
    "keys": [
      {
        "kty": "RSA",
        "kid": "86D88Kf",
        "use": "sig",
        "alg": "RS256",
        "n": "iGaLqP6y-SJCCBq5Hv6pGDbG_SQ11MNjH7rWHcCFYz4hGwHC4lcSurTlV8u3avoVNM8jXevG1Iu1SY11qInqUvjJur--hghr1b56OPJu6H1iKulSxGjEIyDP6c5BdE1uwprYyr4IO9th8fOwCPygjLFrh44XEGbDIFeImwvBAGOhmMB2AD1n1KviyNsH0bEB7phQtiLk-ILjv1bORSRl8AK677-1T8isGfHKXGZ_ZGtStDe7Lu0Ihp8zoUt59kx2o9uWpROkzF56ypresiIl4WprClRCjz8x6cPZXU2qNWhu71TQvUFwvIvbkE1oYaJMb0jcOTmBRZA2QuYw-zHLwQ",
        "e": "AQAB"
      },
      {
        "kty": "RSA",
        "kid": "eXaunmL",
        "use": "sig",
        "alg": "RS256",
        "n": "4dGQ7bQK8LgILOdLsYzfZjkEAoQeVC_aqyc8GC6RX7dq_KvRAQAWPvkam8VQv4GK5T4ogklEKEvj5ISBamdDNq1n52TpxQwI2EqxSk7I9fKPKhRt4F8-2yETlYvye-2s6NeWJim0KBtOVrk0gWvEDgd6WOqJl_yt5WBISvILNyVg1qAAM8JeX6dRPosahRVDjA52G2X-Tip84wqwyRpUlq2ybzcLh3zyhCitBOebiRWDQfG26EH9lTlJhll-p_Dg8vAXxJLIJ4SNLcqgFeZe4OfHLgdzMvxXZJnPp_VgmkcpUdRotazKZumj6dBPcXI_XID4Z4Z3OM1KrZPJNdUhxw",
        "e": "AQAB"
      },
      {
        "kty": "RSA",
        "kid": "YuyXoY",
        "use": "sig",
        "alg": "RS256",
        "n": "1JiU4l3YCeT4o0gVmxGTEK1IXR-Ghdg5Bzka12tzmtdCxU00ChH66aV-4HRBjF1t95IsaeHeDFRgmF0lJbTDTqa6_VZo2hc0zTiUAsGLacN6slePvDcR1IMucQGtPP5tGhIbU-HKabsKOFdD4VQ5PCXifjpN9R-1qOR571BxCAl4u1kUUIePAAJcBcqGRFSI_I1j_jbN3gflK_8ZNmgnPrXA0kZXzj1I7ZHgekGbZoxmDrzYm2zmja1MsE5A_JX7itBYnlR41LOtvLRCNtw7K3EFlbfB6hkPL-Swk5XNGbWZdTROmaTNzJhV-lWT0gGm6V1qWAK2qOZoIDa_3Ud0Gw",
        "e": "AQAB"
      }
    ]
  }
  """

  when(service.apply(any[http.Request]())).thenReturn(Future.value(response))

  val testIdToken =
    "eyJraWQiOiI4NkQ4OEtmIiwiYWxnIjoiUlMyNTYifQ.eyJpc3MiOiJodHRwczovL2FwcGxlaWQuYXBwbGUuY29tIiwiYXVkIjoiY29tLm1pa2UuYXBwbGUtc3NvLmRlbW84ODgiLCJleHAiOjE2MjIwNTM4MjIsImlhdCI6MTYyMTk2NzQyMiwic3ViIjoiMDAxMTgyLmM3ODM2ZDdjYjY0MzQzNmZhYWI5MmQ4ZGNkODQxZjBkLjE3NTEiLCJjX2hhc2giOiJ1TTFzTEh6TE43QU9rQ2dNWWcxQzNBIiwiZW1haWwiOiJwYmR0Z3J2MmJ0QHByaXZhdGVyZWxheS5hcHBsZWlkLmNvbSIsImVtYWlsX3ZlcmlmaWVkIjoidHJ1ZSIsImlzX3ByaXZhdGVfZW1haWwiOiJ0cnVlIiwiYXV0aF90aW1lIjoxNjIxOTY3NDIyLCJub25jZV9zdXBwb3J0ZWQiOnRydWUsInJlYWxfdXNlcl9zdGF0dXMiOjJ9.VlVwCjtGz1jP4cMW3fbnL80peUE-Ne6C4dagTRZ-6gAAesEii6T6mdbglC8u2nEjpqOK7Sn5XQklG7aZ8QaCbfC-ZIkDuUMRVRVr5R5OBtL35IBuhX3px9VLzbSdxLUeukRF2hL570IA_xTvKup1sKaoCC3_avI-AsidcGJ6V0C1Zv0pSXKeA_5mXPeDNVQM0JVEVRwjKRT9y1-1dsX5KT21psskiW4dmY97A0E3xsCLU954Z3cSbUpZKoYGd4XCL7klMS6ui6KEC0Yoj_NK8Ot0Q8I_JDnLYEzCCTd_NJI4a2Wkvu9Y8ZVtAKEihS46uMqf0r4FzaWRc05OmsNN5g"

  val testIdTokenWithBoolEmailVerified =
    "eyJraWQiOiI4NkQ4OEtmIiwiYWxnIjoiUlMyNTYifQ.eyJpc3MiOiJodHRwczovL2FwcGxlaWQuYXBwbGUuY29tIiwiYXVkIjoiY29tLm1pa2UuYXBwbGUtc3NvLmRlbW84ODgiLCJleHAiOjE2MjIwNTM4MjIsImlhdCI6MTYyMTk2NzQyMiwic3ViIjoiMDAxMTgyLmM3ODM2ZDdjYjY0MzQzNmZhYWI5MmQ4ZGNkODQxZjBkLjE3NTEiLCJjX2hhc2giOiJ1TTFzTEh6TE43QU9rQ2dNWWcxQzNBIiwiZW1haWwiOiJwYmR0Z3J2MmJ0QHByaXZhdGVyZWxheS5hcHBsZWlkLmNvbSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJpc19wcml2YXRlX2VtYWlsIjoidHJ1ZSIsImF1dGhfdGltZSI6MTYyMTk2NzQyMiwibm9uY2Vfc3VwcG9ydGVkIjp0cnVlLCJyZWFsX3VzZXJfc3RhdHVzIjoyfQ.dX0lk28ZaJtObCTjvv6ApCy6pmUUBhcuWlDLfbYNiwrBpBagBgQBXOWFneiUZ86QukbuI6U923Nsf1v8kR60m_79TPAJ5iTRJ56pVWdShm5NsawewuRIiQpzBTDVWqiK0VrAKptUw-qJ-PjXUMLfzq8ewgh3a3B4E_mVYyWknTv1RcVHzkVaB4DodSmm2PBx2ObwzB6yr4FSN6qWEyOQqg31yUuy6s3symH5xvlBbvvLmqBzzKmnDotd6k7Yc2lFp54BW7DIyOrIIFKcudi9AM7ekR6GDWB6-FG_ZDPMlPzn96V4lh8BBw3b4Q4Kw5w59DxDYgLFvV9b7hDgQdLrsg"

  val testIdTokenWithoutEmailVerified =
    "eyJraWQiOiI4NkQ4OEtmIiwiYWxnIjoiUlMyNTYifQ.eyJpc3MiOiJodHRwczovL2FwcGxlaWQuYXBwbGUuY29tIiwiYXVkIjoiY29tLm1pa2UuYXBwbGUtc3NvLmRlbW84ODgiLCJleHAiOjE2MjIwNTM4MjIsImlhdCI6MTYyMTk2NzQyMiwic3ViIjoiMDAxMTgyLmM3ODM2ZDdjYjY0MzQzNmZhYWI5MmQ4ZGNkODQxZjBkLjE3NTEiLCJjX2hhc2giOiJ1TTFzTEh6TE43QU9rQ2dNWWcxQzNBIiwiZW1haWwiOiJwYmR0Z3J2MmJ0QHByaXZhdGVyZWxheS5hcHBsZWlkLmNvbSIsImlzX3ByaXZhdGVfZW1haWwiOiJ0cnVlIiwiYXV0aF90aW1lIjoxNjIxOTY3NDIyLCJub25jZV9zdXBwb3J0ZWQiOnRydWUsInJlYWxfdXNlcl9zdGF0dXMiOjJ9.vD7WhkJ64mSNBzutYrAwhwt5XQvwOkEk72LqC3h64njPF-2ngxwqqjOp-hMZi1l3ndsRHE8RAiuEEX9cTyjCJrQC69qEOhYbO0OLIJ87ScdrUfYJ4itRDcBKJ3bmhkgWezBvPbQT-IvvfD833q8_FwnhYOfP7_Ko-ofaip3BmulWVkPz4-y4LgXUXDubjBYXfmpTNJnjS18qAEu2VorkpeqWkLLe3u4hW5s5_l_Sd8fXzB2XN4eISLcK7nN61omWNOnR-hjHTazjdZFvQ1BLxSKp6-DWuTCQKfHh_9B5sZ6tnk2pV87hDZtD4qWW6NOgPhfeuEsUzmGLLwKHw8yeDg"

  val client = new AppleIdProviderClient(service, mapper, verifier)
  test("validates Apple ID Token correctly") {
    when(verifier.verify(any[IdToken])).thenReturn(false)
    assertResult(false)(Await.result(client.isIdTokenValid(testIdToken)))
    when(verifier.verify(any[IdToken])).thenReturn(true)
    assertResult(true)(Await.result(client.isIdTokenValid(testIdToken)))
  }

  test("extracts from Apple ID Token correctly") {
    assertResult(
      SsoProviderInfo(
        providerSsoId = "001182.c7836d7cb643436faab92d8dcd841f0d.1751",
        emailAddress = "pbdtgrv2bt@privaterelay.appleid.com",
        emailVerified = Some(true))
    )(client.extractSsoProviderInfo(testIdToken))
  }

  test("extracts from Apple ID Token correctly when email_verified is boolean") {
    assertResult(
      SsoProviderInfo(
        providerSsoId = "001182.c7836d7cb643436faab92d8dcd841f0d.1751",
        emailAddress = "pbdtgrv2bt@privaterelay.appleid.com",
        emailVerified = Some(true))
    )(client.extractSsoProviderInfo(testIdTokenWithBoolEmailVerified))
  }

  test("extracts from Apple ID Token correctly when email_verified is missing") {
    assertResult(
      SsoProviderInfo(
        providerSsoId = "001182.c7836d7cb643436faab92d8dcd841f0d.1751",
        emailAddress = "pbdtgrv2bt@privaterelay.appleid.com",
        emailVerified = None)
    )(client.extractSsoProviderInfo(testIdTokenWithoutEmailVerified))
  }
}
