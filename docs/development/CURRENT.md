# Current Development State

## Baseline

- Development integration: Guiyuan 0.5.0 / Build 817 (`20261009-817`); NavigationEvent Compose 1.2.0 is integrated, with system-back gestures from Features and Settings to Home accepted on the separate Build 817 Canary. Previous dev UI behavior remains accepted; diagnostics use meaningful compact summaries and keep expanded technical fields raw. Existing diagnostics/list motion, leading icons, feature grouping/defaults, and independent Liquid nav-content preferences remain accepted. Combined Home/Keyguard/refresh acceptance remains tracked separately.
- Stable `main`: Guiyuan 0.5.0 / Build 770; accepted Liquid nav preferences, diagnostics motion, semantic leading icons, feature grouping/defaults and previous runtime-state ownership baseline.
- Charging visibility follows the authoritative SystemUI battery state; charging-glyph identity no longer acts as a second charging-state source.
- Mobile runtime state keeps semantic signal strength instead of persisting native signal/VoLTE/VoWiFi resource IDs; presentation refresh and Hot Reload compatibility remain preserved.
- Liquid Glass selected-state tint follows the active MIUIX theme color, including Monet dynamic color.
- Liquid Glass has its own saved navigation-content choice, defaulting to icons and text; Standard, Blur and Glass share their original choice, defaulting to icons only. Explicit saved selections are preserved.
- Home runtime status keeps its existing state semantics while all accent/background color changes use one short transition.
- Charging lightning sampling and Diagnostics entry motion are integrated in `dev`, with combined Home/Keyguard/refresh device validation still outstanding.

- Build and CI tooling: Gradle 9.8.1, Kotlin Compose/Serialization compiler plugins 2.4.21 and pinned `actions/upload-artifact` 7.0.2. MIUIX stays at the published `0.9.4-0657575a-SNAPSHOT` revision.

## Accepted runtime contract

- Home has its own projected presentation owner.
- Keyguard and AOD share one host-scoped family presentation owner; same-host scene changes retarget that owner instead of creating duplicate mutable owners.
- AOD is never a Control Center transition source.
- Notification Shade and the fully expanded Control Center remain native.
- QS_FAKE is the bounded Control Center transition bridge.
- SystemUI owns native scene state, layout, appearance, alpha, visibility, translation and motion timing.
- Guiyuan owns only its renderer plus the minimum verified suppression, masking, reservation and transition-projection state.
- Any ownership, host, geometry, or compatibility ambiguity triggers fail-native behavior for the smallest affected surface.
- Diagnostic/build-channel flags are observational only and must not alter functional runtime control flow.

## Maintainability baseline

A source-wide static screening has been performed, but its inventory is not proof of a completed line-by-line audit. Continue evidence-based review of actual code and call paths; do not rewrite stable features to satisfy a file-count or style target.

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

## Active stacked preview

- PR #339 (Build 836) corrects mobile-type handoff when native 5G text is hidden: no guessed text slot endpoint, including cached witnesses. A matched native TextView now contributes its visible glyph bounds instead of its whole layout box. This remains Draft for focused visual acceptance; the data-SIM source and existing unmatched clip are unchanged.

## Next

Continue the evidence-based source audit in [#328](https://github.com/CHS-Haple/Guiyuan/issues/328) and Draft [#329](https://github.com/CHS-Haple/Guiyuan/pull/329). Review Hook installation/rollback, Hot Reload cleanup, cache invalidation and the full base→HEAD diff before integration. Combined Home/Keyguard/refresh device acceptance remains outstanding; do not promote the runtime changes on CI evidence alone.
