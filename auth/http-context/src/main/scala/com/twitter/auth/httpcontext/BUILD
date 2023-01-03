scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    provides = scala_artifact(
        org = "com.twitter",
        name = "auth-http-context",
        repo = artifactory,
    ),
    tags = ["bazel-compatible"],
    dependencies = [
        "auth/enforcement/src/main/scala/com/twitter/auth/context:context-domain",
        "auth/http-context/src/main/resources",
        "finagle/finagle-base-http/src/main",
        "platform-security/common/src/main/scala/com/twitter/psec/common/context",
    ],
)
