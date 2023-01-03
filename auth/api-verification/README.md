ApiVerification
====================
Api Verification App

An automated tooling help monitor API status.

Api Verification App scheduled as a cron job doing the following checks:

1. OAuth2 Auth Code PKCE Flow.
2. OAuth2 Refresh Token Flow.
3. OAuth2 Revoke Token Flow.
4. v2 APIs.

# -------------------------------
# Running
# -------------------------------
  $ ./auth/api-verification/run.sh
  

# -------------------------------
# Deploy
# -------------------------------

You will need to [request certificates](https://docbird.twitter.biz/service_authentication/howto/credentials.html#method-2-developer-certs) in order to deploy this project prior to deploying.
This only needs to be done ONCE:
    $ developer-cert-util -e devel --job apiverification

Create the distribution package:
    $ ./pants bundle ./auth/api-verification:bin --bundle-jvm-archive=zip

Upload package to [Packer](http://go/packer):
    $ packer add_version --cluster=atla passbird apiverification dist/apiverification.zip

Schedule:
    $ aurora cron schedule atla/passbird/prod/apiverification ./auth/api-verification/apiverification.aurora
    
Start (start a cron job immediately, outside of its normal cron schedule):
    $ aurora cron start atla/passbird/prod/apiverification

De-schedule:
    $ aurora cron deschedule atla/passbird/prod/apiverification

Clean up (if currently running):
    $ aurora job killall atla/passbird/prod/apiverification
