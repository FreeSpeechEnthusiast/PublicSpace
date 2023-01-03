AUTH_CONTEXT_DOMAIN_SOURCES = [
    "AuthPasetoContext.scala",
    "AuthPasetoContextKey.scala",
]

scala_library(
    sources = ["*.scala"] + exclude_globs(AUTH_CONTEXT_DOMAIN_SOURCES),
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    tags = ["bazel-compatible"],
    dependencies = [
        ":context-domain",
        "finagle/finagle-core/src/main",
        "servo/util",
        "servo/util/src/main/scala",
        "util/util-stats/src/main/scala",
    ],
    exports = [
        ":context-domain",
    ],
)

scala_library(
    name = "context-domain",
    sources = AUTH_CONTEXT_DOMAIN_SOURCES,
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    provides = scala_artifact(
        org = "com.twitter",
        name = "auth-context-domain",
        repo = artifactory,
    ),
    tags = ["bazel-compatible"],
    dependencies = [
        "finagle/finagle-core/src/main",
        "finagle/finagle-http/src/main/scala",
        "finagle/finagle-thrift/src/main/scala",
        "scrooge/scrooge-serializer/src/main/scala",
        "servo/util/src/main/scala:exception",
        "util/util-slf4j-api/src/main/scala/com/twitter/util/logging",
        "util/util-stats/src/main/scala",
    ],
)
