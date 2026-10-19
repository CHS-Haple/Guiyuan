# LSPosed 7930 Compatibility Check

This is a **static examination** of the user-supplied `LSPosed-v2.2.1-7930-release.zip` against Guiyuan's Modern Xposed API 102 contract. It does not prove publisher authenticity or on-device behavior. For the separately verified old-generation → new-generation callback ordering in 7927, see [LSPosed 7927 Hot Reload Contract](lsposed-7927-hot-reload.md).

## Supplied archive

| Evidence | Value |
| --- | --- |
| ZIP SHA-256 | `5623e5a17e6f795061307391146abb21db55984c7d13f91cbcdd2431209ce420` |
| `module.prop` | `id=zygisk_lsposed`, `version=v2.2.1 (7930)`, `versionCode=7930`, Android compatibility in bundled README: 9–17, Zygisk required |
| `framework.dex` SHA-256 | `e84ca1e7e35f7f268c50ff49b1beaff162b0475b9c12f50fa3654bf85b625269` |
| ARM64 `libframework.so` SHA-256 | `967e368f068ac74e75020a742f43f71503d2e2ef66f9cfef451ad1e7c7580957` |
| `daemon.apk` SHA-256 | `3eca20d99b521c222b85f2272fab493052d800c78e3afebac729bfcbc9c9f7ee` |
| Integrity | 41 payload files matched both their ZIP CRC and matching embedded SHA-256 sidecars. Some ZIP entries use XZ compression method 95; this was decoded without executing installation scripts. |
| Java API surface | `framework.dex` still contains `API_102`, `onHotReloading`, `onHotReloaded`, `onModuleLoaded` and `onPackageReady` strings. This demonstrates the entrypoint names survive, not that their invocation order or failure behavior is unchanged. |

These are self-consistency checks, **not** a release signature or independent download-origin verification. The archive itself does not contain a source diff establishing all of its changes relative to 7927. Do not deploy an unverified package merely because its internal sidecars match.

## Reported framework changes

The maintainer supplied the following release notes (not independently proven by binary diff):

- Support injection hook for Android 10+;
- Support updating wrapper on soft reboot;
- Fix late load issues;
- Fix async-safe issues;
- Use injection on Android 10+ to avoid mount-related kernel timing side-channel detection.

Injection versus mount-based delivery is an **LSPosed/Zygisk framework responsibility**. Guiyuan must not add mount heuristics, custom process injection, hidden bypasses or detection-evasion code to emulate it.

## Guiyuan boundary and required on-device validation

Guiyuan's `GyModule` uses API 102 `onHotReloading` to retire its old owner synchronously on the SystemUI main thread and only reuses neutral transfer state. `onHotReloaded` validates the transferred host and releases old Hook handles on failure. This logic was verified against **7927**; the 7930 archive strings alone cannot establish an identical callback order, wrapper identity, or async completion guarantee.

**Do not change the core ownership/Hot Reload implementation based only on the changelog.** On an actual 7930 installation, verify separately:

1. After full reboot, the SystemUI module initializes once, the settings listener is connected, and Home/Keyguard/AOD/Control Center fall back natively if a target is missing.
2. After two successive API 102 Hot Reload operations, stale listeners and Hooks are gone; the correct SystemUI host and native icon restoration survive.
3. After a *supported* soft reboot, scope and wrapper registration are correct and SystemUI still loads the module. Do not assume a soft reboot is equivalent to API 102 Hot Reload.
4. Late-load/restart of SystemUI and companion Settings do not duplicate owners, reserve stale widths, or retain old generation UI.
5. Monitor framework and module logs for early injection, ClassLoader and async callback errors before changing the API boundary.

On failure, preserve native SystemUI and require an explicit process restart; do not compensate with a timer. These tests are the acceptance gate for a 7930 compatibility claim.
