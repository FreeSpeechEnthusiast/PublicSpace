scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    provides = scala_artifact(
        org = "com.twitter",
        name = "oauth-models",
        repo = artifactory,
    ),
    tags = ["bazel-compatible"],
    dependencies = [
        "appsec/sanitization-lib/src/main/scala",
        "auth/oauth/thrift/src/main/thrift:thrift-scala",
        "flightauth/thrift/src/main/thrift:thrift-scala",
        "passbird/bitfields-thrift/src/main/thrift:thrift-scala",
        "passbird/thrift-only/src/main/thrift:thrift-scala",
        "src/scala/com/twitter/accounts/util:crypto",
    ],
)
