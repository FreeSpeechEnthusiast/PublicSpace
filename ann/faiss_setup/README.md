## Overview

This folder contains scripts to build centos7 and macos (x86_64/arm64) faiss binaries. Most likely you don't need to run these, unless you're working on faiss integration itself.

### Code generation structure

Here is directory tree related to jni/swig bridge

```
.
├── faiss_setup # This folder contains scripts to build static native library for supported platforms
│   └── jvm
│       ├── centos7 # Contains swigfaiss.swig
│       └── macos
└── src
    ├── main
    │   ├── java
    │   │   └── com
    │   │       └── twitter
    │   │           └── ann
    │   │               ├── faiss
    │   │               │   ├── selftest
    │   │               │   └── swig
    │   │               │       └── resources # Contains native libraries for local selftest target
```

In the tree above, `swigfaiss.swig` is a swig template to generate JNI bindings and native interface file. This template is derived from original faiss version, but changed to support generating jvm code. [Original template](https://github.com/facebookresearch/faiss/blob/main/faiss/python/swigfaiss.swig)

### Development workflow

Requirements: arm based mac if you want to upload new package to artifactory. Docker installed.

- Make a change to any of the source files
- Run build.sh, iterate until self test pass
- Note current version at 3rdparty/jvm/com/twitter/ann/faiss
- Upload local jar to artifactory, as described by script. Specify group/package name from previous step. Put higher version than already exists.
- Update 3rdparty/jvm/com/twitter/ann/faiss to include updated version
- Continue script to run self test with artifact from artifactory
