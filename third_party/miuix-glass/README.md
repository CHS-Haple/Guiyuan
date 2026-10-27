# Experimental MIUIX OS4 Glass — upstream-first integration

Source PR: https://github.com/compose-miuix-ui/miuix/pull/423
Investigated PR revision: `b9f70838509fbc3f63f1a972777a7e6ce07f578f`
License: Apache-2.0 (retain source headers and LICENSE).

## Temporarily embedded OS4 glyphs

`app/src/main/kotlin/com/chaners/guiyuan/ui/icons/Os4SearchIcons.kt` preserves only the `Regular` paths for official Glass.Close and Glass.Search (now used for outer Cancel and search, while the in-field clear button uses published MIUIX `Basic.SearchCleanup`) from `miuix-glass-icons`, pinned to PR #423 at `b9f70838509fbc3f63f1a972777a7e6ce07f578f`. Their Apache-2.0 copyright/SPDX is retained. The pinned published MIUIX core has no `MiuixIcons.Glass` namespace, so directly importing the unpublished module would break the consistent five-module snapshot. The scoped vectors must be deleted and call sites switched back to `MiuixIcons.Glass.*` after the matched module is published; do not expand this approach to full unpublished composable classes.

The separate `Os4BackIcon.kt` likewise preserves the exact Apache-2.0 PR #423 `Regular.Back` vector for OS4 top-bar glass buttons, including its RTL `autoMirror`. OS3 keeps the published Back icon. No behavior, action target or popup owner is duplicated.

`app/src/main/kotlin/com/chaners/guiyuan/ui/icons/Os4DiagnosticIcons.kt` likewise retains only the `Regular` paths for Filter, Download and More at the same pinned PR #423 revision. OS4 Diagnostics uses those three vectors; OS3 keeps published MIUIX symbols. Remove these temporary files when the matching upstream glass-icons module is integrated.

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

## Upstream popup convergence

PR423 `8d52b45`'s `GlassPopupSurface.kt` and `GlassPopupColumn.kt` are vendored as exact Apache-2.0 source to restore upstream bounded intrinsic measurement, internal scroll and outside gesture policy. `GlassDropdownPopupContent` and `GlassSecondaryPopup` still use the one Guiyuan root `PopupLayout` because their real anchors lie in a clipped top bar or sheet pager. The new `GlassIconDropdownMenu` requires its anchor inside the same `BoxScope`; substituting it directly would change ownership and introduce clipping/backdrop risk. The root-hosted Diagnostics menu instead adopts upstream's primary-only popup recorder and composed clean-page+primary secondary Backdrop without moving its anchor or sampling the secondary itself. Keep that migration gated by actual host relocation and source/device evidence. The source-matched MIUIX UI/Blur build remains pinned to `b9f7083`; the above Glass leaf-source fixes do not imply upgrading the entire graph.

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
- B1501 keeps one PR423-inspired 400ms OS4 search transition for title/width/Cancel, 12dp side insets, 44dp capsules and 10dp gap. Search's visual material separately follows the active page overlap with upstream `GlassMotion.topBarButtonFloat()` (350ms linear): the native `surfaceContainerHigh` base remains, and input/Cancel share a progressively composited glass/rim layer over the same existing clean page Backdrop. Returning to the top reverses the same ramp; missing shader/backdrop is solid. Search state, Back and OS3's native SearchBar are unchanged. PR423's search example itself is solid-only; the scroll transition is a Guiyuan adaptation of its GlassTopAppBar contract. Visual movement, IME, RTL and flicker remain device-open.
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

## Experimental exact-revision MIUIX source graph

PR #423 (SHA `b9f70838509fbc3f63f1a972777a7e6ce07f578f`) passes the
upstream Android module compilation and the Guiyuan joint compilation check.
These are **compile facts**, not physical proof of Blur text-flicker repair,
search-edge quality, or native dialog surface parity.

Plain Gradle builds without the source opt-in retain published MIUIX modules for compatibility. The About page must label a source-built candidate with its verified source revision, not the published artifact version; the signed CI provides `gy.os4.miuixRevision` only after verifying the source checkout SHA.
The canonical OS4 device Canary uses the matched PR423 source graph. To build it locally, check out the exact upstream revision beside Guiyuan and run
Gradle with `-Pgy.os4.miuixDir=../miuix`. The opt-in substitutes the same-source
Core, UI, Shader, Squircle, Blur, Preference, Icons and Navigation Android
modules. No build action downloads source implicitly or alters a default APK.

**Do not include upstream Glass in this mixed experiment:** Guiyuan currently
vendors same-package Glass sources, so loading both would duplicate classes.
The CI-only joint compile probe verifies the upstream SHA and calls the same
Gradle opt-in, without signing keys or publishing an APK. Full upstream-Glass
replacement and dialog/sheet surface migration are separate acceptance gates.

GlassDialog and GlassBottomSheet cannot be compiled by default: the published
MIUIX snapshot lacks PR #423's `DialogSurfaceScope`,
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
verified same-revision source-built or published Blur/UI/Preference/Glass graph can close that ABI gap.
This snapshot needs fresh build and device acceptance. The upstream PR is
still unmerged and the PR remains Draft.

The OS4 experimental Build identifier is intentionally separated from rapidly
advancing dev/runtime Builds; B1410 is also used by a concurrent runtime PR
and must not be treated as the OS4 distribution identity.

## Parallel Blur visual A/B builds

The opt-in compatibility CI can now produce two **debug-only, different
application-ID** Android packages from the same Guiyuan source. The first
uses all published-snapshot source modules at `b587aaf2`, the second uses
PR #423's source modules at `b9f70838`. Both use Guiyuan's same existing
vendored Glass implementation, intentionally holding that part constant.
The comparison is meant to isolate the Blur/UI module revision as a group,
not assert that a single method is the only changed cause.

The jobs never load the Haple production signing key. Android's ordinary
debug signing allows these test applications to coexist with the signed
Guiyuan package. Their application IDs are
`com.chaners.guiyuan.os4published` and
`com.chaners.guiyuan.os4pr423`. They are **visual-only probes**: do not
enable either in LSPosed or assume their module runtime/configuration
behaves like the installed production identity. They do not overwrite the
production package or import its settings. On each test app, select the
OS4 appearance before comparing content scrolling under the bottom bar
and search capsule. If the build or app entry route prevents a valid
comparison, record the blocker rather than claim a glass repair.

## Native OS4 DialogSurface ABI gate

The published MIUIX snapshot has no `DialogSurfaceScope`. A single
`UiOverlayDialog` caller API has two build-time implementations. Standard
published builds compile `app/src/os4Published/kotlin` with the native
`OverlayDialog`. The exact-source PR #423 candidate additionally uses
`-Pgy.os4.dialogSurfaces=true -Pgy.os4.miuixDir=<pinned-upstream-checkout>`,
which substitutes the same-source UI/Blur graph and compiles only
`app/src/os4Upstream/kotlin`. Its exact Apache-2.0 GlassDialog,
GlassDialogSurface and ScrimBackdrop are vendored only in that candidate,
not alongside the published UI. Both paths retain one MiuixPopupHost and the
native onDismissFinished contract. Do not include the upstream Glass artifact
as a second copy of Guiyuan's existing vendored Glass classes.

## Canonical signed OS4 Canary (matched source)

The independent `.github/workflows/os4-miuix-pr423-signed-canary.yml` builds a fixed
PR #423 (SHA `b9f70838509fbc3f63f1a972777a7e6ce07f578f`) **unsigned**
APK in a runner with no Haple signing credentials. A second isolated job fetches
that APK as data, checks out only the trusted signer from fixed `main` SHA
`f9ff55056e3baa21c219440394af9225104493f1`, verifies the expected signing certificate, signs it and checks that
it is non-debuggable. From B1496 this matched-source Canary is the single
user-facing OS4 test package. Published-module builds remain internal compatibility
controls; their presence in Gradle does not make them alternative OS4 releases.
The signing job still never executes upstream Gradle with private signing secrets.
This is **not** evidence that moving-text flicker or glass popup behavior is fixed;
that still requires physical confirmation. PR423 build must first pass.

## Full OS4 UI audit

See [UI_AUDIT.md](UI_AUDIT.md) for the 16-family source-backed, per-state
checklist. It distinguishes source-gated native dialogs/sheets from
published MIUIX fallback, unported upstream `BlurTopAppBar`/icon-catalog
opportunities and the remaining physical visual checks. Do not close PR #861
solely because the signed PR423 APK built successfully.

## Native OS4 collapsing-title blur gate (B1494)

The published MIUIX TopAppBar clips glass controls together with title contents.
The local GlassTopAppBar originally had to measure an empty slot and draw the
real action above the native bar clip. PR #423's reviewed `BlurTopAppBar` provides
large-title blur, native draw-phase `titleAlpha` and **title-local** clipping,
so glass actions can stay in their original measured slots instead of being
redrawn. `UiNativeTitleBar` is an ABI-selected source-set bridge: published
MIUIX retains the original TopAppBar and its legacy clip workaround; only
fixed-source PR423 builds use BlurTopAppBar and bypass the duplicate action
positioning. The same titlePadding, nav/action inset and button sizes are passed
through. Real-device collapse/search/RTL/IME and popup overlap still require
acceptance, particularly Diagnostics' 26dp large-title inset and compact title
alignment after removing its Share button.
