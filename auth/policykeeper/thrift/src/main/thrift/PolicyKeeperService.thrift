namespace java com.twitter.auth.policykeeper.thriftjava
#@namespace scala com.twitter.auth.policykeeper.thriftscala
namespace rb policykeeper

include "Policy.thrift"
include "PolicyExecution.thrift"
include "RouteInformation.thrift"
include "AuthMetadata.thrift"

enum PolicyKeeperErrorCode {
	DEPENDENCY_FAILURE = 1,
	STORAGE_FAILURE = 2,
	SERVICE_FAILURE = 3,
	INVALID_INPUT = 4
}

exception PolicyKeeperServiceException {
	1: required PolicyKeeperErrorCode errorCode,
	2: optional string message
}

//endpoint for inline calls
struct VerifyPoliciesRequest {
 1: required set<string> policy_ids
 2: optional map<string, string> custom_input
 3: optional AuthMetadata.AuthMetadata auth_metadata
}

struct VerifyPoliciesResponse {
 1: required PolicyExecution.Result execution_result // merged results
}

//endpoint for filter calls
struct VerifyRoutePoliciesRequest {
 1: RouteInformation.RouteInformation route_information
 2: optional map<string, string> custom_input
 3: optional AuthMetadata.AuthMetadata auth_metadata
}

struct VerifyRoutePoliciesResponse {
 1: required PolicyExecution.Result execution_result // merged results
}

service PolicyKeeperService {
    VerifyPoliciesResponse VerifyPolicies(
      1: required VerifyPoliciesRequest request;
    ) throws (
        1: PolicyKeeperServiceException e
    )

    VerifyRoutePoliciesResponse VerifyRoutePolicies(
       1: required VerifyRoutePoliciesRequest request;
    ) throws (
       1: PolicyKeeperServiceException e
    )
}
