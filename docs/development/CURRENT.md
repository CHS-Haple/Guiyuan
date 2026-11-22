# Current Development State

This is a **current-state handoff**, not a build diary or replacement for Git,
the source tree, CI or device evidence. Verify the live `main`/`dev` heads,
`gradle.properties`, open PRs/issues and exact-head checks before making
changes. Durable design decisions belong in [DECISIONS](DECISIONS.md);
execution ownership is in [architecture](../architecture/README.md).

## Supported scope and release state

- **Runtime OS family:** Xiaomi HyperOS 4 with compatible LSPosed / Modern
  Xposed API 102. The single active compatibility target is Xiaomi 15 Pro,
  HyperOS `4.0.3.0.XOBCNXM.D02`, SystemUI `17.03.260226.r`. The supplied
  target APK's static contracts were inspected and pinned in
  `compat/targets/hyperos-17.03.260226.r.json`; this is **not** comprehensive
  device, OTA/Framework, Hot Reload or unusual-host acceptance. The “OS3”
  selector changes **UI style** only, not runtime support for HyperOS 3.
- **Stable release:** [v1.1.0][stable-release]
  was published on 2026-11-09 with a signed B1822 Release APK. The release tag,
  installed APK and current `main` or `dev` head are separate identities;
  compare their exact commits rather than assuming source equivalence.
- **Current `dev` build identity:** read `combinedStatus.versionName`,
  `combinedStatus.versionCode` and `combinedStatus.buildId` directly from
  `gradle.properties`. The active source and published MIUIX dependency
  coordinates must resolve to its same exact full revision; a newer upstream
  Git commit alone is not proof that matching Maven artifacts are published.
- **Branches:** work enters `dev` through reviewed PRs, then `main` through
  stable promotion. Retired `it` is historical only. A source-only change does
  not consume a Build; installable/signing gates are defined in
  [CONTRIBUTING](../../CONTRIBUTING.md).

## Runtime and ownership contracts

- **Home:** Guiyuan projects its indicator within the verified native end-side
  carrier, not a second permanent status-bar participant. SystemUI owns peer
  layout, native status slots, system scene facts and native fallback.
  `SysUiPresentationOwner` owns only verified, receipt-scoped masking and
  reservation; the observer observes native state without taking writer
  ownership.
- **Keyguard/AOD:** these use one host-scoped family. AOD is never a Control
  Center transition source. Scene change, host replacement and stale native
  references invalidate only the affected projection; do not retain old View
  objects after replacement or recreate competing native writers.
- **Control Center:** fully expanded CC remains native. Partial handoff
  requires verified native FAKE/FINAL state and measured native peers.
  The CC session owns only its temporary presentation, listeners and native
  receipts. Failed independent release must keep ownership/ineligibility
  visible rather than declaring a successful rollback or reusing the session.
  Do not modify native FINAL geometry to hide drawing differences.
  B1899 device evidence on ROM 4.0.4.0.XOBCNXM.D02 (SystemUI
  17.03.260226.r) identified a competing QS_FAKE width writer:
  SystemUI resized the fake status-bar area 587 → 1361 while the old
  Guiyuan 818-wide lease was active, triggering Fail native and reacquisition.
  The current PR #1817 candidate removes the fake width lease entirely
  and waits for the native layout before updating a measured peer-row
  reservation. This is source-validated design work, not yet accepted
  real-device first-gesture behavior; do not merge before focused testing.
  The B1900 device recording/log (2026-11-22, 18:47:49) then showed
  `preLayoutVisualMask active=false` without any subsequent `layoutReady`
  or Fail native. Removing the fake-width write had also removed the only
  reliable trigger for another native icon-row layout; a child `onLayout`
  may execute before the fake parent's layout-requested state clears.
  PR #1817 now requests one native status-icon relayout and resolves cutover
  from the native ViewTreeObserver pre-draw boundary (after measured
  reservations), without owning the native host width. This B1901 correction
  still requires focused real-device acceptance before merge.
  B1901 device evidence (2026-11-22 19:14) showed CC fake cutover
  `native-pre-draw-after-complete-layout` and `controlCenterPresentation
  active`, but closing CC after rotating to landscape exposed a Home
  positioning fault: original network-speed/Bluetooth/notification icons
  crossed into the Guiyuan carrier instead of remaining on its leading
  side. B1901 diagnostics contain no post-rotation native Home geometry
  snapshot or Fail native. A source audit found HomeRenderSession derived
  its overlay x solely from BatteryContainer.width, while the native
  battery_icon_container may move relative to that parent without its
  dimensions changing. B1902 resolves the Home anchor from the real
  native carrier window position, observes translation-only changes at
  native pre-draw without mutating SystemUI geometry, and keeps one
  per-session observer with attach/detach cleanup. This is an evidence-based
  correction of the stale-coordinate contract; exact post-rotation
  measured displacement still needs the B1902 device diagnostic.
  The same user narrowed the remaining native-only regression: a half
  pull stays combined, a fast full upward swipe usually stays combined,
  but slowly closing to zero reliably exposes native icons. Source tracing
  found CC→Home visibility followed only native visible/ready callbacks,
  while CcTransitionOwner already ended motion at native fraction zero.
  A slow close can therefore leave a ready CC owner hiding Home after
  motion ends and before onVisibleChanged(false); late ready=true can
  also re-hide it. B1902 reconciles Home eligibility from native panel
  visible AND positive fraction AND ready projection at all three
  callbacks (fraction, visible, ready), then resets state on hidden or
  runtime retirement. The result still needs first/slow-up-swipe device
  acceptance and new event evidence.
  A second stale-coordinate dependency was found in
  CcTransitionOwner: Session.matches() compared native View identities but
  not the root viewport dimensions, despite caching a frozen source frame
  and fake-carrier root-relative offset. When SystemUI retains the Views
  while switching portrait/landscape, the old coordinate frame can leak
  into the closing transition. B1902 now invalidates and recreates that
  temporary transition owner on native root-size change, recapturing the
  current source, native final targets and carrier offsets. A regression
  test covers both orientation directions with identical View identity.
  B1902/B1903 device clips and the subsequent 20:46 full-swipe video show
  the combined Home icon briefly replaced by a native mobile/5GA/battery
  row, then restored without user intervention; the latter demonstrates two
  repetitions in a 5.5-second recording. The B1903 diagnostic exported at
  20:21 has `runtimeLevel=not-observed` and no transition-time witness, so
  there is no proof of the actual native fraction or owner state during the
  gap. The previous B1903/B1904 terminal-native-header visual predicate was
  neither supported by such device data nor effective as a verified solution.
  It is removed from the candidate source rather than retained as a fallback.
  Corrected source ownership: CcTransitionOwner masks QS_FAKE's transition
  source RenderView, **not** Home's RenderView; Home visibility belongs to
  HomeRenderSession and GyModule's eligibility coordinator.

  A separate, deterministic Home lifecycle flaw was found: native child
  onLayout can run before the whole host's layout is settled. If its carrier
  position is then temporarily unresolvable, HomeRenderSession used to set
  `layoutReady=false` immediately, which led GyModule.onHomeReadyChanged(false)
  to deactivate native masks and reveal original small mobile type/battery
  icons. The existing Home pre-draw recheck was gated by
  `!nativeHandoffActive`; once the invalid layout had triggered native
  handoff, this blocked same-frame re-acquisition until another layout
  callback. B1906 separates provisional native-layout events from the
  complete pre-draw boundary and permits recovery even during native
  handoff. Detached hosts and a genuinely invalid settled native frame still
  release Home, without width/alpha changes, polling, timers or new hooks.
  The revised policy and a bounded readiness-driven native-return diagnostic
  are source-tested; B1906 on-device confirmation remains required.


  B1903 screenshot at 00:12 while charging shows the combined ring/readout
  intruding toward adjacent mute/network icons. The accompanying
  2026-11-23 00:11 log proves a separate concrete displacement input:
  Home core battery width 105 px but outer native battery 135 px when the
  Home owner reacquired, and the original native padding owner reconciled
  an end delta of -30 px. B1902's Home anchor uses the live *core left*
  coordinate; when the outer battery widens, that left coordinate can
  shift left with the expanded content. B1906 now preserves the native
  core-based anchor when outer/core widths agree, and aligns the compact
  105 px visual to the measured native outer battery right edge only
  when that outer width truly expands; it does not introduce a user
  vertical offset or alter SystemUI native geometry/padding ownership.
  In the same log Home was deactivated at 00:11:00.209 on
  `readiness-lost:hostCapture` and reactivated at 00:11:03.466, aligning
  with the observed temporary native icons. QS_FAKE separately failed its
  capacity contract with `fake-native-peer-row-overflow` at 00:10:47,
  00:10:56 and 00:11:03. The fake peer capacity gate previously read the
  cached row's `measureViews` even while the peer row requested another
  native measure; it now defers this check until the native peer's frame
  settles, retaining strict Fail native if a measured settled overflow
  genuinely persists. These corrections are backed by source and
  reproducible diagnostics but still require one B1906 signed-device test.

- **Network and dual SIM:** native connectivity, subscription identity, radio
  state, tint and resource visibility are authoritative. The accepted
  [dual-SIM work #1205](https://github.com/CHS-Haple/Guiyuan/pull/1205)
  includes a bounded SIM2-default-data activity check; it does not certify
  every SIM switch, VPN transport transition, idle-activity visibility or
  alternate carrier tree. Missing network observations may leave a partial
  combined display when the render model and native owner remain valid; they
  are not themselves evidence of an unsafe native write. Unexpected source
  callback exceptions may arise in downstream renderer updates and therefore
  still require a safety shutdown unless the origin is verified. Never invent
  signal, subscriptions or network state. Unsupported host identity, ownership,
  masking or restoration still fail native for the smallest affected surface,
  without resetting saved user preferences.
- **Runtime failure policy:** independent Keyguard/AOD and CC failures release
  their affected owners; unresolved retirement receipts or a cross-scene
  callback failure suspend the complete module takeover for the current
  generation. New ownership is fenced until a complete manual OFF-ON cycle;
  unconfirmed cleanup needs a SystemUI restart. The preference itself stays
  unchanged. No periodic retry, auto-disable or extra Xposed Hook is used.
- **Battery, geometry and fonts:** preserve verified native resource identity
  and charging/optimization semantics; keep native slot occupancy separate
  from drawing and transition geometry. Use measured native font geometry for
  optical clearance; do not invent permanent pixel, shader or alpha patches
  to mask unverified behavior.
- **Hot Reload (API 102):** transfer neutral V10 data, retire old callbacks,
  Views and listeners on the SystemUI main thread before accepting generation
  retirement, and revalidate the live host before reacquiring. Old V9 callback
  transfer is rejected and crossing that migration boundary requires a SystemUI
  restart. A brief native-only visual gap can occur; no frame-atomic handoff
  is claimed. Failures must not reuse an old ClassLoader callback.
- **Companion app and diagnostics:** UI preferences have one persisted writer;
  Preview Sandbox is a temporary Home-only lease, not stored radio/battery
  state. Diagnostics are bounded, on-demand observations, not authority for
  runtime ownership. App screens follow accepted MIUIX UI-style rules.

For reusable scene/geometry guidance see
[architecture](../architecture/README.md),
[layout policy](../architecture/layout-policy.md) and
[scene policy](../architecture/scene-policy.md).
For SystemUI behavior see
[public integration contracts](../reference/systemui-contracts.md);
detailed binary investigations remain in the maintainer-controlled private
reference, not this public repository.

## Current UI integration boundaries

- The `dev` MIUIX source/dependency graph includes `miuix-material` and
  uses app-local Glass adapters. Do not adopt upstream changes or
  `miuix-glass-android` wholesale based on commit similarity alone. The
  [MIUIX Glass source boundary](../../third_party/miuix-glass/README.md)
  documents the unified Dialog/Sheet/TopBar implementation using published
  MIUIX APIs. Source-matched and published-Maven builds remain independent.
- Glass shape, material, style, shadow and stroke presets reuse the pinned
  upstream Material API. The #479 fused rim/stroke pass is enabled only under
  its upstream shading, width and additive-blend conditions; Guiyuan retains
  the adapted backdrop/refraction and its own popup, button and Back lifecycle
  boundaries.
- OS4 Glass navigation commits the selected destination before asynchronous
  Pager motion; the component previews press/drag and commits pointer selection
  on release. Navigation icons remain fixed as in the pinned MIUIX example,
  including OS3/OS4 style and preview. OS3 UI style uses its corresponding
  MIUIX fallback. The signed B1831 Canary was accepted on-device for the
  focused navigation, selection-motion and preview/style regression scope.
- OS4 progressive top-bar material and menu/popup ownership follow the pinned
  implementation while retaining Guiyuan's native title slots and popup Back
  lifecycle. The saved progressive-blur switch is applied to OS4 app bars,
  including onboarding; OS3 keeps its own native title style. Eligible OS4
  scrollable lists feed MIUIX's explicit elastic offset into the bar without a
  second overscroll engine; refresh/Sheet and OS3 paths retain their respective
  owners. Battery Colors retains its original MIUIX `Card` hierarchy, colors
  and `Regular` Glass shadows.
- This focused acceptance does not establish exhaustive RTL, extreme font scale,
  GPU resume, keyboard/cutout geometry, predictive Back, OS3 dark-mode
  overscroll seam or every Framework/ROM combination.

## Remaining evidence and quality boundaries

- The new ROM403 APK pin is a **static compatibility baseline**. Device
  validation of VPN toggles, multi-SIM activity, charging, AOD/Keyguard,
  CC handoff, Hot Reload and exceptional native lifecycle remains
  scenario-dependent; do not claim blanket verification.
- Native exceptional property acquisition/release and third-party variable
  fonts were historically examined and closed without a reproduced blocking
  defect ([#758](https://github.com/CHS-Haple/Guiyuan/issues/758),
  [#769](https://github.com/CHS-Haple/Guiyuan/issues/769)).
  A new fix requires an actual witness and ownership review; never override
  a foreign writer or replay stale native state.
- [Whole-source quality tracking #1206][quality-tracker]
  owns remaining Codacy/complexity work. Review the **current** findings,
  code paths and CI rather than dated issue exports, and resolve coherent
  batches without changing accepted behavior for cosmetic cleanup.

Historical signed candidates, rejected experiments and release qualification
are available in [Releases](https://github.com/CHS-Haple/Guiyuan/releases),
[CHANGELOG](../../CHANGELOG.md), related PRs/issues, CI and this document's
Git history.
They are not separate current baselines or authorization for old workarounds.

[stable-release]: https://github.com/CHS-Haple/Guiyuan/releases/tag/v1.1.0
[quality-tracker]: https://github.com/CHS-Haple/Guiyuan/issues/1206
