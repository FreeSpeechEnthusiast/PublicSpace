test_suite(
    name = "test",
    tags = ["bazel-compatible"],
    dependencies = [
        "tests/scala/com/twitter/statebird/watchdog/server",
    ],
)

jvm_binary(
    name = "bin",
    basename = "statebird-watchdog-server",
    main = "com.twitter.statebird.watchdog.server.Main",
    runtime_platform = "java11",
    dependencies = [
        "3rdparty/jvm/org/slf4j:slf4j-jdk14",
        "src/scala/com/twitter/statebird/watchdog/server",
        "twitter-server/slf4j-jdk14/src/main/scala",
    ],
)

jvm_binary(
    name = "dev_bin",
    main = "com.twitter.statebird.watchdog.server.Main",
    runtime_platform = "java11",
    dependencies = [
        "3rdparty/jvm/org/slf4j:slf4j-jdk14",
        "src/scala/com/twitter/statebird/watchdog/server",
        "tests/scala/com/twitter/statebird/watchdog/server/config",
        "twitter-server/slf4j-jdk14/src/main/scala",
    ],
)

jvm_app(
    name = "bundle",
    basename = "statebird-watchdog-server-package-dist",
    binary = ":bin",
    bundles = [
        bundle(fileset = ["config/**/*"]),
    ],
)
