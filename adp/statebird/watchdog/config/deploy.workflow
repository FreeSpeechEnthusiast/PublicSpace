{
  "role": "statebird",
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
        "role": "statebird"
      }
    ]
  },
  "config-files": [
    "deploy.aurora"
  ],
  "targets": [
    {
      "key": "smf1/statebird/prod/statebird-watchdog"
    },
    {
      "key": "atla/statebird/prod/statebird-watchdog"
    },
    {
      "key": "smf1/statebird/prod/statebird-watchdog-gcp"
    },
    {
      "key": "pdxa/statebird/prod/statebird-watchdog"
    }
  ]
}
