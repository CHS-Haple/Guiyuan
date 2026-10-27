# OS4 interface conformance matrix (experimental)

Source of truth: compose-miuix-ui/miuix [PR #423](https://github.com/compose-miuix-ui/miuix/pull/423), pinned `b9f70838509fbc3f63f1a972777a7e6ce07f578f`. This inventory covers **visual and behavioral contracts**, not merely class names. The pending PR is not a published full dependency set.

| Area | Upstream OS4 contract | Guiyuan status and required gate |
| --- | --- | --- |
| Bottom destinations | `GlassNavigationBar`: minimum 54dp, content-driven height, equal-width destinations, caller-bounded width and caller-owned Insets | OS4 + selected Glass uses upstream capsule; other selected styles and floating disabled preserve existing behavior across UI styles. Icon-only and icon+text remain user controlled. Upstream page 24dp/20dp/Inset spacing is retained. Device acceptance still open. |
| Appearance mini-preview | Preserve intrinsic sizing, appearance selections and no input for display-only navigation | OS4 Glass preview is noninteractive at the component's gesture and accessibility boundaries, not just a no-op onSelect. Both UI styles preview persisted navigation style and content mode. OS4 display-only native floating items disable presses and child accessibility actions rather than merely using no-op callbacks; OS3 retains its existing behavior. Validate press and gesture paths on device. |
| Top app bars | PR changes `TopAppBar` to title-local clipping and adds `BlurTopAppBar`; native titlePadding stays explicit | Published UI clips all children. Local GlassTopAppBar adapter retains native title/scroll layout and user action slots; B1483 forwards the caller's `titlePadding` through both overloads to the published native TopAppBar (Diagnostics explicitly requests 0dp). Matching upstream UI/Blur is still needed to remove the clip workaround and gain large-title blur. Verify multi-actions, search, fontScale and title geometry on-device. |
| Scroll/overlap | Glass title bar supports an explicit `isContentScrolled` input based on actual list position; scroll behavior retains title collapse | All four screen owners should supply their own `LazyListState.canScrollBackward` rather than infer overlap solely from collapsing title; keep list ownership local. |
| Search | PR #423 sample `GlassSearchOverlay` uses the official 12dp horizontal bar insets, a 44dp field, a 54dp end reservation (10dp gap + 44dp cancel), 24dp search/clear targets, a 16dp clear glyph and 400ms mode geometry | B1482 uses a dedicated OS4 BasicTextField content and retains native SearchBar Back ownership; B1485 restores exactly the upstream row width budget while OS3 preserves native SearchBar/InputField margins. Shared solid/rest vs scrolled-glass material is a Guiyuan experiment (the upstream sample's search pills are solid). Typing/clear/Back/IME/RTL, moving-text flicker and visual edge quality remain unverified on-device. |
| Simple preferences | PR #423 does not replace the published `SliderPreference`, `SwitchPreference`, `BasicComponent`, `TextField` or native checkbox controls | Retain the MIUIX widgets on OS4 unless upstream changes their behavior, typography or token contracts; audit pressed/disabled/summary layout and fontScale for Features (17 sliders, 13 switches), Preview Sandbox (4 sliders, 3 switches), Appearance and dialogs. Do not create glass variants merely because the outer OS4 shell changed. |
| Select preferences | Official `GlassDropdownPreference` owns popup placement and host; a clean page backdrop must exclude the popup and scrim | Adapted through one shared selector route. Check keyboard, dismiss, RTL, touch, 80–110% density and host origin in-app. |
| Segmented tabs | `GlassSegmentedTabRow` for connected segments; `GlassTabRow` for separate pills, both with defined material/sizing | Sandbox retains glass segments only when full text fits equal-width slots and routes longer choices to its existing glass dropdown. **B1403 Sandbox crash** remains unproven without an app-process stack and current target-device retest. |
| Dialogs and input confirmations | `GlassDialog` depends on the **matched** `DialogSurfaceScope`, host `OverlayDialog.surface`, animated scrim/scale and `ScrimBackdrop`; one root `MiuixPopupHost` owns dismissal | **12 current `OverlayDialog` call sites remain legacy shell**, despite role-correct `GlassDialogButton` actions: MainHub (3), HubScreens (4), BatteryColorEditor (2), BatteryColorControls (1), IntegerInputDialog (1), AboutScreen (1). Need a clean backdrop that excludes popup/scrim; MainHub's bottom-nav-only recorder does not yet prove full-page coverage. Preserve onDismissFinished restart handoff, input validation, queued import/reset/delete and Back/focus. Do not wrap old dialog in another modal host. |
| Bottom sheets | PR #423 adds `GlassBottomSheet`, `BottomSheetSurfaceScope` and the new `OverlayBottomSheet.surface` contract for drag, nested scroll, overscroll, IME and back | **2 `OverlayBottomSheet` calls in BatteryColorControls** still use published native sheets; main/detail visibility and detail-clear `onDismissFinished` must retain single ownership. Convert only with matched UI/Blur sources and a proven clean page recorder, never with a standalone blur card. |
| Icon menus/popups | Official `GlassPopupItem`, group dividers and secondary cascading panels, with a root popup host | Battery flat actions keep their root-hosted glass popup; diagnostics uses a shared root-hosted OS4 two-level glass menu, existing `DropdownEntry` selection/callback data and official 0.5dp group lines. OS3/no-shader retains native MIUIX. Because matched PR #423 UI is unpublished, both implementations remain experimental pending tap, Back, nested layer, RTL and device checks. |
| Shared moving-text blur | PR #423 `miuix-blur/DrawBackdropModifier.kt` retains a downsample prefilter through deferred layer rendering and `LayerBackdrop.kt` fixes coordinate quantization | Same visible flicker reported behind OS4 bottom bar **and** search capsule, so investigate common page recorder/Blur module before changing unrelated controls. Complete published/PR423 Android source graphs compile in isolation and jointly with Guiyuan. Parallel debug APK A/B builds (distinct app IDs, no Haple signing) are being tested separately; **physical causality not established**. |
| Iconography | PR #423 has a dedicated `miuix-glass-icons` family, including `Regular.Back` with RTL auto-mirror | B1484 places the exact reviewed Apache-2.0 Regular.Back vector inside OS4 Settings/Sandbox top-bar GlassIconButtons, leaving OS3 Back icons, onClick, accessible labels and tooltips unchanged. Search/Close remain scoped compatibility assets; delete these glyphs once matched official glass-icons can be linked. Do not vendor the complete unpublished icon catalog. Device appearance remains pending. |
| Full-screen UI scale | App modifies `LocalDensity` and `WindowInfo.containerDpSize` together; glass render samples must match the same geometry, with popup roots outside recorded content | Source architecture preserved; **device acceptance missing** for 80%, 100%, 110%, display cutouts, rotation, fontScale, IME, theme, and RTL. |
| Fail native / lifetime | Shader and host failures must preserve MIUIX's accessible native state, not produce invisible controls or stale popup captures | OS4 Glass with no shader renders native MIUIX `NavigationBar` in MainHub and the matching noninteractive mini-preview, including the saved icon-only display choice. Other user-selected floating styles keep their original renderer. Verify device fallback, accessibility and modal host teardown. |

## Design-first OS4 adaptation

For each UI surface, begin with the user task, semantic hierarchy, state and interaction;
then verify upstream OS4 visual, geometry, motion and accessibility contracts before
choosing a component. A renamed or glass-coated OS3 widget is not sufficient. Review
container constraints, system/window insets, typography and fontScale, dynamic density,
keyboard/Back, scrolling, overlay host ownership and shader-free fallback together.

- **Sandbox title, fade and transient dock:** Both OS3 and OS4 retain MIUIX's
  native TopAppBar measurements and avoid transient `bottomContent` height in
  Scaffold padding. The interactive dock belongs to Scaffold's native
  above-top-bar popup-host slot, positioned at the measured native bar bottom;
  retain exactly one native `MiuixPopupHost()` after the dock so dialogs/menus
  remain higher in drawing order. No duplicated dock or backdrop capture cycle.
  Dock animation height feeds only the visual continuation: OS3 extends its
  existing progressiveTextureBlur draw region with the native no-layout-growth
  technique demonstrated in upstream GlassPage; OS4 extends the same
  GlassTopAppBar band via its public `bandOverhang`, without creating a second
  gradient writer. Header/scroll measurement remains stable; the popup slot
  keeps the dock touchable even below the measured header. Verify popup layering,
  input, scroll continuity and fade in both UI eras on device. OS4 still uses
  12dp action insets; OS3 retains native 16dp. Unpublished `BlurTopAppBar`
  and matched MIUIX Blur source remain separate acceptance requirements.
- **B1476 pinned-graph OS4 search glyph compatibility:** Upstream PR #423 GlassSearchOverlay uses MiuixIcons.Glass.Regular.Search/Close vector paths for input, clear text and cancel. The released b587aaf2 miuix-core has no `MiuixIcons.Glass` namespace and miuix-glass-icons is not published alongside the five pinned modules. Port only the two exact Apache-2.0 upstream Regular paths (with copyright/SPDX) into a tiny local search-only compatibility source. Wire Glass.Search to the OS4 action and input leading slot and Glass.Close to the OS4 trailing text-clear (16dp, native InputField spacing and fade) and outside cancel (24dp). Preserve single query writer, focus/IME and exit owner, and entire OS3 basic search/clear/text-cancel branch. Delete the compatibility source and import native icon module when its complete matched release exists. No new graphics shader or icon path approximation.
- **B1480 OS4 search surface / dimensions (historical, input superseded by B1482):** Upstream PR #423's `GlassSearchOverlay` draws the idle search input and 44dp cancel icon as `surfaceContainerHigh` capsules; the published native `InputField` used in B1480 had a 45dp minimum. The previous B1475 local adaptation incorrectly put a live glass shader on the idle search capsule, washing out its surface even when no content passed underneath, and gave the cancel control a different solid treatment. Keep the native SearchBar host, but after B1482 the OS4 input uses its own 44dp BasicTextField and OS3 retains InputField; use **one** pill surface policy for both the input and cancel circle: solid `surfaceContainerHigh` at the top, and the stock pured-thin glass material with a small bloom border only once the underlying list has scrolled (`canScrollBackward`). Both fall back to the same solid native fill without an effective backdrop. Keep 12dp action margin, glass Regular search/clear/close glyphs, TooltipBox, query/focus/IME, and OS3 basic search untouched. This is a Guiyuan extension of the upstream *solid* example to reproduce the reported scrolled-glass state, **not** a claim that upstream publishes a glass SearchBar. Verify stroke, dark color, scrolling transitions, keyboard and Back on-device; no claim of flicker repair.
- **B1481 top-bar search geometry:** The initial OS4 adaption installed `SearchBar` in `GlassTopAppBar.bottomContent`, forcing it below the large title and changing Scaffold's measured top-bar height on expansion. Upstream PR #423's search mode draws the search input at the status-bar inset over the title area, with the underlying bar fading. In OS4 only, retain the existing one `GlassTopAppBar` layout owner, animate its title alpha, and paint the native `SearchBar` as a sibling aligned at the status-bar inset rather than `bottomContent`. Do not add an extra popup host or change query, selection/focus, Back, results scroll-position or OS3 route. The original search `AnimatedVisibility` keeps the exit transition mounted. Native example transition geometry and physical motion/blurring still need A/B calibration; a correct source structure is not visual acceptance.
- **B1482 OS4 filled-search conformance:** A 24dp `IconButton` inside the published `InputField` still receives that control's default 40dp minimum hitbox, making text, clear glyph and outer cancel geometries inconsistent; the legacy `InputField` also suppresses its label whenever expanded, unlike PR #423's BasicTextField decoration. Implement just the OS4 field content against upstream `GlassSearchOverlay`'s 44dp/24dp/16dp structure, retaining `SearchBar` ownership for Back/dismiss, `PageSearch` as the sole query writer, the single parent-provided native/glass surface and unchanged OS3 `InputField`. The clear control explicitly opts into 24dp minimum width/height; it keeps focus on clearing. Search input/completion/empty, focus/IME, accessibility, RTL, dark contrast and blur motion still require device acceptance.
- **B1483 cross-control title-padding contract:** `SettingsPage(titlePadding = 0.dp)` was honored only in its OS3 `SmallTopAppBar` branch (Diagnostics) and ignored in OS4's locally vendored `GlassTopAppBar`. The published `TopAppBar` as well as PR #423's `BlurTopAppBar` both accept the `titlePadding` parameter. Thread the existing caller's value through both GlassTopAppBar overloads and the native title renderer, with the upstream default retained for unaffected consumers. No optical offsets, custom title draw, extra layout state or divergence of the two bar overloads. CI compilation and device title alignment are separate validation gates.
- **B1485 OS4 search outer geometry:** Published `SearchBar` inserts a symmetric `InsideMargin = DpSize(12.dp, 0.dp)`; merely painting PR #423's icons and 44dp pills retained the correct 12dp outer inset but also a 12dp gap before cancel instead of the upstream 10dp gap. The upstream `GlassSearchOverlay` draws one row inside `GlassTopAppBarDefaults.HorizontalPadding` (12dp per side), and reserves precisely `54dp` at the end: a 44dp round cancel plus 10dp gap. B1485 preserves the native SearchBar as the single Back host, but for OS4 only sets that internal symmetric margin to zero, applies the upstream 12dp parent inset, and puts the 10dp gap in the cancel slot; the resulting width budget matches the source example with no hand-tuned optical offsets. B1482's 44dp field, clear hit target and input focus remain unchanged; OS3 retains the 12dp native SearchBar margin and text Cancel. The animation timings still differ from PR #423; physical keyboard/RTL and glass-edge appearance remain unverified. This corrects an erroneous initial B1485 review statement about 24dp: the actual official `HorizontalPadding` constant is 12dp in both the vendored and upstream PR #423 sources; the code always referenced this constant and did not adopt 24dp.
- **B1473 dialog action policy — partial:** OS4 uses the existing upstream GlassDialogButton roles (normal, primary, destructive), preserving disabled/action/press semantics. OS3 keeps its original MIUIX TextButton visual treatment. The underlying OverlayDialog/OverlayBottomSheet remains the *released MIUIX host*, not GlassDialog/GlassBottomSheet: the published snapshot has no `surface` slot with animated scrim/scale, so glass panel blur, predictive-Back sampling and gesture-matched sheets cannot yet conform to PR #423. Never replace these with an extra popup host, a static background, or a fake scrim. Retain original per-screen onDismiss/onDismissFinished, input, and operation sequencing until a reviewed same-revision source-built or published module graph is integrated and device-tested.
- **Sandbox OS3 network selector:** The network selector lives outside its Card; the published MIUIX `TabRowWithContour` defaults its track to `surface`, the same as the page background, so its outer silhouette disappears. Only the outside-Card OS3 track now uses the official `TabRowDefaults.tabRowColors(backgroundColor = surfaceContainerHighest)` color slot; the selected surface, native contour geometry, motion and semantics remain untouched. OS3 selectors inside cards and all OS4 segmented glass tabs are unchanged. Confirm light/dark device contrast.
- **Sandbox choices:** segmented glass tabs remain for fitting short labels.
  Longer labels use the existing scaffold-hosted glass dropdown with full option
  text rather than squeezing equal-width segments or shrinking typography.
  Layout fit is measured with the active MIUIX text styles and font scale.
- **Top-bar actions:** the experimental GlassIconButton has enabled and holdDownState
  compatibility parameters for Home's long-running action; these must be
  reconciled with upstream before removing the vendor sources.
  Published TopAppBar still clips every child, unlike PR
  #423's title-only clip. Native TopAppBar owns slot measurement, scrolling
  and title geometry; its action slots are noninteractive size placeholders.
  The real OS4 glass controls are composed once above the legacy clip in the
  parent glass bar; their measured size also determines the native inert slot
  so multi-button navigation rows cannot overlap the adjacent title. Its
  `boundsInRoot` / `positionInRoot` coordinates are physical, so both unclipped
  overlay positions use `absoluteOffset` instead of layout-direction-relative
  `offset`; otherwise RTL mirrors the x displacement twice. Remove this adapter
  after upgrading the complete upstream UI revision. Rounded button
  interaction/animation remains pending device acceptance.
- **Navigation configuration:** On the first B1464 appearance read, a one-time
  DataStore migration selects OS4 for **all** existing users, including those with
  a previously saved OS3 choice. A persisted migration marker prevents a later
  restart or collection from overriding the user's subsequent OS3 choice.
  Existing navigation enablement, material, content and liquid preferences remain
  intact. A genuinely empty appearance store additionally seeds enabled Glass
  (shown as Soft Glass) with icon+text. Both UI eras retain independently
  selectable navigation modes. Preview selection is inert even for glass drag gestures.
- **Bottom navigation:** 54dp is the upstream glass capsule's minimum
  content height, not the bottom placement. The upstream full GlassPage uses
  24dp horizontal and 20dp top margins, with bottom clearance
  max(24dp, navigationBarInset + 8dp). The main hub now follows that
  layout contract directly instead of layering 12dp over navigationBarsPadding;
  the Appearance preview uses matching 20/24dp vertical rhythm. This
  is a source-conformant choice, not physical-device acceptance. Confirm
  icon/caption intrinsic sizing, theme, density/fontScale, and gesture areas
  before declaring the bottom bar finished.

## Battery custom menu acceptance boundary

The scheme menu is a flat list of actions; its OS4 route uses MIUIX's
root-hosted `PopupLayout` and the vendored GlassDropdownPopupContent
(which already models enter/exit and Back). Rename and copy form one group;
destructive delete is separated using GlassPopupDefaults' official inset,
spacing and 0.5dp line. This popup intentionally uses the upstream opaque
fallback when a clean page Backdrop is unavailable inside the overlaid
battery sheet; do not claim sampled translucent glass until host integration
is proven. The old OverlayIconDropdownMenu stays for OS3/no-shader.
The source-screen pager still owns only the trigger, not the popup.

## Diagnostic glass menu acceptance boundary

The Diagnostics top bar keeps share/export enabled state and tooltip semantics, but
all OS4 actions now use official GlassIconButton, including the two report
actions and the cascading-menu trigger. SettingsPage opts this populated
right-side action row into the native-slot/glass-parent drawing boundary;
pages with no actions reserve no empty 44dp placeholder.

The Diagnostics filter menu keeps its existing level/category toggle callbacks and does not
collapse when a selection changes. The other menu retains mode, scrolling and clear
callbacks and closes after an action. OS4 uses the official glass dropdown primary
and secondary popup presentation inside one Scaffold-owned popup host; OS3 and
devices without a usable shader still use the original Window cascading menu.
All group boundaries use GlassPopupDefaults' 20dp inset, 7.75dp spacing and
0.5dp divider rather than reusing OS3 separator styling. The host survives both closing animations and frees its anchor state on
disposal. The secondary panel handles Back while expanded. During its exit, the primary
Back handler becomes active to prevent a second fast Back from reaching page
navigation, but its dismiss callback is inert until the rendered secondary
panel actually unmounts. The outside-tap catcher follows the same rule.
No duplicate close or page navigation is initiated while secondary exits. This is source-only adaptation:
real-device gesture/Back dispatch, window overlap, 80–110% density, RTL, focus
and themes still require confirmation.

## Current evidence and unresolved diagnostics

- B1403 signed Canary compiled and passed configured tests, but physical device evidence reveals Sandbox launch crash, cropped top-bar action edge, a fixed-height mini-preview defect and unacceptable perceived bottom-bar proportions.
- The supplied `Guiyuan-Diagnostic-20261026-1403-20261026-182500.txt` is a **SystemUI/LSPosed** report: it cannot establish the app-process crash stack or exonerate the Compose glass route.
- Current `main` published MIUIX snapshots don't provide PR #423's modified basic UI/overlay host contract. A green APK using copied glass source and older basic UI does **not** constitute OS4 conformance.
- Read app crash stack from `com.chaners.guiyuan` first; then isolate the corresponding component in the Sandbox Composable and apply a root-cause fix with proper ownership rather than catching UI rendering exceptions.
- Require a single ABI-compatible MIUIX core/UI/blur/nav/preference/glass/icons module graph before removing vendor shims or claiming full OS4 support. An exact-revision source-built graph is permissible for an independently reviewed OS4 experiment; publication remains the preferred distribution path. A green source build or an isolated copied base-module class cannot close this gate. This PR remains Draft.

## Proven upstream API and source chain

The following is a **revision-to-revision source comparison**, not a claim of
target-device verification. The app's pinned published snapshot is
`0.9.4-b587aaf2-SNAPSHOT` (not PR #423). The stable `dev` snapshot is
`0.9.4-8972f941-SNAPSHOT`, also missing the PR-only glass contracts.

| Producer / contract | PR #423 source evidence | App consumer and consequence |
| --- | --- | --- |
| `miuix-blur/LayerBackdrop.kt` | With a downscale factor, PR now uses continuous `translate(-scaledX, -scaledY)`; published snapshot rounds coordinates to a two-texel grid and writes residuals. PR comment explicitly identifies discontinuous motion at boundaries. | This is a **secondary** hypothesis for the reported title-text flicker: MainHub's glass navigation bar and layer recorder stay in place while the text scrolls *inside* the recorded page, so their relative sampling origin may stay fixed. Re-evaluate if a trace proves coordinate movement. |
| `miuix-blur/DrawBackdropModifier.kt` | PR leaves the downsampling source `renderEffect` attached until the destination layer is rendered; published snapshot clears it immediately after recording a `drawLayer` reference. | **Primary source-backed hypothesis** for moving dark title glyphs flashing through the fixed Soft Glass bottom bar: skipping the expected downsampling prefilter can amplify edge aliasing as glyphs move. The stock 20dp material selects a downscaled blur path on ordinary-density screens; causality still needs an exact-version physical A/B. Fix upstream in the matched Blur module, not with glass opacity or text-specific offsets. |
| `miuix-ui/basic/TopAppBar.kt` | PR removes whole-large-title bar `clipToBounds()`, adds title-local clipping, and provides `BlurTopAppBar`. `SmallTopAppBar` retains its existing clip: the contract is **component-specific**. | App `GlassTopAppBar` currently delegates to a published old `TopAppBar`. Floating action shadow clipping and title blur cannot satisfy PR design until the UI ABI moves in lockstep. |
| `miuix-ui/layout/DialogContentLayout.kt`, `DialogSurfaceScope.kt`, `overlay/OverlayDialog.kt` | Adds a drawable surface slot, animated scrim/scale state and host-controlled geometry. | `MainHub`, `HubScreens`, `BatteryColorEditor`, `BatteryColorControls`, `IntegerInputDialog`, `AboutScreen`. Do not replace dismissed/queued action semantics with a separate overlay. |
| `miuix-ui/layout/BottomSheetContentLayout.kt`, `BottomSheetSurfaceScope.kt`, `overlay/OverlayBottomSheet.kt` | Adds replacement surface slot synchronized to nested scroll, drag, back progress and overscroll surface. | `BatteryColorControls` (two sheets). Require same-revision host API and background sampling excluding sheet/scrim. |
| `miuix-ui/layout/CascadingListPopupLayout.kt`, `overlay/OverlayCascadingListPopup.kt`, `miuix-preference/menu/*CascadingDropdownMenu.kt` | Adds shape/surface hook and revised hierarchy/morph geometry; exposes popup clipping modifier. | `DiagnosticsScreen`, `BatterySchemePages` icon menus and shared dropdowns. Audit `Window` vs `Overlay` ownership before migration. |
| `miuix-glass-icons` / PR additions to MIUIX icon declarations | New icon family (not an alternative for every existing descriptive Material Symbol). | Select semantic navigation, back, search, menu and closing icons as a coordinated same-revision update; don't vendor the entire catalog blindly. |

None of these unpublished base-module changes can be installed merely by
copying `miuix-glass` Kotlin files into an app that still links against the
older binary UI and Blur modules. Same-package duplicates or private API
shadows are not a valid single-owner solution. Their early adoption is
permitted only through an isolated, source-audited replacement of a complete
compatible module graph, with exact source provenance, CI and focused
physical-device comparison; see README.md.

## Application-wide screen and control inventory

| Screen / owner | Current code path | OS4-specific review |
| --- | --- | --- |
| Main hub | `ui/MainHub.kt` | Glass 3-destination navigation, Scaffold-owned bottom insets, shader fallback, pager sampling, dialogs/snackbar layering and fast page switches |
| Home | `screens/HomeScreen.kt` | Glass title bar and rounded Hot Reload action. The experimental GlassIconButton supports the existing disabled/hold state so it cannot repeatedly hot-reload while busy; the native action path remains OS3. |
| Features + Settings hub | `screens/HubScreens.kt` | Glass title/search action, round outside-end search close, sixteen-plus sliders and switch rows stay MIUIX absent a changed base norm; review four `OverlayDialog` sites |
| Detail pages | `screens/SettingsScreenLayout.kt`, `screens/StaticScopeScreen.kt`, `screens/AboutScreen.kt` | Back/action glass, title scroll state, official Insets and title clipping, long text wrapping and window-safe popup roots |
| Appearance | `screens/AppearanceScreen.kt` | Intrinsic OS4 navigation preview height, retained OS3 configuration, density and no-shader standard-nav preview |
| Sandbox | `screens/PreviewSandboxScreen.kt` | Independent top bar/backdrop ownership, segmented tabs using their own opaque-base source (no self-sampling), page state does not write runtime settings; launch crash still needs the app-process exception |
| Battery scheme + editor | `screens/BatterySchemePages.kt`, `screens/BatteryColorEditor.kt`, `screens/BatteryColorControls.kt`, `screens/IntegerInputDialog.kt` | Popup menu ownership, two sheets, color and numeric input dialogs; preserve parse/validation/cancel/commit callbacks |
| Diagnostics | `screens/DiagnosticsScreen.kt` | Window-hosted icon cascading menus, raw vs localized values, export and truncation behavior |
| Shared preference selectors | `screens/UiDropdownPreference.kt` | Popup is not a descendant of its own sampled backdrop; accepted selected index and selection writer still belong to the original repo |

## Required next conformance checkpoint

- [ ] Review a whole, exactly revision-matched **MIUIX module graph** with
  PR #423's UI, Blur, Preference and Glass contracts: either official
  published binaries or a thoroughly reviewed source-built set from an
  uncredentialed environment. Do not mix PR-only base classes with the
  existing binary snapshot. Remove the temporary top-bar delegate only
  when API, lifecycle and visual parity are verified.
- [ ] Re-run the 4 independent page hosts with glass action shadows, search,
  tab state and color-scheme changes at fontScale/density 80% / 100% / 110%.
- [ ] Reproduce dark section-title glyph flicker while those glyphs scroll
  behind the fixed Soft Glass navigation bar. Compare the pinned and matched
  revised **actual** Blur implementations on device; distinguish missing
  downsample prefiltering from moving sampling origin. Ignore normal slider
  hue changes and do not declare success from cosmetic UI changes.
- [ ] Collect the **app process** crash stack for Sandbox; the supplied
  SystemUI/LSPosed diagnostic is not the app exception. The sampling cycle
  was fixed on B1405 source, but that alone is not crash proof.
- [ ] Integrate matched `GlassDialog`, `GlassBottomSheet`, glass popups/menus,
  and suitable native icon family. Verify animated backdrop geometry and
  single popup host across overlay dismissal, nested scroll and predictive Back.
- [ ] Verify OS3 unchanged on the target, shader absence fail-native, RTL,
  typography, IME, density, dynamic theme, accessibility, and performance.

B1470 passed exact-source signed Canary tests and APK checks; the reported
bottom-bar glyph flicker, Sandbox motion and any app-process crash reproduction
remain **device-open**. This matrix is source-audited, not physical acceptance.
