namespace java com.twitter.auth.customerauthtooling.thriftjava
#@namespace scala com.twitter.auth.customerauthtooling.thriftscala
namespace rb CustomerAuthTooling

include "EndpointMetadata.thrift"

enum RequestMethod {
    GET = 0,
    POST = 1,
    PATCH = 2,
    UPDATE = 3,
    DELETE = 4,
    HEAD = 5,
    OPTIONS = 6,
}

struct EndpointInfo {
 1: required string url
 2: optional RequestMethod method
 3: optional EndpointMetadata.EndpointMetadata metadata
}
