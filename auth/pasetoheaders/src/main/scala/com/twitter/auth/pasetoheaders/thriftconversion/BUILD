scala_library(
    name = "thriftconversion",
    sources = ["**/*.scala"],
    provides = artifact(
        org = "com.twitter",
        name = "auth-pasetoheaders-thriftconversion",
        repo = artifactory,
    ),
    tags = ["bazel-compatible"],
    dependencies = [
        "auth/enforcement/src/main/thrift:thrift-scala",
        "auth/pasetoheaders/src/main/java/com/twitter/auth/pasetoheaders/models",
        "auth/pasetoheaders/src/main/scala/com/twitter/auth/pasetoheaders/javahelpers",
    ],
)
