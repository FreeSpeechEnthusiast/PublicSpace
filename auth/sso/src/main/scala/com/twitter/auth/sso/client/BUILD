scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/com/google:google-api-client",
        "account_security/utils/src/main/scala/com/twitter/account_security/utils",
        "auth/sso/src/main/scala/com/twitter/auth/sso/models",
        "auth/sso/src/main/scala/com/twitter/auth/sso/util",
        "finatra/http-core/src/main/scala/com/twitter/finatra/http/marshalling",
        "src/thrift/com/twitter/guano:guano-scala",
        "strato/config/columns/auth/sso:sso-strato-client",
        "strato/src/main/scala/com/twitter/strato/client",
        "util/util-jackson/src/main/scala/com/twitter/util/jackson",
    ],
)
