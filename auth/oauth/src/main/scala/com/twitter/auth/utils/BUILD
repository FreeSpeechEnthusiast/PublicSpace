scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    provides = scala_artifact(
        org = "com.twitter",
        name = "oauth-utils",
        repo = artifactory,
    ),
    tags = ["bazel-compatible"],
    dependencies = [
        "appsec/sanitization-lib/src/main/scala",
        "auth/oauth/src/main/scala/com/twitter/auth/models",
        "auth/passport-signing/src/main/scala/com/twitter/auth/passportsigning",
        "passbird/thrift-only/src/main/thrift:thrift-scala",
    ],
)
