{
  "role": "dal",
  "name": "dal",
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
      "key": "smf1/dal/prod/dal"
    },
    {
      "key": "atla/dal/prod/dal"
    },
    {
      "key": "pdxa/dal/prod/dal"
    }
  ]
}
