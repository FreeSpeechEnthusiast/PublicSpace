scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    provides = scala_artifact(
        org = "com.twitter",
        name = "passport-signing",
        repo = artifactory,
    ),
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/commons-codec",
        "appsec/crypto-lib",
        "auth/enforcement/src/main/thrift:thrift-scala",
        "decider",
        "finagle/finagle-core/src/main",
        "util/util-stats/src/main/scala",
    ],
)
