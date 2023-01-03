{
  "role": "screechowl",
  "name": "screech-owl-service",
  "subscriptions": [
    {
      "type": "SLACK",
      "recipients": [
        {
          "to": "team-adp"
        }
      ],
      "events": [
        "*FAILED"
      ],
      "filter": {
        "ids": [
          "*/*/prod/*"
        ],
        "types": [
          "AURORA"
        ]
      }
    },
    {
      "type": "SLACK",
      "recipients": [
        {
          "to": "adp-notifications"
        }
      ],
      "events": [
        "TARGET*"
      ]
    }
  ]
}
