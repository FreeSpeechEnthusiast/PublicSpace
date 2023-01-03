{
  "role": "screechowl-staging",
  "name": "screech-owl-listener",
  "repository": "source",
  "description": "The ScreechOwl Listener",
  "build": {
    "play": "true",
    "steps": [
      {
        "type": "bazel-bundle",
        "target": "adp:screech-owl-listener"
      },
      {
        "type": "packer",
        "package-name": "screech-owl-listener",
        "artifact": "dist/screech-owl-listener.zip",
        "role": "screechowl-staging"
      }
    ]
  },
  "config-files": [
    "deploy.aurora"
  ],
  "targets": [
    {
      "key": "smf1/screechowl-staging/staging/screech-owl-listener"
    },
    {
      "key": "atla/screechowl-staging/staging/screech-owl-listener"
    }
  ]
}
