scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    tags = ["bazel-compatible"],
    dependencies = [
        "auth/sso/src/main/scala/com/twitter/auth/sso/client",
        "auth/sso/src/main/scala/com/twitter/auth/sso/service",
        "auth/sso/src/main/scala/com/twitter/auth/sso/signature",
        "strato/config/columns/auth/sso:sso-strato-client",
        "strato/src/main/scala/com/twitter/strato/client",
        "util/util-jackson/src/main/scala/com/twitter/util/jackson",
    ],
)
