{
  "role": "screechowl-staging",
  "name": "screech-owl-service",
  "repository": "source",
  "description": "The ScreechOwl Service",
  "build": {
    "play": "true",
    "steps": [
      {
        "type": "bazel-bundle",
        "target": "adp/screechowl/service:bundle"
      },
      {
        "type": "packer",
        "package-name": "screech-owl-service",
        "artifact": "dist/screech-owl-service.zip",
        "role": "screechowl-staging"
      }
    ]
  },
  "config-files": [
    "deploy.aurora"
  ],
  "targets": [
    {
      "key": "smf1/screechowl-staging/staging/screech-owl-service"
    },
    {
      "key": "atla/screechowl-staging/staging/screech-owl-service"
    }
  ]
}
