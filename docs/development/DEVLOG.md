## 2026-10-06 — Guiyuan 0.2.1 stable promotion complete

**Type:** stable promotion / repository state
**Display version:** 0.2.1
**Build:** 746 / `20261006-746`
**Accepted runtime baseline:** Build 744

### Promotion result

PR #226 promoted the accepted `dev` state to `main` as Guiyuan 0.2.1 after the dev-to-main Full validation boundary passed. This is a maintenance checkpoint over 0.2.0, not a new architecture or feature phase.

Build 746 changes version/release metadata only over the accepted Build 744 runtime. The rejected Build 745 alpha-layer experiment remains closed and unmerged.

GitHub automatically removed the long-lived `dev` branch when the dev-to-main PR merged. Per repository policy, `dev` was immediately recreated at the promoted `main` commit before further development. This closeout commit is then applied identically to `main` and `dev` so current-state documentation remains synchronized.

### Validation

- Build 746 trusted `dev` validation #2785 passed, including signed Canary/signature and non-debuggable checks;
- README 0.2.1 consistency PR #225 Light validation #2786 and integrated `dev` #2787 passed;
- stable promotion PR #226 Full validation #2788 passed;
- CodeQL / code-scanning checks on the promotion boundary passed;
- no `v0.2.1` tag or existing stable Release conflicted with the promotion.

No runtime behavior, dependency, compatibility target, renderer, transition geometry, native writer, or Build identity changes in this closeout.

## 2026-10-06 — Pre-release consistency follow-up: pinned target contract coverage

**Type:** repository / compatibility contract / CI audit  
**Runtime baseline:** Guiyuan 0.2.0 / Build 744 (`20261006-744`)  
**Runtime behavior:** unchanged

### Audit finding

The repository consistency pass found two current-state drifts after #221 merged:

- `CURRENT.md` still described `dev` as Build 743 and PR #221 / Build 744 as an active candidate awaiting validation/merge;
- the pinned HyperOS profile still covered the earlier hook surface even though current runtime source also consumes exact-target Keyguard/AOD and Control Center lifecycle callbacks.

Build 744 has already passed its exact-head Runtime CI, signed Work-branch Canary, focused device validation, and integrated `dev` Runtime CI #2779. The previous Keyguard + active-island ownership gap is therefore no longer carried as an open blocker. Parallel PR #224 / Build 745 is a separate compositing-cost candidate based on a concrete `saveLayerAlpha` code path; it does not reopen the closed ownership defect and remains subject to its own Runtime CI, Canary, and focused device gate.

### Exact-target contract coverage

The follow-up keeps the existing SystemUI artifact identity `a0e738e41fe599b97950cbf52a9e2ddc6ae2ceff986efbacb1c9840bea78768d` and adds only contracts already recorded against that exact artifact in SystemUI-Reference:

- `MiuiBatteryMeterView.setIsAodAnimate(boolean): void`;
- `MiuiBatteryMeterView.toggleAodMode(boolean): void` plus `mToAod`, `mIsAodAnimate`, and `mAnimToAod`;
- `KeyguardStatusBarViewControllerInject.animateFullAod(boolean, boolean): void`;
- `MiuiKeyguardStatusBarView.animateIconContainer(boolean): void`;
- `ControlCenterHeaderExpandController$controlCenterCallback$1.onExpansionChanged(float): void`;
- `ControlCenterHeaderExpandController$controlCenterCallback$1.onAppearanceChanged(boolean, boolean): void`.

`tools/verify_target_profile.py` cross-checks these profile entries against the corresponding runtime source constants, including the already-pinned Control Center visibility callback. CI can therefore fail when those class/method identities drift instead of validating an obsolete profile surface.

`ControlCenterFakeStatusIcons.onAttachedToWindow()` remains a uniquely runtime-resolved seam and is intentionally **not** promoted to the static profile until the exact-artifact reference index records that method explicitly. The audit records the gap rather than fabricating static verification.

### Boundary

Established internal `CombinedStatus*` preference names, Hook IDs, diagnostics identities and build-property keys remain compatibility identities, not branding defects. Low-value implementation-only leftovers are not sufficient reason to create a runtime checkpoint.

No Xposed/SystemUI behavior, renderer, animation, geometry, alpha/visibility writer, fail-native path, dependency version, external version, internal Build identity, or Canary artifact changes in this follow-up. Repository-selected CI is sufficient; no device gate is introduced by this audit.


## 2026-10-06 — pre-release-grade repository consistency audit

**Type:** repository/documentation consistency maintenance
**Runtime baseline:** Guiyuan 0.2.0 / Build 743 (`20261006-743`)
**Branch:** `fix/pre-release-consistency-audit`

### Scope

This is a pre-release-grade audit standard applied while Guiyuan remains in active development. It is not a release-candidate freeze, a 1.0.0 qualification pass, or a runtime promotion.

### Findings corrected

- public README still advertised the promoted line as 0.1.0 and described AOD as native-only even though the accepted runtime family owner supports independently gated AOD;
- architecture/roadmap/reference documents still presented Build 537/625 Keyguard/AOD work as current candidates or pending validation, risking restoration of superseded ownership routes;
- layout policy referenced the removed `CombinedStatusHomeLayoutResolver` symbol instead of the current `HomeLayoutResolver -> LayoutPolicy` chain;
- privacy documentation still named the old `Download/CombinedStatus` share directory while runtime writes `Download/Guiyuan`;
- third-party notices still named Gradle 9.7.1 while the checked-in wrapper is 9.8.0;
- the security-advisory link still pointed to the former CombinedStatus repository;
- the bug-report version placeholder was tied to obsolete 0.0.3.

### Review boundary

Historical CHANGELOG/DEVLOG facts remain untouched. Established internal `CombinedStatus*` preference, hook, diagnostic, and compatibility identities are not treated as branding defects. Runtime code, Build 743 behavior, version identity, CI workflow behavior, and the active Keyguard-island performance investigation remain unchanged.

Validation for this branch is repository-selected Light validation; no Canary or device gate is required unless later edits cross into runtime/build surfaces.

## 2026-10-04 — Build 685: reconcile AOD family lifecycle with QS_FAKE recovery

**Type:** integration / lifecycle + Control Center recovery reconciliation  
**Parents:** Build 672 dev AOD lifecycle + Build 684 QS_FAKE recovery/performance checkpoint  
**Build:** 685 / `20261004-685`

### Integration strategy

- Preserve the full #196 Home/Keyguard/AOD state machine, same-host Keyguard-family renderer retargeting, AOD presentation claims, source reconciliation and feature-toggle lifecycle.
- Preserve Build 676-684 QS_FAKE peer mirror, fake-carrier capacity lease, Battery-island peer-side reservation, fail-native frame-loss cleanup, visible-cycle lease release and hot-path diagnostic gating.
- Where both branches touched the same Control Center lifetime, keep Build 684's stricter hidden ordering: clear transition reservation before suppressing/releasing the fake-carrier lease.
- In shared presentation sessions, keep #196 deferred-native-layout ownership and Build 684 fake-carrier suppression as independent guards.
- No alpha/translation/visibility writer, timer, custom gesture clock, hardcoded device geometry or second presentation owner is added.

### Pre-merge audit

- Keyguard and AOD share one renderer/session on the verified family host; retargeting does not restore/reacquire represented slots between same-host scenes.
- Family fail-native restores the native presentation inside the owner before notifying the module.
- Feature disable, child-toggle disable, resolver loss, stable Home return and Hot Reload all have explicit cleanup paths.
- AOD remains ineligible as a Control Center source.
- Keyguard Control Center lease is bounded by source/readiness/fraction and is released on authoritative Home.
- Build 682 charging-island peer geometry and Build 683 visible-cycle capacity lifecycle remain unchanged.
- Remaining performance risks (per-frame native padding/layout, repeated tint scan, per-component alpha layers) are tracked for the post-integration audit and are not changed in this merge.

### Pre-merge audit follow-up

Before merging Build 685 into dev, the combined tree was re-audited across Home, Keyguard, AOD and Control Center lifecycle boundaries.

Fixes made during the audit:
- resume deferred native layout ownership when a same-host family retarget leaves the visual-only Keyguard boundary;
- reject stale `Active/Prepared` results when synchronous fail-native has already removed the owning session;
- stop same-callback Control Center reacquisition after visible-cycle rearm fails;
- restore the full accepted Build 682 capacity-bounded reservation calculation and its capacity-requirement policy after three-way reconciliation exposed that only the diagnostic/test side had initially been carried over.

Validation:
- dev-only AOD/Keyguard APIs, state sources, resolver and same-host family owner are preserved;
- 684-only QS_FAKE peer mirror, 4th island hook, capacity lease, fail-native API and diagnostic gate are preserved;
- bidirectional incremental audit found no lost dev/684 statements in Module, ControlCenterRenderSession, settings UI, strings or core tests (intentional strengthened statements excluded);
- Runtime CI #2575 passed the full runtime test/build chain on Build 685.


## 2026-10-04 — Build 684: bound Control Center hot-path diagnostics

**Type:** low-risk performance / observation-only hot-path cleanup  
**Branch / PR:** `fix/qs-fake-native-source-sync` / #200  
**Build:** 684 / `20261003-684`

### Problem

Build 683 is visually accepted and its visible-cycle lease lifecycle is healthy, but the pull-down hot-path audit found diagnostics executing in the same synchronous SystemUI expansion/layout path: per-reservation success event construction and full QS_FAKE peer traversal/formatting after layout.

### Change

- pass the existing runtime Detailed-diagnostics gate into the Control Center native-presentation session;
- suppress per-frame reservation success diagnostics while a transition reservation is active;
- suppress full native-peer source snapshots while a transition reservation is active;
- keep detailed idle/lifecycle snapshots after the transition closes;
- replace a per-call `setOf(...)` diagnostic membership allocation with direct comparisons.

### Safety boundary

No change to `statusIcons.paddingEnd` values/write cadence, fake-carrier capacity, Build 682 Battery-island geometry, TransitionDrawable rendering, tint sampling/authority, endpoint/source-witness resolution, peer mirror/clip ownership, or lifecycle/fail-native cleanup.

Higher-risk candidates—layout-write cadence, alpha-layer grouping, tint caching and endpoint caching—are deliberately deferred until after this low-risk A/B.

## 2026-10-04 — Build 683: scope QS_FAKE capacity lease to visible cycle

**Type:** Control Center lifecycle hardening after Build 682 device acceptance  
**Branch / PR:** `fix/qs-fake-native-source-sync` / #200  
**Build:** 683 / `20261003-683`

### Evidence

Build 682 device video is visually accepted. Its detailed diagnostic shows transition reservation clears at the hidden boundary, but the QS_FAKE fake-carrier width lease remains applied afterward. This is not a current visible defect, but it leaves a stale native-width baseline across hidden lifecycle changes.

### Change

- port only the established visible-cycle lease concept from latest dev; do not merge unrelated AOD runtime behavior;
- strengthen the hidden boundary for this recovery branch: clear transition reservation before suppressing native layout writes, so an abrupt `visible=false` cannot retain expanded peer padding even if HyperOS skips a fraction-zero sample;
- release the fake-carrier capacity lease while keeping compact QS_FAKE presentation prearmed;
- if native hidden geometry already changed, adopt the live width rather than overwrite it;
- cache only that live native baseline for the next visible cycle, then re-establish the lease from runtime geometry;
- add diagnostics for lease suppression and pending native width;
- keep Build 682 peer-side reservation semantics and capacity saturation untouched.

### Lifecycle review

Reviewed before commit:
- hidden order is reservation clear -> lease suppression -> lease release;
- repeated hidden callbacks are idempotent;
- visible re-entry unsuppresses before re-establishing runtime reservation;
- hidden native width replacement is adopted without writing over it;
- pending baseline only bridges the release/re-layout window and is cleared after a normal full stop;
- host detach, host replacement, fail-native, feature disable and Hot Reload retain their existing full restore paths;
- no new translation/alpha/visibility writer, timer, Handler, animator or fixed geometry is introduced.

## 2026-10-02 — Build 617 off-center Wi-Fi badge ring avoidance

**Type:** Battery-ring optical geometry  
**Display version:** 0.0.4  
**Build:** 617 / `20261002-617`  
**Branch:** `fix/wifi-ring-shape-avoidance`

### Device/preview evidence
Preview Sandbox hotspot and no-internet states showed the right-side native badge approaching the battery-ring right shoulder even though ordinary Wi-Fi component-aware avoidance was already improved.

Preview uses the real HyperOS hotspot/unavailable drawable resources and the same `CombinedStatusPainter` native optical probe as runtime, so there is no separate preview-only badge overlay to patch.

### Root cause
`CombinedStatusBatteryTopArcPolicy.resolveGap()` retained an old early-return condition for content wholly left or right of `ringCenterX`. That assumption was valid only for centered text/Wi-Fi envelopes. A disconnected hotspot-link or no-internet badge can be entirely right of center, so its optical component was probed correctly but then discarded by the gap policy.

### Correction
- Preserve the accepted center-crossing gap formula unchanged.
- For components wholly left/right of center, derive the exact angular intervals where the ring centerline lies inside the clearance-expanded component rectangle.
- Merge that interval with the central Wi-Fi component intervals.
- Add symmetric left/right badge tests and a regression asserting a right badge extends only the right shoulder.

### Boundaries
No badge-specific shrink/expand factor, fixed gap angle, resource-name special case, new renderer path, animation ownership, or SystemUI writer.

## 2026-10-02 — Build 616 profile defaults and top-information offset ownership

**Type:** Visual defaults / layout-profile geometry / companion UI  
**Display version:** 0.0.4  
**Build:** 616 / `20261002-616`  
**Branch:** `fix/wifi-ring-shape-avoidance`

### Maintainer direction
- Network centered battery-number default: 120%.
- Battery centered mobile-type default: 80%.
- Battery centered battery-number default: 140%.
- Rename the old battery-information vertical offset to Top information vertical offset and move the control into the Global UI section.
- Offset target depends on layout: number + charging glyph in Network centered, network content in Battery centered.

### Implementation
- `batteryTopTextUiScaleDefault(layout)` is now 1.2 / 1.4 for Network/Battery centered.
- Added layout-aware `mobileTypeSizeScaleDefault(layout)`: 1.0 for Network centered and 0.8 for Battery centered.
- Existing saved profile overrides remain authoritative; only missing/reset defaults change.
- Kept the historical persisted vertical-offset key to avoid a settings migration.
- Added a layout-aware top-info offset policy:
  - Network centered forwards the raw accepted readout offset and leaves network translation unchanged.
  - Battery centered pins readout offset to its baseline and converts the same user-facing offset into network-top translation.
- Ring avoidance, top-overflow calculation and transition source geometry share that network translation.
- Moved the slider out of the battery AnimatedPreferenceGroup into the Global section; it is always visible and remains profile-scoped.

### Boundaries
No new animation/timer, state source, polling, native writer, migration rewrite or geometry fudge factor.

## 2026-10-02 — Build 615 size ranges, mobile-type overall scale, and visual-update serialization

**Type:** Runtime visual geometry / settings transport  
**Display version:** 0.0.4  
**Build:** 615 / `20261002-615`  
**Branch:** `fix/wifi-ring-shape-avoidance`

### Requested sizing changes
- Overall minimum: 60%.
- Wi-Fi minimum: 40%.
- Mobile-type minimum: 40%.
- 5G/5GA must follow overall size.

The previous UI stopped Wi-Fi/mobile type at 80%, while `CombinedStatusCenterGeometry` independently kept a hidden 70% renderer floor. Both are removed in favor of one shared settings range. Slider cadence remains 5%.

### Mobile-type root cause
Production `resolveMobileTypeLayout()` divided mobile-type text/suffix geometry by the full Canvas scale. The Canvas scale already included `combinedScale`, so that division cancelled the user's overall-size setting. Build 615 compensates only the host viewport portion: physical 5G/5GA size now follows `combinedScale` while remaining stable across host viewport scale.

### Transient presentation loss evidence
Maintainer reported one transient loss of Guiyuan while adjusting overall size on Build 612; disabling/re-enabling the feature restored it. The supplied diagnostic showed Build 612 Canary, a recovered combined Home presentation, and later healthy battery/Wi-Fi model-to-draw events; it contained no Java/Kotlin exception in the retained log window.

Code review found an unsafe asymmetry:
- feature preferences explicitly dispatch to the SystemUI main thread before presentation mutation;
- visual preferences did not, although their callback immediately updates RenderViews, manual layout, and end reservation.

Build 615 gives visual preferences the same main-thread ownership boundary and coalesces stale queued slider snapshots. This is root-cause hardening, not a retry/timer repair. Diagnostics now record scale values and `mainThread=true`.

### Boundaries
No polling, delayed retry, geometry fudge factor, duplicate SystemUI writer, or animation ownership change.

## 2026-10-02 — Build 614 component-aware Wi-Fi ring avoidance

**Type:** Runtime visual geometry / battery-ring avoidance  
**Display version:** 0.0.4  
**Build:** 614 / `20261002-614`  
**Branch:** `fix/wifi-ring-shape-avoidance`

### Root cause
Battery-center Wi-Fi already used the actual native drawable for rendering and optical sizing, but top-ring avoidance kept only the union optical envelope. A Wi-Fi glyph is layered/disconnected: the wide upper arc, narrower middle arc, and compact lower arc leave large empty envelope corners. Treating that entire rectangle as occupied made the ring opening visibly wider than necessary. Numeric readout avoidance did not show the defect because text is close to rectangular.

### Change
- Preserve the drawable probe's disconnected optical components in the native center-asset cache.
- Map components through the same resolved draw width/height and center as the actual native glyph.
- Carry those components through top-slot appearance scaling and vertical translation.
- Compute the required top-ring gap per component and merge the resulting angular intervals.
- Use individual fallback Wi-Fi path bounds when a native resource is unavailable.
- Keep the existing 2f visual clearance, ring-stroke clearance, Wi-Fi size, and single-envelope path for text/non-Wi-Fi content.

### Boundaries
No screenshot-derived shrink factor, hard-coded Wi-Fi gap angle, new state source, SystemUI writer, animation/timing change, or numeric-readout geometry change.

### Validation
Focused unit coverage compares a layered Wi-Fi-like shape against its union envelope and verifies that component-aware avoidance reduces only empty-corner reservation. Exact-head Runtime CI and signed Canary are required before the focused device check.

## 2026-10-02 — Build 612 device acceptance and transition-capacity closure

**Type:** runtime acceptance / transition geometry ownership  
**Display version:** 0.0.3  
**Build:** 612 / `20261002-612`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Evidence
- Exact-head Runtime run 36962917018 succeeded at `0f8128c5e2cf8ad1c8715acf7aa776a0ad09ef2a`.
- The maintainer reports the latest Build 612 device pass has no anomaly.
- Build 611 had fixed the late QS_FAKE native-peer underflow with one bounded fixed carrier-capacity lease, but the leased leading width was accidentally sampled as transition motion and produced the all-scene initial left jump.

### Conclusion
The capacity lease and transition motion are separate contracts. QS_FAKE may expose already-unused parent width for native measurement, but lease-only leading capacity must not move the Guiyuan motion carrier. Build 612's end-anchored logical carrier projection preserves the frozen native source-carrier width for motion while keeping the fixed capacity lease for peer layout.

### Acceptance
- No new native translation/alpha/visibility writer, animator, delay, polling path, or target-geometry compensation was introduced.
- LTR/RTL logical-carrier projection is unit-covered.
- The Build-612 device result closes the Build-611 left-jump regression without reopening the Build-609 peer-underflow defect.
- No additional Canary/device gate is required for the documentation-only closure.

## 2026-10-02 — Build 595 embedded MIUIX FAB visual in Add Card

**Type:** App UI / MIUIX interaction visual  
**Display version:** 0.0.3  
**Build:** 595 / `20261002-595`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Maintainer direction
Restore the previous blue FAB / white plus visual, but embed it in the center of the full clickable white Add Card so Card Tilt moves the entire surface and visual together. Remove the FAB shadow.

### Change
- Full white Add Card remains the sole click owner and uses `PressFeedbackType.Tilt`.
- Center visual uses MIUIX FAB metrics: 60dp circle, primary background, white Add glyph.
- Limit state reuses the prior MIUIX disabled FAB semantic colors.
- Center visual has no independent click behavior.
- Shadow elevation is fixed to 0dp in both enabled and disabled states.
- Remove the temporary custom-drawn gray circle/plus and limit-copy treatment.

### Validation
Run exact-head Runtime CI and Work Branch Canary. Device review should verify the embedded visual moves with the Card tilt, has no floating shadow, and disabled state is as clear as the previous FAB.

## 2026-10-02 — Build 594 Canvas composable-context correction

**Type:** App UI compile fix  
**Display version:** 0.0.3  
**Build:** 594 / `20261002-594`  
**Branch / PR:** `feat/battery-top-readout` / #181

### CI evidence
Build 592 Runtime CI #2163 failed Kotlin compilation at the Add-card Canvas because a composable MIUIX color token was read from inside the non-composable draw lambda. Build 593 inherited the same issue.

### Root cause
`MiuixTheme.colorScheme.disabledOnSecondaryVariant` was resolved inside `Canvas { ... }`.

### Change
Resolve the plus foreground color in composable scope before Canvas, then capture the resulting `Color` inside the draw lambda.

### Validation
No visual or behavioral change. Re-run exact-head Runtime CI and Canary.

## 2026-10-02 — Build 593 explicit custom-style limit state

**Type:** App UI / MIUIX disabled-state feedback  
**Display version:** 0.0.3  
**Build:** 593 / `20261002-593`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Maintainer correction
The full-card Add interaction replaced the previous FAB state change, so reaching the five-custom-style limit needed equally clear visual feedback.

### Change
- Available state: white Add Card remains fully clickable with native `PressFeedbackType.Tilt`, gray 60dp circle and thick white plus.
- Limit state: click/Tilt disabled, center circle and plus use MIUIX disabled semantic tokens.
- Reuse existing localized limit copy below the icon: `已达自定义上限 · 5/5` / `Custom style limit reached · 5/5`.
- Card geometry and measured-height synchronization do not change between available/limit states.

### Retained Build-592 behavior
Shared 84% Sheet height, equal title-row height, tightened header/navigation spacing, 32dp arrow press geometry/tooltips, deferred built-in-template naming flow, measured Add/settings Card height equality, and Home preview Tilt feedback.

### Validation
Run exact-head Runtime CI and Work Branch Canary. Device acceptance should confirm the Add Card has a clearly distinguishable limit state without changing its size or alignment.

## 2026-10-02 — Build 592 battery scheme interaction and geometry pass

**Type:** App UI / MIUIX interaction / layout geometry  
**Display version:** 0.0.3  
**Build:** 592 / `20261002-592`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Maintainer direction
- Keep the shared 84% screen-height sheet policy.
- Make level-1 and level-2 title rows identical in height.
- Tighten scheme title→preview and navigator→content spacing.
- Make arrow press geometry match its visible 32dp size and add long-press help.
- Built-in mode editing must first move to Add, request a custom name, then create and enter the requested mode editor.
- Add page must use a same-height full-card MIUIX clickable large Card with touch-point Tilt feedback.
- Add Card center: gray circle + thicker white plus.
- Home preview sandbox large Card should also use MIUIX Tilt feedback.

### Changes
- Level-1 start-action slot reserves `IconButtonDefaults.MinWidth/MinHeight`, matching the native level-2 Back IconButton row.
- Shared `BatterySchemeHeader` title→preview spacing: 12dp → 8dp.
- Fixed navigator→pager spacing: 8dp → 4dp.
- Previous/next controls are single-layer 32×32dp MIUIX IconButtons with matching press surface and MIUIX TooltipBox help.
- Built-in mode click animates to `pages.lastIndex` before naming; confirmation copies the selected built-in source and opens the requested slot editor.
- Ordinary six-row white settings Card reports its actual measured height; Add-page clickable white Card consumes that same height rather than relying on a guessed fixed size.
- Add Card uses `PressFeedbackType.Tilt` only; the whole Card including the centered gray circle / white plus tilts from the press point.
- Home preview sandbox Card uses `PressFeedbackType.Tilt`.

### Review
All requested invariants were rechecked at branch HEAD: shared 84% sheet height, equal title-row height, 8dp scheme title→preview gap, 4dp navigator gap, 32dp navigation controls, tooltip copy, deferred built-in customization flow, Tilt Add Card, measured Card height sync, and Home preview Tilt.

### Validation
Run exact-head Runtime CI then Work Branch Canary. Device review should focus on title-row equality, arrow press footprint/tooltips, Add Card height/bottom alignment and Tilt behavior, centered plus visual, and built-in-template creation flow.

## 2026-10-02 — Build 591 full battery-color layout review

**Type:** App UI / MIUIX conformance / layout ownership  
**Display version:** 0.0.3  
**Build:** 591 / `20261002-591`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Review scope
Re-audit the complete first-level scheme page, Add page, fixed navigator, secondary editor and two-level Sheet geometry against pinned MIUIX components and current maintainer requirements.

### Findings and fixes
- Ordinary/custom/Add pages duplicated title/preview geometry. They now share one `BatterySchemeHeader`.
- Scheme title remains `MiuixTheme.textStyles.title2`, matching MIUIX Card guidance.
- Title is center-aligned with symmetric native IconButton-width safe areas, single-line ellipsis and a trailing More action at the same vertical baseline.
- All functional Cards are explicitly full-width to prevent content-measurement-dependent edge drift.
- Add FAB returns to upstream default elevation; its disabled substitute references the upstream FAB size constant.
- HEX input explicitly fills the white precise-input Card so its edge aligns with the RGB row.
- Apply/Applied remains the complete MIUIX Snackbar action pill: native TextButton, 26dp minimum metrics, action-pill radius/margins, 15sp and primary semantic colors.

### Confirmed native ownership
- OverlayBottomSheet title/back geometry and title typography.
- Card 16dp radius and semantic container colors.
- ArrowPreference / OverlayDropdownPreference / BasicComponent spacing, title/summary typography and action colors.
- SmallTitle default 14sp Bold and 28dp/8dp inset.
- FAB default 60dp geometry and 4dp elevation.
- Sheet height is owned once by the shared OverlayBottomSheet and is identical at both levels.

### Project-owned visuals retained
Only where pinned MIUIX has no exact component or the maintainer explicitly requested it: scheme preview swatches, custom pager indicator, optically smaller visible navigator surface inside the native 40dp hit target, enlarged Add glyph, and page spacing.

### Validation
Run exact-head Runtime CI and Canary. Device review should focus on optical centering, custom-title/Menu alignment, Add/ordinary top baseline, full-width Card edges, true action-pill geometry, and identical first/second-level Sheet height.

## 2026-10-02 — Build 590 complete MIUIX action-pill Apply control

**Type:** App UI / MIUIX pill conformance  
**Display version:** 0.0.3  
**Build:** 590 / `20261002-590`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Maintainer direction
Use the complete MIUIX pill specification for the Apply action, including shape, width behavior and internal metrics.

### Pinned MIUIX source
The Snackbar action pill uses:
- `TextButton`;
- `minWidth = 26.dp`;
- `minHeight = 26.dp`;
- `SnackbarDefaults.ActionCornerRadius`;
- `SnackbarDefaults.ActionInsideMargin` = 12dp horizontal / 0dp vertical;
- `TextStyle(fontSize = 15.sp)`;
- primary semantic action colors.

Its width is content-driven, not fixed.

### Change
- Replace the 120dp-width scheme Apply Button with the exact pill geometry above.
- Remove the project-owned width and generic Button height.
- Keep Apply/Applied enabled state semantics and native MIUIX interaction.

### Review
No custom pill width/height/radius remains. No other layout, hierarchy, Runtime/SystemUI, hook, or persistence change.

### Validation
Run exact-head Runtime CI and Canary; verify the Apply/Applied action reads as a true MIUIX capsule and sizes naturally to its label.

## 2026-10-02 — Build 589 true MIUIX pill Apply button

**Type:** App UI / MIUIX button geometry  
**Display version:** 0.0.3  
**Build:** 589 / `20261002-589`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Maintainer correction
The Apply action must be a true capsule/pill, not merely a more-rounded rectangle.

### Root cause
Build 588 used `ButtonDefaults.MinHeight * 0.5f` as the radius. `MinHeight` is only the component minimum; native Button content padding can make the actual rendered height larger, so a 20dp radius does not guarantee a pill silhouette.

### Pinned MIUIX precedent
Pinned MIUIX Snackbar action pills use an explicit 50dp action corner radius. The squircle renderer also supports capsule/pill degradation at large corner radii.

### Change
- Keep native MIUIX `Button`.
- Keep primary semantic colors, native text style and interaction.
- Keep 120dp minimum width.
- Set `cornerRadius = 50.dp` so the control remains visually capsule-shaped regardless of its final measured height.

### Validation
Run exact-head Runtime CI and Canary. Device acceptance: Apply/Applied must read as a genuine pill.

## 2026-10-02 — Build 588 secondary gray-Sheet hierarchy and locked two-level height

**Type:** App UI / MIUIX hierarchy / sheet geometry  
**Display version:** 0.0.3  
**Build:** 588 / `20261002-588`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Maintainer direction
- Keep the overview as a white Sheet with gray scheme Cards.
- Make the secondary editor Sheet itself gray.
- Remove the secondary gray outer Card.
- Put Color source in a white Card; keep Common colors / Full adjustment / Precise input in white Cards.
- Preserve the original secondary spacing and component sizing.
- Level 1 and level 2 must always have exactly the same Sheet height.

### Change
- Secondary `OverlayBottomSheet.backgroundColor` uses `MiuixTheme.colorScheme.surface`; overview uses `background`.
- Remove the detail page's redundant gray outer Card.
- Wrap `OverlayDropdownPreference` for Color source in a native default Card.
- Restore original detail spacing: 24dp bottom scroll padding, 16dp function-card padding, 12dp common-color row gap, native BasicComponent spacing.
- Introduce one shared `BATTERY_COLOR_SHEET_HEIGHT_FRACTION = 0.84f`; the single shared `OverlayBottomSheet` owns this height for both Pager pages.

### Review
- Level 1/2 height ownership is singular; page content has no independent sheet-height modifier.
- Only background/title/start action vary between levels.
- MIUIX semantic hierarchy: gray `surface` page layer + white `surfaceContainer` functional Cards.
- No Runtime/SystemUI/hook/persistence changes.

### Validation
Run exact-head Runtime CI and Canary. Device acceptance should verify identical Sheet top edge/height while switching levels, gray detail Sheet, white source/function Cards, and unchanged internal spacing.

## 2026-10-02 — Build 587 battery-color sheet geometry and density pass

**Type:** App UI / MIUIX layout / pager geometry  
**Display version:** 0.0.3  
**Build:** 587 / `20261002-587`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Device evidence
Build 586 showed:
- excessive gap between the sheet title and fixed scheme navigator;
- visually oversized previous/next controls;
- ordinary and Add scheme pages starting at different vertical positions;
- Add-page preview strip not optically centered;
- adjacent gray scheme Cards touching during horizontal transitions;
- mode-detail layout too tall and visually loose.

### Root causes
- Both the two-level navigation Pager and the scheme Pager inherited Compose Pager's centered vertical alignment.
- Preview strips were content-width Rows placed in a start-aligned Column.
- The visible navigation surface occupied the full MIUIX IconButton touch target.
- Scheme pages had zero page spacing.
- Overview/detail sheet height still depended on natural page content.
- Secondary editor used generous default grouping rhythm on top of the gray outer Card.

### Change
- Apply one 84%-window-height modifier to the shared OverlayBottomSheet so both levels use the same taller sheet.
- Top-align both Pagers.
- Scheme Pager page spacing: 12dp.
- Center ordinary/Add preview strips explicitly in full-width Boxes.
- Preserve native MIUIX 40dp IconButton interaction geometry but render a 32dp semantic surface and 18dp chevron; navigator-to-indicator spacing is 12dp.
- Compact secondary editor: gray/function Card vertical padding 12dp, common-row gap 8dp, HSV row vertical padding 10dp, bottom scroll padding 16dp.

### Review
- Sheet title remains outside the gray Card.
- Fixed scheme navigator remains outside the horizontally moving gray Card.
- White setting/function Cards remain nested inside the gray outer Card.
- No literal color values, custom font overrides, Runtime/SystemUI, hook, or persistence changes.

### Validation
Run exact-head Runtime CI and Canary. Device acceptance should focus on title/navigator spacing, arrow optical size, identical Add/ordinary Card top baseline, centered preview strip, page gap during swipe, shared sheet height, and secondary editor density.

## 2026-10-02 — Build 586 compile-only correction

**Type:** App UI compile fix  
**Display version:** 0.0.3  
**Build:** 586 / `20261002-586`  
**Branch / PR:** `feat/battery-top-readout` / #181

### CI evidence
Build 585 Runtime CI #2130 failed in Kotlin compilation at the two new title-row `Modifier.heightIn` calls.

### Root cause
`androidx.compose.foundation.layout.heightIn` was not imported.

### Change
Add the missing import only. No geometry, typography, color token, pager, Runtime/SystemUI, hook, or persistence behavior changed.

### Validation
Re-run exact-head Runtime CI. Canary remains blocked until green.

## 2026-10-02 — Build 585 MIUIX geometry/token audit

**Type:** App UI / MIUIX conformance  
**Display version:** 0.0.3  
**Build:** 585 / `20261002-585`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Scope
Audit Build 584's revised hierarchy against pinned MIUIX `0.9.4-5c91d5e5-SNAPSHOT`.

### Kept upstream-owned
- OverlayBottomSheet default white background and 24dp horizontal inside margin.
- Card default 16dp corner radius.
- White nested setting Cards use default `surfaceContainer/onSurfaceContainer`.
- ArrowPreference / OverlayDropdownPreference / SmallTitle / BasicComponent typography and component spacing.
- IconButton default 40dp minimum size / 40dp corner radius.

### Project-owned only where upstream has no exact component
- Scheme preview swatch geometry.
- Pager indicator geometry because pinned MIUIX has no PagerIndicator component.
- Apply-button width for the HyperOS-style primary action; the control itself remains native MIUIX Button.

### Changes
- Apply Button keeps `ButtonDefaults.MinHeight` and derives pill radius as half that native height; no custom drawing.
- Inactive pager indicator uses `disabledOnSecondaryVariant` rather than a literal alpha from `onSurface`.
- No custom font size/weight/color overrides were added for gray-Card content.

### Validation
Run exact-head Runtime CI and Canary; verify gray outer Card/white inner Card hierarchy, fixed navigator, button geometry, and disabled/active visual states.

## 2026-10-02 — Build 584 fixed scheme navigator ownership

**Type:** App UI / pager ownership / MIUIX navigation  
**Display version:** 0.0.3  
**Build:** 584 / `20261002-584`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Maintainer correction
Horizontal switching should move the complete gray scheme Card only. The previous/next buttons and page indicator must stay fixed above that Card.

### Change
- Hoist `BatterySchemeNavigator` out of individual scheme/Add pages into `BatterySchemeOverview`.
- Keep navigator geometry fixed above the `HorizontalPager`.
- Pager pages now own only the gray outer Card and its nested content.
- Remove `pageCount/pageIndex/onNavigateTo` from scheme and Add page APIs.
- Preserve native MIUIX IconButtons, disabled first/last states, pager spring and Build-583 nested Card hierarchy.

### Review
- Navigation state has one owner: `BatterySchemeOverview`.
- Page content no longer owns or animates navigation controls.
- Only the gray Card participates in horizontal page motion.
- No Runtime/SystemUI/hook/persistence delta.

### Validation
Run exact-head Runtime CI and Canary. Verify navigator remains visually stationary while the gray Card slides, including first/last-page disabled states.

## 2026-10-02 — Build 583 nested battery-color card hierarchy

**Type:** App UI / MIUIX hierarchy / layout ownership  
**Display version:** 0.0.3  
**Build:** 583 / `20261002-583`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Maintainer correction
The intended visual hierarchy is not a gray Sheet. It is:
1. white BottomSheet;
2. one light-gray outer Card containing all battery-color page content;
3. white inner Card(s) containing actual settings/function groups.

### Implementation
- Restore native white `OverlayBottomSheet` background.
- Ordinary scheme pages: one `surface` outer Card owns title, preview, pager navigation, apply action and the nested white settings Card.
- Custom title and More menu share the same minimum 40dp title row; the title remains centered and More is aligned `CenterEnd`.
- Add page uses the same outer Card geometry and title/preview/navigation baselines as ordinary scheme pages; its plus action remains centered in a nested white Card.
- Detail editor: one `surface` outer Card owns the bare source dropdown and section labels; Common colors, Full adjustment and Precise input remain native white nested Cards.
- All colors use MIUIX semantic tokens; no hand-drawn borders or literal RGB values.

### Review
- Card hierarchy matches ownership: page container vs setting/function-group container.
- Typography remains pinned MIUIX `title2`, `SmallTitle`, `BasicComponent`, `body2` and native Button/Preference styles.
- No Runtime/SystemUI/hook/persistence change.
- Build 582 visual hierarchy is superseded and should not be used for device acceptance.

### Validation
Run exact-head Runtime CI and Canary. Validate white Sheet, visible gray outer Card, nested white settings Cards, aligned custom More button, and Add-page geometry parity.

## 2026-10-02 — Build 582 BottomSheet/Card semantic color hierarchy correction

**Type:** App UI / MIUIX semantic color hierarchy  
**Display version:** 0.0.3  
**Build:** 582 / `20261002-582`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Maintainer correction
The settings hierarchy should be a light-gray Sheet with white Cards, not a white Sheet with gray Cards.

### Pinned MIUIX evidence
Revision: `5c91d5e5ce1a2fc7e8bdc1258a881c555102bbca`.
- light `surface = #F7F7F7`;
- light `background = #FFFFFF`;
- light `surfaceContainer = #FFFFFF`;
- dark `surface = #000000`;
- dark `surfaceContainer = #242424`.
`OverlayBottomSheet` defaults to `background`, while `Card` defaults to `surfaceContainer`.

### Change
- Set the battery-color `OverlayBottomSheet.backgroundColor` to `MiuixTheme.colorScheme.surface`.
- Return all function-group Cards to native default `CardDefaults.defaultColors()` / `surfaceContainer`.
- Keep the top source dropdown bare on the Sheet surface.

### Review
- Semantic tokens only; no literal colors.
- Correct light and dark hierarchy.
- No custom borders, typography, Runtime/SystemUI, hook, persistence, or color-policy changes.

### Validation
Run exact-head Runtime CI and Canary. Verify the Sheet is visibly light gray in light mode, Cards are white and clearly bounded, and dark mode retains black Sheet / dark-gray Card separation.

## 2026-10-02 — Build 581 scheme navigation affordance and visible function Cards

**Type:** App UI / MIUIX controls / visual hierarchy  
**Display version:** 0.0.3  
**Build:** 581 / `20261002-581`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Device evidence
Build 580 confirmed two visual problems:
- scheme arrows at the pager indicator read like plain glyphs rather than buttons;
- mode-detail function-group Cards were effectively invisible against the white BottomSheet.

### Root cause
- Pinned MIUIX `IconButton` defaults to a 40dp control with 40dp corner radius but `Color.Unspecified` background. The prior chevrons therefore had correct touch semantics but weak visible affordance.
- `OverlayBottomSheet` uses `MiuixTheme.colorScheme.background` while `Card` defaults to `surfaceContainer`; in the current light palette both resolve to white, collapsing the visual boundary.

### Change
- Keep native `IconButton` geometry and click behavior.
- Use MIUIX semantic container tokens only: `secondaryVariant/onSecondaryVariant` when enabled and `disabledSecondaryVariant/disabledOnSecondaryVariant` when unavailable.
- Keep the first/last page button slots present and disabled so the center indicator does not move.
- Add 24dp separation between each 40dp button and the page indicator for optical clarity.
- Common colors / Full adjustment / Precise input Cards use `CardDefaults.defaultColors(color = surface, contentColor = onSurface)`, producing the intended visible light-gray rounded rectangle against BottomSheet `background` without hand-drawn borders.

### Review
- No custom icon drawing or text-arrow fallback.
- No manual button size/shape override; upstream 40dp / 40dp-radius defaults remain authoritative.
- No pager timing change; pinned `PagerNavigationSpringSpec` remains intact.
- No runtime, hook, persistence, or color-policy delta.

### Validation
Run exact-head Runtime CI and Canary. Verify button affordance/disabled edge pages, stable center alignment, and visible function-group Cards in the mode detail page.

## 2026-10-02 — Build 580 mode-detail MIUIX conformance refinement

**Type:** App UI hierarchy / color editor / MIUIX conformance  
**Display version:** 0.0.3  
**Build:** 580 / `20261002-580`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Maintainer direction
- Source dropdown stands alone: no duplicate gray title and no wrapping Card.
- One actual function group maps to one Card.
- Remove redundant Current color and separate Restore-this-mode entry.
- Common colors need larger optical presence, long-press details, and selection that does not shrink the chosen color.
- HSV must expose live values.
- Follow inversion should look inactive but remain directly editable, promoting to Custom only on a valid edit.
- Prefer pinned MIUIX typography, spacing, tokens and controls before project tuning.

### Pinned MIUIX audit
Revision: `5c91d5e5ce1a2fc7e8bdc1258a881c555102bbca`.

Verified directly:
- `SmallTitle`: subtitle style, 28dp horizontal / 8dp vertical default inset.
- `BasicComponent`: native title/body2 typography, 16dp inset, disabled title/summary tokens.
- `TextField`: native text style, 16dp corners and 16dp internal margins.
- `TooltipBox`: native touch long-press tooltip; already used by Guiyuan Hot Reload.
- `ColorPalette`: selected color remains full-size; a white ring/glow is overlaid instead of shrinking the color body.
- MIUIX has no discrete common-color swatch component, so the visible swatch diameter remains an explicit project optical parameter rather than being presented as an upstream default.

### Changes
- Bare `OverlayDropdownPreference` at the top owns source + effective value.
- Common colors / Full adjustment / Precise input each use their own Card.
- Remove duplicate Current color and Management/Restore group.
- Restore SmallTitle default inset.
- Common colors use 40dp interaction cells, 28dp visible bodies, 16dp Card inset, 12dp row spacing, outer-only selection ring, and `TooltipBox(text = "#RRGGBB")`.
- HSV rows show `degree`, `saturation %`, and `brightness %` values using body2/action-color semantics.
- Follow inversion uses MIUIX disabled visual tokens while preserving interaction. A valid common-color/HEX/RGB/HSV edit continues through `setCustomColor`, which is the single copy-on-write writer for promotion to Custom.

### Review
- MIUIX-first hierarchy/typography/spacing confirmed.
- No second editor state writer.
- No Runtime/SystemUI/hook/persistence-schema change.
- No fake color is created for Follow inversion with no remembered seed.
- The only project-owned optical exception is the discrete swatch geometry because upstream exposes no equivalent component.

### Validation
Run exact-head Runtime CI only. Do not trigger Canary until explicitly requested.

## 2026-10-02 — Build 578 scheme-page vertical overflow correction

**Type:** App UI layout / MIUIX settings-page scrolling  
**Display version:** 0.0.3  
**Build:** 578 / `20261002-578`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Device evidence

Build-577 Canary #630 / run `36922895008` passed exact-head signing/runtime validation. Device screenshot then showed:
- HyperOS Charging row rendered its green preview but no HEX summary;
- the following Low-battery row was absent.

### Root cause

The palette data is correct: HyperOS Charging resolves to `#1DCD3A` and Low resolves to `#FA382E`.

The defect is layout ownership. The scheme overview uses a fixed 455dp inner Pager, while the centered header plus six MIUIX setting rows can exceed that viewport on the real device. Build 577's clipping boundary therefore cut the Charging row between title and summary, then placed Low entirely below the viewport.

This is one defect, not two color-state failures.

### Change

- Make `BatterySchemePageContent` vertically scrollable inside its existing Pager viewport.
- Keep the outer BottomSheet at 520dp and the inner Pager at 455dp.
- Keep MIUIX BasicComponent / ArrowPreference row typography, minimum height, spacing, and interaction unchanged.
- Keep the Pager indicator outside the scrolling page so it remains fixed.
- Do not compress rows, shrink text, enlarge the drawer, or special-case Charging/Low geometry.

### 审查 / review

- root cause is bounded to the scheme-page viewport;
- no color source/palette/persistence change;
- no Runtime/SystemUI/hook change;
- no second sheet, nested vertical owner, timer, or geometry compensation;
- vertical scrolling is page-local and coexists with the non-user-scrollable horizontal Pager.

### Validation

Run exact-head Runtime CI. If green, generate signed Work Branch Canary and verify that Charging shows `#1DCD3A`, Low battery is fully reachable, the Pager indicator stays fixed, and no header/content bleed returns.

## 2026-10-01 — Build 577 mode-detail MIUIX dropdown redesign

**Type:** App UI hierarchy / MIUIX preference semantics / visual consistency  
**Display version:** 0.0.3  
**Build:** 577 / `20261001-577`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Maintainer direction

The mode-detail page should behave as a real settings page inside the existing battery-color drawer. The maintainer explicitly requested:
- smooth level-1 -> level-2 drawer navigation without stacking two independent drawers;
- MIUIX components wherever they exist, with native/HyperOS-style emulation only when MIUIX lacks an equivalent;
- Color source to use a dropdown rather than expanding a full option list inline;
- color chips may keep outlines for background separation, but the outline must be visually consistent at every chip size and on every preceding page;
- submit and run CI only; do not trigger Canary until explicitly instructed.

### Exact MIUIX basis

Pinned revision: `5c91d5e5ce1a2fc7e8bdc1258a881c555102bbca`.

Verified directly:
- `OverlayDropdownPreference` is built on `BasicComponent` and owns:
  - standard title / summary typography and 56dp setting-row geometry;
  - selected-value display;
  - `DropdownArrowEndAction`;
  - pressed/hold-down state;
  - ContextClick haptic on open;
  - Confirm haptic on selection;
  - native `OverlayListPopup` rendering and dismissal.
- The existing internal Pager already uses MIUIX `springAnimateToPage` for programmatic level transitions, so a second nested `OverlayBottomSheet` is unnecessary and would create competing sheet geometry/overlay ownership.

### Changes

#### Single-sheet navigation

- Keep one outer `OverlayBottomSheet` only.
- Level-1 scheme page and level-2 mode detail remain two pages of the internal non-user-scrollable Pager.
- Add `clipToBounds()` to the fixed 520dp Pager viewport so vertically scrolled detail content cannot paint into the sheet title/header area.
- Back/dismiss from detail uses the same MIUIX spring page transition to return to level 1.

#### Color source

- Remove project-owned `sourceExpanded` state.
- Remove the inline expandable `RadioButtonPreference` list and the manually selected ExpandMore/ExpandLess icons.
- Replace them with one native `OverlayDropdownPreference`.
- Source order remains:
  1. HyperOS
  2. iOS
  3. Low saturation
  4. Follow inversion
  5. Custom
- The setting row shows:
  - title = Color source;
  - summary = currently effective HEX / Follow inversion / Not set;
  - start action = current fixed/checker preview;
  - MIUIX-owned selected value + dropdown affordance on the end side.

#### No fake fixed color for Follow inversion

- Remove the old fallback to `MiuixTheme.colorScheme.onSurface`, which caused Follow inversion to silently appear as black in HSV/HEX controls.
- Add a nullable editor seed:
  - active fixed-template color wins;
  - otherwise remembered custom color may be reused as an editing seed;
  - otherwise the editor has no fixed seed.
- With no fixed seed:
  - current output stays Follow inversion / Not set rather than `#000000`;
  - HSV controls are replaced by a normal MIUIX explanatory `BasicComponent`;
  - common colors and HEX/RGB remain available, so the first valid user edit still performs copy-on-write into Custom.
- Add unit coverage for Follow inversion with and without remembered custom color.

#### Unified swatch outline

All fixed/checker preview circles now use one shared outline rule rather than page-specific border/no-border decisions:
- stroke width = chip diameter / 24;
- stroke color = `onSurface` at alpha 0.12.

Examples:
- 12dp compact Function preview -> ~0.5dp stroke;
- 20dp editor/common chip -> ~0.83dp;
- 24dp mode setting chip -> 1dp;
- 28dp scheme-header chip -> ~1.17dp.

The same rule is used for fixed-color `Surface`, checkerboard `Surface`, and common-color disks. This intentionally supersedes Build 574's drawer-header/mode-row borderless exception.

### 审查 / review — pre-commit

- **MIUIX-first:** source selection is the exact pinned `OverlayDropdownPreference`; no custom dropdown implementation.
- **drawer ownership:** still one `OverlayBottomSheet`; no nested sheet stack.
- **transition:** existing MIUIX Pager spring remains the sole level transition.
- **scroll boundary:** detail viewport is clipped rather than padded around the bleed symptom.
- **color semantics:** Follow inversion remains dynamic/no-fixed-color and is never represented as fake black.
- **copy-on-write:** common color, HSV, HEX and RGB edits still call the existing custom-color setter; template references remain references until edit.
- **visual consistency:** all preview chips share one proportional outline rule.
- **self drawing:** no project Canvas/drawCircle/drawRect/drawWithCache/raw clickable introduced; checkerboard remains MIUIX public `drawCheckerboard()`.
- **Runtime / persistence:** no hook, writer, schema, projection or color-policy change.
- **Canary:** explicitly prohibited until maintainer instruction.

### Validation

Runtime CI #2108 / run `36921379961` failed during Kotlin compilation before tests because the mode-detail redesign referenced three Android string resources that did not exist:
- `battery_color_unset`;
- `battery_color_no_fixed_color`;
- `battery_color_no_fixed_color_summary`.

Root cause was resource-contract incompleteness in the UI-only refactor, not MIUIX API incompatibility or Runtime behavior. Review found an existing canonical `battery_color_custom_unset` string already expresses the first state, so the correction reuses it rather than adding a duplicate. The two no-fixed-color explanatory strings are added in English and Simplified Chinese.

Runtime CI #2109 / run `36921996822` is green on exact code SHA `621800833c181fd65dd6d6f4c13c2a4e3cb1c7e8`: unit tests, Debug assembly, pinned HyperOS target verification, and Modern Xposed metadata validation all pass. No Canary was generated. A final exact-head Runtime CI follows this documentation-only closure; no further code change is planned.


## 2026-10-01 — Build 576 strict MIUIX typography correction

**Type:** App UI typography conformance only  
**Display version:** 0.0.3  
**Build:** 576 / `20261001-576`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Second audit finding

A second exact-revision review of Build 574 found that the component choices were correct, but three text nodes still used hand-added font weights:
- style name: `title2.fontSize + FontWeight.Medium`;
- Add-page title: `title2.fontSize + FontWeight.Medium`;
- Add-page caption: `headline1.fontSize + FontWeight.Medium`.

Pinned MIUIX `TextStyles.kt` defines `title2 = 24sp` and `headline1 = 17sp` **without** those weights. Therefore the prior code was MIUIX-adjacent rather than strictly default.

### Change

- Style name now uses `style = MiuixTheme.textStyles.title2`.
- Add-page title now uses `style = MiuixTheme.textStyles.title2`.
- Add-page caption now uses `style = MiuixTheme.textStyles.headline1`.
- Remove the now-unused `FontWeight` import.

### Deliberate product-specific exceptions

These are retained and must not be represented as MIUIX defaults:
- Add glyph at 32dp: explicit maintainer request for a stronger/larger plus; the container itself remains native 60dp MIUIX FloatingActionButton.
- Header swatches 28dp and mode-row swatches 24dp: pinned MIUIX provides no static color-preview-circle component/spec. Guiyuan therefore defines presentation sizes while still using MIUIX Surface/public drawCheckerboard and no project Canvas.

### 审查 / review

- no hierarchy change;
- no spacing/layout change;
- no state/persistence/runtime change;
- no custom font weight remains in the scheme/Add page;
- native ArrowPreference/BasicComponent/FloatingActionButton ownership remains intact;
- no Canary after CI without explicit maintainer instruction.


## 2026-10-01 — Build 575 custom-style dialog MIUIX state audit

**Type:** App UI state semantics / MIUIX conformance  
**Display version:** 0.0.3  
**Build:** 575 / `20261001-575`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Trigger

Maintainer device screenshots showed the custom-style management / rename controls and explicitly required MIUIX conformance at every detail level, including typography and font weight.

### Exact pinned MIUIX audit

Revision: `5c91d5e5ce1a2fc7e8bdc1258a881c555102bbca`.

Verified directly:
- `OverlayDialog`
  - title: MIUIX `title4` = 18sp, Medium, centered;
  - default inside margin: 24x24dp;
  - default outside margin: 12x12dp;
  - mobile bottom-attached corner radius derives from screen corners and is clamped to 32..48dp.
- `TextField`
  - default text style: `main` = 17sp;
  - floating label: 10dp;
  - corner radius: 16dp;
  - inside margin: 16x16dp.
- `TextButton`
  - default text style: MIUIX `button` = 17sp;
  - min height: 40dp;
  - corner radius: 16dp;
  - inside margin: 16dp horizontal / 13dp vertical.
- Official OverlayDialog two-action example:
  - two equal-weight TextButtons;
  - 20dp spacer;
  - affirmative action uses `ButtonDefaults.textButtonColorsPrimary()`.
- `BasicComponent`
  - title: headline1 17sp Medium;
  - minimum row height: 56dp;
  - inset: 16dp;
  - summary: body2 14sp.

### Change

- Keep all existing native dimensions and typography; do **not** hard-code substitute font sizes/weights.
- Create dialog: drive `TextButton.enabled` directly from `name.trim().isNotEmpty()`; invalid state therefore uses MIUIX native disabled colors and interaction.
- Rename dialog: affirmative button is enabled only when trimmed text is non-empty **and** differs from the current displayed scheme name.
- Callbacks receive the already-trimmed value; remove silent no-op guards from the click callback.
- Management action list remains `BasicComponent`; Copy already uses its native disabled state when custom-style capacity is full.
- Delete remains the only destructive action; its title color now goes through `BasicComponentDefaults.titleColor(color = error)` instead of a hand-built `BasicComponentColors`, keeping disabled/title semantics under MIUIX ownership.
- Rename pre-fills the same effective display name used by the style page/management title, including the legacy unnamed-style fallback.

### 审查 / review — pre-commit

- no custom typography added;
- no custom font weight added;
- no custom dialog radius/margin/size added;
- no project drawing added;
- no persistence/runtime/model change;
- Build-574 first-page hierarchy untouched;
- no Canary after CI without explicit maintainer instruction.

### Validation

Run exact-head automated CI only. Freeze the green SHA and wait.


## 2026-10-01 — Build 574 scheme page becomes a MIUIX settings page

**Type:** App UI hierarchy / MIUIX setting-row semantics  
**Display version:** 0.0.3  
**Build:** 574 / `20261001-574`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Maintainer direction

The battery-color drawer is a **settings page**, not an information-display page. The first Pager page must visibly establish the selected style, then present every battery semantic mode as one setting item. The style title and preview are centered. Drawer swatches should be larger and cleaner than the compact Function-page summary. The Add Pager page should preserve the visual “large + on a circle” concept while using MIUIX rather than project drawing.

### Exact MIUIX basis

Verified against pinned revision `5c91d5e5ce1a2fc7e8bdc1258a881c555102bbca`:
- `ArrowPreference` is implemented on top of `BasicComponent` and provides the standard MIUIX title + summary + native Basic ArrowRight affordance;
- `BasicComponent` owns the common 56dp minimum row height / 16dp inset / title-summary typography;
- `FloatingActionButton` is a MIUIX `Surface` with `CircleShape`, default 60x60dp minimum size and 4dp shadow;
- MIUIX text styles expose `title2 = 24sp`;
- theme roles provide `primary/onPrimary` and disabled button colors.

### Changes

- Replace the previous small header Card with a centered style header:
  - style name uses MIUIX `title2` (24sp, Medium);
  - six style-preview swatches are 28dp with 10dp spacing;
  - drawer header swatches use MIUIX `Surface` and no outline.
- Replace the previous mode/color/HEX/action table with six actual setting rows:
  - custom scheme mode -> `ArrowPreference`;
  - built-in scheme mode -> the same underlying `BasicComponent` setting geometry without a misleading edit arrow;
  - title = semantic mode;
  - summary = current HEX or Follow inversion;
  - right-side preview = 24dp borderless fixed/checker swatch;
  - MIUIX owns row height, inset, typography, native arrow and click feedback.
- Update Chinese row labels to the maintainer wording: 普通 / 省电模式 / 性能模式 / 超级省电 / 充电 / 低电量. English Power save / Performance become Power save mode / Performance mode.
- Keep the Function-page compact preview unchanged through parameterized defaults (12dp / 6dp / bordered); drawer-only calls request the larger borderless presentation.
- Replace the Add-page interactive Card with native MIUIX `FloatingActionButton`:
  - default 60dp circular FAB and 4dp shadow;
  - theme primary/onPrimary colors;
  - `MiuixIcons.Add` at 32dp for the maintainer-requested stronger plus;
  - centered MIUIX title2 “新建样式” and action label;
  - max-cap state keeps the 60dp circle using a non-clickable MIUIX Surface + disabled button colors.
- Remove the obsolete Add-card `PressFeedbackType`, `holdDownState` wiring and the previously misused `Forward` icon.

### 审查 / review — pre-commit

- **MIUIX-first:** mode rows are real MIUIX setting components rather than a hand-built Row imitation; the Add action is the native MIUIX FAB.
- **visual hierarchy:** style identity is the first visual level; settings follow below.
- **swatch semantics:** Function-page summary stays compact; drawer header and mode rows are explicitly larger. Drawer fixed/checker circles have no outline as requested.
- **built-in behavior:** built-ins retain normal visual weight and the same setting-row geometry but do not display a false edit arrow.
- **custom behavior:** only custom scheme rows expose MIUIX ArrowPreference navigation to the existing mode editor.
- **Runtime / persistence:** untouched.
- **drawing:** no new project Canvas/draw primitive is introduced; checkerboard remains MIUIX public `drawCheckerboard()`.
- **scope:** no second-level editor redesign in this build.

### Validation / gate

Run exact-head automated CI. **Do not trigger Canary after CI.** Freeze the green SHA and wait for explicit maintainer instruction.


## 2026-10-01 — Build 573 final MIUIX proportion / optical pass

**Type:** App UI proportion / spacing normalization  
**Display version:** 0.0.3  
**Build:** 573 / `20261001-573`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Basis

After the component-first pass was structurally closed in Build 572, the maintainer requested one final global layout review against MIUIX itself rather than screenshot-fitted tuning.

The exact pinned dependency is:
- MIUIX `0.9.4-5c91d5e5-SNAPSHOT`;
- revision `5c91d5e5ce1a2fc7e8bdc1258a881c555102bbca`.

Exact defaults read from that revision:
- `BasicComponent`: 56dp minimum height, 16dp internal padding, 8dp action spacing; title uses headline1 (17sp Medium), summary uses body2 (14sp);
- `Card`: 16dp corner radius, 0dp default internal padding;
- `BottomSheet`: 28dp top corner radius, 24dp horizontal internal margin, 640dp max width; title uses title4 18sp Medium; title row top/bottom 6/12dp;
- official BottomSheet demo: in-sheet `SmallTitle` uses 16dp horizontal / 8dp vertical margins and Cards are not given an additional horizontal inset;
- `IconButton`: 40x40dp minimum hit target;
- `TextField`: 16dp corner radius, 16x16dp internal margin;
- MIUIX HSV sliders: 26dp track height, 20dp indicator;
- `FloatingToolbar`: 50dp corner radius, surfaceContainer background, 4dp default shadow.

### Changes

- Fix the inner BottomSheet navigation Pager at the previously chosen 520dp baseline instead of letting `heightIn(520..650)` expand the page and create unnecessary blank vertical space.
- Remove the extra 12dp horizontal page/editor inset because `OverlayBottomSheet` already supplies 24dp.
- Normalize scheme-header custom padding to 16dp and remove the mode-card's ad-hoc 2dp internal margin; mode rows therefore use native `BasicComponent` 56/16 geometry.
- Scheme title uses MIUIX headline1 17sp Medium.
- Scheme mode value/status column uses MIUIX body2 14sp + summary color rather than default-size secondary text.
- Keep the approved aligned mode / swatch / value / action columns and their functional widths.
- Add page keeps the approved centered add-card concept, but centers the card within the Pager, uses a 24dp MIUIX Add icon, 8dp icon-label gap and 24dp vertical card inset.
- Replace the page-indicator container Card with actual MIUIX `FloatingToolbar`; page dots remain MIUIX `Surface` primitives and preserve the active short-pill behavior.
- Detail page title becomes `<mode> color` / `<模式>颜色`; remove the redundant custom-scheme subtitle below it.
- Use the official BottomSheet-demo section-title margin of 16x8dp throughout the editor.
- Keep custom-content Card insets at 16dp; remove the residual 16x14dp asymmetry.
- Current color preview moves from 28dp to 26dp to align with the native HSV track height.
- Palette strip mini swatches use 12dp with 6dp spacing.
- Common-color options retain a 40dp clickable MIUIX `Surface` target but reduce the visible swatch to ~20dp; selected state uses a white MIUIX Surface ring + 2dp shadow around the smaller color core instead of a giant filled 36dp disc.
- Reset action icon uses the standard 24dp icon footprint.

### 审查 / review — pre-commit

- **hierarchy:** unchanged; one BottomSheet, one whole-scheme Pager, fixed floating indicator, in-sheet detail page, default-collapsed source selector;
- **behavior:** source order, copy-on-write, template references, custom persistence, five-style cap, create/rename/delete and destructive error semantics unchanged;
- **MIUIX:** all visual primitives remain MIUIX components/public APIs; no project Canvas/drawCircle/drawRect/drawWithCache/raw clickable was introduced;
- **Runtime:** no Xposed hook, state source, painter, transition, geometry, reservation, color policy or Runtime preference-key change;
- **no screenshot fitting:** numeric changes are traced to exact MIUIX defaults/examples or preserve an already-approved Guiyuan functional alignment constraint.

### Validation

Run exact-head automated CI. If green, perform a final code review and build one signed exact-head Canary for App-UI device validation.


## 2026-10-01 — Build 572 final battery-color editor MIUIX semantics

**Type:** App UI component semantics only  
**Display version:** 0.0.3  
**Build:** 572 / `20261001-572`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Goal

Close the remaining component-semantic gaps in the final custom-mode editor before beginning the maintainer-requested global MIUIX ratio/proportion review. The approved page hierarchy, source model, copy-on-write behavior and layout remain unchanged.

### Change

- Color source stays an in-place expandable MIUIX `BasicComponent`.
- While expanded, that component now uses its built-in `holdDownState`.
- Its end area keeps the current fixed-color/MIUIX-checker preview and adds:
  - `MiuixIcons.ExpandMore` while collapsed;
  - `MiuixIcons.ExpandLess` while expanded;
  - `onSurfaceVariantActions` tint.
- Restore-this-mode remains a normal, non-destructive `BasicComponent` action and gains `MiuixIcons.Reset` with the same MIUIX action tint.
- Existing `RadioButtonPreference`, `HsvHueSlider`, `HsvSaturationSlider`, `HsvValueSlider`, `TextField`, `Card`, `SmallTitle`, and MIUIX `Surface` color chips remain authoritative.
- No switch to full `ColorPicker`: its alpha channel would expose unsupported transparency semantics, while the separate MIUIX HSV controls already match Guiyuan's opaque-color contract.

### Exact-version verification

Verified directly against pinned MIUIX revision `5c91d5e5ce1a2fc7e8bdc1258a881c555102bbca`:
- `MiuixIcons.ExpandMore`;
- `MiuixIcons.ExpandLess`;
- `MiuixIcons.Reset`;
- `BasicComponent.holdDownState`;
- `MiuixTheme.colorScheme.onSurfaceVariantActions`.

### 审查 / review

- information architecture unchanged;
- source remains default-collapsed and expands in place;
- no new project drawing;
- no new dependency;
- no persistence/runtime/color-policy change;
- Restore remains non-destructive; Delete remains the only error-colored destructive action;
- copy-on-write from template/follow source to Custom is untouched;
- final numeric sizing/spacing/typography tuning is intentionally deferred to the next single global MIUIX proportion pass.

### Validation

Run exact-head automated CI. A green result closes component semantics for the entire battery-color flow and unlocks the final MIUIX ratio/proportion review before the next device Canary.


## 2026-10-01 — Build 571 Add-card hold-state compile correction

**Type:** compile-only correction  
**Display version:** 0.0.3  
**Build:** 571 / `20261001-571`  
**Branch / PR:** `feat/battery-top-readout` / #181

### CI evidence

Build 570 Runtime CI run 36909145146 reported one Kotlin error only: the sole `BatterySchemeOverview` call did not pass `addHeldDown`, which was introduced solely to feed MIUIX Card `holdDownState` while the create-style dialog is visible.

### Correction / review

- pass `addHeldDown = showCreateDialog` at the only overview call;
- verify the generated source contains both `managedCustomId = manageCustomId` and `addHeldDown = showCreateDialog` before commit;
- no UI structure, proportion, color, persistence, or Runtime change.

### Validation

Run exact-head automated CI. If green, close the pre-editor component pass and continue with the final custom-mode editor component semantics.


## 2026-10-01 — Build 570 pre-editor MIUIX interaction-state pass

**Type:** App UI component semantics only  
**Display version:** 0.0.3  
**Build:** 570 / `20261001-570`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Goal

Continue the agreed component-first pass through the pages before the final color editor, without changing the approved layout or beginning the final proportion-tuning phase.

### Change

- Feature-page `Battery colors` remains an MIUIX `ArrowPreference`, and now feeds `showBatteryColorSheet` into its built-in `holdDownState`; the originating preference therefore retains the standard MIUIX pressed ownership while its BottomSheet is open.
- The Add-scheme page keeps the existing centered MIUIX `Card + Add icon + label` composition.
- Its interactive Card now uses MIUIX `PressFeedbackType.Sink`, native indication, and `holdDownState` while the create-style OverlayDialog is visible.
- Custom-scheme More already uses MIUIX `IconButton.holdDownState` while the management dialog is open; no change needed.
- Create/rename/delete dialogs remain `OverlayDialog + TextField/BasicComponent + TextButton`; their two-button/20dp confirmation layout matches the pinned MIUIX 0.9.4 documentation and is intentionally retained.

### Exact-version basis

Verified against pinned MIUIX revision `5c91d5e5ce1a2fc7e8bdc1258a881c555102bbca`:
- `ArrowPreference.holdDownState`;
- interactive `Card.pressFeedbackType / showIndication / holdDownState`;
- `PressFeedbackType.Sink`;
- `IconButton.holdDownState`.

### 审查 / review

- call-chain scan: `BatteryColorPreference`, `BatterySchemeOverview`, and `BatteryAddSchemePage` each have one invocation and one declaration in their owning files;
- no new project drawing;
- no layout/proportion tuning;
- no data/persistence/runtime modification;
- no change to destructive Delete semantics.

### Validation

Run exact-head automated CI. If green, the entry/scheme/add/custom-management layers are considered component-closed. Continue next with the final custom-mode editor's component semantics, then perform the single global MIUIX ratio/proportion review requested by the maintainer.


## 2026-10-01 — Build 569 MIUIX management-state compile correction

**Type:** compile-only correction  
**Display version:** 0.0.3  
**Build:** 569 / `20261001-569`  
**Branch / PR:** `feat/battery-top-readout` / #181

### CI evidence

Build 568 Runtime CI run 36908190302 reached `:app:compileDebugKotlin` and reported one error only: the sole `BatterySchemeOverview` invocation did not pass the newly introduced `managedCustomId` parameter used to drive MIUIX `IconButton.holdDownState`.

### Correction

- Pass `manageCustomId` into the existing overview call.
- Directed scan confirms `BatteryColorControls.kt` has exactly one invocation and one function declaration, so there is no second call site to reconcile.

### 审查 / review

- compile-only wiring change;
- no layout/proportion adjustment;
- no new drawing primitive;
- no Runtime/persistence/data-model change;
- Build-568 MIUIX component substitutions remain otherwise byte-for-byte unchanged.

### Validation

Run exact-head automated CI. If green, resume the component-first cleanup; final proportion tuning remains deferred until the last editor page is structurally complete.


## 2026-10-01 — Build 568 battery-color MIUIX component-conformance pass

**Type:** App UI component-conformance only  
**Display version:** 0.0.3  
**Build:** 568 / `20261001-568`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Device-review trigger

Build 567 was functionally valid and automated validation was fully green, but maintainer screenshots showed that several battery-color surfaces merely used Compose/MIUIX-adjacent styling rather than consistently expressing MIUIX component semantics. The requested correction is not a new layout. The previously approved hierarchy and interaction model remain authoritative; implementation primitives should be MIUIX wherever available, with project drawing avoided.

### Change

- Replace the in-sheet text `Back` pill with MIUIX `IconButton` + `MiuixIcons.Back`.
- Replace the project text glyph `›` with `MiuixIcons.Forward`.
- Refactor each semantic mode row from a project-owned title Row into MIUIX `BasicComponent(title, endActions)`; only the already-approved color/value/action columns remain composed in `endActions`.
- Replace project clip/background pagination primitives with MIUIX `Surface`.
- Replace project-drawn fixed-color chips with MIUIX circular `Surface` components and `BorderStroke` parameters.
- Replace the project Canvas checker swatch with a circular MIUIX `Surface` whose content uses the **public MIUIX 0.9.4 `Modifier.drawCheckerboard()`** from `ColorPicker.kt`.
- Replace project common-color Box/background/border/clickable construction with clickable MIUIX `Surface` composition. Selection remains a simple white MIUIX-surface ring, consistent with the current ColorPalette/ColorSlider visual language, without copying their private draw implementation.
- Keep custom management on MIUIX `OverlayDialog + BasicComponent` because current DropdownItem coloring is menu-wide and cannot preserve the already-approved error-red Delete row independently. The More `IconButton` now uses MIUIX `holdDownState` while its management dialog is visible.
- Keep MIUIX `HsvHueSlider / HsvSaturationSlider / HsvValueSlider`, `TextField`, `Card`, `RadioButtonPreference`, `OverlayDialog`, and Pager spring/gesture APIs unchanged.

### Exact-version verification

The project is pinned to `miuix.version=0.9.4-5c91d5e5-SNAPSHOT`, revision `5c91d5e5ce1a2fc7e8bdc1258a881c555102bbca`. Before commit, that exact revision was checked for:
- `Surface` clickable/non-clickable overloads with shape/border/shadow;
- public `Modifier.drawCheckerboard()`;
- `MiuixIcons.Back` and `MiuixIcons.Forward`;
- `IconButton.holdDownState`;
- `surfaceContainer` theme role.

### 审查 / review

- **layout contract:** unchanged; this is component substitution, not a re-layout.
- **self-drawing:** directed scan of the new `BatteryColorControls.kt` reports zero project `Canvas`, `drawWithCache`, `drawCircle`, `drawRect`, raw `.background(`, raw `.border(`, raw `.clickable`, or text-glyph chevrons.
- **MIUIX boundary:** checkerboard rendering is invoked only through MIUIX's own public API; Guiyuan does not copy/reimplement its drawing algorithm.
- **destructive semantics:** Delete remains error-colored and confirmation-gated.
- **Runtime / persistence:** untouched; no hook, state source, writer, color projection, preference schema, or SystemUI behavior changes.
- **proportion discipline:** no final spacing/size/typography tuning is attempted in this pass. That review is intentionally deferred until the last editor page is structurally complete, then will be based on pinned MIUIX defaults rather than screenshot fitting.

### Validation

Run exact-head automated CI. If green, continue the component-first cleanup. Do not request broad device testing yet; the next device visual gate belongs after the final MIUIX ratio/proportion pass.


## 2026-10-01 — Build 567 HyperOS-default contract-test correction

**Type:** test-only contract correction  
**Display version:** 0.0.3  
**Build:** 567 / `20261001-567`  
**Branch / PR:** `feat/battery-top-readout` / #181

### CI evidence

Full CI #2096 / run 36904240233 compiled Debug and Canary successfully and reached `:app:testDebugUnitTest`. Of 426 tests, exactly one failed: `CombinedStatusColorPolicyTest.modeColorOnlyChangesBatteryByDefault`. That assertion still expected `CombinedStatusRecommendedBatteryPalette.PERFORMANCE`, which contradicts the maintainer-approved Build-564 contract that HyperOS is the default scheme.

### Correction

- Change the default color-policy expectation from Recommended/Low-saturation to `CombinedStatusHyperOsBatteryPalette.PERFORMANCE`.
- Strengthen the HyperOS charging test: supply a deliberately non-HyperOS runtime semantic input (`#123456`) and assert the selected HyperOS fixed template still resolves to the pinned `#1DCD3A`. This distinguishes the new fixed-template contract from the old `SystemDefault` behavior rather than passing accidentally because the target runtime color happens to equal the template.

### 审查 / review

- production source is untouched;
- the failed assertion is demonstrably stale relative to the user-approved default/order and exact-target template model;
- center/mobile remain status-tint by default; only battery-family outputs consume the selected scheme unless linkage switches are enabled;
- this change increases regression strength by explicitly separating fixed-template resolution from runtime semantic input.

### Validation

Run exact-head Full CI. A green result closes automated validation for the battery-color redesign and permits exact-head signed Canary generation.


## 2026-10-01 — Build 566 Compose padding compile correction

**Type:** compile-only correction  
**Display version:** 0.0.3  
**Build:** 566 / `20261001-566`  
**Branch / PR:** `feat/battery-top-readout` / #181

### CI evidence

Full CI #2095 / run 36903859758 cleared Build 564's Pager `PagerSnapDistance` mismatch and reached the next Kotlin compile check. The only reported source failure was `BatteryColorControls.kt:740`: Compose has separate `padding(horizontal, vertical)` and `padding(start, top, end, bottom)` overloads, so `padding(horizontal = 12.dp, bottom = 24.dp)` is invalid.

### Correction

- Replace that call with `padding(start = 12.dp, end = 12.dp, bottom = 24.dp)`.
- Directed scan of the same file's remaining `padding`, `PagerDefaults.flingBehavior`, and `heightIn` calls found no second matching overload misuse.
- No UI geometry value changes: horizontal 12dp and bottom 24dp are preserved exactly.

### 审查 / review

- compile-only; no behavior or state transition changes;
- Build-565 migration compatibility correction remains intact;
- MIUIX interaction ownership and destructive-error semantics remain intact;
- App-only scheme metadata / Runtime projection boundary remains unchanged;
- no new dependency, hook, listener, animator, writer, or persistence key.

### Validation

Run exact-head Full CI. If green, close automated validation and proceed to one signed exact-head Canary for the new battery-color BottomSheet visual/interaction review.


## 2026-10-01 — Build 565 battery-color post-review correction

**Type:** compile correction + compatibility review  
**Display version:** 0.0.3  
**Build:** 565 / `20261001-565`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Build 564 CI finding

Full CI #2094 / run 36903357998 reached Kotlin compilation and failed on one API binding in `BatteryColorControls.kt`: the positional `PagerNavigationSpringSpec` argument was interpreted as `PagerSnapDistance` by the current Compose `PagerDefaults.flingBehavior` signature. MIUIX's current guide/example uses named `state` and `snapAnimationSpec`; Build 565 follows that exact form.

### Review corrections

- Use:
  `PagerDefaults.flingBehavior(state = pagerState, snapAnimationSpec = PagerNavigationSpringSpec)`.
- Legacy migration now preserves **effective behavior**, not invalid raw state: an old slot marked CUSTOM with no stored color maps back to the active preset source, matching the previous Runtime fallback. A pure helper/test locks this rule.
- Dormant stored custom colors remain preserved when the old slot currently uses Preset.
- Aligned custom mode rows now route click ownership through MIUIX `BasicComponent`; the Add scheme page uses MIUIX interactive `Card` feedback.
- Destructive Delete remains `MiuixTheme.colorScheme.error` and confirmation-gated.
- No scheme-library key is added to Runtime visual-key classification; only the flattened active color keys synchronize to SystemUI.

### 审查 / review

- **ownership:** unchanged; no new SystemUI hook/listener/animator/geometry writer/painter.
- **MIUIX:** Pager spring invocation now matches the current upstream documentation and library API; interaction surfaces use MIUIX components where available.
- **migration:** active behavior and dormant custom memory are both retained; invalid legacy CUSTOM-without-color cannot create an empty custom source.
- **scope:** App UI/settings + existing color-policy projection only.
- **device gate:** still UI-focused after automated validation; no need to repeat the broader transition matrix unless colors/runtime unexpectedly diverge.

### Validation

Run exact-head Full CI. If green, freeze Build 565 and produce one signed exact-head Canary for the battery-color BottomSheet interaction/visual review.


## 2026-10-01 — Build 564 MIUIX battery-color scheme library

**Type:** App UI / settings schema / Runtime projection  
**Display version:** 0.0.3  
**Build:** 564 / `20261001-564`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Goal

Replace the temporary Build-561/562 battery-color selector with the agreed hierarchy: one BottomSheet, whole-scheme horizontal paging, aligned six-mode previews, up to five named custom schemes, and one in-sheet mode editor whose first edit automatically becomes Custom.

### Exact-target color basis

Directed review of `CHS-Haple/SystemUI-Reference` for SystemUI `17.03.260226.r` confirms the status-bar semantic resources:
- charging `#1DCD3A`;
- power save `#FF9F05`;
- performance `#3482FF`;
- low battery `#FA382E`;
- no distinct target-proven super-power-save progress color; Guiyuan's HyperOS template reuses power-save for that slot;
- Normal remains the status-icon tint/inversion path and is represented by the checker/mosaic semantic rather than a fake fixed HEX.

The new HyperOS built-in is intentionally a fixed verified template. `Follow inversion` remains a separate per-mode source.

### Data / migration

- Add an App-side `BatteryColorSchemeLibraryRepository` in the existing visual preferences file.
- Built-in order/default: HyperOS -> iOS -> Low saturation.
- Custom scheme cap: five, with stable IDs and smallest-free-ID naming support.
- Each custom mode stores a source reference: HyperOS / iOS / Low saturation / Follow inversion / Custom.
- Template references remain references in App metadata; only Custom stores an authored fixed color.
- Activating a built-in/custom scheme projects to the pre-existing Runtime `batteryColorPreset / mode / override` keys. Scheme names/order/library metadata are not Runtime keys and therefore never cross into SystemUI.
- Legacy migration keeps the currently effective preset/mode result and retains dormant stored custom colors even when the old slot was currently set back to Preset.
- Deleting the active custom scheme returns to its recorded base built-in.

### MIUIX UI

- Keep one `OverlayBottomSheet`; detail editing is an internal spring page transition rather than stacked sheets.
- Scheme card + six mode rows form one `HorizontalPager` page and move together.
- Pager uses the upstream `PagerNavigationSpringSpec`, `pagerGestureOverride`, and `PagerGestureNestedScrollConnection`.
- A fixed adaptive MIUIX `Card` capsule below the pager contains dot indicators; the active page stretches to a short pill.
- Built-in rows are read-only and reserve the same action-column width as custom rows.
- Custom rows expose the mode editor.
- Color source is collapsed by default; selecting a source updates the editor, and changing common colors / HSV / HEX / RGB performs copy-on-write to Custom.
- Create and rename use `OverlayDialog`; Add/More use MIUIX icons from the already-present icons dependency.
- Delete uses `MiuixTheme.colorScheme.error` in both the management row and confirmation action.

### 审查 / review — pre-commit

- **single writer:** no new SystemUI painter, geometry writer, state observer, hook, or animator; Runtime still consumes the existing flattened color keys.
- **process boundary:** custom scheme metadata is App-only; `isCombinedStatusVisualPreferenceKey` is intentionally unchanged for library keys.
- **native-first:** Pager spring/gesture, Card, Dialog, Button, Radio preference, HSV sliders, TextField and icons use MIUIX APIs. Only the checker swatch and compact page dots are project-drawn display primitives because MIUIX 0.9.4 has no PagerIndicator component.
- **migration:** old active color behavior is representable; dormant custom values are retained instead of silently discarded.
- **destructive action:** Delete is error-colored and confirmation-gated.
- **copy-on-write:** a template/follow source remains referenced until the first actual edit; the first edit is applied without a value jump and changes the slot source to Custom.
- **Fail-native / Runtime:** the fixed HyperOS template is tied to the verified target and documented as target evidence, not a universal Xiaomi constant.

### Validation

Run exact-head CI first. If green, a signed Canary is warranted for visual/interaction review of the new BottomSheet hierarchy; Runtime device testing is only required if observed colors differ from the projected fixed/template result.




## 2026-10-01 — Build 549 faster continuous retract AB

**Type:** focused device-evidence timing refinement  
**Display version:** 0.0.3  
**Build:** 549 / `20261001-549`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Device evidence

Build-548 video confirms the continuous retract topology remains correct and the Build-545 discontinuity does not return. A small outlet-side residual arc is still visible for several frames after CENTER/network has already committed to the exit, so the remaining defect is only late completion.

### Root cause

Build 548 finishes the ring at 45% of the existing HyperOS transition clock. The symmetric smoothstep keeps the last visible arc continuous, but the completion point is still later than the desired choreography.

### Change

Single-variable AB:
- `TRANSITION_COMPLETE_PROGRESS: 0.45f -> 0.35f`;
- local ring progress reaches 0.5 at global progress 0.175 and 1.0 at 0.35;
- existing symmetric smoothstep remains unchanged;
- LEFT/RIGHT/NONE ordered-arc semantics remain unchanged;
- CENTER/network native target geometry and timing remain unchanged;
- Battery target/handoff, reverse symmetry, and Build-542 island behavior remain unchanged.

### 审查 / review

- timing scalar only; no new geometry gate, Animator, delay, or second timeline;
- Build-546/547/548 continuity preserved; Build-545 live gate remains rejected;
- ownership, lifecycle, single-writer, Fail-native, and performance behavior unchanged;
- steady rendering, battery-top controls, typography handoff, native peer motion, and island projection untouched;
- remap test now locks 0.175 -> 0.5 and 0.35 -> 1.0; existing arc/direction tests remain authoritative.

### Validation

Run exact-head Runtime CI, then one signed exact-head Canary. Primary device check: residual outlet-side arc should clear earlier than Build 548 while remaining visually continuous on normal/fast pulls and symmetric on reverse collapse.


## 2026-10-01 — Build 550 front-loaded continuous retract AB

**Type:** focused device-evidence curve refinement  
**Display version:** 0.0.3  
**Build:** 550 / `20261001-550`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Device evidence

Build 549 moves full Battery-ring completion to 35% of the existing HyperOS transition clock. Maintainer feedback says the first half of the visible retract still feels too similar to the prior version. The requested change is therefore not another earlier terminal cutoff; it is a faster first half while preserving the accepted continuous topology.

### Root cause

The Build-549 local curve is still symmetric smoothstep. Its zero start slope intentionally eases in, so even with an earlier 35% terminal point the first part of the retract remains visually conservative.

### Change

Keep `TRANSITION_COMPLETE_PROGRESS = 0.35` and front-load only the shape-progress curve:
- warp normalized local progress with `p + 0.45 * p * (1 - p)`;
- feed the warped value into the existing smoothstep;
- at local p=0.25, consumed sweep increases from 15.625% to about 26.065%;
- at local p=0.50, consumed sweep increases from 50.0% to about 66.590%;
- endpoints remain exact (0 -> 0, 1 -> 1), with smoothstep retaining zero endpoint slope;
- no piecewise threshold, jump, delay, Animator, or second timeline is introduced.

### 审查 / review

- **scope:** curve shape only; 35% completion point unchanged.
- **continuity:** one monotonic continuous warp followed by the existing continuous smoothstep; no Build-545 gate behavior.
- **ownership / lifecycle / single writer:** unchanged.
- **native motion:** CENTER/network target path and HyperOS expansion clock remain authoritative.
- **reverse:** the same stateless mapping is evaluated in reverse; no separate collapse animator.
- **performance:** constant arithmetic only; no allocation, probe, reflection, listener, or hierarchy traversal.
- **tests:** explicit curve checkpoints lock the faster first half; ordered-arc tests derive geometry from the policy's remaining fraction so they continue to verify topology rather than hard-code the old easing.

### Validation

Run exact-head Runtime CI and then one signed exact-head Canary. Device focus: compare Build 549 vs 550 during the first half of a normal and slow pull. The ring should yield visibly sooner from the start while the last part remains continuous, with no chunk disappearance or change to CENTER/network trajectory.


## 2026-10-01 — Build 551 Preview Sandbox mobile-network coverage

**Type:** preview/UI coverage + regression tests  
**Display version:** 0.0.3  
**Build:** 551 / `20261001-551`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Goal

Expose the mobile standards already handled by the generic native-label rendering path in Preview Sandbox instead of limiting manual preview to None / 4G / 5G / 5G-A.

### Implementation

- Add Preview Sandbox choices for `2G`, `E`, `3G`, `H+`, and `LTE`.
- Final UI order: None / 2G / E / 3G / H+ / 4G / LTE / 5G / 5G-A.
- Keep the original PreviewMobileNetwork ordinals (None=0, 4G=1, 5G=2, 5G-A=3) stable; new enum values append after them. The UI uses an explicit ordered choice list rather than enum ordinal order.
- Add `systemLabel` to PreviewMobileNetwork and feed it directly into the existing `CenterIndicator.MobileType` model.
- Use one horizontally scrollable MIUIX `TabRowWithContour` at a fixed comfortable content width rather than squeezing nine labels into the card width.
- Add bilingual resource entries; technology labels remain standards notation in both locales.
- Add production-path regression coverage proving 2G / E / 3G / H+ / 4G / LTE labels pass through `NativePresentationResolver.normalizeDrawableNetworkType` unchanged.

### 审查 / review

- **runtime architecture:** unchanged; production still reads HyperOS `mobile_type` / `mobile_type_single` and renders generic native text.
- **no per-standard fork:** no extra production branch for 2G/3G/LTE/H+ is introduced.
- **state compatibility:** legacy preview ordinals are preserved to avoid rememberSaveable restoring an old 4G/5G selection as a newly inserted standard.
- **layout:** scrolling prevents label compression; no custom density/touch geometry.
- **scope:** sandbox UI/model/resources and tests only; current Build-550 battery-ring transition runtime remains untouched.

### Validation

Run Runtime CI. Because the functional mapping is deterministic, no runtime-transition device gate is required. A Canary is useful only to visually review the nine-option sandbox control and confirm scrolling/touch ergonomics on the target device.


## 2026-10-01 — Build 552 smooth long-tail Battery-ring retract AB

**Type:** focused device-evidence curve refinement  
**Display version:** 0.0.3  
**Build:** 552 / `20261001-552`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Goal

Keep the accepted fast first half from Build 550, but make the latter half slower and finish later without creating an obvious two-speed or piecewise animation.

### Root cause / design

A literal 50/50 piecewise timing split would create a slope handoff that can read as layered speed. Build 552 instead keeps the same single analytic form used by Build 550 and retunes its two scalar parameters:
- completion point: `0.35 -> 0.45`;
- continuous front-load coefficient: `0.45 -> 0.92`;
- warp remains `p + FRONT_LOAD * p * (1 - p)`, followed by the same smoothstep.

This produces one monotonic continuous curve. The stronger front-load offsets the longer completion window during the early phase, while the longer terminal window and low end slope stretch the tail naturally.

### Checkpoints

Composite global progress -> consumed ring:
- 0.0875 -> about 26.62% (Build 550: about 26.06%);
- 0.175 -> about 65.88% (Build 550: about 66.59%);
- 0.35 -> about 98.85% (Build 550: 100%);
- 0.45 -> 100%.

So the first half stays visually close to Build 550, while the final ~1.15% decays through the extra tail instead of disappearing at 35%.

### 审查 / review

- one continuous stateless curve; no piecewise speed tier, threshold gate, delay, Animator, or second clock;
- ordered-arc topology and LEFT/RIGHT/NONE semantics unchanged;
- CENTER/network native target path and timing authority unchanged;
- Battery handoff, reverse symmetry, Build-542 island projection, and Build-551 sandbox coverage unchanged;
- constant arithmetic only; no new runtime allocations or hierarchy work;
- tests now lock both local curve continuity checkpoints and the intended global early/tail relationship.

### Validation

Run exact-head Runtime CI, then signed exact-head Canary because the requested difference is visual. Device focus: normal and slow pulls. The first half should feel essentially as quick as Build 550, then decelerate naturally into a slightly later tail without any visible speed step or chunk disappearance.


### Build 552 CI correction

Runtime CI #2066 failed only in `leftExitPreservesBatterySemanticsByIntersection`: the stronger continuous front-load means that at local progress 0.5 the retained LEFT suffix begins after the original 75% active-fill end, so `result.active` is correctly empty. The test's unconditional `active.single()` assumption was stale.

Correction is test-only: sample the active-fill intersection at local progress 0.35, where the retained suffix still overlaps the original active fill. Runtime policy, Build ID, curve parameters, topology, and APK behavior remain unchanged.


## 2026-10-01 — Build 553 compact mobile-standard selector

**Type:** Preview Sandbox UI refinement  
**Display version:** 0.0.3  
**Build:** 553 / `20261001-553`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Device / design feedback

The nine-standard horizontally scrollable segmented control from Build 551/552 exposes every standard but creates too many visible slots and dominates the Network card.

### Implementation

- Replace only the mobile-standard control with MIUIX `OverlayDropdownPreference`.
- Keep Mobile/Wi-Fi as the existing two-option `TabRowWithContour`, since that is a primary mutually-exclusive mode switch with only two choices.
- The new row shows the current standard inline and opens a single-choice MIUIX popup for None / 2G / E / 3G / H+ / 4G / LTE / 5G / 5G-A.
- Limit popup height to 360dp so long option lists scroll inside the native popup instead of expanding the page.
- Reuse `SandboxPreferenceInsideMargin` so title/value spacing aligns with `SliderPreference` and `SwitchPreference`.
- Remove the custom horizontal-scroll segmented helper and its scroll-state imports.

### 审查 / review

- Uses the library's purpose-built preference component rather than custom geometry.
- No preview model, enum ordinal, runtime SystemUI path, transition curve, or state ownership changes.
- All nine network standards remain available in the same explicit display order.
- Build-552 Battery-ring transition runtime remains byte-for-byte untouched by this UI refinement.

### Validation

Run Runtime CI. One signed Canary is justified only to inspect popup placement, row density, current-value alignment, and interaction feel on the target device.


## 2026-10-01 — Build 554 customization settings foundation

**Type:** settings schema / color policy foundation  
**Display version:** 0.0.3  
**Build:** 554 / `20261001-554`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Goal

Prepare the requested combined-icon sizing, ring thickness, Wi-Fi/mobile-type tuning, feature reset, and per-battery-mode color customization before exposing the controls.

### Settings model

Profile-scoped geometry (independent for Network-centered and Battery-centered layouts):
- combined scale 85%-115%, default 100%;
- ring stroke scale 70%-130%, default 100%;
- Wi-Fi size 80%-125%, default 100%;
- mobile-type size 80%-125%, default 100%;
- mobile-type weight 500-950, default 800.

Wi-Fi weight is deliberately not exposed: the steady path preferentially renders a native SystemUI drawable. Synthetic dilation or blur would violate native-first rendering and risks optical fuzziness.

Global battery-color state:
- presets: HyperOS native / iOS style;
- custom opaque overrides for Normal, Power Save, Performance, Super Power Save, Charging, and Low;
- iOS-style defaults use yellow #FFCC00, blue #007AFF, orange #FF9500, green #34C759, red #FF3B30; Normal follows status-icon tint;
- custom per-slot overrides win over the selected preset.

### Runtime semantic groundwork

- Add `SUPER_POWER_SAVE` as a distinct semantic state and accept common native enum aliases if HyperOS exposes one.
- Probe optional `mBatterySuperPowerSaveColor` / `mBatterySuperSaveColor`; if absent, HyperOS-native fallback uses the existing power-save color field.
- Existing semantic authority remains `MiuiBatteryMeterIconView.getProgressStatus()`.

### Reset semantics

- `CombinedStatusVisualSettingsRepository.resetToDefaults()` clears all visual settings back to schema defaults.
- `CombinedStatusFeatureSettingsRepository.resetToDefaults()` clears feature settings and refreshes the feature-change timestamp.
- Battery-color overrides also have a dedicated reset helper so a palette can be restored without resetting unrelated controls.

### 审查 / review

- Color preset/overrides are global; geometry remains layout-profile scoped.
- No second runtime settings owner is introduced; `RuntimeVisualPreferencesOwner` stays the single visual-settings bridge.
- Build-552 transition curve and Build-553 sandbox selector are untouched.
- Geometry fields are not consumed by Painter in this checkpoint, preventing half-wired steady vs transition geometry.

### Validation

Runtime CI must lock normalization, runtime-key participation, preset resolution, override precedence, super-power-save parsing, and compilation before geometry/UI wiring proceeds.


## 2026-10-01 — Build 555 reset lifecycle correction

**Type:** lifecycle / settings synchronization fix  
**Display version:** 0.0.3  
**Build:** 555 / `20261001-555`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Pre-commit 审查 / review

The Build-554 foundation was re-reviewed before continuing geometry/UI work. The review found that both visual and feature reset helpers used `SharedPreferences.clear()`, while App/Runtime listeners filtered only concrete keys. Android clear notifications may use `key == null`, so reset could restore persisted defaults without immediately refreshing observers.

The candidate correction was reviewed before branch update:
- feature key relevance is single-source through `isCombinedStatusFeaturePreferenceKey()`;
- App and Runtime feature listeners share that predicate;
- visual key relevance treats `null` as a whole-domain change and Runtime already delegates to that same predicate;
- unrelated non-null keys still do not trigger feature updates;
- feature reset keeps the existing change timestamp in the same editor transaction;
- no second settings owner, poller, or restart path is introduced.

### Change

- Accept `key == null` as a relevant whole-domain change for the dedicated feature and visual preference files.
- Add shared feature-key predicate to prevent App/Runtime filter drift.
- Add unit coverage for clear notification relevance.
- No changes to Build-554 geometry ranges, color presets, semantic mapping, or painter behavior.

### Validation

Run exact-head Runtime CI. No real-device gate is required because this change only repairs observer invalidation semantics; UI reset controls are not exposed yet.


## 2026-10-01 — Build 556 outer-weight geometry wiring

**Type:** runtime geometry wiring  
**Display version:** 0.0.3  
**Build:** 556 / `20261001-556`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Requirement correction

Maintainer clarified that “outer-ring thickness” is intentionally a coupled visual family: changing it should also change the four mobile dots and the unavailable X mark, with dot spacing adapting so the whole lower opening remains visually even.

The repository already contains the correct primitive: `CombinedStatusOuterGeometry.resolve(weightScale)` scales:
- ring stroke;
- mobile-dot radius;
- unavailable-mark stroke and extent;

and then solves dot angular spacing so ring-to-dot and dot-to-dot edge gaps remain balanced.

### Pre-commit 审查 / review

The Build-556 candidate was reviewed before branch update:
- foundation naming changed from `ringStrokeScale` to `outerWeightScale` so UI/schema semantics match the actual coupled behavior;
- setting remains profile-scoped, default 1.0, supported UI range 0.70-1.30;
- all runtime outer-geometry entry points use `visualSettings.outerWeightScale`;
- steady draw, Battery/Battery-number transition draw, Mobile transition draw, and transition source bounds therefore share one geometry source;
- the only remaining static default is the painter cache initializer, which is replaced on first resolved draw and is not an authoritative runtime path;
- no new solver/animator/listener/writer is introduced;
- existing balanced-gap solver remains authoritative;
- existing `fiveVisualEdgeGapsStayBalancedAcrossSupportedScales` regression coverage is preserved;
- new test explicitly locks that ring, dots, and unavailable mark scale as one family.

### Compatibility

No user-facing Build-554/555 UI exposed the foundation-only `ring_stroke_scale` key, so renaming it to `outer_weight_scale` does not migrate a released user setting. Default 100% preserves the accepted 8.25 ring baseline and existing dot/X geometry.

### Validation

Run exact-head Runtime CI. No device gate is required yet because the control is not exposed in UI and the default value leaves runtime appearance unchanged.


## 2026-10-01 — Build 557 independent center geometry

**Type:** runtime geometry wiring  
**Display version:** 0.0.3  
**Build:** 557 / `20261001-557`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Goal

Expose only the requested center-family controls without letting Wi-Fi sizing accidentally resize native airplane/no-SIM icons.

### Pre-commit 审查 / review

The candidate was reviewed before branch update:
- remove the old shared `centerSizeScale` / `centerTextWeightScale` painter override API;
- one settings-backed resolver now supplies Wi-Fi size, mobile-type size, and mobile-type source weight to all center draw/transition/source-bound paths;
- Wi-Fi fallback vector uses only `wifiSizeScale`;
- airplane/no-SIM max sizes remain fixed at the accepted native optical baselines;
- mobile-type size scales text/suffix geometry only;
- mobile-type weight is absolute 500-950, default 800;
- custom mobile weight remains the transition source weight; existing `MobileTypeTransitionPolicy` still interpolates to the SystemUI target weight;
- settings UI bounds remain 80%-125% while lower-level geometry keeps a wider defensive clamp;
- zero legacy shared-size/shared-weight tokens remain in the candidate painter;
- no new writer, listener, animator, or target-geometry owner is introduced.

### Tests

Replace the obsolete “all center families share one size” test with independent contracts:
- Wi-Fi scale changes Wi-Fi only;
- mobile-type scale changes mobile text/suffix only;
- mobile-type weight changes typography only;
- airplane/no-SIM remain fixed when Wi-Fi changes;
- invalid/out-of-range inputs clamp safely;
- existing 5GA lower-right suffix direction remains locked.

### Validation

Run exact-head Runtime CI. Default values preserve current runtime appearance, so no device gate is required until UI controls are exposed.


## 2026-10-01 — Build 558 shrink-only overall combined scale

**Type:** runtime geometry wiring  
**Display version:** 0.0.3  
**Build:** 558 / `20261001-558`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Final range

Maintainer set 100% as both the default and maximum overall size. The supported range is therefore 75%-100%, with 100% as the future slider key point/magnet.

This intentionally permits shrinking only. It avoids increasing the host viewport requirement and remains safe when outer weight is independently increased.

### Pre-commit 审查 / review

The candidate was reviewed before branch update:
- `COMBINED_SCALE_MIN = 0.75`, `MAX = DEFAULT = 1.00`;
- one `resolveCanvasTransform()` owns effective scale and centered offsets;
- all ten runtime geometry paths use the same helper;
- raw `min(width / CANONICAL_SIZE, height / CANONICAL_SIZE)` calculation remains only inside that helper;
- steady draw, top-overflow calculation, transition drawing/specs, airplane/no-SIM bounds, mobile-type current bounds, and Battery Number current bounds therefore cannot diverge;
- default 100% preserves Build-557 geometry exactly;
- no new View size, LayoutParams, writer, listener, animator, or transition clock is introduced;
- range constants are sourced from settings schema rather than duplicated in Painter.

### Tests

Settings normalization now locks:
- 100% is both default and maximum;
- values above max clamp to 100%;
- values below the supported range clamp to 75%.

### Validation

Run exact-head Runtime CI. No device gate yet because no UI exposes the new setting and the default leaves runtime output unchanged.


### Build 558 CI correction

Runtime CI #2075 failed at Kotlin compilation because two functions retained an obsolete local `NativeRenderTransform(...)` construction after being migrated to `resolveCanvasTransform()`, producing duplicate `nativeTransform` declarations.

Pre-commit review of the correction confirmed:
- both duplicate constructions are removed;
- every function using `resolveCanvasTransform()` now has at most one local `nativeTransform`;
- the raw canonical scale calculation still exists only inside `resolveCanvasTransform()`;
- helper call count and all Build-558 scale semantics remain unchanged.

This correction is compile-only. Build ID, 75%-100% range, default/max 100%, transition geometry, and runtime behavior are unchanged.


### Build 558 CI correction 2

Runtime CI #2076 exposed one remaining compile-only residue in `transitionBatteryNumberCurrentBounds()`: after the duplicate local transform was removed, the returned bounds still referenced deleted local `offsetX/offsetY` names.

Pre-commit review of the correction confirmed:
- the function owns exactly one `nativeTransform`;
- all four returned bound coordinates use `nativeTransform.offsetX/offsetY` directly;
- raw canonical scale calculation remains only inside `resolveCanvasTransform()`;
- helper call count and all Build-558 scale semantics remain unchanged.

No runtime behavior, range, transition timing, or visual default changed.


## 2026-10-01 — Build 559 per-mode battery color sources and Recommended preset

**Type:** color settings schema / runtime policy foundation  
**Display version:** 0.0.3  
**Build:** 559 / `20261001-559`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### User-facing model

Palette order/naming for the upcoming UI:
1. Recommended
2. HyperOS
3. iOS

Recommended is the default for new installs and after a full feature reset. HyperOS keeps the persisted value `hyperos_native` for backward compatibility; only the UI label changes.

Each battery semantic slot independently selects one source mode:
- preset color;
- follow system tint/inversion;
- custom color.

The global palette therefore supplies defaults only for slots currently using “preset color”; it never locks the whole color set.

### Recommended palette candidate

Initial muted status-bar candidate:
- Power save: `#D5A623`
- Performance: `#4A7FC1`
- Super power save: `#D8752C`
- Charging: `#3FA760`
- Low battery: `#D64A4A`
- Normal: follow status-icon tint

These values are intentionally less luminous than the iOS semantic set and are not treated as final until the color BottomSheet/preview receives optical and device review.

### Backward compatibility

- Existing installs without an explicit palette are detected through the pre-existing visual schema marker. The shared `readCombinedStatusVisualSettings()` fallback resolves them as HyperOS immediately, and the App-side one-time migration persists that choice when the repository initializes. SystemUI therefore cannot transiently switch an old install to Recommended merely because it starts first.
- Fresh installs have no previous visual schema marker and default to Recommended.
- Existing `hyperos_native` persisted values map directly to the renamed HyperOS enum member.
- Legacy custom colors that predate per-slot mode keys infer `CUSTOM` automatically.
- A stored custom color remains persisted when a slot switches to Preset or Follow System; it becomes active again if the slot later returns to Custom.
- Custom mode without a valid stored color falls back to that slot’s current preset source.

### Pre-commit 审查 / review

- Palette selection remains global; source mode remains per semantic slot.
- Runtime resolution order is explicit: per-slot mode -> selected palette/custom/system source -> existing visibility fallback.
- Follow System always resolves to the current status-icon tint and therefore retains native black/white inversion behavior.
- HyperOS preset still delegates to SystemUI semantic colors instead of duplicating fixed hex values.
- Recommended/iOS Normal remain monochrome by following status-icon tint.
- Mode keys are included in the visual runtime-key set, so App and SystemUI hot updates use the existing single visual-settings bridge.
- No second battery observer, color owner, listener, or writer is introduced.

### Tests

Coverage added/updated for:
- Recommended as the new default;
- old-install missing-preset migration to HyperOS;
- fresh-install missing-preset default to Recommended;
- legacy stored custom color -> Custom mode inference;
- Recommended semantic values;
- per-slot Follow System overriding an iOS preset;
- stored custom color ignored while slot mode is Preset;
- stored custom color used again when slot mode is Custom;
- all new mode keys participating in runtime synchronization.

### Validation

Run exact-head Runtime CI before any BottomSheet/UI work is committed.


### Build 559 CI correction — legacy color-policy expectations

Runtime CI #2078 compiled the new palette/mode model but exposed four existing `CombinedStatusColorPolicyTest` cases whose expectations still assumed the old global default was HyperOS.

Pre-commit review separated test intent instead of blindly replacing expected colors:
- the native semantic-color test now explicitly selects the HyperOS preset;
- the default performance-mode test now validates the Recommended performance color;
- the optional center/mobile follow test explicitly selects HyperOS so it continues to test propagation of the final battery color rather than palette choice;
- the battery-text / charging-icon independent tint test explicitly selects HyperOS so it continues to isolate its intended follow-system behavior.

Runtime production code is unchanged. This is a test-contract correction for the intentional default-palette change introduced by Build 559.


### Build 559 validation closure

Exact-head Runtime CI #2079 (run `36880365780`) completed successfully on `e37d516`.
- unit tests passed after old HyperOS-default expectations were separated from new Recommended-default behavior;
- debug APK build succeeded;
- pinned HyperOS target verification passed;
- modern Xposed metadata verification passed.

Build 559 color-source foundation is closed. No device gate is required before UI exposure because existing installs remain on HyperOS unless the user explicitly changes the palette, while fresh/reset defaults are not user-visible until the settings UI is completed.


## 2026-10-01 — Build 560 MIUIX feature-page size controls and reset card

**Type:** settings UI / feature-page organization  
**Display version:** 0.0.3  
**Build:** 560 / `20261001-560`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Scope

Keep the feature page as the primary settings surface, grouped by the existing card structure instead of turning it into a navigation-only page.

Cards:
1. Global
2. Network
3. Battery
4. Management

### New direct controls

Global:
- Overall size: 75%-100%, 5% steps, default/max 100%;
- Outer weight: 70%-130%, 5% steps, default 100%; this is the existing coupled ring + four-dot + unavailable-mark family.

Network:
- Wi-Fi size: 80%-125%, 5% steps, default 100%;
- Mobile type size: 80%-125%, 5% steps, default 100%;
- Mobile type weight: 500-950, 50-weight steps, default 800.

All five controls use MIUIX `SliderPreference`, `showKeyPoints = true`, a single default `keyPoints` value, and the existing magnetic snap threshold. No custom slider or gesture implementation is introduced.

### Restore defaults

A Management card adds “Restore defaults”.
- It is intentionally available even when the feature master switch is off.
- Confirmation uses the existing MIUIX `OverlayDialog`.
- Confirming resets both `CombinedStatusFeatureSettingsRepository` and `CombinedStatusVisualSettingsRepository`.
- Feature defaults restore the master feature to enabled and lock-screen combined status to disabled.
- Visual defaults restore the active schema defaults, including Recommended palette and 100% geometry defaults.

### Copy review

Chinese and English copy was shortened and normalized during the same UI pass:
- layout summary is reduced to the memory behavior;
- battery readout summary focuses on percentage + automatic avoidance;
- charging summary removes redundant phrasing;
- lock-screen summary removes repeated “combined icon” wording;
- network color-follow summaries use consistent terminology;
- new controls use concise titles such as “Overall size / 整体大小” and “Mobile type weight / 移动制式字重”.

### Pre-commit 审查 / review

- Existing MIUIX Card / SmallTitle spacing is reused; no custom card style is added.
- `HubPage` gains only an optional fourth section, so Settings and other existing three-section callers remain unchanged.
- New controls bind directly to the existing single visual-settings repository; no additional state owner is introduced.
- Slider ranges and default key points come from the same schema constants consumed by runtime.
- Restore is the only control intentionally not gated by `featureSettings.enabled`.
- Existing battery-number/charging detailed sliders remain on the page in this checkpoint; they are not prematurely moved to drawers before final density review.
- No runtime drawing, transition, or SystemUI hook behavior changes in Build 560.

### Validation

Run exact-head Runtime CI to compile the new MIUIX calls/resources and lock repository wiring. Device review is deferred until the color BottomSheet and final feature-page density pass are complete.


### Build 560 validation closure

Exact-head Runtime CI #2081 (run `36881730709`) completed successfully on `c857759`.
- all unit tests passed;
- MIUIX feature-page controls/resources compiled successfully;
- debug APK build succeeded;
- pinned HyperOS target verification and modern Xposed metadata checks passed.

Build 560 is closed. The next change is isolated to battery-color BottomSheet UI and will use a separate, descriptive commit.


## 2026-10-01 — Build 561 MIUIX battery-color BottomSheet

**Type:** settings UI / battery color overview  
**Display version:** 0.0.3  
**Build:** 561 / `20261001-561`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Scope

Expose the Build-559 color-source model through a native MIUIX BottomSheet without introducing a second battery-state authority.

The Battery card gains one concise entry:
- title: Battery colors / 电量颜色;
- summary: current palette;
- end area: five semantic preview dots for power save, performance, super power save, charging, and low battery.

### Preview semantics

The App process does not own HyperOS battery semantic state; the authoritative source remains SystemUI `MiuiBatteryMeterIconView.getProgressStatus()`.

Therefore:
- Recommended/iOS fixed palette colors render as filled preview dots;
- stored Custom colors render as filled preview dots;
- HyperOS preset colors render as outlined/dynamic dots because the actual semantic value comes from SystemUI at runtime;
- Follow System also renders as outlined/dynamic;
- Custom mode without a stored custom color follows the runtime policy and previews its preset fallback;
- Normal remains dynamic for Recommended/iOS because it follows status-icon tint.

This deliberately does not add PowerManager/BatteryManager inference or another cross-process writer merely to fake a “current mode” preview.

### BottomSheet interaction

A single MIUIX `OverlayBottomSheet` is used as a state machine:
- overview: Recommended / HyperOS / iOS palette selection plus all semantic slots;
- slot detail: Scheme color / Follow system / Custom source selection;
- backing out of slot detail returns to the overview rather than stacking another sheet.

MIUIX `RadioButtonPreference`, `ArrowPreference`, `Card`, and `SmallTitle` are reused. No custom drawer implementation is introduced.

### Pre-commit 审查 / review

- Only one `OverlayBottomSheet` exists in the new color UI.
- Palette selection order is Recommended -> HyperOS -> iOS.
- Slot source writes use the existing `CombinedStatusVisualSettingsRepository.setBatteryColorMode`.
- Palette writes use the existing `setBatteryColorPreset`.
- The existing feature reset dialog remains independent from the color sheet.
- UI copy is bilingual and concise.
- No SystemUI runtime/hook/transition code changes.
- No new state observer, listener, or cross-process writer.

### Tests

Pure preview-resolution tests cover:
- Recommended charging and iOS low-battery fixed colors;
- HyperOS and Follow System remain dynamic;
- Custom uses its stored color;
- Custom without a stored color falls back to the selected preset;
- Recommended Normal remains dynamic/status-tint based.

### Deferred to next isolated change

Custom color editing UI:
- common colors;
- MIUIX ColorPicker / ColorPalette;
- RGB and HEX input;
- per-mode reset-to-default.

The source mode is already persisted in Build 561, but no incomplete custom editor is represented as finished.

### Validation

Run exact-head Runtime CI before adding the custom color editor.


### Build 561 validation closure

Exact-head Runtime CI #2083 (run `36884110591`) completed successfully on `b0ba53f`.
- new MIUIX BottomSheet UI compiled successfully;
- all preview-resolution tests passed;
- debug APK build succeeded;
- pinned HyperOS target and modern Xposed metadata verification passed.

Build 561 is closed. Custom color editing remains isolated to the next commit.


## 2026-10-02 — Build 563 scale-aware compact reservation and Mobile Type weight range

**Type:** runtime geometry / settings correction  
**Display version:** 0.0.3  
**Build:** 563 / `20261001-563`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Device evidence and root cause

Build-562 device feedback identified two independent issues:
- Mobile Type weight still exposed the old 500-950 / 800 contract rather than the requested 400-1400 / 900 midpoint.
- Overall size scaled only Guiyuan painter pixels. The native replacement reservation remained the full stable Battery carrier width, so neighboring HyperOS status icons could not close the visual gap.

The Build-562 diagnostic confirms `compactSlotWidth=105` remained unchanged while the renderer accepted live visual settings. The correction therefore belongs to the existing reservation geometry, not to a new spacing offset.

### Implementation

- Mobile Type weight: 400-1400, 50-weight slider intervals, default/key point 900.
- Add one centered-scale reservation rule: because painter shrink is centered in the stable Battery carrier, peer reservation ends at the scaled visual's leading edge while retaining the transparent end-side inset.
- Reuse that rule for Home/Keyguard/Control Center native padding and the transition reservation/latent-reveal compact baseline.
- Visual preference changes ask the existing `SysUiPresentationOwner` to resync its reservation; no second padding/translation writer is added.

### Review

- Geometry is derived from the same base carrier width + user scale; no device-specific px compensation.
- `paddingEnd` remains single-writer owned by the existing presentation session.
- Scale remains shrink-only and the painter remains the sole Guiyuan pixel owner.
- Transition target geometry, HyperOS island width authority, animation clocks, and native peer motion are unchanged.
- Failure paths remain native because unavailable carrier/layout inputs still abort the existing reservation path.

### Validation

Automated validation is expected to be Full while #181 still includes the independently reviewed CI run-title delta. No work-branch Canary is requested by this change alone; device evidence is deferred until the color-UI/runtime palette work is grouped into one focused checkpoint.

## 2026-10-01 — Build 562 MIUIX custom battery color editor

**Type:** settings UI / custom battery colors  
**Display version:** 0.0.3  
**Build:** 562 / `20261001-562`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Scope

Complete the Custom source path introduced by Build 559/561 without adding another screen or another BottomSheet instance.

The existing single BottomSheet now has a third internal state:
1. palette/mode overview;
2. per-mode source selection;
3. per-mode custom color editor.

### Custom editor

The editor provides:
- 10 common color shortcuts;
- full opaque HSV adjustment using MIUIX `HsvHueSlider`, `HsvSaturationSlider`, and `HsvValueSlider`;
- exact six-digit HEX input;
- exact RGB input;
- current-color preview;
- per-mode restore-default action.

The built-in MIUIX `ColorPalette` / `ColorPicker` components were reviewed but intentionally not used because MIUIX 0.9.4 always exposes an alpha slider while Guiyuan persists battery semantic colors as opaque. Showing a control whose result is discarded would violate the UI/runtime contract.

### Common colors

Ten compact shortcuts cover red, orange, yellow, green, cyan, blue, indigo, purple, pink, and neutral gray. They are shortcuts only, not a fourth named palette.

### Persistence

- Picking/editing a custom color guarantees the slot is in `CUSTOM` mode and writes the opaque ARGB value through the existing visual-settings repository.
- `resetBatteryColorSlot(slot)` removes that slot’s source-mode key and override in one SharedPreferences editor transaction.
- Resetting a slot therefore returns it to `PRESET` mode using the currently selected Recommended / HyperOS / iOS scheme.
- No global palette or other slot is changed.

### Input rules

- HEX accepts exactly six hexadecimal digits (optional leading `#`) and forces alpha to FF.
- RGB accepts integer channels 0-255.
- Invalid or incomplete input does not mutate the persisted color.
- For an unset custom color, editor initialization prefers: stored override -> selected fixed palette color -> current MIUIX foreground only as a local editing seed for dynamic SystemUI colors. This seed is not persisted until the user changes a value.

### Pre-commit 审查 / review

- No alpha control is exposed.
- No Material color picker or text field is introduced.
- The BottomSheet instance count remains one.
- The new editor uses only existing repository ownership.
- Per-mode reset is atomic.
- Common colors are UI shortcuts, not persisted as a separate scheme.
- No SystemUI hook, transition, or battery-state ownership changes.

### Tests

Battery color UI tests now also cover:
- valid/invalid six-digit HEX parsing;
- RGB 0-255 bounds;
- RGB split/round-trip;
- editor initial-color precedence and dynamic fallback opacity.

### Validation

Run exact-head Runtime CI before any device review.


### Build 562 CI correction — final reviewed editor candidate

Runtime CI #2085 (run `36885816625`) used an earlier editor candidate and failed Kotlin compilation at `BatteryColorControls.kt` because of an explicit `androidx.compose.foundation.layout.weight` import. In this Compose version that import resolves to an internal parent-data property, while `Modifier.weight()` is already available from the RowScope used by the existing project UI.

The correction:
- removes the explicit `weight` import only; layout behavior is unchanged;
- restores the later reviewed interaction where opening the Custom editor does not immediately write `CUSTOM`;
- writes `CUSTOM + color` only after a valid common-color / HSV / HEX / RGB edit;
- uses the Recommended color for the same semantic slot as the editor start when the selected source is dynamic, falling back to current foreground only where no semantic fixed color exists;
- keeps the per-mode atomic reset and opaque-only color contract;
- expands pure UI logic tests for HEX/RGB parsing, RGB round-trip, and editor initial-color priority.

The PR display title process was also verified: CI #2085 displayed `feat: add MIUIX custom battery color editor`, confirming that updating the PR title before the work-branch HEAD update makes the Actions list describe the concrete Build objective without changing workflow trigger/security semantics.

No SystemUI runtime, hook, transition, or rendering behavior changed in this correction.


### Build 562 CI correction 2 — remove duplicate editor tests

Runtime CI #2090 (run `36887864769`) compiled the production app successfully. Unit-test compilation then failed because iterative review had appended a second set of tests covering the same HEX/RGB parsing and editor initial-color priority, including a duplicate function named `editorInitialColorPrefersStoredThenPresetThenDynamicFallback`.

Correction:
- remove the later duplicate parser / initial-color / RGB round-trip block;
- keep the original seven focused tests;
- confirm there are no duplicate test function names;
- production code is unchanged.

The workflow/run-name cleanup is intentionally deferred until after the next Canary is delivered for device testing.


## 2026-10-02 — CI run-title clarity

**Type:** CI presentation only

GitHub PR-triggered workflow runs previously inherited the pull-request title because the workflows did not define `run-name`. This made unrelated commits appear under the same Actions title.

Change:
- Build PR runs now show run number + PR number + work branch + exact PR HEAD SHA.
- Build push runs show run number + branch + push head commit message.
- Manual Build runs show run number + branch.
- Comment-triggered Canary runs show Canary run number + PR number.
- Manual Canary runs show Canary run number + requested source branch.

Review:
- workflow names remain `Build` and `Work Branch Canary`;
- job ids/names remain `build` and `canary`;
- no permissions, triggers, validation scope, signing, artifact, concurrency, or required-check behavior changed.


### CI run-title correction — restore PR-title-driven Actions labels

The later `run-name` experiment did not satisfy the intended per-change label contract for pull-request builds. On `pull_request` events, the top-level `run-name` expression has the PR metadata and head SHA but not the checked-out head commit message, so the resulting titles repeated the PR number / branch / SHA pattern and obscured the actual change summary.

This also left `.github/workflows/build.yml` and `.github/workflows/work-branch-canary.yml` in the runtime PR diff, forcing Full / mixed-surface classification for unrelated runtime commits.

Correction:
- restore both workflow files exactly to the current `dev` versions, removing only the experimental `run-name` additions;
- preserve all triggers, permissions, job ids, validation routing, signing, artifact, and concurrency behavior;
- return to the already verified process: update PR #181 title to the concise commit/change summary before moving the work-branch HEAD, so the default PR-triggered Actions display title is the desired `feat:/fix:/test: short summary`;
- existing workflow runs keep their historical titles and are not renamed retroactively.

This correction is CI presentation/branch hygiene only and does not affect the APK or runtime behavior.


## 2026-10-02 — Build 600 battery scheme card navigation and safe custom names

**Type:** settings UI / persistence guard / diagnostic conclusion

- Move previous/next style controls from the pager-indicator row into the gray scheme Card title row. Controls use pinned MIUIX `IconButton` behavior at 36dp with 20dp chevrons and no synthetic shadow; custom-style More remains inside the right navigation control.
- Keep the page indicator in a fixed 28dp rail so this change does not silently alter the accepted sheet-to-card vertical rhythm.
- When custom capacity remains, the Add page title shows the next localized default name (`Custom style X` / `自定义样式X`) and the prior Apply-action slot becomes an enabled `New style` action. At capacity the existing disabled limit action remains.
- Change only the Add white Card press feedback from `Tilt` to pinned MIUIX `Sink`; upstream `SinkFeedback` owns the 0.94 pressed scale.
- Replace duplicated UI `take(28)` limits with one 24-code-point rule shared by create/rename UI and the repository write path. The limiter truncates by Unicode code point rather than UTF-16 code unit so a surrogate pair is never split.
- Build-599 device diagnostics settle the late peer-icon disappearance mechanism: QS_FAKE `MiuiStatusIconContainer` stays 478px wide while transition reservation can apply 249px end padding, leaving 229px usable at ~full expansion; final QS has 645px usable. The disappearing network-speed icon is therefore native fake-carrier underflow, not source-state loss. Build 600 records this evidence only and does not alter runtime geometry.

## 2026-10-02 — Build 609 late peer disappearance: causal hypothesis reopened

**Type:** runtime diagnostics / ownership review

- Build-605 proves substantial QS_FAKE capacity pressure: the fake status-icon row remains 478px wide while transition reservation can apply ~249px end padding.
- The same terminal evidence also shows `ControlCenterFakeStatusIcons` can already be alpha 0 around fraction ~0.998 while that reservation is still present. Therefore fake-row underflow is proven to exist but is not yet sufficient evidence that it is the user-visible disappearance.
- Pre-commit review rejects widening `system_icon_area`: that would introduce a second native peer-layout writer and violate the accepted architecture where `statusIcons.paddingEnd` is the sole Guiyuan native peer-layout writer.
- Build 609 stays read-only and records bounded fake/final native row membership and visibility across the existing 8 Detailed-diagnostic progress buckets so the disappearance can be assigned to fake-row underflow or native fake->final handoff before any behavior change.


## 2026-10-02 — Build 610 QS_FAKE peer-capacity correction

**Type:** runtime root cause / layout ownership

- Build-609 device evidence closes the remaining causality gap: with the fake root still alpha 1 and the final root alpha 0, `network_speed` changes from native visibleState 0 to 2 as fake usable width falls from 320px to 291px; on return it becomes visible again at about 292px. The peer is therefore being removed by native QS_FAKE underflow before HyperOS appearance transfers visual ownership to the final row.
- The earlier Build-609 rejection of an unbounded `system_icon_area` width writer is retained as a guard, not as a ban on evidence-backed correction. Build 610 keeps one host-scoped transition-reservation owner and consumes only the live leading slack already present in the end-anchored fake parent.
- Positive transition `paddingEnd` delta and fake-carrier width expansion are equal and use the same raw HyperOS progress sample. This keeps peer usable capacity stable while preserving the existing semantic peer-X reservation. No peer visibleState, alpha, visibility, translation, native appearance threshold, timer, delay, or second animator is added.
- The width path requires concrete native layout width, sole-child parent topology, exact end anchoring, sufficient live slack, and writer exclusivity. Failure restores/keeps native behavior rather than applying a guessed geometry compensation.
- Padding is restored before carrier width on cleanup; carrier width is applied before padding during expansion, preventing Guiyuan-owned ordering from creating a transient underflow window.


## 2026-10-02 — Build 611 fixed QS_FAKE capacity lease review correction

**Type:** runtime ownership / lifecycle review

- Build 610 keeps the correct Build-609 root-cause conclusion, but its implementation changes `system_icon_area` width on each expansion sample. Review rejects that per-progress width path because HyperOS `ControlCenterFakeViewController.updateFakeStatusIconsSize()` owns the native fake-carrier baseline; making width a second progress-driven property creates avoidable writer overlap.
- Build 611 moves width ownership to one bounded session lease. After HyperOS has established a concrete fake-carrier width, Guiyuan requires `system_icon_area` to be the parent's sole zero-horizontal-margin child, snapshots the native width, and expands it once to the existing parent content width. The width stays fixed until cleanup; only the already-accepted `statusIcons.paddingEnd` reservation follows raw HyperOS progress.
- On Build-609 evidence the lease is 587 -> 837px, exposing 250px of existing parent capacity for a maximum observed 249px reservation delta. If required reservation exceeds that capacity, or the parent topology/width contract is unavailable, the fake surface fails native.
- The lease forces a fresh native status-icon layout before compact visual cutover. Existing-layout adoption and the Hot Reload transferred-layout shortcut are blocked while that layout is pending; the completed layout must still preserve the carrier's end anchor before masking can begin.
- Any later native/third-party width change during the lease is treated as a competing writer. Cleanup never overwrites such a new value; it restores only the exact Guiyuan-applied padding/width state.
- No peer visibleState, alpha, visibility, translation, appearance threshold, timer, polling path, new Hook, or second animator is added.


## 2026-10-02 — Build 612 separate QS_FAKE capacity from motion geometry

**Type:** runtime root cause / transition geometry ownership

- Build-611 device video reports an all-scene left jump immediately after Control Center entry. The matching diagnostics show the fixed capacity lease activates before expansion samples, changing QS_FAKE `system_icon_area` 587 -> 837px and `MiuiStatusIconContainer` 478 -> 728px while preserving the same end edge.
- The transition matrix still sampled the whole live fake `MiuiStatusIconContainer` as `currentCarrier`. A 250px leading-side width increase moves that View's center by 125px left, and `rebaseSourceToCurrentCarrier()` faithfully carried the whole Guiyuan source by the same amount. This is why the regression appears as a discrete initial left jump rather than a changed animation curve.
- Build 612 keeps Build 611's fixed session capacity lease because it solved the Build-609 native-peer underflow. Instead it freezes the native source motion-carrier width and, for transition sampling only, maps the live fake row to an end-anchored logical sub-carrier of that width. Lease-only leading capacity therefore participates in native measurement but not motion.
- The correction is geometry-semantic rather than numeric compensation: no 125px offset is hard-coded, no device width is assumed, RTL uses the matching start/end anchor, and the logical width comes from the live frozen source witness.
- No native View width/padding/translation/alpha/visibility writer is added. The existing capacity lease and raw-HyperOS-progress `statusIcons.paddingEnd` reservation remain unchanged.

## 2026-10-02 — Build 619 airplane / no-SIM sizing accepted on device

**Type:** device evidence / visual-geometry acceptance

- Signed Work Branch Canary #650 validated Build `20261002-619` on the exact work-branch tree; the later merge of current `dev` ancestry into the feature branch changed no files or APK/runtime bytes.
- Device evidence is from Xiaomi 15 Pro (`haotian`), Android 17 / SDK 37, HyperOS SystemUI `17.03.260226.r`.
- Maintainer device review reports the independent Airplane mode and No-SIM size controls behave as expected, including the requested 40%-125% range and separate content-layout memory, with no visible regression requiring another runtime change.
- The returned detailed diagnostic reports `overall=healthy`; module, compatibility, presentation runtime, renderer/session, airplane observer, native-network suppression, panel transition, and Keyguard renderer paths remain ready. No `FATAL EXCEPTION`, `AndroidRuntime`, `state=failed`, `state=error`, or generic exception marker is present in the collected report.
- AOD remains on the existing native/read-only path in this Build; the future independent AOD control recorded in ROADMAP is not implemented here.
- Diagnostic limitation noted: the current `visualSettings.changed` summary does not yet emit `airplaneSizeScale` / `noSimSizeScale`, so exact per-profile value evidence comes from the profile-key/unit coverage plus maintainer visual validation rather than the runtime summary line itself.
- Build 619 is accepted for integration into `dev`; no further runtime change is required for this feature.

## 2026-10-02 — Build 620 AOD continuity rejected on device; Build 621 narrow correction

**Type:** device evidence / AOD lifecycle root cause / presentation continuity  
**Branch / PR:** `feat/aod-display-control` / #196  
**Builds:** 620 -> 621

### Build 620 device evidence

- Xiaomi 15 Pro / `haotian`, Android 17 / SDK 37, HyperOS 4, SystemUI `17.03.260226.r`.
- Stable Keyguard and stable AOD Guiyuan presentation both acquire successfully with both child switches enabled.
- Maintainer video reports a visible temporary return to native status presentation whenever Keyguard and AOD switch states.
- The diagnostic matches that visual symptom directly: when `setIsAodAnimate` enters the transition state, the outgoing AOD renderer becomes ineligible, readiness drops, the AOD presentation restores clip bounds/end reservation/ignored slots, and native presentation is exposed until the destination stable scene reacquires Guiyuan.
- This is not a timing race or host-resolution failure. It is the explicit Build-620 policy that classified every AOD animation interval as native-only.

### Root-cause correction

Build 621 keeps the existing native AOD authority and changes only scene eligibility during the verified transition interval:

- with both Keyguard and AOD child switches enabled, entering AOD retains the existing Keyguard Guiyuan scene while `toAod=true && isAodAnimate=true`;
- exiting AOD retains the existing AOD Guiyuan scene while `toAod=false && isAodAnimate=true`;
- stable destination boundaries still select their independent Keyguard or AOD session;
- if either child switch is disabled, the transition remains native-only;
- the global Guiyuan master switch still releases both scenes immediately;
- AOD never becomes a Control Center transition source.

No timer, delayed cleanup, polling, copied AOD animation, native translation writer, or duplicate alpha animator is introduced. HyperOS remains the transition clock/motion authority; Guiyuan only avoids voluntarily dropping its already-valid presentation during the dual-enabled handoff interval.

### Review / automated validation

- Scene policy tests cover enter/exit retention only when both child switches are enabled, plus native fallback for single-enabled and master-disabled combinations.
- Render-session tests cover outgoing-scene eligibility continuity.
- Runtime CI #2245 passed Build 621 code/test compilation and the pinned HyperOS target-profile checks.
- Focused real-device validation remains mandatory before integration.

## 2026-10-02 — Build 621 transition-direction hypothesis rejected; Build 623 ownership-driven AOD handoff

**Type:** exact-target device evidence / AOD-Keyguard-Home presentation ownership  
**Branch / PR:** `feat/aod-display-control` / #196  
**Builds:** 621 -> 623

### Device evidence

Build 621 still visibly restored native represented status icons during both non-Keyguard Home <-> AOD switching and Keyguard <-> AOD switching.

The detailed exact-target trace shows why the Build-621 rule was invalid: while AOD animation is active, HyperOS can expose `toAod=true` together with `animToAod=false`, and later publish `toggleAodMode(false)`. Build 621 used `toAod` as transition direction, causing the actually owned scene to become ineligible, readiness to drop, and the presentation session to restore ignored slots / clip masks / reservation before the destination scene was ready.

Historical architecture already classifies `mAnimToAod` as diagnostic-only, so replacing one direction guess with another is rejected.

### Build 623 correction

- Track the authoritative steady Home/Keyguard source from the existing native scene callback.
- During `isAodAnimate=true`, resolve continuity from actual Home/Keyguard/AOD presentation ownership rather than `toAod` direction.
- Keep an already-owned AOD scene eligible for the whole native animation while AOD remains enabled.
- Keep visible Keyguard ownership during Keyguard -> AOD when AOD is enabled.
- When Home is the actual source, allow AOD render/presentation prearm during the native AOD animation so the AOD host does not expose its native represented icons first.
- When Home appears while AOD still owns presentation, defer AOD cleanup to the AOD lifecycle boundary.
- Stable scene resolution still releases unsupported/disabled destinations to native and keeps AOD out of Control Center source ownership.

No timer, delay, polling, copied AOD motion, alpha/translation writer, or project-owned animation clock is introduced.

### Validation

- Unit coverage now verifies ownership-driven animation routing, Home->AOD prearm eligibility, AOD-owner retention across scene callback ordering, and direction-independent render-session retention.
- Runtime CI #2257 passes Build 623 on the pinned HyperOS target profile.
- Signed Canary and focused device validation are still required before integration.

## 2026-10-03 — Build 623 device rejection; Build 625 Keyguard-family continuous ownership

**Type:** Keyguard/AOD presentation ownership / lifecycle handoff  
**Branch / PR:** `feat/aod-display-control` / #196  
**Builds:** 623 -> 625

### Problem

Build 623 removed AOD-direction guessing, but device testing still shows native represented status icons flashing during both Home <-> AOD and Keyguard <-> AOD transitions.

### Evidence

The retained Build-623 trace shows a structural handoff gap rather than another direction-state error:
- the outgoing Keyguard/AOD presentation loses readiness and performs cleanup, restoring clip bounds, end reservation and ignored-slot ownership;
- the target presentation then enters `prepared` with `fallbackVisual=native-until-native-layout`;
- only the next native status-icon layout marks the target presentation `active` and releases native handoff.

That sequence deliberately exposes native represented visuals between two otherwise valid Guiyuan sessions.

### Conclusion

Keyguard and AOD are distinct scene semantics but, on the verified target, they resolve to the same native Keyguard-family host. Their represented-slot suppression, reversible visual mask and end reservation therefore require one continuous host-scoped presentation owner across an internal family scene change. Likewise, one module render View should retarget scene semantics rather than detach/re-add at the same host.

Home remains a distinct host. Home -> AOD may pre-mask the target native represented visuals during explicit prearm, but compact-layout readiness remains a separate native-layout fact; pre-mask does not grant layout ownership or renderer readiness.

### Change

- Collapse separate Keyguard/AOD presentation sessions into one role-retargetable Keyguard-family Session.
- Collapse separate Keyguard/AOD render sessions into one role-retargetable Session / RenderView.
- Guard role-specific cleanup so an outgoing-role cleanup cannot stop the already-retargeted target.
- Distinguish presentation claim from compact-layout readiness for transition routing.
- Permit reversible AOD pre-mask only during explicit Home -> AOD prearm.
- Reset child alpha to 1 when leaving AOD for Keyguard.
- Keep child feature gates independent and preserve fail-native restoration when the family host/topology is invalid.

No native animation clock, timer, delay, polling loop, native translation writer or native visibility writer is added.

### Validation

- Focused unit coverage protects role-retarget cleanup, AOD pre-mask gating, independent child feature gates and AOD->Keyguard alpha reset.
- Runtime CI #2278 succeeds on reviewed runtime head `5aa8752197ce8328496d3ca68c8ee5875e98ef91`.
- Manual ownership review confirms one family presentation owner and one family RenderView.
- Signed Canary and focused device validation remain required before integration.



## 2026-10-03 — Build 625 device rejection; Build 626 retarget-before-readiness handoff

**Type:** device evidence / Keyguard-AOD family ordering / root-cause correction  
**Branch / PR:** `feat/aod-display-control` / #196  
**Builds:** 625 -> 626

### Device evidence

Build 625 keeps one Keyguard-family presentation owner and one RenderView, but maintainer testing still shows a short restoration of native represented status icons while switching into/out of AOD. The supplied Build-625 diagnostics are otherwise healthy and repeatedly show an outgoing presentation/readiness cleanup followed by the destination AOD/Keyguard presentation taking over.

### Root cause

The shared owner removed the old stop/recreate topology, but module callback ordering still created an ownership gap:

1. HyperOS AOD state is already stored by `SystemUiKeyguardAodStateSource`.
2. `onKeyguardAodStateUpdate()` first called `CombinedStatusKeyguardRenderSession.onAodState(update)`.
3. The session was still labelled as the outgoing family scene, so the new state could make that scene ineligible and publish `readiness=false`.
4. That readiness loss restored native represented slots.
5. Only after that did `onKeyguardHostResolution()` read the same new state and retarget the family session to the destination scene.

Thus the remaining flash is an ordering bug inside a single owner, not evidence that separate Keyguard/AOD sessions should return.

### Build 626 correction

Build 626 resolves/retargets the Keyguard-family scene first and delivers the same AOD update to the render session second. The destination scene therefore consumes the update under the correct family role before any old-role readiness cleanup can run.

No timing compensation, delay, timer, polling loop, direction guess, duplicate animator, new native visibility/translation writer, or host topology change is introduced.

### Review / validation

- The correction is limited to `CombinedStatusModule.onKeyguardAodStateUpdate()`.
- Build-625 family ownership, render-session reuse, pre-mask, compact-layout, child-gate and fail-native rules remain unchanged.
- Runtime CI #2289 passed the exact source change before the Build-id bump.
- Build 626 requires focused signed-Canary device validation because the defect is visible only across the live HyperOS AOD callback sequence.


## 2026-10-03 — Build 626 device rejection; Build 628 single AOD eligibility authority

**Type:** exact-device evidence / ownership authority / rejected hypothesis  
**Branch / PR:** `feat/aod-display-control` / #196  
**Builds:** 626 -> 628

### Device evidence

Build 626 remains visually incorrect on Xiaomi 15 Pro / HyperOS SystemUI `17.03.260226.r`: during AOD enter/exit, the combined presentation gives way to native battery/status icons before Guiyuan returns.

The detailed trace proves the failure occurs even after the Build-626 callback reorder:
- Keyguard RenderSession reports `sceneEligible=false` during the native AOD animation, readiness drops, and `readiness-lost:aod:setIsAodAnimate` restores clip masks, reservation and ignored slots.
- AOD can subsequently reach `presentation.cutover state=combined`, yet a later scene transfer still invokes `aod-not-eligible` and restores the same presentation state.
- Both paths occur while the shared Keyguard-family host/session architecture is otherwise healthy.

### Root cause

Build 626 fixed event ordering but left multiple eligibility writers:
1. Module/ScenePolicy selected the family projection from current ownership and native state.
2. RenderSession attach/update independently re-derived scene eligibility from raw `blocksProjection` / stable-AOD fields.
3. AOD readiness/cutover independently required stable AOD or Home prearm again.

Those secondary rules could invalidate a projection that ScenePolicy intentionally retained for continuity. The defect is therefore authority duplication, not missing delay or another AOD direction signal.

### Build 628 correction

- Make `CombinedStatusScenePolicy.resolveKeyguardAodProjection()` the sole family projection authority.
- Pass the selected scene eligibility into RenderSession instead of recomputing it there.
- Raw AOD callbacks no longer mutate RenderSession eligibility or presentation readiness.
- Keyguard/AOD readiness and final cutover query the same current ScenePolicy projection.
- Keep one family presentation Session, one RenderView, same-host retarget and role-specific release guards.
- Keep raw AOD fields for state evidence/diagnostics and Home pre-mask mode only where appropriate; they no longer constitute a second ownership policy.

No timer, delay, polling, guessed direction, duplicate animation clock or new native translation/visibility writer is introduced.

### Review / validation

- Obsolete unit coverage for the removed RenderSession eligibility policy is deleted; ScenePolicy tests remain the projection-matrix contract.
- Runtime CI passes the source-level Build-628 correction on the pinned target before the identity/docs closure.
- Signed Canary/device evidence remains mandatory before integration.


## 2026-10-03 — Build 628 device finding; Build 630 family-child alpha correction

**Type:** device evidence / render alpha ownership / root-cause correction  
**Branch / PR:** `feat/aod-display-control` / #196  
**Builds:** 628 -> 630

### Device evidence

Build 628 fixes the earlier native represented-icon restoration: during AOD/Keyguard family switching, native Wi-Fi/mobile/battery no longer flash back. The remaining visible defect is different: the Guiyuan combined visual itself disappears for roughly ten video frames before returning.

The matching detailed trace keeps the family projection healthy across the same interval: readiness stays true, scene eligibility stays true, and presentation cutover remains combined. This excludes another presentation cleanup/reacquire gap.

### Root cause

Build 628 still applied AOD alpha with:

`renderView.alpha = batteryView.alpha`

for the whole combined child.

Exact-target architecture evidence already distinguishes HyperOS AOD animation layers: `animateFullAod()` independently drives Battery alpha/AOD state and status-icon alpha/visibility. Battery alpha can therefore reach zero while the verified family/status-icons carrier remains the correct owner for Guiyuan. Copying Battery alpha onto the entire module child incorrectly turned one native child animation into Guiyuan's global alpha clock.

### Build 630 correction

- Keep the module child alpha at 1 whenever ScenePolicy/presentation says the family overlay is visible.
- Continue inheriting the verified `system_icons` parent carrier's native visibility/motion; no native parent alpha/visibility is written.
- Keep Battery, status-icons and system-icons alpha as read-only diagnostics so any surviving device artifact can be assigned to the correct native layer.
- Preserve Build-628 single ScenePolicy eligibility authority, one family Session/RenderView and role-retarget ownership.

No custom AOD animator, delay, timer, threshold, native alpha writer or duplicate scene state machine is added.

### Review / validation

- Unit coverage now locks that family child alpha does not copy independent Battery AOD alpha.
- Exact Build-630 Runtime CI #2322 succeeds.
- Focused signed-Canary device evidence remains mandatory.


## 2026-10-03 — Build 630 device narrowing; Build 634 ancestor-visibility diagnostic

**Type:** device evidence / cross-host AOD handoff / diagnostic checkpoint  
**Branch / PR:** `feat/aod-display-control` / #196  
**Builds:** 630 -> 634

### Device evidence

Build 630 succeeds for Keyguard <-> AOD: the earlier Guiyuan-only blank interval is gone and native represented icons do not flash back.

One edge remains: Home -> AOD flashes once.

The matching Build-630 trace shows:
- Home -> AOD traverses HOME -> KEYGUARD -> AOD;
- Keyguard presentation reaches `state=combined` almost immediately after the steady scene changes;
- family RenderSession readiness remains true;
- AOD retarget later reuses the same family session and remains `overlayVisible=true`;
- `childAlpha=1` throughout the relevant AOD callbacks;
- `systemIconsAlphaReadOnly=1` while the flash can still be observed.

This excludes the Build-628/630 child-alpha failure and another native-slot ownership gap.

### Exact-target evidence

SystemUI-Reference verifies:
- `MiuiKeyguardStatusBarView`;
- field `mKeyguardStatusBarContent`;
- `mStatusIconAnim`, `mHideStatusIconAnim`, `mShowStatusIconAnim`;
- method `animateIconContainer(boolean)`;
- `KeyguardStatusBarViewControllerInject.animateFullAod(boolean, boolean)`.

The existing reference rule also states that a child / ViewOverlay inherits its host ancestor visibility and alpha lifecycle. Therefore `system_icons.alpha == 1` alone does not prove the on-screen family host is visible.

### Build 634 diagnostic

- Add read-only visual-chain summaries for:
  - `MiuiKeyguardStatusBarView`;
  - reflected `mKeyguardStatusBarContent`;
  - `mSystemIconsContainer`.
- Each summary records:
  - each ancestor class;
  - `visibility`;
  - local `alpha`;
  - `isShown`;
  - accumulated effective alpha / all-visible result.
- Emit these fields on the existing AOD state diagnostic path.

No visual behavior, ownership, masking, scene policy, geometry, timer, animator, delay, or native writer is changed.

### Decision gate

One detailed Home -> AOD device trace from Build 634 is required. The first ancestor whose effective visibility collapses during the flash determines the correct bridge host. Do not introduce a root-level bridge before that evidence identifies the failing native layer.


## 2026-10-03 — Build 634 device audit; Build 636 same-scene lifecycle dedupe

**Type:** AOD lifecycle audit / ownership hygiene  
**Branch / PR:** `feat/aod-display-control` / #196  
**Builds:** 634 -> 636

### Device evidence

The Build-634 trace captures repeated Home -> AOD transitions. The early transfer passes through KEYGUARD while `MiuiKeyguardStatusBarView` is not yet shown; later native AOD callbacks show the complete Keyguard host/content/system-icons chain visible with effective alpha 1.

The same device observation also confirms stock HyperOS performs a visible Home -> AOD status-bar transition because the visual style changes across that boundary. Therefore a single flash is not by itself proof of a Guiyuan bug.

### Lifecycle audit

Across the captured session there is no:
- Keyguard/AOD RenderSession detach;
- Keyguard/AOD presentation cleanup or inactive event;
- host replacement;
- fail-native path;
- represented-slot suppression release/reacquire.

The family renderer and presentation both reuse their existing same-host Session objects. Repeated `renderer.attach` / `presentation.cutover=combined` diagnostics are therefore not real re-attachments.

One unnecessary lifecycle action remains: `CombinedStatusKeyguardRenderSession.Session.retarget()` always calls `dispatchPresentationReadiness(..., force=true)`, even when the target family scene has not changed. That causes repeated cutover work and mask/reservation refreshes under multiple callbacks, although the Session itself remains stable.

### Build 636 correction

- Force readiness dispatch only when the family scene actually changes.
- Same-scene retargets rely on the normal readiness-delta check.
- KEYGUARD <-> AOD role changes still force dispatch so the presentation surface can retarget while readiness remains true.

No cross-host bridge, timer, delay, alpha patch, visibility patch, geometry writer, or native animation override is introduced.

### Review intent

This correction is lifecycle hygiene, not an attempt to eliminate native Home -> AOD visual switching. Future AOD work should only resume if device evidence shows Guiyuan adds an extra artifact beyond the stock transition.


## 2026-10-03 — Build 643: single-child AOD handoff + authoritative unlock boundary

**Type:** device evidence / lifecycle root cause / performance isolation  
**Branch / PR:** `feat/aod-display-control` / #196  
**Builds:** 636 -> 643

### Device evidence

Build 636 exposed three related lifecycle symptoms on the AOD branch:
- Keyguard ON / AOD OFF: AOD -> Keyguard waits for the late native AOD-animation teardown callback before Guiyuan reappears.
- Keyguard OFF / AOD ON: AOD -> Keyguard retains Guiyuan AOD ownership after the verified steady source has already returned to Keyguard.
- With both Keyguard and AOD enabled, lockscreen -> Home -> immediate Control Center pull-down reproducibly can show native status-bar icons on the first fast pull. Ordinary Home pull-down can also occasionally feel non-responsive/stuttery.

### Root causes

1. Single-enabled Keyguard/AOD children were still using the dual-enabled animation-interval retention rule, so ownership transfer lagged the verified steady source boundary.
2. HOME scene updates entered AOD ownership/cleanup code even when no AOD runtime was attached, adding avoidable synchronized lifecycle work to ordinary desktop pull-down.
3. A stale Keyguard Control Center lease could survive the verified unlocked HOME boundary. Because the fraction callback can arrive before the next visible/source callback, the first fast pull after unlock could consume cached `controlCenterSourceScene=KEYGUARD`. Existing HOME handling then deferred the source rewrite because the stale lease was already active, producing a short native-QS fallback window.
4. Cleanup ordering could also refresh Control Center eligibility from stale KEYGUARD state if Keyguard runtime teardown happened before HOME source ownership was committed.

The existing ScenePolicy already rejects retaining a Keyguard Control Center lease for HOME; the defect was the module feeding it cached KEYGUARD state rather than honoring the newly verified HOME source.

### Build 643 correction

- Keyguard-only mode acquires Keyguard as soon as verified steady Keyguard is observed.
- AOD-only mode releases AOD at the same verified steady-Keyguard boundary once Home no longer owns the source.
- AOD-only Home -> AOD preserves/prearms AOD ownership across the transient native Keyguard source while Home ownership is still present.
- Ordinary HOME updates skip AOD ownership/cleanup work entirely when no AOD runtime is attached.
- `UNLOCKED_STATUS_BAR + HOME ancestry` is now authoritative over any stale Keyguard Control Center lease:
  - release old lease with no Keyguard-readiness reconciliation;
  - publish Control Center source ownership as HOME;
  - only then tear down the old Keyguard renderer.
- `aodRuntimeAttached` is reset on AOD activation failure, readiness loss, fail-native, Keyguard retarget, explicit deactivation, hot reload takeover, and old-generation teardown.

No timer, delay, polling, guessed transition direction, geometry writer, or copied native animation was added. Build-621 `toAod/animToAod` direction inference remains rejected.

### Device gate

Validate:
- Keyguard ON / AOD OFF: AOD -> Keyguard has no delayed Guiyuan entrance.
- Keyguard OFF / AOD ON: AOD -> Keyguard has no delayed Guiyuan exit; Home -> AOD has no transient native represented-icon frame.
- Keyguard ON / AOD ON: lockscreen -> Home -> immediate fast pull-down repeatedly; first pull must stay Guiyuan-projected with no native-status-bar flash.
- Repeated ordinary Home pull-downs: no intermittent stutter attributable to AOD lifecycle bookkeeping.
- Dual-enabled Keyguard <-> AOD continuity remains unchanged.


### Renderer-lifetime review correction

A post-fix review found that the initial AOD hot-path flag was being cleared on presentation readiness loss / presentation failure even though the AOD RenderSession can remain attached. Build 642 corrects the flag semantics:
- track renderer lifetime, not presentation readiness;
- set true only after AOD renderer attach;
- clear only on Keyguard retarget, explicit AOD detach, hot reload takeover, or old-generation teardown;
- presentation readiness/failure does not suppress a later real renderer cleanup.


### Hidden Control Center lifecycle isolation

Build-636 diagnostics showed `controlCenterProjection state=native authority=keyguard-readiness-lost controlCenterVisible=false`: Keyguard lifecycle loss was mutating Control Center projection while Control Center was not even open. Build 643 isolates that path:
- when Control Center is hidden, native fraction is zero, and no Keyguard Control Center lease is active, Keyguard readiness-lost/fail/deactivate no longer changes Control Center projection;
- the next native Control Center visible callback resolves the actual `realSystemIcons` source scene and selects HOME/KEYGUARD projection from current state;
- active/visible Control Center and a live Keyguard lease still reconcile immediately.


## 2026-10-03 — Build 645: outgoing-child ownership fixes Keyguard/AOD cutover

**Type:** device evidence / Keyguard-AOD lifecycle ownership  
**Branch / PR:** `feat/aod-display-control` / #196  
**Builds:** 643 -> 645

### Build-643 evidence

Device feedback identified three failures:
- with Keyguard and AOD Guiyuan both enabled, AOD -> Keyguard followed by an immediate fast Control Center pull can start with no Guiyuan transition and later jump into the already-progressed transition;
- with Keyguard enabled and AOD disabled, Keyguard -> native AOD keeps Guiyuan visible too long;
- with Keyguard disabled and AOD enabled, the Keyguard screen can still show Guiyuan.

The detailed log proves the third issue is a child-ownership defect rather than preference propagation: Keyguard feature teardown completes, then the enabled AOD renderer is attached from the same `feature-settings` update and AOD presentation becomes active on the Keyguard host.

### Root cause

The animating family policy treated an existing AOD/Keyguard presentation claim mainly as a retention preference. During a real family transition, however, that claim is stronger evidence of the **outgoing** child:
- outgoing AOD + verified steady Keyguard means transfer to the enabled Keyguard child, or Native if Keyguard is disabled;
- outgoing Keyguard during AOD animation means transfer to the enabled AOD child, or Native if AOD is disabled.

The Home -> AOD transient Keyguard interval remains distinguishable through still-owned Home presentation and does not require `toAod` / `animToAod`.

### Build-645 correction

- Interpret family presentation ownership as outgoing-child evidence during AOD animation.
- Preserve Home-owned AOD prearm as the higher-priority Home -> AOD exception.
- AOD-owned + steady Keyguard -> Keyguard when enabled, otherwise Native.
- Keyguard-owned + AOD animation -> AOD when enabled, otherwise Native.
- No currently-owned family child + steady Keyguard -> acquire Keyguard when enabled.
- UNKNOWN scene preserves only a currently-owned enabled child; otherwise Native.

The existing single family RenderSession is retargeted in place. No timer, polling, second owner, native geometry writer, or rejected direction-field inference is introduced.


## 2026-10-03 — Build 651: stable family-scene latch and authoritative first-pull source

**Type:** device evidence / lifecycle state-machine correction  
**Branch / PR:** `feat/aod-display-control` / #196  
**Builds:** 645 -> 651

### Build-645 evidence

Device feedback showed:
- both child features ON: AOD -> Home and Keyguard -> Home followed by immediate fast Control Center pull can probabilistically remain native for the whole gesture;
- disabling Keyguard Guiyuan does not reliably keep Keyguard native;
- with AOD Guiyuan disabled, AOD <-> Keyguard switching can expose native represented icons for one interval before Guiyuan reappears;
- AOD -> Keyguard + immediate partial pull can start native and recover only after holding the gesture.

The diagnostic repeatedly records:
`prepared/active -> cleanup(keyguard-aod-native or cutover-projection-ineligible) -> fresh attach`.

### Root cause

Build 645 interpreted **current presentation ownership** as AOD-animation direction evidence. Ownership is mutable output of the same attach/cleanup state machine, so repeated native callbacks can flip the decision after each mutation and create a self-sustaining KEYGUARD -> NATIVE -> KEYGUARD oscillation.

Two related lifecycle races also remained:
- direct AOD -> Home could clear family origin before the outgoing AOD animation finished, allowing a later callback to look like Home -> AOD prearm;
- the first Control Center visible callback after unlock can still carry stale KEYGUARD `realSystemIcons` identity even after the scene source has authoritatively reached HOME.

### Build-651 correction

- Add `StableKeyguardAodScene { UNKNOWN, KEYGUARD, AOD }`.
- Update it only from non-animating runtime evidence; freeze it for the duration of AOD animation.
- Remove `keyguardPresentationOwned/aodPresentationOwned` from ScenePolicy direction inputs.
- Latched KEYGUARD routes only to enabled AOD, else Native.
- Latched AOD routes only to enabled Keyguard, else Native.
- Home -> AOD prearm is allowed only from UNKNOWN family history while Home still owns the visible source.
- AOD -> Home retains AOD history through the outgoing animation and clears it only after stable HOME + non-animating + `toAod=false` evidence.
- Verified steady scene overrides a stale first Control Center callback source after lifecycle transitions.
- Feed the same effective scene to Control Center eligibility and TransitionOwner.
- Reset the family latch on both hot-reload reset and old-generation teardown.

### Review notes

Lifecycle review additionally verified:
- SceneUpdate and AOD state are both sourced from `MiuiBatteryMeterView`, so the latch reads the same host object rather than crossing view classes;
- hidden Control Center remains isolated from unrelated Keyguard readiness churn;
- the family renderer remains single-writer;
- no `toAod/animToAod` direction inference is restored.

No timer, delay, polling, extra owner, native visibility/alpha/geometry write, or copied SystemUI animation is introduced.

## 2026-10-03 — Build 652: direction-aware first-pull source and outgoing-child retention

**Type:** device evidence / lifecycle boundary correction  
**Branch / PR:** `feat/aod-display-control` / #196  
**Builds:** 651 -> 652

### Problem

Build-651 device validation found three related boundary failures:
- with both child features enabled, AOD -> Keyguard or Keyguard -> Home followed by an immediate fast Control Center pull can begin native and recover only after the gesture is held;
- with Keyguard projection disabled, steady Keyguard can still be occupied by the AOD child and Home Control Center can become native;
- with AOD projection disabled, Keyguard -> AOD restores native represented icons while the visible Keyguard scene is still outgoing.

### Evidence

The returned diagnostic captures Control Center becoming `state=native` with effective `sourceScene=KEYGUARD` and `keyguardRuntimeReady=false`, immediately while the native panel callback already reports `sourceScene=HOME`. A later stable-family callback restores Keyguard readiness and reattaches the projection, matching the visible late recovery.

Code review also found:
- the non-animating AOD-only shortcut treated `homePresentationOwned` as sufficient evidence to attach AOD on a steady Keyguard source;
- the animating route let stale Home ownership override a latched family origin;
- a disabled destination child forced Native at animation start rather than at the stable destination boundary.

### Conclusion

Neither native source witness is globally newest. A HOME/KEYGUARD disagreement needs transition direction, not a fixed winner. The existing non-animating stable-family latch supplies that direction without borrowing mutable ownership: a latched KEYGUARD/AOD child is outgoing toward Home, while UNKNOWN is the stable-Home origin entering the family.

AOD prearm must require an actual animation plus UNKNOWN family origin. During a native Keyguard/AOD animation, a disabled destination does not justify restoring native beneath a still-visible enabled outgoing child; retain the outgoing child until stable target evidence, then enforce the disabled child.

### Change

- Resolve HOME/KEYGUARD Control Center conflicts from stable-family history.
- Feed the same resolved source to projection eligibility and TransitionOwner.
- Remove non-animating speculative AOD prearm.
- Restrict transient-Keyguard AOD prearm to UNKNOWN family origin.
- Retain the enabled outgoing family child when the destination child is disabled, only for the native animation interval.
- Keep stable disabled child states native.

No timer, delay, polling, duplicate owner, native geometry/alpha/visibility writer, or `animToAod` direction inference is added.

### Validation

Focused tests cover both unlock and lock conflict directions, prearm gating, stale Home ownership, and disabled-destination outgoing retention. Exact-HEAD Runtime is required, followed by signed Canary device validation because the change affects scene ownership and first-pull lifecycle ordering.

## 2026-10-03 — Build 653: visible-host scene authority and native visual cutover

**Type:** device evidence / lifecycle authority correction  
**Branch / PR:** `feat/aod-display-control` / #196  
**Builds:** 652 -> 653

### Device evidence

Build-652 validation separates two remaining defects:
- with Keyguard enabled, Keyguard/AOD -> Home followed by an immediate fast Control Center pull can start fully native and remain native for the whole gesture;
- single-child Keyguard/AOD mode now releases the outgoing Guiyuan presentation too late because Build 652 waits for the native AOD animation flag to end.

### Root cause

`SystemUiSceneStateSource` hooks class-wide `MiuiBatteryMeterView.updateState()`. More than one Home/Keyguard Battery view can publish structurally valid state in the same lifecycle interval. Build 652 stored the last matching callback in one global `steadyStatusSourceScene` without checking whether that source host was actually shown.

Historical Build-634 evidence already proved that HyperOS can report Keyguard ancestry while `MiuiKeyguardStatusBarView` is not shown. A hidden structural callback is therefore observation/host-discovery evidence, not ownership authority.

Waiting for `isAodAnimate=false` is also not the visual cutover boundary. Exact-target evidence shows `animateFullAod()` independently changes native Keyguard status-icon/Battery visual state during the animation.

### Build-653 correction

- Separate structural host discovery from visible scene ownership.
- Only a shown Home/Keyguard native host can update the steady source and stable-family latch.
- Hidden Keyguard events still feed the host resolver so Home -> AOD can prepare the verified family host without granting it scene authority.
- Add a read-only native Keyguard status-presentation fact: the host must be shown and either status-icons or Battery must be shown with positive native alpha.
- In Keyguard-only mode, hand off to Native as soon as that Keyguard presentation has yielded during AOD animation.
- In AOD-only mode, hand off to Native only when qualified visible KEYGUARD source evidence and visible native Keyguard presentation agree.
- If the native visual fact is unavailable, keep the existing conservative retention path.
- Dual-enabled family transfer remains driven by the stable family latch.

No timer, delay, polling, alpha/visibility/geometry writer, copied native animator, or `animToAod` direction inference is introduced.



## 2026-10-03 — Build 654: close QS_FAKE lease per visible cycle and restore structural scene authority

**Type:** device evidence / lifecycle ownership correction  
**Branch / PR:** `feat/aod-display-control` / #196  
**Builds:** 653 -> 654

### Device evidence

Build-653 validation rejected the visible-host scene gate:
- steady Keyguard could become fully native even with Keyguard projection enabled;
- AOD/Keyguard -> Home immediate fast Control Center pull could remain native;
- when only one family child was enabled, AOD <-> Keyguard could briefly show Guiyuan before returning native.

The diagnostic additionally captures a failing Home pull where HyperOS already reports `sourceScene=HOME`, followed by `controlCenterPresentation failNative reason=fake-carrier-width-writer-conflict`.

### Root cause

Two independent ownership mistakes remained.

First, `View.isShown` is a visual/lifecycle fact during HyperOS Keyguard/AOD animation, not a valid steady scene authority. Build 653 used it to reject structurally valid Keyguard state and therefore removed the steady Keyguard owner.

Second, QS_FAKE carrier-width ownership was scoped to the fake-root session instead of the visible Control Center cycle. HyperOS can restore the fake carrier width when Control Center closes while the root stays attached. The next pull then sees a live native width different from Guiyuan's previous leased width and correctly trips the existing single-writer conflict guard.

For single-child AOD/Keyguard handoff, the whole-host/Battery visibility aggregate was also too broad: native Battery AOD animation is independent of the Keyguard status-icons layer.

### Build-654 correction

- Restore Build-652 structural ancestry + native status-bar-state steady-scene authority.
- Do not use `isShown` to choose steady scene ownership or Keyguard host ownership.
- Preserve the Build-652 direction-aware Control Center source arbitration; the Build-653 failing trace already resolves effective HOME before failing, so source routing is not the defect.
- Preserve the prearmed QS_FAKE presentation owner, represented-slot exclusion, clip masks and compact readiness across pulls. On a real visible -> hidden boundary, release only the carrier-width capacity lease and suppress hidden-state lease reacquisition; remember the native hidden-boundary layout width as a one-cycle read-only baseline witness so the next visible cycle does not confuse pending layout with a writer conflict, then clear that witness immediately after lease reacquisition.
- Use only the read-only native Keyguard status-icons alpha/visibility as the single-child visual cutover witness; Battery AOD alpha does not extend the whole combined child.
- Preserve the existing dual-enabled single-family renderer/presentation owner.

### Review boundaries

No timer, retry loop, polling, native alpha/visibility/translation writer, geometry hard patch, or second presentation owner is added. The existing width-conflict guard remains fail-native for genuine concurrent writers; Build 654 narrows only the capacity-lease lifetime while retaining the already-verified long-lived QS_FAKE prearm owner.

### Validation

Focused tests preserve the existing direction-aware Control Center arbitration and cover visible-cycle capacity-lease release, Keyguard status-icons cutover and Home->AOD prearm ordering. Exact-HEAD Runtime and signed Canary device validation are required.

## 2026-10-03 — Build 655: use native full-AOD target boundary for single-child handoff

**Type:** exact-device timing evidence / Keyguard-AOD native lifecycle source  
**Branch / PR:** `feat/aod-display-control` / #196  
**Builds:** 654 -> 655

### Build-654 device result

The major Build-653 regressions are no longer the reported blocker. Remaining focused timing feedback is directional:

- Keyguard OFF / AOD ON: AOD -> Keyguard is about half a beat late.
- Keyguard ON / AOD OFF: AOD -> Keyguard is about half a beat late; Keyguard -> AOD is about half a beat early.
- With both enabled, Home -> AOD can still flash once; the tester accepts that flash for this gate.

The diagnostic shows why a symmetric alpha/end-state adjustment cannot solve both directions. Keyguard -> AOD drops the Keyguard presentation at the first `setIsAodAnimate(true)` edge, before the later AOD-mode callback. AOD -> Keyguard does not rebuild until the final `setIsAodAnimate(false)` stable-family edge. Once that late edge arrives, renderer attach and native-layout cutover complete within a few milliseconds, so layout readiness is not the source of the visible delay.

### Build-655 candidate

The exact target already verifies `KeyguardStatusBarViewControllerInject.animateFullAod(boolean, boolean)` and `MiuiKeyguardStatusBarView.mToLockScreen`. Build 655 adds a read-only event hook at that full-AOD callback and re-evaluates the existing family owner after native code has committed its target state.

For single-child mode only:
- `mToLockScreen=true` selects Keyguard if Keyguard Guiyuan is enabled, otherwise Native;
- `mToLockScreen=false` selects AOD if AOD Guiyuan is enabled, otherwise Native.

The target rule is used only with a known stable family origin. UNKNOWN-origin Home -> AOD prearm remains higher priority, and dual-enabled family routing is unchanged. Status-icons alpha remains the compatibility fallback when the exact full-AOD target source is unavailable.

No raw `animateFullAod` boolean argument is assigned product semantics. No native view geometry/alpha/visibility is written, and no local timing constant or animation is introduced.

### Device hypothesis

This should move AOD -> Keyguard earlier from animation completion to the native full-AOD target switch, while moving Keyguard -> AOD later from the generic `isAodAnimate` start flag to that same native target switch. Real-device Build-655 evidence is required before promotion.



## 2026-10-03 — Build 656: separate full-AOD direction from status-icon visual cutover

**Type:** device evidence / native lifecycle boundary correction  
**Branch / PR:** `feat/aod-display-control` / #196  
**Builds:** 655 -> 656

### Build-655 device result

The exact full-AOD target removed the previous late handoff, but both focused single-child handoffs now occur about half a beat early: AOD -> Keyguard and Keyguard -> AOD.

The Build-655 diagnostic shows Guiyuan switching immediately after `aod.target` / `animateFullAod`, before HyperOS has progressed the native status-icon presentation to its visual handoff. This proves `mToLockScreen` is useful direction evidence but is not itself the visual cutover event.

### Build-656 correction

- Keep `animateFullAod` and `mToLockScreen` read-only and use them only to latch the pending target direction.
- Add an exact-target read-only event hook on `MiuiKeyguardStatusBarView.animateIconContainer(boolean)`; its raw Boolean argument is diagnostics only.
- While the target is pending, a single-child family retains the enabled outgoing child, or Native when that outgoing child is disabled. Later `setIsAodAnimate` callbacks therefore cannot recreate Build-655's early cut.
- The pending target may be consumed only by the native status-icon animation event. At that event, policy reads the already-committed `mToLockScreen` target and switches to the enabled incoming child or Native.
- A non-animating native AOD state clears the pending latch. If the status-icon animation hook is unavailable, routing falls back to the Build-654 status-icons-alpha compatibility path.

No timer, delay, polling loop, copied native duration/interpolator, native View alpha/visibility/translation writer, geometry hard patch, or second family owner is introduced.


### Final review refinement

The status-icon callback may occur inside `animateFullAod(...)` itself. Build 656 therefore opens its bounded pending scope **before** calling native `animateFullAod`, lets HyperOS run normally, and allows `animateIconContainer(...)` to consume the scope if it occurs during that call. The post-`animateFullAod` callback is diagnostics/direction confirmation only and never performs the cutover. This avoids missing a nested native lifecycle event and avoids re-entrant assumptions about callback order.

The exact target verifies the method contract but not that method entry is a visual midpoint. Device validation remains mandatory: Build 656 is testing whether this native icon-container lifecycle boundary matches the observed visual handoff, not imposing a synthetic midpoint.


## 2026-10-03 — Build 657: retain reverse pending and prearm Home -> AOD owner

**Type:** device evidence / lifecycle handoff correction  
**Branch / PR:** `feat/aod-display-control` / #196  
**Builds:** 656 -> 657

### Build-656 device result

Focused validation separates three paths:
- Keyguard -> AOD now has acceptable timing.
- AOD -> Keyguard is late.
- Home -> AOD is late and can visibly lose the composed indicator before it returns.

The 656 diagnostic provides direct evidence for the reverse delay. A full-AOD window opens, but the post-`animateFullAod` snapshot still reports the old non-animating AOD state, so 656 clears `keyguardAodFullTargetPending`. The native `animateIconContainer(true)` event arrives later and is then ignored; the family handoff waits until final stable Keyguard evidence.

Home -> AOD has a different ownership problem. Native full-AOD/status-icon events can begin while SystemUI still reports `UNLOCKED_STATUS_BAR`, and the status-icon callback can precede `isAodAnimate=true`. Waiting for that generic animation flag means the outgoing Home host can yield before the AOD owner has been prepared.

### Build-657 correction

- Remove the post-`animateFullAod` pending clear. The single-child pending lease now survives until the native status-icon event consumes it or a later non-animating AOD state closes the transition.
- Preserve the Build-656 Keyguard -> AOD status-icon boundary unchanged.
- Add a bounded Home -> AOD target-prearm lease. It can arm only from an authoritative native AOD target while the observed origin is Home, family history is UNKNOWN, AOD is enabled, and Home still owns represented slots.
- After arm, the lease may bridge transient Keyguard ancestry until native AOD state catches up. This is a continuity lease, not a new scene router.
- The existing AOD presentation session uses its verified pre-mask / compact-layout cutover path while the outgoing Home renderer continues to inherit its own native host lifecycle.
- Explicit AOD runtime teardown, stable non-animating state, feature ineligibility, or a reverse/non-Home target ends the prearm lease.

No timer, delay, polling, project-owned animation progress, native alpha/visibility/translation writer, or geometry compensation is added.


## 2026-10-03 — Build 660: latch Home origin before native full-AOD mutation

**Type:** device evidence / lifecycle authority correction  
**Branch / PR:** `feat/aod-display-control` / #196  
**Builds:** 657 -> 660

### Build-657 device result

Focused validation still reproduces one ownership gap:
- Keyguard OFF / AOD ON and dual-enabled: Home -> AOD briefly loses Guiyuan and then reacquires it.
- Keyguard ON / AOD OFF: Home -> AOD briefly loses Guiyuan, then transient Keyguard ancestry can reacquire Guiyuan before stable native AOD; AOD -> Keyguard still appears late.

The diagnostic contains no successful `aod.homePrearm` event. It also shows that `animateIconContainer(false)` may run inside `animateFullAod` before the post-call native target snapshot is processed. Build 657 therefore asks mutable Home scene/ownership state for evidence after HyperOS has already started changing that state.

A second lifetime defect remains in reverse single-child handoff: the pending full-AOD lease records only “pending”, not the target endpoint. A later non-animating snapshot can therefore close it without proving that the target endpoint was reached.

### Build-660 correction

- Capture only the Home/UNKNOWN origin witness and Home represented-slot ownership at `animateFullAod` entry. This is frozen origin evidence, not direction inference.
- Continue reading direction only from native `mToLockScreen`.
- Continue using `animateIconContainer` as the native status-icon visual boundary.
- Once native direction confirms AOD, allow the frozen Home origin to arm the existing AOD family owner even if mutable scene ancestry or current Home ownership has already moved.
- If AOD projection is disabled, the same verified Home-origin/AOD-target pair resolves to Native, preventing transient Keyguard ancestry from becoming an unintended Guiyuan child.
- Persist the pending native target and release that lease only when a later non-animating AOD state matches the same endpoint.

No timer, delay, polling, copied animation timeline, native alpha/visibility/translation writer, geometry compensation, or second family owner is introduced.

## 2026-10-03 — Build 662: preserve native boundary through synchronous handoff

**Type:** device evidence / lifecycle reentrancy and origin-authority correction  
**Branch / PR:** `feat/aod-display-control` / #196  
**Builds:** 660 -> 662

### Problem

Build 660 closes the Keyguard-OFF/AOD-ON handoff, but two focused defects remain:
- with AOD disabled, AOD -> Keyguard is visibly late although Keyguard -> AOD is responsive;
- with both family projections enabled, Home -> AOD still loses Guiyuan briefly and then reacquires it.

### Evidence

The Build-660 AOD -> Keyguard trace reaches native `animateIconContainer(true)`, immediately attaches a layout-ready Keyguard renderer, and then emits `keyguardRenderReadiness source=stop` in the same synchronous call stack. Roughly 0.38 s later the stable Keyguard state attaches it again. The visual delay therefore comes from re-entrant policy evaluation after the correct native boundary, not from a late boundary or slow layout.

For Home -> AOD, device video shows an actual composed-indicator disappearance/reappearance interval. At `animateFullAod:before`, Home still owns represented slots but `homeOriginLatched=false`; requiring historical `lastStableFamily=UNKNOWN` lets stale family history veto current Home evidence. The reverse AOD -> Keyguard trace also proves that Home ancestry/ownership can linger during stable AOD, so Home ownership alone is not sufficient authority.

### Conclusion

The exact native status-icon boundary must remain authoritative throughout its synchronous renderer/readiness/cutover re-entry. Home origin must be qualified by the current native AOD state rather than historical family state alone.

### Change

- Add a bounded in-call visual-boundary dispatch flag around `animateIconContainer` handling. Synchronous Keyguard readiness and cutover checks reuse that same boundary context; it is cleared in `finally` before returning to normal policy.
- At full-AOD entry, latch Home origin only when Home is the current steady source, Home owns represented slots, and the native Keyguard/AOD state is explicitly non-AOD and non-animating.
- An explicit native-qualified Home latch may override stale family history; stable or animating AOD cannot create that latch.
- Preserve the Build-660 target-matched pending lifetime and the accepted Keyguard-OFF/AOD-ON path.

No timer, delay, polling, copied animation timeline, native alpha/visibility/translation writer, geometry compensation, or second family owner is added.

### Validation

Focused policy tests cover the native-state-qualified Home origin and stale-family override after that explicit witness. Exact-HEAD Runtime and signed Canary device validation are required.

## 2026-10-03 — Build 663: recover from boundary layout takeover

**Type:** device rejection / ownership correction  
**Branch / PR:** `feat/aod-display-control` / #196  
**Builds:** 662 -> 663

### Device evidence

Build 662 does not improve the reported timing and introduces a new AOD -> Keyguard regression in the Keyguard-enabled/AOD-disabled mode. Video shows the represented native mobile/battery row exposed as separated, stationary-looking components before Guiyuan finally composes.

The diagnostic provides the ownership ordering: native `animateIconContainer(true)` is observed, then the Keyguard presentation writes its end reservation about 2 ms later, while stable Keyguard is not reached until roughly 0.38 s later. Build 662 therefore moved ignored-slot/end-reservation ownership into the middle of HyperOS's own status-icon animation.

The Build-662 Home-origin change also has no accepted device benefit; Home -> AOD still flashes.

### Conclusion

The native status-icon boundary is useful as a **visual** boundary but is too early for Guiyuan to mutate native compact layout. Timing and layout ownership must be separated.

### Build-663 change

- Restore the Build-660 Home-origin policy and remove Build-662's synchronous boundary-layout propagation.
- Only for incoming Keyguard from stable AOD when Keyguard projection is enabled and AOD projection is disabled, create a visual-only handoff lease at `animateIconContainer(true)`.
- During that lease, the same presentation owner clip-masks represented native views and exposes the already-ready Guiyuan Keyguard renderer, but explicitly blocks ignored-slot/end-reservation writes and compact-layout completion.
- At stable Keyguard, commit the deferred native layout ownership while the represented native views remain masked; then finish the normal compact-layout cutover.
- Any failure restores native clip state and fails native.

No timer, delay, polling, copied animation timeline, native alpha/visibility/translation writer, geometry compensation, or second presentation owner is added.

## 2026-10-03 — Build 664: prearm Keyguard visual lease at native target commit

**Type:** device-evidence timing/ownership correction  
**Branch / PR:** `feat/aod-display-control` / #196  
**Builds:** 663 -> 664

### Problem

Build 663 removes the Build-662 layout takeover regression, but AOD -> Keyguard still shows native represented icons at the beginning of the transition before Guiyuan becomes visible.

### Evidence

The Build-663 diagnostic separates the two boundaries:
- `animateFullAod:after` reports authoritative native target `keyguard` at 11:10:22.014;
- `animateIconContainer(true)` / visual-only handoff is not reached until 11:10:22.304, roughly 290 ms later;
- at the 663 visual lease, `ignoredSlotsWrites=0 paddingWrites=0`, so the two-phase ownership split is working;
- stable Keyguard arrives at 11:10:22.700, then deferred ignored-slot/end-reservation ownership is committed and native compact layout completes at 11:10:22.705.

Video evidence matches that interval: the raw native row appears at the transition start/left position before the compact Guiyuan presentation takes over.

### Conclusion

`animateIconContainer(true)` is too late to **start** visual suppression for this single-enabled AOD -> Keyguard path. It remains a valid native status-icon lifecycle event, but the authoritative `mToLockScreen=true` commit is already available earlier and can safely prearm only reversible visual ownership.

### Change

- Start the existing Build-663 visual-only Keyguard lease at `animateFullAod:after` when the native target is Keyguard and policy proves stable-AOD -> enabled-Keyguard / disabled-AOD.
- Allow that explicitly armed lease to project the Keyguard renderer before `isAodAnimate=true` reaches the AOD state source.
- Keep native ignored slots, end reservation, compact layout and stable cutover deferred exactly as in Build 663.
- Treat the later `animateIconContainer(true)` callback as confirmation of an already-active lease, not a second attach.

No timer, custom animation, native alpha/visibility/translation writer, or geometry compensation is added.

### Validation

Exact-head Runtime CI and one signed Canary are required. Primary device gate: no raw native row at AOD -> Keyguard start, no early native layout jump, and no regression in the accepted Keyguard-OFF/AOD-ON path.

## 2026-10-03 — Build 665: prepare compact Keyguard layout before reveal

**Type:** device-evidence lifecycle/ownership correction  
**Branch / PR:** `feat/aod-display-control` / #196  
**Builds:** 664 -> 665

### Problem

Build 664 fixes the raw native-row exposure at AOD -> Keyguard start, but two independent defects remain:
- adjacent native Keyguard peers visibly collapse from a middle position toward Guiyuan late in the transition;
- Home -> AOD with AOD disabled briefly attaches a transient Keyguard Guiyuan and releases it only after the native AOD transition has already progressed.

### Evidence

AOD -> Keyguard:
- target Keyguard commits at 11:30:58.145;
- visual-only Guiyuan handoff is active by 11:30:58.147 with `ignoredSlotsWrites=0 paddingWrites=0`;
- the native Keyguard host is still not shown in this phase;
- only at stable Keyguard, 11:30:58.846, are persistent ignored slots and end reservation committed.

The resulting peer motion is therefore a layout-ownership cutover: represented native views are visually masked early but still occupy the native row until the stable edge.

Home -> AOD:
- the full-AOD entry records Home presentation ownership even after scene ancestry has transiently become Keyguard;
- `mToLockScreen=false` later identifies native AOD, but the transient Keyguard projection remains until a later AOD callback.

### Conclusion

For incoming Keyguard, native compact occupancy must be established **before the hidden Keyguard host is revealed**, not after the native animation begins. This is distinct from rejected Build 662, which changed native layout inside the visible animation window.

For Home -> disabled-AOD, the full-AOD start Home-ownership witness is the missing origin authority; once the native target is AOD and there is no established Keyguard/AOD family, the transient Keyguard owner has no valid lifetime.

### Change

- Latch Home presentation ownership at full-AOD entry and use it only to release a transient Keyguard owner when native target=AOD, AOD replacement is disabled, and family origin is UNKNOWN.
- For stable AOD -> enabled Keyguard, precommit ignored slots/end reservation only if the Keyguard host is still hidden at authoritative target commit.
- Keep the renderer behind native handoff until `animateIconContainer(true)`; reveal it after compact layout is ready.
- If the hidden-host precondition is not met, fall back to Build-664 visual-only/deferred-layout behavior.
- Keep Keyguard -> AOD unchanged.

No timer, delay, peer translation, alpha/visibility writer, or geometry compensation is added.

### Validation

Exact-head Runtime CI, signed Canary, then the three focused device checks recorded in CURRENT.

## 2026-10-03 — Build 666: native status-icon lifecycle gate

**Type:** Keyguard/AOD presentation lifecycle  
**Branch / PR:** `feat/aod-display-control` / #196  
**Build:** 666 / `20261003-666`

### Build-665 evidence

The AOD -> Keyguard peer-icon inward merge still reproduces. Diagnostics consistently report `hostShownAtArm=true`, so Build 665 rejects its hidden-host prelayout path. The same incoming transition reports the exact native Keyguard status-icon container at `statusIconsAlpha=0.0`.

Therefore the enclosing Keyguard host visibility is not the lifecycle authority for the animated status-icon presentation.

### Build-666 correction

- Preserve the Build-665 two-phase presentation owner and stable-AOD -> enabled-Keyguard / disabled-AOD eligibility.
- Replace `View.isShown` with the read-only `MiuiStatusIconContainer` presentation alpha.
- Allow compact occupancy precommit only when that exact native status-icon layer is attached and fully hidden (`alpha == 0f`).
- Unknown or partially visible native status icons fail back to the existing deferred path.
- Do not change Home -> AOD transient-owner handling in this focused checkpoint.

### Boundaries

No timer, delay, copied native duration/interpolator, peer translation, native alpha/visibility writer, or geometry compensation.

### Validation gate

Exact-head Runtime CI, then a signed Canary. Device focus is AOD -> Keyguard peer layout and Guiyuan continuity; Keyguard -> AOD is regression-only.

## 2026-10-03 — Build 667: separate visual ownership from stable Keyguard readiness

**Type:** Keyguard/AOD lifecycle ownership  
**Branch / PR:** `feat/aod-display-control` / #196  
**Build:** 667 / `20261003-667`

### Problem

Build 666 fixes AOD -> Keyguard peer-layout convergence, but device testing exposes two lifecycle-boundary defects:
- Keyguard -> AOD releases Guiyuan as soon as native Keyguard status icons begin fading rather than when their visual lifetime ends.
- AOD -> Keyguard can briefly fall back to native Control Center/status icons during a fast pull because the incoming Guiyuan presentation is already visually valid while stable `keyguardRuntimeReady` is still false.

### Evidence

- Outgoing Keyguard: native target resolves to AOD, then the first `isAodAnimate=true` callback deactivates Keyguard only milliseconds later, while stable AOD arrives hundreds of milliseconds afterward.
- Incoming Keyguard: `aod.visualHandoff state=revealed` occurs before stable Keyguard. During that bounded interval Control Center logs `state=native ... keyguardRuntimeReady=false`, then automatically returns to combined when stable Keyguard commits.
- Build 666 proves the exact Keyguard status-icon presentation lifecycle is the reliable visual authority; enclosing host visibility is not.

### Conclusion

Stable-family readiness and visual-presentation readiness are different lifecycle facts and must not share one boolean.

### Change

- Outgoing Keyguard remains owner while exact native Keyguard status-icons alpha is greater than zero. Native fade start is not cleanup.
- Incoming AOD -> Keyguard derives a transient presentation-ready fact from the existing handoff state: visual handoff active, compact prelayout ready, visual boundary reached, host attached, Keyguard enabled, AOD projection disabled.
- Control Center eligibility and lease acquisition may use stable readiness or this bounded transient readiness.
- AOD-blocked state may not revoke an already-valid incoming Keyguard Control Center lease.
- Fraction-zero/readiness churn does not tear down the incoming boundary owner while that transient presentation-ready fact remains true.
- Stable `keyguardRuntimeReady` is not set early and remains owned by stable Keyguard cutover.

### Lifecycle review

Reviewed before Build-667 freeze:
- one Keyguard presentation owner;
- one Control Center compact owner;
- no duplicate alpha/visibility/translation writer;
- host detach, feature disable, AOD enable, resolver loss, Hot Reload and normal fail-native paths invalidate transient readiness;
- stable AOD still forces native when AOD projection is disabled;
- Home -> AOD origin handling remains intentionally separate.

### Validation

Exact-head Runtime CI and signed Canary are required. Device gate is in CURRENT.

## 2026-10-03 — Build 668: preserve Home origin through transient Keyguard AOD entry

**Type:** Home / Keyguard / AOD lifecycle ownership  
**Branch / PR:** `feat/aod-display-control` / #196  
**Build:** 668 / `20261003-668`

### Build-667 device result

Build 667 is accepted for the previously failing Keyguard/AOD paths:
- no adjacent peer merge on AOD -> Keyguard;
- no reproduced transient native status/QS row on fast or partial pull;
- Keyguard -> AOD keeps Guiyuan until the native Keyguard status-icon visual lifetime ends.

The only remaining issue is Home/Desktop -> AOD with AOD Guiyuan disabled: native takeover is visibly late.

### Root cause

The direct screen-off path is not a single Home -> AOD target. Device evidence shows:
1. Home / `UNLOCKED_STATUS_BAR` is authoritative and Home still owns represented slots;
2. the first Full-AOD target is Keyguard;
3. before a stable Keyguard endpoint forms, native AOD state enters `toAod=true / isAodAnimate=true`.

Build 667 therefore mistakes a transient Keyguard target for a genuine stable Keyguard -> AOD lifecycle and inherits the outgoing-alpha retention rule, delaying native takeover.

### Build-668 correction

- Arm a Home-native-AOD candidate only at Full-AOD entry from authoritative HOME with Home represented-slot ownership, Keyguard enabled and AOD disabled.
- Do not consume the candidate on the intermediate Keyguard target.
- If native AOD animation begins before stable Keyguard, promote the candidate to native-AOD fallback, reset any transient incoming-Keyguard boundary handoff, and release Keyguard presentation immediately.
- Keep that fallback authoritative until stable AOD so subsequent animation callbacks cannot reattach Keyguard.
- If stable Keyguard arrives first, clear the candidate; subsequent Keyguard -> AOD therefore remains on the accepted Build-667 alpha lifecycle.
- Direct native target=AOD may consume the candidate immediately.
- Reverse target, missing resolver, settings changes, stable endpoints, Hot Reload and teardown fail closed.

### AOD -> Keyguard fast-pull risk audit

A residual race was found even though Build 667 device testing passed: native expansion fraction may arrive before Control Center visible/source reconciliation. If the cached source is still HOME, lease acquisition can miss the already-valid incoming Keyguard presentation.

Build 668 uses the existing incoming-boundary-ready fact only:
- fraction > 0 promotes the CC source to KEYGUARD before lease acquisition when incoming Keyguard presentation is already valid;
- HOME/KEYGUARD source disagreement resolves to KEYGUARD only while incoming-boundary-ready is true and at least one native witness explicitly reports KEYGUARD;
- normal unlock is unaffected because that readiness fact is absent.

### Boundaries

No timer/delay, copied duration/interpolator, native alpha/visibility/translation writer, peer-motion writer, geometry compensation, or second presentation owner.

## 2026-10-03 — Build 669: consume Home-origin fallback at native AOD animation start

**Type:** Home / transient Keyguard / native AOD lifecycle ownership  
**Branch / PR:** `feat/aod-display-control` / #196  
**Build:** 669 / `20261003-669`

### Build-668 device rejection

With Keyguard Guiyuan enabled and AOD Guiyuan disabled, Home/Desktop -> AOD still frequently shows:
1. Guiyuan disappears briefly;
2. Guiyuan appears again;
3. only afterward does native AOD take over.

Detailed Build-668 diagnostics show the direct screen-off path:
- authoritative Home immediately before transition;
- Full-AOD first reports `target=keyguard`;
- before stable Keyguard forms, native state changes to `toAod=true / isAodAnimate=true`;
- tested Build 668 does not latch `homeNativeAodFallbackCandidate`, so the transient Keyguard presentation remains eligible through this native AOD transition start.

### Root cause

The native Full-AOD Keyguard target is a routing stage, not proof of a genuine stable Keyguard lifecycle. Home-origin must survive that transient target and be consumed when native AOD animation actually begins.

### Build-669 correction

- Arm a one-shot Home-native-AOD candidate only at Full-AOD entry from authoritative HOME while Home still owns represented slots, Keyguard is enabled and AOD projection is disabled.
- Do not clear that candidate on the intermediate native Keyguard target.
- Consume the candidate on native `toAod=true / isAodAnimate=true`, release transient Keyguard presentation, reset incoming-Keyguard boundary state, and mark native-AOD fallback active.
- While fallback is active, Keyguard projection resolves NATIVE so later callbacks cannot reattach Guiyuan.
- Clear candidate/active fallback on stable AOD, stable Keyguard, authoritative HOME after an aborted transition, reverse/failed resolution, settings changes, Hot Reload and teardown.
- Direct native target=AOD can consume the same candidate without waiting for the AOD-state callback.

### AOD -> Keyguard Control Center race audit

Build 667 fixed the reproduced native-QS fallback, but callback-order review found a remaining theoretical race:
- expansion fraction can arrive before visible/source reconciliation while cached CC source is still HOME.

Build 669 keeps the same single incoming-Keyguard presentation owner and:
- promotes CC source to KEYGUARD on fraction > 0 only when the existing incoming-boundary-ready fact is already true;
- resolves HOME/KEYGUARD disagreement to KEYGUARD only under that same readiness and only when at least one source witness explicitly reports KEYGUARD.

Normal unlock remains unaffected because incoming-boundary readiness is absent.

### Lifecycle review

Reviewed before freeze:
- Home candidate creation has one authority: Full-AOD entry + authoritative HOME + Home presentation ownership;
- intermediate Keyguard target does not consume the candidate;
- native AOD animation start is the primary consumption boundary;
- stable Keyguard invalidates Home-origin fallback;
- stable AOD completes the native fallback lifetime;
- authoritative HOME clears an aborted active fallback;
- no second presentation owner, no timer, no custom motion/alpha/visibility/translation writer.

### Validation

Exact-head Runtime CI and one signed Canary are required. Device gate is recorded in CURRENT.

## 2026-10-03 — Build 669 correction: Home provenance follows native `system_icons` lifetime

**Type:** lifecycle authority correction after Build-668 device evidence  
**Branch / PR:** `feat/aod-display-control` / #196  
**Build:** 669 / `20261003-669`

### Build-668 evidence

Detailed Build-668 diagnostics show the failing Home -> AOD attempt immediately before `animateFullAod:before` still emits raw `UNLOCKED_STATUS_BAR` updates and still has Home represented-slot ownership, yet `homeNativeAodFallbackCandidate=false`. The candidate therefore never enters the state machine; transient Keyguard remains eligible and Guiyuan reappears before stable native AOD.

The failure is not a missing consumer. It is an authority mismatch: candidate arming used cached `steadyStatusSourceScene`, whose structural ancestry requirement can remain non-Home while the actual Home end-side presentation is still visibly owned. Reusing raw Battery `mStatusBarState` as a second Home-visibility authority is explicitly forbidden by the existing architecture.

### Correction

- Expose a read-only Home presentation fact from the existing Home owner: the exact native `MiuiStatusBatteryContainer(system_icons)` carrier must be attached, visible, alpha > 0, and shown.
- Candidate arming now requires both Home compact ownership and that exact native carrier presentation to be visible.
- The candidate no longer depends on cached `steadyStatusSourceScene`.
- Returning Home does not clear an inert candidate during the transient routing window; stable Keyguard, stable AOD, feature/settings teardown, resolver failure and Hot Reload still clear it.
- No new Hook or visibility writer is added. Guiyuan only reads the carrier whose native alpha/visibility/translation it already inherits.

### AOD -> Keyguard Control Center review

The Build-667 incoming-boundary readiness fix remains. Build 669 retains the additional source-order guards: fraction-first acquisition and HOME/KEYGUARD witness disagreement prefer KEYGUARD only while the verified incoming Keyguard presentation-ready fact is true. No ordinary unlock path can use that guard.

### Validation

Exact-head Runtime CI and one signed Canary are required before device testing.

## 2026-10-03 — Build 672: block Keyguard visual rearm during native AOD fallback

**Type:** Home -> AOD lifecycle handoff correction  
**Branch / PR:** `feat/aod-display-control` / #196  
**Build:** 672 / `20261003-672`

### Problem

Build 669 still reproduces the Home/Desktop -> AOD sequence “Guiyuan disappears -> Guiyuan returns -> native” when Keyguard Guiyuan is enabled and AOD Guiyuan is disabled.

### Evidence

- Build 669 successfully arms Home provenance: `homeCarrierVisibleAtStart=true`, `homePresentationOwnedAtStart=true`, `homeNativeAodFallbackCandidate=true`.
- Native `toAod=true / isAodAnimate=true` consumes the candidate, releases transient Keyguard presentation, and records `homeNativeAodFallbackActive=true`.
- While that fallback is still active, HyperOS later emits a Keyguard-directed status-icon visual boundary; `aod.visualHandoff state=armed` and `renderer.attach` occur again.
- The main scene resolver already returns NATIVE while the fallback is active. The reappearance therefore bypasses the projection resolver through the dedicated incoming-Keyguard visual-handoff path.

### Conclusion

The remaining Build-669 defect is not Home-origin detection and not fallback consumption. `armKeyguardBoundaryVisualHandoffIfEligible()` had an eligibility contract that did not include the already-authoritative Home-native-AOD fallback, allowing one side-channel renderer reacquire.

### Change

- Add `homeNativeAodFallbackActive` to incoming Keyguard visual-handoff eligibility and reject the handoff while active.
- Apply the same veto to Keyguard boundary layout precommit.
- Keep Build-669 Home carrier provenance, fallback consumption, stable-family cleanup and native animation ownership unchanged.
- Add focused unit coverage for both visual-handoff and precommit rejection under an active fallback.

### Boundaries

No timer, delay, copied native duration/interpolator, geometry compensation, native alpha/visibility/translation write, extra Hook, second presentation owner, or new lifecycle authority.

### Validation

Exact-head Runtime CI, then one signed Canary. Device focus remains Home/Desktop -> AOD with Keyguard ON / AOD OFF, plus Build-667 Keyguard/AOD regressions.



## 2026-10-03 — Build 624 accepted ring baseline; Build 627 charging-glyph target handoff

**Type:** device evidence / charging transition / target geometry  
**Branch / PR:** `feat/battery-fill-retract-follow` / #197  
**Builds:** 624 -> 627

### Device baseline

Maintainer device testing accepts Build 624 battery-ring/fill retract behavior. Further charging-glyph work must preserve that ring path rather than retune its geometry or easing.

### Requested visual contract

The charging glyph should look as if the retracting ring reaches and removes it:
- stay fully visible while the retained ring is above 60%;
- fade smoothly as retained ring falls from 60% to 50%;
- be fully invisible at 50%;
- do not move before it is fully invisible;
- after that, travel invisibly toward the corresponding native charging target;
- only near the target should it fade back in while converging to the target;
- target size must follow native optical geometry, not a fixed Guiyuan size;
- if no reliable target exists, stop after the source-side fade.

The percentage number must not jump when the glyph disappears.

### Implementation

- Split `CHARGING_ICON` from `BATTERY_NUMBER` as an independent transition participant.
- Preserve the readout layout/group geometry while only changing charging-glyph draw alpha, so percentage X placement is not recomputed at the 50% threshold.
- Derive source fade from `CombinedStatusBatteryRingTransitionPolicy.transitionProgress()` plus the same `remainingFraction()` used by the ring.
- Begin charging-glyph geometry progress only after retained ring reaches 50%; hidden travel maps the remaining 50% -> 0% ring interval to a smooth target-progress curve.
- Reappearance starts only in the final 20% of charging-glyph target travel.
- Resolve only exact-target `MiuiBatteryMeterView.mBatteryChargingView` when it is an attached, laid-out `ImageView` with a valid drawable.
- Use existing drawable optical-geometry sampling and `TransitionScalePolicy.TARGET`, matching the Wi-Fi principle of translation plus target-derived uniform scaling.
- If the charging target is missing or unreliable, target resolution returns null and the glyph remains hidden after the source fade; no synthetic/fallback coordinate is generated.

No timer, delayed runnable, additional animation clock, native target mutation, or guessed geometry constant is added.

### Review / tests

- Unit coverage verifies the glyph is hidden before target motion becomes visible, cannot reappear without a target, and reaches full target opacity/motion at completion.
- Ownership remains single-writer: Guiyuan only draws its transition participant; native charging target is read-only geometry evidence.
- Build 624 ring/fill implementation is otherwise unchanged.


## 2026-10-03 — Build 627 device findings; Build 629 terminal ring and late charging handoff

**Type:** device evidence / transition optics / target handoff timing  
**Branch / PR:** `feat/battery-fill-retract-follow` / #197  
**Builds:** 627 -> 629

### Device evidence

Build 627 preserves the accepted Build-624 main retract behavior but exposes two visual problems:
- near the end, a tiny remaining ring segment appears to retract disproportionately slowly;
- the charging glyph starts fading/leaving too early and too softly relative to the ring.

### Root cause

The ring uses ROUND stroke caps. Once the remaining centerline arc becomes comparable to the stroke width, reducing sweep no longer produces a proportionally smaller visual mark: the two round caps dominate and the remainder looks like a nearly fixed dot until mathematical progress finally reaches zero.

The charging-glyph source fade in Build 627 was tied to 60% -> 50% retained ring, so it began well before the ring visually approached the glyph. Its hidden target travel also consumed the entire remaining 50% -> 0% interval, making the exit feel prolonged.

### Build 629 correction

- Do not retune `transitionProgress()`, `FRONT_LOAD`, fill-follow semantics or the accepted Build-624 main retract curve.
- Compute the retained transition arc length from current drawable sweep and `CombinedStatusOuterGeometry.RING_RADIUS`.
- Stop drawing only when that retained arc length is less than or equal to the resolved ring stroke width. This removes the cap-dominated terminal artifact and naturally adapts to outer-weight scaling and top-gap sweep.
- Move charging fade to retained ring 26% -> 20%.
- Begin hidden target motion only after full source disappearance; map retained ring 20% -> 4% to the complete hidden travel.
- Start target reveal only in the final portion of that target motion.
- Keep exact native charging-target optical geometry, target scaling, no-target fail-native behavior and percentage-layout isolation unchanged.

No timer, extra animator, guessed pixel offset, native target mutation or second progress clock is introduced.

### Review / validation

- Geometry test distinguishes a 5% retained default arc (still drawable) from a 3% retained arc (cap-dominated terminal state).
- Charging handoff tests verify later source visibility, full disappearance before target travel, short hidden travel and no-target no-reveal behavior.
- Runtime CI passes the source correction before final Build identity/docs closure.
- Signed Canary/device evidence remains mandatory before integration.


### Source-position lock clarification

Maintainer clarification after the first Build-629 timing pass: the charging glyph must not move at all while any source-side alpha remains visible.

Review found that `chargingMotionProgress()` was numerically zero during fade, but the shared geometry path still rebased the source component to the current carrier before interpolation. That could produce visible movement even with zero target progress.

Build 629 therefore makes the phase boundary explicit:
- `chargingSourceOpacity(progress) > 0` hard-forces `chargingMotionProgress(progress) == 0`;
- while that source opacity remains non-zero, Control Center rendering uses the frozen `sourceGeometry` directly;
- carrier rebase and target interpolation are both bypassed during source fade;
- only after source opacity reaches exactly zero may hidden target travel begin;
- target-side reappearance near the destination remains unchanged.

A dense unit sample across the fade interval protects the no-motion invariant independently of the exact fade constants.


## 2026-10-03 — Build 629 device rejection; Build 631 number-relative charging handoff

**Type:** device evidence / relative geometry / charging alpha timing  
**Branch / PR:** `feat/battery-fill-retract-follow` / #197  
**Builds:** 629 -> 631

### Device evidence

Build 629 confirms the terminal ring cleanup direction but rejects the charging-glyph handoff:
- the source glyph is held in root/screen coordinates while the percentage number continues its transition, so their relative spacing changes;
- the glyph remains visible after retained ring progress has crossed 50%;
- target-side glyph becomes visible too soon after ring completion.

The intended source-side invariant is relative, not absolute: while visible, the glyph belongs visually to the percentage readout group and must follow the number's transform. Only a fully invisible glyph may separate and travel toward its independent native target.

### Build 631 correction

- Restore source fade to the ring-defined 60% -> 50% retained interval.
- Numerically derive the corresponding handoff-progress start/end from the existing ring policy once, so 50% retained ring is the exact source-alpha-zero boundary.
- Reuse that derived progress span as the late target fade-in duration.
- Start target reveal at overall handoff progress 92%; use the same smoothstep and equal progress duration as source fade.
- Keep target alpha at zero throughout hidden travel and after target arrival until the late reveal window.
- Replace Build-629 root-coordinate freeze with a battery-number follower transform:
  - compute the current battery-number geometry from its normal transition path;
  - express charging-glyph center/basis in the source number's local basis;
  - map that local geometry through the current number basis;
  - preserve relative offset, scale and orientation while the source glyph is visible/fading.
- Keep independent charging-target motion hard-gated until source alpha reaches zero.
- Keep exact `mBatteryChargingView` target scaling/position and no-target fail-native behavior.

No additional animator, wall-clock timer, guessed coordinate or native target writer is introduced.

### Review / tests

- Follower-geometry unit coverage verifies translation/scale propagation relative to the number.
- Fade-window tests verify 60%/50% ring thresholds, late 92% reveal and equal source/target fade durations.
- Dense source-opacity sampling still guarantees charging-target motion remains zero while any source alpha is present.
- Build 624 ring progress/easing remains untouched.


## 2026-10-03 — Build 631 device timing refinement; Build 632 slightly earlier target reveal

**Type:** device visual timing / charging target reveal  
**Branch / PR:** `feat/battery-fill-retract-follow` / #197  
**Builds:** 631 -> 632

### Device feedback

Build 631's source-side behavior is retained. The only requested refinement is that the final native charging-glyph reveal may begin slightly earlier.

### Build 632 correction

- Move charging target reveal start from overall handoff progress 92% to 88%.
- Preserve the source fade boundary at retained ring 60% -> 50%.
- Preserve number-relative follower geometry while the source glyph remains visible/fading.
- Preserve hidden target travel and exact native `mBatteryChargingView` target geometry.
- Preserve equal fade-in/fade-out progress duration and the same smoothstep easing.
- No ring-curve, percentage-layout, target geometry, native writer, timer or additional animation clock change.

### Validation

- Unit coverage locks the 88% reveal start and equal fade-window duration.
- Exact Build-632 Runtime CI and signed Canary remain required before device validation.


## 2026-10-03 — Build 633 selective pull-down tint transition

**Type:** Control Center visual color handoff / settings  
**Branch / PR:** `feat/battery-fill-retract-follow` / #197  
**Build:** 633

### Requested behavior

Maintainer clarified that the Control Center pull-down should not recolor every projected participant. Only participants currently receiving a semantic/preset/custom battery-linked color should transition back to the system reverse tint. Already-native/system-tinted participants should remain on the native tint path.

The color change should also be concentrated in the middle of the handoff: unchanged at the beginning, fast smooth transition in the middle, unchanged at the end.

A default-enabled switch is required. Disabling it means colorized participants remain in their original source color during pull-down rather than transitioning to reverse tint.

### Implementation

- Added global visual setting `controlCenterTintTransitionEnabled`, persisted under `control_center_tint_transition_enabled`, default `true`, synchronized through the existing visual-settings owner.
- Added MIUIX `SwitchPreference` in the Global section:
  - EN: **Pull-down tint transition**
  - zh-CN: **下拉反色过渡**
- Added `CombinedStatusBatteryColorPolicy.isTinted(...)` so transition participation is based on the active semantic color source being an actual `Custom` source after preset/custom resolution.
- Battery ring participates whenever the active semantic battery source is tinted.
- Center/mobile/top-number/charging-glyph participate only when their existing follow-battery-color setting is enabled in addition to the active battery source being tinted.
- Non-tinted participants resolve directly to the live final native peer tint.
- Colorized participants with the switch disabled keep their source color.
- With the switch enabled, `transitionTintProgress()` is:
  - 0 through handoff 35%;
  - smoothstep 0 -> 1 over 35% -> 65%;
  - 1 from 65% onward.
- ARGB channels are interpolated independently.
- Native target tint is read from the existing final `statusIcons` peer authority; reads refresh each pre-draw while retaining the last valid value.

No new animator, timer, color guess, native tint writer, geometry writer, or second settings owner is introduced.

### Charging timing retained

Build 632's 88% final charging-glyph reveal start remains. The 60% -> 50% source fade, number-relative follower geometry, hidden target travel, equal fade durations, exact target geometry and fail-native behavior are unchanged.

### Review / tests

- Tests distinguish preset/custom tinted states from FOLLOW_SYSTEM.
- Tests lock source/target/midpoint ARGB interpolation.
- Tests lock the 35%/65% middle-only phase.
- Tests lock switch OFF -> source color for tinted participants and non-tinted -> native target tint.
- Visual-settings tests lock default ON and runtime-sync key coverage.


## 2026-10-03 — Build 633 device rejection; Build 635 opaque clip transitions and tint fallback

**Type:** device visual feedback / transition semantics / tint authority  
**Branch / PR:** `feat/battery-fill-retract-follow` / #197  
**Builds:** 633 -> 635

### Device feedback

Build 633 exposed two visual issues:
- charging-glyph target reappearance still reads as a fade/flash even after moving reveal earlier;
- selective pull-down reverse-tint transition is effectively absent: native status peers reach reverse tint while Guiyuan's colorized ring/readout remain at their semantic color.

### Root cause and design correction

Semantic visibility should not be represented by alpha when the rest of the transition keeps icons physically opaque. The existing unmatched-exit and latent-reveal policies already supply timing/progress; they can drive visible clip fraction instead of opacity without changing motion or space-reservation rules.

For tint, final status-icons peer tint is not guaranteed to be available from the suppression-owner cache on every active transition path. The final native Battery participates in the same SystemUI tint authority and provides a live read-only fallback.

### Build 635 implementation

- Replace charging source/target opacity semantics with visible-fraction semantics.
- Keep the charging source fully opaque and clip it over retained ring 60% -> 50%.
- Keep hidden-only target travel.
- Begin target clip reveal at 85% while preserving the old Build-633 reveal completion time.
- Replace generic no-target cubic alpha exit with cubic clip-out.
- Replace latent second-mobile / airplane / no-SIM alpha reveal with clip reveal while preserving existing spatial and target-distance gates.
- Add reusable horizontal clip bounds with LTR/RTL-aware edge anchoring; charging chooses the edge facing the battery-ring side.
- Resolve native reverse tint from live final status-icons peer first, live final Battery tint second, and last valid cached tint third.
- Log native tint value and authority for detailed transition diagnostics.

No new animator, timer, independent geometry path, scale animation, guessed tint, or native writer is introduced.

### Review / tests

Coverage locks clip fractions and edge anchoring, charging reveal timing, latent reveal policy, and final-Battery tint fallback. The pre-identity runtime source passed Runtime CI #2355; final exact-HEAD CI remains required after docs/build closure.


## 2026-10-03 — Build 635 trace review; Build 637 tint-decision diagnostics

**Type:** device evidence / diagnostic instrumentation  
**Branch / PR:** `feat/battery-fill-retract-follow` / #197  
**Builds:** 635 -> 637

### Build-635 trace result

The supplied detailed runtime log repeatedly reports `nativeTint=e6ffffff` with `nativeTintAuthority=final-battery-tint` across the pull-down. This confirms the final-Battery fallback introduced in Build 635 is active and eliminates the Build-633 failure mode where no reliable native target tint was available.

No fatal/exception signature is present in the supplied log.

### Remaining observability gap

Build 635 did not log:
- semantic colorized-state classification;
- pull-down tint-transition switch value at draw time;
- source versus resolved participant colors.

Therefore the trace alone cannot distinguish a participation-classification issue from an actual rendering issue if the user still sees no visual color transition.

### Build 637 diagnostics

Add read-only `tintTransition` diagnostics containing:
- `batteryTinted`;
- switch enabled state;
- motion progress;
- target native tint;
- source -> resolved battery, number, charging, center and mobile tint;
- normalized tint-phase progress.

No visual behavior or ownership semantics are changed.


## 2026-10-03 — Build 644: QS_FAKE tint authority, exact supplemental icon size, half-ring charging Clip

**Type:** device feedback / Control Center visual root cause  
**Branch / PR:** `feat/battery-fill-retract-follow` / #197  
**Builds:** 638 -> 644

### Build-638 feedback

- Airplane reveal remains visibly smaller than the fully-expanded native Airplane icon, causing a final size discontinuity.
- Projected icons that are not battery-colorized still appear white instead of matching nearby native icons during pull-down.
- Charging source glyph disappears too late.

### Root causes

- `FOLLOW_SYSTEM` classification was already correct: only resolved `Custom` semantic color sources are considered colorized.
- Tint authority was wrong. Build 638 sampled `finalStatusIcons`, the fully-expanded QS destination, rather than `QS_FAKE / fakeStatusIcons`, the native transition carrier visible beside Guiyuan during the gesture. The final destination can legitimately already be white.
- The temporary review attempt to use `NativeNetworkSuppressionOwner.activeManager` for an arbitrary final group was rejected because that manager belongs to the Home status-bar host, not the independent QS/QS_FAKE icon group.
- Supplemental Airplane / No-SIM already resolve a native single-icon optical target, but Build 638 projected them with `SHRINK_ONLY`; therefore a larger native target could never be reached.
- Charging Clip at retained ring 26% -> 20% starts too late.

### Build-644 correction

- Transition tint samples already-applied tint from visible non-represented native peers in `QS_FAKE / fakeStatusIcons`.
- No Home-manager tint reconstruction, no final-QS white target assumption, and no Battery tint fallback are used.
- FOLLOW_SYSTEM participants directly use the live QS_FAKE native peer tint.
- Only custom battery-colorized participants use the existing optional 35%-65% source -> native interpolation.
- Supplemental Airplane / No-SIM use `TARGET` scale against their resolved native drawable optical geometry, matching the accepted Wi-Fi target-size principle.
- Charging source Clip begins with ring retract and completes at retained ring 50%; source-visible charging remains number-relative, hidden travel and late native-target reveal are preserved.
- Latent additional-mobile target Clip-envelope correction from Build 638 remains.

No new animator, timer, guessed tint, per-icon size multiplier, or native writer is introduced.


## 2026-10-03 — Build 646: accelerate charging target reveal and delay custom tint

**Type:** device feedback / timing polish  
**Branch / PR:** `feat/battery-fill-retract-follow` / #197  
**Builds:** 644 -> 646

### Build-644 feedback

- Charging target glyph still takes too long from first reappearance to fully visible.
- Custom-color pull-down tint begins too early and completes too quickly.

### Root cause

- Charging target reveal was a fixed global `0.85 -> 0.98` smooth window. This is not the same cadence as latent resources, whose local reveal completes after only the first 35% of their unlocked reveal progress.
- Custom-color interpolation still used the earlier `0.35 -> 0.65` phase.

### Build-646 correction

- Charging target reveal still begins at global progress 0.85, but completes after 35% of the previous 0.85 -> 0.98 local reveal span, matching the accelerated latent-resource cadence.
- Source charging Clip remains unchanged: ring retract start = Clip start; retained ring 50% = source fully hidden.
- Hidden travel, target geometry, and fail-native behavior remain unchanged.
- Custom-color interpolation moves to `0.45 -> 0.80`: later start and longer transition.
- `FOLLOW_SYSTEM` participants remain outside this custom interpolation and continue to use live QS_FAKE applied tint directly.

No new animator, timer, target geometry change, native writer, or additional transition clock is introduced.


## 2026-10-03 — Build 647: custom tint follows battery-ring retract lifetime

**Type:** device feedback / transition timing refinement  
**Branch / PR:** `feat/battery-fill-retract-follow` / #197  
**Builds:** 644 -> 647

### Build-644 follow-up

The maintainer requested that custom-color fade be visually tied to battery-ring retract rather than an independent fixed progress window.

### Build-647 correction

- Remove the independent custom tint window.
- Custom-color fade starts when battery-ring retract starts and reaches native QS_FAKE tint exactly when ring retract completes.
- The shared lifetime comes from `CombinedStatusBatteryRingTransitionPolicy.transitionProgress()`.
- Color itself keeps a smoothstep over that shared lifetime, rather than copying the ring's front-loaded shrink curve, so the color transition remains visually gentler.
- `FOLLOW_SYSTEM` remains outside this interpolation and continues to use live QS_FAKE applied tint directly.
- Build-646 accelerated charging target reveal is retained: reveal still starts at 0.85 and completes in the first 35% of the former late reveal span.
- Charging source Clip remains unchanged: ring retract start -> retained ring 50%.

No new timer, animator, transition clock, tint writer, or geometry change.


## 2026-10-03 — Build 648: explicit 0.85-0.90 charging target reveal

**Type:** device feedback / transition timing finalization  
**Branch / PR:** `feat/battery-fill-retract-follow` / #197  
**Builds:** 647 -> 648

### Decision

The charging target glyph now uses an explicit late reveal window:
- start visible reveal at global progress `0.85`;
- complete visibility at `0.90`.

This replaces the Build-647 provisional "35% of the previous late reveal span" derivation.

### Rationale

- The source charging glyph is already fully clipped earlier and completes its hidden travel before the target reveal.
- A fixed 0.85 -> 0.90 reveal is fast enough to avoid the Build-644 slow appearance, but not so fast that the glyph pops in abruptly.
- Keeping target reveal independent from source hide duration prevents future changes to the ring/charging Clip rule from accidentally changing the target appearance cadence.

### Retained behavior

- Source charging Clip still begins when ring retract starts and completes when retained ring reaches 50%.
- Hidden travel remains invisible.
- Target reveal has no independent translation or scale.
- Custom-color tint fade remains bound to the battery-ring retract lifetime.
- FOLLOW_SYSTEM still uses live QS_FAKE applied tint directly.


## 2026-10-03 — Build 649: correct obsolete tint test after ring-sync change

**Type:** CI review / test correction  
**Branch / PR:** `feat/battery-fill-retract-follow` / #197  
**Builds:** 648 -> 649

Build 648 Runtime reached the unit-test phase and failed one stale assertion in `transitionTintHoldsEndsAndChangesOnlyInMiddlePhase`. The test still required source color at global progress 0.20, which contradicts the approved Build-647/648 rule that custom tint fade begins as soon as battery-ring retract begins.

No runtime behavior was changed for this correction:
- source tint is exact at progress 0;
- fade starts immediately with ring retract;
- midpoint remains tied to half of the ring-retract lifetime;
- native tint is exact when ring retract completes;
- charging target reveal remains fixed at 0.85 -> 0.90.


## 2026-10-03 — Build 650: charging Clip follows half of ring-retract lifetime

**Type:** device evidence / timing correction  
**Branch / PR:** `feat/battery-fill-retract-follow` / #197  
**Builds:** 649 -> 650

Build 649 device video showed the charging source glyph fully disappearing before the battery-ring retract animation itself reached halfway. The previous rule used 50% **remaining visible arc**, but the ring's retained arc is front-loaded and therefore reaches 50% well before half of the retract lifetime.

Build 650 changes the authority:
- source Clip begins at ring-retract lifetime 0%;
- source is exactly 50% visible at retract lifetime 25%;
- source is fully clipped at retract lifetime 50%;
- while any source remains, charging stays locked to the battery-number follower;
- only after complete source Clip does hidden travel begin;
- target reveal remains 0.85 -> 0.90;
- source disappearance remains opaque horizontal Clip with `opacity=1`, not alpha fade or scale.

No new animator, timer, geometry writer, or second transition clock is introduced.


## 2026-10-03 — Build 652: calibrate charging Clip between two device-proven bad endpoints

**Type:** device video / visual timing calibration  
**Branch / PR:** `feat/battery-fill-retract-follow` / #197  
**Builds:** 650 -> 652

### Device evidence

Build 649 and Build 650 bracket the desired charging-source disappearance point:
- Build 649: source fully clipped when retained ring arc reached 50%; device video shows this is too early.
- Build 650: source fully clipped at 50% of raw ring-retract lifetime; because the ring uses `FRONT_LOAD=0.92`, only ~18% of ring arc remains at that point, and device video shows the source disappears near the end.

### Build-652 correction

- Keep Clip start at ring-retract start.
- Complete source Clip at 40% of ring-retract lifetime.
- Under the current ring curve this corresponds to ~32% retained arc, visually between the two rejected endpoints.
- Source Clip remains linear and opaque; source remains number-relative while visible.
- Hidden target travel begins only after source is fully clipped.
- Target reveal remains 0.85 -> 0.90.
- Ring geometry/easing and tint timing are unchanged.

No timer, animator, secondary transition clock, geometry writer, alpha fade, or scale animation is added.


## 2026-10-04 — Build 679: charging-island native peers follow current projected occupancy

**Type:** device-feedback geometry ownership correction  
**Display version:** 0.0.5  
**Build:** 679 / `20261003-679`  
**Branch / PR:** `fix/qs-fake-native-source-sync` / #200

### Problem

Build 678 reduced the charging-island native peer reservation endpoint by deriving it from fake/final status-row capacity, but device video still shows a large empty gap between native peers and Guiyuan while the combined status unfolds.

### Evidence

The Build-678 device recording and Detailed Diagnostic align at the early charging-island pull:
- compact Guiyuan slot width is 105px;
- around 25% native expansion, the visible Guiyuan left edge is still effectively inside that compact envelope;
- native QS_FAKE reservation has already grown to about 134px;
- VPN/mute therefore move left before Guiyuan has occupied the released space.

This rejects final-row capacity as the correct spacing authority. The defect is not the Build-677 capacity saturation: the visible gap appears well before saturation.

### Conclusion

Two reservations have different responsibilities:
- **logical semantic reservation** must keep the accepted frozen-final-total-width interpolation so latent participants receive deterministic layout capacity and do not reintroduce the previously rejected global dead-zone behavior;
- **charging-island native-peer proximity** must describe the width occupied by the current projected spans, because the native fake row remains visibly authoritative while Guiyuan pixels are still unfolding.

The previously rejected per-span trajectory remains rejected as the global logical occupancy contract. New exact Battery-island evidence narrows current-span union to the native peer-spacing adapter only.

### Change

- Keep `resolveTransitionReservationWidth()` unchanged for Guiyuan logical occupancy and latent reveal.
- Replace Build-678 fake/final capacity-difference proxy with `resolveBatteryIslandNativePeerReservationWidth()`.
- The charging-island native peer width uses `resolveReservationWidth()` over the already-frozen source/target spans at the same raw HyperOS progress and is bounded by the semantic reservation.
- Ordinary-island reservation, steady Home peer mirror, Build-677 capacity saturation, native root motion, and fake/final appearance ownership are unchanged.
- Remove the Build-678 dependence on final status-row usable-width measurements for native peer spacing.

### Ownership / lifecycle

No new Hook, timer, poller, animator, island geometry authority, native child state writer, alpha/visibility/translation writer, or fixed pixel spacing is added. The existing Session owns the same single `statusIcons.paddingEnd` writer and clears it through the existing transition teardown/fail-native paths.

### Validation

Focused unit coverage verifies that a latent target can advance semantic reservation while charging-island native peer reservation remains at the compact width until the current projected span actually extends beyond it, then converges to the target occupancy. Exact-head Runtime CI and one signed Canary are required before the focused device gate in CURRENT.


## 2026-10-04 — Build 680: project Battery-island peer reservation into the live QS_FAKE end frame

**Type:** device feedback / coordinate-frame correction / lifecycle review  
**Display version:** 0.0.5  
**Build:** 680 / `20261003-680`  
**Branch / PR:** `fix/qs-fake-native-source-sync` / #200

### Build-679 device result

The charging-island peer distance remains visibly too large. The Detailed Diagnostic still shows Battery-island native reservation growing while the visible fake row and Guiyuan projection are separating under native Control Center motion.

### Root cause

Build 679 corrected *which width* was measured but still mixed coordinate frames:
- reservation target spans are frozen relative to the final QS Battery end;
- native peers consume `QS_FAKE statusIcons.paddingEnd` relative to the live fake-row end;
- the renderer rebases source geometry through the moving fake carrier before interpolating to the absolute final target.

Therefore a target span cannot be used directly as a fake-row-local span while HyperOS applies charging-island fake-root translation.

### Build-680 correction

- Measure the current logical end offset between `finalBattery` and `fakeStatusIcons` on each reservation sync.
- Project frozen target span X coordinates by that live offset before evaluating Battery-island current occupancy.
- Keep semantic reservation, latent reveal, ordinary-island/no-island paths and physical capacity saturation unchanged.
- Add `nativePeerTargetEndOffset` to detailed transition diagnostics.
- Add a focused regression test proving that a final target to the right of the current fake end reduces fake-local reservation instead of opening a false gap.

### Submission and lifecycle review

Before commit:
- one existing transition-reservation call site remains;
- no native translation writer, timer, animator or Handler is added;
- Session stop still clears transition reservation, removes overlay and restores source clip;
- endpoint replacement and detached-view paths stop the old Session;
- panel runtime failure still detaches to native;
- Hot Reload detaches the transition owner before presentation/capacity-lease release;
- island authority is refreshed per native expansion sample and reverse pull uses the same live sampling path.

## 2026-10-04 — Build 681: fail native when Battery-island peer frame is unavailable

**Type:** pre-device code review / lifecycle hardening  
**Display version:** 0.0.5  
**Build:** 681 / `20261003-681`  
**Branch / PR:** `fix/qs-fake-native-source-sync` / #200

### Review finding

Build 680 correctly projects final-QS target spans into the live QS_FAKE end frame, but submission review found one lifecycle gap before device testing. When that live end-frame sample was unavailable, `syncTransitionReservation()` returned immediately and could leave the prior frame's `statusIcons.paddingEnd` reservation applied.

### Correction

- Route Battery-island end-frame loss through the existing Control Center presentation fail-native chain.
- The existing presentation session restores native ignored slots, clip masks and reservation, then its readiness callback hides the projected renderer and stops the Transition Session.
- If the presentation session is already absent, detach the Transition Session directly.
- Do not add a Hook, Handler, timer, retry loop, translation writer or second lifecycle owner.

### Lifecycle / ownership review

- `Session.stop()` still clears transition reservation, removes the pre-draw listener/overlay and restores the source clip.
- Host replacement, view detach, panel runtime failure and Hot Reload retain their existing cleanup paths.
- The fail-native callback can synchronously stop the Transition Session; the failing reservation sync returns immediately and performs no further writes.
- A failed presentation remains native for the current session; ordinary state/tint refresh does not silently reacquire native presentation ownership.
- Reverse pull uses the same native progress/end-frame path.

### Validation

Focused unit coverage locks the idle failure entry as a no-op when no presentation session exists. Exact-head Runtime CI is required before the signed Canary/device gate.



## 2026-10-04 — Build 682: reserve only peer-side Battery-island intrusion

**Type:** device root-cause correction / cross-device geometry semantics  
**Display version:** 0.0.5  
**Build:** 682 / `20261003-682`  
**Branch / PR:** `fix/qs-fake-native-source-sync` / #200

### Build-681 device result

The charging-island native-peer position remains wrong even though end-frame loss is now fail-native safe.

Detailed Diagnostic proves the Build-680 live frame offset is not stale: its constancy in this gesture follows the native HyperOS relationship between the fake and final Control Center rows. The remaining spacing error tracks `statusIcons.paddingEnd` growth itself.

### Root cause

`resolveBatteryIslandNativePeerReservationWidth()` used the complete projected span union:

`right - left`.

After final-QS target spans are projected into the current QS_FAKE end frame, a valid portion of that union can live at logical `x > 0`, to the end side of the fake-row boundary. That portion belongs to Guiyuan drawable/target motion, but native status peers occupy only the opposite side of the boundary.

Counting end-side extent as `paddingEnd` double-reserves empty horizontal space and pushes native peers too far left.

### Correction

- Keep the live final-QS -> QS_FAKE end-frame projection.
- Define current QS_FAKE end as logical `x=0`.
- Compute native peer reservation from `0` to the left-most projected Guiyuan span only.
- Keep the runtime compact slot as the minimum and semantic reservation as the maximum.
- Ignore projected extent at `x>0` for peer spacing.
- Add a focused unit test where a target crosses the live fake end and verify only the peer-side 80px depth is reserved from arbitrary runtime geometry values.

### Cross-device requirement

No observed Xiaomi 15 Pro values are encoded. In particular, the device-observed 105/135/249px values remain diagnostic evidence only. Battery width, inter-frame offset, target span positions and compact width all come from live/frozen View geometry.

### Submission / lifecycle review

- one `updateControlCenterTransitionReservation()` call site remains;
- ordinary-island/no-island behavior is unchanged;
- no native translation, alpha, visibility, timing or collision ownership is added;
- Session stop, endpoint replacement, pre-draw detach, panel runtime failure, frame-loss fail-native and Hot Reload teardown paths remain unchanged.


## 2026-10-04 — Build 686 QS_FAKE hot-path reduction

**Type:** performance / post-integration audit
**Display version:** 0.0.5
**Build:** 686 / `20261004-686`
**Branch:** `fix/qs-fake-hotpath-overhead`

### Review finding

Build 685 lifecycle/single-writer review did not expose a new mutable owner or stale lease in the inspected Home, Keyguard/AOD family and QS_FAKE cleanup paths. The performance pass did find redundant work in the active Control Center transition:
- native peer tint resolution built a temporary filtered child collection on every pre-draw;
- immutable transition-source snapshot/color resolution was repeated when source state had not changed;
- tint diagnostic formatting ran inside every drawable frame;
- short-lived coordinate arrays were allocated by fake-layout and charging-island endpoint sampling.

### Change

Reduce only those hot-path costs:
- cache `TransitionSourceSnapshot` by the existing source `stateVersion`;
- keep live native peer tint refresh on pre-draw, because native applied tint may change independently from Guiyuan source state, but preserve selection order while scanning children without `map/filter` collections;
- format tint diagnostics only when the diagnostic snapshot is queried;
- reuse session-local coordinate buffers.

No geometry, transition timing, reservation curve, capacity lease, clip mask, source-scene policy, native appearance ownership, alpha/translation/visibility writer, or fail-native contract changes.

### Validation

Draft PR uses Light validation while the diff is reviewed. Mark ready only after static review; exact-head Runtime CI is required before merge. No Canary is justified by this checkpoint alone because default behavior and device-visible geometry are unchanged.

## 2026-10-04 — Build 687: remove heavy diagnostics from Keyguard-island stress path
**Type:** low-risk performance / observation-path cleanup
**Branch:** `fix/keyguard-island-diagnostic-hotpath`
**Build:** 687 / `20261004-687`
### Evidence
Build 686 is visually correct, but the maintainer reports progressive visible frame loss when Keyguard combined status is enabled, an island is active, and Control Center is repeatedly pulled fully down and swiped fully back up. The supplied Detailed diagnostic reproduces that stress loop and contains 22 island show/hide events plus 146 full `controlCenterTransitionGeometry` snapshots in roughly seven seconds. The geometry snapshots account for about 450 KB of synchronous log text; Home native-source snapshots add about 52 KB.
### Root cause / change
- Home presentation called `reportNativeSourceSyncDiagnosticAfterLayout()` after every intercepted native `onLayout`. For the Home surface, the function traversed native peers and formatted the full snapshot before the outer event sink could discard it when Detailed diagnostics were disabled.
- Home native-source snapshots are now constructed only when Detailed diagnostics are enabled and the functional steady-peer-mirror state actually changed.
- Detailed Control Center full geometry snapshots are reduced from every 1/8 bucket to two meaningful checkpoints: 4/8 (mid-transition) and 7/8 (handoff edge). The existing lightweight panel-transition trace remains unchanged.
- Unit tests cover general-vs-Detailed gating, transition-reservation suppression, Home mirror-change gating, and selected heavy geometry buckets.
### Safety boundary
No change to island state ownership, steady-peer-mirror functional updates, source-scene selection, Keyguard Control Center lease, transition reservation/capacity, animation timing, drawing geometry, alpha/translation/visibility writers, native tint authority, or HyperOS lifecycle ownership.
### Device gate
Stress only the reported scenario first: Keyguard combined status + active island + repeated full pull/down-up cycles. If progressive jank persists, investigate the functional island-active Home peer scan next; do not mix that higher-risk path with this diagnostic A/B.

## 2026-10-04 — Build 689 remove gesture-frame diagnostics

**Type:** performance / diagnostic hot-path follow-up
**Display version:** 0.0.5
**Build:** 689 / `20261004-689`
**Branch / PR:** `fix/keyguard-island-diagnostic-hotpath` / #203

### Problem

Build 687 retained visible jank during repeated complete Control Center pull/down-up cycles from Keyguard with an active island.

### Evidence

The reproduced LSPosed window still showed hundreds of Guiyuan records emitted synchronously on the SystemUI main thread, including repeated multi-KB transition geometry snapshots, fraction-bucket panel traces, QS_FAKE native-source snapshots and repeated island-owner observations. A bucket gate reduced frequency but did not move those diagnostics out of the gesture path; reversals repeatedly crossed the selected buckets.

### Conclusion

Build 687's reductions remain valid but are insufficient. Finish eliminating avoidable observation work before changing functional island mirroring or drawable/compositing behavior.

### Change

- keep all Build 687 diagnostic gates;
- stop full transition geometry/state/projection snapshot construction from expansion callbacks;
- stop fraction-bucket-only panel diagnostics and diagnostic-only anchor/Home-motion capture;
- stop QS_FAKE native-source layout snapshot generation;
- retain Home native-source diagnostics only for actual steady-peer-mirror changes;
- deduplicate appearance and island-owner diagnostics to semantic state changes.

No transition geometry, reservation, functional steady-peer-mirror scan, scene ownership, native writer, animation timing or draw-layer compositing changes.

### Validation

Unit coverage asserts that fraction bucket changes alone are non-reporting while lifecycle/semantic edges remain observable. Exact-head Runtime CI and a focused Keyguard + active-island repeated-pull Canary device gate are required.

## 2026-10-04 — Build 690 cache native peer reflection

**Type:** performance / behavior-preserving functional hot-path optimization  
**Display version:** 0.0.5  
**Build:** 690 / `20261004-690`  
**Branch / PR:** `fix/keyguard-island-diagnostic-hotpath` / #203

### Evidence

Build 689 still reproduces substantial jank in the focused Keyguard + active-island repeated full pull/down-up stress case, so diagnostic construction is no longer treated as the primary suspect.

The captured runtime shows the steady Home peer mirror repeatedly following island state during the stress case while `hiddenSlots=[]`. Static review found the mirror's per-native-layout peer sampling repeatedly discovers reflection metadata: `slotOf()` scans the child method hierarchy for `getSlot()`, while `readTransitionIconState()` resolves the Companion accessor and re-scans state fields for every peer.

### Change

- cache native `getSlot()` accessors by concrete status-icon view class;
- cache native transition-state Companion accessors by concrete status-icon-group class;
- cache transition-state Fields by concrete state class;
- add a minimal island visibility reader that fetches only `visibleState` and `inIslandState` for steady-peer mirror decisions;
- keep the full cached transition-state reader for diagnostic/other callers.

### Safety boundary

No reduction in native-layout sampling cadence, no hidden-slot decision change, no peer clip ownership change, no animation/timing/geometry/reservation change, and no new writer. If reflection resolution fails, the same existing null/fail-soft behavior is preserved.

### Device gate

Repeat the exact Keyguard + active-island rapid full Control Center pull/down-up stress case. If jank remains substantial, move next to hidden Home tint work and then residual draw/compositing cost rather than changing mirror semantics.

## 2026-10-04 — Build 691 remove Detailed native-layout peer snapshots

**Type:** performance / diagnostic hot-path cleanup  
**Display version:** 0.0.5  
**Build:** 691 / `20261004-691`  
**Branch / PR:** `fix/keyguard-island-diagnostic-hotpath` / #203

### Evidence

Build 690 made the focused Keyguard + active-island pull path substantially smoother, confirming the native peer reflection work was valuable. A direct diagnostics-level A/B then showed General smoother than Detailed; switching back to Detailed reintroduced a mid-gesture hitch.

In that same SystemUI session, Detailed re-enabled repeated `homePresentation nativeSourceSyncDiag` records on island mirror edges. Each record rebuilt and formatted the complete non-represented peer row, even though the functional mirror already emitted the low-cost `active/hiddenSlots` semantic result.

### Change

- remove the full Home native-source peer snapshot from native `onLayout` callbacks;
- remove its obsolete HotPathDiagnosticPolicy branch and unit expectations;
- retain Build 690 reflection caches and minimal live mirror state reads;
- retain lightweight mirror, lease, scene, readiness and lifecycle diagnostics.

### Safety boundary

No functional mirror cadence/result change, no clip ownership change, no animation/timing/geometry/reservation change and no native writer change.

### Device gate

Repeat the same Keyguard + active-island pull stress in General and Detailed. Detailed should no longer have a distinct mid-gesture hitch. If both levels become equivalent but still trail Home, move to residual draw/compositing cost.

## 2026-10-04 — Build 693 deduplicate semantic diagnostics

**Type:** diagnostics-only performance cleanup  
**Display version:** 0.0.5  
**Build:** 693 / `20261004-693`  
**Branch / PR:** `fix/keyguard-island-diagnostic-hotpath` / #203

### Evidence

Build 691 removed the full Home native-source peer snapshot. In the supplied A/B session, Detailed still emitted paired records for the same semantic edges: Home mirror state followed by a QS_FAKE mirror echo, and island-owner show/hide followed by an appearance echo.

### Change

- retain Home authoritative `steadyPeerMirror source=home` state;
- remove the immediate target-session mirror echo;
- retain direct `islandOwner` state-edge diagnostics;
- remove text-only `controlCenterAppearance` logging while preserving the native appearance hook and functional update callback.

### Safety

No native state read/write, mirror cadence, hidden-slot result, appearance update, geometry, transition, reservation, tint or lifecycle behavior changes. Exact-head CI is sufficient; no new device gate is required.

## 2026-10-04 — Build 694 fix QS_FAKE hidden/prearm lease lifecycle

**Type:** lifecycle correctness / fail-native ownership  
**Display version:** 0.0.5  
**Build:** 694 / `20261004-694`  
**Branch / PR:** `fix/keyguard-island-diagnostic-hotpath` / #203

### Evidence

The previously captured one-time native fallback was caused by a hidden/prearm capacity lease being overwritten by a legitimate HyperOS hidden relayout. The lease snapshot retained the expanded applied width and the next visible preparation misclassified the live native width as a foreign writer conflict.

### Change

- track QS_FAKE visible-cycle ownership separately from hidden/prearm ownership;
- reconcile the first visible edge while hidden/prearm ownership is still active;
- adopt a changed live native width only when hidden/prearm, positive, within the unchanged parent-content contract;
- clear the stale snapshot, record the adopted live width as the pending native baseline, and reacquire through the existing lease path;
- preserve fail-native for every visible-cycle width mismatch;
- preserve fail-native when the parent-content contract itself changed.

### Safety

The writer-conflict guard is not removed or weakened for visible ownership. No timers, polling, persistent ownership state, or alternate layout writer are introduced. Existing hidden-boundary release remains authoritative.

### Validation

Unit coverage reproduces the exact hidden 587→836→587 ownership sequence and verifies visible mismatch, unchanged hidden reuse, and parent-contract mismatch behavior. Exact-head Runtime CI plus code review are required before dev merge.

## 2026-10-04 — Build 695 confirm attached QS_FAKE visible ownership

**Type:** lifecycle correctness follow-up  
**Display version:** 0.0.5  
**Build:** 695 / `20261004-695`  
**Branch / PR:** `fix/keyguard-island-diagnostic-hotpath` / #203

### Review finding

The first native visible callback may arrive before a QS_FAKE session has been attached. The early owner-level `beginVisibleCycle()` therefore cannot by itself guarantee that the new session enters visible lease ownership.

### Change

- keep the early owner-level visible-cycle reconcile for existing prearmed sessions;
- on an attached session's `requestedVisible false→true` edge, confirm owner visible-cycle handoff again;
- repeated true→true does not re-enter the handoff;
- false→true failure leaves requested visibility false and preserves fallback;
- keep Build 694 hidden/prearm native-width adoption and visible writer-conflict semantics unchanged.

### Validation

Unit coverage verifies false→true is the only attached-session begin edge and true→false remains the only end edge. Exact-head Runtime CI and final lifecycle review are required before dev merge.

## 2026-10-04 — Build 696 bounded SystemUI restart transaction

**Type:** maintenance UI / Root lifecycle  
**Display version:** 0.0.5  
**Build:** 696 / `20261004-696`  
**Branch / PR:** `fix/systemui-restart-dismiss-boundary` / #205

### Problem

The explicit restart action used `killall com.android.systemui` immediately after hiding the MIUIX confirmation dialog. It neither waited for the dialog exit lifecycle nor verified which SystemUI process was terminated or whether a replacement process appeared.

### Change

- positive confirmation only arms a transient restart and dismisses the dialog;
- cancel/back clears the pending action;
- Root restart begins only from MIUIX `OverlayDialog.onDismissFinished`;
- resolve the current SystemUI PID with `pidof`;
- signal only the originally resolved PID with SIGTERM;
- if that PID is already stale at signal time, do not signal a newly appeared PID;
- verify within 60 × 100 ms that the original PID disappears and a replacement `com.android.systemui` PID appears;
- retain a 10-second outer Root command timeout;
- update failure copy so it covers Root, termination and replacement-start verification failures;
- add policy tests that reject `force-stop`, `am crash`, SIGKILL, `killall`, `pkill`, and any retry-PID signal path.

### Safety boundary

No island state mutation, fixed UI delay, resident Root service, runtime polling, new SystemUI writer, or project-owned restart lifecycle state machine is introduced. Build 695 runtime behavior remains unchanged.

### Device gate

Verify no-island, normal-island, and charging-island restart. Confirm the dialog exits first, SystemUI automatically returns, cancel/back never triggers restart, and failure feedback appears only when restart cannot be confirmed.

## 2026-10-04 — Build 697 replay validated charging-island ring direction

**Type:** integration of device-validated transition correction  
**Display version:** 0.0.5  
**Build:** 697 / `20261004-697`  
**Branch / PR:** `fix/charging-island-ring-direction-dev` / #207  
**Base:** Build 696 dev

### Change

Replayed only the previously device-validated Battery-Island battery-ring direction policy and its unit coverage onto current dev.

- native Battery-Island active: derive ring exit from final-row logical start (LTR -> LEFT, RTL -> RIGHT);
- non-island: preserve live Build 544 CENTER source-to-target geometry direction;
- retain Build 696 restart-maintenance and Build 695 runtime/performance changes unchanged;
- no pixel constant, delay, latch, new animator, geometry writer, reservation, tint, peer-ownership, or transition-timing change.

### Validation

The same functional correction passed the Build 688 device gate. This replay requires exact-head Runtime CI and review only.

## 2026-10-04 — Build 701 diagnostics report icon semantics

**Type:** app UI / visual semantics  
**Display version:** 0.0.5  
**Build:** 701 / `20261004-701`  
**Branch / PR:** `feat/diagnostics-info-icons` / #206

### Evidence

Build 700 device feedback accepted the Material Symbols information-row treatment and requested the Diagnostics & reports section to use the same visual language. Follow-up clarified that symbol selection must follow each row's text meaning rather than mimic the previous MIUIX glyph.

### Change

- diagnostics level → `troubleshoot`;
- export diagnostic report → `file_export`;
- share diagnostic report → `share`;
- all three render through the same local drawable leading-icon path as the accepted information rows;
- no runtime icon library dependency is added.

`diagnosis` was rejected because its actual vector reads as medical/health diagnosis; `rule_settings` was rejected as generic rules/settings. `tune` was not retained merely for visual continuity with the previous MIUIX glyph.

### Validation

Runtime behavior is unchanged. Exact-head CI plus one focused device optical/semantic review is sufficient.

## 2026-10-04 — Build 704 semantic leading-icon convention

**Type:** app UI / visual semantics  
**Display version:** 0.0.5  
**Build:** 704 / `20261004-704`  
**Branch / PR:** `feat/diagnostics-info-icons` / #206

### Evidence

Build 700 device review showed that Material Symbols work well as semantic row-leading glyphs inside MIUIX pages. Follow-up review distinguished those explanatory glyphs from MIUIX-owned control/navigation affordances and found several Diagnostics mappings that followed value text or duplicated another row's visual semantics.

### Conclusion

Use Material Symbols for semantic leading icons that explain what a settings/information row represents. Keep MIUIX icons where the icon is part of a MIUIX control/navigation affordance. Select symbols from the row's text semantics, not from the previous icon shape or a word appearing in the value.

### Change

Diagnostics refines version → `tag`, package/application ID → `data_object`, and framework → `schema`. Appearance adds theme mode → `contrast`, dynamic color → `palette`, floating navigation → `bottom_navigation`, navigation style → `style`, and navigation content → `view_list`. One shared semantic renderer owns the 24dp optical box, theme tint and disabled alpha.

### Validation

App UI/resources only. Exact-head CI plus focused light/dark optical/semantic device review is sufficient.

## 2026-10-04 — Build 705 Filled-first semantic icon pass

**Type:** app UI / visual semantics  
**Display version:** 0.0.5  
**Build:** 705 / `20261004-705`  
**Branch / PR:** `feat/diagnostics-info-icons` / #206

### Evidence

Build 704 device review showed that the semantic-icon direction is correct but the Outlined weight is too light for Guiyuan's MIUIX cards. The framework `schema` glyph also reads as a flow/structure diagram rather than an Xposed-style framework/module concept, and the compatibility-baseline `HyperOS 4` prefix adds visual noise.

### Convention

- semantic row-leading icons prefer Material Symbols Filled;
- when Filled and Outline are nearly identical, use Filled for convention consistency;
- retain Outline only when Filled materially harms legibility or weight balance;
- MIUIX icons remain for MIUIX control/navigation affordances.

### Change

- framework: `schema` → Filled `extension`;
- device model: Filled `smartphone`;
- navigation content: `view_list` → Filled `format_list_bulleted`;
- all other compatible semantic resources switch to their official Fill=1 paths;
- compatibility baseline deliberately retains Outline `fact_check` because its Filled card is visually heavier than adjacent rows;
- remove `HyperOS 4` from the compatibility-baseline secondary label;
- remove obsolete local vectors `info/api/package_2/schema/view_list`.

### Validation

App UI/resources/text only. Exact-head CI plus one focused light/dark optical/semantic device review is sufficient.

## 2026-10-04 — Build 706 semantic icon optical-weight normalization

**Type:** app UI / optical tuning  
**Display version:** 0.0.5  
**Build:** 706 / `20261004-706`  
**Branch / PR:** `feat/diagnostics-info-icons` / #206

### Evidence

Build 705 device review accepted the Filled-first direction but showed that several line-constructed Material Symbols still read visually hollow/light beside solid silhouettes such as `smartphone`, `dashboard`, and `extension`.

### Decision

Use Material Symbols weight as the optical-balancing axis instead of changing icon semantics or scaling the whole drawable. Keep the existing 24dp optical box and 22dp rendered size.

### Change

- `tag`: Fill1 / W500;
- `data_object`: Fill1 / W500;
- `target`: Fill1 / W500;
- `troubleshoot`: Fill1 / W500;
- `fact_check`: Outline / W500;
- all other Build 705 semantic glyphs remain unchanged.

### Safety

Drawable resources only; no Compose geometry, settings behavior, diagnostics/runtime logic, SystemUI hooks, or Xposed ownership changes.

### Validation

Exact-head CI plus focused Build 705 vs 706 optical A/B is sufficient.


## 2026-10-04 — Build 711 diagnostics event-viewer pass

### Device evidence
- Build 710 proved the new Diagnostics destination can read the real LSPosed module log, but full raw envelope lines made cards visually noisy and inconsistent in height.
- Device review selected LSPosed's information hierarchy as a reference: explicit level, time, event summary, compact collapsed rows, and expanded detail on demand.
- The product requirement is not to clone LSPosed styling. Guiyuan should use MIUIX-native components, preserve technical identifiers in English, and localize only the human-facing display layer.

### Decision
- Keep the existing runtime log producers unchanged for this checkpoint.
- Add a read-side envelope/event parser that understands current LSPosed envelopes, logcat fallback, RuntimeDiagnosticsProtocol Schema 1, and legacy key/value messages.
- Display log level through MIUIX Badge rather than a custom-drawn tag.
- Expose only All / Info / Warning / Error filters because current Guiyuan runtime producers use INFO, WARN, and ERROR; do not invent unused Verbose/Debug/Fatal controls.
- Rename the scope to This run / Current log: the first is the latest runtime session selected by sessionId/PID, while the second is every Guiyuan line in the currently selected source file.
- Use the middle dot only for short peer-level summary metadata; structural hierarchy uses layout rather than separator characters.


## 2026-10-04 — Build 712 useful-log hierarchy pass

- Build 711 device review confirmed INFO/WARN/ERROR parsing works, but healthy sessions naturally contain mostly INFO. The issue is relevance, not missing severity support: real WARN/ERROR producers remain compatibility-unavailable, hot-reload-declined/incomplete, hook-install failures and similar abnormal paths.
- The visible source label was removed. LSPosed/logcat are transport/storage sources, not the product identity of the logs.
- Runtime view is now a positive allow-list of stable lifecycle/state events plus every warning/error; Detailed view remains the full Guiyuan-filtered source. This avoids treating high-frequency tint, presentation probes and latency telemetry as the primary user-facing log stream.
- Range (This run / Current log) and severity are filter dimensions, not top-level log types, so they move into the MIUIX filter menu. Runtime / Detailed becomes the page-level view switch.
- Expanded fields use vertical labels/values and raw transport text is second-level disclosure. Bulk parsing is moved to Dispatchers.Default so thousands of source lines are not regex-parsed during Compose recomposition.


## 2026-10-04 — Build 713 unify diagnostics snapshot ownership

**Problem**

The Diagnostics page and diagnostic-report path both consumed Guiyuan runtime evidence, but each assembled its own view of that evidence. This made LSPosed/logcat transport appear like a separate product from the report and allowed page state and an exported report to be captured at different boundaries.

**Conclusion**

Guiyuan has one diagnostic data model. LSPosed/logcat remains a transport/storage source only. App-side presentation and report formatting must consume one canonical on-demand snapshot rather than becoming independent collectors.

**Change**

- add `DiagnosticsSnapshotProvider` as the single app-side capture boundary for environment, diagnostics preference, runtime log source, parsed entries, Runtime health, share diagnostics and capture time;
- make `DiagnosticsScreen` consume that snapshot instead of directly reading/parsing the transport log;
- make `DiagnosticsReportBuilder` format a `DiagnosticsSnapshot`, retaining its context overload only as a convenience that captures through the same provider;
- keep runtime producers, hooks, event schema, LSPosed/logcat fallback and diagnostics-level semantics unchanged.

**Validation**

Exact-head Runtime CI and static review are required. No device gate is required for this data-ownership refactor because it intentionally preserves the Build 712 visible presentation and SystemUI runtime behavior.


## 2026-10-04 — Build 714 diagnostics workbench product boundary

**Problem**

Even after Build 713 unified app-side capture, the visible UI still behaved like a second LSPosed log browser: range/severity controls and raw transport disclosure competed with Runtime health and the generated report. Report actions also remained in About, reinforcing the impression that logs and diagnostics were separate products.

**Conclusion**

LSPosed/logcat is an implementation transport, not Guiyuan's diagnostics UI. Guiyuan Diagnostics should present interpreted health and useful events from one captured snapshot; the diagnostic report is simply another representation of that same snapshot.

**Change**

- replace the log-viewer hierarchy with Runtime health and a bounded useful-event stream;
- keep raw transport lines out of the first-class Guiyuan UI while retaining them as report evidence;
- move diagnostics level, refresh, Clear view, copy, export and share into the Diagnostics top-bar action menu;
- make copy/export/share format the exact in-memory snapshot currently displayed by Diagnostics;
- define Clear view as presentation-only state: no LSPosed/logcat file or Guiyuan diagnostic evidence is deleted;
- remove diagnostics/report controls from About so it returns to static identity/environment information.

**Validation**

Exact-head Runtime CI plus UI/code review are required. A focused Canary visual/interaction pass is warranted after automated validation because the page hierarchy and action placement change, while SystemUI runtime behavior does not.


## 2026-10-04 — Build 716 keep Diagnostics snapshot capture lightweight

**Review finding**

Build 713 unified app-side diagnostic ownership, but it also moved the existing share-feature logcat probe into every Diagnostics page capture. That made opening/refreshing the workbench pay for evidence unrelated to SystemUI runtime health, including on builds where share diagnostics are not populated.

**Change**

- remove the `CombinedStatusShare` logcat Root command and share-operation store from `DiagnosticsSnapshotProvider`;
- remove the unrelated Share diagnostics section from the generated diagnostic report;
- keep share-operation debug ownership in `DiagnosticsReportFiles` / `ShareDiagnosticsStore` where those records are produced;
- preserve the LSPosed-first Guiyuan runtime evidence, parsed events, Runtime health, environment and diagnostics-level data used by both the workbench and report.

**Validation**

Exact-head Runtime CI is required. No device gate is added for this capture-cost reduction because it removes unrelated collection work without changing the workbench hierarchy or SystemUI runtime behavior.


## 2026-10-04 — Build 717 remove retired diagnostics viewer surface

- Build 716 Runtime CI #2645 passed.
- Static review found the pre-workbench raw-log card, raw-line disclosure, severity filter helper and About diagnostic action helpers remained in `FeatureScreens.kt` with zero call sites after the workbench cutover.
- Remove those retired composables/helpers, their now-unused MIUIX/icon imports, and strings that only served source/range/severity/raw-log browsing.
- No runtime producer, snapshot semantics, report content, workbench behavior or SystemUI path changes.
- Exact-head Runtime CI is the final automated gate before focused Canary validation.


## 2026-10-04 — Build 718 parse only the diagnostic session

- After the raw-log viewer was removed, `DiagnosticsSnapshot.allEntries` had no consumer.
- Stop parsing every Guiyuan line in the selected source file; parse only `latestSessionLines` for the workbench.
- Keep the underlying reader's full source lines only long enough to identify the latest session; the diagnostic report already embeds only that same session's bounded raw evidence.
- This is an allocation/CPU cleanup only; no UI, report semantics, runtime producer or SystemUI behavior changes.


## 2026-10-04 — Build 719 align Diagnostics product wording and action affordance

- The workbench no longer presents raw LSPosed/logcat browsing, so its loading state now says “Refreshing diagnostics / 正在刷新诊断” instead of “Reading logs”.
- The top-bar menu owns diagnostics level, refresh, Clear view, copy, export and share; use the MIUIX `More` affordance rather than `Tune`, which would incorrectly imply a filter/settings-only menu.
- No snapshot content, report content, Root collection, runtime producer, SystemUI/Xposed behavior or interaction semantics change.
- Exact-head Runtime CI is sufficient before the already-required focused Canary UI gate.


## 2026-10-04 — Build 720 restore Preview Sandbox Tune import

**CI evidence**

Build 719 Runtime CI #2647 failed compilation at `FeatureScreens.kt:603` and `:623` with unresolved `Tune`. The Diagnostics action menu had correctly moved to MIUIX `More`, but the same file still uses `MiuixIcons.Normal.Tune` in two pre-existing Preview Sandbox navigation examples.

**Change**

Restore the MIUIX `Tune` extension import. Diagnostics remains on `More`; Preview Sandbox retains its existing `Tune` icons.

**Validation**

Exact-head Runtime CI must pass before Canary. No device-only behavior changed.


## 2026-10-04 — Build 721 restore compact Diagnostics presentation

**Device evidence**

Build 720 Canary #766 passed automated, signing, Modern Xposed metadata and non-debuggable checks. Device review rejected the visible workbench hierarchy: the Runtime health card rendered expected-but-unobserved components as repeated `unknown` rows after hot reload, occupied most of the first screen, and a structured event with no friendly summary fell back to internal `key=value` payload text.

**Decision**

Keep the Build 713-720 unified Diagnostics data/report architecture. Reuse the Build 712 compact semantic event-card presentation as the visual baseline instead of exposing the report-oriented health matrix as a first-class UI.

**Change**

- remove the Runtime health matrix from the visible Diagnostics page; it remains intact in `DiagnosticsSnapshot` and exported/copied/shared reports;
- restore the compact event-list hierarchy with one lightweight “This run · N key events” summary above the list;
- retain the Build 712 card rhythm: level + category + time, title, one-line summary, structured details on expansion;
- never use a structured transport message as the default summary fallback; use “Recorded / 已记录” when no user-facing state/source/reason summary exists;
- keep the MIUIX More menu, diagnostics level, refresh, Clear view, and same-snapshot copy/export/share actions unchanged.

**Safety**

No Xposed/SystemUI producer, RuntimeDiagnosticsProtocol, snapshot capture, report content, hook, writer, lifecycle, transition or native fallback behavior changes.

**Validation**

Exact-head Runtime CI plus focused Canary visual review are required.


## 2026-10-04 — Build 722 restore compact event-card helper

Static diff review of Build 721 caught that removing the rejected Runtime health block also mechanically removed `DiagnosticsUsefulEventCard`, while the new compact list still referenced it. Restore the same compact card implementation used by the prior workbench: level/category/time, title, one-line summary, and structured details on expansion. No diagnostics model, filtering, report, menu, runtime or SystemUI behavior changes.


## 2026-10-04 — Build 723 toolbar actions, grouped More menu and dated event time

- toolbar order is Share -> Export -> More, using MIUIX `Share`, `FileDownloads` and `More` icon actions;
- Share, Export and More use MIUIX `TooltipBox`, so touch long-press shows their labels without custom gesture or bubble code;
- Copy diagnostic report is removed from the product UI;
- the More popup uses three `DropdownEntry` groups, allowing MIUIX `OverlayIconCascadingDropdownMenu` to insert native `HorizontalDivider` separators:
  1. diagnostics level + refresh;
  2. scroll to top + scroll to bottom;
  3. Clear view;
- Diagnostics owns its `LazyListState`; scroll actions animate the existing list and never refresh or recapture data;
- the shared private `SettingsPage` accepts an optional list state while existing callers retain an internally remembered state;
- event timestamps display `MM-dd HH:mm:ss` for both LSPosed and logcat envelopes, covered by parser tests.

No snapshot/report schema, runtime producer, SystemUI/Xposed hook, writer, lifecycle, transition or native fallback behavior changes.


## 2026-10-04 — Build 724 diagnostics interaction and typography redesign

**Device evidence**

Build 723 Canary #767 confirmed the compact event-list direction but exposed several presentation issues: the export glyph was visually inconsistent with Share/More, the cascading diagnostics-level selector replaced/morphed over the primary menu instead of reading as a side submenu, the page still relied on middle-dot separators, and the overall text hierarchy needed a full MIUIX typography pass. The user also requested a multi-select Filter control beside Back.

**Interaction changes**

- top bar leading side: Back + MIUIX Normal Filter; Filter long-press uses MIUIX Tooltip and a theme-primary dot indicates any non-default filter;
- top bar trailing side: MIUIX Normal Share + Download + More, each with long-press Tooltip;
- Filter opens MIUIX OverlayBottomSheet with multi-select log-level and event-type groups, Reset filters and Done actions;
- view filters are local presentation state only. They do not modify DiagnosticsSnapshot or report content;
- filter order is semantic-event selection -> user filter -> 40-event display cap, so matching older events are not hidden by unrelated newer entries;
- More is rebuilt from MIUIX OverlayListPopup. Diagnostics level opens a second MIUIX OverlayListPopup anchored to its own row using a dedicated PopupPositionProvider, preserving the primary menu as the spatial parent;
- native HorizontalDivider rows separate diagnostics controls, list navigation and Clear view.

**Visual/typography changes**

- replace FileDownloads with the lighter MIUIX Normal Download glyph;
- remove middle-dot separators from Diagnostics UI copy; localized natural separators are used instead;
- map the page to the pinned MIUIX text scale with no hand-added font weights: metadata/run summary -> footnote1, event title -> body1, summaries/values -> body2, detail labels -> footnote1;
- collapsed event cards use `heightIn(min = 86.dp)` rather than a fixed 96dp height, preserving font-scale growth;
- any recognized semantic/legacy event without a friendly summary shows “Recorded / 已记录” instead of exposing raw `key=value` payload text;
- filter rows are data-driven to avoid nine duplicated CheckboxPreference blocks.

**Runtime boundary**

No RuntimeDiagnosticsProtocol, DiagnosticsSnapshot capture, report serialization, SystemUI/Xposed hook, writer, lifecycle, transition, renderer or native-fallback behavior changes.

**Validation**

Runtime CI #2657 passed on the behavior-complete code before the final deduplication/docs-only cleanup. Run exact-head validation after this commit, then a focused signed Canary visual/interaction gate.


## 2026-10-04 — Builds 725-727 MIUIX window controls and semantic status tags

**Device evidence**

Build 724 Canary #768 passed automated/signing checks, but device review found three first-class UI problems:
- the Filter icon rendered beside Back but tapping it did not open the filter sheet;
- Share / Download / More did not have balanced optical weight;
- the diagnostics-level submenu still appeared to replace/overlap the primary More menu instead of reading as a side hierarchy.

The event-level INFO badge also read visually like a small button. Reference review established that the intended treatment is the small META-style rounded-rectangle status tag rather than a notification badge or plain text.

**MIUIX component decision**

Use MIUIX semantic components wherever the library provides them, and use MIUIX basic primitives to reproduce a missing higher-level component:
- `WindowBottomSheet` replaces the Scaffold/overlay-dependent filter sheet;
- `WindowListPopup` replaces overlay popups for the primary More menu and diagnostics-level selector;
- `IconButton`, `TooltipBox`, `CheckboxPreference`, `HorizontalDivider`, `Card`, `SnackbarHost`, `SmallTopAppBar` and MIUIX `Text` remain the first-class controls;
- Compose `Row`, `Box`, `Column`, `Spacer` and `LazyColumn` remain layout primitives only;
- MIUIX has no dedicated Tag/Chip component in the pinned revision, so the event-level tag is composed from non-clickable MIUIX `Surface + Text`, not custom Canvas drawing and not a Button.

**Toolbar**

- leading: Back + Light Filter;
- trailing: Light Share + Light Download + Medium More;
- all auxiliary actions retain MIUIX long-press Tooltip behavior and the standard IconButton hit area;
- visual weight is tuned through the MIUIX icon weight family rather than geometry scaling or positional compensation.

**Event-level tag**

- read-only rounded rectangle, not a circular badge or button;
- minimum height: 20dp;
- corner radius: 5dp;
- padding: 6dp horizontal / 2dp vertical;
- typography: MIUIX `footnote2` (11sp) + Bold to match the reference status-label hierarchy;
- INFO: MIUIX `tertiaryContainer/onTertiaryContainer`;
- ERROR/FATAL: MIUIX error family;
- neutral/debug: MIUIX secondary/surface family;
- WARN: Guiyuan's pre-existing runtime warning accent rather than a new diagnostics-only color.

Build 727 moves the existing Home runtime success/warning accents into `ui/theme/RuntimeStatusColors.kt` so Home and Diagnostics share the same semantic warning token.

**Badge semantics**

MIUIX `Badge` is retained only for the active-filter dot, where its documented dynamic-indicator semantics are appropriate. It is no longer used for INFO/WARN/ERROR labels.

**Runtime boundary**

No diagnostics capture/parser/report schema, runtime producer, Xposed/SystemUI hook, writer, lifecycle, transition, renderer, or native fallback behavior changes.

**Validation**

- Build 725 intermediate window-component checkpoint: Runtime CI #2659 passed.
- Build 726 status-tag checkpoint: Runtime CI #2661 passed after replacing the failed pure-text experiment from #2660.
- Build 727 exact-code Runtime CI #2662 passed.
- Run final exact-head validation after this documentation commit, then a focused signed Canary device gate.

## 2026-10-04 — Build 728 dedicated MIUIX diagnostics menus

**Problem**

Build 727 used MIUIX primitives but still manually composed Filter and More from lower-level window popups/sheets. Device evidence showed that the result did not read like native MIUIX: Filter was too heavy as a settings-style sheet, menu/submenu placement was fragile, group dividers were manually owned, and Light/Medium icon mixing produced inconsistent toolbar visual mass.

**Pinned-revision audit**

The exact pinned MIUIX revision `5c91d5e5ce1a2fc7e8bdc1258a881c555102bbca` provides `WindowIconCascadingDropdownMenu`. It owns IconButton interaction, hold-down state, haptics, window popup, two-level cascade and grouped-entry separators. `DropdownEntry` boundaries render MIUIX dividers internally. The revision has no Tag/Chip component, so the read-only `Surface + Text` status tag remains the smallest correct representation for INFO/WARN/ERROR metadata.

**Change**

- Filter moves from `WindowBottomSheet + CheckboxPreference` to `WindowIconCascadingDropdownMenu`.
- Log level and Event type are child menus; selected leaves use MIUIX dropdown selection semantics; Reset filters is a separate entry group.
- Filter changes apply immediately to local presentation state; the draft/apply state machine is removed.
- More moves from hand-built `WindowListPopup` instances and a custom position provider to `WindowIconCascadingDropdownMenu`.
- Diagnostics level becomes a native child menu; Refresh, scroll actions and Clear view use `DropdownEntry` groups so MIUIX owns separators.
- Filter/Share/Download/More use the MIUIX default alias (Regular weight in this revision), removing Light/Medium mixing.

**Diagnostics page component audit**

Keep MIUIX `SmallTopAppBar`, `IconButton`, `TooltipBox`, `WindowIconCascadingDropdownMenu`, `Card`, `SnackbarHost`, `Surface`, `Text`, and `Badge/BadgedBox` only for the active-filter indicator. Keep Compose layout primitives only where no MIUIX semantic component exists. No higher-level MIUIX Tag/Chip or event-detail component exists in this revision.

**Runtime boundary**

No snapshot capture, parser/report schema, runtime producer, SystemUI/Xposed hook, writer, lifecycle, renderer, transition, native fallback, or exported diagnostic content changes.

## 2026-10-04 — Build 730 diagnostics continuity, hierarchy and technical detail language

**Device evidence**

Build 729 confirmed that MIUIX cascading menus themselves animate correctly, but the surrounding Diagnostics page still felt discontinuous because presentation-owned state changed abruptly: card expansion inserted detail rows immediately, filtering replaced/reordered LazyColumn items without item transitions, refresh swapped list/state content without a live indicator, and the active-filter badge changed wrapper structure. Device review also found the toolbar glyphs optically unbalanced despite using one nominal weight family, and the event-card typography hierarchy remained too flat.

**Motion**

- Keep MIUIX menu/submenu motion untouched.
- Give every diagnostic event a stable LazyColumn key and use the Compose lazy-item animation path for filtered insertion/removal/placement.
- Animate expanded detail with fade + top-anchored vertical expand/shrink.
- Animate the summary's one/two-line size change.
- Keep the Filter anchor stable and fade only the MIUIX Badge dot.
- Use MIUIX `InfiniteProgressIndicator` for the refresh/loading state instead of static text-only feedback.

**Optical icon balance**

No scaling, translation or custom drawing is introduced. Weight selection stays inside the pinned MIUIX icon family:
- Back: default Regular;
- Filter: Normal;
- Share: Medium;
- Download: Medium;
- More: Normal.

This is deliberately optical rather than mechanically identical: Filter/More occupy more dark area at the same 24dp canvas, while Share/Download are more open outlines.

**Typography hierarchy**

- run summary: `subtitle` (14sp Bold);
- event title: `headline1` (17sp) + Medium;
- category/time: `footnote2` (11sp);
- event summary: `body2` (14sp);
- expanded technical labels: `footnote2` + Medium;
- expanded technical values: `footnote1` (13sp);
- INFO/WARN/ERROR/FATAL tag remains `footnote2 + Bold`.

**Expanded technical language**

The collapsed event surface remains localized and friendly. Once an event is expanded, the technical field layer is intentionally language-stable: `Event`, `Component`, `State` and raw protocol field names remain English regardless of app locale. The three now-unused localized resource strings are removed.

**Runtime boundary**

No DiagnosticsSnapshot capture, parser/report schema, runtime producer, SystemUI/Xposed hook, writer, lifecycle, transition, native fallback or report export content changes.

### Build 731 review correction

Post-commit diff review of Build 730 caught one mechanical scope leak and one missing animation attachment before device testing:
- restore `AppearanceMiniPreview` to its pre-Build-730 root `Modifier`; the Diagnostics presentation pass must not alter Appearance;
- attach the passed LazyItemScope animation modifier to `DiagnosticsUsefulEventCard` itself, so stable-key filter/reorder animation actually reaches the card.

No motion design, typography, icon-weight decision, technical-detail language, runtime path or report behavior otherwise changes from Build 730.

### Build 732 toolbar optical correction

Build 731 device evidence shows the Download glyph materially heavier than Share even when both use MIUIX Medium. The mismatch is intrinsic to the glyph silhouettes: Download concentrates the vertical arrow and tray into a denser dark area, so matching nominal weight does not produce matching optical weight.

Correction:
- Share stays `MiuixIcons.Medium.Share`;
- Download returns to the default `MiuixIcons.Download` alias (Regular at the pinned revision);
- Back remains Regular; Filter and More remain Normal;
- keep the native 24dp vector canvas and IconButton hit target unchanged; do not scale, translate, stroke, or redraw icons.

This is presentation-only. Cascading-menu behavior and all runtime/report paths remain unchanged.

### Build 733 — restore pinned MIUIX toolbar defaults

Maintainer review rejects project-local per-glyph weight compensation for small toolbar actions. The pinned MIUIX revision already defines the icon aliases, intrinsic 24dp vectors and 40dp IconButton geometry; project code should not mix Normal/Medium weights to force optical matching.

Build 733 therefore restores the Diagnostics toolbar to the upstream defaults:
- Back: `MiuixIcons.Back`;
- Filter: `MiuixIcons.Filter`;
- Share: `MiuixIcons.Share`;
- Download: `MiuixIcons.Download`;
- More: `MiuixIcons.More`.

Direct actions remain MIUIX `IconButton`; Filter/More remain MIUIX `WindowIconCascadingDropdownMenu`, whose trigger is the same MIUIX IconButton primitive. No explicit icon size, scale, translation, stroke, or weight override remains.

Final device acceptance is deferred to an integrated dev Canary so this Diagnostics conformance pass can be checked together with the battery-color menu/pager cleanup requested by the maintainer.


## 2026-10-04 — Build 738 About conformance and Diagnostics pull refresh

**About**

- Keep the existing Guiyuan animated identity-mark implementation unchanged.
- Replace the short placeholder identity copy with the same application/LSPosed description resource used by the package manifest.
- Separate brand identity from version metadata with pinned-MIUIX `HorizontalDivider` using upstream default thickness and divider color.
- Keep read-only metadata on MIUIX `BasicComponent`; use MIUIX `ArrowPreference` only for real navigation/actions.
- Project homepage and GNU GPL v3.0 license open directly; the redundant license detail route/page is removed.
- Third-party dependencies remain a real secondary page in the MIUIX navigation stack.
- Project homepage uses a Material Symbols `code` glyph and license uses `license`; third-party dependencies keeps `inventory_2`. Shared Material Symbols renderer and its geometry are unchanged.
- Device name/codename and Android/API metadata use same-line spacing rather than line breaks.
- Remove the duplicate project-address row from the outer Settings hub.

**Dependency/version/license audit**

- Direct dependency versions displayed in About now come from the same Gradle variables that declare the dependencies; UI constants no longer duplicate those versions.
- MIUIX shows the exact published snapshot `0.9.4-5c91d5e5-SNAPSHOT` rather than truncating it to `0.9.4`.
- Displayed license identifiers use SPDX forms: `Apache-2.0` and `EPL-1.0`.
- Material Symbols is explicitly classified as a local embedded asset with no fabricated library version.
- `THIRD_PARTY_NOTICES.md` is synchronized with the exact direct versions and SPDX license identifiers.

**Diagnostics**

- Add pinned-MIUIX `PullToRefresh` around the existing Diagnostics LazyColumn.
- Pull refresh and the existing More > Refresh action share the same `DiagnosticsSnapshotProvider.capture` generation path.
- Initial page load does not display the pull-refresh indicator; only a user pull raises the pull-refresh state.
- Refresh gesture state/text is localized; the MIUIX component owns drag threshold, animation and nested-scroll interaction.

**Runtime boundary**

No Xposed/SystemUI hook, renderer, transition, network source, native participant, writer, fail-native policy or runtime lifecycle behavior changes in this build.


## 2026-10-04 — Build 739 semantic-icon and refresh ownership normalization

- Extend the Material Symbols left-side semantic icon policy to the whole companion app, not a single page.
- Require actual glyph-shape inspection before selection; semantic correctness outranks visual neatness.
- Align the default Material Symbols baseline with pinned MIUIX Regular: Outlined W400 first, W500 only for perceptually light glyphs, Filled only when state semantics or legibility justify an exception.
- Enforce one renderer geometry contract through `SemanticLeadingIcon`: 24 dp optical box, 22 dp visual size, common alignment/tint; remove per-call visual-size overrides.
- Re-audit current semantic icons and replace About package/project/dependency glyphs with `package_2`, `folder_code`, and `account_tree`; normalize the remaining semantic vectors to official Material Symbols Outlined W400 sources where applicable.
- Remove superseded `data_object`, `code_blocks`, and `inventory_2` assets.
- Diagnostics refresh now has one UI owner. Initial capture alone may show the loading state card; all later recaptures keep the current list visible and use pinned-MIUIX `PullToRefresh`.
- Pull gesture, More > Refresh, and diagnostics-level recapture call the same guarded `requestRefresh()` path; duplicate refresh requests are ignored while capture is active.
- Pull-to-refresh is not installed during the initial empty snapshot, avoiding gesture/header feedback before the first capture completes.
- No Xposed/SystemUI runtime behavior changes.

## 2026-10-06 — Build 740 companion UI hierarchy and localization polish

**Type:** companion app / MIUIX navigation / localization
**Build:** 740 / `20261006-740`
**Branch:** `fix/companion-ui-polish`

### Problem

Device review exposed five companion-app issues: battery-color detail Back visually dismissed the whole sheet, the English Diagnostics title was ellipsized, the Preview Sandbox `Network` label wrapped in its fixed column, the top-information offset title carried redundant direction text, and About metadata/copy used ambiguous plain-space separation.

### Evidence / root cause

- Pinned MIUIX `OverlayBottomSheet` completes its Back dismissal motion before calling `onDismissRequest`. Changing the internal pager only from that callback is therefore too late: the sheet has already moved off-screen.
- Pinned MIUIX `SmallTopAppBar` reserves its default 26 dp horizontal title padding inside the width left after navigation/actions. Diagnostics has two leading and three trailing actions, so the English title loses another 52 dp even though the action geometry itself is valid.
- `PreviewStatusLine` assigned every localized label a fixed 48 dp width; `Network` is wider and wrapped.
- About device/codename and Android/API values were joined with ordinary spaces, and the Chinese About summary itself contained a plain space after “版本”.

### Conclusion / change

- Give the battery-color detail page its own nested NavigationBackHandler and disable outer-sheet dismissal while that detail level is active; Back first returns to the overview, while overview retains normal MIUIX dismiss behavior.
- Expose the upstream `SmallTopAppBar.titlePadding` seam through `SettingsPage` and set only Diagnostics to zero padding. Font size, button size, positions, and MIUIX action components remain unchanged.
- Let Preview status labels use intrinsic text width with a normal 12 dp gap; the value owns the remaining Row width.
- Remove the directional suffix from the top-information offset label in both locales.
- Use `｜` for Device/Codename, Android/API, and Scope/Package pairs; refine About summary copy to “Version, project, and runtime environment” / “版本、项目与运行环境”.

### Review / validation

This checkpoint does not touch Material semantic-icon resources or the shared `SemanticLeadingIcon` contract. Pre-CI review also removed an explicit `androidx.compose.foundation.layout.weight` import because the project already records that import as invalid for the pinned Compose version; `Modifier.weight()` is supplied by RowScope.

Run one exact-head Runtime PR validation. A Work-branch Canary is only needed if the interaction and optical fixes require focused device acceptance after automated validation.

## 2026-10-06 — Build 741 nested-sheet and Home observation readiness follow-up

**Type:** companion UI / MIUIX navigation / runtime readiness
**Build:** 741 / `20261006-741`
**Branch / PR:** `fix/companion-ui-polish` / #218

### Device evidence

Build 740 confirmed the first companion-UI fixes but exposed four follow-ups:
- Preview battery-mode and super-fast-charging labels still ellipsized in compact segmented controls;
- predictive Back from the battery-color detail page briefly shifted the returning overview horizontally;
- diagnostics event cards had no direct per-entry copy action;
- startup could log `statusIconObservation unavailable ... dark-icon-manager-missing` from `hostCapture`.

### Root cause

The battery-color implementation placed a non-scrollable `HorizontalPager` inside one MIUIX `OverlayBottomSheet` and added a second `NavigationBackHandler` while the sheet retained its own predictive-Back handler. One gesture could therefore participate in both sheet resistance and pager return, producing the observed transient horizontal offset.

The warning was a lifecycle-order mismatch rather than a missing target contract. Guiyuan captures `MiuiNotificationStatusContainer.onFinishInflate()`, while the required `mDarkIconManager` belongs to the parent `MiuiPhoneStatusBarView`. Exact-target SystemUI evidence already establishes `StatusBarIconControllerImpl.addIconGroup(...)` as the Home manager readiness boundary and explicitly rejects fixed delay/polling.

### Change

- Replace the pager/detail-back stack with a real second MIUIX `OverlayBottomSheet`; the overview remains the first-level owner and the detail sheet owns its own dismissal.
- Keep full battery-state names for summaries and add compact-only segmented labels.
- Add MIUIX Card `onLongPress` copy of `DiagnosticLogEntry.rawLine` plus localized Toast feedback.
- Add one read-only `addIconGroup` observation hook to the existing native-network owner. A missing Home manager at host capture becomes INFO/pending; registration of the same manager completes observation and emits structured ready state. True structural failures remain WARN + fail native.
- Hook count for that owner becomes five; suppression, native geometry and presentation writer ownership are unchanged.

### Review / validation gate

Pre-CI review removed the obsolete pager imports, kept pending readiness weak and single-host, closes pending state on success/failure/deactivation, and preserves full-summary localization. Run repository-selected exact-head CI; device validation is required afterward for nested-sheet Back continuity and cold-start absence of the former warning.

## 2026-10-06 — Home collapse-path device-evidence wording correction

**Type:** device evidence clarification / development memory
**Build:** 742 / `20261006-742`
**Branch:** `dev`

The maintainer clarified the residual native-status symptom before the 0.2.0 stable promotion:

- steady Home itself is not observed to switch back to native;
- the visible native exposure occurs after starting a panel pull from Home and then swiping/collapsing it back up;
- the defect therefore belongs to the Control Center collapse / return-handoff path, not to steady Home ownership or steady Home acquisition.

Future diagnosis must preserve that distinction. A shorthand such as “Home/desktop becomes native” is too broad and can send investigation toward the wrong owner. If this residual issue is reopened, inspect the QS_FAKE -> Home return boundary, projection release/reacquisition ordering, and native visibility handoff first; do not assume steady Home presentation has been lost without separate evidence.

This entry is documentation-only and changes no APK/runtime behavior. Build 742 device/runtime acceptance remains the Build 741 implementation checkpoint plus release metadata, with integrated dev Runtime CI #2757 passed.

## 2026-10-06 — Build 743: bound ordinary QS_FAKE reservation before fail-native

**Type:** runtime blocker / Control Center reverse-transition ownership
**Display version:** 0.2.0
**Build:** 743 / `20261006-743`
**Branch / PR:** `fix/control-center-capacity-overflow` / #220

### Evidence

Build 741 Detailed diagnostics captured the maintainer's rare Home-origin pull-down / swipe-up symptom. During a still-visible Control Center cycle, QS_FAKE had a verified native width of 587px, a leased parent-content width of 836px, and therefore 249px of additional physical carrier capacity. The session later emitted `failNative reason=fake-carrier-capacity-insufficient restoredNative=true` while Control Center was still visible.

The same session was non-charging and reported `batteryIsland=false`, so this is not the charging-island path and not a steady-Home owner loss.

### Root cause

`resolveCapacityBoundedReservationDelta()` bounded physical QS_FAKE reservation only when HyperOS marked the native Battery hidden. With the Battery visible, the function returned the full requested transition delta unchanged, but the following capacity guard still required that delta to fit the verified fake-carrier lease.

A valid transition frame could therefore request more native `statusIcons.paddingEnd` than the verified carrier can physically expose and tear down the whole QS_FAKE presentation even though Guiyuan's semantic transition remained valid.

### Correction

- keep Guiyuan semantic transition width, targets, progress, motion carrier and drawable geometry unchanged;
- keep one native peer-layout writer: `statusIcons.paddingEnd`;
- for Battery-visible QS_FAKE, cap only the native reservation delta at the live verified fake-carrier capacity;
- retain the existing Battery-hidden rule, where the compact slot plus leased capacity is the physical reservation limit;
- retain fail-native for real carrier-contract, writer-conflict, host, layout, or restoration failures;
- add focused unit coverage showing an in-capacity visible-Battery request is unchanged and an overflow request saturates at the verified capacity.

### Safety / device gate

No fixed device pixels, delay, retry, polling, translation, alpha, visibility writer, target rewrite or gesture timeline is added. The cap is derived from the same verified parent-content lease already owned by the QS_FAKE presentation session.

Required device gate: Home, non-charging, no island; repeatedly pull Control Center down and swipe it fully back up, including fast reversals. Confirm the projected transition never drops to native mid-return. Then smoke-test charging/island and Keyguard reverse pulls.

## 2026-10-06 — Build 744: scope Home steady-peer mirror to Home-origin QS_FAKE

**Type:** ownership correction / Keyguard-island performance
**Display version:** 0.2.0
**Build:** 744 / `20261006-744`
**Branch / PR:** `fix/keyguard-island-home-mirror-scope` / #221

### Evidence

The remaining device-visible gap is Keyguard + active island + repeated full Control Center pull-down / swipe-up remaining slightly less smooth than the equivalent Home path after the Build 689-693 diagnostic/reflection reductions.

Historical Build 690 diagnostics captured `sourceScene=KEYGUARD` while the functional `steadyPeerMirror source=home` continued following Home native layouts. Static review confirms `captureSteadyPeerMirror()` samples only the Home presentation surface and QS_FAKE previously consumed that module-level mirror without checking the authoritative Control Center source scene.

### Correction

- reuse the existing `GyModule.controlCenterSourceScene` authority rather than adding a second detector;
- propagate source-scene changes to `SysUiPresentationOwner`;
- sample and consume the Home steady-peer mirror only for `SourceScene.HOME`;
- clear the Home-derived mirror for `KEYGUARD` and `UNKNOWN`, leaving native QS_FAKE island authority in force;
- immediately seed the existing mirror from the current Home session when source authority returns to Home;
- keep Home hidden-slot policy, peer clip ownership and fake-island suppression unchanged.

### Review boundary

The native-layout hook still executes its normal layout validation, clip refresh and layout-ready completion for Keyguard-origin QS_FAKE; only the Home-derived mirror scan/propagation is skipped. The helper Boolean is not used as a failure or lifecycle signal.

No transition geometry, reservation/capacity, tint, progress, draw layer, alpha/translation/visibility writer, timer, poller, retry loop or fixed device geometry changes.

### Device gate

A/B Build 744 against Build 743 with Keyguard + active island and repeated complete pull-down / swipe-up cycles. Verify Home + island remains unchanged. If a meaningful gap remains, continue to residual TransitionDrawable/compositing audit rather than adding more mirror logic.

## 2026-10-06 — Build 745 rejected: bounded alpha layers clipped mobile signal

**Type:** rejected rendering-performance experiment
**Display version:** 0.2.0
**Candidate build:** 745 / `20261006-745`
**Branch / PR:** `fix/control-center-alpha-layer-bounds` / #224
**Integration status:** closed unmerged; accepted runtime baseline remains Build 744

### Intent

After Build 744 closed the proven Home-mirror ownership mismatch, one residual audit examined the cost of per-component `Canvas.saveLayerAlpha(null, ...)` calls in the Control Center transition drawable. Build 745 kept group-alpha semantics but bounded each offscreen layer to the source viewport plus known transition overflow.

### Device evidence

Focused Canary validation showed a visible mobile-signal clipping regression during the transition. The affected path is consistent with the new finite offscreen-layer boundary: mobile morph / latent reveal pixels can extend beyond the nominal source viewport after transform and axis compensation, while the previous unbounded layer did not impose that additional local edge.

The diagnostic session otherwise retained healthy runtime ownership and mobile presentation state, so the visual failure is sufficient to reject the optimization rather than reinterpret it as a state-source defect.

### Decision

- PR #224 is closed without merge.
- Do not add guessed padding, margins, or geometry compensation around the bounded layer.
- Do not carry Build 745 into `dev`, `main`, or a later checkpoint as an optimization baseline.
- Build 744 remains the accepted runtime baseline.
- The recent Keyguard / Control Center performance optimization line is closed. Reopen it only if new reproducible device evidence identifies a concrete blocker or bounded root cause.

## 2026-10-06 — Build 746: prepare Guiyuan 0.2.1 stable promotion

**Type:** version / stable-promotion metadata
**Display version:** 0.2.1
**Build:** 746 / `20261006-746`
**Branch:** `dev`

### Scope

The maintainer explicitly authorized promotion of the accepted current development state to `main` as Guiyuan 0.2.1.

Build 746 changes version/release metadata only. Its runtime code is the accepted Build 744 integration; rejected Build 745 remains closed and unmerged.

### Release boundary

- bump external version from 0.2.0 to 0.2.1;
- advance the internal Build identity to 746 so the rejected 745 Canary identity is never reused;
- add the dated 0.2.1 CHANGELOG section for the net accepted changes since 0.2.0;
- keep Build 744 device evidence applicable because no APK/runtime behavior changes are introduced by this checkpoint;
- require the normal dev-to-main Full stable-promotion validation before merge.

## 2026-10-07 — Build 746: runtime plumbing maintainability pass

**Type:** behavior-neutral maintainability / runtime plumbing
**Display version:** 0.2.1
**Build:** 746 / `20261006-746` unchanged
**Branch:** `refactor/runtime-plumbing`

### Why this batch exists

The previous cleanup reduced several long names and extracted some rules, but a second review still found concrete maintenance problems in SystemUI glue code: multiple booleans describing one lifecycle, one-line Policy wrappers around obvious expressions, duplicated sealed result hierarchies, success/failure wrappers with no useful success payload, very long plumbing names, and diagnostic fields that were hard-coded rather than observed.

This batch fixes those specific problems. It is not a general rewrite and does not continue splitting files just to make them smaller.

### Structural cleanup

- fold the full-AOD transition window and Keyguard boundary handoff into explicit local state instead of independent Pending/Active/Ready booleans;
- keep Home→AOD fallback flags separate where they are genuinely independent across different native callbacks;
- remove trivial presentation policies and inline obvious one-line conditions;
- simplify internal install/attach APIs to `String?` where callers only need success or a failure reason;
- merge the duplicate Control Center presentation result hierarchy into the existing shared presentation state result;
- remove the unused native combined-participant detach API and its unused result type;
- consolidate owned-list bookkeeping under one `OwnedEntries` helper;
- shorten probe/source/runtime names where the surrounding scope already provides the domain context, without keeping compatibility aliases for dead internal names.

### Diagnostics rule

Synthetic metrics are now explicitly forbidden by CONTRIBUTING. A diagnostic field presented as a metric, readiness input, health signal or observed value must come from a real runtime read or calculation.

The branch removes hard-coded self-proof fields such as `nativeGeometryWrites=0`, `suppressionWriters=0`, `hookDelta=0`, fixed `eventDriven/readOnly/stable/mainThread` tags, and similar detached/installed claims that were not backed by a measurement. Real geometry, hook counts, runtime state, failure reasons and protocol control fields remain.

### Review boundary

No display-version or Build bump is made. The accepted ownership model, native geometry authority, fail-native boundaries, transition timing and stable Build 746 identity remain the target behavior.

The full-AOD state refactor received a direct old-vs-new lifecycle review. One subtle missing-host difference was found during review and corrected before CI: when the Keyguard host cannot be resolved, the cached Home-at-start ownership snapshot is invalidated just as in the previous implementation.

### Closeout

- PR #248 passed exact-head Full CI #2879 at head `c5fe296`: target-profile verification, Kotlin compilation, unit tests, required APK variants, Modern Xposed metadata and non-debuggable validation all passed.
- Compiler warnings match the existing Build 746 baseline; this batch did not introduce a new warning class.
- PR #248 was squash-merged to `dev` as `6f7c3a1`.
- No Canary/device gate is required because the remaining questions were resolved by source-level lifecycle comparison and automated validation.
- `main` remains unchanged; version 0.2.1 and Build 746 are not bumped by this maintenance batch.

## 2026-10-07 — Build 746: close maintainability coverage gaps

**Type:** behavior-neutral maintainability / coverage-gap audit  
**Display version:** 0.2.1  
**Build:** 746 / `20261006-746` unchanged  
**Branch:** `refactor/maintainability-gap-audit`

### Coverage method

This pass starts from accepted `dev@50f5aa2` and uses maintenance-PR changed-files as the coverage map instead of scanning already-reviewed areas again. #217 and #228-#248 were unioned first, then merged functional PRs between those checkpoints were checked for later changes that could invalidate an earlier review.

The remaining uncovered set was 15 production Kotlin files plus 11 test/tooling/workflow entries. Intermediate feature/fix work did not expose an additional post-review gap: the affected Home/UI/target-profile paths were subsequently covered by later maintenance PRs.

### Confirmed gaps

- `SystemActiveSubscriptionSource` carried an unused `Snapshot + Authority + reason` shell even though every caller consumed only the nullable subscription-ID set. The source now returns that set directly. `null` still means the platform authority is unavailable; an empty set remains an authoritative no-active-subscription result.
- `SystemUiCompatibilityProbe.summary` unconditionally said `SystemUI ready` even on a partial structural match. The summary is now neutral; the actual diagnostic ready/unavailable state remains derived from the observed status-host marker.
- `NativeStatusBarSlotGeometryTest` had two equivalent cases, one named as if it proved transient battery expansion even though no battery-expansion input existed. The self-proof duplicate is removed.

### False positives retained

- `RootShell.Result` represents real timeout / exit / error outcomes rather than a success/failure wrapper.
- `CenterTransitionPolicy` protects a real three-family transition rule.
- `NativeWifiOpticalReferencePolicy.canShareReferenceViewport` is shared by two optical-geometry paths, so inlining it would duplicate the same invariant.
- render-latency samples, native-status inventory counts and runtime-health state are based on actual runtime timestamps/events/view scans, not synthetic metrics.
- workflow validation is based on real diffs, exact source SHAs, build outputs, signatures and metadata checks; no fixed health/readiness evidence was found.

### Boundary

No version or Build bump. No geometry constant, native writer, transition clock, ownership boundary, fail-native path or device-specific compensation is changed. One coherent Runtime CI checkpoint is sufficient unless it leaves a device-only uncertainty.

### Closeout

- PR #250 exact-head Runtime CI #2883 passed on `7ae45292`: target-profile verification, Kotlin compilation, unit tests, debug APK build and Modern Xposed metadata checks succeeded.
- PR #250 was squash-merged to `dev` as `8cf504c1`.
- Integrated `dev` Runtime validation #2884 passed the signed Canary path, including Haple signing, metadata and non-debuggable validation.
- No new warning class came from the touched files; existing warnings remain in previously reviewed runtime areas and are not reopened by this audit.
- No Canary/device gate is required beyond the automatic integrated-dev artifact because the batch is behavior-neutral and leaves no device-only engineering question.
- External version remains 0.2.1 and Build remains 746 / `20261006-746`.

