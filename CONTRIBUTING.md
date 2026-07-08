# Contributing

Thanks for helping keep Azure DevOps support alive in JetBrains IDEs!

**Quick links:**
[Report a bug](https://github.com/Bayrakovsky/azure-devops-intellij/issues/new?template=bug_report.md) ·
[Suggest a feature](https://github.com/Bayrakovsky/azure-devops-intellij/issues/new?template=feature_request.md)

## Requirements

- Any recent JDK to launch Gradle — the build itself provisions **JDK 21** automatically via the
  Gradle toolchain mechanism (foojay resolver).
- The Gradle wrapper (pinned to Gradle 9) — always use `./gradlew`.
- The build downloads binary dependencies (Microsoft ALM client jars, the TFS Java SDK) from GitHub
  releases and verifies them against SHA-256 hashes. A hash mismatch deletes the file and fails the
  build — just retry.
- For manual TFVC testing: a real Azure DevOps / TFS account and the
  [TEE CLC](https://github.com/microsoft/team-explorer-everywhere/releases) `tf` client.

## Building

```bash
./gradlew :plugin:buildPlugin   # plugin zip → plugin/build/distributions/
./gradlew :plugin:runIde        # sandbox IDE with the plugin (and reactive backend) installed
./gradlew verifyPlugin          # JetBrains Plugin Verifier against Rider 2026.1
```

## Project layout

| Module | What it is |
|---|---|
| `plugin` | The IntelliJ plugin (Java). Non-standard source layout: `src`, `test`, `resources`. |
| `client:protocol` | Shared rd protocol model (`TfsModel.kt`); `rdgen` generates bindings into backend/connector. Generated sources are build artifacts — never edit them by hand. |
| `client:backend` | Standalone Kotlin app hosting the TFS Java SDK in a separate JVM; shipped inside the plugin zip. |
| `client:backend:tfs-sdk` | Downloads and unpacks the Team Explorer Everywhere TFS SDK. |
| `client:connector` | Plugin-side rd connection library. |

TFVC operations run through one of two backends: the legacy `tf` CLI wrappers
(`plugin/external/commands/`) or the reactive client (`plugin/external/reactive/` → `client:*`
modules over the JetBrains rd protocol).

## Code style

Checkstyle runs as part of the build and **fails it on violations**:

- Every Java source file must start with the MIT license header:
  ```
  // Copyright (c) Microsoft. All rights reserved.
  // Licensed under the MIT license. See License.txt in the project root.
  ```
  (Kept in the original Microsoft form — the code is MIT-licensed by Microsoft and this fork
  preserves the attribution.)
- 4-space indent, no tabs.
- No wildcard imports.

## Tests — known gap

The unit test sources still use PowerMock, which is incompatible with JDK 21, and are currently
**not compiled or run** (neither locally nor in CI). Migrating them to Mockito 5 +
`mockito-inline` is a welcome contribution. Integration tests (`L2Tests`) additionally require a
real Azure DevOps organization and `MSVSTS_INTELLIJ_*` environment variables (see the upstream
README history for the full list).

## Pull requests

Before opening a PR, please check:

- [ ] `./gradlew :plugin:buildPlugin` is green (this includes Checkstyle).
- [ ] New Java files carry the license header.
- [ ] The change is described in `CHANGELOG.md` under `[Unreleased]` if user-visible.
- [ ] For TFVC behavior changes: describe how you verified them against a real workspace
      (IDE + TEE CLC version).

## Release process (maintainer)

1. Update `CHANGELOG.md`: move `[Unreleased]` content into a new version section.
2. Push a tag `vX.Y.Z` — the [release workflow](.github/workflows/release.yml) builds the plugin
   with that version, runs the Plugin Verifier, and publishes a GitHub Release with the zip and
   SHA-256 checksums.
