# LSPosed 7930 Compatibility Check

This reference separates a **static examination** of the supplied `LSPosed-v2.2.1-7930-release.zip` from **bounded 7930 runtime evidence** in earlier maintainer-provided Guiyuan diagnostic logs. The archive checks do not prove publisher authenticity or exact installed-framework byte identity. The supplied 7930 Java framework has now been independently compared with the byte-verified 7927 Java framework; for the underlying API 102 callback path, see [LSPosed 7927 Hot Reload Contract](lsposed-7927-hot-reload.md).

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

### Direct 7927–7930 Java framework parity check (2026-10-30)

The original supplied 7927 and 7930 release ZIPs were re-extracted (ZIP method 95, XZ) and the actual `framework.dex` SHA-256 hashes matched the table above and the 7927 reference: `455155d12173462d51bea31465fe1278fcf91da4ef1d5427dc9beec0c07ccdb4` versus `e84ca1e7e35f7f268c50ff49b1beaff162b0475b9c12f50fa3654bf85b625269`. Both DEX inputs were independently disassembled with the original `baksmali-3.0.10-fat-release.jar`, producing **181 class definitions each**. All class paths, instruction sequences and method bodies match after excluding the changed R8 `.source` map ID and the single expected version constant in `y0.smali` (`0x1ef7` = 7927 to `0x1efa` = 7930). No other instruction-level changes were found in the extracted Java framework.

In particular, `y0` retains the same `onHotReloading` and `onHotReloaded` calls and their surrounding class-loader handoff logic as the independently examined 7927 binary. Thus, the 7927 normal-path API 102 **Java callback ordering also applies to the supplied 7930 Java DEX**, with stronger evidence than string presence or user logs alone. This comparison does **not** show equivalence of `libframework.so`, Zygisk injection/wrapper/daemon behavior, the exact files actually installed on the user's device, or native exceptional scheduling/cleanup. No Guiyuan runtime change or new device acceptance follows from this source check.

## Reported framework changes

The maintainer supplied the following release notes (not independently proven by binary diff):

- Support injection hook for Android 10+;
- Support updating wrapper on soft reboot;
- Fix late load issues;
- Fix async-safe issues;
- Use injection on Android 10+ to avoid mount-related kernel timing side-channel detection.

Injection versus mount-based delivery is an **LSPosed/Zygisk framework responsibility**. Guiyuan must not add mount heuristics, custom process injection, hidden bypasses or detection-evasion code to emulate it.

## Observed target-device runtime (2026-10-20–23)

These observations are from maintainer-provided diagnostics for Xiaomi 15 Pro / Android 17 / SystemUI `17.03.260226.r`, **not** conclusions drawn only from the supplied archive:

- **Settings load:** B1136 (2026-10-20 22:54 +08) records `module.loaded` in `com.android.settings:remote` with API 102, `framework=LSPosed`, `frameworkVersion=2.2.1` and `frameworkVersionCode=7930`.
- **SystemUI V10 normal handoff:** B1181 (2026-10-21 15:53 +08), B1186 (17:54) and B1187 (18:56) independently record `module.reloaded` reporting 7930, `compatibility.revalidated statusHost=available`, `statusHostHook hook.replace`, `hotReload.generationHandoff mode=retired-before-reload` and `hotReload.complete state=ready restartScope=false staleHooks=32 statusHostHook=replaced`. The reported SystemUI PID and restored host identity are consistent across those three captures.
- **Consecutive same-Build reloads:** Two B1232 Canary diagnostic reports on 2026-10-23 show completed Hot Reload at 15:35:14.086 and 15:35:25.656 (+08:00) within the same SystemUI PID `16822`, retaining host identity `146151917` across distinct module diagnostic sessions. Each records `hotReload.generationHandoff mode=retired-before-reload`, `hotReload.complete state=ready restartScope=false staleHooks=32 statusHostHook=replaced`, and Home combined presentation restored. This is two consecutive successful normal-path reloads of the same reported Build, not a byte-for-byte installed APK verification, memory-leak proof or failure-path stress test.
- **Presentation boundary:** B1181 records the Home presentation restored as combined. The B1186/B1187 reports also include native-systemui fallback with the feature disabled. `controlCenterFakePrearm=restored-laid-out-native-layout-pending` must not be interpreted as proof of finished CC native layout or frame-atomic drawing.

This is positive evidence for repeated **normal-path** API 102 V10 module reloads and Settings loading on the observed 7930 installation. The B1232 captures add two consecutive same-Build normal-path reloads; the logs do **not** prove a cryptographically identical installed APK, a clean full reboot, soft reboot/wrapper upgrade, injection internals, simulated failure paths, ClassLoader disposal or absence of every listener leak. They also do not cryptographically tie the installed LSPosed files to the separately inspected ZIP.

## Guiyuan boundary and remaining validation

Guiyuan's `GyModule` uses API 102 `onHotReloading` to retire its old owner synchronously on the SystemUI main thread and only reuses neutral transfer state. `onHotReloaded` validates the transferred host and releases old Hook handles on failure. Direct normalized DEX comparison confirms that the supplied 7930 `framework.dex` retains the 7927 Java normal-path callback ordering. The 7930 **device logs** separately confirm completed normal-path handoffs; neither source establishes identical native failure behavior, installed-binary identity, wrapper identity or asynchronous completion guarantees.

**Do not change the core ownership/Hot Reload implementation based only on the changelog.** The observed 7930 normal-path handoffs narrow the remaining gates to:

1. After full reboot, the SystemUI module initializes once, the settings listener is connected, and Home/Keyguard/AOD/Control Center fall back natively if a target is missing.
2. After a *supported* soft reboot, scope and wrapper registration are correct and SystemUI still loads the module. Do not assume a soft reboot is equivalent to API 102 Hot Reload.
3. Late-load/restart of SystemUI and companion Settings do not duplicate owners, reserve stale widths, or retain old generation UI.
4. Monitor framework and module logs for early injection, ClassLoader and async callback errors before changing the API boundary. The B1232 consecutive normal-path reloads are not failure-path or long-run leak testing.

On failure, preserve native SystemUI and require an explicit process restart; do not compensate with a timer. These tests are the acceptance gate for a 7930 compatibility claim.
