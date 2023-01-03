jvm_binary(
    name = "bin",
    basename = "dal-server",
    main = "com.twitter.dal.server.Main",
    runtime_platform = "java11",
    dependencies = [
        "adp/dal_v2/common/resources/logback",
        "loglens/loglens-logback/src/main/scala/com/twitter/loglens/logback",
        "src/scala/com/twitter/dal/server",
        "twitter-server/logback-classic",
    ],
)

jvm_app(
    name = "dal",
    binary = ":bin",
    bundles = [
        bundle(fileset = ["config/*"]),
    ],
)

jvm_binary(
    name = "dev_bin",
    main = "com.twitter.dal.server.Main",
    runtime_platform = "java11",
    dependencies = [
        "adp/dal_v2/common/resources/logback_test",
        "src/scala/com/twitter/dal/server",
        "tests/scala/com/twitter/dal/server/config/test",
        "twitter-server/logback-classic",
    ],
)
