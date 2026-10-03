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

## Build 680 candidate

Build 680 keeps every accepted ownership boundary and corrects only the Battery-island peer-spacing frame:

- logical Guiyuan reservation remains the frozen-final-total-width interpolation and is unchanged;
- ordinary-island and no-island paths remain unchanged;
- while native Battery island is active, each frozen target span is projected into the **current live QS_FAKE end frame** using the measured end offset between `finalBattery` and `fakeStatusIcons`;
- the existing per-span union is then evaluated at the same native progress and remains bounded by the semantic reservation;
- HyperOS still owns fake-root translation, Battery-island motion, appearance and collision. Guiyuan does not read or rewrite `batteryWidthDiff`; the live end-frame measurement inherits that native motion instead;
- Build 677 capacity saturation and the sole `statusIcons.paddingEnd` writer remain unchanged.

No new Hook, timer, animator, polling loop, fixed pixel compensation, alpha/visibility/translation writer, or second island authority is introduced.

Submission review also verifies Session stop, endpoint replacement, view detach, runtime failure and Hot Reload cleanup. Reverse pull reuses the same live progress/end-frame sampling rather than a second reverse path.

## Validation state

Candidate identity: `0.0.5` / versionCode `261003680` / Build `20261003-680`.

Automated gate:
- exact-head Runtime CI for PR #200.

Required device gate after Runtime success:
1. charging island, slow early/mid pull: native peers remain adjacent to the currently drawn Guiyuan envelope instead of the Build-679 expanding gap;
2. charging island, full pull and reverse: spacing remains continuous with no overlap, native takeover or reverse flash;
3. charging island + dual SIM: both mobile targets remain available and converge normally;
4. ordinary island and no-island pull remain unchanged;
5. no `fake-carrier-capacity-insufficient` or new fail-native loop;
6. if spacing is still abnormal, export Detailed Diagnostic and inspect `nativePeerTargetEndOffset` beside `nativeReservation`.

Runtime is frozen once the exact-head signed Canary is produced for this gate.
