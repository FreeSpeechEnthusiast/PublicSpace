scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "oauth-utils",
        repo = artifactory,
    ),
    tags = ["bazel-compatible"],
    dependencies = [
        "auth/oauth/thrift/src/main/thrift:thrift-scala",
    ],
)
