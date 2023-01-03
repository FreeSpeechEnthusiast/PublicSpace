scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    provides = scala_artifact(
        org = "com.twitter",
        name = "authorization-scopes-policy",
        repo = artifactory,
    ),
    tags = ["bazel-compatible"],
    dependencies = [
        "auth/authorization-scopes/src/main/scala/com/twitter/auth/authorizationscope",
    ],
)
