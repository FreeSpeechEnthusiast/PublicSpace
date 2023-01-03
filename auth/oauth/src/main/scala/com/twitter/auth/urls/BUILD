scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    provides = scala_artifact(
        org = "com.twitter",
        name = "oauth-urls",
        repo = artifactory,
    ),
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/commons-lang",
        "3rdparty/jvm/commons-validator",
        "appsec/sanitization-lib/src/main/scala",
        "auth/oauth/src/main/scala/com/twitter/auth/models",
        "passbird/bitfields-thrift/src/main/thrift:thrift-scala",
        "twitter-text/lib/java/src/main/java/com/twitter/twittertext",
        "util/util-logging",
    ],
)
