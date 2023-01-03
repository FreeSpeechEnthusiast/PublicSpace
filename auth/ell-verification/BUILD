jvm_binary(
    name = "bin",
    basename = "ell-verification",
    main = "com.twitter.auth.ellverification.EllVerificationAppMain",
    runtime_platform = "java11",
    dependencies = [
        "3rdparty/jvm/ch/qos/logback:logback-classic",
        "3rdparty/jvm/org/slf4j:jcl-over-slf4j",
        "3rdparty/jvm/org/slf4j:jul-to-slf4j",
        "3rdparty/jvm/org/slf4j:log4j-over-slf4j",
        "auth/ell-verification/src/main/scala/com/twitter/auth/ellverification",
        "loglens/loglens-logback/src/main/scala/com/twitter/loglens/logback",
    ],
)

#  Aurora Workflows build phase convention requires a jvm_app named with ${project-name}-app
jvm_app(
    name = "ell-verification-app",
    archive = "zip",
    binary = ":bin",
)
