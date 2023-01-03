jvm_binary(
    name = "role_migration-bin",
    basename = "role_migration-cli",
    main = "com.twitter.twadoop.batch.role_migration.RoleMigrationAssistant",
    runtime_platform = "java11",
    dependencies = [
        ":log_resources",
        "src/scala/com/twitter/twadoop/batch/role_migration",
        "twitter-server/slf4j-jdk14/src/main/scala",
    ],
)

resources(
    name = "log_resources",
    sources = ["log4j.properties"],
    tags = ["bazel-compatible"],
)
