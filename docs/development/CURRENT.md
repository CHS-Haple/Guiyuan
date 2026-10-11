# Current Development State

## Baseline

- Development integration: Guiyuan 0.5.0 / Build 880 (`20261011-880`), incorporating the tested B875 AOD/Keyguard/Control Center baseline and the distinct runtime-card guidance, 5GA native-target text transition, SIM-bound mobile event deduplication and Hot Reload network-state fixes from historical #335/#336/#340/#341. Maintainer acceptance covers previously tested paths; the new integration and rare rebind/Hot Reload interleavings are not claimed separately device verified. The two independently accepted feature-pref fixes from B878 (latest main-thread config guard) and B879 (clear-aware remote mirror) are combined; the resulting B880 integration has not been separately exercised on a device. The diagnostic logcat fallback now scopes current-session entries to the observed native PID instead of mixing interleaved process output; the LSPosed log-path behavior is unchanged and covered by a companion regression test.
- Control Center callback failure diagnostics distinguish fallback requests from incomplete cleanup; Wi-Fi seed diagnostics preserve source and resource details for both ready and unavailable states.
- Stable `main`: Guiyuan 0.5.0 / Build 770; accepted Liquid nav preferences, diagnostics motion, semantic leading icons, feature grouping/defaults and previous runtime-state ownership baseline.
- Charging visibility follows the authoritative SystemUI battery state; charging-glyph identity no longer acts as a second charging-state source.
- Mobile runtime state retains semantic signal strength without persisting native signal/VoLTE/VoWiFi resource IDs. Native icon event deduplication includes SIM identity, and Hot Reload restoration preserves attached roots plus newly observed Wi-Fi/mobile state while retaining fallback for missing transfer data.
- Liquid Glass selected-state tint follows the active MIUIX theme color, including Monet dynamic color.
- Liquid Glass has its own saved navigation-content choice, defaulting to icons and text; Standard, Blur and Glass share their original choice, defaulting to icons only. Explicit saved selections are preserved.
- Home runtime status keeps its existing state semantics and color transitions, with a scoped Xposed enable prompt when supported and one MIUIX loading indicator while checking/reloading.
- Charging lightning sampling and Diagnostics entry motion remain integrated in `dev`; current tested Home/Keyguard/refresh paths have no reported regression.

- Build and CI tooling: Gradle 9.8.1, Kotlin Compose/Serialization compiler plugins 2.4.21 and pinned `actions/upload-artifact` 7.0.2. MIUIX stays at the published `0.9.4-0657575a-SNAPSHOT` revision.

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

The initial repository-wide screening is not proof of a completed line-by-line audit. The reviewed source and runtime changes from [#329](https://github.com/CHS-Haple/Guiyuan/pull/329) are now part of the accepted `dev` baseline through #381; its redundant PR was closed. The systematic evidence-based audit [#328](https://github.com/CHS-Haple/Guiyuan/issues/328) remains open for unreviewed files and risks.

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

## Guardrails

- Root cause before workaround.
- One writer per mutable surface.
- No polling, delay/retry repair loops, guessed thresholds or fixed device geometry.
- Do not take over native translation, alpha, visibility or final Control Center appearance.
- Do not weaken exact restoration or fail-native behavior.
- Change external versionName only on explicit promotion; current baseline is 0.5.0. Increment Build ID and Android versionCode only for a materially different APK (behavior, UI/assets or compatibility), not for docs, non-behavioral refactors, or repeat CI/builds. The exact Git SHA distinguishes source revisions within a Build.

## Next

Continue the evidence-based audit in #328 from the integrated Build 880 `dev` tree, keeping `main` as a separate stable promotion boundary. The historical UI/5GA/mobile draft candidates #335/#336/#340/#341 have been consolidated into this source tree; do not re-merge their old branches. Review future runtime changes from current `dev`, and request focused device evidence only when necessary.
