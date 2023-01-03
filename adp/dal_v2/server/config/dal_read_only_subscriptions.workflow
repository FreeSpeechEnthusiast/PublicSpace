{
  "role": "dal",
  "name": "dal_read_only",
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
