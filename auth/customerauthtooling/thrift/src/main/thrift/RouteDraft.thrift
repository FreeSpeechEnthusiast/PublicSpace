namespace java com.twitter.auth.customerauthtooling.thriftjava
#@namespace scala com.twitter.auth.customerauthtooling.thriftscala
namespace rb CustomerAuthTooling

enum AppliedAction {
    INSERT = 0,
    UPDATE = 1,
    DELETE = 2,
    NOTHING = 3,
    ERROR = 4
}

struct RouteDraft {
 1: required string uuid
 2: required string expectedRouteId
 3: optional AppliedAction action
}

struct BatchRouteDraft {
 1: required i32 updated = 0
 2: required i32 inserted = 0
 3: required i32 ignoredInvalid = 0
 4: required i32 ignoredDueToErrors = 0
 5: required i32 unchanged = 0
 6: optional set<RouteDraft> routeDrafts
 7: optional bool wasStopped = false
 8: optional list<string> errors
 9: optional list<string> warnings
 10: optional list<string> messages
}
