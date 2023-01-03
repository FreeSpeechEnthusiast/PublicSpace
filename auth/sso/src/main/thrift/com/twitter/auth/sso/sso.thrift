namespace java com.twitter.auth.sso.thriftjava
#@namespace scala com.twitter.auth.sso.thriftscala
#@ namespace strato com.twitter.auth.sso

include "thrift_struct_encryptor.thrift"

enum AssociationMethod {
  Signup = 0
  Login = 1
}(persisted='true')

enum SsoProvider {
  Google = 0,
  Apple = 1,
  Test = 2
}

struct SsoMetaV1 {
  1: required string ssoId(personalDataType = 'ExternalUserIdUsedForAuthentication')
  2: required string ssoEmail(personalDataType = 'UserEmailAddress')
  3: required AssociationMethod associationMethod
  4: required i64 creationTimeMs(personalDataType = 'PrivateTimestamp')
  5: required i32 hashKeyVersion
}(persisted='true', hasPersonalData = 'true')

union SsoMeta {
  1: SsoMetaV1 v1
}(persisted='true')

struct SsoInfo {
  1: required string ssoIdHash
  2: required thrift_struct_encryptor.EncryptedSignedEntity meta
}(persisted='true', hasPersonalData = 'true')

// SSO Info viewable by the public API. 
struct PublicSsoInfo {
  1: required string ssoIdHash
  2: required SsoProvider ssoProvider
}(persisted='true', hasPersonalData = 'true')
