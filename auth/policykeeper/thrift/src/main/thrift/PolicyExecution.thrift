namespace java com.twitter.auth.policykeeper.thriftjava
#@namespace scala com.twitter.auth.policykeeper.thriftscala
namespace rb policykeeper

include "Bouncer.thrift"

enum Code {
    TRUE = 0,
    FALSE = 1,
    FAILED = 2,     //a dataprovider execution failed
    TIMEOUT = 3,    //a dataprovider is timed out
    IMPOSSIBLE = 4, //rule syntax error or unknown parameters
    NOINPUT = 5,    //missing or incomplete input
    BADINPUT = 6,   //bad input type or cast exception
    NORESULTS = 7,  //no applicable policies
    MIXED = 8,      //policies returned different results
}

struct Result {
    1: required Code policy_execution_code
    // finatra api error code, see https://sourcegraph.twitter.biz/git.twitter.biz/source/-/blob/finatra-internal/api11/src/main/scala/com/twitter/finatra/api11/ApiError.scala
    2: optional i32 api_error_code
    // as option bouncer can be used if api_error_code is not set
    3: optional Bouncer.BouncerRequest bouncer_request
}


