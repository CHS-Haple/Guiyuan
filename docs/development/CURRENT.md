# Current Development State

## Baseline

- Development integration: Guiyuan 0.4.0 / Build 769 (`20261008-769`); accepted Preview Sandbox and Keyguard/AOD leading icons, Diagnostics entry/list motion, Feature grouping and defaults. Combined Home/Keyguard/refresh acceptance remains tracked separately.
- Stable `main`: Guiyuan 0.4.0 / Build 749; accepted Liquid Glass, diagnostics refresh, runtime-state ownership, and maintainability baseline.
- Charging visibility follows the authoritative SystemUI battery state; charging-glyph identity no longer acts as a second charging-state source.
- Mobile runtime state keeps semantic signal strength instead of persisting native signal/VoLTE/VoWiFi resource IDs; presentation refresh and Hot Reload compatibility remain preserved.
- Liquid Glass selected-state tint follows the active MIUIX theme color, including Monet dynamic color.
- First-time Liquid Glass selection defaults navigation content to icons and text without overriding an existing user choice.
- Home runtime status keeps its existing state semantics while all accent/background color changes use one short transition.
- Charging lightning sampling and Diagnostics entry motion are integrated in `dev`, with combined Home/Keyguard/refresh device validation still outstanding.

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

Repository-wide maintainability review is considered complete unless a concrete new finding appears.

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
- Keep external versionName 0.4.0 unchanged unless explicitly requested. Increment Build ID and Android versionCode only for a materially different APK (behavior, UI/assets or compatibility), not for docs, non-behavioral refactors, or repeat CI/builds. The exact Git SHA distinguishes source revisions within a Build.

## Next

Choose the next task from [ROADMAP.md](ROADMAP.md) or a concrete device issue. Validate remaining combined Home/Keyguard/refresh scenarios before the next runtime stable promotion.
