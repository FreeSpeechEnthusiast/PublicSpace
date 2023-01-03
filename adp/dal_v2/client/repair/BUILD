# first two targets are bundled and deployed by aurora workflow from deploy-repair-service.sh
jvm_binary(
    name = "bin",
    basename = "dal-repair-service",
    main = "com.twitter.dal.client.repair.DALRepairService",
    runtime_platform = "java11",
    dependencies = [
        # The logback.xml config file
        "adp/dal_v2/common/resources/logback",
        "loglens/loglens-logback/src/main/scala/com/twitter/loglens/logback",
        "src/scala/com/twitter/dal/client/repair",
        # the logback logging implementation
        "twitter-server/logback-classic",
    ],
)

jvm_app(
    name = "bundle",
    basename = "dal-repair-service-dist",
    binary = ":bin",
    bundles = [
        bundle(fileset = ["config/deploy-repair-service.aurora"]),
    ],
)

# next two targets are bundled and deployed to aurora from deploy-backfill-adhoc.sh
jvm_binary(
    name = "backfill-bin",
    basename = "dal-backfill-bin",
    main = "com.twitter.dal.client.repair.DALRepairTool",
    runtime_platform = "java11",
    dependencies = [
        # The logback.xml config file
        "adp/dal_v2/common/resources/logback",
        "loglens/loglens-logback/src/main/scala/com/twitter/loglens/logback",
        "src/scala/com/twitter/dal/client/repair",
        # the logback logging implementation
        "twitter-server/logback-classic",
    ],
)

jvm_app(
    name = "backfill-bundle",
    basename = "dal-backfill-dist",
    binary = ":backfill-bin",
    bundles = [
        bundle(fileset = [
            "config/deploy-backfill-adhoc.aurora",
            "config/backfill.txt",
        ]),
    ],
)
