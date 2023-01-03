jvm_binary(
    name = "batch-wrapper-test-bin",
    basename = "statebird-v2-batch-wrapper-test-bin",
    main = "com.twitter.twadoop.batch.BatchWrapper",
    runtime_platform = "java11",
    dependencies = [
        ":log_resources",
        "src/java/com/twitter/twadoop/batch",
    ],
)

jvm_app(
    name = "batch-wrapper-test",
    basename = "statebird-v2-batch-wrapper-test",
    binary = ":batch-wrapper-test-bin",
    bundles = [
        # Files are relative to the location of current BUILD file
        # Any resource from current directory would not picked, needs to be inside a directory (like here inside 'resources')
        bundle(fileset = ["config/batch_wrapper_test*"]),
    ],
)

resources(
    name = "log_resources",
    sources = ["log4j.properties"],
    tags = ["bazel-compatible"],
)
