SVC_DOMAIN = "aws-dal-reg-svc"
SVC_NAME = "AWS-DAL-Registration-Service"
SVC_ROLE = {
  "prod": "aws-dal-registration-svc",
  "staging": "aws-dal-reg-svc-staging",
}

AWS_CLIENT_CONNECT_TIMEOUT = 10  # seconds
AWS_CLIENT_READ_TIMEOUT = 120
AWS_CLIENT_RETRY_CONF = {
  "max_attempts": 3,
  "mode": "adaptive",
}

AWS_CLOUD_WATCH_REGION = "us-west-1"
AWS_CLOUD_WATCH_ACCOUNT_ID = "482194395845"

# disable "opt-in" AWS regions to prevent access error log entries like the following:
# `An error occurred (UnrecognizedClientException) when calling the ListStreams operation: The security token included in the request is invalid`
# https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/using-regions-availability-zones.html
AWS_EXCLUDED_REGIONS = [
  "af-south-1",  # Africa (Cape Town)
  "ap-east-1",  # Asia Pacific (Hong Kong)
  "eu-south-1",  # Europe (Milan)
  "me-south-1",  # Middle East (Bahrain)
]

AWS_ORGANIZATION_ACCOUNT_ID = "171959851929"
AWS_ORGANIZATION_ACCOUNT_KITE_TAG = "kite_project"

DAL_CLIENT_NAME = "dal_client"
DAL_RATE_LIMIT = 1  # seconds
DAL_SERVICE_NAME = {
  "prod": "/cluster/local/dal/prod/dal",
  "staging": "/cluster/local/dal-staging/staging/dal",
}

DEFAULT_OWNER_TEAM = "UNKNOWN"

DYNAMO_ACCESS_CACHE_TABLE_NAME = "twttr-pdp-access-cache"
DYNAMO_ACCESS_CACHE_ACCOUNT_TTL = {
  "084876669870": 604800,  # seconds
  "673964658973": 604800,
}
DYNAMO_DEFAULT_ACCESS_CACHE_TTL = 86400  # seconds
DYNAMO_METASTORE_TABLE_NAME = "twttr-pdp-datasets"
DYNAMO_STAGES_TABLE_ACCOUNT_ID = "482194395845"
DYNAMO_STAGES_TABLE_NAME = "twttr-pdp-reg-stages"
DYNAMO_STAGES_TABLE_TTL = 1209600  # seconds
DYNAMO_SVC_TABLE_REGION = "us-west-1"

# unit: seconds
FUTURE_TIMEOUTS = {
  "scanner": 1800,  # resource/IAM entity iteration future groups
  "processor": 7200,  # batched access simulation/registration future groups
  "main": 14400,
}

GLOBAL_API_REGION = "us-west-2"  # use when working with global APIs like IAM or orgs.

IAM_ACCESS_SIML_RATE_LIMIT = 15  # seconds
IAM_API_DELAYED_RETRIES = 5
IAM_API_RETRY_DELAY_INTERVAL = 120  # seconds
IAM_MAX_API_FUTURES = 6  # per-iam entity/access scan, per-account

# The SimulatePrincipalPolicy API method only supports
# inputs less than 1000 vs the length of the product of ActionNames and
# ResourceArns. These API calls are executed serially by resource to
# limit the number of actions input. This value will need to be reduced if
# a resource is added to RESOURCE_ACCESS_POLICY_ACTIONS where the sum of
# the defined actions is larger than 10.
IAM_RSRC_ACCESS_SIML_BATCH_SIZE = 50

INIT_TS_FIELD_PFX = "init_"

KITE_PROJECT_NAME_PATTERN = r"^([a-z0-9_-]+)$"

PDP_ROLE_ARN_PATTERN = "arn:aws:iam::{}:role/iam-role-pdp-dal-reg-svc-stackset"

SERVER_MONITOR_INTVL = 120  # seconds
STAGE_PROGRESS_TIMEOUTS = {
  "default": 1200,
  "registration": 7200,
}

SUPPORTED_IAM_ENTITIES = [
  "group",
  "role",
  "user",
]

TSS_PATH = "/".join(["/var/lib/tss/keys", SVC_DOMAIN])

REGISTRATION_EXCLUSION_MAX_INTERVAL = 86400  # 24 hours
REGISTRATION_EXCLUSION_MIN_INTERVAL = 43200  # 12 hours

RESOURCE_ACCESS_POLICY_ACTIONS = {
  "dax": {
    "read": [
      "dax:BatchGetItem",
      "dax:ConditionCheckItem",
      "dax:GetItem",
      "dax:Query",
      "dax:Scan",
    ],
    "write": [
      "dax:BatchWriteItem",
      "dax:DeleteItem",
      "dax:PutItem",
      "dax:UpdateItem",
    ],
  },
  "dynamodb": {
    "read": [
      "dynamodb:BatchGetItem",
      "dynamodb:GetItem",
      "dynamodb:GetRecords",
      "dynamodb:Query",
      "dynamodb:Scan",
    ],
    "write": [
      "dynamodb:BatchWriteItem",
      "dynamodb:DeleteItem",
      "dynamodb:PutItem",
      "dynamodb:UpdateItem",
      "dynamodb:UpdateTimeToLive",
    ],
  },
  "kinesis": {
    "read": [
      "kinesis:GetRecords",
      "kinesis:SubscribeToShard",
    ],
    "write": [
      "kinesis:PutRecord",
      "kinesis:PutRecords",
    ],
  },
  "s3": {
    "read": [
      "s3:GetObject",
      "s3:GetObjectTorrent",
      "s3:GetObjectVersion",
      "s3:GetObjectVersionTorrent",
      "s3:ListBucket",
      "s3:ListBucketVersions",
    ],
    "write": [
      "s3:DeleteObject",
      "s3:DeleteObjectVersion",
      "s3:PutObject",
      "s3:RestoreObject",
    ],
  },
  "sqs": {
    "read": [
      "sqs:ReceiveMessage",
    ],
    "write": [
      "sqs:PurgeQueue",
      "sqs:SendMessage",
      # temp disable simulating `SendMessageBatch` access due to CAT-4465
      # 'sqs:SendMessageBatch',
      "sqs:SetQueueAttributes",
    ],
  },
  # TODO: add support for elasticache backups
  "elasticache": {},
}

RESOURCE_ATTRIBUTION_FILTERS = {
  "673964658973": {
    "default": "PSCP",
    "CLOUDACCEL": {
      "include": [".*harbinger.*", ".*registrar.*"],
    },
    "FLEETS": {
      "include": [".*fleets.*"],
    },
    "PSVC": {
      "include": [
        ".*broadcast.*",
        ".*channels.*",
        ".*chat(db|man|-quality).*",
        ".*social-service.*",
        ".*vip-.*",
      ],
      "exclude": [".*vidman.*"],
    },
    "PVID": {
      "include": [".*hydra.*", ".*vidman.*"],
      "exclude": [".*registrar.*"],
    },
  },
}

RESOURCE_SCANNER_REQUIRED_ATTRS = [
  "account_id",
  "authenticator",
  "entities_key",
  "scan_method",
  "service",
]

RESOURCE_STATUSES = {
  "dax": ["ACTIVE"],
  "dynamodb": [
    "CREATING",
    "UPDATING",
    "DELETING",
    "ACTIVE",
    "INACCESSIBLE_ENCRYPTION_CREDENTIALS",
    "ARCHIVING",
    "ARCHIVED",
  ],
  "elasticache": [
    "available",
    "creating",
    "deleted",
    "deleting",
    "incompatible-network",
    "modifying",
    "rebooting cluster nodes",
    "restore-failed",
    "snapshotting",
  ],
  "kinesis": ["CREATING", "DELETING", "ACTIVE", "UPDATING"],
  "s3": ["ACTIVE"],
  "sqs": ["ACTIVE"],
}
