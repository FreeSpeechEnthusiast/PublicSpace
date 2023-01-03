package com.twitter.auth.apiverification

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.util.Await
import com.twitter.util.logging.Logger

object VNextApiRunner {

  def runAllV2APIs(
    accessToken: String,
    client: Service[Request, Response],
    logger: Logger,
    expectedStatus: Status,
    statsReceiver: StatsReceiver
  ): Unit = {
    // v2 Tweets
    runV2GetTweets(
      accessToken = accessToken,
      client = client,
      logger = logger,
      expected = expectedStatus,
      statsReceiver = statsReceiver.scope("GET/2/tweets"))
    runV2GetTweetById(
      accessToken = accessToken,
      client = client,
      logger = logger,
      expected = expectedStatus,
      statsReceiver = statsReceiver.scope("GET/2/tweets/{id}"))
    // v2 Users
    runV2GetUsers(
      accessToken = accessToken,
      client = client,
      logger = logger,
      expected = expectedStatus,
      statsReceiver = statsReceiver.scope("GET/2/users"))
    runV2GetUserById(
      accessToken = accessToken,
      client = client,
      logger = logger,
      expected = expectedStatus,
      statsReceiver = statsReceiver.scope("GET/2/users/{id}"))
    runV2GetUsersByUsernames(
      accessToken = accessToken,
      client = client,
      logger = logger,
      expected = expectedStatus,
      statsReceiver = statsReceiver.scope("/2/users/by?usernames"))
    // v2 Users Following
    runV2PostUsersFollowing(
      accessToken = accessToken,
      client = client,
      logger = logger,
      expected = expectedStatus,
      statsReceiver = statsReceiver.scope("POST/2/users/{id}/following")
    )
    runV2GetUsersFollowing(
      accessToken = accessToken,
      client = client,
      logger = logger,
      expected = expectedStatus,
      statsReceiver = statsReceiver.scope("GET/2/users/{id}/following")
    )
    runV2DeleteUsersFollowing(
      accessToken = accessToken,
      client = client,
      logger = logger,
      expected = expectedStatus,
      statsReceiver = statsReceiver.scope("DELETE/2/users/{id}/following")
    )
    // v2 Space
    runV2GetSpaces(
      accessToken = accessToken,
      client = client,
      logger = logger,
      expected = expectedStatus,
      statsReceiver = statsReceiver.scope("GET/2/spaces/:id")
    )
    runV2GetSpacesByCreatorIds(
      accessToken = accessToken,
      client = client,
      logger = logger,
      expected = expectedStatus,
      statsReceiver = statsReceiver.scope("GET/2/spaces/by/creator_ids?user_ids=:id")
    )
    runV2GetSpacesByIds(
      accessToken = accessToken,
      client = client,
      logger = logger,
      expected = expectedStatus,
      statsReceiver = statsReceiver.scope("GET/2/spaces?ids=:id")
    )
  }

  // /2/tweets
  def runV2GetTweets(
    accessToken: String,
    client: Service[Request, Response],
    logger: Logger,
    expected: Status,
    statsReceiver: StatsReceiver
  ): Status = {
    logger.info(s"Request: v2 GET Tweet /2/tweets?ids=1261326399320715264,1278347468690915330")

    val request = HttpRequestHelper.vnextV2GetRequest(
      tokenInHeader = accessToken,
      path = "/2/tweets?ids=1261326399320715264,1278347468690915330")
    val response: Response = Await.result(client(request))
    val status: Status = response.status

    HttpRequestHelper.assertStatusCode(expected, status, statsReceiver)
    logger.info(s"Response: v2 GET Tweets HTTP Status: $status.")
    status
  }

  // /2/tweets/{id}
  def runV2GetTweetById(
    accessToken: String,
    client: Service[Request, Response],
    logger: Logger,
    expected: Status,
    statsReceiver: StatsReceiver
  ): Status = {
    logger.info(s"Request: v2 GET Tweet by Id /2/tweets/1261326399320715264")

    val request = HttpRequestHelper.vnextV2GetRequest(accessToken, "/2/tweets/1261326399320715264")
    val response: Response = Await.result(client(request))
    val status: Status = response.status

    HttpRequestHelper.assertStatusCode(expected, status, statsReceiver)
    logger.info(s"Response: v2 GET Tweet by Id HTTP Status: $status.")
    status
  }

  // /2/users
  def runV2GetUsers(
    accessToken: String,
    client: Service[Request, Response],
    logger: Logger,
    expected: Status,
    statsReceiver: StatsReceiver
  ): Status = {
    logger.info(s"Request: v2 GET Users /2/users?ids=2244994945,6253282")

    val request =
      HttpRequestHelper.vnextV2GetRequest(accessToken, "/2/users?ids=2244994945,6253282")
    val response: Response = Await.result(client(request))
    val status: Status = response.status

    HttpRequestHelper.assertStatusCode(expected, status, statsReceiver)
    logger.info(s"Response: v2 GET Users HTTP Status: $status.")
    response.status
  }

  // /2/users/{id}
  def runV2GetUserById(
    accessToken: String,
    client: Service[Request, Response],
    logger: Logger,
    expected: Status,
    statsReceiver: StatsReceiver
  ): Status = {
    logger.info(s"Request: v2 GET User by Id /2/users/2244994945")

    val request =
      HttpRequestHelper.vnextV2GetRequest(accessToken, "/2/users/2244994945")
    val response: Response = Await.result(client(request))
    val status: Status = response.status

    HttpRequestHelper.assertStatusCode(expected, status, statsReceiver)
    logger.info(s"Response: v2 GET User by Id HTTP Status: $status.")
    status
  }

  // /2/users/by?usernames={ids}
  def runV2GetUsersByUsernames(
    accessToken: String,
    client: Service[Request, Response],
    logger: Logger,
    expected: Status,
    statsReceiver: StatsReceiver
  ): Status = {
    logger.info(
      s"Request: v2 GET Users By Usernames https://api.twitter.com/2/users/by?usernames=TwitterDev,Twitter")

    val request =
      HttpRequestHelper.vnextV2GetRequest(
        accessToken,
        "https://api.twitter.com/2/users/by?usernames=TwitterDev,Twitter")
    val response: Response = Await.result(client(request))
    val status: Status = response.status

    HttpRequestHelper.assertStatusCode(expected, status, statsReceiver)
    logger.info(s"Response: v2 GET Users By Usernames HTTP Status: $status.")
    status
  }

  // POST/2/users/{id}/following
  def runV2PostUsersFollowing(
    accessToken: String,
    client: Service[Request, Response],
    logger: Logger,
    expected: Status,
    statsReceiver: StatsReceiver
  ): Status = {
    logger.info(s"Request: v2 POST User Following /2/users/1176196242566574085/following")

    val request =
      HttpRequestHelper.vnextV2PostRequest(
        tokenInHeader = accessToken,
        path = "/2/users/1176196242566574085/following",
        content = "{\"target_user_id\": \"2244994945\"}")
    val response: Response = Await.result(client(request))
    val status: Status = response.status

    HttpRequestHelper.assertStatusCode(expected, status, statsReceiver)
    logger.info(s"Response: v2 POST User Following Status: $status.")
    status
  }

  // DELETE/2/users/{id}/following
  def runV2DeleteUsersFollowing(
    accessToken: String,
    client: Service[Request, Response],
    logger: Logger,
    expected: Status,
    statsReceiver: StatsReceiver
  ): Status = {
    logger.info(
      s"Request: v2 DELETE User Following /2/users/1176196242566574085/following/2244994945")

    val request =
      HttpRequestHelper.vnextV2DeleteRequest(
        tokenInHeader = accessToken,
        path = "/2/users/1176196242566574085/following/2244994945",
        content = "")
    val response: Response = Await.result(client(request))
    val status: Status = response.status

    HttpRequestHelper.assertStatusCode(expected, status, statsReceiver)
    logger.info(s"Response: v2 DELETE User Following Status: $status.")
    status
  }

  // GET/2/users/{id}/following
  def runV2GetUsersFollowing(
    accessToken: String,
    client: Service[Request, Response],
    logger: Logger,
    expected: Status,
    statsReceiver: StatsReceiver
  ): Status = {
    logger.info(s"Request: v2 GET User Following /2/users/1176196242566574085/following")
    val request =
      HttpRequestHelper.vnextV2GetRequest(
        tokenInHeader = accessToken,
        path = "/2/users/1176196242566574085/following")
    val response: Response = Await.result(client(request))
    val status: Status = response.status

    HttpRequestHelper.assertStatusCode(expected, status, statsReceiver)
    logger.info(s"Response: v2 GET User Following Status: $status.")
    status
  }

  // GET/2/spaces/
  def runV2GetSpaces(
    accessToken: String,
    client: Service[Request, Response],
    logger: Logger,
    expected: Status,
    statsReceiver: StatsReceiver
  ): Status = {
    logger.info(s"Request: v2 GET Spaces /2/spaces/:id")
    val request =
      HttpRequestHelper.vnextV2GetRequest(
        tokenInHeader = accessToken,
        path = "/2/spaces/1DXxyRYNejbKM")
    val response: Response = Await.result(client(request))
    val status: Status = response.status

    HttpRequestHelper.assertStatusCode(expected, status, statsReceiver)
    logger.info(s"Response: v2 GET Spaces /2/spaces/:id Status: $status.")
    status
  }

  // GET/2/spaces/by/creator_ids
  def runV2GetSpacesByCreatorIds(
    accessToken: String,
    client: Service[Request, Response],
    logger: Logger,
    expected: Status,
    statsReceiver: StatsReceiver
  ): Status = {
    logger.info(s"Request: v2 GET Spaces by Creator Ids /2/spaces/by/creator_ids?user_ids=:id")
    val request =
      HttpRequestHelper.vnextV2GetRequest(
        tokenInHeader = accessToken,
        path = "/2/spaces/by/creator_ids?user_ids=2244994945,6253282")
    val response: Response = Await.result(client(request))
    val status: Status = response.status

    HttpRequestHelper.assertStatusCode(expected, status, statsReceiver)
    logger.info(
      s"Response: v2 GET Spaces by Creator Ids /2/spaces/by/creator_ids?user_ids=:id Status: $status.")
    status
  }

  // GET/2/spaces?ids=:id
  def runV2GetSpacesByIds(
    accessToken: String,
    client: Service[Request, Response],
    logger: Logger,
    expected: Status,
    statsReceiver: StatsReceiver
  ): Status = {
    logger.info(s"Request: v2 GET Spaces by ids /2/spaces?ids=:id")
    val request =
      HttpRequestHelper.vnextV2GetRequest(
        tokenInHeader = accessToken,
        path = "/2/spaces?ids=1DXxyRYNejbKM,1nAJELYEEPvGL")
    val response: Response = Await.result(client(request))
    val status: Status = response.status

    HttpRequestHelper.assertStatusCode(expected, status, statsReceiver)
    logger.info(s"Response: v2 GET Spaces by ids /2/spaces?ids=:id Status: $status.")
    status
  }
}
