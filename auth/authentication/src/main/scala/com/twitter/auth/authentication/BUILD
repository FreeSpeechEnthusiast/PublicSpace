scala_library(
    sources = ["**/*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    provides = scala_artifact(
        org = "com.twitter",
        name = "auth-authentication",
        repo = artifactory,
    ),
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/com/google/guava",
        "3rdparty/jvm/com/twitter:joauth",
        "3rdparty/jvm/org/apache/httpcomponents:httpclient",
        "auth/enforcement/src/main/thrift:thrift-scala",
        "auth/oauth/src/main/scala/com/twitter/auth/models",
        "auth/oauth/src/main/scala/com/twitter/auth/utils",
        "auth/passport-signing/src/main/scala/com/twitter/auth/passportsigning",
        "finagle-internal/mtls/src/main/scala/com/twitter/finagle/mtls/authentication",
        "finagle/finagle-stats",
        "finatra-internal/tfe",
        "src/scala/com/twitter/common/ip_address_utils",
    ],
)
