namespace java com.twitter.auth.policykeeper.thriftjava
#@namespace scala com.twitter.auth.policykeeper.thriftscala
namespace rb policykeeper

include "com/twitter/tsla/auth_events/AuthEvents.thrift"

struct AuthMetadata {
 1: required bool has_access_token
 2: optional list<AuthEvents.AuthEvent> auth_events
 3: optional i64 gizmoduck_user_id # see com.twitter.finatra.gizmoduck.GizmoduckUserContext for more information
 4: optional string token
 5: optional i32 token_kind
}
