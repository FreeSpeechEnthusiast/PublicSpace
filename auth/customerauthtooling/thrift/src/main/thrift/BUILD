create_thrift_libraries(
    base_name = "thrift",
    sources = ["**/*.thrift"],
    tags = ["bazel-compatible"],
    dependency_roots = [
        "auth/enforcement/src/main/thrift",
        "finatra-internal/thrift/src/main/thrift",
    ],
    generate_languages = ["scala"],
    provides_scala_name = "customerauthtooling-scala",
)
