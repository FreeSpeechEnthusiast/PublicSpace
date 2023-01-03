namespace java com.twitter.auth.policykeeper.thriftjava
#@namespace scala com.twitter.auth.policykeeper.thriftscala
namespace rb policykeeper

include "com/twitter/bouncer/bounce.thrift"
include "com/twitter/bouncer/templates.thrift"
include "com/twitter/bouncer/assignments.thrift"

struct BouncerRequest {
 1: required bounce.Bounce bounce
 2: required list<templates.TemplateId> template_ids
 3: optional templates.TemplateData template_data
 4: optional set<templates.Tag> referring_tags
 5: required assignments.Target target
}

struct BouncerTargetSettings {
 1: required string target_type
 2: optional string user_id
 3: optional string session_hash
 4: optional string feature
}

struct DeepStringMap {
  1: required bool is_map
  2: optional string string_val
  3: optional map<string,DeepStringMap> map_val
}

struct BouncerSettings {
 1: optional string location
 2: optional string error_message
 3: optional string deep_link
 4: optional string experience
 5: required list<string> template_ids
 6: optional map<string,DeepStringMap> template_mapping
 7: optional set<string> referring_tags
 8: required BouncerTargetSettings target
}
