# OS4 interface conformance matrix (experimental)

Source of truth: compose-miuix-ui/miuix [PR #423](https://github.com/compose-miuix-ui/miuix/pull/423), pinned `b9f70838509fbc3f63f1a972777a7e6ce07f578f`. This inventory covers **visual and behavioral contracts**, not merely class names. The pending PR is not a published full dependency set.

| Area | Upstream OS4 contract | Guiyuan status and required gate |
| --- | --- | --- |
| Bottom destinations | `GlassNavigationBar`: minimum 54dp, content-driven height, equal-width destinations, caller-bounded width and caller-owned Insets | OS4 + selected Glass uses upstream capsule; other selected styles and floating disabled preserve existing behavior across UI styles. Icon-only and icon+text remain user controlled. Upstream page 24dp/20dp/Inset spacing is retained. Device acceptance still open. |
| Appearance mini-preview | Preserve intrinsic sizing, appearance selections and no input for display-only navigation | OS4 Glass preview is noninteractive at the component's gesture and accessibility boundaries, not just a no-op onSelect. Both UI styles preview the persisted navigation style and content mode. |
| Top app bars | PR changes `TopAppBar` to title-local clipping and adds `BlurTopAppBar` | Published core still clips all children. Temporary adapter measures each real action group, reserves that group’s actual width/height in the native slot, then paints the interactive controls once above the legacy clip. Verify multi-action navigation rows, dynamic search entry and fontScale on device. Full title-blur conformance still blocked on matched upstream UI. |
| Scroll/overlap | Glass title bar supports an explicit `isContentScrolled` input based on actual list position; scroll behavior retains title collapse | All four screen owners should supply their own `LazyListState.canScrollBackward` rather than infer overlap solely from collapsing title; keep list ownership local. |
| Search | MIUIX SearchBar/InputField remain native; glass top-bar icon actions are OS4 | Restores floating OS4 glass search circle in parent top-bar action layer while native measured action slot and search/Back/focus behavior remain unchanged. OS3 retains text cancel. Test no clipping and expand/collapse transitions. |
| Simple preferences | No PR-only replacements for the existing `SliderPreference`, `SwitchPreference`, or ordinary `BasicComponent` | Use native MIUIX, but review theme, state semantics, hit targets, row spacing, font size, dynamic type, animations and full UI density. |
| Select preferences | Official `GlassDropdownPreference` owns popup placement and host; a clean page backdrop must exclude the popup and scrim | Adapted through one shared selector route. Check keyboard, dismiss, RTL, touch, 80–110% density and host origin in-app. |
| Segmented tabs | `GlassSegmentedTabRow` for connected segments; `GlassTabRow` for separate pills, both with defined material/sizing | Sandbox retains glass segments only when full text fits equal-width slots and routes longer choices to its existing glass dropdown. **B1403 Sandbox crash** remains unproven without an app-process stack and current target-device retest. |
| Dialogs and input confirmations | `GlassDialog`, `GlassDialogButton`, `DialogSurfaceScope`, and modified `OverlayDialog` surface/animated scrim APIs; owner is Scaffold's `MiuixPopupHost` | Not integrated. Migrate input/settings, confirmation, reset and color dialogs only against the **same upstream UI revision**; preserve completion/dismiss/predictive Back and form focus. |
| Bottom sheets | `GlassBottomSheet`, `BottomSheetSurfaceScope`, modified overlay surface API; scroll, overscroll, inset and Back behavior is host-owned | Not integrated. Color editing sheets require coordinated upstream UI + glass package; don't wrap an existing sheet in a duplicate project-owned glass host. |
| Icon menus/popups | Official `GlassPopupItem`, group dividers and secondary cascading panels, with a root popup host | Battery flat actions keep their root-hosted glass popup; diagnostics uses a shared root-hosted OS4 two-level glass menu, existing `DropdownEntry` selection/callback data and official 0.5dp group lines. OS3/no-shader retains native MIUIX. Because matched PR #423 UI is unpublished, both implementations remain experimental pending tap, Back, nested layer, RTL and device checks. |
| Iconography | Separate `miuix-glass-icons` catalog from PR #423 | Adopt applicable control/navigation semantics at an exact published revision. Keep Material Symbols for explanatory leading icons, not an unreviewed bulk swap. |
| Full-screen UI scale | App modifies `LocalDensity` and `WindowInfo.containerDpSize` together; glass render samples must match the same geometry, with popup roots outside recorded content | Source architecture preserved; **device acceptance missing** for 80%, 100%, 110%, display cutouts, rotation, fontScale, IME, theme, and RTL. |
| Fail native / lifetime | Shader and host failures must preserve MIUIX's accessible native state, not produce invisible controls or stale popup captures | OS4 Glass with no shader renders the native MIUIX `NavigationBar` in both MainHub and Appearance preview; other user-selected floating styles keep their original renderer. Verify device failure path and modal host teardown. |

## Design-first OS4 adaptation

For each UI surface, begin with the user task, semantic hierarchy, state and interaction;
then verify upstream OS4 visual, geometry, motion and accessibility contracts before
choosing a component. A renamed or glass-coated OS3 widget is not sufficient. Review
container constraints, system/window insets, typography and fontScale, dynamic density,
keyboard/Back, scrolling, overlay host ownership and shader-free fallback together.

- **Sandbox title and transient dock:** Both OS3 and OS4 use their native
  TopAppBar without transient `bottomContent`. Upstream measures `bottomContent`
  into the top-bar height, which Scaffold uses as the list's top inset: animating
  that slot causes page displacement. The list consumes Scaffold's measured inset
  for correct expanded-title clearance; a sibling overlay is positioned at that
  same measured boundary without contributing to the header's height. Keep the
  overlay outside the page's `LayerBackdrop` recording to avoid glass
  self-sampling; its OS4 action samples that page-only backdrop explicitly. The
  dock visibility threshold remains relative to the list's `beforeContentPadding`
  and its animation never changes list geometry. Verify scroll continuity and
  input/hit regions on device. OS4 uses the same 12dp end inset as the glass
  top-bar action; OS3 retains native 16dp. The upstream OS4 `BlurTopAppBar`
  title-clip contract is still unpublished.
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
- Require a single exact published upstream MIUIX core/UI/blur/nav/preference/glass/icons revision and proper release review before removing vendor shims or claiming full OS4 support. This PR remains Draft.

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
shadows are not a valid single-owner solution.

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

- [ ] Obtain and review an exactly versioned published **whole MIUIX module graph**
  with PR #423's UI, Blur, Preference and Glass interfaces (or have upstream
  merge/release them). Stop using the temporary top-bar compatibility delegate.
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
