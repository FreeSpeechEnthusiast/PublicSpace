namespace java com.twitter.auth.thrift.authenforcement
namespace py gen.twitter.auth.AuthEnforcement
namespace rb AuthEnforcement

#@namespace scala com.twitter.auth.authenforcement.thriftscala
#@namespace strato com.twitter.auth.authenforcement

include "com/twitter/auth/PassportType.thrift"
include "com/twitter/auth/paseto/TamperProofing.thrift"

/**
* Principal (PDP Customer Auth)
*
* A single verified identity associated with a request.
**/
union Principal {
  1: UserPrincipal userPrincipal
  2: AuthenticatedUserPrincipal authenticatedUserPrincipal
  3: ClientApplicationPrincipal clientApplicationPrincipal
  4: SessionPrincipal sessionPrincipal
  5: EmployeePrincipal employeePrincipal  // Employee_Passport LDAP username
  6: GuestPrincipal guestPrincipal
  7: ServiceClientPrincipal serviceClientPrincipal
}

struct UserPrincipal {
  1: required i64 userId
}

struct ClientApplicationPrincipal {
  1: required i64 clientApplicationId
}

struct ServiceClientPrincipal {
  1: required string clientId
}

struct SessionPrincipal {
   /**
   * Session hash is required for auth types like Session, OAuth2 + Session. It is also used to
   * uniquely identify a user session.
   **/
  1: required string sessionHash
   /**
   * Scopes are optional: OAuth1 may actually have scopes; OAuth2+Session may instead have
   * an special "allow-all" scope
   * Note that scopes are only used in the the AuthZ process. Once passport is created
   * with DPs/FPs, scopes will be removed, and passport signed.
   * In other words, scopes will NOT be available to downstream services
   **/
  2: optional set<string> scopes
}

struct AuthenticatedUserPrincipal {
  1: required i64 userId
}

struct EmployeePrincipal {
  1: optional string ldapAccount
}

struct GuestPrincipal {
  1: required i64 guestToken
}

enum DataPermissionState {
    ENFORCED = 1,
    OPTIONAL = 2
}

struct DataPermission {
  1: optional i64 id
  2: optional DataPermissionState state
  3: optional bool testing
}

/**
* Policy (PDP Customer Auth)
*
* Determines permissions required for access.
**/
struct Policy {
  // DEPRECATED - use dataPermissionsAnotated instead
  1: optional set<string> dataPermissions
  2: optional set<string> featurePermissions
  3: optional set<DataPermission> dataPermissionsAnotated
  4: optional set<string> subscriptionPermissions
}

/**
* Customer Auth Passport (PDP Customer Auth)
*
* Tamper-resistant structure containing requestor’s authenticated principals and associated
* authorized feature flags and data permissions. Also contains records of complex request policy
* checks that have already been performed.
*
* Note: Only fields affect policy decisions should be included in Passport
**/
struct Passport {
  1: required string passportId
  2: required set<Principal> principals
  3: optional DataPermissionDecisions dataPermissionDecisions
  4: optional FeaturePermissionDecisions featurePermissionDecisions
  5: optional PassportType.PassportType passportType
  6: optional SubscriptionPermissionDecisions subscriptionPermissionDecisions
  /**
  * Reserved for verifying integrity of a Passport
  **/
  999: optional Metadata metadata
}

struct DataPermissionDecisions {
  1: optional set<string> allowedDataPermissions
  2: optional set<string> rejectedDataPermissions
  3: optional set<i64> allowedDataPermissionIds
  4: optional set<i64> rejectedDataPermissionIds
}

struct FeaturePermissionDecisions {
  1: optional set<string> allowedFeaturePermissions
  2: optional set<string> rejectedFeaturePermissions
}

struct SubscriptionPermissionDecisions {
  1: optional set<string> allowedSubscriptionPermissions
  2: optional set<string> rejectedSubscriptionPermissions
}

struct Metadata {
  1: optional TamperProofing.PasetoMetadata pasetoMetadata
}

struct LegacyMetadata {
  1: optional binary accessToken  // oauth token (OAuth1, OAuth2)
  2: optional binary clientApplication
  3: optional binary sessionToken // session/web token (Session)
  4: optional binary requestToken
  5: optional binary passbirdToken // TIA token
  6: optional binary clientAccessToken
  7: optional binary serviceClient
}

/**
* Auth Context (PDP Customer Auth)
*
* Twitter Broadcast Context that is transmitted through the RPC tree. Contains pertinent auth
* metadata and passport.
**/
struct AuthContext {
  1: optional Passport passport
  /**
  * Reserved for carrying legacy headers populated by TFE
  **/
  2: optional LegacyMetadata legacyMetadata
}
