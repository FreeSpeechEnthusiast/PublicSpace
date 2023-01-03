jvm_app(
    name = "proxee-test-examples-nest-bundle",
    basename = "proxee-testing-examples-dist",
    binary = "csl/proxee/app:app",
    bundles = [
        bundle(
            fileset = [
                "proxee_config.yaml",
                "start_proxee_on_nest.sh",
            ],
            relative_to = ".",
        ),
    ],
)
