# Architecture document status

This directory contains the current architecture policy and scene/layout capability boundaries for Guiyuan.

## Current architecture status

The pinned target uses the accepted native Home carrier, one host-scoped Keyguard/AOD family presentation owner, and the bounded QS_FAKE Control Center transition bridge. Build-specific milestones below remain evidence for those contracts; live execution state belongs in `docs/development/CURRENT.md`.

- Builds 386-393 remain historical evidence for the superseded permanent extra-participant / occupancy-handoff route.
- Builds 397-535 establish the native carrier, width, motion and transition contracts that remain historical evidence for the current path.
- Build 536 device-validates the logical-slot / physical-overflow split: Guiyuan keeps the verified Home slot unchanged while one module-owned direct child of `MiuiStatusBatteryContainer` may extend only its transparent drawing surface upward.
- Build 537 extended that overflow policy to the opt-in Keyguard renderer; later Keyguard/AOD family integration and device validation retain the same logical-viewport / transparent-overflow boundary.

Current Home direction:

`MiuiNotificationStatusContainer / system_icon_area -> MiuiStatusBatteryContainer / system_icons -> module-owned direct child -> logical viewport -> Guiyuan renderer`

SystemUI retains native peer measurement/layout, Battery hide/presentation, tint authority, end-side visibility and live island/Folme motion. Guiyuan measures/layouts only its own child after native layout and keeps its logical viewport separate from any transparent physical overflow.

## Documents

- [layout-policy.md](layout-policy.md)
  - current shared geometry and Home carrier/reservation contract;
  - separation of visual geometry, native occupancy, motion and optical adjustment;
  - rejected geometry/writer patterns and future sizing boundary.

- [scene-policy.md](scene-policy.md)
  - current scene capability map;
  - Home plus the opt-in Keyguard/AOD family are runtime-verified Guiyuan rendering surfaces on the pinned target; bounded QS_FAKE transition projection is also accepted while the fully expanded Control Center remains native;
  - Notification Shade and the fully expanded Control Center remain native-only; Home, Keyguard, opt-in AOD, and the bounded QS_FAKE transition bridge are PROJECTED surfaces with SystemUI-owned native motion.

- [../reference/README.md](../reference/README.md)
  - generalized reusable implementation evidence;
  - reference evidence never grants SystemUI write ownership by itself.

## Superseded architecture route

The current architecture must not return to:

`extra permanent status participant -> zero/full-width occupancy handoff -> custom slot/translation compensation`

Those builds still provide useful evidence about native APPEAR behavior, battery-slot release, peer occupancy, charging geometry and panel anchors, but their carrier model created conflicting layout identities across scene transitions.

A superseded mechanism may be reconsidered only if new exact-target evidence invalidates the current route and a fresh ownership/lifecycle/single-writer review proves the alternative safer.

## History policy

Do not rewrite historical `DEVLOG.md` entries to match current conclusions. Preserve what was actually implemented and believed at the time, append later corrections, and keep current policy in this directory plus `docs/development/CURRENT.md`.
