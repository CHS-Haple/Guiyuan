# Current Development State

## Accepted baseline

- Product / display version: Guiyuan 0.0.5.
- `main`: promoted Build 618 stable checkpoint.
- latest `dev`: Build 672 integration, including PR #196 AOD lifecycle work.
- Verified target: Xiaomi 15 Pro / HyperOS SystemUI 17.03.260226.r / Android 17 / SDK 37 / Modern Xposed API 102.

## Active objective

Branch: `fix/qs-fake-native-source-sync` / PR #200.

This recovery line intentionally restarted from accepted Build 652 rather than continuing PR #197's rejected 653-672 island experiments. It restores HyperOS/Home steady island membership as the peer-visibility authority and keeps Guiyuan limited to represented-slot adaptation, one reversible fake-row peer mirror, one bounded fake-carrier capacity lease, and the existing `statusIcons.paddingEnd` reservation writer.

The branch is still one latest-`dev` integration commit behind because PR #196 landed after this recovery base. That AOD integration is unrelated to the charging-island geometry under correction and must be reconciled before final integration, not mixed into the current device attribution checkpoint.

## Confirmed device evidence

Build 676:
- ordinary-island fake peer membership matches Home steady through the reversible slot mirror;
- charging island no longer needs an independent fake-row island decision.

Build 677:
- charging-island native takeover at the previous capacity boundary is removed by saturating native end reservation to the already-leased physical fake-carrier capacity;
- logical Guiyuan reservation remains unmodified.

Build 678 device rejection:
- native peer spacing is still visibly too large during charging-island pull;
- at about 25% progress the compact slot is still the visible left boundary, but native peer reservation has already grown from 105px to about 134px;
- therefore the remaining gap is not the Build-677 capacity clamp and not a final-width constant;
- Build 678 incorrectly used future/final native-row capacity as a proxy for the peer spacing needed by the currently visible Guiyuan projection.

## Build 679 device rejection

Build 679 reduced the premature gap, but the charging-island distance is still abnormal during the pull.

The remaining defect is a coordinate-frame mismatch:
- frozen reservation spans express target X relative to the **final QS Battery end**;
- visible native peers are laid out by `QS_FAKE statusIcons.paddingEnd`, whose end edge moves with HyperOS' charging-island fake-root translation;
- Guiyuan rendering already rebases its source through that live fake carrier before interpolating to the absolute final target;
- Build 679 measured the span union without projecting the final-Battery end into the current fake-carrier end frame, so the native peer reservation increasingly diverges from the pixels as the two carrier frames separate.

This rejects both a fixed-width subtraction and direct `batteryWidthDiff` compensation.

## Build 680 pre-device review

Build 680 keeps every accepted ownership boundary and corrects only the Battery-island peer-spacing frame. Submission review then found one fail-native gap before device testing: if the live QS_FAKE/final end-frame sample becomes temporarily unavailable, the transition owner returned while the previous frame's native reservation could remain applied.

## Build 681 device rejection

Build 681 fixes the end-frame loss lifecycle gap, but device video still shows the charging-island native-peer distance is wrong during the QS_FAKE phase.

The new diagnostic closes the remaining geometry question:
- `nativePeerTargetEndOffset` is stable for this gesture because HyperOS moves fake/final Control Center rows with the same progress-scaled normal translation and the Battery-island fake row carries one additional native end offset;
- this means Build 680/681 frame projection is not the remaining fault;
- the remaining fault is the **meaning of the value written to `statusIcons.paddingEnd`**.

Build 681 measures the full projected span union as `right - left`. After final-QS spans are projected into the live QS_FAKE end frame, part of that union can lie at logical `x > 0`, on the end side of the fake-row boundary. That region is valid drawable/target occupancy, but it does not occupy the peer side of the QS_FAKE row and must not push native peers left.

The full-progress device sample makes the over-count visible: semantic/native union is 384px while the projected end-side extent is the live inter-frame offset. The applied fake-row capacity reaches its independent 249px expansion limit. The defect is therefore not a device-specific constant and must not be corrected by subtracting 135px or any other fixed value.

## Build 682 candidate

Build 682 changes only the Battery-island native peer-spacing adapter:

- keep the final-QS -> live-QS_FAKE end-frame projection from Build 680;
- keep the Build 681 fail-native lifecycle handling when that frame is unavailable;
- keep logical semantic reservation, latent reveal, ordinary-island/no-island paths and physical capacity saturation unchanged;
- for native `statusIcons.paddingEnd`, measure only the projected Guiyuan extent on the **peer side** of the live QS_FAKE end edge;
- define that edge as logical `x = 0` and reserve from `0` leftward to the left-most current projected span, floored by the runtime compact width and capped by semantic reservation;
- ignore any projected span extent at `x > 0` because it is on the end side and cannot collide with native peers.

All geometry inputs remain runtime-derived from the actual View hierarchy. No device width, Battery width, island translation or pixel compensation is hard-coded.

## Validation state

Candidate identity: `0.0.5` / versionCode `261003682` / Build `20261003-682`.

Submission review:
- one existing transition-reservation writer remains;
- no translation/alpha/visibility writer, Hook, timer, Handler or animator is added;
- ordinary-island and no-island branches remain on semantic reservation;
- Session stop, endpoint replacement, view detach, panel runtime failure, Battery-island frame-loss fail-native and Hot Reload cleanup remain unchanged;
- reverse pull uses the same stateless live-progress intrusion calculation.

Automated gate:
- exact-head Runtime CI for PR #200.

Required device gate after Runtime success:
1. charging island, slow early/mid pull: VPN/mute/native peers remain adjacent to the visible Guiyuan peer-side envelope;
2. charging island, near-full/full pull: no large empty fake-row gap before final appearance handoff;
3. reverse pull: spacing retraces continuously with no flash or stale reservation;
4. charging island + dual SIM: both mobile targets remain available;
5. ordinary island and no-island pull remain unchanged;
6. no `fake-carrier-capacity-insufficient` or new fail-native loop.

Runtime is frozen once the exact-head signed Canary is produced for this gate.
