# Current Development State

## Repository / build

- Product: Guiyuan 0.1.0.
- `main` and `dev` are aligned on the promoted Build 709 stable checkpoint before this work branch.
- Active work: `feat/diagnostics-log-workbench` / Build 710 (`20261004-710`).
- Build 710 renames the existing Diagnostics information page to **About** without moving any of its cards, adds a separate **Diagnostics** destination, and introduces an on-demand log workbench with search, current-session/all-log scope, and manual refresh.
- The log workbench and exported diagnostic report share one `DiagnosticsLogReader`: LSPosed module log first, filtered logcat fallback second, identical Guiyuan filtering and latest-session selection. Reading occurs once on page entry and only again on explicit refresh; no polling, resident collector, runtime hook, or SystemUI/Xposed ownership change is introduced.
- Existing Project address and SystemUI restart rows remain in place for this checkpoint.
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

- the transition pre-draw listener rebuilt a temporary peer collection before resolving live native tint on every frame;
- `TransitionSourceSnapshot` and resolved color state were rebuilt on every pre-draw even when `stateVersion` was unchanged;
- `TransitionDrawable.draw()` formatted the full tint diagnostic string on every frame even when diagnostics were not read;
- charging-island end-frame sampling and fake projection layout allocated short-lived coordinate arrays.

Build 686 keeps the same native evidence and refresh seams while reducing those costs:
- transition-source snapshots are cached by `stateVersion`;
- live native peer tint sampling remains on pre-draw because applied native tint can change independently from Guiyuan `stateVersion`; its candidate traversal is now allocation-free and preserves reverse child order;
- tint diagnostics are formatted lazily when a diagnostic snapshot is requested;
- layout/island coordinate sampling reuses session-local scratch arrays.

Remaining audit: review Home / Keyguard / AOD hot paths and the residual QS_FAKE draw-layer allocations. Do not pursue Painter-spec caching or `saveLayerAlpha` replacement without stronger evidence because those paths begin to affect drawing-state semantics. No animation curve, transition endpoint, padding/reservation formula, clip ownership, alpha/translation/visibility writer, or HyperOS appearance authority changed in Build 686.

## Build 689 Keyguard-island diagnostic hot-path follow-up

Build 687 reduced two diagnostic sources but failed the focused device gate: Keyguard combined status + active island still becomes visibly janky under repeated complete Control Center pull/down-up cycles.

The reproduced LSPosed window shows the remaining Detailed path still runs synchronously on the SystemUI main thread and emits hundreds of records during the stress gesture. The dominant avoidable work is:
- repeated multi-KB `controlCenterTransitionGeometry` snapshots when the gesture re-crosses selected buckets;
- bucket-only `panelTransition` logging and diagnostic anchor/Home-motion snapshots;
- QS_FAKE `nativeSourceSyncDiag` construction from native layout callbacks;
- repeated appearance and island-owner observation records.

Build 689 preserves every Build 687 reduction and further limits observation to semantic/lifecycle edges:
- no full transition geometry/state/projection snapshot is built from expansion callbacks;
- fraction-bucket changes alone no longer emit `panelTransition` diagnostics;
- diagnostic-only control-anchor/Home-motion snapshots are not captured during expansion;
- QS_FAKE native-source layout snapshots are disabled; Home retains only steady-peer-mirror change diagnostics;
- appearance and island-owner diagnostics are deduplicated to state changes.

Functional callbacks, native source resolution, Keyguard lease, steady-peer-mirror scanning, transition reservation, geometry, tint, alpha/translation/visibility ownership and drawable compositing are unchanged.

Device gate: repeat the exact Keyguard + active-island rapid full pull/down-up stress case. If visible jank remains, the observation path is no longer the primary suspect; only then review the functional steady-peer-mirror/layout work and residual draw/compositing cost.


## Build 690 native peer reflection hot-path follow-up

Build 689 passed exact-head Runtime and removed the remaining gesture-frame geometry/anchor diagnostics, but the focused device gate still reports substantial jank with Keyguard combined status + active island + repeated full Control Center pull/down-up.

The Build 689 evidence narrows the next functional cost:
- island-active Home native layouts continue to sample the full non-represented peer row for the QS_FAKE steady-peer mirror;
- `NativeParticipantRuntimeAccess.slotOf()` re-walks each concrete child class method hierarchy for `getSlot()` on every lookup;
- `readTransitionIconState()` re-runs `Class.forName`, scans companion declared methods, calls `setAccessible`, invokes the state accessor, then re-walks the state class fields for every property of every sampled peer;
- the stress log repeatedly reports mirror transitions while `hiddenSlots=[]`, so this reflection cost is paid even when no peer ultimately needs clipping.

Build 690 preserves the exact native-layout sampling cadence and hidden-slot policy, but caches the stable reflection contracts by concrete SystemUI class. The steady peer mirror now reads only `visibleState` and `inIslandState`, the only fields used by `SteadyPeerMirrorPolicy.isIslandHidden()`; full transition diagnostics retain the full cached state reader.

This is intentionally narrower than changing mirror cadence or native layout ownership. If the focused device gate remains poor after Build 690, inspect hidden Home tint work and residual TransitionDrawable/compositing cost next.

## Build 691 Detailed native-layout diagnostic follow-up

Build 690 materially improved Keyguard + active-island repeated pull smoothness, but device A/B showed a remaining diagnostics-level effect: General felt smoother, while switching back to Detailed reintroduced a mid-gesture hitch.

The same SystemUI session recorded the level transition to General at 07:24:01.550 and back to Detailed at 07:24:14.341. Once Detailed was active, each island show/hide mirror edge again produced a full `homePresentation nativeSourceSyncDiag` containing the entire non-represented peer row. Build 690 had already reduced the functional mirror reader to cached reflection + the two required island fields, so the full peer snapshot was now redundant hot-path observation work.

Build 691 removes only that full Home native-layout source snapshot and its obsolete policy/test path. The existing lightweight `steadyPeerMirror active/hiddenSlots` event remains, as do lease, scene, readiness and lifecycle diagnostics. Functional mirror sampling, clip ownership and native-state reads are unchanged.

Device gate: compare General versus Detailed using the same Keyguard + active-island repeated full pull/down-up sequence. Detailed should no longer introduce a distinct mid-gesture hitch. If a gap versus Home remains with both levels equivalent, proceed to residual draw/compositing review rather than further diagnostic trimming.

## Build 693 duplicate semantic diagnostic cleanup

Build 691 confirmed that the full `nativeSourceSyncDiag peers=[...]` snapshot is gone. The supplied Detailed/General A/B still shows Detailed producing repeated pairs for the same semantic edges: Home steady-peer mirror state is immediately echoed by the QS_FAKE target session, and native island owner show/hide is immediately echoed by `controlCenterAppearance`.

Build 693 keeps one authoritative record for each:
- keep Home `steadyPeerMirror source=home active/hiddenSlots`;
- remove the immediate QS_FAKE `controlCenterPresentation steadyPeerMirror` echo;
- keep `islandOwner event showing`;
- remove text-only `controlCenterAppearance` diagnostics while preserving its native hook and functional `onUpdate` payload.

Panel visibility, lease, readiness, scene, fail-native and suppression diagnostics remain. No functional runtime behavior changes, so device validation is not required beyond exact-head CI and review.

## Build 694 QS_FAKE hidden/prearm lease lifecycle

A prior one-time native fallback showed a real ownership gap: QS_FAKE prearm leased the fake carrier from native width 587 to parent width 836 while hidden, then a HyperOS native hidden relayout restored the live width to 587 without delivering the expected visible=false boundary. The stored lease still believed 836 was owned, so the first visible preparation treated the legitimate native reset as a foreign writer conflict and failed native for that pull.

Build 694 makes the ownership boundary explicit:
- a QS_FAKE session starts in hidden/prearm ownership;
- the first visible=true edge performs one reservation reconciliation before promoting the lease to visible ownership;
- while hidden/prearm, if the parent-content contract is unchanged and the live width is a valid positive native width within that parent, a width different from the leased width is adopted as the new native baseline and the lease is reacquired;
- while visible, the same mismatch remains a hard writer conflict and preserves fail-native;
- parent-content changes are never silently adopted by this path;
- visible=false demotes ownership before clearing transition reservation and releasing the lease, reusing the existing hidden-boundary semantics.

No timer, polling, second state machine, or relaxed visible writer ownership is introduced. Unit coverage locks the exact 587→836→587 hidden/prearm case, visible mismatch failure, unchanged reuse, and parent-contract mismatch failure.

## Build 695 attached-session visible ownership confirmation

Review of Build 694 found an ordering edge: the native Control Center visible callback calls `beginVisibleCycle()` before the fallback attach path. On the first ever visible cycle, no QS_FAKE owner may exist yet, so that early begin call can legitimately return success without marking the subsequently attached session as visible.

Build 695 closes that gap without removing the early begin:
- the early `beginVisibleCycle()` still reconciles an already-prearmed session before other visible-cycle work;
- after a session is attached/prepared, its own `requestedVisible false→true` edge confirms `onControlCenterVisibilityChanged(true)` again;
- an already-visible prearmed owner treats the second call as idempotent;
- a newly attached owner performs the hidden/prearm reconcile and then promotes to visible ownership;
- if the attached-session handoff fails, requested visibility is not promoted and native/Home fallback remains authoritative.

Unit coverage locks false→true as the only attached-session begin edge, while existing tests keep true→false as the only end edge. No extra timer, polling, geometry writer, or duplicate lease acquisition is introduced.

## Build 696 bounded SystemUI restart transaction

Build 696 rebases the restart-maintenance work onto the merged Build 695 dev baseline so the branch contains only the restart transaction change.

The previous action was functional but under-specified:
- the positive restart button hid the MIUIX confirmation dialog and immediately executed the Root command before the dialog/blur exit completed;
- the Root command blindly used `killall com.android.systemui`;
- command success was treated as restart success without checking that a replacement SystemUI process appeared.

Build 696 uses explicit semantic boundaries:
- the positive action arms a transient pending restart and dismisses the dialog;
- cancel/back clears that pending action;
- MIUIX 0.9.4 `OverlayDialog.onDismissFinished` is the only boundary that starts the Root transaction;
- `pidof` resolves the current `com.android.systemui` PID;
- exactly that originally resolved PID is sent SIGTERM once;
- a bounded 60 × 100 ms in-shell probe requires the old PID to be absent and at least one replacement SystemUI PID to be present;
- the Root command has a 10-second outer timeout and never escalates to `force-stop`, `am crash`, SIGKILL, `pkill`, or `killall`.

The probe exists only inside the single user-triggered maintenance command; it is not a runtime poller, service, or lifecycle owner. If the original PID disappears before signal delivery, no newly appeared PID is signalled.

Device gate: verify restart with no island, normal island, and charging island if available. Confirm the dialog fully exits before SystemUI disappears, SystemUI relaunches automatically, cancel/back never restarts it, and treat any remaining transient island mosaic as a separate presentation issue rather than restart correctness.

