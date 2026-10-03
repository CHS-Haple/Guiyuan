# Current Development State

## Repository / build

- Product: Guiyuan 0.0.5.
- `main` remains on the promoted Build 618 stable checkpoint.
- Integration candidate: Build 685 / versionCode `261004685` / Build ID `20261004-685`.
- Candidate reconciles `dev` Build 672 AOD lifecycle (#196) with Build 684 QS_FAKE recovery/performance work.
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

Build 685 must pass exact-head Runtime CI before it may update `dev`.

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
