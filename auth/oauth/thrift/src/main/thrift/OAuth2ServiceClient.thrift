namespace java com.twitter.auth.oauth2.thriftjava
#@namespace scala com.twitter.auth.oauth2.thriftscala
#@namespace strato com.twitter.auth.oauth2
namespace rb oauth2
#@namespace strato com.twitter.auth.oauth2

/**
* Thrift models used by data layer (storage service) only, and should not be used for Thirft APIs
* (e.g. FlightAuth).
* DO NOT change the existing fields order as the Manhattan index is based on the field position
* and changing the existing order of the field will result into wrong indexing or errors during index
* creation
**/
struct OAuth2ServiceClientStorageThrift {
  /** client ID for service client */
  1: required string clientId (personalDataType = 'ConsumerKey')
  /** client secret for service client*/
  2: optional string clientSecret (personalDataType = 'ConsumerSecret')
} (persisted='true', hasPersonalData='true')
