create_thrift_libraries(
    base_name = "thrift",
    sources = ["**/*.thrift"],
    platform = "java8",
    tags = ["bazel-compatible"],
    dependency_roots = [
        "account_security/tsla/src/main/thrift/com/twitter/tsla/auth_events:auth-event",
        "finatra-internal/thrift/src/main/thrift",
        "src/thrift/com/twitter/bouncer:bounce-action-thrift",
        "src/thrift/com/twitter/bouncer:thrift",
    ],
    generate_languages = [
        "java",
        "scala",
    ],
    provides_java_name = "policykeeper-java",
    provides_scala_name = "policykeeper-scala",
)
