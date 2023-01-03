scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    provides = scala_artifact(
        org = "com.twitter",
        name = "auth-common",
        repo = artifactory,
    ),
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-core",
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-databind",
        "3rdparty/jvm/com/fasterxml/jackson/module:jackson-module-scala",
        "3rdparty/jvm/com/yubico:u2flib-server-attestation",
        "3rdparty/jvm/net/liftweb:lift-json",
        "appsec/crypto-lib/src/main/scala",
        "auth/enforcement/src/main/thrift:thrift-scala",
        "configbus/subscriber",
        "finagle/finagle-core/src/main",
        "util/util-logging",
        "util/util-stats/src/main/scala",
    ],
)
