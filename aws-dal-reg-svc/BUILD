python3_library(
    name = "aws_common",
    sources = [
        "aws_arn.py",
        "aws_cloud_watch.py",
        "aws_iam.py",
        "aws_organizations.py",
        "aws_resource.py",
        "aws_scanners.py",
    ],
    tags = [
        "bazel-compatible",
        "no-mypy",
    ],
    dependencies = [
        ":datastore",
        ":server_common",
        "3rdparty/python/boto3",
        "src/python/twitter/common/log",
        "src/python/twitter/common/metrics",
    ],
)

python3_library(
    name = "aws_kite_role",
    sources = [
        "aws_kite_role.py",
    ],
    tags = [
        "bazel-compatible",
        "no-mypy",
    ],
    dependencies = [
        ":aws_common",
        "gcp_registration/kite:kite_role",
    ],
)

python3_library(
    name = "datastore",
    sources = ["datastore.py"],
    tags = [
        "bazel-compatible",
        "no-mypy",
    ],
    dependencies = [
        ":server_common",
        "3rdparty/python/boto3",
    ],
)

python3_library(
    name = "registration",
    sources = [
        "dataset.py",
        "dataset_attribution.py",
        "dataset_metastore.py",
        "dataset_stagestore.py",
        "registration.py",
    ],
    tags = [
        "bazel-compatible",
        "no-mypy",
    ],
    dependencies = [
        ":aws_common",
        ":aws_kite_role",
        ":datastore",
        ":server_common",
        "3rdparty/python/requests:default",
        "gcp_registration:kite_client",
        "gcp_registration/kite:kite_role",
        "src/python/twitter/ml/common",
        "src/python/twitter/s2s",
        "src/python/twitter/s2s/client",
        "src/thrift/com/twitter/dal:dal_no_constants-python",
        "src/thrift/com/twitter/dal/dataclassification:dataclassification-python",
    ],
)

python3_library(
    name = "server_common",
    sources = [
        "errors.py",
        "server_config.py",
        "server_state.py",
        "util.py",
    ],
    tags = [
        "bazel-compatible",
        "no-mypy",
    ],
    dependencies = [
        "src/python/twitter/common/log",
    ],
)

python37_binary(
    name = "account_status",
    source = "account_status.py",
    dependencies = [
        ":aws_common",
        ":registration",
        ":server_common",
        "3rdparty/python/_closures/aws-dal-reg-svc:account_status",
        "src/python/twitter/common/app",
        "src/python/twitter/common/log",
    ],
)

python37_binary(
    name = "dal_delete_datasets",
    source = "dal_delete_datasets.py",
    tags = ["no-mypy"],
    dependencies = [
        ":aws_common",
        ":registration",
        ":server_common",
        "3rdparty/python/_closures/aws-dal-reg-svc:dal_delete_datasets",
        "kite/utils/python/src/python/twitter/kite/utils",
        "src/python/twitter/common/app",
        "src/python/twitter/common/log",
        "src/python/twitter/common_internal/log:loglens_handler",
    ],
)

python37_binary(
    name = "dal_list",
    source = "dal_list.py",
    dependencies = [
        ":registration",
        ":server_common",
        "3rdparty/python/_closures/aws-dal-reg-svc:dal_list",
        "src/python/twitter/common/app",
        "src/python/twitter/common/log",
    ],
)

python37_binary(
    name = "dataset_observer",
    source = "dataset_observer.py",
    tags = ["no-mypy"],
    dependencies = [
        ":aws_common",
        ":registration",
        ":server_common",
        "3rdparty/python/_closures/aws-dal-reg-svc:dataset_observer",
        "src/python/twitter/common/app",
        "src/python/twitter/common/log",
    ],
)

python37_binary(
    name = "server",
    source = "server.py",
    tags = ["no-mypy"],
    dependencies = [
        ":aws_common",
        ":registration",
        ":server_common",
        "3rdparty/python/_closures/aws-dal-reg-svc:server",
        "kite/utils/python/src/python/twitter/kite/utils",
        "src/python/twitter/common/app",
        "src/python/twitter/common/log",
        "src/python/twitter/common/metrics",
        "src/python/twitter/common_internal/log:loglens_handler",
    ],
)

python37_tests(
    name = "tests",
    sources = ["test/*.py"],
    tags = [
        "bazel-compatible",
        "no-mypy",
    ],
    dependencies = [
        ":aws_common",
        ":registration",
        ":server_common",
        "3rdparty/python/_closures/aws-dal-reg-svc:tests",
    ],
)
