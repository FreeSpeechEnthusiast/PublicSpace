scala_library(
    sources = ["**/*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    tags = ["bazel-compatible"],
    dependencies = [
        "auth/authentication/src/main/scala/com/twitter/auth/authentication",
        "auth/authorization-scopes/src/main/scala/com/twitter/auth/policy",
        "auth/data-permissions/src/main/scala/com/twitter/auth/policy",
        "auth/enforcement/src/main/thrift:thrift-scala",
        "auth/feature-permissions/src/main/scala/com/twitter/auth/policy",
        "finagle/finagle-stats",
        "servo/util",
    ],
)
