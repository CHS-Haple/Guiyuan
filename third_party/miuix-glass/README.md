# MIUIX Glass integration

This folder documents the **active integration boundary and source
attribution**, not an alternative UI implementation or historical build log.
The exact accepted revision is defined by `gradle.properties` and the matching
source checkout in `.github/actions/miuix-source/action.yml`.

## Source and licensing

- Guiyuan's `dev` uses MIUIX `0.9.4-ca674992-SNAPSHOT` at full revision
  `ca674992c9645395cdb0d5109d66504ff53d018c`
  ([integration PR #1440](https://github.com/CHS-Haple/Guiyuan/pull/1440)).
  The stable `main` line must be checked separately; do not treat a `dev`
  pin as a stable-release upgrade.
- `app/src/main/kotlin/top/yukonga/miuix/kmp/glass/` contains an adapted,
  app-compiled subset of the upstream
  [MIUIX Glass implementation](https://github.com/compose-miuix-ui/miuix/pull/423).
  Its earlier source was reconciled at `45645854cd46c32275c5666e3aafa9980d8f104e`;
  the dependency graph later advanced to `ca674992`. Some declarations are
  intentionally app-specific or from intermediate upstream commits.
  **This is not a byte-for-byte copy of the complete upstream module.**
- Reused and adapted MIUIX source is Apache-2.0. Preserve upstream SPDX and
  copyright headers, [the third-party notices](../../THIRD_PARTY_NOTICES.md)
  and this folder's [license](LICENSE). Do not silently copy a new upstream
  revision into local packages while retaining an older dependency graph.
- MIUIX background effects are separately sourced; see
  [miuix-effect](../miuix-effect/README.md).

## Why the source variants exist

`app/build.gradle.kts` selects **exactly one** Kotlin source directory using
`-Pgy.os4.dialogSurfaces`:

| Compiled source | Dependency contract | Actual implementation |
| --- | --- | --- |
| `src/os4Upstream/kotlin` (true) | Exact-revision MIUIX source checkout supporting `DialogSurfaceScope` and `BottomSheetSurfaceScope` | OS4 `GlassDialog` / `GlassBottomSheet`, `BlurTopAppBar`; OS3 UI style retains native Overlay hosts |
| `src/os4Published/kotlin` (false) | Published MIUIX Maven API, without those source-only surface slots | Native `OverlayDialog` / `OverlayBottomSheet`, `TopAppBar` fallback |

Normal source-matched `dev` CI and Release compilation select `true`;
separate compatibility checks exercise `false` and the published artifacts.
These directories are **compile-time alternatives**, not two copies packaged
into the same APK, two installable channels or separate HyperOS 3/4 runtime
support. The `OS3` selector is a visual style only.

## Live app-specific boundaries

- Keep the native popup and transformed-anchor Back/dismiss lifecycle,
  single-owner backdrop/scrim and any necessary host/Sheet separation.
  `GlassIconButton` has Guiyuan-used long-press, hold-down and enabled APIs;
  upstream source equivalence alone cannot replace them.
- `UiNativeTitleBar` protects the compact title, title-layer clipping and
  unclipped measured navigation/action controls. The published `TopAppBar`
  and source `BlurTopAppBar` have different clipping/API contracts.
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

The long-term target is **one** stable public-API implementation when the
published MIUIX graph fully covers the accepted OS4 Glass behavior.
A whole-module `miuix-glass-android` swap must atomically remove conflicting
same-package local declarations; linking it alongside the local bridge causes
duplicate classes and can expose `internal` visibility/ABI failures. Do not
modify the upstream checkout during CI to bypass these issues.

Before removing the compile-time fallback or app-local bridge:

1. Verify official published artifacts, full pinned revision and public APIs;
   confirm the exact `GlassDialog`/`GlassBottomSheet` surface and title
   contracts, dependency graph, source attribution and licensing.
2. Run matched-source and ordinary published-Maven CI; review popup Back,
   scrim, dialog/Sheet dismissal, keyboard, native fallback, navigation hit
   zones, title clipping, light/dark, OS3/OS4 **UI styles**, RTL and font scale.
3. For behavior-visible changes, obtain a single grouped signed candidate and
   maintainer device feedback before merging. An upstream PR or successful
   source build alone is not acceptance.

Dated integration evidence is in
[PR #1440](https://github.com/CHS-Haple/Guiyuan/pull/1440),
[PR #1384](https://github.com/CHS-Haple/Guiyuan/pull/1384),
[PR #1370](https://github.com/CHS-Haple/Guiyuan/pull/1370) and repository
history. The previous long-form conformance/audit investigations remain
accessible in Git history, not in active documentation.
