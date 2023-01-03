target(
    name = "thrift",
    dependencies = [
        "src/thrift/com/twitter/statebird:compiled-v2-scala",
    ],
)

test_suite(
    name = "test",
    tags = ["bazel-compatible"],
    dependencies = [
        "tests/scala/com/twitter/statebird/server/v2",
    ],
)

target(
    name = "server",
    dependencies = [
        "src/scala/com/twitter/statebird",
    ],
)

target(
    name = "bin",
    dependencies = ["src/scala/com/twitter/statebird:bin-v2"],
)

jvm_binary(
    name = "dev_bin",
    basename = "statebird-v2-server",
    main = "com.twitter.statebird.server.v2.Main",
    runtime_platform = "java11",
    dependencies = [
        "src/scala/com/twitter/statebird",
        "tests/scala/com/twitter/statebird/server/v2/config",
    ],
)

jvm_app(
    name = "bundle",
    basename = "statebird-v2-server-package-dist",
    binary = "src/scala/com/twitter/statebird:bin-v2",
    bundles = [
        bundle(fileset = ["config/**/*"]),
    ],
)
