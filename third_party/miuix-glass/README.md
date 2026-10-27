# Experimental MIUIX OS4 Glass — upstream-first integration

Source PR: https://github.com/compose-miuix-ui/miuix/pull/423
Investigated PR revision: `b9f70838509fbc3f63f1a972777a7e6ce07f578f`
License: Apache-2.0 (retain source headers and LICENSE).

## Temporarily embedded OS4 search glyphs

`app/src/main/kotlin/com/chaners/guiyuan/ui/icons/Os4SearchIcons.kt` preserves only the `Regular` path data for official Glass.Close and Glass.Search from `miuix-glass-icons`, pinned to PR #423 at `b9f70838509fbc3f63f1a972777a7e6ce07f578f`. Their Apache-2.0 copyright/SPDX is retained. The pinned published MIUIX core has no `MiuixIcons.Glass` namespace, so directly importing the unpublished module would break the consistent five-module snapshot. The scoped vectors must be deleted and call sites switched back to `MiuixIcons.Glass.*` after the matched module is published; do not expand this approach to full unpublished composable classes.

## Experimental upstream adoption gate (hold this Draft PR)

PR #423 remains open at the 2026-10-27 review, pinned to
`b9f70838509fbc3f63f1a972777a7e6ce07f578f`. It changes
`miuix-core`, `miuix-ui`, `miuix-blur`, `miuix-preference`,
`miuix-glass`, and `miuix-glass-icons`. There is no matching published
artifact set at this revision. **Publication is preferred, not a prerequisite
for a separately reviewed OS4 experiment.** Do not promote the Draft merely
because an unpublished component builds.

An upstream fix may be integrated ahead of publication when its exact source
revision, license, changed interfaces, consumers, lifetime, fallbacks and
removal/reconciliation path are recorded. For a pure resource or application
component with no binary/ABI dependency, a narrowly attributed compatibility
copy may be acceptable. For an existing published MIUIX module, never add a
second definition of the same class or mix a changed binary contract with
unmodified sibling artifacts: validate a **complete, matching, source-built
module graph** in an isolated, uncredentialed build first, or defer the
integration. Source-level compatibility alone is not binary compatibility.
Review full source→consumer diffs, configured CI and focused device A/B before
accepting a rendering or motion fix. When upstream finalizes different code,
compare its behavior, API and lifecycle and replace the experiment rather than
perpetuating a local fork.

**Current decision — Blur text-glyph flicker:** The PR #423
`DrawBackdropModifier.kt` prefilter lifetime fix and
`LayerBackdrop.kt` continuous coordinates are credible upstream improvements.
But `miuix-blur` is linked as a pinned AAR in Guiyuan. Copying either class
alongside the AAR would duplicate owners, and swapping a module without a
complete ABI-checked dependency build would be unreviewed. **Not imported**;
prioritize an isolated matched-graph Blur build and direct pinned-vs-revised
device comparison before changing the shipped dependency. This is not an
assertion that the flicker is fixed.

**Next development order:**
1. Review PR #423 and its affected module graph at one exact source revision;
   prefer the publisher when a full matching artifact set becomes available.
2. For early source adoption, validate the complete compatible graph without
   third-party signing access, then substitute it coherently. Retire temporary
   glass sources, icon vectors and title-bar compatibility delegates only
   after API/behavior equivalence is proven.
3. Review OS4 page design, navigation, top bars, search, menus, dialogs,
   sheets and suitable icons against current upstream contracts; preserve
   OS3 and stored user preferences.
4. Test geometry/insets, 80–110% density, font scale, RTL, Back/IME, popup
   ownership, fallback and visual motion. Keep #861 Draft pending configured
   CI and necessary focused device acceptance.

Never run unreviewed third-party build scripts in credentialed signing jobs.
Do not hide ABI drift with private API shadows, new blur engines, extra popup
hosts or screenshot-fitted rendering patches. A green APK is not proof of
OS4 conformance.

## Experimental source scope (Draft; check `gradle.properties` for current Build)

- GlassNavigationBar on the main three-page navigation and its preview.
- GlassTopAppBar / GlassIconButton in hub/detail layouts, and OS4
  GlassTopAppBar on independent Home and Preview Sandbox scaffolds.
  The Home hot-reload action uses the vendored GlassIconButton compatibility
  parameters for disabled and in-flight hold state; confirm press and action
  ownership on device.
- GlassDropdownPreference / popup host for shared selector preferences.
- GlassSegmentedTabRow in existing Sandbox segmented choices uses a separate
  backdrop recording the opaque card base: sampling the scrolling page's
  recorder was a demonstrable source/consumer cycle. Shader-unavailable
  devices retain MIUIX TabRowWithContour. B1403's reported Sandbox crash
  is not yet proven to be solely caused by this cycle.
- OS4 search retains the released MIUIX SearchBar/InputField focus, query-clear and Back ownership. The search capsule uses the available upstream `glassPanel` with stock pured-thin material, with native surface fallback; its circular cancel action remains a standard MIUIX IconButton per the upstream OS4 GlassSearchOverlay example, not a top-bar GlassIconButton. Both input and cancel use the pinned native InputField 45dp minimum (upstream OS4 sample uses 44dp). Two unmodified Regular icon vector paths from the pinned PR #423 Glass.Close and Glass.Search are temporarily embedded for this search scene only. OS3 retains native SearchBar input/clear icons, text Cancel and motion. Device action/IME/animation validation remains open.
- OS4 appearance mini-preview sizes to intrinsic GlassNavigationBar height
  rather than the fixed OS3 preview viewport. App margins follow the
  upstream documented 24.dp horizontal and 12.dp vertical usage.
- OS4 no-shader fail-native path selects MIUIX's standard NavigationBar in
  both the main page and its mini-preview, not the saved OS3 floating style.
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

B1450 tracks the full source-level audit in CONFORMANCE.md, syncs B1406–1409
runtime safety fixes from dev and adjusts OS4 no-shader fallback/search.
It does **not** patch the released MIUIX Blur binary. Upstream PR #423's
continuous downscaled sample position and retained prefilter effect are
relevant to the user-reported scrolling flash, but only a version-matched
published Blur/UI/Preference/Glass module set can close that ABI gap.
This snapshot needs fresh build and device acceptance. The upstream PR is
still unmerged and the PR remains Draft.

The OS4 experimental Build identifier is intentionally separated from rapidly
advancing dev/runtime Builds; B1410 is also used by a concurrent runtime PR
and must not be treated as the OS4 distribution identity.
