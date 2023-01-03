namespace java com.twitter.auth.customerauthtooling.thriftjava
#@namespace scala com.twitter.auth.customerauthtooling.thriftscala
namespace rb CustomerAuthTooling

include "finatra-thrift/finatra_thrift_exceptions.thrift"
include "AdoptionStatus.thrift"
include "EndpointInfo.thrift"
include "RouteDraft.thrift"
include "RouteInfo.thrift"

enum CustomerAuthToolingErrorCode {
	INVALID_INPUT = 1,
	UNKNOWN = 2,
	FUTURE_USE_1 = 3,
}

exception CustomerAuthToolingException {
	1: required CustomerAuthToolingErrorCode errorCode,
	2: optional string message // failure reason
}

struct AdoptionStatsResponse {
 1: required bool isDone
}

struct AdoptionStatsRequest {
 1: required bool shouldReportNGRouteStats
}

struct CheckAdoptionStatusResponse {
 1: required AdoptionStatus.AdoptionStatus adoptionStatus
}

struct CheckAdoptionStatusRequest {
 1: required EndpointInfo.EndpointInfo endpointInfo
}

struct DraftRouteResponse {
 1: required bool status
 2: optional RouteDraft.RouteDraft routeDraft
}

struct DraftRouteRequest {
 1: required RouteInfo.RouteInfo routeInfo
 2: optional bool automaticDecider
 3: optional bool update
}

struct ApplyRouteResponse {
 1: required bool status
 2: optional RouteDraft.RouteDraft routeDraft
}

struct ApplyRouteRequest {
 1: required RouteInfo.PartialRouteInfo routeInfo
 2: optional bool automaticDecider
}

struct ApplyRoutesResponse {
 1: required bool status
 2: optional RouteDraft.BatchRouteDraft batchRouteDraft
}

struct ApplyRoutesRequest {
 1: required set<RouteInfo.PartialRouteInfo> routes
 2: optional bool automaticDecider
 3: optional bool ignoreInvalid
 4: optional bool ignoreErrors
}

struct GetRoutesByRouteIdsResponse {
 1: required bool status
 2: optional set<RouteInfo.RouteInfo> routes
}

struct GetRoutesByRouteIdsRequest {
 1: required set<string> routeIds
}

struct GetRoutesByProjectsResponse {
 1: required bool status
 2: optional set<RouteInfo.RouteInfo> routes
}

struct GetRoutesByProjectsRequest {
 1: required set<string> projects
}

struct DraftRouteFromEndpointResponse {
 1: required bool status
 2: optional RouteDraft.RouteDraft routeDraft
}

struct DraftRouteFromEndpointRequest {
 1: required EndpointInfo.EndpointInfo endpointInfo
 2: required string project
 3: optional string dpProviderName
 4: optional bool automaticDecider
 5: optional bool update
}

service CustomerAuthToolingService {

    AdoptionStatsResponse GenerateAdoptionStats(
      1: required AdoptionStatsRequest request;
    ) throws (
      1: CustomerAuthToolingException e
    )

    CheckAdoptionStatusResponse CheckAdoptionStatus(
      1: required CheckAdoptionStatusRequest request;
    ) throws (
      1: CustomerAuthToolingException e
    )

    DraftRouteResponse DraftRoute(
      1: required DraftRouteRequest request;
    ) throws (
      1: CustomerAuthToolingException e
    )

    ApplyRouteResponse ApplyRoute(
      1: required ApplyRouteRequest request;
    ) throws (
      1: CustomerAuthToolingException e
    )

    ApplyRoutesResponse ApplyRoutes(
      1: required ApplyRoutesRequest request;
    ) throws (
      1: CustomerAuthToolingException e
    )

    GetRoutesByRouteIdsResponse GetRoutesByRouteIds(
      1: required GetRoutesByRouteIdsRequest request;
    ) throws (
      1: CustomerAuthToolingException e
    )

    GetRoutesByProjectsResponse GetRoutesByProjects(
      1: required GetRoutesByProjectsRequest request;
    ) throws (
      1: CustomerAuthToolingException e
    )

    DraftRouteFromEndpointResponse DraftRouteFromEndpoint(
      1: required DraftRouteFromEndpointRequest request;
    ) throws (
      1: CustomerAuthToolingException e
    )
}
