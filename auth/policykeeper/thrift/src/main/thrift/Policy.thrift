namespace java com.twitter.auth.policykeeper.thriftjava
#@namespace scala com.twitter.auth.policykeeper.thriftscala
namespace rb policykeeper

include "Bouncer.thrift"

struct Policy {
 1: required string policy_id //immutable human-friendly unique policy identifier, matching ([a-zA-Z0-9_]+) pattern
 2: optional string decider
 3: optional set<string> data_providers
 4: optional set<string> eligibility_criteria
 5: required set<Rule> rules
 6: required string name
 7: required string description
 8: optional bool fail_closed
}

struct Rule {
  1: required string expression
  2: required RuleAction action
  3: required i64 priority // sets the priority of a rule where 0 is maximum
  // if fallback_action is set, then in case of expression failure (due to missing input or other conditions) rule evaluation will return fallback_action
  4: optional RuleAction fallback_action
}

struct RuleAction {
  1: required bool action_needed
  // finatra api error code, see https://sourcegraph.twitter.biz/git.twitter.biz/source/-/blob/finatra-internal/api11/src/main/scala/com/twitter/finatra/api11/ApiError.scala
  2: optional i32 api_error_code
  // as option bouncer can be used if api_error_code is not set
  3: optional Bouncer.BouncerSettings bouncer_settings
}

