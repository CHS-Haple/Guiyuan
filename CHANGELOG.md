# Changelog

All notable changes to Guiyuan are documented here.

The project follows a Keep a Changelog-style structure. `[Unreleased]` contains durable net changes after the latest promoted checkpoint. Pre-1.0 versions are validated development checkpoints; **1.0.0 remains the first planned formal release**.

Failed experiments, CI-by-CI adjustments and investigation history belong in Git history or engineering decision records, not this file.

## [Unreleased]

## [0.5.0] - 2026-10-08

### Added
- Gave Liquid floating navigation its own saved icon/text preference, while Standard, Blur and Glass share the original preference.

### Changed
- Grouped Features settings by display/transition, layout, network, battery and management without changing their controls.
- Improved Diagnostics log entry presentation and refined semantic leading icons and level label weights.
- Updated first-use Keyguard, AOD, battery fill and top-readout defaults, preserving existing user choices.

### Fixed
- Diagnostics retains MIUIX navigation smoothness while displaying real loading state and bounded sequential log motion.
- Charging readout seeds its native lightning ID on host binding, synchronizes active render sessions, and retains the ID across callback gaps and Hot Reload; unavailable drawables no longer reserve empty space.
- Liquid Glass navigation derives its selected-state tint from the active MIUIX theme, including Monet dynamic color.
- Home runtime status transitions smoothly across neutral/success/warning/error accent and background changes.

## [0.4.0] - 2026-10-07

### Added
- Added Liquid Glass bottom navigation using AndroidLiquidGlass/Backdrop, with blurred and clear glass finishes and a non-interactive production preview.

### Changed
- Diagnostics now uses MIUIX pull-to-refresh as its single refresh interaction, including the initial page refresh animation.
- Mobile runtime state now keeps subscription-scoped semantic signal strength instead of persisting native signal, VoLTE, and VoWiFi resource IDs.
- Grouped the clearest Xposed runtime domains into `battery`, `prefs`, and `network` packages while keeping cross-domain orchestration at the root.

### Fixed
- Kept the charging glyph visible while SystemUI still reports charging, so a transient missing glyph sample no longer clears the charging presentation.
- Preserved mobile presentation refresh timing and adjacent-generation Hot Reload compatibility while removing unused native resource persistence.
- Internal test builds now show the gated project-entry notice through the MIUIX Scaffold host instead of silently ignoring the tap.

### Engineering
- Added AndroidLiquidGlass/Backdrop and Kyant Shapes notices and dependency metadata.
- Added a non-debuggable internal test variant and unified work-branch APK workflow; internal builds may gate the project-home link without changing Xposed runtime behavior.
- Kept the 0.4.0 repository, documentation, and build identity aligned on Build 749.

## [0.3.0] - 2026-10-07

### Changed
- Unified the README, app, About page, and Xposed module description around one concise Guiyuan project summary.
- Refined English and Simplified Chinese UI text so labels and explanations match the implemented behavior more closely.
- Consolidated public repository documentation around current architecture, durable decisions, and reusable SystemUI contracts.
- Promoted the accepted repository-wide maintainability cleanup without changing the established runtime ownership model.

### Removed
- Removed obsolete string resources left behind by superseded preview, settings, and diagnostics surfaces.

### Engineering
- Added an artifact-only signed release-APK path so a release build can be produced for validation without publishing a GitHub Release.
- Kept version/build, documentation, changelog, and public repository status aligned on the 0.3.0 baseline.

## [0.2.1] - 2026-10-06

### Fixed
- Bounded Control Center peer reservation to verified live carrier capacity so a rare pull-down/collapse path no longer falls back to native mid-transition.
- Scoped Home-derived island mirroring to Home-origin Control Center cycles so Keyguard/unknown sources keep native QS_FAKE island authority.

### Changed
- Aligned pinned-target compatibility coverage and repository state with the accepted 0.2.1 runtime.
- Closed the residual Keyguard/Control Center performance investigation without adopting the rejected alpha-layer experiment.

## [0.2.0] - 2026-10-06

### Added
- A parsed, filterable Diagnostics runtime-log view with raw access, severity presentation, pull-to-refresh, per-entry copy and compact toolbar actions.
- A separate About page with device/system, project, license and direct dependency metadata.
- Shared semantic leading-icon treatment and long-press guidance for compact icon actions.

### Changed
- Refined MIUIX navigation, sheets, localized labels, diagnostics interaction and environment presentation.
- Standardized Material semantic icons around an Outlined W400 baseline, using heavier/filled variants only when justified by the actual glyph.
- Simplified settings, rendering, transition, diagnostics and test structure while preserving compatibility identities and runtime ownership.
- Delayed Home status-icon observation until the verified native icon-manager registration boundary when startup ordering requires it.

## [0.1.0] - 2026-10-04

### Added
- Modern Xposed API 102 module integration for the verified Xiaomi HyperOS SystemUI target.
- Event-driven battery, Wi-Fi, mobile-network, airplane, SIM/data, connectivity and native tint state.
- Compact Home status presentation with reversible native replacement and fail-native fallback.
- Optional Keyguard and AOD presentation through a shared host-scoped family owner while Home remains independently owned.
- A bounded QS_FAKE Control Center transition bridge; the fully expanded Control Center stays native.
- Optional battery-top percentage and native charging-glyph presentation with user controls for size, weight and vertical position.
- Battery semantic color schemes plus configurable center/mobile color-follow behavior.
- Preview/Sandbox based on production rendering semantics.
- MIUIX companion-app Home, Features, Settings, appearance, diagnostics, export/share and language/launcher controls.
- Hot Reload with generation replacement and bounded cleanup.

### Changed
- Moved runtime state acquisition toward authoritative SystemUI/platform callbacks and process-scoped cached state instead of polling/re-query loops.
- Kept SystemUI as owner of native layout, translation, visibility, appearance and animation wherever practical.
- Reworked Home/Control Center transition geometry around verified native hosts, real endpoints and native progress rather than fixed correction offsets or custom gesture timing.
- Kept Keyguard/AOD scene selection separate from native animation ownership and made AOD independently feature-gated.
- Preserved native resource identity/tint behavior for status resources and direct Drawable rendering where verified.
- Unified public product identity as **Guiyuan / 归元** and package identity as `com.chaners.guiyuan`, while preserving compatibility-sensitive internal identifiers where required.
- Adopted bounded event-driven diagnostics and separated diagnostic capability from functional runtime control flow.
- Standardized companion-app MIUIX components, navigation behavior, semantic icon treatment and preview/configuration structure.
- Hardened build/release workflow, signing checks, dependency update flow, branch routing and stable promotion gates.

### Fixed
- Home/native handoff around Notification Shade and Control Center so the compact presentation follows verified native scene ownership rather than local timing thresholds.
- Wi-Fi semantic/resource handling for signal levels, no-internet/hotspot variants, VPN/default-network edge cases and fail-native fallback.
- Mobile/no-SIM/airplane replacement boundaries, including dual/aggregated mobile cases and event-driven airplane exit reacquisition.
- Native tint authority selection and transient/uninitialized tint handling.
- Hot Reload host/resource re-resolution and main-thread mutation boundaries.
- Network-hook installation and startup ordering so independent sources fail soft instead of tearing down unrelated state.
- Per-app language persistence and CI signer-output compatibility.

### Removed
- Experimental Battery-anchor island motion follower after evidence showed that the Battery anchor was not the motion owner.
- Experimental Home native-slot padding ownership after evidence showed it altered native Battery geometry and leaked into other scenes.

### Engineering
- Established host-scoped runtime ownership, single-writer rules, exact restoration and fail-native boundaries.
- Separated native slot geometry, Guiyuan visual geometry, transition geometry and optical adjustment.
- Kept Canary/Release functional behavior aligned while diagnostics remain observation-only.
- Added public contribution, privacy, security, third-party notice and release-governance surfaces.
