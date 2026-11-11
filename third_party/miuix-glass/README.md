# MIUIX Glass integration

This folder documents the **active integration boundary and source
attribution**, not an alternative UI implementation or historical build log.
The exact accepted revision is defined by `gradle.properties` and the matching
source checkout in `.github/actions/miuix-source/action.yml`.

## Source and licensing

- Guiyuan's `dev` uses MIUIX `0.9.4-96d613aa-SNAPSHOT` at full revision
  `96d613aa42c485463c596886a2433a61c1bd7d6f`.
  The prior Material integration is documented in
  [PR #1440](https://github.com/CHS-Haple/Guiyuan/pull/1440).
  The stable `main` line must be checked separately; do not treat a `dev`
  pin as a stable-release upgrade.
- `app/src/main/kotlin/top/yukonga/miuix/kmp/glass/` contains an adapted,
  app-compiled subset of the upstream
  [MIUIX Glass implementation](https://github.com/compose-miuix-ui/miuix/pull/423).
  Its earlier source was reconciled at `45645854cd46c32275c5666e3aafa9980d8f104e`;
  the dependency graph later advanced to `96d613aa`. Some declarations are
  intentionally app-specific or from intermediate upstream commits.
  **This is not a byte-for-byte copy of the complete upstream module.**
- Reused and adapted MIUIX source is Apache-2.0. Preserve upstream SPDX and
  copyright headers, [the third-party notices](../NOTICE.md)
  and this folder's [license](LICENSE). Do not silently copy a new upstream
  revision into local packages while retaining an older dependency graph.
- MIUIX background effects are separately sourced; see
  [miuix-effect](../miuix-effect/README.md).

## Single published-API source

The pinned MIUIX `96d613aa` Maven artifacts expose `DialogSurfaceScope`,
`BottomSheetSurfaceScope` and `BlurTopAppBar`. Both former source choices
compiled against published Maven without composite-source substitution in
[PR #1521](https://github.com/CHS-Haple/Guiyuan/pull/1521). The obsolete
source forks and `gy.os4.dialogSurfaces` property are retired. The one
`app/src/main/kotlin` implementation keeps Glass Dialog/Sheet in OS4 UI
style and MIUIX-native Overlay hosts in OS3 UI style. Exact-source checkout
and independent published-Maven compilation remain separate verification
paths, not separate compiled source trees.

## Material model ownership

The source-matched and published MIUIX Material graph provides the canonical
Glass shape, style, colour layers, material, shadow and preset definitions.
`GlassMaterialBridge.kt` preserves local Kotlin source call sites through
typealiases and delegates the two shape factories and blur combination to the
published upstream API. Guiyuan keeps its adapted `GlassStroke`/shader and
popup/Back lifecycle; these are not falsely claimed to use the upstream fused
rim drawing pass. Do not introduce another app-owned copy of Material models.

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

Dialog, BottomSheet and title now use one source tree backed by published
MIUIX APIs. Future consolidation concerns local Glass shaders and strokes.
A whole-module `miuix-glass-android` swap must atomically remove conflicting
same-package local declarations; linking it alongside the local bridge causes
duplicate classes and can expose `internal` visibility/ABI failures. Do not
modify the upstream checkout during CI to bypass these issues.

For later Glass renderer consolidation, preserve the owned shader, popup,
Back/scrim and navigation contracts. Verify both source-matched and published
Maven builds, OS3/OS4 UI styles, RTL, font scale, title clipping and dismissal
before grouped signed-device acceptance. CI alone is not device acceptance.

Dated integration evidence is in
[PR #1440](https://github.com/CHS-Haple/Guiyuan/pull/1440),
[PR #1384](https://github.com/CHS-Haple/Guiyuan/pull/1384),
[PR #1370](https://github.com/CHS-Haple/Guiyuan/pull/1370) and repository
history. The previous long-form conformance/audit investigations remain
accessible in Git history, not in active documentation.
