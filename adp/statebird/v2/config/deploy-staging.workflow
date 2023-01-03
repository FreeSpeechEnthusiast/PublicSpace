{
  "role": "statebird-staging",
  "name": "statebird-v2",
  "enforceOrdering": false,
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
        "role": "statebird-staging"
      }
    ]
  },
  "targets": [
    {
      "key": "smf1/statebird-staging/staging/statebird-v2"
    },
    {
      "key": "atla/statebird-staging/staging/statebird-v2"
    }
  ]
}
