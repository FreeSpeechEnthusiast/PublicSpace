{
  "role": "statebird-staging",
  "name": "statebird-watchdog",
  "repository": "source",
  "description": "The Statebird Watchdog",
  "build": {
    "play": "true",
    "steps": [
      {
        "type": "bazel-bundle",
        "target": "adp/statebird/watchdog:bundle"
      },
      {
        "type": "packer",
        "package-name": "statebird-watchdog",
        "artifact": "dist/statebird-watchdog-server-package-dist.zip",
        "role": "statebird-staging"
      }
    ]
  },
  "config-files": [
    "deploy.aurora"
  ],
  "enforceOrdering": false,
  "targets": [
    {
      "key": "smf1/statebird-staging/staging/statebird-watchdog"
    },
    {
      "key": "atla/statebird-staging/staging/statebird-watchdog"
    },
    {
      "key": "smf1/statebird-staging/staging/statebird-watchdog-gcp"
    }
  ]
}
