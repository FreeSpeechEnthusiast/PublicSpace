scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    provides = scala_artifact(
        org = "com.twitter",
        name = "tamper-proofing",
        repo = artifactory,
    ),
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/net/aholbrook/paseto:meta",
        "auth/common/src/main/scala/com/twitter/auth/common",
        "auth/tamper-proofing/src/main/thrift:thrift-scala",
        "util/util-stats/src/main/scala",
    ],
)
