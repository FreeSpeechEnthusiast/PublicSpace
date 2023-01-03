java_library(
    name = "encryption",
    sources = ["**/*.java"],
    platform = "java8",
    provides = artifact(
        org = "com.twitter",
        name = "auth-pasetoheaders-encryption",
        repo = artifactory,
    ),
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-annotations",
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-core",
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-databind",
        "3rdparty/jvm/com/fasterxml/jackson/datatype:jackson-datatype-jdk8",
        "3rdparty/jvm/com/fasterxml/jackson/datatype:jackson-datatype-jsr310",
        "3rdparty/jvm/com/google/guava",
        "3rdparty/jvm/dev/paseto/jpaseto:jpaseto-api",
        "3rdparty/jvm/dev/paseto/jpaseto:jpaseto-bouncy-castle",
        "3rdparty/jvm/dev/paseto/jpaseto:jpaseto-impl",
        "3rdparty/jvm/dev/paseto/jpaseto:jpaseto-jackson",
    ],
)
