# Scene capability policy

This document defines current scene capability and presentation ownership. Shared geometry belongs in [layout-policy.md](layout-policy.md).

Build chronology, rejected candidates and one-off device investigations do not belong here.

## Capability model

A scene capability may define:

- whether Guiyuan may render on the surface;
- whether rendering is projected from verified native geometry;
- who owns motion;
- the source-code evidence classification.

It must not define per-scene size formulas, correction offsets or independent gesture timing.

The current source policy is:

| Scene | Render mode | Motion owner | Source evidence |
| --- | --- | --- | --- |
| Home stable | PROJECTED | NONE | RUNTIME_VERIFIED |
| Notification Shade transition | NATIVE_ONLY | SYSTEM_UI | STATIC_VERIFIED |
| Control Center steady/final | NATIVE_ONLY | SYSTEM_UI | STATIC_VERIFIED |
| Keyguard | PROJECTED | SYSTEM_UI | STATIC_VERIFIED |
| AOD | PROJECTED | SYSTEM_UI | STATIC_VERIFIED |

The table mirrors `ScenePolicy`. Runtime acceptance and the active development baseline are tracked separately in `docs/development/CURRENT.md`.

Classification never grants permission to mutate SystemUI. Every integration still requires a verified host, lifecycle, writer boundary and fail-native path.

## Home

Home is the primary persistent Guiyuan presentation.

- It uses its own host-scoped presentation owner.
- The renderer lives inside the verified native end-side carrier.
- Native peer layout and motion remain SystemUI-owned.
- Panel motion must not be reconstructed from a local fraction threshold or Battery state.
- Home ownership yields only through verified scene/lifecycle facts.

## Notification Shade

Notification Shade remains native-only.

Guiyuan must not create a second visibility system from generic panel progress. Native callbacks may be useful as motion context or diagnostics, but they do not automatically become presentation authority.

## Control Center

The fully expanded Control Center remains native-only.

A separate bounded QS_FAKE bridge may project Guiyuan during an eligible Home- or Keyguard-origin transition. That bridge is not a second steady Control Center scene.

### Source eligibility

A transition source must resolve to a currently eligible projected source:

- Home; or
- Keyguard when Keyguard replacement is enabled and presentation-ready.

AOD is never a Control Center transition source.

Conflicting native source witnesses must be reconciled from current lifecycle/family state; an unknown source fails native rather than guessing.

### Transition bridge

During an eligible transition:

- SystemUI supplies the native expansion/motion context;
- source geometry comes from the verified native source carrier and Guiyuan logical basis;
- target geometry comes from verified native final participants;
- Guiyuan draws only its own replacement correspondence;
- native final participants retain their own alpha, visibility and translation;
- final-only native participants remain native;
- transition reservation may expose the projected semantic occupancy to native peers through the one existing reservation writer;
- carrier-capacity leasing remains a separate bounded lifecycle concern, not a second motion system.

The bridge must not introduce:

- a custom gesture clock;
- a project-owned fake/final visibility threshold;
- native peer translation writes;
- final-surface suppression;
- duplicate tint authority.

At the native final appearance handoff, Guiyuan yields to the native Control Center surface.

## Keyguard and AOD family

Keyguard and AOD share one verified host-scoped family presentation owner.

When the native host remains the same, changing Keyguard/AOD semantics retargets that owner instead of restoring native state and creating a second session.

The family owner may retain:

- its one renderer;
- its exact represented-slot exclusion delta;
- reversible masks;
- owned reservation state;

only for the lifetime of that verified family host.

### Same-host retarget

A same-host Keyguard/AOD change may keep existing compact layout ownership when that ownership is still valid.

Changing semantic scene does not by itself justify:

- a second renderer;
- a second ignored-slot owner;
- native restore/reacquire between the two family states.

### Cross-host handoff

Home and the Keyguard/AOD family are distinct owners.

A cross-host target may prepare only a bounded reversible visual claim before its compact layout is actually valid. Visual pre-mask is not layout readiness, and source ownership must not be borrowed into the target host.

### AOD

AOD is separately feature-gated.

SystemUI remains authoritative for:

- AOD direction;
- native status-icon alpha;
- native visibility;
- native translation;
- AOD timing.

Guiyuan may select its family child from those verified facts but must not create its own AOD animation clock.

If AOD replacement is disabled, native AOD remains authoritative even when Keyguard replacement is enabled.

## Charging and island state

Charging, quick charging and super charging are renderer states, not scenes.

They must not create:

- a separate scene capability;
- a second slot-width policy;
- independent scene motion.

Island/Battery-island facts may affect verified reservation or transition semantics, but native island geometry and motion remain SystemUI-owned.

## Motion ownership

Home stable uses `NONE` because Guiyuan has no independent motion requirement there; its carrier inherits native motion.

Notification Shade and Control Center use `SYSTEM_UI`.

Keyguard/AOD projected presentation also keeps native scene motion under `SYSTEM_UI`.

No current scene grants Guiyuan ownership of native motion. Any future module-owned transition would require proof of the complete start-to-cleanup motion lifecycle before the capability policy could change.

## Promotion rule

Changing a scene from `NATIVE_ONLY` to `PROJECTED`, or adding a new native geometry/motion write, requires:

1. exact target-SystemUI evidence;
2. runtime host/lifecycle verification;
3. single-writer analysis;
4. fail-native behavior;
5. bounded diagnostics;
6. focused device validation;
7. matching source capability and public engineering documentation.

Without that evidence, the scene remains native.
