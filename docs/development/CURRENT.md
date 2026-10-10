# Current Development State

## Baseline

- Development integration: Guiyuan 0.5.0 / Build 875 (`20261010-875`), integrating the maintainability and AOD/Keyguard/Control Center lifecycle work through #381. Previously accepted UI, diagnostics, and NavigationEvent Compose 1.2.0 behavior remain part of this baseline. The maintainer reports no problems in the tested scenes; untested combinations are not claimed verified.
- Control Center callback failure diagnostics distinguish fallback requests from incomplete cleanup; Wi-Fi seed diagnostics preserve source and resource details for both ready and unavailable states.
- Stable `main`: Guiyuan 0.5.0 / Build 770; accepted Liquid nav preferences, diagnostics motion, semantic leading icons, feature grouping/defaults and previous runtime-state ownership baseline.
- Charging visibility follows the authoritative SystemUI battery state; charging-glyph identity no longer acts as a second charging-state source.
- Mobile runtime state keeps semantic signal strength instead of persisting native signal/VoLTE/VoWiFi resource IDs; presentation refresh and Hot Reload compatibility remain preserved.
- Liquid Glass selected-state tint follows the active MIUIX theme color, including Monet dynamic color.
- Liquid Glass has its own saved navigation-content choice, defaulting to icons and text; Standard, Blur and Glass share their original choice, defaulting to icons only. Explicit saved selections are preserved.
- Home runtime status keeps its existing state semantics while all accent/background color changes use one short transition.
- Charging lightning sampling and Diagnostics entry motion remain integrated in `dev`; current tested Home/Keyguard/refresh paths have no reported regression.

- Build and CI tooling: Gradle 9.8.1, Kotlin Compose/Serialization compiler plugins 2.4.21 and pinned `actions/upload-artifact` 7.0.2. MIUIX stays at the published `0.9.4-0657575a-SNAPSHOT` revision.

## Accepted runtime contract

- Home has its own projected presentation owner.
- Keyguard and AOD share one host-scoped family presentation owner; same-host scene changes retarget that owner instead of creating duplicate mutable owners.
- AOD is never a Control Center transition source.
- Notification Shade and the fully expanded Control Center remain native.
- QS_FAKE is the bounded Control Center transition bridge. A new fake-carrier width lease waits for the native host width/layout agreement and resumes on native layout; a hidden prearm lease may yield to an authoritative native width reset, while visible ownership conflicts fail native.
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

Continue the evidence-based audit in #328 from the integrated Build 875 `dev` tree, keeping `main` as the separate stable promotion boundary. Evaluate remaining independently diverged UI/5GA/mobile draft branches against current `dev` before adopting their unique behavior; they are not part of #381. Prefer coherent integration over more checkpoint branches, and request device evidence only when runtime changes warrant it.
