hadoop_binary(
    name = "crane_binary",
    main = "com.twitter.twadoop.crane.CraneDriver",
    platform = "java8",
    runtime_platform = "java8",
    tags = [
        "bazel-compatible",
        "bazel-compatible:migrated",
        "bazel-only",
    ],
    dependencies = [
        "src/java/com/twitter/twadoop/batch",  # StateBird BatchDependencyValidator
        "src/java/com/twitter/twadoop/crane",
        "src/java/com/twitter/twadoop/crane/validation",
    ],
)
