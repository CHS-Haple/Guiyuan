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

## Build 679 candidate

Build 679 separates **logical occupancy** from **charging-island native-peer proximity**:

- Guiyuan logical reservation remains the accepted frozen-final-total-width interpolation from raw HyperOS expansion progress. Latent participant reveal and transition geometry still consume that semantic reservation.
- Ordinary-island native peer reflow remains unchanged.
- Only while the native Battery island is active, the native QS_FAKE peer reservation is derived from the **current union of the same frozen source/target spans at the same raw native progress**, bounded by the logical semantic reservation.
- This prevents native peers from pre-reserving future latent width before that width exists in the current projection.
- Build 677 physical-capacity saturation remains the final safety guard.
- Home steady peer membership remains the sole island hide authority; the fake row still mirrors that result instead of running a second island collision algorithm.

No custom island rectangle/collision, per-peer geometry writer, new animator/timeline, delay, polling, alpha/visibility/translation write, or fixed spacing constant is introduced.

## Validation state

Candidate identity: `0.0.5` / versionCode `261003679` / Build `20261003-679`.

Automated gate:
- exact-head Runtime CI for PR #200.

Required device gate after Runtime success:
1. charging island, early/mid pull: VPN/mute/native peers stay visually adjacent to the currently visible Guiyuan envelope instead of opening the Build-678 empty gap;
2. charging island, full pull and reverse: no `fake-carrier-capacity-insufficient`, no native takeover, no overlap;
3. charging island + dual SIM: both mobile targets remain available and converge normally;
4. ordinary island: accepted Home-steady mirror behavior remains unchanged;
5. no-island pull remains unchanged;
6. export one Detailed Diagnostic if any spacing anomaly remains.

Runtime is frozen once the exact-head signed Canary is produced for this gate.
