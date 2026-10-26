# OS4 interface conformance matrix (experimental)

Source of truth: compose-miuix-ui/miuix [PR #423](https://github.com/compose-miuix-ui/miuix/pull/423), pinned `b9f70838509fbc3f63f1a972777a7e6ce07f578f`. This inventory covers **visual and behavioral contracts**, not merely class names. The pending PR is not a published full dependency set.

| Area | Upstream OS4 contract | Guiyuan status and required gate |
| --- | --- | --- |
| Bottom destinations | `GlassNavigationBar`: minimum 54dp, **content-driven height**, equal-width destinations, fills the **caller-bounded width**, optional trailing action; outer insets belong to caller | App uses actual upstream component with documented 24dp horizontal and 12dp vertical margins. Review touch/drag, fontScale, 80–110% density and two-line labels; do not impose a fixed height/width. |
| Appearance mini-preview | Preview must respect the **same intrinsic height** as the component and reserve room for shadows | OS4 path now measures the real bar and reserves surrounding space instead of treating the old 76dp preview as authoritative; OS3 preview unchanged. |
| Top app bars | PR modifies core `TopAppBar` viewport clipping so floating action shadows extend past title region, and adds `BlurTopAppBar` with title blur and draw-phase opacity | Published `TopAppBar` still uses whole-region clipping. App uses a temporary native delegate and native top-bar IconButtons while incompatible. **Not compliant** until matching core UI revision is integrated. Avoid alpha, shadow clipping or padding patches. |
| Scroll/overlap | Glass title bar supports an explicit `isContentScrolled` input based on actual list position; scroll behavior retains title collapse | All four screen owners should supply their own `LazyListState.canScrollBackward` rather than infer overlap solely from collapsing title; keep list ownership local. |
| Search | PR #423 does not change `SearchBar` or `InputField`, but provides glass top-bar actions. The system screenshot uses a round graphical close affordance. | OS4 retains MIUIX SearchBar and round native actions while the published TopAppBar clips glass shadows. This is an explicit temporary fail-native boundary, not full GlassTopAppBar conformity; re-evaluate on the matched upstream UI revision. OS3 retains text cancel. Test search focus, Back, clear and layout. |
| Simple preferences | No PR-only replacements for the existing `SliderPreference`, `SwitchPreference`, or ordinary `BasicComponent` | Use native MIUIX, but review theme, state semantics, hit targets, row spacing, font size, dynamic type, animations and full UI density. |
| Select preferences | Official `GlassDropdownPreference` owns popup placement and host; a clean page backdrop must exclude the popup and scrim | Adapted through one shared selector route. Check keyboard, dismiss, RTL, touch, 80–110% density and host origin in-app. |
| Segmented tabs | `GlassSegmentedTabRow` for connected segments; `GlassTabRow` for separate pills, both with defined material/sizing | Sandbox retains glass segments only when full text fits equal-width slots and routes longer choices to its existing glass dropdown. **B1403 Sandbox crash** remains unproven without an app-process stack and current target-device retest. |
| Dialogs and input confirmations | `GlassDialog`, `GlassDialogButton`, `DialogSurfaceScope`, and modified `OverlayDialog` surface/animated scrim APIs; owner is Scaffold's `MiuixPopupHost` | Not integrated. Migrate input/settings, confirmation, reset and color dialogs only against the **same upstream UI revision**; preserve completion/dismiss/predictive Back and form focus. |
| Bottom sheets | `GlassBottomSheet`, `BottomSheetSurfaceScope`, modified overlay surface API; scroll, overscroll, inset and Back behavior is host-owned | Not integrated. Color editing sheets require coordinated upstream UI + glass package; don't wrap an existing sheet in a duplicate project-owned glass host. |
| Icon menus/popups | Modified `OverlayIconCascadingDropdownMenu`, cascading popup layout, official glass popup family | Not integrated. Battery-scheme icon menus need exact same-revision MIUIX preference/UI and anchoring/surface hierarchy; other menus are evaluated by semantic use. |
| Iconography | Separate `miuix-glass-icons` catalog from PR #423 | Adopt applicable control/navigation semantics at an exact published revision. Keep Material Symbols for explanatory leading icons, not an unreviewed bulk swap. |
| Full-screen UI scale | App modifies `LocalDensity` and `WindowInfo.containerDpSize` together; glass render samples must match the same geometry, with popup roots outside recorded content | Source architecture preserved; **device acceptance missing** for 80%, 100%, 110%, display cutouts, rotation, fontScale, IME, theme, and RTL. |
| Fail native / lifetime | Shader and host failures must preserve MIUIX's accessible native state, not produce invisible controls or stale popup captures | Explicit OS4 no-shader branch in main navigation and its Appearance mini-preview selects standard MIUIX `NavigationBar`, not the user's previously persisted OS3 floating style. Verify Sandbox, dialog and menu host teardown. |

## Design-first OS4 adaptation

For each UI surface, begin with the user task, semantic hierarchy, state and interaction;
then verify upstream OS4 visual, geometry, motion and accessibility contracts before
choosing a component. A renamed or glass-coated OS3 widget is not sufficient. Review
container constraints, system/window insets, typography and fontScale, dynamic density,
keyboard/Back, scrolling, overlay host ownership and shader-free fallback together.

- **Sandbox title:** OS4 must use the Scaffold's actual top-bar measurement for
  list content instead of the collapsed SmallTopAppBar height. OS3 retains its
  dock-independent pinned inset.
- **Sandbox choices:** segmented glass tabs remain for fitting short labels.
  Longer labels use the existing scaffold-hosted glass dropdown with full option
  text rather than squeezing equal-width segments or shrinking typography.
  Layout fit is measured with the active MIUIX text styles and font scale.
- **Top-bar actions:** old published TopAppBar clips the glass action-shadow
  drawing region. Feature search, detail back and Sandbox back/theme/scroll-top
  therefore retain native MIUIX IconButton surfaces until the matching
  upstream title-local clipping API is published. This is a fail-native
  compatibility boundary, not completed OS4 action styling.
  SearchBar, input, focus and Back remain MIUIX-owned.
- **Bottom navigation:** upstream 54dp is a minimum content height, not a
  certification of on-screen placement. Confirm icon/caption sizing, intrinsic
  height, status/navigation Insets, theme and 80–110% scale before modifying
  the caller's outer margins. Never fix perceived low placement with a
  screenshot-fitted translation.

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
| `miuix-blur/LayerBackdrop.kt` | With a downscale factor, PR now uses continuous `translate(-scaledX, -scaledY)`; published snapshot rounds coordinates to a two-texel grid and writes residuals. PR comment explicitly identifies discontinuous motion at boundaries. | `MainHub`'s `rememberLayerBackdrop` and `layerBackdrop` feed `GlassNavigationBar` while its `HorizontalPager` contains moving/scrolling pages. **High-confidence plausible contributor** to reported bottom glass flash; no frame trace yet. |
| `miuix-blur/DrawBackdropModifier.kt` | PR leaves the downsampling source `renderEffect` attached until the destination layer is rendered; published snapshot clears it immediately after recording a drawLayer reference. | Glass bar blur under scrolling may skip prefilter work. The PR correction must be adopted **in the module**, not by setting alpha on the app bar. |
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
| Home | `screens/HomeScreen.kt` | Glass title bar; hot-reload control stays native until busy/disabled/hold state has an exact OS4 counterpart; preview/dock remains consistent |
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
- [ ] Reproduce bottom-bar scroll flashing with screen capture / frame
  evidence and compare old versus revised **actual** Blur dependency; do not
  declare success from a cosmetic UI change.
- [ ] Collect the **app process** crash stack for Sandbox; the supplied
  SystemUI/LSPosed diagnostic is not the app exception. The sampling cycle
  was fixed on B1405 source, but that alone is not crash proof.
- [ ] Integrate matched `GlassDialog`, `GlassBottomSheet`, glass popups/menus,
  and suitable native icon family. Verify animated backdrop geometry and
  single popup host across overlay dismissal, nested scroll and predictive Back.
- [ ] Verify OS3 unchanged on the target, shader absence fail-native, RTL,
  typography, IME, density, dynamic theme, accessibility, and performance.

B1405 completed signed Canary unit/build checks; user-reported scrolling flash
and the B1403 Sandbox launch crash remain **device-open**. This matrix is
source-audited, not physical acceptance.
