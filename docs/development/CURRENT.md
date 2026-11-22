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
- **Control Center:** SystemUI owns `QS_FAKE.system_icon_area` width,
  native FAKE/FINAL header alpha and translations, icon membership, and full
  expansion. Guiyuan may own only its scoped icon exclusions, reversible peer
  `paddingEnd` reservation, and temporary FAKE overlay. The native FAKE area
  can reflow to an end-anchored overhang after rotation; never reinstate a
  competing fake-host width lease or alter native FINAL geometry.
  Cutover requires settled native carrier and peer layout, checked at
  pre-draw; a child `onLayout` is not a completed parent traversal.
  The OEM peer-width calculation includes per-child start/end padding and a
  separate `NetworkSpeedView` layout footprint. Optional animation padding
  may be bounded by actual peer space; mandatory compact-space and
  ownership failures still fail native locally.
- **CC-to-Home handoff:** `GyModule` is the single scene authority. The
  native CC fraction-zero callback can precede `onVisibleChanged(false)`;
  resolve Home ownership after the native expansion callback has finished
  applying SystemUI's own geometry/appearance, not from the stale
  pre-proceed state. Home `onDraw` is an application callback, **not** a
  SurfaceControl/compositor completion fence. Do not hold only a FAKE
  child expecting it to override an alpha-zero native ancestor.
  Native FAKE/FINAL alpha, translation and composited visibility remain
  independent inputs; slow-close, held-close and rotated-close visual
  continuity are **not yet accepted on device**.
- **Home native layout:** `HomeRenderSession` renders in the verified
  `system_icon_area` host overlay, outside the native battery child's
  measured lifetime. Native child `onLayout` may provide provisional
  geometry during charging, privacy and orientation reflow; the existing
  host pre-draw checks the completed native anchor and can recover from a
  transient invalid state. An actual detach or settled invalid anchor
  still retires native ownership. Charging may widen the outer native
  battery beyond its stable inner core: align the combined visual to the
  measured native end without modifying native width/translation/alpha.
- **Open SystemUI candidate:** [PR #1817](https://github.com/CHS-Haple/Guiyuan/pull/1817)
  carries the source-level corrections and bounded native geometry witnesses.
  A capped native peer reservation and a requested animation span are
  **not** interchangeable: the currently applied width gates some latent
  reveals, but does not yet certify the whole transition envelope. A
  session-cached FAKE carrier-root offset is now recaptured only after a
  completed native FAKE-host width reflow; an unfinished layout and peer-only
  relayout retain the previous motion anchor. The specific device-frame
  mismatch remains unverified. The user's ROM 4.0.4.0.D02 reports
  SystemUI 17.03.260226.r, but exact installed-binary parity with the
  ROM403 reference and compositor frame order are unverified.
  Preserve separate native owner/writer/lifecycle semantics, remove rejected
  workarounds, and keep the PR isolated until exact-HEAD quality review and
  focused signed-device acceptance. CI alone does not validate the
  rotated/crowded/held-upswipe behaviors.
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
