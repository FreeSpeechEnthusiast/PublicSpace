package com.twitter.auth.apiverification

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.util.Await
import com.twitter.util.logging.Logger

object AuthTestApiRunner {

  def runAuthTestLoggedOutApi(
    accessToken: String,
    client: Service[Request, Response],
    logger: Logger,
    statsReceiver: StatsReceiver
  ): Unit = {
    logger.info(s"Request: Auth Test Logged Out /tfetestservice/auth/logged_out.txt")
    statsReceiver.scope("auth_test_logged_out").counter("count").incr()

    val request =
      HttpRequestHelper.authTestRequest(
        accessToken,
        "https://api.twitter.com/tfetestservice/auth/logged_out.txt")

    val response: Response = Await.result(client(request))

    logger.info(s"Response: Auth Test Logged Out HTTP Status: ${response.status}.")
    statsReceiver.scope("auth_test_logged_out").counter(s"${response.status}").incr()
  }

}
