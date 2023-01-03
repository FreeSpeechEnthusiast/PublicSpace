scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    tags = ["bazel-compatible"],
    dependencies = [
        "auth/authorization/src/main/scala/com/twitter/auth/authorization",
        "auth/enforcement/src/main/thrift:thrift-scala",
        "tfe/core/src/main/scala/com/twitter/tfe/core/routingng",
        "util/util-stats/src/main/scala",
    ],
)
