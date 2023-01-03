{
  "role": "dal-staging",
  "name": "dal",
  "repository": "source",
  "config-files": [
    "deploy.aurora"
  ],
  "description": "The Staging DAL Thrift Server",
  "build": {
    "bazel-bundle-jvm": "adp/dal_v2/server:dal",
    "play": "true"
  },
  "targets": [
    {
      "type": "group",
      "name": "staging",
      "enforceOrdering": false,
      "description": "The staging deploy of DAL Thrift Server",
      "targets": [
        {
          "key": "smf1/dal-staging/staging/dal"
        },
        {
          "key": "atla/dal-staging/staging/dal"
        }
      ]
    }
  ]
}
