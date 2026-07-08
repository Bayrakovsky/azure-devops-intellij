# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

An IntelliJ Platform plugin (platform **2026.1+**, since-build 261; Rider is the primary target IDE) for working with **Git** and **TFVC** (Team Foundation Version Control) repositories hosted on Azure DevOps Services and Team Foundation Server 2015+. Supported on Linux, macOS, and Windows.

The upstream Microsoft project is unmaintained; this is a revived fork (plugin id `io.github.bayrakovskiy.azuredevops`, maintainer Stanislav Bayrakovskiy).

## Prerequisites

- No JDK setup needed: the build uses Gradle toolchains (foojay resolver) and auto-provisions **JDK 21**.
- The build downloads binary dependencies (Microsoft ALM client jars, the TFS Java SDK) from GitHub releases and verifies them against SHA-256 hashes baked into `dependencies.gradle` and `client/backend/tfs-sdk/build.gradle`. A hash mismatch deletes the file and fails the build — just retry.

## Common commands

Run from the repository root; the Gradle wrapper pins **Gradle 9.0.0**, and the build uses the **IntelliJ Platform Gradle Plugin 2.x** against `intellijIdea(2026.1.4)`.

- `./gradlew :plugin:buildPlugin` — build the plugin; output zip (`azure-devops-<version>.zip`, reactive backend bundled inside) lands in `plugin/build/distributions/`. The version comes from `buildNumber` in `gradle.properties`; override with `-PbuildNumber=X.Y.Z` (the release workflow does this from the git tag).
- `./gradlew :plugin:runIde` — launch a sandbox IDE with the plugin (and reactive backend) installed.
- `./gradlew verifyPlugin` — JetBrains Plugin Verifier (uses the local Rider.app when present, otherwise downloads Rider).
- **Unit tests are NOT currently runnable**: the test sources still use PowerMock (incompatible with JDK 21) and do not compile. Avoid `check`/`test` tasks until the tests are migrated to Mockito 5; `buildPlugin` does not compile tests.

### Quality gates
- **Checkstyle** (`config/checkstyle/custom-rules.xml`) runs on the main projects and **fails the build on violations**. It enforces the MIT license header — every Java source file must start with:
  ```
  // Copyright (c) Microsoft. All rights reserved.
  // Licensed under the MIT license. See License.txt in the project root.
  ```
- Style rules the checks enforce: 4-space indent (no tabs), no wildcard imports.
- **PMD** (`config/pmd/custom-pmd-rules.xml`) runs but does not fail the build (`ignoreFailures = true`).

### Integration tests (L2Tests)
Disabled unless `MSVSTS_INTELLIJ_RUN_L2_TESTS=true`. They require a real Azure DevOps organization plus a set of `MSVSTS_INTELLIJ_*` environment variables (repo URLs, PAT, team project, `tf` executable path — see `L2Tests/test/com/microsoft/alm/L2/L2Test.java` for the full list). The reactive backend tests in `:client:backend` gate on the same variable.

## Module architecture

Gradle modules (`settings.gradle`): `plugin`, `plugin:test-utils`, `L2Tests`, `client:protocol`, `client:backend`, `client:backend:tfs-sdk`, `client:connector`.

### `plugin` — the IntelliJ plugin (Java)
Sources under `plugin/src/com/microsoft/alm/`. Note the non-standard Gradle source layout used across the main projects: `src`, `test`, `resources`, `test-resources` (not `src/main/java`). Entry point / extension registrations are in `plugin/resources/META-INF/plugin.xml`.

Internal split:
- `plugin/context`, `plugin/authentication`, `plugin/operations`, `plugin/services` — platform-independent core: server connections, auth, REST/SOAP context, background operations. Depends on the Microsoft ALM `com.microsoft.alm.client.*` libraries.
- `plugin/idea/common` — shared IDE UI, settings, status bar, starters (URL/command-line handlers).
- `plugin/idea/git` — Git-on-Azure-DevOps integration, built on top of the bundled `git4idea` plugin.
- `plugin/idea/tfvc` — TFVC integration (`core`, `ui`, `actions`, `tfignore`, `extensions`).
- `plugin/external` — drives the **legacy `tf` command-line client**. `external/commands` wraps individual `tf` operations (Add, Checkin, Merge, Status, …) as `Command` subclasses; `external/tools` locates the executable; `external/reactive` bridges to the newer reactive client (below).

### TFVC has two backends
TFVC operations can go through either:
1. The legacy external `tf` CLI (`plugin/external/commands/*`), or
2. The **reactive client** — an out-of-process Kotlin service (see `client` modules) that hosts the official TFS Java SDK.

`plugin/external/reactive/` (e.g. `ReactiveTfvcClientHost`, `ReactiveClientConnection` consumers) is the plugin-side bridge to backend #2.

### `client` — reactive TFVC client (Kotlin)
The plugin cannot load the TFS Java SDK directly in-process, so the SDK runs in a separate JVM and communicates with the plugin over the JetBrains **Reactive Distributed (rd) framework**.

- **`client:protocol`** — defines the shared protocol model in `src/main/kotlin/model/tfs/TfsModel.kt`. The `rdgen` task generates matching Kotlin bindings into `client/backend/.../generated` and `client/connector/.../generated`. **These generated sources are build artifacts** — regenerate via rdgen rather than editing by hand; `clean` deletes them. Both `backend` and `connector` `compileKotlin` depend on `:client:protocol:rdgen`.
- **`client:backend`** — a standalone Kotlin application (`com.microsoft.tfs.MainKt`) that wraps the TFS SDK (`TfsClient.kt`, `sdk/`, `watcher/`). Built with the `application` plugin; `installDist` output is copied into the plugin sandbox by the `prepareBackendSandbox` task so `runIde`/`buildPlugin` ship the backend inside the plugin.
- **`client:backend:tfs-sdk`** — downloads and unpacks the Team Explorer Everywhere TFS SDK jar (version pinned in its `build.gradle`), exposed as an artifact the backend depends on.
- **`client:connector`** — the client-side rd connection (`ReactiveClientConnection.kt`) library, a `compile` dependency of `plugin`.

## Dependency notes / gotchas

- Several dependencies are pulled ahead of IntelliJ's bundled copies via a custom `priorityTestCompile` configuration in `build.gradle` (JUnit 4.12, a specific `jackson-databind`). This exists because IDEA's classloader otherwise supplies conflicting versions that break tests — keep this in mind before "upgrading" or reordering those.
- The plugin depends on IntelliJ's bundled `git4idea` plugin (and `java` plugin for L2Tests).
- Binary ALM/SDK jars come from `flatDir` after being downloaded by `dependencies.gradle` / `tfs-sdk` — they are not on Maven Central.
