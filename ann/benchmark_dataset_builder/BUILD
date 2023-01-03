python37_binary(
    name = "main",
    source = "main.py",
    dependencies = [
        ":benchmark_dataset_builder",
        "3rdparty/python/_closures/ann/benchmark_dataset_builder:main",
    ],
)

python3_library(
    name = "benchmark_dataset_builder",
    dependencies = [
        "3rdparty/python/numpy:default",
        "3rdparty/python/tensorflow",
        "ann/benchmarks",
    ],
)
