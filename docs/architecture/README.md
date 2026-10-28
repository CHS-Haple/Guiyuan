# Architecture

This directory defines Guiyuan's current runtime ownership, scene, and layout contracts.

Historical experiments and build-by-build evidence do not belong here. Current execution state lives in [CURRENT.md](../development/CURRENT.md); durable rationale lives in [DECISIONS.md](../development/DECISIONS.md).

## Current model

Guiyuan reuses verified native SystemUI hosts instead of creating a second permanent status-bar layout identity.

Current boundaries:
- Home is an independent projected presentation hosted by the verified native Home end-side carrier.
- Keyguard and AOD share one host-scoped family presentation owner and retarget scene semantics without duplicate mutable owners.
- Notification Shade remains native-only.
- Control Center remains native at the fully expanded endpoint; a bounded QS_FAKE bridge may project Guiyuan during the native transition.
- SystemUI remains authoritative for native scene state, layout, appearance, alpha, visibility, translation and motion timing.
- Guiyuan owns only its renderer plus bounded represented-slot suppression/masking, verified reservation state and transition-only projection geometry.
- In the current Home path, `NativeStatusObserver.attachObserver` observes native status icons through three hooks (Home manager registration, icon addition and static-icon visibility); `SysUiPresentationOwner` owns represented-slot masking and reservation. Native network icon suppression is not performed by the observer.
- Any ambiguity in host, lifecycle, writer ownership, or compatibility triggers fail-native behavior for the smallest affected surface.

The steady Home hierarchy is:

`MiuiNotificationStatusContainer / system_icon_area -> MiuiStatusBatteryContainer / system_icons -> Guiyuan child -> logical viewport -> renderer`

The logical viewport is independent from transparent drawing overflow. Enlarging Guiyuan's drawing surface must not change native slot geometry or peer motion.

## Preview sandbox

The Sandbox is a temporary **Home-only display lease**, not a source of real radio or battery state.

- `GyApp` owns the editable `PreviewSandboxUiState`. The switch starts the lease with the currently displayed state; only later control changes trigger `update`. Enabling the lease must not replay another snapshot.
- `SandboxClient` is the sole remote writer. It serializes writes, renews the three-second lease and stops on page exit or Xposed service loss. Simulation values are not saved as module settings.
- `SandboxHost` reads and validates the lease in SystemUI. Only eligible Home rendering uses its model; expiry, unbind and invalid input fall back to the latest real model.
- Keyguard, AOD, Control Center, native network and battery remain SystemUI-owned. Diagnostics only observe lease transitions.

## Documents

- [layout-policy.md](layout-policy.md) — geometry, occupancy, masking, reservation, motion and drawing-surface ownership.
- [scene-policy.md](scene-policy.md) — scene capabilities, Keyguard/AOD family ownership, and Control Center transition boundaries.
- [../reference/systemui-contracts.md](../reference/systemui-contracts.md) — reusable target SystemUI evidence. Evidence does not grant write ownership by itself.

## Authority

Architecture docs describe current reusable contracts, not candidate implementations or chronology.

When source/runtime evidence and documentation disagree, verify the current implementation and target behavior, then correct the document that owns the rule. Do not revive an older route merely because it exists in Git history.
