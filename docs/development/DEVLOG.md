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
- Visual preference changes ask the existing `SystemUiHomePresentationOwner` to resync its reservation; no second padding/translation writer is added.

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
- The temporary review attempt to use `SystemUiNativeNetworkSuppressionOwner.activeManager` for an arbitrary final group was rejected because that manager belongs to the Home status-bar host, not the independent QS/QS_FAKE icon group.
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
