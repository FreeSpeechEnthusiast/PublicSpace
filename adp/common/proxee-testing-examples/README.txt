Wily Names for ADP services
---------------------------

DAL:
- /s/dal/dal
- /cluster/local/dal-staging/staging/dal

Statebird:
- /s/statebird/statebird-v2
- /cluster/local/statebird-staging/staging/statebird-v2

DAL Read-Only:
- s/dal/dal_read_only
- /cluster/local/dal-staging/staging/dal_read_only


Running Proxee and Statebird, DAL tests on a laptop, with NO S2S Auth
---------------------------------------------------------------------

- Run Proxee in one terminal:
  adp/common/proxee-testing-examples/start_proxee_on_laptop.sh

  This script script uses a `proxee_config.yml` at
  adp/common/proxee-testing-examples/proxee_config.yaml

- With Proxee running, start another terminal and run the Statebird confidence check.
  This connects to a service location of `localhost:9991` as it's default, matching the 
  proxee configuration in proxee_config.yaml

  ./bazel run tests/scala/com/twitter/statebird/server/v2/confidence_check:confidence-check-bin -- -serviceLocation=localhost:9991 -opportunisticTls=false


- With Proxee running, start another terminal and run the DAL confidence check.
  This connects to a service location of `localhost:9993` as it's default, matching the 
  proxee configuration in proxee_config.yaml

  ./bazel run src/scala/com/twitter/dal/client/test/confidence_check:confidence_check_bin -- -serviceLocation=localhost:9993  -opportunisticTls=false


Running Proxee and Statebird, DAL tests on a laptop, with S2S Auth Enabled
--------------------------------------------------------------------------

- First, you'll need to create certs on your laptop, using the `developer-cert-util` tool that's part of MDE.
  __YOU ONLY NEED TO CREATE THE CERTS IN THIS NEXT STEP ONCE.__

  # create a cert for use by the proxee app when talking to DAL, Statebird services in mesos:
  developer-cert-util --local --job proxee-testing-example

  # create a cert for use by the DAL confidence check app
  developer-cert-util --local --job e2e-job-dal-v2-confidence-check

  # create a cert for use by the Statebird confidence check app
  developer-cert-util --local --job e2e-job-statebird-confidence-check

- Re-run the Statebird and DAL confidence checks, with arguments adjusted to specify the serviceIdentifier, and to use the S2S Auth enabled ports:

  ./bazel run tests/scala/com/twitter/statebird/server/v2/confidence_check:confidence-check-bin -- -serviceIdentifier.environment=devel -serviceIdentifier.role=jboyd -serviceIdentifier.cluster=local -serviceLocation=localhost:9995
  ./bazel run src/scala/com/twitter/dal/client/test/confidence_check:confidence_check_bin -- -serviceIdentifier.environment=devel -serviceIdentifier.role=jboyd -serviceIdentifier.cluster=local -serviceLocation=localhost:9997


Run Proxee on Nest
------------------
- Bundle Proxee and configs specific to nest use:
  ./bazel bundle adp/common/proxee-testing-examples:proxee-test-examples-nest-bundle --bundle-jvm-archive=tgz
- Copy file dist/proxee-testing-examples-dist.tar.gz to a nest
  scp dist/proxee-testing-examples-dist.tar.gz hadoopnest3.atla.twitter.com:.
- SSH to machine:
  ssh hadoopnest3.atla.twitter.com  
- Extract tarball and start proxee:
  mkdir proxee-testing-examples-dist
  cd proxee-testing-examples-dist
  tar xzvf ../proxee-testing-examples-dist.tar.gz
  ./start_proxee_on_nest.sh
- Verify proxee is running with the '--service-path' option to dalv2 command line
  dalv2 segment lookup --service-path localhost:9993   --url gcs:///logs/client_event/2019/04/29/13 --location-type gcs --location-name gcs


Other Notes
-----------

- You can build proxee on its own with the below target, but it's more useful to create your own target
  that can include config files.

  ./bazel bundle csl/proxee/app:bundle --bundle-jvm-archive=zip

