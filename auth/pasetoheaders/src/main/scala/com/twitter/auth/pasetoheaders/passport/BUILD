scala_library(
    name = "passport",
    sources = ["**/*.scala"],
    platform = "java8",
    provides = artifact(
        org = "com.twitter",
        name = "auth-pasetoheaders-passport",
        repo = artifactory,
    ),
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-annotations",
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-core",
        "3rdparty/jvm/com/fasterxml/jackson/core:jackson-databind",
        "3rdparty/jvm/com/fasterxml/jackson/datatype:jackson-datatype-jdk8",
        "3rdparty/jvm/com/fasterxml/jackson/datatype:jackson-datatype-jsr310",
        "auth/pasetoheaders/src/main/java/com/twitter/auth/pasetoheaders/encryption",
        "auth/pasetoheaders/src/main/java/com/twitter/auth/pasetoheaders/models",
        "auth/pasetoheaders/src/main/resources",
        "auth/pasetoheaders/src/main/scala/com/twitter/auth/pasetoheaders/finagle",
        "auth/pasetoheaders/src/main/scala/com/twitter/auth/pasetoheaders/javahelpers",
    ],
)
