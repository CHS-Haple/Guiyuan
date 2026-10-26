# Experimental MIUIX OS4 Glass (PR #423)

Upstream: https://github.com/compose-miuix-ui/miuix/pull/423
Source revision: `b9f70838509fbc3f63f1a972777a7e6ce07f578f`
License: Apache-2.0 (source copyright and SPDX headers unchanged; LICENSE retained)

The app-only glass sources in `app/src/main/kotlin/top/yukonga/miuix/kmp/glass/`
are based on a fixed upstream revision while the official module is not
published. Two limited compatibility adjustments are tracked below. MIUIX UI/Preference/Nav/Blur stay pinned to one published main
snapshot. Do not independently mix upstream revisions.

## Temporary published-snapshot compatibility

- `GlassPopup.kt`: an unused KDoc import of the unpublished
  `CascadingPopupDefaults` API was removed; rendering and motion are unchanged.
- `GlassTopAppBar.kt`: replaces the unpublished `BlurTopAppBar` delegate with
  the published native `TopAppBar`. The glass buttons/material and bar
  interaction remain upstream-owned, but OS4 large-title blur is **not**
  implemented. The input title opacity is still forwarded to existing MIUIX
  title colors. Remove this adjustment when PR #423's full UI revision is
  available; do not treat this as complete top-bar parity.

## Coverage and remaining API boundary

- Implemented in the experimental app integration: GlassNavigationBar,
  GlassTopAppBar and GlassIconButton, GlassDropdownPreference with the official
  popup material/host, and GlassTabRow in the existing sandbox selector.
- GlassPopup, GlassDropdownPopup, GlassTransformPopup, GlassSecondaryPopup
  are copied as upstream dependencies. They are not permission to replace
  semantically different app menus just to change their appearance.
- Not integrated: GlassDialog and GlassBottomSheet; both depend on PR #423's
  revised MIUIX Overlay/DialogSurfaceScope/BottomSheetSurfaceScope APIs, absent
  from the published main snapshot. Updating a dependency version alone does
  not supply those unpublished contracts. Do not ship a locally invented
  replacement surface/overlay lifecycle as an upstream adaptation.
- OS4 icons still need a relevance/compatibility pass; do not copy the entire
  icon catalog speculatively.

The app uses the page's own background recording as the shared backdrop for
the top bar, tabs and popup surfaces; popup hosts must not be recorded by
their own backdrop. The existing OS3 component tree, saved navigation prefs,
and the SystemUI/Xposed runtime are left intact.

Review gates: actual Gradle Debug/Canary compile and unit tests (a green
Draft PR Light check is insufficient); OS3 parity, OS4 design conventions,
density/window scaling, RTL, themes, accessibility, popup placement, layered
host ownership, lifecycle and target-device behavior.

Once `miuix-glass-android` has a compatible published revision, remove
the copied sources and replace them with its pinned module dependency,
subject to CI and physical parity review.
