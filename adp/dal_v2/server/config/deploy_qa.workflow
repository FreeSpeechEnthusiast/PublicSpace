{
  "role": "dal-staging",
  "name": "dal-qa",
  "repository": "source",
  "config-files": [
    "deploy.aurora"
  ],
  "description": "The DAL Thrift Server",
  "build": {
    "bazel-bundle-jvm": "adp/dal_v2/server:dal",
    "play": "true"
  },
  "targets": [
    {
      "type": "group",
      "name": "staging",
      "enforceOrdering": false,
      "description": "The QA deploy of DAL Thrift Server",
      "targets": [
        {
          "key": "smf1/dal-staging/staging/dal-qa"
        },
        {
          "key": "atla/dal-staging/staging/dal-qa"
        }
      ]
    }
  ]
}
