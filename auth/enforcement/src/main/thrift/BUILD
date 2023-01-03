create_thrift_libraries(
    base_name = "thrift",
    sources = ["**/*.thrift"],
    platform = "java8",
    tags = ["bazel-compatible"],
    dependency_roots = [
        "auth/tamper-proofing/src/main/thrift",
    ],
    generate_languages = [
        "go",
        "java",
        "lua",
        "python",
        "ruby",
        "scala",
        "strato",
    ],
    provides_java_name = "enforcement-thrift-java",
    provides_ruby_name = "enforcement_thrift",
    provides_scala_name = "enforcement-thrift-scala",
)
