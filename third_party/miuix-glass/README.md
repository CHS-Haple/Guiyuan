# Experimental MIUIX OS4 Glass — upstream-first integration

Source PR: https://github.com/compose-miuix-ui/miuix/pull/423
Investigated PR revision: `b9f70838509fbc3f63f1a972777a7e6ce07f578f`
License: Apache-2.0 (retain source headers and LICENSE).

## Adoption gate (hold this Draft PR)

PR #423 was open and unmerged at the 2026-10-26 review. The source
contains its own `miuix-core`, `miuix-ui`, `miuix-blur`,
`miuix-preference`, `miuix-glass` and `miuix-glass-icons` changes.
Its latest Android CI and other upstream test jobs were green, but upstream
review and official publication are separate gates. There is no matching
official main-canary or released Maven artifact for this PR revision.

**Next development order:**
1. Follow PR #423 through code review, merge and the official publisher.
   Check that *all* affected MIUIX module artifacts resolve from one exact
   published revision, including `miuix-glass-android` and
   `miuix-glass-icons-android`.
2. Replace all temporary `top.yukonga.miuix.kmp.glass` copies and the
   documented published-snapshot shims with that one consistent upstream
   version. Remove the copied-code notice only after the sources are gone.
3. Expand Guiyuan OS4 mode with upstream-native components where the
   semantics match: home navigation, hub/detail top bars, icon actions,
   selectors/tabs, dropdowns/menus, dialogs, sheets and available OS4
   controls. OS3 remains unchanged, including persisted preferences.
4. Review all page structures, menu/overlay host ownership, scaling
   (80%-110%), layout direction, insets, themes, accessibility, animation,
   shader fallback and lifecycles. Validate Gradle CI and focused device
   behavior before marking #861 ready or merging to dev.

Do **not** run build scripts from an unmerged third-party fork as an included
build in Guiyuan's credentialed signing workflows. Do not mix PR-only
glass components with published older UI modules and then conceal API drift
using copied overlays, private API shadows, hand-written effects or geometry
patches. A green experimental APK compile is not OS4 conformance.

## Current experimental snapshot (B1405, not release-ready)

- GlassNavigationBar on the main three-page navigation and its preview.
- GlassTopAppBar / GlassIconButton in hub/detail layouts, and OS4
  GlassTopAppBar on independent Home and Preview Sandbox scaffolds.
  The Home hot-reload action stays native MIUIX while its disabled and
  in-flight states lack exact parity in GlassIconButton.
- GlassDropdownPreference / popup host for shared selector preferences.
- GlassSegmentedTabRow in existing Sandbox segmented choices uses a separate
  backdrop recording the opaque card base: sampling the scrolling page's
  recorder was a demonstrable source/consumer cycle. Shader-unavailable
  devices retain MIUIX TabRowWithContour. B1403's reported Sandbox crash
  is not yet proven to be solely caused by this cycle.
- OS4 search keeps MIUIX's SearchBar/InputField focus and Back ownership,
  replacing only the expanded outside-end cancel text with an official
  GlassIconButton + MiuixIcons.Basic.Close. OS3 keeps text cancel.
- OS4 appearance mini-preview sizes to intrinsic GlassNavigationBar height
  rather than the fixed OS3 preview viewport. App margins follow the
  upstream documented 24.dp horizontal and 12.dp vertical usage.
- All independent page owners pass explicit list overlap to GlassTopAppBar.
- Current OS3 routes, settings and SystemUI/Xposed functional controls
  remain separate.

The copied `app/src/main/kotlin/top/yukonga/miuix/kmp/glass/`
source is derived from the fixed upstream PR revision. Unlike a complete
upstream build, this copy currently uses two temporary compatibility edits:
- `GlassPopup.kt` drops an unused KDoc import of the unpublished
  `CascadingPopupDefaults`; popup behavior is unchanged.
- `GlassTopAppBar.kt` temporarily delegates title rendering to the
  published MIUIX `TopAppBar` instead of the unpublished
  `BlurTopAppBar`. This omits PR #423's full large-title blur behavior.

GlassDialog and GlassBottomSheet are not yet integrated: the published
MIUIX main snapshot lacks PR #423's `DialogSurfaceScope`,
`BottomSheetSurfaceScope`, overlay `surface` slots and matching host
APIs. Popup subtypes and the OS4 glass icon family also remain to be
integrated where appropriate.

B1399's signed Canary workflow passed unit tests, compilation, APK
signature and Xposed metadata checks:
https://github.com/CHS-Haple/Guiyuan/actions/runs/40301616290
This does **not** establish source/API parity, UI scale correctness or
physical device acceptance.

When matching upstream packages become available, prefer the published
upstream components over maintaining these transient copied files.

B1403 Canary passed configured CI but device evidence found missing OS4
conformance: cropped title-action edges (published MIUIX TopAppBar clips),
wrong mini-preview height and Sandbox launch crash. The B1405 changes
address the separate preview geometry and sampling ownership defects;
full title-button clipping needs the matching unpublished upstream UI ABI.
Source parity and physical acceptance remain unverified. See CONFORMANCE.md.
