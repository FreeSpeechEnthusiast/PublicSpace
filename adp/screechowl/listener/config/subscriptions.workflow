{
  "role": "screechowl",
  "name": "screech-owl-listener",
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
