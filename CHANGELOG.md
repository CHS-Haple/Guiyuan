# Changelog

All notable changes to Guiyuan are documented here.

The project follows a Keep a Changelog-style structure. `[Unreleased]` contains durable net changes after the latest promoted checkpoint. Pre-1.0 versions are validated development checkpoints; **1.0.0 remains the first planned formal release**.

Failed experiments, CI-by-CI adjustments and investigation history belong in Git history or engineering decision records, not this file.

## [Unreleased]

## [0.7.0] - 2026-10-15

### Added
- Added optional two-row mobile signal dots for two authoritative active SIMs, while keeping the original single-row default and safely falling back when a second SIM is unavailable.
- Added separately remembered single-row and dual-row visual profiles and richer dual-SIM and data-SIM controls in Preview Sandbox; simulation remains app-local.
- Added a bounded native-geometry morph for verified dual-root and one-root composite signal targets during Control Center expansion, without assigning unsupported per-SIM identity to composite rows.

### Changed
- Changed the four-dot Control Center transition to collapse, anchor travel, progressive reveal and native bar growth, using the existing native progress and reversible motion ownership.
- Improved native 5G/5GA glyph matching across data-SIM and aggregate-root differences only when live subscription and rendered-text evidence match.
- Improved native fake/final peer spacing and charging-time geometry retargeting without taking over native final Control Center motion.
- Refined network/battery preview summaries, bilingual captions and composite leading-icon proportions.

### Fixed
- Invalidated native drawable/configuration and mobile-target witness caches when their verified native source changed, including supported same-subscription mobile binder rebinds.
- Kept Hot Reload UI progress application-scoped across navigation and hardened transfer validation before framework Hook takeover, avoiding unowned preference bindings on failed takeover.
- Removed repeated native dual-target resolution within a Control Center draw, retaining the existing single-writer, session and fail-native rules.

Native child changes that bypass verified binder events and abnormal SystemUI generation-disposal edges remain scoped compatibility observations in #521 and #519; they are not claimed universally fixed.

## [0.6.1] - 2026-10-12

### Added
- Added an optional Guiyuan shortcut inside Settings with configurable position and icon style; it is off by default and requests only its own Xposed scope when needed.
- Reworked the About header with its animated Guiyuan mark, expanding wave, text-linked response and background presentation.

### Changed
- Expanded the existing SystemUI restart action into a selectable scope restart for SystemUI, Settings or both; only authorized targets appear, and failures are reported separately. Restarting Settings does not launch its UI.
- Refined About scope reporting to distinguish connected, not enabled, confirmed missing package and explicit runtime failure, keeping unknown conditions neutral.
- Improved MIUIX companion-app scrolling, control alignment and contextual slider feedback, along with version and status presentation.
- Updated user-facing guidance for Home runtime status, optional scope authorization and restart controls.

### Fixed
- Improved observed Keyguard/AOD/native-icon handoff ordering and battery/peer restoration at verified transition boundaries; ambiguous paths still fail native.
- Improved verified 5GA native-text matching and SIM-bound mobile source updates, retaining native fallback when targets differ.
- Hardened Hot Reload/session diagnostics, service rebind callbacks and preference mirroring so prior generations cannot overwrite newer state.

## [0.6.0] - 2026-10-11

### Changed
- Refined the Home runtime-status card with an actionable SystemUI restart path and an Xposed scope-request route when supported, while retaining explicit pending/unavailable states.
- Improved native-target status transitions, including verified 5GA text targets and SIM-scoped mobile updates. Missing or incompatible targets continue to fail native.
- Updated Liquid navigation layout tracking so its motion follows the current width and layout direction without creating a second motion owner.

### Fixed
- Rebound the retained Keyguard/AOD icon-alpha observer after host detachment and reattachment, keeping one active listener tied to the current View tree.
- Hardened Hot Reload network-state restoration and preference propagation across service binding, changes and failures, preserving the authoritative current settings.
- Kept diagnostic logs from separate Hot Reload generations in the same SystemUI process distinguishable, including takeover failure events preceding a successful reload marker.

### Engineering
- Completed a bounded maintainability cleanup with evidence-backed dead-code, resource and test simplification; routine per-change maintainability review remains required.
- Updated build tooling to Gradle 9.8.1, Kotlin 2.4.21 and pinned Actions artifact upload 7.0.2, retaining the verified MIUIX snapshot.

## [0.5.0] - 2026-10-08

### Added
- Liquid floating navigation remembers its icon/text content independently; Standard, Blur and Glass continue sharing one content choice. First-time defaults are icons and text for Liquid and icons only for the other styles.
- Preview Sandbox offers its own dark/light simulation switch and contextual long-press guidance, without changing the device theme.

### Changed
- Reorganized Features into display/transitions, layout/size, network icons, battery/charging and management, keeping existing controls and saved choices.
- Refined semantic leading icons across Features, Keyguard/AOD, and Preview Sandbox, including a clearer relationship between simulation controls and their icons.
- Diagnostics presents more legible level badges, a real loading indicator, and a coordinated upward entrance for the event summary and log entries.
- Adjusted initial defaults: Keyguard and AOD display on, battery-fill-follow-retraction on, and top battery percentage off. First enabling the percentage sets the top offset to +5 only when that layout's offset has never been changed; existing user settings are preserved.
- Moved the top-information vertical offset control beneath outer-ring thickness in Layout & Size.

### Fixed
- Smoothed the Diagnostics secondary-page slide and log-list appearance, keeping cached content on re-entry and retaining MIUIX pull-to-refresh without entry flashing.
- Preserved charging-lightning visibility across transient native callback gaps, host binding and Hot Reload, avoiding empty icon space.
- Matched Liquid Glass selected-state coloring to the active MIUIX theme, including dynamic colors.
- Smoothed Home runtime-status indicator and card-background color changes across their real status states.

### Engineering
- Kept the app settings and preview tied to a single persistent source for each floating-nav content choice; old saved settings remain compatible where their origin can be determined.
- Refreshed the pinned MIUIX development snapshot and maintained existing SystemUI/Xposed runtime behavior.

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
