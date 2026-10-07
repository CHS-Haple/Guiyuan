# Architecture

This directory defines Guiyuan's current runtime ownership and scene/layout contracts.

Historical experiments and Build-by-Build evidence do not belong here. Current execution state lives in `docs/development/CURRENT.md`; durable historical decisions belong in development history/decision records.

## Current model

Guiyuan reuses verified native SystemUI hosts instead of creating a second permanent status-bar layout identity.

Current boundaries:

- Home is an independent projected presentation hosted by the verified native Home end-side carrier.
- Keyguard and AOD share one host-scoped family presentation owner and retarget scene semantics without duplicate mutable owners.
- Notification Shade remains native-only.
- Control Center remains native at the fully expanded endpoint; a bounded QS_FAKE bridge may project the compact representation during the native transition.
- SystemUI remains authoritative for native scene state, layout, appearance, alpha, visibility, translation and motion timing.
- Guiyuan owns only its own renderer, bounded represented-slot suppression/masking, verified reservation state and transition-only projection geometry.
- Any ambiguity in host, lifecycle, writer ownership or compatibility must fail native for the smallest affected surface.

The steady Home hierarchy is:

`MiuiNotificationStatusContainer / system_icon_area -> MiuiStatusBatteryContainer / system_icons -> Guiyuan child -> logical viewport -> renderer`

The logical viewport is independent from transparent drawing overflow. Enlarging Guiyuan's drawing surface must not change native slot geometry or peer motion.

## Documents

- [layout-policy.md](layout-policy.md) — geometry, occupancy, masking, reservation, motion and drawing-surface ownership.
- [scene-policy.md](scene-policy.md) — current scene capability classification, Keyguard/AOD family ownership and Control Center transition boundaries.
- [../reference/README.md](../reference/README.md) — reusable target-platform evidence. Reference evidence does not grant write ownership by itself.

## Authority

Architecture documents describe current reusable contracts. They must not accumulate Build chronology, candidate implementations or superseded experiments.

When runtime source and documentation disagree, verify the current source and target behavior, then correct the documentation. Do not revive an older route merely because it remains visible in Git history.
