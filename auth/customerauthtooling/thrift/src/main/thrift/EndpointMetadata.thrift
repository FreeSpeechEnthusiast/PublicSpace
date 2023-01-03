namespace java com.twitter.auth.customerauthtooling.thriftjava
#@namespace scala com.twitter.auth.customerauthtooling.thriftscala
namespace rb CustomerAuthTooling

struct EndpointMetadata {
  1: optional set<i64> suppliedDps
  2: optional bool foundInTfeOverride
  3: optional bool isInternalEndpointOverride
  4: optional bool isNgRouteOverride
  5: optional bool requiresAuthOverride
  6: optional bool isAppOnlyOrGuestOverride
  7: optional bool oauth1OrSessionOverride
  8: optional bool alreadyAdoptedDpsOverride
}
