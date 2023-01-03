namespace java com.twitter.auth.oauth2.thriftjava
#@namespace scala com.twitter.auth.oauth2.thriftscala
#@namespace strato com.twitter.auth.oauth2
namespace rb oauth2
#@namespace strato com.twitter.auth.oauth2
include "OAuth2Enums.thrift"

/**
* Thrift models used by data layer (storage service) only, and should not be used for Thirft APIs
* (e.g. FlightAuth).
**/
struct OAuth2AuthorizationCodeStorageThrift {
 /** authorization code key */
  1: required string codeKey
 /** authorization code hash */
  2: required string codeHash
 /** randomly generated CLIENT secret string, to be verified by the client */
  3: required string state
 /** client ID (generated when the app was first created & registered w/ Twitter) */
  4: required string clientId (personalDataType = 'ConsumerKey')
 /** userId of the authenticated user */
  5: required i64 userId (personalDataType = 'UserId')
 /** URI to return the user to after authorization is done */
  6: required string redirectUri (personalDataType = 'AppCallbackUrl')
 /** one or more scopes (for user data) that the user wants to access */
  7: required set<string> scopes (personalDataType = 'AuthorizationAccessModel')
 /** code challange method */
  8: required OAuth2Enums.CodeChallengeMethod codeChallengeMethod
 /** code challange */
  9: required string codeChallenge
 /** TTL lifetime in seconds */
  10: required i64 expiresAt (personalDataType = 'PrivateTimestamp')
 /** time of the code creation */
  11: required i64 createdAt (personalDataType = 'PrivateTimestamp')
 /** time of the code creation */
  12: optional i64 approvedAt (personalDataType = 'PrivateTimestamp')
}(persisted='true', hasPersonalData='true')

