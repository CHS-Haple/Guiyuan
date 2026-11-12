# MIUIX Glass integration

This folder documents the **active integration boundary and source
attribution**, not an alternative UI implementation or historical build log.
The exact accepted revision is defined by `gradle.properties` and the matching
source checkout in `.github/actions/miuix-source/action.yml`.

## Source and licensing

- Guiyuan's `dev` uses MIUIX `0.9.4-9d4b9c85-SNAPSHOT` at full revision
  `9d4b9c85c18707c7da12cf8c497ea57955fcf81e`.
  The prior Material integration is documented in
  [PR #1440](https://github.com/CHS-Haple/Guiyuan/pull/1440).
  The stable `main` line must be checked separately; do not treat a `dev`
  pin as a stable-release upgrade.
- `app/src/main/kotlin/top/yukonga/miuix/kmp/glass/` contains an adapted,
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

The pinned MIUIX `9d4b9c85` Maven artifacts expose `DialogSurfaceScope`,
`BottomSheetSurfaceScope` and `BlurTopAppBar`. Both former source choices
compiled against published Maven without composite-source substitution in
[PR #1521](https://github.com/CHS-Haple/Guiyuan/pull/1521). The obsolete
source forks and `gy.os4.dialogSurfaces` property are retired. The one
`app/src/main/kotlin` implementation keeps Glass Dialog/Sheet in OS4 UI
style and MIUIX-native Overlay hosts in OS3 UI style. Exact-source checkout
and independent published-Maven compilation remain separate verification
paths, not separate compiled source trees.

## Material model ownership

The pinned MIUIX Material graph owns the canonical shape, style, colour layers,
material, shadow and stroke presets. `GlassMaterialBridge.kt` delegates the
existing call sites to published upstream Material definitions and functions.
`GlassStroke.kt` aliases the upstream `EdgeStroke` and `GlassEdgeStrokes`.
The app renders strokes, rim, masks and shadows via Material; the accepted
#479 fused edge pass runs only when shading, positive stroke width and additive
blend mode allow it. App-specific backdrop refraction/color blending and
popup/Back lifecycles stay locally owned. Do not copy upstream shader sources
back into this package or add a second Material model.

## Live app-specific boundaries

- Keep the native popup and transformed-anchor Back/dismiss lifecycle,
  single-owner backdrop/scrim and any necessary host/Sheet separation.
  `GlassIconButton` has Guiyuan-used long-press, hold-down and enabled APIs;
  upstream source equivalence alone cannot replace them.
- `UiNativeTitleBar` retains the accepted `BlurTopAppBar` (now available
  through pinned published MIUIX), title-local clipping and measured unclipped
  navigation/action controls. The old `TopAppBar` fallback is retired.
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

Dialog, BottomSheet, title and edge drawing now use one published MIUIX API
contract. Remaining local source implements adapted backdrop/refraction and
Guiyuan-specific UI and lifecycle ownership.
A whole-module `miuix-glass-android` swap must atomically remove conflicting
same-package local declarations; linking it alongside the local bridge causes
duplicate classes and can expose `internal` visibility/ABI failures. Do not
modify the upstream checkout during CI to bypass these issues.

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
