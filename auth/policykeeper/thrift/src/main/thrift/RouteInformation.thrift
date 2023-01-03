namespace java com.twitter.auth.policykeeper.thriftjava
#@namespace scala com.twitter.auth.policykeeper.thriftscala
namespace rb policykeeper

include "RequestInformation.thrift"

struct RouteInformation {
 1: required bool is_ng_route
 2: optional set<string> route_tags
 3: optional RequestInformation.RequestInformation request_information
}
