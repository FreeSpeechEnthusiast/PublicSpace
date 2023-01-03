junit_tests(
    name = "hnsw",
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    runtime_platform = "java11",
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/junit",
        "3rdparty/jvm/org/scalatest",
        "3rdparty/jvm/org/scalatestplus:junit",
        "ann/src/main/resources",
        "ann/src/main/scala/com/twitter/ann/service/query_server/hnsw:server",
        "ann/src/test/resources:files",
        "finatra-internal/mtls-thriftmux/src/test/scala:test-deps",
        "finatra/thrift/src/test/scala/com/twitter/finatra/thrift",
        "twitter-server-internal/src/main/scala",
    ],
)

jvm_binary(
    name = "index_generator",
    basename = "index_generator",
    main = "com.twitter.ann.service.query_server.hnsw.IndexGenerator",
    runtime_platform = "java11",
    testonly = True,
    dependencies = [
        ":hnsw",
        "3rdparty/jvm/ch/qos/logback:logback-classic",
        "3rdparty/jvm/org/slf4j:jcl-over-slf4j",
        "3rdparty/jvm/org/slf4j:jul-to-slf4j",
        "3rdparty/jvm/org/slf4j:log4j-over-slf4j",
        "ann/src/main/scala/com/twitter/ann/hnsw",
        "loglens/loglens-logback/src/main/scala/com/twitter/loglens/logback",
    ],
)
