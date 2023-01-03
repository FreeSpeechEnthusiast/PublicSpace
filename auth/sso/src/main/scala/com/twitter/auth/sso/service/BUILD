scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    tags = ["bazel-compatible"],
    dependencies = [
        "auth/sso/src/main/scala/com/twitter/auth/sso/client",
        "auth/sso/src/main/scala/com/twitter/auth/sso/models",
        "auth/sso/src/main/scala/com/twitter/auth/sso/signature",
        "auth/sso/src/main/scala/com/twitter/auth/sso/store",
        "auth/sso/src/main/scala/com/twitter/auth/sso/util",
        "stitch/stitch-core",
    ],
)
