# Current Development State

## Repository / build

- Product: Guiyuan 0.0.5.
- `main` remains on the promoted Build 618 stable checkpoint.
- `dev` baseline: Build 685 / `02b5398` with exact-head dev push Runtime CI #2577 passing.
- Current work-branch candidate: Build 686 / versionCode `261004686` / Build ID `20261004-686` on `fix/qs-fake-hotpath-overhead`.
- Build 686 is a behavior-preserving post-integration hot-path reduction; it does not change QS_FAKE geometry, timing, reservation semantics, native appearance ownership or writer boundaries.
- Verified target: Xiaomi 15 Pro / HyperOS SystemUI 17.03.260226.r / Android 17 / SDK 37 / Modern Xposed API 102.

## Accepted runtime facts carried into Build 685

### Home / Keyguard / AOD

- Home remains its own presentation owner.
- Keyguard and AOD use one host-scoped family renderer/presentation owner on the verified Keyguard host and retarget scene semantics without duplicate mutable owners.
- Keyguard and AOD child preferences remain independent under the global Guiyuan master gate.
- HyperOS remains owner of native AOD animation timing, alpha, visibility and translation.
- AOD is never a Control Center transition source.
- Home-origin native-AOD fallback blocks transient Keyguard visual rearm while the native AOD fallback is authoritative.

### Control Center / QS_FAKE

- QS_FAKE remains the bounded Control Center bridge; fully expanded Control Center stays native.
- Build 682 Battery-island peer reservation counts only peer-side intrusion in the live QS_FAKE end frame; no device pixel constant is encoded.
- Build 683 scopes fake-carrier capacity expansion to each visible Control Center cycle and releases it at the hidden boundary.
- Hidden ordering is reservation clear -> lease suppression -> capacity release.
- Build 684 removes high-frequency diagnostic construction from active QS_FAKE transition/layout frames without changing runtime geometry.
- One transition-reservation writer remains; no project translation/alpha/visibility writer was added.

## Integration audit status

Pre-merge static reconciliation is complete for:
- same-host Keyguard/AOD retarget lifecycle;
- feature toggle and resolver-failure cleanup;
- family fail-native restore ordering;
- Control Center source reconciliation and Keyguard lease boundaries;
- Hot Reload presentation release;
- deferred native-layout ownership + QS_FAKE visible-cycle capacity ownership;
- peer mirror / reservation / capacity single-writer structure.

Build 685 is now on `dev`; exact-head dev push Runtime CI #2577 passed.

## Post-integration audit still required

After Build 685 is on `dev`, run the full repository-level audit requested by the maintainer:
- Home / Keyguard / AOD / Control Center cross-scene lifecycle and callback-order review;
- Hot Reload, host replacement, detach, feature/settings changes and fail-native cleanup;
- single-writer and stale-lease review;
- performance hot path review, especially per-frame `statusIcons.paddingEnd` layout, native tint scans, TransitionDrawable alpha layers, allocations and repeated pre-draw work;
- device gate across Home, Keyguard, AOD, island/no-island, charging island, dual SIM and fast/partial pull-down.

No higher-risk performance optimization should be mixed into Build 685 before that audit.


### Build 685 pre-merge lifecycle audit hardening

The combined dev + QS_FAKE tree exposed and fixed three reconciliation/lifecycle hazards before merge:

- same-host Keyguard -> AOD retarget can leave native layout ownership deferred if the outgoing visual-only Keyguard boundary has not committed yet; the reused family session now resumes persistent ignored-slot/end-reservation ownership before waiting for the next native layout;
- synchronous fail-native during Home, Keyguard/AOD-family or QS_FAKE activation can clear the owner inside `start/sync/clip`; every activation/adoption path now revalidates current owner/surface before reporting `Active` or `Prepared`;
- visible-cycle rearm failure no longer reacquires QS_FAKE presentation in the same callback; that native event remains fail-native and a later native event may re-evaluate.

The audit also caught a three-way merge omission where Build 682's capacity-bounded charging-island reservation policy had not been wired into the merged runtime even though its diagnostics/tests were present. The accepted Build 682-684 runtime semantics are restored and covered by the existing capacity tests.

Runtime CI #2575 is the first complete Build 685 tree to pass compilation and tests with both AOD-family and QS_FAKE recovery semantics present.


## Build 686 post-integration hot-path audit

The first post-integration performance pass found four avoidable costs in the active QS_FAKE transition path without finding a new lifecycle owner or stale lease:

- the transition pre-draw listener rescanned the fake native status-icon hierarchy for peer tint on every frame even though the same native tint authority is already sampled on start, appearance changes and source-state changes;
- `TransitionSourceSnapshot` and resolved color state were rebuilt on every pre-draw even when `stateVersion` was unchanged;
- `TransitionDrawable.draw()` formatted the full tint diagnostic string on every frame even when diagnostics were not read;
- charging-island end-frame sampling and fake projection layout allocated short-lived coordinate arrays.

Build 686 keeps the same native evidence and refresh seams while reducing those costs:
- transition-source snapshots are cached by `stateVersion`;
- pre-draw refreshes native peer tint only when the source `stateVersion` changes, while start and native-appearance refreshes remain;
- tint diagnostics are formatted lazily when a diagnostic snapshot is requested;
- status-icon tint candidate traversal is allocation-free and preserves reverse child order;
- layout/island coordinate sampling reuses session-local scratch arrays.

The remaining audit is intentionally open for higher-risk draw-layer/allocation work and the final device gate. No animation curve, transition endpoint, padding/reservation formula, clip ownership, alpha/translation/visibility writer, or HyperOS appearance authority changes in this checkpoint.
