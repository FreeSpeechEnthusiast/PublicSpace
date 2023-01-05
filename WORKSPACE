load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive", "http_file", "http_jar")
load("@bazel_tools//tools/jdk:local_java_repository.bzl", "local_java_repository")
load("//tools/build_rules/repository:buildozer.bzl", "buildifier_tool", "buildozer_tool")
load("//tools/build_rules/repository:apache_thrift.bzl", "apache_thrift_tool")
load("//tools/build_rules/repository:common.bzl", "builds_repository", "workspace_path")
load("//tools/build_rules/repository:packer.bzl", "packer_tool")
load("//tools/build_rules/repository:nodejs.bzl", "nodejs_tool")
load("//tools/build_rules/repository:rules_proto_deps.bzl", "register_proto_deps", "rules_proto_dependencies_override")
load("@bazel_tools//tools/jdk:local_java_repository.bzl", "local_java_repository")
load("@bazel_tools//tools/jdk:remote_java_repository.bzl", "remote_java_repository")

# Even though these are local JDKs, bazel will still try to fetch the hardcoded "remotejdk"
# unless it is overwritten here.
local_java_repository(
    name = "remotejdk_8",
    java_home = "/tmp/bazel_tools/jdk/8",
    version = "8",
)

local_java_repository(
    name = "remotejdk_11",
    java_home = "/tmp/bazel_tools/jdk/11",
    version = "11",
)

local_java_repository(
    name = "remotejdk8_linux",
    java_home = "/tmp/bazel_tools/jdk/8",
    version = "8",
)

local_java_repository(
    name = "remotejdk11_linux",
    java_home = "/tmp/bazel_tools/jdk/11",
    version = "11",
)

workspace_path(
    name = "workspace_root",
    path = __workspace_dir__,
)

# INTL-11641 / DPB-13124: Hard code tools.jar from TwitterJDK (1.8.0_275)
http_file(
    name = "tools_jar",
    downloaded_file_path = "tools.jar",
    sha256 = "5cb0662eb2b7bd7c3529ec04fc5292336dda8585b3b9ebd3fe50bc7edd1a3c3f",
    urls = ["https://science-binaries.local.twitter.com/home/build/tools_jar/1.8.0_275/tools.jar"],
)

JAVA_VIRTUAL = "https://artifactory.local.twitter.com/java-virtual/"

# platforms needs to be updated to support ARM/M1 based Macs. See
# https://github.com/bazelbuild/bazel/issues/15175
# At minimum this update must include the change from
# https://github.com/bazelbuild/platforms/pull/22
http_archive(
    name = "platforms",
    patch_args = [
        "-p0",
    ],
    sha256 = "54095d9e2a2c6c0d4629c99fc80ecf4f74f93771aea658c872db888c1103bb93",
    strip_prefix = "platforms-fbd0d188dac49fbcab3d2876a2113507e6fc68e9",
    urls = [
        "https://artifactory.twitter.biz:443/tools/bazel-plugin-deps/github.com/bazelbuild/platforms/archive/fbd0d188dac49fbcab3d2876a2113507e6fc68e9.zip",
    ],
)

http_archive(
    name = "pypi__pip",
    # taken from https://github.com/bazelbuild/rules_python/blob/main/python/pip_install/repositories.bzl
    build_file_content = """package(default_visibility = ["//visibility:public"])""",
    sha256 = "78cb760711fedc073246543801c84dc5377affead832e103ad0211f99303a204",
    type = "zip",
    url = "https://artifactory.local.twitter.com:443/generic-3rdparty-local/packages/47/ca/f0d790b6e18b3a6f3bd5e80c2ee4edbb5807286c21cdd0862ca933f751dd/pip-21.1.3-py3-none-any.whl",
)

http_archive(
    name = "pypi__setuptools",
    # taken from https://github.com/bazelbuild/rules_python/blob/main/python/pip_install/repositories.bzl
    build_file_content = """package(default_visibility = ["//visibility:public"])""",
    sha256 = "ddae4c1b9220daf1e32ba9d4e3714df6019c5b583755559be84ff8199f7e1fe3",
    type = "zip",
    url = "https://artifactory.local.twitter.com:443/generic-3rdparty-local/packages/a2/e1/902fbc2f61ad6243cd3d57ffa195a9eb123021ec912ec5d811acf54a39f8/setuptools-57.1.0-py3-none-any.whl",
)

http_archive(
    name = "pypi__pkginfo",
    # taken from https://github.com/bazelbuild/rules_python/blob/main/python/pip_install/repositories.bzl
    build_file_content = """package(default_visibility = ["//visibility:public"])""",
    sha256 = "37ecd857b47e5f55949c41ed061eb51a0bee97a87c969219d144c0e023982779",
    type = "zip",
    url = "https://artifactory.local.twitter.com:443/generic-3rdparty-local/packages/77/83/1ef010f7c4563e218854809c0dff9548de65ebec930921dedf6ee5981f27/pkginfo-1.7.1-py2.py3-none-any.whl",
)

http_archive(
    name = "rules_python",
    sha256 = "a30abdfc7126d497a7698c29c46ea9901c6392d6ed315171a6df5ce433aa4502",
    strip_prefix = "rules_python-0.6.0",
    url = "https://artifactory.local.twitter.com:443/generic-3rdparty-local/bazel/rules/rules_python/rules_python-0.6.0.tar.gz",
)

# rules_pkg dep.
http_archive(
    name = "rules_license",
    sha256 = "4865059254da674e3d18ab242e21c17f7e3e8c6b1f1421fffa4c5070f82e98b5",
    url = "https://artifactory.local.twitter.com/generic-3rdparty-local/bazel/rules/rules_license/rules_license-0.0.1.tar.gz",
)

http_archive(
    name = "rules_pkg",
    patch_args = ["-p1"],
    patches = [
        "//tools/patches/rules_pkg:rules_pkg-0.7.0-repo_root_import.patch",
    ],
    sha256 = "8a298e832762eda1830597d64fe7db58178aa84cd5926d76d5b744d6558941c2",
    url = "https://artifactory.local.twitter.com/generic-3rdparty-local/bazel/rules/rules_pkg/rules_pkg-0.7.0_3.tar.gz",
)

http_archive(
    name = "rules_antlr",
    sha256 = "d4ff26e96f75e958e5bd769e08244540cce038e32c022816c71c087480b57129",
    url = "https://artifactory.local.twitter.com/generic-3rdparty-local/bazel/rules/rules_antlr-mirror/rules_antlr-mirror_7ad7536f8e1f119bbf4b6599c1dd34a6d51c8569.zip",
)

load("@rules_antlr//antlr:repositories.bzl", "rules_antlr_dependencies")

rules_proto_dependencies_override()

register_proto_deps()

load("@rules_proto//proto:repositories.bzl", "rules_proto_dependencies", "rules_proto_toolchains")

rules_proto_dependencies()

rules_proto_toolchains()

load("@bazel_skylib//:workspace.bzl", "bazel_skylib_workspace")

bazel_skylib_workspace()

# io_bazel_rules_scala needs to be registered after `rules_proto_dependencies_override`/`rules_proto_dependencies`
# as io_bazel_rules_scala will attempt to register its down protobuf dependencies in
# https://github.com/twitter-forks/rules_scala/blob/master/scala/private/macros/scala_repositories.bzl#L9
http_archive(
    name = "io_bazel_rules_scala",
    # Necessary flag for patches in the format of git diffs
    patch_args = ["-p1"],
    # Used for non-upstreamed patches, see tools/patches/rules_scala/README.md
    patches = [
        "//tools/patches/rules_scala:scrooge_generator.patch",
        "//tools/patches/rules_scala:thrift_validators.patch",
        "//tools/patches/rules_scala:javabin.patch",
    ],
    sha256 = "6e727629b48896ed20ce778c09b57cd660e9823875d3325612f878fac27ab540",
    url = "https://artifactory.twitter.biz/generic-3rdparty-local/bazel/rules/rules_scala-upstream/rules_scala-upstream_1373d308bda90e42eb9c0f52d6c0ff76558f7870.zip",
)

# git archive --format zip --output /tmp/rules_proto_grpc-mirror_<commitsha>.zip  master
http_archive(
    name = "rules_proto_grpc",
    patch_args = ["-p1"],
    patches = [
        "//tools/patches/rules_proto_grpc:per_target_protoc.patch",
        "//tools/patches/rules_proto_grpc:comment_out_grpc_java_plugin.patch",
    ],
    sha256 = "dfd2b4aeb4203f5d59ed0dfd2151a65146cc1618aa4e9c89ae6671afeaf4a86b",
    url = "https://artifactory.twitter.biz/artifactory/generic-3rdparty-local/bazel/rules/rules_proto_grpc-mirror/rules_proto_grpc-mirror_2e8be8e27cc82203794f14dbdcf37189b02ab722.zip",
)

http_archive(
    name = "remote_coverage_tools",
    sha256 = "96ac6bc9b9fbc67b532bcae562da1642409791e6a4b8e522f04946ee5cc3ff8e",
    urls = [
        "https://science-binaries.local.twitter.com/home/build/bazel_remote_coverage_tools/coverage_output_generator-v2.1.zip",
    ],
)

# Mirrored from https://github.com/bazelbuild/java_tools
http_archive(
    name = "remote_java_tools",
    sha256 = "a7ac5922ee01e8b8fcb546ffc264ef314d0a0c679328b7fa4c432e5f54a86067",
    urls = [
        "https://science-binaries.local.twitter.com/home/build/bazel_java_tools/v11.6/java_tools-v11.6.zip",
    ],
)

http_archive(
    name = "remote_java_tools_windows",
    sha256 = "939f9d91f0df02851bbad8f5b1d26d24011329394cafe5668c1234e31ac2a1f7",
    urls = [
        "https://science-binaries.local.twitter.com/home/build/bazel_java_tools/v11.6/java_tools_windows-v11.6.zip",
    ],
)

http_archive(
    name = "remote_java_tools_linux",
    sha256 = "15da4f84a7d39cd179acf3035d9def638eea6ba89a0ed8f4e8a8e6e1d6c8e328",
    urls = [
        "https://science-binaries.local.twitter.com/home/build/bazel_java_tools/v11.6/java_tools_linux-v11.6.zip",
    ],
)

http_archive(
    name = "remote_java_tools_darwin",
    sha256 = "f17ee54582b61f1ebd84c8fa2c54df796914cfbaac3cb821fb1286b55b080bc0",
    urls = [
        "https://science-binaries.local.twitter.com/home/build/bazel_java_tools/v11.6/java_tools_darwin-v11.6.zip",
    ],
)

load("//tools/build_rules:scala_version.bzl", "get_scala_version_string")
load("//tools/toolchains:scala_toolchains.bzl", "scala_register_toolchains")

scala_version = get_scala_version_string(get_patch = False)

scala_version_full = get_scala_version_string()

load("@io_bazel_rules_scala//:scala_config.bzl", "scala_config")

scala_config(scala_version = scala_version_full)

load("@io_bazel_rules_scala//scala:scala.bzl", "scala_repositories")
load("@io_bazel_rules_scala//scalatest:scalatest.bzl", "scalatest_repositories")

# Because rules_scala does its own resolves for things like scalatest, we need to configure how they do resolves.
# TODO: Upstream a way to re-use our own resolve from rules_jvm_external, rather than having rules_scala finding
# its own independent copies.
internal_maven_repo_urls = [
    "https://artifactory-ci.twitter.biz/java-virtual/",
]

overriden_scala_artifacts = {
    "io_bazel_rules_scala_scala_library": {
        "artifact": "org.scala-lang:scala-library:{}".format(scala_version_full),
        "sha256": "e518bb640e2175de5cb1f8e326679b8d975376221f1b547757de429bbf4563f0",
    },
    "io_bazel_rules_scala_scala_compiler": {
        "artifact": "org.scala-lang:scala-compiler:{}".format(scala_version_full),
        "sha256": "bf4d3ed5a5bc1581bc44512c98f61e0aee128fbfa7536f0ce367d2e42b84db07",
    },
    "io_bazel_rules_scala_scala_reflect": {
        "artifact": "org.scala-lang:scala-reflect:{}".format(scala_version_full),
        "sha256": "d5a21ab16b35dbe1fa9f50267d3c198d4797084a61557874ca53c85f15747e48",
    },
}

scala_repositories(
    maven_servers = internal_maven_repo_urls,
    overriden_artifacts = overriden_scala_artifacts,
)

scalatest_repositories(
    fetch_sources = True,
    maven_servers = internal_maven_repo_urls,
)

register_toolchains("//tools/jdks:twitter_jdk8_toolchain_definition")

register_toolchains("//tools/jdks:twitter_jdk11_toolchain_definition")

scala_register_toolchains()

register_toolchains("//tools/toolchains:testing_toolchain")

load("@io_bazel_rules_scala//twitter_scrooge:twitter_scrooge.bzl", "scrooge_scala_library", "twitter_scrooge")

twitter_scrooge(
    libthrift = "//tools/implicit_deps:thrift-implicit-deps-impl",
    maven_servers = internal_maven_repo_urls,
    scrooge_core = "//scrooge/scrooge-core",
    scrooge_generator = "//scrooge-internal/generator:lib",
    util_core = "//util/util-core",
    util_logging = "//util/util-logging",
)

load("@io_bazel_rules_scala//junit:junit.bzl", "junit_repositories")

junit_repositories(maven_servers = internal_maven_repo_urls)

load("@io_bazel_rules_scala//specs2:specs2.bzl", "specs2_repositories")

specs2_repositories(maven_servers = internal_maven_repo_urls)

load("@io_bazel_rules_scala//specs2:specs2_junit.bzl", "specs2_junit_repositories")

specs2_junit_repositories(maven_servers = internal_maven_repo_urls)

http_jar(
    name = "antlr4_runtime",
    sha256 = "2337df5d81e715b39aeea07aac46ad47e4f1f9e9cd7c899f124f425913efdcf8",
    urls = [JAVA_VIRTUAL + "org/antlr/antlr4-runtime/4.8/antlr4-runtime-4.8.jar"],
)

http_jar(
    name = "antlr4_tool",
    sha256 = "6e4477689371f237d4d8aa40642badbb209d4628ccdd81234d90f829a743bac8",
    urls = [JAVA_VIRTUAL + "org/antlr/antlr4/4.8/antlr4-4.8.jar"],
)

http_jar(
    name = "javax_json",
    sha256 = "0e1dec40a1ede965941251eda968aeee052cc4f50378bc316cc48e8159bdbeb4",
    urls = [JAVA_VIRTUAL + "org/glassfish/javax.json/1.0.4/javax.json-1.0.4.jar"],
)

http_jar(
    name = "antlr3_tool",
    sha256 = "5ac36c2acfb0a0f3d37dafe20b5b570f2643e2d000c648d44503c2738be643df",
    urls = [JAVA_VIRTUAL + "org/antlr/antlr/3.5.2/antlr-3.5.2.jar"],
)

http_jar(
    name = "antlr3_runtime",
    sha256 = "ce3fc8ecb10f39e9a3cddcbb2ce350d272d9cd3d0b1e18e6fe73c3b9389c8734",
    urls = [JAVA_VIRTUAL + "org/antlr/antlr-runtime/3.5.2/antlr-runtime-3.5.2.jar"],
)

http_jar(
    name = "stringtemplate4",
    sha256 = "28547dba48cfceb77b6efbfe069aebe9ed3324ae60dbd52093d13a1d636ed069",
    urls = [JAVA_VIRTUAL + "org/antlr/ST4/4.3/ST4-4.3.jar"],
)

# For bazel-multideps
load("//tools/build_rules/repository:multiversion.bzl", "multiversion_repository", "register_multiversion_tools")

register_multiversion_tools()

# Generate `builds.bzl`, which provides a list of labels
# that point to the buildfiles in 3rdparty/jvm
# This allows us to encode the dependency between the 3rdparty
# resolution and the build files that declare the dependencies.
builds_repository(
    name = "jvm_builds",
    builds = "//:.3rdparty-jvm-builds.txt",
)

load(
    "@jvm_builds//:builds.bzl",
    jvm_builds = "builds",
)

multiversion_repository(
    name = "3rdparty_jvm",
    builds = jvm_builds(),
    multiversion_tool = "@multiversion//:run_multiversion.sh",
)

load("@3rdparty_jvm//:jvm_deps.bzl", "jvm_deps")

jvm_deps()

load("@maven//:jvm_deps.bzl", "load_jvm_deps")

load_jvm_deps()

# Register a custom rules_scala JMH toolchain that uses JMH libraries declared under 3rdparty/jvm
register_toolchains("//tools/toolchains:jmh_toolchain")

# Define and fetch python requirements
builds_repository(
    name = "python_builds",
    builds = "//:.3rdparty-python-builds.txt",
)

load(
    "@python_builds//:builds.bzl",
    python_builds = "builds",
)
load("//tools/build_rules/repository:python.bzl", "parse_requirements")

parse_requirements(
    name = "3rdparty_python",
    builds = python_builds(),
    staging_repos = [],
)

load("@3rdparty_python//:defs.bzl", "install_py_deps")

install_py_deps()

http_file(
    name = "buildozer_linux",
    executable = True,
    sha256 = "c76e8e7dd37eddb219beb5b604cf98d0b84faa521fbf60d994401245fd557fe0",
    urls = ["https://science-binaries.local.twitter.com/home/pants/build-support/bin/buildozer/linux/x86_64/0.26.0-e11f9905088bf81ccefd17b6d05cad9003eae8eb/buildozer"],
)

http_file(
    name = "buildozer_darwin",
    executable = True,
    sha256 = "2b8eeaab9b429e04956942e5f7f126a92d6ecef33a8225ae5155f46e00600008",
    urls = ["https://science-binaries.local.twitter.com/home/pants/build-support/bin/buildozer/mac/10.17/0.26.0-e11f9905088bf81ccefd17b6d05cad9003eae8eb/buildozer"],
)

buildozer_tool(name = "buildozer")

http_file(
    name = "buildifier_linux",
    executable = True,
    sha256 = "6145ccb6690f32526a5e6e658ed063169847021a13cb25ce4043289db3c6e25d",
    urls = ["https://science-binaries.local.twitter.com/home/pants/build-support/bin/buildifier/linux/x86_64/0.6.0-3cbb8da4f5ae3b6a5e0a64a5d20d426d308af838/buildifier"],
)

http_file(
    name = "buildifier_darwin",
    executable = True,
    sha256 = "767df2d33af643b5215231f5b4b0b1d4d8023a545a7e5d79c8bf1a48499d174d",
    urls = ["https://science-binaries.local.twitter.com/home/pants/build-support/bin/buildifier/mac/10.17/0.6.0-3cbb8da4f5ae3b6a5e0a64a5d20d426d308af838/buildifier"],
)

buildifier_tool(name = "buildifier")

http_file(
    name = "apache_thrift_linux",
    executable = True,
    sha256 = "5b2fcf65de2ce5816cb28c474a47437497c2a086af8a1a05439d001a80427847",
    urls = ["http://science-binaries.local.twitter.com/home/pants/build-support/bin/thrift/linux/x86_64/0.13.0/thrift"],
)

http_file(
    name = "apache_thrift_darwin",
    executable = True,
    sha256 = "884e847bdbd93692752d73d605ebd36c5068f75ec92c5831d4e61dc35a8e5e2f",
    urls = ["http://science-binaries.local.twitter.com/home/pants/build-support/bin/thrift/mac/10.13/0.13.0/thrift"],
)

apache_thrift_tool(name = "apache_thrift")

# Register the ee-python toolchain for native bazel rules
register_toolchains("//tools/build_rules/python:ee_python_toolchain")

# Register internal python toolchains
register_toolchains("//tools/build_rules/python/toolchains/cp310:cp310_toolchain")

register_toolchains("//tools/build_rules/python/toolchains/cp39:cp39_toolchain")

register_toolchains("//tools/build_rules/python/toolchains/cp38:cp38_toolchain")

register_toolchains("//tools/build_rules/python/toolchains/cp37:cp37_toolchain")

http_file(
    name = "aurora_cli",
    executable = True,
    # sha256 checksum computed via:
    # $ wget <url>
    # $ openssl dgst -sha256 <file>
    sha256 = "d48e6d4c89a853ce01b08267839c25a1647b26b2e2bd2db8f2e248874bd646d3",
    # URL from Packer's "alternateUris" value: `packer versions --json --cluster=smf1 aurora aurora`
    urls = ["https://science-binaries.local.twitter.com/home/pants/build-support/bin/aurora/v173/aurora.pex"],
)

http_file(
    name = "translation_bundle_cli",
    executable = True,
    # sha256 checksum computed via:
    # $ wget <url>
    # $ openssl dgst -sha256 <file>
    sha256 = "59d943a19c6c8addf34ee2406a0d0c20c529c77f81080d5169ad0842afadd3a4",
    urls = ["https://science-binaries.local.twitter.com/home/pants/build-support/bin/download_translations/v1/download_translations.pex"],
)

http_file(
    name = "thriftstore_codegen",
    executable = True,
    sha256 = "02cbedad3243453603a32d77eb3366ef96374f413677b19c8aace3a753150449",
    urls = ["https://science-binaries.local.twitter.com/home/pants/build-support/bin/thriftstore_codegen/0.1.2/thriftstore_codegen.pex"],
)

# This is the rust language support. This is maintained by the rust language team. Find us at http://go/rust.
load("//tools/build_rules/rust:workspace_fetch.bzl", "fetch_rust_rules")

fetch_rust_rules()

load("//tools/build_rules/rust:workspace_setup.bzl", "setup_rust_rules")

setup_rust_rules()

# Go support
http_archive(
    name = "bazel_gazelle",
    patch_args = [
        "-p1",
    ],
    patches = [
        # TODO(DPB-16630): prevent SQ from testing gazelle internals so that test removal patch is not necessary
        "//tools/patches/bazel-gazelle:remove_internal_tests.patch",
    ],
    sha256 = "501deb3d5695ab658e82f6f6f549ba681ea3ca2a5fb7911154b5aa45596183fa",
    urls = [
        "https://artifactory.local.twitter.com/tools/bazel-plugin-deps/bazelbuild/bazel-gazelle/releases/download/v0.26.0/bazel-gazelle-v0.26.0.tar.gz",
    ],
)

http_archive(
    name = "io_bazel_rules_go",
    patch_args = [
        "-p1",
    ],
    patches = [
        # https://github.com/bazelbuild/rules_go/pull/3256
        "//tools/patches/rules_go:rules_go-0.34.0-env_inherit.patch",
    ],
    sha256 = "16e9fca53ed6bd4ff4ad76facc9b7b651a89db1689a2877d6fd7b82aa824e366",
    urls = [
        "https://artifactory.local.twitter.com/tools/bazel-plugin-deps/bazel-mirror/github.com/bazelbuild/rules_go/releases/download/v0.34.0/rules_go-v0.34.0.zip",
    ],
)

load("//tools/build_rules/go:deps.bzl", "gazelle_dependencies", "go_register_toolchains", "go_rules_dependencies")
load("//3rdparty/go:deps.bzl", "go_dependencies")

# gazelle:repository_macro 3rdparty/go/deps.bzl%go_dependencies
go_dependencies()

go_rules_dependencies()

go_register_toolchains(
    sdks = {
        "linux_amd64": ("go1.18.2.linux-amd64.tar.gz", "e54bec97a1a5d230fc2f9ad0880fcbabb5888f30ed9666eca4a91c5a32e86cbc"),
        "darwin_amd64": ("go1.18.2.darwin-amd64.tar.gz", "1f5f539ce0baa8b65f196ee219abf73a7d9cf558ba9128cc0fe4833da18b04f2"),
        "darwin_arm64": ("go1.18.2.darwin-arm64.tar.gz", "6c7df9a2405f09aa9bab55c93c9c4ce41d3e58127d626bc1825ba5d0a0045d5c"),
    },
)

gazelle_dependencies()

# Container experimentation for source
load(
    "//tools/build_rules/contrib/container:deps.bzl",
    internal_container_deps = "deps",
)

internal_container_deps()

load(
    "//tools/build_rules/contrib/container:repositories.bzl",
    internal_container_repos = "deps",
)

internal_container_repos()

load(
    "@io_bazel_rules_docker//repositories:repositories.bzl",
    container_repositories = "repositories",
)

container_repositories()

load(
    "@io_bazel_rules_docker//repositories:deps.bzl",
    container_deps = "deps",
)

container_deps()

load("//3rdparty/container:images.bzl", "setup_base_images")

setup_base_images()

# NodeJS

# 14.18.3 from MDE - https://sourcegraph.twitter.biz/git.twitter.biz/managed_development_environment/-/blob/package/eng/core/nodejs/group.json
http_file(
    name = "nodejs_darwin",
    downloaded_file_path = "nodejs.tar.gz",
    executable = False,
    sha256 = "623579faa9faf1148e42c84e36c7b701ddded220d1795d94d93ed7561b699407",
    urls = ["https://artifactory.local.twitter.com/tools/managed_development_environment/package/MacOSX/node-v14.18.3-darwin-x64.tar.gz=2022-01-18=623579faa9faf1148e42c84e36c7b701ddded220d1795d94d93ed7561b699407"],
)

# 14.18.3 from packer web-shared/nodejs/live (v33)
# This comes bundled with yarnpkg
http_file(
    name = "nodejs_linux",
    downloaded_file_path = "nodejs.tar.gz",
    executable = False,
    sha256 = "0feb8aeda44654aae6657b41af73a566d63abfb8eb98ee92ab7fc86a0c6a6566",
    urls = ["https://artifactory.local.twitter.com/artifactory/generic-3rdparty-local/bazel/node/linux-x86_64/nodejs.tar.gz"],
)

# 1.19.2 from MDE - https://sourcegraph.twitter.biz/git.twitter.biz/managed_development_environment/-/blob/package/eng/core/nodejs/group.json?L45
http_file(
    name = "yarnpkg_darwin",
    downloaded_file_path = "yarnpkg.tar.gz",
    executable = False,
    sha256 = "5ff867ae23750614b5890201ff757ce6defcd68e07d5ea8f932ca457bfbf76d5",
    urls = ["https://artifactory.local.twitter.com/tools/managed_development_environment/package/MacOSX/yarnpkg-v1.19.2.tar.gz=2020-02-14=5ff867ae23750614b5890201ff757ce6defcd68e07d5ea8f932ca457bfbf76d5"],
)

nodejs_tool(name = "nodejs")

# Packer

# from MDE - https://sourcegraph.twitter.biz/git.twitter.biz/managed_development_environment/-/blob/package/eng/core/packer/group.json?L8
http_file(
    name = "packer_darwin",
    downloaded_file_path = "packer.tar.gz",
    executable = False,
    sha256 = "688cabac91500c8d398699079ffcd9e5ab77a58b9f503d734bf12370526412d3",
    urls = ["https://artifactory.local.twitter.com/tools/managed_development_environment/package/MacOSX/bundle_package_packer.tar.gz=2021-01-11=688cabac91500c8d398699079ffcd9e5ab77a58b9f503d734bf12370526412d3"],
)

# from packer - packer/packer-client-no-pex (v623)
http_file(
    name = "packer_linux",
    downloaded_file_path = "packer",
    executable = True,
    sha256 = "4e8ce5e1d1cc4140b6bbd38d1771fea5b82f9f4e42f80c7a49ab167a71b33aed",
    urls = ["https://artifactory.local.twitter.com/artifactory/generic-3rdparty-local/bazel/packer/linux-x86_64/packer.tar.gz"],
)

packer_tool(name = "packer")

# rules_jvm_export
# https://github.com/twitter/bazel-multiversion
# See https://confluence.twitter.biz/display/DPB/Adding+Bazel+rules
http_archive(
    name = "twitter_rules_jvm_export",
    sha256 = "48cb54c4537840f7c4653a628484b9ed7ee1f6d07f67993a5925848e5d0f9942",
    strip_prefix = "rules_jvm_export",
    url = "https://artifactory.local.twitter.com/artifactory/generic-3rdparty-local/bazel/rules/bazel-multiversion-mirror/bazel-multiversion-mirror_e1a00e3f4b057a7848c15bbac1ff996abbd16d25.zip",
)
