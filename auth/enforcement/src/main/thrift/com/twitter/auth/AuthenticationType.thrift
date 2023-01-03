namespace java com.twitter.auth.thrift.authenticationtype
namespace py gen.twitter.auth.AuthenticationType
namespace rb AuthenticationType
namespace go AuthenticationType
#@namespace scala com.twitter.auth.authenticationtype.thriftscala
#@namespace strato com.twitter.auth.authenticationtype

/*
 ************************************************************************************************
 * IF YOU ARE ADDING NEW AUTH TYPE IN THIS FILE, PLEASE REACH OUT TO CORE METRICS TEAM AND ASKED
 * THEM TO UPDATE UserAuditsTfe JOB IN mAU/DAU PIPELINE
 ************************************************************************************************
 */

enum AuthenticationType {
/* NOTE:
  - from here and up till #12 below, the contents are copied verbatim from tfe_log.thrift
    where an AuthType is already defined
  - we are copying the first 12 lines in order to preserve them, and allow for easy conversion
 */
      UNKNOWN = 1,
      OAUTH_1 = 2,
      SESSION = 3,
      OAUTH_2 = 4,
      OAUTH_2_APP_ONLY = 5,
      OAUTH_2_GUEST_AUTH = 6,
      TIA = 7,
      OAUTH_1_TWO_LEGGED = 8,
      OAUTH_1_XAUTH = 9,
      DEVICE_AUTH = 10,
      OAUTH_1_REQUEST_TOKEN = 11,
      OAUTH_2_SESSION_AUTH = 12,
      /* add only after here */
      /* --- */
      /*
      the next one is a special case for Strato / GraphQL scenario, where TFE will NOT
      do Authentication Method verification, and will allow a "trusted backend" to do it
       */
      TRUSTED_BACKEND = 13,
      /*
      the next one is a special meta type that will be "expanded" into ALL OAuth types
      it is provided to the users of the NgRoute configs in order to simplify method entry
      NOTE that it is only available during serialization/deserialization, and actual processing
      is always done using the fine-grained permissions, rather than this meta field
       */
      ANY_OAUTH = 14,
      /*
      the next one allows the end-point specify that no authentication of any kind at all
      this allows the end-point to remain open, and will also allow us to do enforcement
      when end-points require Session or NoAuth, but instead are hit with OAuth. Requests without
      auth headers or session requests with no web token will be marked as no authentication type.
      NgRoutes with no authentication annotated only allows requests with no authentication as the
      auth type. For example, OAuth request hitting no authentication route will be bounced.
      */
      NO_AUTHENTICATION = 15,
      /*
      this authentication type is only used for email lite login use case.
      */
      RESTRICTED_SESSION = 16,
      /*
      this authentication type is only used for email lite login use case.
      */
      RESTRICTED_OAUTH2_SESSION = 17,
      /*
      this authentication type is only used for interactive (AMP) emails use case.
      */
      OAUTH_2_AMP_EMAIL = 18,
      /*
      this authentication type is only used for client credential use case.
      */
      OAUTH_2_CLIENT_CREDENTIAL = 19
}
