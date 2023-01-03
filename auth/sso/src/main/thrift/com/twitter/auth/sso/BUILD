create_thrift_libraries(
    base_name = "auth-sso",
    sources = ["sso.thrift"],
    platform = "java8",
    tags = ["bazel-compatible"],
    dependency_roots = [
        "account_security/utils/src/main/thrift",
    ],
    generate_languages = [
        "java",
        "scala",
        "strato",
    ],
    provides_java_name = "auth-sso-thrift-java",
    provides_scala_name = "auth-sso-thrift-scala",
)
