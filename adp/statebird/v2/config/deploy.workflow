{
  "role": "statebird",
  "name": "statebird-v2",
  "repository": "source",
  "config-files": [
    "deploy.aurora"
  ],
  "description": "The Statebird Thrift Service",
  "build": {
    "play": "true",
    "steps": [
      {
        "type": "bazel-bundle",
        "target": "adp/statebird/v2:bundle"
      },
      {
        "type": "packer",
        "package-name": "statebird-v2",
        "artifact": "dist/statebird-v2-server-package-dist.zip",
        "role": "statebird"
      }
    ]
  },
  "targets": [
    {
      "key": "smf1/statebird/prod/statebird-v2"
    },
    {
      "key": "atla/statebird/prod/statebird-v2"
    },
    {
      "key": "pdxa/statebird/prod/statebird-v2"
    }
  ]
}
