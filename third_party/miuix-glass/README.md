# MIUIX Glass integration

This folder documents the **active integration boundary and source
attribution**, not an alternative UI implementation or historical build log.
The exact accepted revision is defined by `gradle.properties` and the matching
source checkout in `.github/actions/miuix-source/action.yml`.

## Source and licensing

- The work-branch candidate targets `0.9.5-0485ae81-SNAPSHOT` at
  `0485ae81bc26d8df0a189a110e63f4f75c3fb5b8`.
  Upstream [Publish #46154551491](https://github.com/compose-miuix-ui/miuix/actions/runs/46154551491)
  completed successfully, including the Android Symbol publication. The
  candidate remains subject to independent published-Maven and exact-source
  Guiyuan CI plus focused device acceptance. The pre-migration dev baseline
  is `bc1024d78a154f5167efff262ae5ed66264c481e`; a newer main commit without
  matching publication is not an interchangeable artifact.
- Use published `miuix-icons-symbol-android` for MIUIX navigation and action
  glyphs. The former regular and Glass icon artifacts and namespaces are
  removed upstream. Render through `rememberSymbolPainter`, using
  `toImageVector` only for APIs that require a vector; the old local Glass
  aliases, fixed-canvas test and Rail toggle vector copies are retired.
  Embedded Material Symbols used for semantic settings-row icons are separate.
- Consume the public backdrop underlay API for capture-only backgrounds, keeping
  the same surface/host owner. Public shape mask classification is used in the
  retained action-bar pipeline; its local RenderEffect caches are removed so
  the upstream node can recreate effects on resource invalidation. Progressive
  blur uses upstream's current curve and scrim without the old extra tint.
  Popup constraints are measured against the same window/host safe area used
  for placement. Predictive Back rebound follows the visible gesture fraction.
  Constant shadow alpha uses the public upstream shadow modifier.
- The public top-bar defaults are used directly. The remaining app-specific
  underlay, deferred-alpha shadow, unclipped title/action slots, held and
  long-press interactions, local popup host exit and Rail/Liquid ownership
  are retained because their matching upstream hooks are internal or absent.
- `app/src/main/kotlin/com/chaners/guiyuan/ui/glass/` contains an adapted,
  app-compiled subset of the upstream
  [MIUIX Glass implementation](https://github.com/compose-miuix-ui/miuix/pull/423).
  Its earlier source was reconciled at `45645854cd46c32275c5666e3aafa9980d8f104e`;
  the dependency graph later advanced to `9d4b9c85`. Some declarations are
  intentionally app-specific or from intermediate upstream commits.
  **This is not a byte-for-byte copy of the complete upstream module.**
- Reused and adapted MIUIX source is Apache-2.0. Preserve upstream SPDX and
  copyright headers, [the third-party notices](../NOTICE.md)
  and this folder's [license](LICENSE). Do not silently copy a new upstream
  revision into local packages while retaining an older dependency graph.
- MIUIX background effects are separately sourced; see
  [miuix-effect](../miuix-effect/README.md).

## Single published-API source

The pinned MIUIX snapshot Maven artifacts expose `DialogSurfaceScope`,
`BottomSheetSurfaceScope` and `TopAppBarLayout`. Both former source choices
compiled against published Maven without composite-source substitution in
[PR #1521](https://github.com/CHS-Haple/Guiyuan/pull/1521). The obsolete
source forks and `gy.os4.dialogSurfaces` property are retired. The one
`app/src/main/kotlin` implementation uses published Glass Dialog/Sheet;
MIUIX owns their native Overlay hosts and fallback material. Exact-source checkout
and independent published-Maven compilation remain separate verification
paths, not separate compiled source trees.

## Material model ownership

The pinned MIUIX Material graph owns the canonical shape, style, colour layers,
material, shadow and stroke presets. Adapted Glass components import published MIUIX Material shape, style,
stroke and shadow definitions directly; local Material type/factory bridges
and stroke aliases have been retired.
The app renders strokes, rim, masks and shadows via Material; the accepted
#479 fused edge pass runs only when shading, positive stroke width and additive
blend mode allow it. Refraction is owned by the published MIUIX implementation.
The app-local action-bar underlay pipeline has no shading/refraction caller;
its unused refraction shader and uniform bindings are retired. It retains
only the colour-blend shader for the app-specific two-layer underlay, with
published Material stroke and mask drawing. Popup/Back lifecycles stay locally
owned. Do not copy shared shaders into the app or add a second Material model.

## Live app-specific boundaries

- Keep the native popup and transformed-anchor Back/dismiss lifecycle,
  single-owner backdrop/scrim and any necessary host/Sheet separation.
  `GlassIconButton` has Guiyuan-used long-press, hold-down and enabled APIs;
  upstream source equivalence alone cannot replace them.
- `UiNativeTitleBar` uses the upstream `TopAppBarLayout` with native title
  blur, retaining accepted title-local clipping and measured unclipped
  navigation/action controls. The old parallel fallback is retired.
- Search top-bar fade/scale/blur now uses the published MIUIX
  `rememberGlassSearchBarAlpha` and `glassSearchBarTransition` functions directly;
  the duplicate app-local copy is retired. `PageSearch` retains query and result ownership,
  while its own search geometry still uses the source-matched easing curve. Native
  `TopAppBarLayout` still clips actions, so the measured outer shadow slot
  remains until upstream provides an equivalent unclipped API.
- Navigation retains noninteractive preview behavior, touch-safe bottom
  extension and exact native icon/material semantics. Do not replace a
  caller-owned touch or animation boundary with a similar-looking upstream
  sample. Glass icon canvas and displayed navigation icon size differ.
- Battery Colors preserves its outer/inner card hierarchy in Glass sheets,
  using upstream `GlassCard` translucent fill and 20 dp corner without custom
  alpha. Card-embedded actions retain native IconButton; progressive Sheet
  header actions inherit upstream material and shadow. Preserve existing
  popup and floating-action shadow behavior.
- Prefer native MIUIX progress, material, shape and motion; do not introduce
  ad-hoc geometry, z-order, shader, seam or delayed-animation workarounds.
  Reproduce an actual deviation before changing native ownership.

## Migration and verification

The published MIUIX Glass module is now linked directly. The app uses its
`GlassDialog`, `GlassBottomSheet`, `glassDialogSurface`, `glassBottomSheetSurface`,
`GlassDropdownPreference`, `GlassDialogButton`, `GlassMotion`, `glassPressScale`,
and `glassPanel` APIs instead of maintaining corresponding component copies.
Its published material, edge and shader APIs also remain authoritative.
The preview sandbox's segmented Glass tabs use the published upstream `GlassSegmentedTabRow` and defaults; the app-local `GlassTabRow.kt` source copy is retired.
The scroll-safe segmented tab adapter in `ui/glass/ScrollAwareGlassSegmentedTabRow.kt` now
owns only the X/Y gesture arbitration needed for scrollable containers. In the pinned
upstream `a09d4f32`, `GlassTabRow.kt` claims a drag by X touch-slop without first
excluding a dominant vertical swipe; current upstream main has the same recognizer.
Guiyuan still delegates all material, indicator motion, selection and accessibility
to the published native Glass tab. The screen-only gesture duplicate is removed.
On each MIUIX revision update, check this recognizer and exercise vertical,
horizontal, diagonal and RTL gestures; when the upstream component fixes axis
arbitration, delete the adapter's pointer overlay without modifying its callers.
The standard basic contour TabRow, intentional 2D color picker, and the pager
header indicator are different gesture owners and are not patched speculatively.
Navigation destinations and `GlassNavigationBarDefaults` now also use the upstream public types; local navigation/rail still owns only Guiyuan-specific gestures, geometry and hit-area extension.
The identical `GlassNavigationRailDefaults` object is likewise taken from upstream; app-specific rail state remains local because it uses a private morph clock and supports a trailing action absent from the public upstream Rail API.
The public glass surface and constant-alpha shadow modifiers now resolve to MIUIX's published API; only app-specific action-bar underlay and state-driven shadow helpers remain local.
Integer and custom-scheme create/rename dialogs use published `GlassTextField` directly. Scheme names use its native placeholder mode; card-embedded HEX/RGB fields in the battery color Glass sheet use the same glass input defaults with persistent channel labels. Keep existing input validation and keyboard behavior.
Local Glass rendering retains Guiyuan's action-bar underlay and reusable
edge/rim/mask brush caches from the upstream Material APIs. Colour-blend
RenderEffects are rebuilt by the upstream blur node on state/resource
invalidation rather than cached independently by the app-local pipeline. Local button press feedback uses the upstream
`MiuixIndication`, with explicit Guiyuan long-press, disabled and held-state
bridges. The transformed popup anchor tracks attached Compose coordinates via
upstream-style Modifier Nodes and releases them on detach; obsolete local
dropdown-value anchor state and fading code was removed. The local navigation
bar scopes pressed-item state to each item while preserving Guiyuan's
touch-extension and pointer selection owner.

The remaining app-local Glass files preserve the accepted navigation/rail,
noninteractive preview and touch exclusion, controlled search, popup anchor
and Back lifecycle, long-press/held-state button, title slots and action-bar
underlay colour blending where upstream does not expose an equivalent
public entrypoint. These are app-specific adapters, not a second complete MIUIX
module implementation.
The work-branch trial links the published `miuix-glass-android` module and
source-substitutes it from the same pinned upstream revision, after relocating
adapted local components to `com.chaners.guiyuan.ui.glass`. This resolves
same-package JVM class collisions but does not itself prove public API parity,
correct upstream runtime behavior, or safe removal of app-owned interaction
and lifecycle implementations. Do not modify upstream source in CI.

For later upstream alignment, preserve adapted backdrop/underlay and
popup, Back/scrim and navigation ownership. Validate both source-matched and
published-Maven builds; for rendering changes review relevant navigation choices,
capability fallbacks, light/dark rims, alpha transitions, title clipping and
dismissal on device.
CI alone is not device acceptance.

Dated integration evidence is in
[PR #1440](https://github.com/CHS-Haple/Guiyuan/pull/1440),
[PR #1384](https://github.com/CHS-Haple/Guiyuan/pull/1384),
[PR #1370](https://github.com/CHS-Haple/Guiyuan/pull/1370) and repository
history. The previous long-form conformance/audit investigations remain
accessible in Git history, not in active documentation.

The prior B1893 Glass alignment replaced the obsolete approximate
`(barHeight + 20.dp) * 1.2f` band geometry with the measured title height
plus the configured gradient overhang, matching upstream's correction.
Adapted internal popup, navigation and rail layouts use upstream-aligned
Compose fast rounding/clamping utilities. The new blur shader, kernel,
noise-layer and padding fixes are consumed from published `miuix-blur`,
not copied into the app.

The 28d45984 alignment also makes the app-local Glass cascading menu skip
secondary backdrop recording and popup composition when no submenu is active,
retaining submenu content through its exit animation. The local top-bar
fallback caches its fixed gradient shader like upstream; its own title and
unclipped action slots remain Guiyuan-owned. These source adaptations and
published Android artifact compilation require separate Guiyuan CI verification.

Battery Colors uses upstream `GlassCard` for its glass-sheet content groups,
preserving the outer/inner card hierarchy, slot-row actions, and add-card
click feedback. The 20 dp GlassCard corner and translucent
fill are upstream defaults, not local custom material settings; this scoped
UI change still requires light/dark and editor-sheet device acceptance.

## B1920 source-pinned private API and resource review

The custom-scheme **More** action inside the outer GlassCard now uses
published MIUIX `basic.IconButton` instead of a floating glass button.
The native 40 dp transparent circle retains app-owned popup anchors and
their Back lifecycle without a second rim/shadow. The card hierarchy,
blue Apply action and actual native capability fallback are unchanged.

The upstream `c83aacd8..28d45984` change set was reviewed against the
22 live app-local Glass Kotlin adapters. The updated indication, menu
recording, popup Modifier Nodes, drawing caches, top-bar fade, and per-item
pressed-state changes are already included. Two additional applicable
private-source changes were ported: the selected navigation indicator's
first-layout bounds before coroutine settlement and the glass shadow's
ShaderBrushCache for deferred alpha. Upstream's public stroke-provider
overloads remain in the library; the app-specific action-bar underlay does
not acquire another duplicate without a real caller.

The separate experimental MIUIX effect PR #321 was closed unmerged on 2026-11-28 at
`73bdd49b4ea965dc7822e276b0a6ce29a5564f54`; all seven
`app/src/main/kotlin/top/yukonga/miuix/kmp/effect/bg/` files were compared
against that exact PR source. Four match byte-for-byte. The Background and
Painter differences are required imports from published MIUIX Material,
and the Modifier difference is Guiyuan's documented color animation
restart/cancellation fix. The library remains unpublished and must not
be replaced with a nonexistent Maven dependency. See
`third_party/miuix-effect/README.md`.

MIUIX icons now resolve through the published variable Symbol module. Native
settings-row Material Symbols remain separate. Non-public Rail, popup and
native-title behavior stays adapted only where no behavior-equivalent public
entrypoint is available.

## Upstream alignment and optional API decisions

- **Adopt:** MIUIX owns Slider tap-to-seek, ColorPicker
  bounds, Squircle's drawing and zero-radius fast paths, TopAppBarState
  observation and navigation runtime fixes. No app-local reimplementation.
  The About title follows example commit `84ce2a0a`: threshold 0.45 and
  200 ms tween instead of the old continuous scroll ramp. Keep the existing
  title-only alpha boundary in SettingsPage and all hero/background motion.
  Native OverlayBottomSheet uses SheetMotion.Default, GlassBottomSheet uses
  SheetMotion.Glass internally (#496); do not add a redundant motion setting
  or a second scrim/drag clock.
- **Adopt:** the controlled search field uses public SearchClearButton
  and keeps query/focus ownership and a localized accessibility label. The
  adapted transformed menu carries its glass anchor shadow receipt and reads
  shadow alpha during drawing. It calculates settled placement once, with
  window-safe horizontal alignment. The host already mounts on demand and
  retires after both menu exits; the upstream lazy initial mount does not
  justify a second visibility owner.
- **Adopt scoped horizontal alignment:** the diagnostics filter is in
  the leading navigation slot, so use Start; trailing More and scheme
  management stay End. Carry upstream's alignment through the adapted primary
  menu into its one settled placement, preserving safe-area clamping and RTL.
  Secondary menus retain their native row geometry. Shader-unavailable hosts
  retain the native menu; published dropdown preferences retain their value anchor.
  Center is supported by the same upstream-aligned policy, with no current
  caller or additional setting.
- **Adopt scoped route Expansion:** only Home's preview
  sandbox Card binds AppRoute.PreviewSandbox via public navTransitionSource,
  the actual CardDefaults corner and surfaceContainer colour, and stable
  sourceKey. The destination uses the upstream per-entry Expansion preset;
  the Scaffold's normal opaque surface and NavDisplayEffects supply the
  container. Upstream owns capture, shape, predictive Back and missing-source
  fallback. No custom navigation clock, source registry, or change to the
  preview lease/theme lifetime. All other routes and onboarding retain their
  existing transitions. Device-test same-window capture, missing/restored
  source, swipe/Back cancellation, theme switching and lease cleanup.
  Align ordinary navigation with the example's multi-window-only corner
  disabling; retain onboarding's existing no-corner policy.
- Icon updates (XiaomiListen/CheckSelection and private backing names) are
  consumed through upstream dependencies; the app's regular glass icon
  accessors use unrelated names. Example desktop packaging, example icon
  lists and baseline-profile changes are not app source migrations.
  Experimental effect PR #321 and resume PR #461 are not in the pinned
  published revision and are not copied into the app.

- **Adopt 03d15786 overlay lifecycle:** native Dialog, Sheet, and dropdown
  touch release and exit motion come from the matched dependency graph.
  The app-local transformed popup uses upstream's visible bounds/center
  threshold instead of waiting for the spring tail. Its page-owned anchor
  receives the closing position and velocity for the remaining rebound.
  The root-hosted popup transfers that rebound only when leaving the active
  effect; retiring the inactive host must not restart it. The controlled
  search row follows upstream's 1/255 visible-progress gate. Keep the
  existing submenu, Back, query, material and capability-fallback owners.
  No extra timer, input lock or animation is introduced. The nullable
  dropdown completion callback does not remove any caller-owned retirement
  obligation. The separate resume and effect proposals remain deferred.

- **Adopt a09d4f32 Sheet header and material defaults:** consume the new public
  `GlassBottomSheet` header padding inside each actual scroll container.
  The battery overview places its existing dot rail in `headerBottomContent`,
  shares the pager and keyed page scroll states for the native header's
  `scrollOffset`, and retains the deck until upstream dismissal completes.
  The detail and agreement sheets keep one scroll container each. The detail
  sheet enables the progressive header and shares its actual request-scoped
  ScrollState with the header and scrollbar. Its public GlassIconButton
  inherits the header's backdrop and floating material; retain local
  top-bar context only where its internal motion, touch or hold contracts
  remain necessary. No second blur engine, custom alpha or new preference.
  The updated top-bar BandOverhang and GlassCard, GlassTextField and
  GlassDialogButton defaults come from the dependency. Predictive Back
  reset and upward overscroll surface fixes are upstream-owned.

- **Align later top-bar and Sheet behavior:** the local GlassTopAppBar follows the
  upstream one-pass fixed-fade shader (removing the split draw and
  translation), bounded collapse offset and progressive scrim. Retain
  measured native title/compact slots and existing backdrop ownership.
  The shared GlassBottomSheet wrapper explicitly selects a
  non-overlay default, now matching the pinned upstream default. The battery overview and long-form Terms/Privacy
  readers explicitly opt into progressive blur and apply header padding
  to their scroll content and native scrollbar. The long battery detail
  editor also uses that progressive header contract, sharing the live editor
  scroll offset rather than accumulating another scroll position. The pinned
  `0485ae81` half-sheet example uses ordinary IconButton for fixed headers;
  the progressive example uses GlassIconButton with inherited material.
  No additional local Sheet effect is needed. The new GlassScrollableTabRow
  is an independent scrolling pill row, not a replacement for fixed
  segmented tabs. No current caller warrants migration; the segmented
  recognizer still checks X touch slop alone, so its axis adapter remains.
  Library baseline profiles are now supplied by the pinned MIUIX graph;
  do not copy generated example profiles into Guiyuan.

- **Dropdown scroll indicators:** upstream `588d029f` provides a non-interactive,
  non-fading indicator for scrollable menus. The local shared
  `GlassPopupSurface` mirrors its 16 dp vertical track padding, using the
  same ScrollState for the indicator and scrolling. Both transformed and
  secondary popups inherit this surface. Published GlassDropdownPreference,
  ListPopupColumn and overlay/window dropdown dialogs inherit their native
  indicators directly; no second indicator is added. Page scrollbars retain
  their interactive defaults and existing lazy-list stability policy.

### Prior a09d4f32 local-boundary review

The remaining 29 app-local Glass Kotlin files at a09d4f32 included implementation fragments
that matched that upstream revision apart from package/import adaptation (popup
Back progress, popup column, shader, drag target and secondary popup). Their
internal entrypoints are not callable through the published dependency. Do not
replace them with reflection or a second implementation. Top-app-bar public
defaults are already delegated; its private band brush and unclipped slots
remain coupled to the local measured layout. The controlled search cannot be
replaced wholesale by the upstream overlay, which owns and clears its own
query state. FloatingToolbar/FloatingActionButton do not replace the shared
navigation action slot with reload enabled/held state, touch-exclusion and
liquid press linkage. No additional relevant, behavior-equivalent published
replacement was identified in this review. Revisit these boundaries when the
upstream API or the actual caller requirements change.
