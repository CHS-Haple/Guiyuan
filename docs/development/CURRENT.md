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
  device, OTA/Framework, Hot Reload or unusual-host acceptance. The companion
  app has one current MIUIX interface; HyperOS 3 runtime is unsupported.
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
- **Integrated SystemUI work [#1817](https://github.com/CHS-Haple/Guiyuan/pull/1817):**
  retain Home and CC projections in their separate native lifecycles.
  Exact ROM403 source confirms native peer-row underflow, signal exceptions
  and `paddingEnd` ownership. The work-branch retains only the compact steady
  QS_FAKE end reservation;
  the unfolding component envelope now controls visual reveal only and no
  longer writes extra native peer padding. The parallel all-candidate width
  cap and canvas clipping remain removed. Native FAKE host
  width, native FINAL, native alpha/motion and Hot Reload remain OEM-owned.
  The rotated FAKE native capacity lease is reacquired only after settled
  native layout; a virtual motion carrier may exceed the current physical
  host width without relaxing physical slot admission. The signed B1939
  Canary passed the reported landscape-to-portrait first upward-close
  disappearance on the maintainer's device. This does not certify every
  crowded peer, charging/island, slow-close or alternative ROM scenario.
  See the merged PR for exact-source acceptance and validation; dev's
  post-merge CI passed.
- **Network and dual SIM:** native connectivity, subscription identity, radio
  state, tint and resource visibility are authoritative. The accepted
  [dual-SIM work #1205](https://github.com/CHS-Haple/Guiyuan/pull/1205)
  includes a bounded SIM2-default-data activity check; it does not certify
  every SIM switch, VPN transport transition, idle-activity visibility or
  alternate carrier tree. Missing/ambiguous native facts fail native for the
  smallest surface, without resetting saved user preferences.
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

- MIUIX `07d48590` updates both native and Glass sheet motion,
  Slider/ColorPicker interaction, Squircle rendering, top-bar state and the
  navigation runtime through the matched dependency graph. OS4 search uses
  the public `SearchClearButton`; transformed menus share one settled placement
  and follow upstream's shadowless glass-anchor material transition. The OS4
  diagnostics filter uses Start alignment; other menus keep their existing
  anchors. Only Home's preview sandbox Card uses route-bound Expansion;
  the existing route-owned lease and theme lifecycle are unchanged.
  About title reveal follows the new upstream threshold/tween, not the old
  scroll ramp. Full-screen navigation retains system corners, except in
  multi-window mode. See the
  [integration boundary](../../third_party/miuix-glass/README.md) for optional
  feature decisions. The maintainer accepted signed B1940 from exact source
  `323773127dc532765aacfaa5f38928ca0e1db787` after source/Maven CI and signing
  passed ([#1834](https://github.com/CHS-Haple/Guiyuan/pull/1834#issuecomment-7118485814)).
  This is a focused device acceptance, not exhaustive certification. Integrate
  source without a second distribution Build; the delivered B1940 identity
  remains attached to its original signed work-branch artifact.

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
- The single MIUIX interface retains Glass navigation's selection-before-Pager
  contract, release-committed pointer selection, fixed icons and noninteractive
  preview. Standard/Blur floating navigation, native Rail and native capability
  fallback remain available; they are not a second OS-style implementation.
  All floating navigation choices share the label preference. Upgrade preserves
  the active legacy style's label choice and retires the old style and separate
  Liquid label keys; Liquid finish, theme and other preferences remain separate
  only where they represent actual features.
- Progressive top-bar material and menu/popup ownership follow the pinned
  implementation, retaining native title slots and popup Back lifecycle.
  The progressive-blur switch applies to app bars, including onboarding.
  Eligible scrollable lists feed MIUIX's explicit elastic offset into the bar
  without a second overscroll engine; refresh/Sheet retain their owners.
  Battery Colors uses upstream `GlassCard` defaults inside `GlassBottomSheet`
  and preserves the scheme/editor hierarchy. Its custom-scheme More control
  uses upstream `basic.IconButton` without the floating glass shadow.
  Separate Glass back controls retain the source-matched `Regular` shadow.
- Single-interface migration requires focused device acceptance; prior B1940
  and B1941 acceptance does not certify it. Test upgrades, all navigation
  choices, light/dark, narrow/wide, search, modal and sandbox lifecycle.
  Exhaustive RTL, extreme font scale, GPU resume, keyboard/cutout geometry,
  predictive Back and every Framework/ROM combination remain unverified.

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
