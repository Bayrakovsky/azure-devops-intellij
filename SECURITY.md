# Security Policy

This is a community fork maintained by Stanislav Bayrakovskiy. It is **not** covered by the
Microsoft Security Response Center — please do not report issues in this repository to MSRC.

## Reporting a vulnerability

**Please do not report security vulnerabilities through public GitHub issues.**

Use one of the private channels instead:

- GitHub [Private Vulnerability Reporting](https://github.com/Bayrakovsky/azure-devops-intellij/security/advisories/new), or
- e-mail **stanislav.job@gmail.com** with the subject `[azure-devops-intellij security]`.

Please include as much of the following as you can:

- Affected plugin version and IDE (name + build number).
- Type and impact of the issue, and how an attacker might exploit it.
- Step-by-step instructions or a proof of concept.
- Any suggested fix.

## Response timeline

- Acknowledgment within **72 hours**.
- Confirmation (or a request for more detail) within **7 days**.
- Fixes are prioritized by severity and shipped in the next release; you will be credited in the
  changelog unless you prefer otherwise.

## Scope

**In scope:** the plugin itself, the bundled reactive TFVC backend process, handling of
credentials/personal access tokens by the plugin, and this repository's GitHub Actions workflows.

**Out of scope:** Azure DevOps Services / Team Foundation Server themselves, the Microsoft TEE CLC
(`tf`) client and TFS Java SDK (report those to Microsoft), and the JetBrains platform.

## Known security context

- The plugin stores credentials via the IDE's PasswordSafe / native keychain.
- The plugin launches two local helper processes: the user-configured `tf` executable and the
  bundled backend JVM.
- Release artifacts are plain zips published on GitHub Releases with SHA-256 checksums; the plugin
  is not yet signed for JetBrains Marketplace.
