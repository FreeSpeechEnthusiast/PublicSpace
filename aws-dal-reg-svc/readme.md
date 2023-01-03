# Developing

Run the Twitter linter which includes flake8 and fmt
```
bazel lint //aws-dal-reg-svc:all
```

# Running tests
```
bazel test //aws-dal-reg-svc:all
```

# Auth setup
```
grep "AdministratorAccess-482194395845" ~/.aws/config || aws-sso-profile 482194395845 >> ~/.aws/config
env AWS_PROFILE=AdministratorAccess-482194395845 aws-sso-authenticate
```

# Running the server locally
```
docker run -it --rm -p 8000:8000 amazon/dynamodb-local:latest
env AWS_PROFILE=AdministratorAccess-482194395845 exec-with-sso-login-creds bazel run //aws-dal-reg-svc:server -- --disable-access-simulation --disable-resource-filter --dry-run --load-env-creds --log_to_stderr=google:INFO --local-dynamodb-create --local-dynamodb-endpoint='http://localhost:8000'
```

# Running the dataset observer locally
```
env AWS_PROFILE=AdministratorAccess-482194395845 exec-with-sso-login-creds bazel run //aws-dal-reg-svc:dataset_observer -- --load-env-creds --local-dynamodb-create --local-dynamodb-endpoint='http://localhost:8000' --log_to_stderr=google:INFO
```

# Running the account status script locally
```
env AWS_PROFILE=AdministratorAccess-482194395845 exec-with-sso-login-creds bazel run //aws-dal-reg-svc:account_status -- --load-env-creds
```

# Deploying registration server to staging
```
bazel bundle //aws-dal-reg-svc:server
packer add_version --cluster=smf1 aws-dal-reg-svc-staging reg-svc-staging dist/server.pex
aurora job create smf1/aws-dal-reg-svc-staging/staging/aws-dal-reg-svc aws-dal-reg-svc/server.aurora
```

# Killing a staging registration server job
```
aurora job killall smf1/aws-dal-reg-svc-staging/staging/aws-dal-reg-svc
```

# Deploying registration server to production
```
bazel bundle //aws-dal-reg-svc:server
packer add_version --cluster=smf1 aws-dal-registration-svc reg-svc-prod dist/server.pex
aurora cron schedule smf1/aws-dal-registration-svc/prod/aws-dal-reg-svc aws-dal-reg-svc/server.aurora
aurora cron start smf1/aws-dal-registration-svc/prod/aws-dal-reg-svc
```

# Killing a production registration server job
```
aurora job killall smf1/aws-dal-registration-svc/prod/aws-dal-reg-svc
aurora cron deschedule smf1/aws-dal-registration-svc/prod/aws-dal-reg-svc
```

# List Prod DAL datasets
```
bazel bundle //aws-dal-reg-svc:dal_list
packer add_version --cluster=smf1 aws-dal-registration-svc dal-list dist/dal_list.pex
aurora job create smf1/aws-dal-registration-svc/prod/aws-dal-list aws-dal-reg-svc/dal_list.aurora
```

# Delete Prod DAL/Kite datasets
The parameters to configure how/what datasets are deleted are defined in `dal_delete_datasets.aurora`.
```
bazel bundle //aws-dal-reg-svc:dal_delete_datasets
packer add_version --cluster=smf1 aws-dal-registration-svc dal-delete-datasets dist/dal_delete_datasets.pex
aurora cron schedule smf1/aws-dal-registration-svc/prod/aws-dal-delete-datasets aws-dal-reg-svc/dal_delete_datasets.aurora
aurora cron start smf1/aws-dal-registration-svc/prod/aws-dal-delete-datasets
```

# Killing a production delete prod dal/kite datasets
```
aurora job killall smf1/aws-dal-registration-svc/prod/aws-dal-delete-datasets
aurora cron deschedule smf1/aws-dal-registration-svc/prod/aws-dal-delete-datasets
```

# Deploy dataset observer to production
```
bazel bundle //aws-dal-reg-svc:dataset_observer
packer add_version --cluster=smf1 aws-dal-registration-svc aws-dataset-observer dist/dataset_observer.pex
aurora cron schedule smf1/aws-dal-registration-svc/prod/aws-dataset-observer aws-dal-reg-svc/dataset_observer.aurora
aurora cron start smf1/aws-dal-registration-svc/prod/aws-dataset-observer
```

# Killing a production dataset observer job
```
aurora job killall smf1/aws-dal-registration-svc/prod/aws-dataset-observer
aurora cron deschedule smf1/aws-dal-registration-svc/prod/aws-dataset-observer
```

