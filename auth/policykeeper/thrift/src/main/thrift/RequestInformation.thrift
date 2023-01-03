namespace java com.twitter.auth.policykeeper.thriftjava
#@namespace scala com.twitter.auth.policykeeper.thriftscala
namespace rb policykeeper

struct RequestInformation {
 1: required string path
 2: optional string host
 3: required string method
 4: optional map<string, string> query_params
 5: optional map<string, string> body_params
}
