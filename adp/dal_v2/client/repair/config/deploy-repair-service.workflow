{
  "role": "dal",
  "name": "dal-repair-service",
  "config-files": [
    "deploy-repair-service.aurora"
  ],
  "repository": "source",
  "description": "The DAL repair service, which monitors HDFS and does some repair of DAL datasets.",
  "build": {
    "play": "true",
    "steps": [
      {
        "type": "bazel-bundle",
        "target": "adp/dal_v2/client/repair:bundle"
      },
      {
        "type": "packer",
        "package-name": "dal-repair-service",
        "artifact": "dist/dal-repair-service-dist.zip",
        "role": "dal"
      }
    ]
  },
  "targets": [
    {
      "key": "smf1/dal/prod/dal-repair-service-dw2-smf1"
    },
    {
      "key": "smf1/dal/prod/dal-repair-service-dwrev-smf1"
    },
    {
      "key": "atla/dal/prod/dal-repair-service-cold-atla"
    },
    {
      "key": "atla/dal/prod/dal-repair-service-proc-atla"
    },
    {
      "key": "atla/dal/prod/dal-repair-service-proc2-atla"
    },
    {
      "key": "atla/dal/prod/dal-repair-service-proc3-atla"
    },
    {
      "key": "atla/dal/prod/dal-repair-service-procpi-atla"
    },
    {
      "key": "atla/dal/prod/dal-repair-service-procrev-atla"
    }
  ]
}
