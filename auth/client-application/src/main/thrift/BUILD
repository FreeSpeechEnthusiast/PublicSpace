create_thrift_libraries(
    base_name = "thrift",
    sources = ["**/*.thrift"],
    platform = "java8",
    tags = ["bazel-compatible"],
    generate_languages = [
        "go",
        "java",
        "lua",
        "python",
        "ruby",
        "scala",
        "strato",
    ],
    provides_java_name = "client-app-flag-thrift-java",
    provides_ruby_name = "client-app-flag_thrift",
    provides_scala_name = "client-app-flag-thrift-scala",
)
