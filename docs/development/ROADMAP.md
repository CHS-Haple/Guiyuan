# Guiyuan Development Roadmap

ROADMAP describes future direction only. Build chronology/debugging history belongs in DEVLOG; current execution state belongs in CURRENT.

## Phase 1 — Core Home/SystemUI foundation — complete

Established:
- compact Home Guiyuan rendering;
- authoritative battery, Wi-Fi, mobile-network, airplane and SIM/data state;
- native tint/resource reuse;
- reversible native replacement with fail-native fallback;
- event-driven diagnostics and Hot Reload;
- single/dual-SIM, hotspot, no-SIM, airplane and mobile-type semantics.

## Phase 2 — Home carrier and Control Center ownership — ownership complete; accepted on the pinned target

Stable contracts:
- steady Home geometry remains SystemUI-hosted;
- charging/Super-Island motion stays native-owned;
- Notification Shade remains native-only on the pinned target;
- QS_FAKE is the bounded Control Center bridge;
- QS_FAKE carrier-capacity expansion is scoped to each native Control Center visible cycle and must release at the hidden boundary;
- fully expanded Control Center remains native.

Ongoing guardrails:
- preserve accepted steady Home/Keyguard/AOD geometry, Build-473 Wi-Fi optical behavior, and the verified QS_FAKE capacity/reservation boundaries;
- treat Build 744's Home-mirror source scoping as the accepted ownership boundary on the pinned target;
- require concrete code-level or reproducible device evidence before performance/compositing changes, and keep each runtime candidate behind CI/Canary/device acceptance;
- avoid project-owned gesture timing, duplicate translation writers, endpoint snaps, duplicate occupancy, or benchmark-only geometry changes.

## Phase 3 — Keyguard / AOD ownership — integrated and runtime-verified

Established:
- optional steady Keyguard Guiyuan remains independent from Home ownership while Keyguard/AOD share one host-scoped family presentation owner;
- optional AOD is an independently gated family scene and does not inherit Home or Keyguard ownership implicitly;
- Keyguard-originated QS_FAKE uses the same verified transition boundary;
- session-owned slot exclusions are reversible and fail native on ambiguity;
- Build 620/621 adds a dedicated default-off AOD display preference while preserving the global Guiyuan enable as the parent runtime gate;
- Keyguard and AOD child preferences persist independently; changing either one does not rewrite the other;
- Build 623 device evidence rejects separate Keyguard/AOD presentation/render sessions because restore/reacquire exposes native represented icons between sessions; Build 625 instead keeps one Keyguard-family presentation owner and one RenderView across same-host Keyguard<->AOD retargeting, while Home->AOD uses bounded reversible pre-mask without declaring compact layout ready early;
- unsupported or ambiguous AOD authority/topology fails native without affecting accepted Home behavior;
- AOD remains ineligible as a Control Center transition source.

Remaining:
- preserve family continuity and preference independence as new features or targets are added;
- revalidate the family owner on any new SystemUI target instead of inferring compatibility from the pinned target;
- reopen cross-scene performance work only from concrete code-level or device evidence rather than carrying forward a closed pinned-target ownership investigation.

## Phase 4 — Companion app and customization — product polish

Established:
- MIUIX Home / Features / Settings;
- runtime state and diagnostics UI;
- Preview Sandbox using the production renderer;
- language, launcher visibility, navigation appearance, diagnostics export/share;
- battery-top percentage/native charging-glyph presentation with profile-scoped size, weight and offset controls;
- profile-scoped combined size, outer ring/dot weight, Wi-Fi size, and mobile-type size/weight controls;
- battery color scheme library with built-in/custom schemes and per-semantic fixed/follow/custom color sources;
- battery semantic colors and color-link controls.

Planned/deferred:
- further adaptive layout/spacing compatibility only where live native geometry requires it;
- additional Preview scenarios only when they improve real configuration usability.

Do not add controls merely because a renderer parameter exists.

## Phase 5 — Compatibility and 1.0.0 qualification

Before 1.0.0:
- close supported Home/Keyguard/AOD/Control Center transition acceptance;
- verify fail-native behavior on unsupported/unknown conditions;
- review target-profile compatibility and dependencies;
- remove obsolete diagnostics/probes and stale branches with no remaining engineering value;
- ensure public docs, privacy/security notices, third-party notices, changelog, signing, metadata, and release workflow are ready.

1.0.0 remains an explicit maintainer decision.

## Persistent design seams

Preserve:
- authoritative native state -> domain state -> scene/presentation policy -> renderer;
- independent Home ownership plus one host-scoped Keyguard-family presentation owner that retargets Keyguard/AOD scene semantics without duplicate mutable owners;
- native final Control Center ownership; opt-in AOD uses the bounded Keyguard-family replacement contract while HyperOS retains AOD timing/motion authority, and AOD never acts as a Control Center transition source;
- native resource identity/tint authority where available;
- custom colors/sizing as presentation policy, not duplicate platform state;
- bounded diagnostics outside hot paths.

## Rejected by default

Do not reintroduce without new exact-target evidence:
- polling-based state/geometry ownership;
- copied Battery/peer animation timelines;
- timer/delay lifecycle fixes;
- persistent duplicate status participants when a verified native carrier exists;
- global native-slot suppression outside the owning scene/session;
- per-resource grayscale/alpha compensation used only to force visual similarity;
- historical Build-specific patches as current architecture.
