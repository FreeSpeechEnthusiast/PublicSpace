jvm_binary(
    name = "customerauthcli",
    main = "com.twitter.auth.customerauthtooling.cli.JvmForkerMain",
    compiler_option_sets = ["fatal_warnings"],
    runtime_platform = "java11",
    strict_deps = False,
    dependencies = [
        ":cli",
    ],
)

scala_library(
    name = "cli",
    sources = ["**/*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    # This target directly depends on a logback appender in code thus
    # add a dependency on Logback here which we enable by adding the following tag
    tags = [
        "bazel-compatible",
        "bazel-incompatible-scaladoc",  # see http://go/bazel-incompatible-scaladoc
        "logging_impl_check_whitelisted_target",
    ],
    dependencies = [
        "3rdparty/jvm/ch/qos/logback:logback-classic",
        "3rdparty/jvm/com/fasterxml/jackson/dataformat:jackson-dataformat-yaml",
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/commons-lang",
        "3rdparty/jvm/info/picocli",
        "3rdparty/jvm/info/picocli:picocli-shell-jline3",
        "3rdparty/jvm/javax/inject:javax.inject",
        "3rdparty/jvm/net/codingwell:scala-guice",
        "3rdparty/jvm/org/apache/commons:commons-csv",
        "3rdparty/jvm/org/jline",
        "3rdparty/jvm/org/jline:jline-terminal-jansi",
        "3rdparty/jvm/org/slf4j:jul-to-slf4j",
        "3rdparty/jvm/org/slf4j:slf4j-api",
        "auth/customerauthtooling/server/src/main/resources",
        "auth/customerauthtooling/server/src/main/scala/com/twitter/auth/customerauthtooling/api/models",
        "auth/customerauthtooling/thrift/src/main/thrift:thrift-scala",
        "finagle/finagle-mux/src/main/scala",
        "finagle/finagle-thrift/src/main/scala",
        "finagle/finagle-thriftmux/src/main/scala",
        "finatra-internal/mtls-thriftmux/src/main/scala",
        "finatra/http-server/src/main/scala/com/twitter/finatra/http",
        "finatra/inject/inject-app/src/main/scala/com/twitter/inject/app/internal",
        "finatra/inject/inject-core/src/main/scala",
        "finatra/inject/inject-logback/src/main/scala",
        "finatra/inject/inject-thrift-client/src/main/scala",
        "finatra/thrift/src/main/scala/com/twitter/finatra/thrift",
        "loglens/loglens-logback/src/main/scala/com/twitter/loglens/logback",
        "twitter-server/logback-classic/src/main/scala",
    ],
)
