python37_binary(
    name = "ann_benchmark",
    source = "run.py",
    zip_safe = False,
    dependencies = [
        ":benchmarks",
        "3rdparty/python/_closures/ann/benchmarks:ann_benchmark",
    ],
)

python3_library(
    name = "benchmarks",
    sources = [
        "!**/*.pyc",
        "!BUILD*",
        "**/*.py",
    ],
    dependencies = [
        ":resources",
        "3rdparty/python/annoy:default",
        "3rdparty/python/ansicolors:default",
        "3rdparty/python/faiss:default",
        "3rdparty/python/h5py:default",
        "3rdparty/python/jinja2:default",
        "3rdparty/python/matplotlib:default",
        "3rdparty/python/psutil:default",
        "3rdparty/python/pyyaml:default",
        "3rdparty/python/scikit-learn:default",
        "3rdparty/python/scipy:default",
        "3rdparty/python/six:default",
        "src/python/twitter/ml/common",
        "twml",
    ],
)

resources(
    name = "resources",
    sources = ["ann_benchmarks/resources/**/*"],
)
