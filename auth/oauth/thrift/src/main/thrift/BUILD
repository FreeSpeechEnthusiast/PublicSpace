create_thrift_libraries(
    base_name = "thrift",
    sources = ["**/*.thrift"],
    platform = "java8",
    tags = ["bazel-compatible"],
    dependency_roots = [
        "finatra-internal/thrift/src/main/thrift",
    ],
    generate_languages = [
        "java",
        "scala",
        "strato",
    ],
    provides_java_name = "oauth2-thrift-java",
    provides_scala_name = "oauth2-thrift-scala",
)
