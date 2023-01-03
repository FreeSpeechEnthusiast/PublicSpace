namespace java com.twitter.auth.oauth2.thriftjava
#@namespace scala com.twitter.auth.oauth2.thriftscala
#@namespace strato com.twitter.auth.oauth2
namespace rb oauth2
#@namespace strato com.twitter.passbird.accesstoken
#@namespace strato com.twitter.auth.oauth2

include "OAuth2Enums.thrift"
/**
* Thrift models used by data layer (storage service) only, and should not be used for Thirft APIs
* (e.g. FlightAuth).
* DO NOT change the existing fields order as the Manhattan index is based on the field position
* and changing the existing order of the field will result into wrong indexing or errors during index
* creation
**/
struct OAuth2AccessTokenStorageThrift {
 /** access token key*/
 1: required string tokenKey
  /** should be used by other system as indentifier for token */
 2: required string tokenHash
 /** token type of the access token */
 3: required OAuth2Enums.OAuthTokenType tokenType
 /** client application id (not the clientId)*/
 4: required i64 clientApplicationId (personalDataType = 'AppId')
 /** userId of the authenticated user.
 * DO NOT change the order of this field as there is a MH index called user_id for this fied position
**/
 5: required i64 userId (personalDataType = 'UserId')
 /** one or more scopes (for user data) that the user wants to access */
 6: required set<string> scopes
 /** epoch time of the token expiretation*/
 7: required i64 expiresAt (personalDataType = 'PrivateTimestamp')
 /** epoch time of the token creation */
 8: required i64 createdAt (personalDataType = 'PrivateTimestamp')
 /** epoch time of the token updation */
 9: optional i64 updatedAt (personalDataType = 'PrivateTimestamp')
 /** last time this token is used by user */
 10: optional i64 lastSeenAt (personalDataType = 'PrivateTimestamp')
 /** flag to indicate if the token is deleted due to TTL/exchange operation or by manually using revoke endpoint */
 11: optional bool isAutoDeleted
 /** refresh token key associate with the access token */
 12: optional string refreshTokenKey
 /** epoch time of the token invalidated */
 13: optional i64 invalidateAt (personalDataType = 'PrivateTimestamp')
 /** ttl for token, this field is used to set TTL in mh */
 14: optional i64 ttl (personalDataType = 'PrivateTimestamp')
 /**
  * epoch time of token authorization, this indicates time when user authorized the app, if
  * the app exchanges refresh token with access token this field will still show the time when
  * user first authorized the app
 **/
 15: optional i64 authorizedAt (personalDataType = 'PrivateTimestamp')
} (persisted='true', hasPersonalData='true')

struct OAuth2RefreshTokenStorageThrift {
 /** refresh token key */
 1: required string tokenKey
   /** should be used by other system as indentifier for token */
 2: required string tokenHash
 /** hash of the access token associate with the refresh token */
 3: required string accessTokenKey
 /** client application id (not the clientId)*/
 4: required i64 clientApplicationId (personalDataType = 'AppId')
 /** userId of the authenticated user */
 5: required i64 userId (personalDataType = 'UserId')
 /** epoch time of the token expiretation*/
 6: required i64 expiresAt (personalDataType = 'PrivateTimestamp')
 /** epoch time of the token creation */
 7: required i64 createdAt (personalDataType = 'PrivateTimestamp')
 /** flag to indicate if the token is deleted due to TTL/exchange operation or by manually using revoke endpoint */
 8: optional bool isAutoDeleted
 /**
 * one or more scopes (for user data) that the user wants to access, this is use to exchange refresh token
 * with access token when access token is already expired and we need to know the scope requested
 * during access token creation
 **/
 9: required set<string> scopes,
 /**
  * epoch time of token authorization, this indicates time when user authorized the app, if
  * the app exchanges refresh token with access token this field will still show the time when
  * user first authorized the app
 **/
 10: optional i64 authorizedAt (personalDataType = 'PrivateTimestamp')
} (persisted='true', hasPersonalData='true')

struct OAuth2AppOnlyTokenStorageThrift {
 /** access token key */
 1: required string tokenKey (personalDataType = "AccessToken")
  /** should be used by other system as indentifier for token */
 2: required string tokenHash
 /** client application id (not the clientId)*/
 3: required i64 clientApplicationId (personalDataType = 'AppId')
 /** epoch time of the token creation */
 4: required i64 createdAt (personalDataType = 'PrivateTimestamp')
 /** epoch time of the token updation */
 5: optional i64 updatedAt (personalDataType = 'PrivateTimestamp')
 /** last time this token is used by user */
 6: optional i64 lastSeenAt (personalDataType = 'PrivateTimestamp')
 /** epoch time of the token authorization */
 7: required i64 authorizedAt (personalDataType = 'PrivateTimestamp')
 /** epoch time of the token invalidation*/
 8: optional i64 invalidateAt (personalDataType = 'PrivateTimestamp')
} (persisted='true', hasPersonalData='true')

struct OAuth2ClientAccessTokenStorageThrift {
 /** access token key */
 1: required string tokenKey (personalDataType = "AccessToken")
 /** should be used by other system as indentifier for token */
 2: required string tokenHash
 /** client id **/
 3: required string clientId
 /** one or more scopes that the client wants to access */
 4: required set<string> scopes
 /** epoch time of the token expiretation*/
 5: required i64 expiresAt (personalDataType = 'PrivateTimestamp')
 /** epoch time of the token creation */
 6: required i64 createdAt (personalDataType = 'PrivateTimestamp')
 /** epoch time of the token invalidated */
 7: optional i64 invalidateAt (personalDataType = 'PrivateTimestamp')
 /** client type of token */
 8: required OAuth2Enums.ClientType clientType
} (persisted='true', hasPersonalData='true')
