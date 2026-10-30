# Reverse-analysis tools and evidence boundary

This is a **public, tool-agnostic recovery guide**, not a record of a particular person's machine, an installed MCP server or an APK build dependency. The runtime and app release do not depend on any tool described here.

## Public versus private

- **Public repository:** engineering rules, independently useful compatibility contracts, reproducible *methodology* and minimal source-backed conclusions. Identify third-party components only as necessary; distinguish observed binary facts, inference and unknown runtime behavior.
- **Keep outside Git:** full proprietary SystemUI/Framework/APK/DEX files, decompiled source trees, raw diagnostic logs, user/device identifiers, ROM fingerprints tied to a person, local executable paths, MCP client config, credentials and signing material. Use an access-controlled local/private evidence store as appropriate. Do not copy third-party binaries into this public repository.
- Put temporary analysis output in `/.local-analysis/` (ignored). An ignored path is **not** a security control: inspect `git status --short` and every proposed diff; never use `git add -f` for private material. Removing a tracked file later does not erase public Git history.

## Recover in a new workspace or conversation

1. Re-read `CONTRIBUTING.md`, `docs/development/CURRENT.md` and the relevant `docs/reference/` evidence. Check the live branch, Build identity, PRs and CI; do not reconstruct state from chat memory.
2. Probe **actual** local tools instead of assuming a prior session's installation survived: `java -version`, `jadx --version`, `dexsql --version`, `rea --version`, `adb version` as applicable. Missing commands mean the tool is not ready, not that its repository instructions can execute. Verify the connected MCP client's tools/list when MCP is required.
3. Obtain compatible maintained versions from official sources: [JADX](https://github.com/skylot/jadx), [DexSQL releases](https://github.com/LSPosed/dexsql-release/releases), [REA](https://github.com/morluto/rea), and [Node.js](https://nodejs.org/). Read each release's requirements before installing. Prefer recent stable compatible versions, but pin the **actual binary version and digest used for each investigation** in private evidence. A newer tool never silently changes accepted SystemUI evidence.
4. Register a local MCP server only through the active client's documented mechanism, preserve existing configuration and verify a real handshake and tool list. A CLI can be useful without MCP registration. GitHub repository docs cannot register tools inside a separate ChatGPT conversation or preserve an ephemeral execution environment.
5. Reacquire target binaries only from the authorized device or private evidence source; verify their digests before quoting instruction offsets or matching them to SystemUI/Framework versions. If the exact device/runtime pairing is unknown, say so. Revalidate after ROM or dependency changes.
6. Keep detailed call graphs, Smali excerpts and diagnostic artifacts outside Git. Bring only necessary, lawful, non-sensitive findings into a reviewed public PR; do not present a hypothetical exception, inferred callback or static instruction path as an observed device failure.

## Limits

Tool installation, device instrumentation, app builds and signed Canary/Release distribution are separate actions. This guide authorizes **none** of them by itself. Follow the current contribution/CI rules and the maintainer's explicit APK and signing approvals. Do not add REA/DexSQL to the Android app's production dependencies.
