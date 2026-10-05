# Aspect Model Editor backend

## Table of Contents

- [Introduction](#introduction)
- [Getting help](#getting-help)
- [Setup](#setup)
- [Build and run](#build-and-run)
- [Release](#release)
- [Further documentation](#further-documentation)
- [License](#license)

## Introduction

This project is used as the backend for the Aspect Model Editor and interacts with the [ESMF-SDK](https://github.com/eclipse-esmf/esmf-sdk) to
create [SAMM](https://github.com/eclipse-esmf/esmf-semantic-aspect-meta-model) specific Aspect Models.

## Getting help

Are you having trouble with Aspect Model Editor backend? We want to help!

* Check the [developer documentation](https://eclipse-esmf.github.io)
* Check the SAMM [specification](https://eclipse-esmf.github.io/samm-specification/2.0.0/index.html)
* Having issues with the Aspect Model Editor backend? Open a [GitHub issue](https://github.com/eclipse-esmf/esmf-aspect-model-editor-backend/issues).

## Setup

* Download and install [JDK 25](https://github.com/adoptium/temurin25-binaries/releases)
* Download and Install [Maven](https://maven.apache.org/download.cgi)
* Environment Settings
    * Add "{JAVA_HOME}/bin" to PATH
    * Add "{MAVEN_HOME}/bin" to PATH
* Configure Maven Settings

> Note : Configure your IDE to use the new JDK and Maven installation.

## Build and run

```
mvn clean package
mvn exec:java -pl aspect-model-editor-runtime
```

## Release

The workflow `.github/workflows/tagged_release.yml` (manually started with the release version) builds the backend as
[jpackage](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jpackage.html) app image with its own Java runtime
and publishes it as GitHub release. The desktop app of the
[Aspect Model Editor](https://github.com/eclipse-esmf/esmf-aspect-model-editor) bundles these app images, so the backend
release must exist before the editor release with the same version is created.

1. `prepare` creates the branch `<major>.<minor>.x`, the tag `v<version>` and a draft release.
   Release candidates (e.g. `2.3.0-rc1`) become a pre-release.
2. `build` creates the app image on every platform and uploads it to the draft release:

   | Runner           | Release asset                                                        |
   |------------------|----------------------------------------------------------------------|
   | `ubuntu-latest`  | `ame-backend-v<version>-linux.tar.gz`                                |
   | `macos-15-intel` | `ame-backend-v<version>-mac-x64.zip` (Intel)                         |
   | `macos-latest`   | `ame-backend-v<version>-mac-arm64.zip` (Apple silicon)               |
   | `windows-latest` | `ame-backend-v<version>-win` (workflow artifact only, signed and uploaded by Jenkins) |

3. `publish` publishes the release and triggers the Jenkins job which signs the Windows app image.
   The Jenkins job is only triggered in the repository `eclipse-esmf`, so the workflow can be tested in a fork.

The Java runtime in the app image matches the processor architecture, therefore macOS is built separately for Intel and
Apple silicon.

## Further documentation

* [Workspace deletion and missing references](docs/workspace-and-references.adoc): reference-aware deletion of files,
  namespaces and the workspace, `ignoreMissing` and the `unresolvedElements` error responses, and how to run the Bruno tests

We are always looking forward to your contributions. For more details on how to contribute just take a look at the
[contribution guidelines](CONTRIBUTING.md). Please create an issue first before opening a pull request.

## License

SPDX-License-Identifier: MPL-2.0

This program and the accompanying materials are made available under the terms of the
[Mozilla Public License, v. 2.0](LICENSE).

The [Notice file](NOTICE.md) details contained third party materials.
