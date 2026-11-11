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
  `gradle.properties`. The accepted MIUIX source/dependency pin is
  `96d613aa` via [#1517](https://github.com/CHS-Haple/Guiyuan/pull/1517);
  CI and the upstream checkout must use that exact revision.
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

- The `dev` MIUIX source/dependency graph includes `miuix-material` and
  uses app-local Glass adapters. Do not adopt upstream changes or
  `miuix-glass-android` wholesale based on commit similarity alone. The
  [MIUIX Glass source boundary](../../third_party/miuix-glass/README.md)
  explains why the published-ABI and source-matched dialog/sheet/top-bar
  compile variants remain.
- Upstream #479's fused rim/stroke pass is not in the app-local Glass renderer;
  app-local Glass rendering still uses separate passes and a local stroke type.
  Material models/presets are now canonical upstream aliases; a later renderer
  migration must preserve local button, Back and popup ownership without
  speculative shaders.
- OS4 progressive top-bar material, navigation preview and menu/popup owners
  follow their upstream-aligned implementations. OS3 UI style uses its
  appropriate native MIUIX fallback. Original Battery Colors outer/inner
  `Card` hierarchy and colors, and consistent default `Regular` Glass
  button/popup shadows, are accepted. Avoid speculative changes to the
  native animation, shader or backdrop ownership.
- Signed B1828 Canary was reported visually normal for the exercised MIUIX
  UI scope. This does not establish exhaustive RTL, extreme font scale,
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
