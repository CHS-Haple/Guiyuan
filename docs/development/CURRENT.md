# Current Development State

## Baseline

- Latest `dev` integration: Guiyuan 0.6.1 / Build 956 (`20261013-956`) at commit [`4c24d1c`](https://github.com/CHS-Haple/Guiyuan/commit/4c24d1cbec59a32fc73690333be306e64a7a1f04), after squash [#455](https://github.com/CHS-Haple/Guiyuan/pull/455). Mobile-data enable/disable now refreshes the current presentation through a subscription-scoped native Telephony callback, even when the default network does not change. VPN with explicitly disabled mobile data no longer retains a 4G/5G center label; separate SIM signal strength remains available. JVM/Debug Runtime CI, signed Canary and source checks passed. The maintainer requested closure after receiving the integrated B956 APK; focused acceptance should not be generalized to untested VPN/dual-SIM/device variants.
- Previous `dev` integration: Build 954 (`20261013-954`) via [#454](https://github.com/CHS-Haple/Guiyuan/pull/454), explicitly accepted on device. The CC battery ring keeps its own retract motion without a native final battery endpoint. The battery number moves only to a valid visible native number representation, otherwise using the established no-target exit. Build 956 inherits both fixes; no experimental Sandbox parasite feature was merged.
- Earlier `dev` integration: Build 949 (`20261013-949`) at [`55679bc`](https://github.com/CHS-Haple/Guiyuan/commit/55679bcf45a64a11a5f83342897b9bc9e24cd238). Home runtime status uses a shared MIUIX progress ring with bidirectional mark transitions, while decorative progress is excluded from accessibility metrics; B949 itself had no separate device acceptance.
- The Sandbox parasite experiment [#452](https://github.com/CHS-Haple/Guiyuan/pull/452) was closed without merge. Arbitrary simulated participants would require additional native endpoint/layout ownership for trustworthy full Control Center transitions. The standalone Preview Sandbox remains; independently actionable normal-mode bugs were handled by #454 and #455.
- Build 945 (`20261013-945`) after squash [#449](https://github.com/CHS-Haple/Guiyuan/pull/449). The static-scope page now shows authorization independently of host process activity: `已授权` uses the MIUIX primary tone, while `未授权`/missing/unavailable remain neutral with a 7% fill. An explicitly failed target is labeled as a previous framework observation. The 5 dp tag corners and 20 dp minimum height align with Diagnostics label geometry. Focused JVM tests, Runtime CI and signed Canary checks passed; the maintainer approved integration. This does **not** claim fresh B945 device coverage. At that point, the last explicitly device-accepted integration was **B943** (2026-10-13). B943 integrated original battery color UI and the native MIUIX color editor via [#445](https://github.com/CHS-Haple/Guiyuan/pull/445); its hue palette retains distinct 0° and 360° cursor positions for the same color. Stable `main` remains B921.
- Earlier stable device baseline: Guiyuan 0.6.1 / Build 921 (`20261012-921`), based on maintainer-accepted B920 (`20261012-920`) and squash integration #428. This adds the optional Settings shortcut and About animation, extends the existing SystemUI restart to selectable authorized scopes and source-authoritative scope display without new SystemUI ownership or speculative AOD changes. Internal (`-internal`) and Canary share runtime behavior; Internal gates the project link only. Previous accepted B875–B885 runtime and diagnostics improvements remain integrated. This does not constitute broader device/system compatibility certification.
- B881 liquid navigation updates captured width and layout direction while retaining its drag motion owner for the same tab count. The maintainer confirms focused B881 device acceptance; unusual resize-mid-drag and forced-RTL paths are not claimed independently verified.
- B882 App preference mirroring skips only an already successfully committed snapshot on the same Xposed service; binding/rebinding always sends the full state and failed commits stay eligible for a later attempt. This removes redundant remote commits from multi-key local preference notifications without changing the remote keys or synchronous commit contract. Focused B882 device acceptance is not yet claimed.
- B883 diagnostic export separates distinct Hot Reload sessions sharing a SystemUI PID, preferring actual module start events when present and excluding only explicitly tagged stale-generation lines. Untagged legacy lines retain their former behavior; the App-only parser has focused JVM regressions, not a claimed device or AOD repair.
- B884 keeps new-generation Hot Reload failure diagnostics visible even when hook takeover fails before `module.reloaded`, without allowing a delayed error from an already observed old session to override a successful new start. The selection behavior is covered by focused JVM tests; no SystemUI hooks or scene ownership changed.
- Build 885 fixes the retained Keyguard/AOD host alpha-observer reattachment lifecycle (#420). The maintainer reports B885 working on device after a green PR Runtime CI and signed Canary build; rare AOD edge cases remain observation-only.
- AOD observation items [#348](https://github.com/CHS-Haple/Guiyuan/issues/348) (native peer gap) and [#375](https://github.com/CHS-Haple/Guiyuan/issues/375) (battery-only flash) are closed as completed on the accepted B875/B885 ancestry, with their focused source and device evidence recorded in each issue. [#359](https://github.com/CHS-Haple/Guiyuan/issues/359) (sporadic Home→native AOD lag) is closed as not planned due to no current actionable recurrence, **not** as an exact-cause repair. None of these closures claims exhaustive rapid-reversal or charging-boundary verification; reopen upon fresh reproducible evidence.
- Control Center callback failure diagnostics distinguish fallback requests from incomplete cleanup; Wi-Fi seed diagnostics preserve source and resource details for both ready and unavailable states.
- Stable `main` is 0.6.1 / Build 921 after the Full-validated promotion #432. `dev` continues as the separate integration branch; assess its actual diff before any later stable promotion. Device acceptance remains based on B920; the subsequent B921 version/build identity does not claim new runtime coverage.
- Charging visibility follows the authoritative SystemUI battery state; charging-glyph identity no longer acts as a second charging-state source.
- Mobile runtime state retains semantic signal strength without persisting native signal/VoLTE/VoWiFi resource IDs. Native icon event deduplication includes SIM identity, and Hot Reload restoration preserves attached roots plus newly observed Wi-Fi/mobile state while retaining fallback for missing transfer data.
- Liquid Glass selected-state tint follows the active MIUIX theme color, including Monet dynamic color.
- Liquid Glass has its own saved navigation-content choice, defaulting to icons and text; Standard, Blur and Glass share their original choice, defaulting to icons only. Explicit saved selections are preserved.
- Home runtime status keeps its existing state semantics and color transitions, with a scoped Xposed enable prompt when supported and one MIUIX loading indicator while checking/reloading.
- Charging lightning sampling and Diagnostics entry motion remain integrated in `dev`; current tested Home/Keyguard/refresh paths have no reported regression.

- Build and CI tooling: Gradle 9.8.1, Kotlin Compose/Serialization compiler plugins 2.4.21 and pinned `actions/upload-artifact` 7.0.2. MIUIX is pinned to the published `0.9.4-8b85f965-SNAPSHOT` revision; upstream changes since `0657575a` are build/dependency-only.

## Accepted runtime contract

- Home has its own projected presentation owner.
- Keyguard and AOD share one host-scoped family presentation owner; same-host scene changes retarget that owner instead of creating duplicate mutable owners.
- AOD is never a Control Center transition source.
- Notification Shade and the fully expanded Control Center remain native.
- QS_FAKE is the bounded Control Center transition bridge. Its width lease waits for native layout agreement and resumes on native layout; a hidden prearm lease may yield to an authoritative native reset. A 5GA suffix only morphs toward a verified native text target; an unknown target preserves fail-native behavior.
- SystemUI owns native scene state, layout, appearance, alpha, visibility, translation and motion timing.
- Guiyuan owns only its renderer plus the minimum verified suppression, masking, reservation and transition-projection state.
- Any ownership, host, geometry, or compatibility ambiguity triggers fail-native behavior for the smallest affected surface.
- Diagnostic/build-channel flags are observational only and must not alter functional runtime control flow.

## Maintainability baseline

The bounded evidence-based maintainability phase [#328](https://github.com/CHS-Haple/Guiyuan/issues/328) is closed after repository-wide screening, focused call-chain and lifecycle reviews, and reviewed cleanups through #422. This is not proof of a completed line-by-line review or full HyperOS runtime verification. The accepted changes from [#329](https://github.com/CHS-Haple/Guiyuan/pull/329) are integrated; its redundant PR was closed. Continue scoped maintainability review for each non-trivial change, rather than treating #328 as an endless repository-wide blocker.

Current rules:
- prefer concise, scope-appropriate names over modifier/suffix stacking;
- keep abstractions only when they carry real ownership, lifecycle, compatibility, reuse or policy value;
- group crowded runtime code by stable problem domain, not by suffix/technical role;
- keep source paths aligned with Kotlin packages and move cohesive areas incrementally;
- keep cross-domain orchestration in the root package when a narrower owner would be artificial;
- model one mutually exclusive lifecycle as one state rather than a wall of invalid boolean combinations;
- keep genuinely independent facts independent;
- diagnostics report observed facts rather than invented proof fields, pass rates or symmetry-only events;
- preserve compatibility identities when they are externally consumed.

The first accepted package split is now complete: `xposed.battery`, `xposed.prefs`, and `xposed.network` contain the clearest cohesive domains. Do not continue splitting the remaining root `xposed` code merely for directory symmetry; create another subpackage only when a stable maintenance boundary is evident.

The focused large-runtime-class source audit [#433](https://github.com/CHS-Haple/Guiyuan/issues/433) is complete on `dev`: duplicated native View lookup, diagnostic callbacks, clip restoration/synchronization and battery-number witness construction were consolidated without moving host/session ownership. The large coordination, drawing and native-presentation methods remain intentionally cohesive where splitting would duplicate lifecycle, state or geometry authority. This source/CI audit is not a full SystemUI device or performance certification.

## Guardrails

- Root cause before workaround.
- One writer per mutable surface.
- No polling, delay/retry repair loops, guessed thresholds or fixed device geometry.
- Do not take over native translation, alpha, visibility or final Control Center appearance.
- Do not weaken exact restoration or fail-native behavior.
- Change external versionName only on explicit promotion; the current stable checkpoint is 0.6.1. Increment Build ID and Android versionCode only for a materially different APK (behavior, UI/assets or compatibility), not for docs, non-behavioral refactors, or repeat CI/builds. The exact Git SHA distinguishes source revisions within a Build.

## Next

Continue issue-driven development from the accepted 0.6.1 runtime, with targeted maintainability checks on each changed call chain. If a new native compatibility, Hot Reload, AOD, CC or rendering regression is reproducible, investigate its authoritative source, host/session lifecycle and writer before changing behavior. Keep unmeasured performance concerns observational, preserve native fallback and avoid new abstraction layers or arbitrary file-size targets. Promote `dev` separately only after its actual diff and applicable validation are reviewed.
