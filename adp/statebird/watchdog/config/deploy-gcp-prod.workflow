{
  "role": "statebird",
  "name": "statebird-watchdog",
  "config-files": [
    "deploy.aurora"
  ],
  "targets": [
    {
      "key": "smf1/statebird-staging/staging/statebird-watchdog-gcp"
    },
    {
      "key": "smf1/statebird/prod/statebird-watchdog-gcp"
    }
  ]
}
