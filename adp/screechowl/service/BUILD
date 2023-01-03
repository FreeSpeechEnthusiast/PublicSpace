test_suite(
    name = "test",
    tags = ["bazel-compatible"],
    dependencies = [
        "tests/scala/com/twitter/screechowl/service",
    ],
)

jvm_binary(
    name = "bin",
    basename = "screech-owl-service-server",
    main = "com.twitter.screechowl.service.Main",
    runtime_platform = "java11",
    dependencies = [
        "src/scala/com/twitter/screechowl/service",
        # the logback logging implementation
        "twitter-server/logback-classic",
        # The logback.xml config file
        "adp/screechowl/service/logback",
        # Splunk logging
        "loglens/loglens-logback/src/main/scala/com/twitter/loglens/logback",
    ],
)

jvm_app(
    name = "bundle",
    basename = "screech-owl-service",
    binary = ":bin",
    bundles = [
        bundle(fileset = ["config/*"]),
    ],
)
