{
  "role": "dal-staging",
  "name": "dal-{{WF_USER}}",
  "repository": "source",
  "config-files": [
    "deploy.aurora"
  ],
  "description": "The DAL Thrift Server",
  "build": {
    "play": "true",
    "steps": [
      {
        "type": "bazel-bundle",
        "target": "adp/dal_v2/server:dal"
      },
      {
        "type": "packer",
        "package-name": "dal-{{WF_USER}}",
        "artifact": "dist/dal.zip",
        "role": "dal-staging"
      }
    ]
  },
  "targets": [
    {
      "type": "group",
      "name": "devel",
      "enforceOrdering": false,
      "description": "The devel deploy of DAL Thrift Server",
      "targets": [
        {
          "key": "smf1/dal-staging/devel/dal-{{WF_USER}}"
        },
        {
          "key": "atla/dal-staging/devel/dal-{{WF_USER}}"
        }
      ]
    }
  ]
}
