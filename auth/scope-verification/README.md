ScopeVerification
=================
Scope Verification App

An automated tooling help monitor that all the Customer Auth policies are consistent.

Scope Verification App scheduled as a cron job doing the following checks:

1. Load Route ID to NgRoute Mapping.
2. Load Route ID to Scopes Mapping.
3. Verify Route IDs.
4. Load Route ID to Annotated Data Permissions Mapping.
5. Verify Scopes.
6. Load Route Scope to Data Permission Mapping.
7. Compare Data Permissions between Route ID -> Scopes -> DPs vs. Route ID -> NgRoutes -> Annotated DPs.

# -------------------------------
# Running
# -------------------------------
  $ ./auth/scope-verification/run.sh
  

# -------------------------------
# Deploy
# -------------------------------

You will need to [request certificates](https://docbird.twitter.biz/service_authentication/howto/credentials.html#method-2-developer-certs) in order to deploy this project prior to deploying.
This only needs to be done ONCE:
    $ developer-cert-util -e devel --job scopeverification

Create the distribution package:
    $ ./pants bundle ./auth/scope-verification:bin --bundle-jvm-archive=zip

Upload package to [Packer](http://go/packer):
    $ packer add_version --cluster=atla passbird scopeverification dist/scopeverification.zip

Schedule:
    $ aurora cron schedule atla/passbird/prod/scopeverification ./auth/scope-verification/scopeverification.aurora
    
Start (start a cron job immediately, outside of its normal cron schedule):
    $ aurora cron start atla/passbird/prod/scopeverification

De-schedule:
    $ aurora cron deschedule atla/passbird/prod/scopeverification

Clean up (if currently running):
    $ aurora job killall atla/passbird/prod/scopeverification
