namespace java com.twitter.auth.customerauthtooling.thriftjava
#@namespace scala com.twitter.auth.customerauthtooling.thriftscala
namespace rb CustomerAuthTooling

include "EndpointInfo.thrift"
include "com/twitter/auth/AuthenticationType.thrift"

struct ExperimentBucket {
  1: required string key
  2: required string bucket
}

struct DataPermissionAnnotation {
  1: required i64 id
  2: optional string state
}

struct RouteInfo {
 1: required string path
 2: required set<string> domains
 3: required string cluster
 4: optional EndpointInfo.RequestMethod method
 5: optional set<AuthenticationType.AuthenticationType> authTypes
 6: optional set<DataPermissionAnnotation> requiredDps
 7: optional set<string> userRoles
 8: optional set<string> routeFlags
 9: optional set<string> featurePermissions
 10: optional set<string> subscriptionPermissions
 11: optional string decider
 12: optional i32 priority
 13: optional set<string> tags
 14: optional set<ExperimentBucket> experimentBuckets
 15: optional set<string> uaTags
 16: optional i32 rateLimit
 17: optional i32 timeoutMs
 18: optional set<string> ldapOwners
 19: optional string id
 20: optional string lifeCycle
 21: optional set<string> scopes
 22: optional string description
 23: optional string requestCategory
 24: optional string projectId
}

struct PartialRouteInfo {
 1: optional string path
 2: optional set<string> domains
 3: optional string cluster
 4: optional EndpointInfo.RequestMethod method
 5: optional set<AuthenticationType.AuthenticationType> authTypes
 6: optional set<DataPermissionAnnotation> requiredDps
 7: optional set<string> userRoles
 8: optional set<string> routeFlags
 9: optional set<string> featurePermissions
 10: optional set<string> subscriptionPermissions
 11: optional string decider
 12: optional i32 priority
 13: optional set<string> tags
 14: optional set<ExperimentBucket> experimentBuckets
 15: optional set<string> uaTags
 16: optional i32 rateLimit
 17: optional i32 timeoutMs
 18: optional set<string> ldapOwners
 19: optional string id
 20: optional string lifeCycle
 21: optional set<string> scopes
 22: optional string description
 23: optional string requestCategory
 24: optional string projectId
}
