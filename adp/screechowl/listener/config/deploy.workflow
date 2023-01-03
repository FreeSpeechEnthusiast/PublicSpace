{
  "role": "screechowl",
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
        "role": "screechowl"
      }
    ]
  },
  "config-files": [
    "deploy.aurora"
  ],
  "targets": [
    {
      "key": "smf1/screechowl/prod/screech-owl-listener"
    },
    {
      "key": "atla/screechowl/prod/screech-owl-listener"
    },
    {
      "key": "pdxa/screechowl/prod/screech-owl-listener"
    }
  ]
}
