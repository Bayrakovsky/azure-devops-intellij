# Changelog

All notable changes to the Azure DevOps plugin fork are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/).

## [Unreleased]

_Changes that will land in the next release will be listed here._

## [2.0.5] — 2026-08-03

Maintenance release, no new features. Replaces a first small batch of deprecated platform API
calls with their current equivalents. All replacements are one-to-one with no intended behavior
change; the rest of the deprecated and scheduled-for-removal warnings will be addressed in later
releases.

### Changed

- Replaced deprecated platform APIs: the internal container `HashMap` (now `java.util.HashMap`),
  `ModalityState.NON_MODAL` (now `ModalityState.nonModal()`), the `EmptyIcon` constructor (now
  `EmptyIcon.create()`), stream reading via `StreamUtil` (now `InputStream.readAllBytes()`), and the
  `ActionPlaces.isPopupPlace` place check (now `AnActionEvent.isFromContextMenu()`).
- Reworked the Feedback button. It no longer opens the old Microsoft "Send a Smile / Send a Frown"
  dialog, whose comment was never actually sent anywhere. It is now a small menu that links to the
  community fork's own channels: the plugin's review page on JetBrains Marketplace, GitHub Issues, and
  GitHub Discussions.

### Fixed

- Background UI updates were deferred until the current modal dialog closed, so several dialogs hung on
  a spinner. This was a regression from 2.0.2, where the internal `getAnyModalityState()` call was
  replaced with `defaultModalityState()`; on a background thread the latter resolves to a non-modal
  state and postpones the update. Restored the original behavior through the public `ModalityState.any()`.
  This fixes cloning a TFVC repository (the repository project list hung on "loading projects") and the
  Edit Workspace dialog (every field stuck on "Loading...").
- Adding a working folder before the workspace finished loading no longer throws
  `ArrayIndexOutOfBoundsException`. The mappings table now creates its columns up front instead of only
  after the workspace data arrives.
- Cloning a Git repository ("Clone Repository" → "Azure DevOps Git") no longer fails immediately with an
  "Access from Event Dispatch Thread (EDT) is not allowed" error. The Git executable version check now
  runs on a background thread under a modal progress, as the current platform requires.

## [2.0.4] — 2026-07-24

Maintenance release, no new features. Follows up on 2.0.3: removes the last internal API
usage, replaces a batch of deprecated platform API calls, and drops one more API that is
scheduled for removal. This keeps the plugin working as these APIs are deleted in later IDE
versions.

### Changed

- The reactive TFVC client now finds its bundled backend through the public `PathManager` API
  instead of the internal `PluginManager.getPlugin(PluginId)` API. This removes the last
  Internal API warning, so the plugin verifier reports no internal API usages.
- Replaced deprecated platform APIs across the plugin: application and project service lookups
  (`ServiceManager`), null checks (`ObjectUtils`), project base directory lookups, saving open
  documents before a checkout, and background task progress options. No intended behavior changes.
- The credentials prompt now uses its own user name / password dialog instead of the platform's
  `vcsUtil.AuthDialog`, which is scheduled for removal.

### Fixed

- The credentials prompt now opens on top of the Settings dialog and is modal to it, instead of
  staying hidden until Settings is closed.
- Cancelling "Update credentials..." no longer stores an empty credential. Previously it replaced the
  saved context with one that had no authentication info, which then broke TFVC change detection with a
  null-authentication error.

## [2.0.3] — 2026-07-17

Maintenance release, no new features. JetBrains periodically deletes platform APIs that were
"scheduled for removal", and a plugin that still calls them starts crashing on the next IDE
update. This release replaces almost all such calls (49 flagged usages down to 2), so the
plugin should survive platform 2026.2 and later. The two remaining warnings sit in the
checkout dialog, where the replacement API requires a rewrite of the dialog itself; that is
planned for a separate release.

### Changed

- Replaced platform APIs scheduled for removal throughout the TFVC and Git UI: progress
  dialogs, file chooser fields, action event lookups, the TFVC server tree (popup menu,
  speed search, data context), icons, tree colors, and the check-in environment.
- Proxy settings are now read through the current platform proxy API instead of the removed
  `HttpConfigurable`. One behavior change: proxy authentication is used whenever credentials
  are saved for the proxy host, since the old "Proxy authentication" checkbox no longer
  exists in the IDE settings.
- Project open and close events now come from a startup activity and a `ProjectCloseListener`
  instead of the removed `ProjectManagerListener`. In practice the status bar widget may show
  up a moment later after a project opens.

### Removed

- `BackCompatibleUtils`, a reflection helper for IDEA 2016 and older that had no callers left.

## [2.0.2] — 2026-07-11

Marketplace compliance release: replaces all Internal API and override-only API usages
flagged by the JetBrains Plugin Verifier so the plugin can pass Marketplace moderation.

### Fixed

- **Marketplace rejection (Internal API)** — replaced private platform calls in `IdeaHelper`,
  `ApplicationStartup`, `TFSContentRevision`, `RenameFileDirectory`, `DialogContentMerger`, and
  the build status bar (`StatusBarWidgetFactory` extension point instead of `StatusBar.addWidget`).
- CI `verifyPlugin` now fails on `INTERNAL_API_USAGES` and `OVERRIDE_ONLY_API_USAGES` to catch
  regressions before upload.

## [2.0.1] — 2026-07-11

Maintenance release after Marketplace listing approval. Renames the plugin for Marketplace
uniqueness, enables automated signed publishing, and improves TFVC error handling.

### Added

- **Automated JetBrains Marketplace publishing** in the release workflow: tag-driven releases now
  sign the plugin zip and upload it via `publishPlugin` when repository secrets are configured.
- **`commons-lang` 2.6** dependency for compatibility with future IntelliJ platform versions.

### Changed

- Plugin display name updated to **Azure DevOps Community** to avoid confusion with the unmaintained
  Microsoft plugin on JetBrains Marketplace.

### Fixed

- **Check-in failures with opaque errors** — `CheckinCommand` now surfaces `tf` stderr messages for
  easier debugging.
- **Reactive backend crashes on non-translatable server paths** — `TfsClient` handles
  `ServerPathFormatException` and filters paths the SDK cannot map.
- **Notification titles** — several TFVC error notifications no longer pass a redundant null title.
- **File permission handling** in `TfsFileUtil` for more reliable local workspace operations.

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

[Unreleased]: https://github.com/Bayrakovsky/azure-devops-intellij/compare/v2.0.5...HEAD
[2.0.5]: https://github.com/Bayrakovsky/azure-devops-intellij/releases/tag/v2.0.5
[2.0.4]: https://github.com/Bayrakovsky/azure-devops-intellij/releases/tag/v2.0.4
[2.0.3]: https://github.com/Bayrakovsky/azure-devops-intellij/releases/tag/v2.0.3
[2.0.2]: https://github.com/Bayrakovsky/azure-devops-intellij/releases/tag/v2.0.2
[2.0.1]: https://github.com/Bayrakovsky/azure-devops-intellij/releases/tag/v2.0.1
[2.0.0]: https://github.com/Bayrakovsky/azure-devops-intellij/releases/tag/v2.0.0
