# Changelog

All notable changes to the Azure DevOps plugin fork are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/).

## [Unreleased]

_Changes that will land in the next release will be listed here._

## [2.0.0] — 2026-07-08

First release of the revived community fork. The upstream Microsoft plugin (last targeting
platform 2021.2) is unmaintained; this release brings it back to life on the current platform,
with **TFVC in Rider** as the primary target.

### Added

- New plugin identity: `io.github.bayrakovskiy.azuredevops` (vendor Stanislav Bayrakovskiy). The
  fork installs independently of the original Microsoft plugin.
- GitHub Actions CI and tag-driven release automation (`v*` tags publish a GitHub Release with the
  plugin zip).
- New plugin icon (light + dark variants): an azure diamond badge with a branch glyph, echoing the
  palette of the original VSTS mark.

### Changed

- **Migrated to the IntelliJ Platform 2026.1** (`since-build 261`); verified compatible with
  Rider 2026.1.4 by the JetBrains Plugin Verifier. Roughly a hundred API-drift fixes across the
  codebase: notification, VFS-listener, tree-model, fetch, and starter APIs, among others.
- **Build modernized**: Gradle 9, IntelliJ Platform Gradle Plugin 2.x, JDK 21 (auto-provisioned via
  Gradle toolchains), Kotlin 2.1.
- **rd protocol upgraded to 2026.1.3** — the out-of-process reactive TFVC backend (hosting the TFS
  Java SDK) now runs on JDK 21 and speaks the current JetBrains rd protocol.
- `plugin.xml` modernized: application components replaced with an `AppLifecycleListener`,
  declarative `.tfignore` file type, declarative notification group.

### Fixed

- **Every reactive TFVC operation (rollback, check-in preparation, change detection) hung
  forever.** The rd 2026.1 generated `TfsModel.create()` no longer binds the model to the protocol,
  so the plugin↔backend handshake never completed. Both sides now use the `protocol.tfsModel`
  extension, which performs the actual binding.
- **`tf` CLI commands failed with "WARNING: A restricted method in java.lang.System has been
  called".** JBR 25 / JDK 24+ print native-access and `sun.misc.Unsafe` warnings to stderr, and the
  plugin treats any stderr output as a command failure. The TEE CLC JVM now runs with
  `--enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow`, and JVM `WARNING:`
  lines are filtered out of the error stream.
- **Three "Class initialization must not depend on services" errors on first TFVC operation.**
  `ServerContextManager` restored saved server contexts (touching PasswordSafe) inside its
  singleton's static initializer, which platform 2026.1 forbids; restoration is now lazy.
- **IDE freeze when opening a TFVC project.** Root validation blocked the VCS mapping thread on a
  cold backend; `TfvcRootChecker` now answers from its cache ("valid if unknown") without calling
  the backend, while change detection still waits for real workspace mappings.
- **Deadlock when the TEE CLC EULA had not been accepted.** The EULA dialog was shown with
  `invokeAndWait` from a thread holding a read lock; it is now scheduled with `invokeLater`.

[Unreleased]: https://github.com/Bayrakovsky/azure-devops-intellij/compare/v2.0.0...HEAD
[2.0.0]: https://github.com/Bayrakovsky/azure-devops-intellij/releases/tag/v2.0.0
