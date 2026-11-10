# OS4 glass integration — current source boundary

Guiyuan's optional HyperOS 4 visual style incorporates an adapted subset of
upstream MIUIX glass components. This folder documents **provenance and
maintenance boundaries**, not an alternative build pipeline or a standalone
library.

## Provenance and current dependency graph

- **Current `dev` MIUIX dependency (B1821 accepted):** `0.9.4-45645854-SNAPSHOT` from official `45645854cd46c32275c5666e3aafa9980d8f104e`, matched to the same source checkout. [#1384](https://github.com/CHS-Haple/Guiyuan/pull/1384) passed full [Ready source CI #42592043065](https://github.com/CHS-Haple/Guiyuan/actions/runs/42592043065), direct official-Maven resolution, Codacy, and [trusted B1821 signing #42591053313](https://github.com/CHS-Haple/Guiyuan/actions/runs/42591053313); the maintainer accepted B1821, and it was squash-merged into `dev` as `e2a49b6e9938dbdeb08a78e3b5b8b31647ecb7f6`. The previous accepted `ea1b935d` graph and B1810 UI review through #1371/#1370 remain historical evidence, not a second active pin. `main` has not been promoted.
- **Historical B1737 source-pinned graph (superseded by B1786):** [MIUIX Glass PR #423](https://github.com/compose-miuix-ui/miuix/pull/423) at `f4cfe9b0adaa5151b68ec169ed18d470b957a6f6` (previous accepted graph `b9f70838509fbc3f63f1a972777a7e6ce07f578f`). Guiyuan B1737 (`8f2f4bddc091f057b1f5dbbe07678f3cc7f9e20c`) passed [exact-head Full CI](https://github.com/CHS-Haple/Guiyuan/actions/runs/41604585823), [isolated signed Canary verification](https://github.com/CHS-Haple/Guiyuan/actions/runs/41605488416), and focused maintainer device acceptance. It was integrated into `dev` through [#1179](https://github.com/CHS-Haple/Guiyuan/pull/1179) at `7eb025ddd99f00367377f8eaeadd6b155015c9e3`. **PR #423 was unmerged at B1737 acceptance and was merged to MIUIX `main` on 2026-11-07.** Device acceptance covers the tested app-local implementation, not direct replacement with the merged upstream Glass module or universal device compatibility.
- The CI composite build checks out the same pinned MIUIX source through
  [the repository Action](../../.github/actions/miuix-source/action.yml). The
  accepted published Maven version and exact source checkout now both match
  `45645854`. **Glass UI still uses app-local adaptations** rather than directly
  linking the whole upstream `miuix-glass` module; existing top-bar clipping,
  preview/touch extension and cascade Back contracts must be retained or
  migrated through verified upstream API equivalents. The single native Glass
  Icon alias file lives in
  `app/src/main/kotlin/com/chaners/guiyuan/ui/icons/Os4GlassIcons.kt` for both
  UI source variants.
- The accepted B1810 migration replaced the previous six `os4Published` vector
  files and one `os4Upstream` icon alias file with a single `src/main` source of
  `MiuixIcons.Glass.Regular` aliases. `miuix-glass-icons-android` is now a
  normal dependency in both OS4 compilation modes; no generated icon copies or
  duplicated icon state remain. The `os4Published` and `os4Upstream` directories
  still host **distinct Sheet/Dialog UI adapters** until the whole upstream
  dialog API can be proved safe; those are not separate MIUIX dependency
  versions.
- **Historical OS4 Share exception:** accepted `b7a5f08f` had a clipped
  `Glass.Regular.Share`, so Diagnostics used ordinary `MiuixIcons.Share` even in
  OS4. [merged PR #1370](https://github.com/CHS-Haple/Guiyuan/pull/1370) now
  reuses the official corrected Glass Share through the common native alias only
  for OS4; OS3 style retains the ordinary 24dp icon. The 44dp button target,
  export intent, accessibility text, and event behavior are unchanged. The
  maintainer reported B1810 normal after the grouped OS4 UI acceptance; the
  result is scoped to that test, not all fonts, themes or devices.
- Licensing and individual upstream attribution are recorded in
  [THIRD_PARTY_NOTICES](../../THIRD_PARTY_NOTICES.md) and preserved source
  headers. Do not remove SPDX/copyright notices from adapted files.

## MIUIX Material migration and B1827 acceptance (PR #1440, 2026-11-10)

PR [#1440](https://github.com/CHS-Haple/Guiyuan/pull/1440) integrates the
maintainer-accepted B1827 MIUIX Material revision into `dev` without promoting
`main`. Its exact upstream base is
[`ca674992`](https://github.com/compose-miuix-ui/miuix/commit/ca674992).
This migrates the removed `miuix-shader` source module to
`miuix-material-android` while retaining Guiyuan's Glass ownership and UI
adapters. The previous B1821 `45645854` pin is documented below.

The UI audit found that the Battery Colors sheet had a meaningful outer
MIUIX `Card` with its own full-page background and a nested settings `Card`.
The B1824
experiment removed the outer Card, but the maintainer's screenshot confirmed
that this **lost the intended visual grouping** and must not be kept. The
B1825 correction restored the complete native `Card` hierarchy for built-in,
custom and add pages. The follow-up correction restores the **original** outer
Card theme pair, `surface/onSurface`, including the original dark
`surface=#000000`. The inner Card keeps its normal native
`surfaceContainer/onSurfaceContainer` defaults. This preserves the exact
previous light/dark grouping, 16dp inset, Squircle silhouette, header,
palette, actions and interaction feedback. No global theme tokens, storage,
paging, nested-sheet lifecycle or color data are changed.

The OS4 custom scheme management button retains the existing native
`GlassIconButton` plus `GlassTransformPopup` anchor contract. B1826
experimentally overrode the button shadow to `GlassShadows.Float`, but
the supplied device recording showed a pronounced shadow discontinuity
as the trigger became the popup. The explicit override was removed:
both the upstream `GlassIconButton` and the upstream
`GlassPopupDefaults.visuals()` use `GlassShadows.Regular` by default.
The popup transforms the button surface and panel material but does
**not** animate between different `GlassShadow` presets; using `Float`
on one end introduced a mismatch. Leave both defaults intact rather
than layering custom shadows or altering the host lifecycle.

The root-hosted popup still owns Back and its exit; the button still does
not sample the Sheet's own recorded surface to avoid a recursive HWUI
RenderNode dependency. OS3 continues using native
`OverlayIconDropdownMenu`. Restoring the preset is source-backed, but
the entire shadow transition remains subject to device verification.

OS3 dark-mode battery scheme Card bottom-line review (2026-11-10):
the sheet's `BottomSheetContentLayout` uses upstream `squircleSurface`
for the opaque shell and an independently drawn `OverscrollBackground`
for negative drag offsets, deliberately extending its fill by `1.dp`.
The scheme pager additionally has `HorizontalPager.clipToBounds()`;
the full-size parent/child Cards are upstream MIUIX `Card`, with the
requested original `surface/onSurface` and default `surfaceContainer`
theme colors. On tall surfaces (>2048 physical pixels), upstream
`SquircleBackground.kt` intentionally records the content and draws
masked top/bottom corner bands separated by a straight middle, using
whole-pixel `ceil`/`floor` boundaries. This affects both UI styles
where the same Card is used; it does not establish a new Guiyuan-only
z-order implementation fault. A thin visible seam in a device recording
cannot be certified as **intended** merely because this native strategy
is present, but the repository currently provides no demonstrated
upstream-incompatible local crop or clip operation to fix. Therefore no
new global Squircle override, extra background strip, black-line mask,
shader patch or change to bottom-sheet overscroll has been introduced.
The original card color is retained as the maintainer requested.

OS4's GlassBottomSheet still draws its complete rim, including the square
screen-edge bottom. Upstream `ca674992` retains this behavior. The trial
`stroke = null` removed top and side highlights and was reverted. This is
not proof that the white lower rim or internal Squircle joins are fixed.
The maintainer reported signed B1827 normal on the target device. The
integrated source passed full Android CI. These scoped checks do not establish
that the historical OS3 dark overscroll seam was eliminated; no local
rendering workaround is claimed.

## Accepted upstream OS4 Glass graph (B1821, 2026-11-09)

- **Accepted pinned revision:** MIUIX upstream `main` commit [`45645854cd46c32275c5666e3aafa9980d8f104e`](https://github.com/compose-miuix-ui/miuix/commit/45645854cd46c32275c5666e3aafa9980d8f104e), containing merged [#478](https://github.com/compose-miuix-ui/miuix/pull/478). Guiyuan B1821 uses its official same-revision Maven artifacts and checkout, integrated by #1384 after source/CI checks and the maintainer's device acceptance. This advances the dependency graph from `ea1b935d` while preserving app-local Glass presentation and ownership contracts.
- **Upstream/package evidence:** exact-head [Build All Tests #42578007171](https://github.com/compose-miuix-ui/miuix/actions/runs/42578007171), [Example App #42578007061](https://github.com/compose-miuix-ui/miuix/actions/runs/42578007061) and [publication #42578007090](https://github.com/compose-miuix-ui/miuix/actions/runs/42578007090) succeeded. More importantly, Guiyuan's full [#1384 Ready CI #42592043065](https://github.com/CHS-Haple/Guiyuan/actions/runs/42592043065) resolved and built with official published Maven artifacts without source substitution, alongside the pinned source graph and OS4 fallback checks. This validates this application source, not every upstream Glass API or device.
- **Exact upstream delta from `ea1b935d`:** one merge commit (#478), eight
  changed files, comprising Glass navigation bar/layout, new tests, example and
  docs. The app-local `GlassNavigationBarLayout.kt` blob SHA
  `70969103e0a7509335e05c02e9b5eea305dbe500` is **byte-identical** to upstream
  `45645854`; no duplicate animation port is needed. App-local
  `GlassNavigationBar.kt` differs intentionally: preview non-interactivity,
  Android bottom hit-area extension and associated input semantics. The new
  upstream version additionally changes nullable-backdrop fallback,
  Apple-platform bottom spacing, API documentation and delay API. These cannot
  be replaced wholesale without testing existing contracts.
- **Adoption boundary:** `miuix.version`, both full source revision properties
  and the CI checkout were advanced atomically to `45645854` in #1384. The
  tested B1821 integration retains Guiyuan's app-local Glass bridge, including
  native popup/modal ownership, title clipping, preview interaction and bottom
  touch extension; it does not replace the entire upstream Glass module. Unused
  dynamic trailing navigation action, broad RTL/font-scale/graphics-resume
  combinations and exceptional platform lifecycles remain unverified rather than
  silently declared accepted.
- **Superseded work:** #1380 (B1819 Preview Sandbox), #1381
  (persisted top-bar control) and #1383 (published dependency-graph candidate)
  were closed without independent merges after #1384 integrated their relevant
  functionality and B1821 passed the required gates. The earlier B1819 and B1820
  APKs remain their own exact-SHA historical checkpoints, not evidence for later
  untested source changes. This integration does not promote `main`.

## Upstream merge checkpoint (2026-11-08)

- MIUIX [PR #423](https://github.com/compose-miuix-ui/miuix/pull/423) merged
  into `main` at `b7a5f08f5a8c20350406e1665a97fbf99b5d9c8c`. The exact-SHA
  upstream **Build All Tests** and **Publish to GitHub Packages** workflows
  succeeded; the latest tagged stable release remains `v0.9.4` (2026-09-20).
  Workflow success is not a verified Maven resolution or app-level acceptance.
- Relative to Guiyuan's **previous** source pin `f4cfe9b0`, later upstream Glass
  commits affect progressive top-bar backdrops, button materials and blur
  composition. Those Glass implementations have **not** replaced Guiyuan's
  app-local components; advancing the source checkout alone does not migrate
  them.
- **B1786 focused qualification:** [PR #1299](https://github.com/CHS-Haple/Guiyuan/pull/1299) advances the same-source OS4 **UI/Nav/Blur/core dependency checkout** to `b7a5f08f5a8c20350406e1665a97fbf99b5d9c8c`. Comparing repository trees against `f4cfe9b0` found only `Component.kt` (unbounded-width sizing), `NavSwipeDismiss.kt` (consumed-release cancellation) and one navigation test changed in the included runtime modules; the build catalog's AGP 9.5.1 is guarded and normalized to Guiyuan's 9.5.0. **No wholesale Glass module migration.** The app-local menu integrates upstream `GlassIconDropdownMenu` with isolated root-host exit coordination and five JUnit tests. [Exact-head CI](https://github.com/CHS-Haple/Guiyuan/actions/runs/42427142011) and [signed Canary B1786](https://github.com/CHS-Haple/Guiyuan/actions/runs/42427816564) succeeded; the maintainer reported no problems after the requested device test, including the earlier B1783 low-saturation swipe regression. This is scoped device evidence, not general device certification. Two Codacy medium complexity alerts trace to the upstream-origin menu body and remain explicitly documented in the PR rather than blindly rewritten or suppressed.
- **Menu-source provenance (PR #1316):** the local OS4 Glass menu/transform
  chain was reconciled with upstream PR #423's accepted `b7a5f08f` source and
  its later `799d3d09` material-opacity correction. `GlassTransformPopup.kt`
  matches the latter revision byte-for-byte; the app-local `GlassIconButton.kt`
  keeps Guiyuan's already-used caller API and is not a verbatim upstream module
  replacement. At that B1802–B1809 checkpoint, the accepted source checkout was
  `b7a5f08f`, not a six-module `799d3d09` binary upgrade; that later `dev`
  checkout was `ea1b935d` after #1371/#1370; B1821 subsequently advanced it to
  `45645854` via #1384. Historical signed B1802 testing covered the menu
  migration scenarios reported as normal except an independent nested
  battery-color Sheet flash; signed B1805 was reported normal after its separate
  content-disposal ownership fix, which was integrated into `dev` through #1336.
  PR #1316 later merged into `dev` at `8f8f29009f4c94e5277bb502990882e32ca1cd0d`
  after the separately evidenced B1809 submenu-exit acceptance; its accepted
  host ownership remains app-local.
- The isolated native-module compile probe
  [#1183](https://github.com/CHS-Haple/Guiyuan/pull/1183) failed on public-API
  and Kotlin `internal` visibility boundaries. The merged Glass source still
  does not expose the app's compact/unclipped top-bar, preview/touch navigation,
  and cascade Back/host interaction contracts. Keep the accepted app-local
  bridge and its source pin until a bounded, source-matched module trial can
  meet these contracts without hidden upstream patches.

## Earlier accepted milestone: 28dp Glass iconography at `ea1b935d`

MIUIX [`ea1b935d`](https://github.com/compose-miuix-ui/miuix/commit/ea1b935ddba7bb25de816342b2e7a4ad2113bb46) establishes a **28dp Glass vector canvas** and matching viewport/group coordinates while ordinary MIUIX icons remain 24dp and standard Glass top-bar buttons remain 44dp. Its `GlassPopupItem` takes `maxOf(24.dp, icon.defaultWidth/Height)` on each axis instead of forcing an icon into 24dp. The official [`Build All Tests`](https://github.com/compose-miuix-ui/miuix/actions/runs/42486425937) and [`Example App`](https://github.com/compose-miuix-ui/miuix/actions/runs/42486425918) workflows passed on that SHA; that does **not** verify a complete published dependency graph.

Guiyuan [merged PR #1370](https://github.com/CHS-Haple/Guiyuan/pull/1370) uses
the already merged `0.9.4-ea1b935d-SNAPSHOT` Maven baseline, consolidated ten
Glass icon aliases into one common source file and deleted the six duplicate
fallback vector files. It also adopts the upstream 28dp sizing at OS4 call sites
and the upstream `GlassPopupItem` intrinsic-size rule; OS3 style retains the
24dp ordinary icons. The original six and four newly supplied paths had been
verified against the exact upstream `ea1b935d` source before switching to native
aliases.

**B1810 verification (historical):** MIUIX's official [tests](https://github.com/compose-miuix-ui/miuix/actions/runs/42486425937) and [package publication](https://github.com/compose-miuix-ui/miuix/actions/runs/42486425920) succeeded on `ea1b935d`. Guiyuan's exact B1810 source `91b84c9abceb7f4627ac181a11ef93c935028d0d` passed [full Android, two OS4 source modes, and direct published-Maven JVM CI #42546207293](https://github.com/CHS-Haple/Guiyuan/actions/runs/42546207293); [independent trusted signing #42546874538](https://github.com/CHS-Haple/Guiyuan/actions/runs/42546874538) verified the package/version/certificate. The maintainer then reported the B1810 grouped UI check **“正常”**. This is positive focused device acceptance, not proof of every exceptional input, extreme font scale, RTL, process resume, or alternate device. `dev` integration is [PR #1370](https://github.com/CHS-Haple/Guiyuan/pull/1370); `main` was not promoted.

The accepted app-local implementation also follows upstream `GlassMaterial.withUnderlayBlur()` for the action bar, both TabRow forms and OS4 search surface, removing the former rounded blur constants and the last deprecated helper call. The final B1810 source and its additional published-Maven unit tests passed [CI #42546207293](https://github.com/CHS-Haple/Guiyuan/actions/runs/42546207293), followed by the maintainer's normal device feedback. Source and device evidence have distinct scopes; keep the current app-owned Back and popup contracts. Codacy's upstream-identical navigation labeled-return warning is still open.

## Follow-up upstream capability audit (2026-11-09)

**B1818 device-accepted local adaptation (merged to `dev` at `b0f4dc18`):**
[#1378](https://github.com/CHS-Haple/Guiyuan/pull/1378) enables progressive blur
on OS4 Home, Features/search and Settings and imports the reviewed #478 nav
layout and 25dp destination icon size. Its signed exact-source candidate
`3eaa3be` received the maintainer's “未看出异常” feedback. This covers visible
layouts and materials only, not dormant dynamic trailing-action motion, extreme
font scales, RTL or other GPUs; no full upstream Glass module replacement is
implied. The 28dp Glass **vector canvas** remains upstream-defined and is
distinct from the navigation icon's 25dp displayed size. The maintainer later
reported visible horizontal banding under scrolling text but explicitly accepted
integration without a local blur-algorithm patch; the measured Sandbox
overlay-band extension and shared top-bar switch were subsequently integrated
and accepted with B1821 through #1384.

- **[MIUIX PR #478](https://github.com/compose-miuix-ui/miuix/pull/478):
  app-local bounded adoption, now merged upstream at `45645854`.** Guiyuan #1378
  copies `GlassNavigationBarLayout.kt` byte-for-byte from upstream `d1588a2`,
  preserving its exit-content retention, animated width/spacing/scale/opacity
  and input/accessibility retirement, while changing displayed destination icons
  to 25dp. MainHub/Appearance do not provide a trailing action, so the new
  non-null action transition is **not** a device-verified feature and must not
  be advertised as such. Existing bottom touch extension and navigation previews
  remain app-owned.
- **[MIUIX PR #461](https://github.com/compose-miuix-ui/miuix/pull/461): defer,
  upstream unmerged.** It reconstructs graphics layers and runtime shaders on
  resume for Xiaomi MTK/Mali graphics drivers. No device/runtime evidence
  establishes that Guiyuan's current target needs this fallback; introducing
  another graphics lifecycle writer would need an independently scoped
  reproduction and ownership review.
- **[MIUIX PR #321](https://github.com/compose-miuix-ui/miuix/pull/321):
  source-scoped adoption already exists.** Guiyuan's 7 `effect.bg` files match
  this PR's `73bdd49b` source exactly in 6 files. The documented
  `BgEffectModifier.kt` exception only corrects palette/animation job
  retirement; `GyBgEffect` remains the About/Onboarding adapter. As the module
  is not merged/published, wholesale dependency replacement is not ready. See
  [MIUIX effect source](../miuix-effect/README.md).
- **Accepted upstream API reconciliation:** `GlassTopAppBar.progressiveBlur` is
  now on in the three user-scrollable OS4 primary screens, using the page-owned
  `topBarBackdropSource` and upstream 80dp/48dp progressive band. The Preview
  Sandbox retains its custom dock-height overlay band; onboarding remains a
  compact, step-specific top bar: do not copy the primary-page geometry there
  without separate evidence. The local
  `GlassIconButton.inheritTopBarMaterial`/`material` APIs now match the upstream
  material-selection contract while retaining Guiyuan
  hold-down/long-press/anchor semantics. Turning material inheritance off still
  permits fallback to the parent backdrop **source** by upstream design; it does
  not re-enable inherited material.
- **[MIUIX issue #470](https://github.com/compose-miuix-ui/miuix/issues/470):
  upstream-only issue, no local timing patch.** The `RefreshComplete` feedback
  can fade during the native pull-to-refresh collapse. Guiyuan Diagnostics
  passes its own text labels but uses MIUIX `PullToRefresh` state/animation as
  intended. The upstream issue is open without a merged correction; no
  independent Guiyuan reproduction has been supplied, so keep lifecycle
  ownership upstream and reevaluate only on a proven fix or device report.
- **Whole Glass module replacement remains deferred.** The accepted app-local
  bridge has 32 Kotlin files; eight matched upstream `ea1b935d` at the earlier
  comparison; #1384 retained the bridge with nine matching files against
  `45645854`, while the published `GlassSurface`, Dialog and Sheet
  implementation still differs at API/internal-visibility/host boundaries. A
  forced same-package dependency swap would conflict with the compact/unclipped
  title, native modal/cascade host and touch-extension contracts. Retain the
  verified local bridge until an isolated public-API migration succeeds.

## Historical candidate: `ba0fca0` (subsumed by accepted `ea1b935d`)

**Historical decision (before B1810): qualify later, without upgrading to this intermediate commit alone.** The candidate was upstream commit [`ba0fca0cae3ace964eef59a3108cd782f7f9ec3d`](https://github.com/compose-miuix-ui/miuix/commit/ba0fca0cae3ace964eef59a3108cd782f7f9ec3d). It is a **candidate source revision for one coherent, same-revision OS4 Glass dependency graph**, not an accepted artifact/version or authorization to replace local Glass code. At that earlier `ba0fca0` comparison, Guiyuan's dependency and Build identity had not changed; the dependency subsequently advanced through #1371 and then to `45645854` via B1821 #1384, as recorded above.

[Compared with the accepted `b7a5f08f` source](https://github.com/compose-miuix-ui/miuix/compare/b7a5f08f5a8c20350406e1665a97fbf99b5d9c8c...ba0fca0cae3ace964eef59a3108cd782f7f9ec3d), this revision adds two Glass-only commits across nine source/test files:
- [`799d3d09`](https://github.com/compose-miuix-ui/miuix/commit/799d3d09f996d66cb2b1c2318deed8242c4f440e): separates button material-layer alpha from popup anchor visibility, with related Glass button/top-bar/popup changes and tests. A bounded correction from this commit is represented in Guiyuan's **merged** menu migration [#1316](https://github.com/CHS-Haple/Guiyuan/pull/1316); this is not adoption of the whole Glass module.
- [`ba0fca0`](https://github.com/compose-miuix-ui/miuix/commit/ba0fca0cae3ace964eef59a3108cd782f7f9ec3d): retargets Glass navigation indicator drag/release to current geometry and adds dynamic-theme accent treatment, including new desktop interaction/colour tests. It does not itself establish Guiyuan's preview, touch-extension, compact top-bar or popup-host contracts.

**Evidence at the candidate checkpoint (2026-11-08):** the upstream exact-SHA [Build All Tests](https://github.com/compose-miuix-ui/miuix/actions/runs/42464288102) is overall **failed**, although its Android `assembleAndroidTest` job passed; the JS/Wasm browser test job failed. The upstream [Example App build](https://github.com/compose-miuix-ui/miuix/actions/runs/42464288107) is **failed**, including its Android job. The [Publish to GitHub Packages](https://github.com/compose-miuix-ui/miuix/actions/runs/42464288129) job was still in progress when checked. None of these results establishes that all required Android modules are officially published at the identical source revision, independently resolvable by Guiyuan, or app-compatible. Latest verified tagged stable release remained `v0.9.4`, not this SHA.

**Admission gates before a runtime dependency or module upgrade:**
1. Obtain trustworthy official **same-revision** Android POM/Gradle metadata and AAR artifacts for the complete resolved dependency graph (including the relevant UI, Blur, Core, Preference, Icons, Nav, Shader, Squircle, Glass and Glass Icons modules); prove actual Maven/GitHub Packages resolution, source-version provenance and compatible transitive versions. A successful publish workflow alone, cached sources, a locally synthesized SNAPSHOT, and mixed-revision modules are insufficient. Keep credentials inside the trusted resolution path.
2. Require successful/understood upstream **Android library and Android
   example** build evidence at the selected exact SHA. Investigate any other
   failed upstream matrix jobs rather than reporting an unqualified all-green
   result; assess API, AGP/Kotlin and source-to-binary compatibility
   independently.
3. On an isolated `feat/` or `refactor/` branch, attempt an atomic Glass module
   replacement only if the public API and Kotlin `internal` visibility
   boundaries can replace app-local same-package classes **without duplicate
   symbols or CI source rewriting**. Re-run a complete `base→HEAD` review and
   source-matched Guiyuan Android unit tests/Debug assembly/metadata checks. Do
   not point the protected CI checkout at an unqualified candidate.
4. Compare both UI styles' affected paths. For OS4: native/simplified button
   material alpha; top-bar clipping; popup/dismiss/Back, rapid reopen and
   cascades; drag interruptions, RTL and variable action count; theme/dynamic
   colours; bottom touch-safe zones and noninteractive preview; Sheet/Dialog
   lifecycle and backdrop; diagnostics Share exception. For OS3 **UI style**,
   verify common dependency contracts and absence of regression. No HyperOS 3
   runtime support is implied.
5. Only after the above gates pass, use an explicitly reviewed same-SHA signed
   Canary checkpoint for focused Xiaomi 15 Pro interaction acceptance where
   runtime visuals changed. Do not allocate a Build or sign for this candidate
   tracking record. Promotion remains a separate review and `dev→main` decision.

Retain the current accepted source/published pins and local bridges until the
complete gate passes. Monitor meaningful upstream fixes or an officially
available matching artifact set without re-notifying unchanged/pending status.

## Maintenance contract

1. Check whether the upstream component is released, experimental or absent
   before changing a MIUIX-facing control.
2. Review its layout, state, gesture, scroll, inset, lifecycle and animation
   behavior against the **exact revision** selected for the build. Do not mix
   unrelated UI, Blur, Nav and glass implementations.
3. Compare OS3 and OS4 *UI styles* separately. The OS3 visual selector does
   **not** expand the advertised system support below HyperOS 4.
4. When the upstream module becomes suitable for adoption, compare ABI,
   transitive dependencies, screenshot/gesture parity, runtime fallback,
   performance and licensing before replacing local bridge code. Never
   auto-upgrade on upstream activity.
5. Keep experimental tests and source-matched signing separated from trusted
   publishing. Do not revive superseded OS4-only workflows.

## Native Glass module migration — source-based acceptance boundary (2026-11-03)

At the **2026-11-03 comparison checkpoint**, the accepted `dev` app contained
**30 local Glass Kotlin files** (approximately 331 KiB) under
`app/src/main/kotlin/top/yukonga/miuix/kmp/glass/`. A file-by-file comparison
against **exact upstream PR #423 HEAD** `f4cfe9b0`, excluding comments and
whitespace-only differences, found **24 files with the same implementation
logic** (approximately 215 KiB). This is a duplication inventory, **not** proof
that 24 files may individually be deleted: upstream and local classes share
package-qualified names, and module-internal declarations have different
visibility when compiled separately.

At that checkpoint, six implementation differences and their respective
ownership decisions were:

| Local file | Relevant difference | Replacement decision |
| --- | --- | --- |
| `GlassDropdownPopup.kt` | Local `handleBack` switch prevents the primary popup from competing with a visible submenu for Back; panel arguments otherwise align with upstream behavior. | Keep until the upstream cascade handles the same root-host/Back contract or an explicit public API provides this choice. |
| `GlassNavigationBar.kt` | Local `interactive` and `touchExtension` preserve noninteractive previews and extended bottom-area hit testing; Android inset behavior otherwise aligns. | Investigate caller-owned touch modifier and explicit enabled/semantics API; do not drop safe touch targets. |
| `GlassPopup.kt` | Selection checkmark tint and panel argument ordering differ. | Verify upstream selected/unselected visuals and popup composition semantics before replacing. |
| `GlassSecondaryPopup.kt` | Popup-surface modifier/interaction argument ordering differs, without a separately identified new owner. | Candidate for native reuse once the whole module is substituted. |
| `GlassTopAppBar.kt` | Local compact title/padding, `UiNativeTitleBar`, and unclipped navigation/action controls protect the verified OS4 top-bar layout. | Keep a small app-owned presentation adapter or ask upstream for the missing public extension points; do not remove clipping protections. |
| `GlassTransformPopup.kt` | Popup-surface modifier/interaction construction differed at this historical checkpoint. | Any future replacement must also honor the newer B1786 anchored-popup dismissal/host lifecycle contracts. |

**Historical B1786 inventory boundary:** at the B1786/#1299 integration, `dev`
has **31 local Glass Kotlin files**, including the new
`GlassIconDropdownMenu.kt`; `GlassTransformPopup.kt` and the menu host lifecycle
also changed. The six-item comparison above is **not a current full-file diff**.
Regenerate source/ABI/diff evidence against the exact accepted revision before
considering any deletion or module replacement.

**Dependency boundary:** the exact upstream checkout contains `miuix-glass`, and
its KMP module depends on `miuix-ui`/`miuix-blur` plus navigation events.
Guiyuan's current composite substitution includes UI, Blur, Nav, Icons, and
Glass Icons, **not** `miuix-glass-android`. Adding that substitution alone would
leave local duplicate class names; deleting selected files alone would break
same-module internal references or app-specific API uses. An upstream merge or
successful publish workflow does not, by itself, establish stable-release
artifact availability or Guiyuan dependency compatibility.

**Deferred whole-module option (not part of the current plan):** only if
separately requested and justified, trial the complete upstream Glass module on
an isolated `refactor/*` branch, with all duplicate app-local Glass declarations
excluded **atomically**. Place only genuinely app-specific behavior in a
separate `com.chaners.guiyuan.ui` adapter/API layer, using stable public
upstream APIs; where those APIs are absent, evaluate an upstream extension or
defer adoption. Do **not** silently edit checked-out MIUIX Kotlin sources inside
CI, copy six files into the upstream build, relax source-revision locks, or use
an unverified binary fallback. Keep OS3-style behavior separate.

**Required evidence before merging a replacement:** (1) exact-revision Android
compile and unit tests without duplicate classes or inaccessible `internal` API
usage; (2) public API, source ownership, licensing, native/published fallback
and transitive dependency review; (3) compare B1737 normal/cancelled Back, popup
dismissal, rapid reopen, selected state, title clipping, bottom touch-safe zones
and noninteractive preview; (4) light/dark and OS3/OS4 style switching; (5) one
grouped signed Canary and maintainer device acceptance only if runtime UI
behavior changes. A code-only/docs-only investigation does not consume a new
Build.

**Alternatives rejected for this baseline:** continue manually copying all 30
files (high recurring drift), link native Glass alongside same-named local
classes (duplicate symbols), or rewrite upstream checkout on CI
(hidden third-party fork). Partial individual deletion is not a safe substitute
for an owned module boundary.

## Historical conformance evidence

- [CONFORMANCE.md](CONFORMANCE.md): source-backed component and interaction
  matrix from earlier validation checkpoints.
- [UI_AUDIT.md](UI_AUDIT.md): dated deeper investigations, old trial results and
  adoption decisions.

These long-form records are **historical**, not current build instructions. For
current behavior use the repository source and
[CURRENT](../../docs/development/CURRENT.md); for release admission use
[RELEASE_READINESS](../../docs/development/RELEASE_READINESS.md). The original
build and experimental references are retained in Git history and PR records
rather than being reasserted here.

### Five-commit UI audit from earlier `b7a5f08f` to accepted `ea1b935d`

| Upstream change | Guiyuan assessment |
| --- | --- |
| `799d3d09` material-alpha preservation | Local `GlassTransformPopup.kt` is byte-identical to the updated official source. Still verify popup/button material and fade against actual UI behaviour before a broader module migration. |
| `ba0fca0` navigation drag/indicator colours | Candidate ported the **exact upstream gesture body**, drag-release selection retargeting and dynamic-theme primary tint into local `GlassNavigationBar.kt`; retained its caller-owned noninteractive preview, bottom touch-safe extension and nonnull backdrop. Android inset/Platform abstractions remain native to this Android-only app; the grouped B1810 device check was reported normal, but RTL and all drag/colour edge cases were not independently documented. |
| `b59ed570` animated display-cutout handling | MIUIX now supplements per-frame WindowInsetsAnimation cutout insets. Guiyuan intentionally disables upstream host-listener replacement via `LocalDisplayCutoutFixEnabled provides false` in `GyTheme`; do not enable another owner of the Activity's inset listeners without host/lifecycle evidence. |
| `7d8ec89a` upstream ARM CI runners | Build infrastructure only; no Guiyuan runtime UI change. |
| `ea1b935d` Glass 28dp canvas and popup sizing | Accepted into `dev` through merged #1370 using official Glass icons and popup intrinsic sizing; the signed B1810 grouped device check was reported normal. |

The local `miuix-glass` bridge contains 32 files; 8 exactly match the current
upstream file bytes. Most differing files are comment/style-only or app-owned
adapters; **file equality is not evidence of safe removal**. The new upstream
`GlassIconButton` provides `inheritTopBarMaterial` and explicit material
selection but lacks Guiyuan's long-press, enabled and hold-down API. Its top-bar
source also does not directly replace the existing compact/unclipped title host.
A wholesale same-package `miuix-glass` link would conflict with local
declarations, so migrate only supported, source-proven APIs without hidden local
forks. Device acceptance and release promotion remain separate gates.
