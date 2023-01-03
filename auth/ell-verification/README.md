ell-verification
==========
An example injectable App.

(run all commands from the root of Source)

# -------------------------------
# Build and Test
#
# See example tests in:
# http://go/code/auth/ell-verification/src/test/scala/com/twitter/auth/ellverification
# -------------------------------

    $ ./bazel test auth/ell-verification/src/test/scala

It is highly recommended that you favor writing [FeatureTests](http://twitter.github.io/finatra/user-guide/testing/#feature-tests)
for testing locally. These tests will start a *real* version of your server locally and allow you to 
programmatically define your testing surface. Please prefer this over attempting to run locally and 
randomly testing your code paths. In addition to being able to test more systematically (along with 
all the other benefits of writing tests), writing feature tests will build a regression test suite 
which helps to ensure that changes outside of your service code do not break your tested assumptions 
without potentially failing your tests.

Writing a simple FeatureTest will start a real version of your server locally -- this may be great 
while you are in development but current CI rules prevent tests from making network calls. Thus once 
you have settled on a test case and understood how your remote dependencies respond you should ensure 
that your test does not make any network calls by either mocking or stubbing your remote services.

Writing [FeatureTests](http://twitter.github.io/finatra/user-guide/testing/#feature-tests) are not the only way to test
your service only the type recommended to implement at a bare minimum.

For more information, please see the Finatra [testing documentation](http://twitter.github.io/finatra/user-guide/testing/).

# -------------------------------
# Running
# -------------------------------
  $ auth/ell-verification/run.sh
  

# -------------------------------
# Deploy
# -------------------------------

You will need to [request certificates](https://docbird.twitter.biz/service_authentication/howto/credentials.html#method-2-developer-certs) in order to deploy this project prior to deploying.
This only needs to be done ONCE:
    $ developer-cert-util -e devel --job ell-verification

Create the distribution package:
    $ ./pants bundle auth/ell-verification:bin --bundle-jvm-archive=zip

Upload package to [Packer](http://go/packer):
    $ packer add_version --cluster=atla passbird ellverification dist/ell-verification.zip

Schedule:
    $ aurora cron schedule atla/passbird/prod/ellverification ./auth/ell-verification/ellverification.aurora

Start (start a cron job immediately, outside of its normal cron schedule):
    $ aurora cron start atla/passbird/prod/ellverification

De-schedule:
    $ aurora cron deschedule atla/passbird/prod/ellverification

Clean up (if currently running):
    $ aurora job killall atla/passbird/prod/ellverification

# -------------------------------
# Next Steps
# -------------------------------

See [Finatra Examples - Next Steps](https://go.twitter.biz/finatra-examples-next-steps) on how to
evolve this project and get it deployed outside of your developer account.
