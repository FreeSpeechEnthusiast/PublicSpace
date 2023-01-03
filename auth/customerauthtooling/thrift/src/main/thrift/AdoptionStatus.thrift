namespace java com.twitter.auth.customerauthtooling.thriftjava
#@namespace scala com.twitter.auth.customerauthtooling.thriftscala
namespace rb CustomerAuthTooling

struct AdoptionStatus {
 0: required AdoptionRequirement requirement
 1: optional bool foundInTfe
 2: optional bool isInternalEndpoint
 3: optional bool isNgRoute
 4: optional bool requiresAuth
 5: optional bool isAppOnlyOrGuest
 6: optional bool oauth1OrSession
 7: optional bool alreadyAdoptedDps
}

enum AdoptionRequirement {
    RequiredCustomerAuthAndNgRoutesAdoption = 0,
    RequiredNgRoutesAdoptionOnly = 1,
    Required = 2,
    NotRequired = 3,
    UnableToDetermine = 4
}
