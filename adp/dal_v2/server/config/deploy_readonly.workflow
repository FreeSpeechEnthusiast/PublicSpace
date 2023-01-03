{
  "role": "dal",
  "name": "dal_read_only",
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
      "name": "prod",
      "enforceOrdering": false,
      "description": "The Read-Only deploy of DAL Thrift Server",
      "targets": [
        {
          "key": "smf1/dal/prod/dal_read_only"
        },
        {
          "key": "atla/dal/prod/dal_read_only"
        }
      ]
    }
  ]
}
