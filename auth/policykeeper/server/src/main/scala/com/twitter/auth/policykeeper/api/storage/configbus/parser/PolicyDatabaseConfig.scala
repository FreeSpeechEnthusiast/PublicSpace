package com.twitter.auth.policykeeper.api.storage.configbus.parser

object PolicyDatabaseConfig {
  final val ConfigBusStorageDir = "auth/policykeeper"
  final val policyFolderStructurePattern =
    "^(.*)auth/policykeeper/([a-zA-Z0-9\\-_]+)/policies.yaml$".r
}
