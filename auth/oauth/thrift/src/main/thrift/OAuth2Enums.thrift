namespace java com.twitter.auth.oauth2.thriftjava
#@namespace scala com.twitter.auth.oauth2.thriftscala
namespace rb oauth2
#@namespace strato com.twitter.auth.oauth2

enum CodeChallengeMethod {
	S256 = 1,
	PLAIN = 2,
}

enum ClientType {
  THIRD_PARTY_APP = 1,
  SERVICE_CLIENT = 2
}

enum OAuthTokenType {
  OAUTH_2_APP_ONLY = 1
  GUEST = 2
  OAUTH_2_AUTHORIZATION_CODE = 3
  OAUTH_2_ACCESS_TOKEN = 4
  OAUTH_2_REFRESH_TOKEN = 5
}

