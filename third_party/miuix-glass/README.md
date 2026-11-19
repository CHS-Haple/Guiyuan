# MIUIX Glass integration

This folder documents the **active integration boundary and source
attribution**, not an alternative UI implementation or historical build log.
The exact accepted revision is defined by `gradle.properties` and the matching
source checkout in `.github/actions/miuix-source/action.yml`.

## Source and licensing

- The active source and Maven graph uses the successfully published MIUIX
  snapshot `0.9.5-5ff41ba9-SNAPSHOT` at full revision
  `5ff41ba90d963e48c3db1f3b28eb8dbebc9391f9`. The upstream Maven publish step succeeded in
  [run #44571884911](https://github.com/compose-miuix-ui/miuix/actions/runs/44571884911).
  Upstream `v0.9.5-beta01` is a pre-release, not a `v0.9.5` stable
  release. The exact source-pinned snapshot is identified separately by
  upstream's `0.9.5-<short SHA>-SNAPSHOT` Maven convention.
  The `5ff41ba9` integration follows upstream system-bars title insets,
  conditional idle backdrop capture, and horizontal bottom-bar safe insets;
  it also consumes upstream progressive-blur and scroll behavior refinements
  through the matched dependency graph. Guiyuan retains its adapted glass,
  search, navigation and input ownership. Source-Maven parity and focused
  device validation are required before accepting this candidate.
  The prior Material integration is documented in
  [PR #1440](https://github.com/CHS-Haple/Guiyuan/pull/1440).
  The stable `main` line must be checked separately; do not treat a `dev`
  pin as a stable-release upgrade.
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
`app/src/main/kotlin` implementation keeps Glass Dialog/Sheet in OS4 UI
style and MIUIX-native Overlay hosts in OS3 UI style. Exact-source checkout
and independent published-Maven compilation remain separate verification
paths, not separate compiled source trees.

## Material model ownership

The pinned MIUIX Material graph owns the canonical shape, style, colour layers,
material, shadow and stroke presets. Adapted Glass components import published MIUIX Material shape, style,
stroke and shadow definitions directly; local Material type/factory bridges
and stroke aliases have been retired.
The app renders strokes, rim, masks and shadows via Material; the accepted
#479 fused edge pass runs only when shading, positive stroke width and additive
blend mode allow it. Refraction now embeds upstream `MaterialAgsl` silhouette
and rim fragments and delegates shape uniforms to `setSilhouetteUniforms`;
no local copy of either shared implementation remains. App-specific backdrop
refraction/color blending and popup/Back lifecycles stay locally owned. Do not
copy shared shaders into the app or add a second Material model.

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
- Preserve the original Battery Colors outer/inner MIUIX `Card` hierarchy,
  native `surface/onSurface` color pair, default inner Card material and
  regular `GlassIconButton` / popup shadow pairing.
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
Navigation destinations and `GlassNavigationBarDefaults` now also use the upstream public types; local navigation/rail still owns only Guiyuan-specific gestures, geometry and hit-area extension.
The identical `GlassNavigationRailDefaults` object is likewise taken from upstream; app-specific rail state remains local because it uses a private morph clock and supports a trailing action absent from the public upstream Rail API.
The public glass surface and constant-alpha shadow modifiers now resolve to MIUIX's published API; only app-specific action-bar underlay and state-driven shadow helpers remain local.
Integer and scheme-rename dialogs use published `GlassTextField` in OS4, retaining native `TextField` for OS3. Card-embedded numeric/hex fields keep their existing non-Glass presentation.
The remaining app-local Glass files preserve the accepted navigation/rail,
noninteractive preview and touch exclusion, controlled search, popup anchor
and Back lifecycle, long-press/held-state button, title slots and custom
refraction/material ownership where upstream does not expose an equivalent
public entrypoint. These are app-specific adapters, not a second complete MIUIX
module implementation.
The work-branch trial links the published `miuix-glass-android` module and
source-substitutes it from the same pinned upstream revision, after relocating
adapted local components to `com.chaners.guiyuan.ui.glass`. This resolves
same-package JVM class collisions but does not itself prove public API parity,
correct upstream runtime behavior, or safe removal of app-owned interaction
and lifecycle implementations. Do not modify upstream source in CI.

For later upstream alignment, preserve adapted backdrop/refraction and
popup, Back/scrim and navigation ownership. Validate both source-matched and
published-Maven builds; for rendering changes review OS3/OS4 UI styles,
light/dark rims, alpha transitions, title clipping and dismissal on device.
CI alone is not device acceptance.

Dated integration evidence is in
[PR #1440](https://github.com/CHS-Haple/Guiyuan/pull/1440),
[PR #1384](https://github.com/CHS-Haple/Guiyuan/pull/1384),
[PR #1370](https://github.com/CHS-Haple/Guiyuan/pull/1370) and repository
history. The previous long-form conformance/audit investigations remain
accessible in Git history, not in active documentation.

The accepted `d27cbb0b` baseline already brought progressive blur/graphics-layer resource gating and drag badge clipping. The `5ff41ba9` candidate retains those contracts and adds the source-matched inset and idle-capture adjustments described above. Source and Maven parity must pass before any device checkpoint.
