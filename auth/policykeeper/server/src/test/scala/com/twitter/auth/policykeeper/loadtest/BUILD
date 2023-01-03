scala_library(
    name = "loadtest",
    sources = ["PolicyKeeperLoadTest.scala"],
    tags = ["bazel-compatible"],
    dependencies = [
        "auth/policykeeper/server/src/test/resources",
        "auth/policykeeper/thrift/src/main/thrift:thrift-scala",
        "iago-internal/src/main/scala/com/twitter/iago/internal",
        "iago/src/main/scala/com/twitter/iago",
        "iago/src/main/scala/com/twitter/iago/core",
        "iago/src/main/scala/com/twitter/iago/thrift",
    ],
)

jvm_binary(
    name = "loadtest-bin",
    basename = "loadtest-server",
    main = "com.twitter.iago.internal.ServerMain",
    runtime_platform = "java11",
    dependencies = [
        ":loadtest",
        "3rdparty/jvm/org/slf4j:slf4j-simple",
    ],
)
