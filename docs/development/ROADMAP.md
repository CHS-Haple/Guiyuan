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

## Phase 2 — Home carrier and Control Center ownership — ownership complete; transition polish in progress

Stable contracts:
- steady Home geometry remains SystemUI-hosted;
- charging/Super-Island motion stays native-owned;
- Notification Shade remains native-only on the pinned target;
- QS_FAKE is the bounded Control Center bridge;
- QS_FAKE carrier-capacity expansion is scoped to each native Control Center visible cycle and must release at the hidden boundary;
- fully expanded Control Center remains native.

Remaining:
- make Guiyuan and relevant native-peer motion through the Control Center gesture visually coherent with HyperOS;
- preserve accepted steady Home/Keyguard geometry and Build-473 Wi-Fi optical behavior;
- avoid project-owned gesture timing, duplicate translation writers, endpoint snaps, or duplicate occupancy.

## Phase 3 — Keyguard / AOD ownership — steady Keyguard complete; optional AOD planned

Established:
- optional steady Keyguard Guiyuan with independent host/session;
- AOD remains native-only in the accepted runtime baseline;
- Keyguard-originated QS_FAKE uses the same verified transition boundary;
- session-owned slot exclusions are reversible and fail native on ambiguity.

Planned AOD control contract:
- add a dedicated AOD display toggle; AOD must remain disabled when this toggle is off;
- the global combined-status enable switch is the parent gate for Home, Keyguard, and AOD, so disabling Guiyuan must also release any AOD replacement and restore native AOD;
- Keyguard display and AOD display are independent child preferences: enabling/disabling either one must not change the other;
- AOD must use its own bounded scene/host/session ownership and lifecycle cleanup rather than reusing Keyguard visibility as a proxy;
- unsupported or ambiguous AOD topology fails native without affecting Home or Keyguard.

Remaining:
- shared Control Center transition presentation from the Keyguard source scene;
- exact-target AOD host/lifecycle evidence before implementing the optional AOD scene.

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
- close supported Home/Keyguard/Control Center transition acceptance;
- verify fail-native behavior on unsupported/unknown conditions;
- review target-profile compatibility and dependencies;
- remove obsolete diagnostics/probes and stale branches with no remaining engineering value;
- ensure public docs, privacy/security notices, third-party notices, changelog, signing, metadata, and release workflow are ready.

1.0.0 remains an explicit maintainer decision.

## Persistent design seams

Preserve:
- authoritative native state -> domain state -> scene/presentation policy -> renderer;
- independent Home, Keyguard, and optional AOD scene ownership;
- native final Control Center ownership; AOD remains native unless the dedicated AOD scene safely acquires its own bounded replacement contract;
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
