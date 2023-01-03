PolicyKeeper
=============

## Development (IDE setup)
- fastpass create --name policykeeper auth/policykeeper::
- fastpass open --intellij policykeeper

## Local Service
- Run a local service: `./auth/policykeeper/scripts/runlocal.sh`
- View the server admin page: `$ open http://0.0.0.0:31383/admin`

## Debug
- Run a local service: `./auth/policykeeper/scripts/runlocal.sh -debug`
- Connect debugger using "Remote JVM Debug" on port 5007

## Run with TFE
- Run an https terminator `./t3/build.sh && t3/run.sh`
- Run a local policykeeper service `PANTS_CONCURRENT=True ./auth/policykeeper/scripts/runlocal.sh`
- Run a tfe router `PANTS_CONCURRENT=True DTAB_ADD="/s/limiter/limiter => /$/nil;/s/limiter/limiter => /srv#/staging/local/limiter/limiter;/s/policykeeper/policykeeper => /$/inet/127.0.0.1/31908" ./tfe/tfe-router.sh`
- Sample http curl:
```
curl -k -v --resolve api.twitter.com:443:127.0.0.1 --insecure --key ~/workspace/tsa/config/certs/clientcacert.key --cert ~/workspace/tsa/config/certs/clientcacert.pem --cacert ~/workspace/tsa/config/certs/servercacert.pem \
  -H "X-Forwarded-Proto: https" \
  -H "x-decider-overrides: tfe_route:hawkeye_delete_sso_connetion_endpoint=on; tfe:tfe_enable_policy_keeper_enforcement=on; policykeeper:enable_tsla_password_protected_policy=on" \
  -H "X-B3-Flags: 1" \
  -XPOST https://api.twitter.com/1.1/sso/delete_connection
```
Do not forget to add cookie headers from the browser to above sample
Make sure endpoint, policykeeper and policy itself are enabled through decider

## Unit & Feature Tests

## Local Load Test
- `./auth/policykeeper/scripts/runlocal.sh`
- `developer-cert-util --local --job iago`
- `./auth/policykeeper/scripts/loadtest-local.sh`
- `./iago-internal/scripts/client.sh -req=start`

## Remote Load Test
- `./auth/policykeeper/scripts/loadtest.sh`
- `./iago-internal/scripts/client.sh -req=start`

## FAQ
