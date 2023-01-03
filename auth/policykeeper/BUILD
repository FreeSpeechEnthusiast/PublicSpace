jvm_binary(
    name = "bin",
    basename = "policykeeper",
    main = "com.twitter.auth.policykeeper.api.PolicyKeeperThriftServerMain",
    runtime_platform = "java11",
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/ch/qos/logback:logback-classic",
        "auth/policykeeper/server/src/main/scala/com/twitter/auth/policykeeper/api",
        "finagle-internal/slo/src/main/scala/com/twitter/finagle/slo",
        "finagle/finagle-zipkin-scribe/src/main/scala",
        "finatra/inject/inject-logback/src/main/scala",
        "loglens/loglens-logback/src/main/scala/com/twitter/loglens/logback",
        "twitter-server-internal/src/main/scala",
        "twitter-server/logback-classic/src/main/scala",
    ],
)

#  Aurora Workflows build phase convention requires a jvm_app named with ${project-name}-app
jvm_app(
    name = "policykeeper-app",
    archive = "zip",
    binary = ":bin",
    tags = ["bazel-compatible"],
)
