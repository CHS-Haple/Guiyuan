# Changelog

All notable changes to Guiyuan are documented here.

The project follows a Keep a Changelog-style structure. `[Unreleased]` contains durable net changes after the latest promoted checkpoint. Pre-1.0 versions are validated development checkpoints; **1.0.0 remains the first planned formal release**.

Failed experiments, CI-by-CI adjustments and investigation history belong in Git history or engineering decision records, not this file.

## [Unreleased]

No changes since the 0.9.0 development checkpoint.

## [0.9.0] - 2026-10-23

### Added
- Added a temporary Home-status-bar sandbox parasite preview with bounded lease expiry and automatic display restoration on exit.
- Added a configurable low-battery threshold from 5–30% (20% default) for live rendering and preview.
- Added an optional Duo horizontal eight-dot layout for dual-SIM signals, retaining the original curved layout by default and sharing the existing Control Center transition, with separately saved vertical offsets for the two dual-row styles.

### Fixed
- Rejected conflicting per-mode visual settings in imported backups instead of silently overwriting them by layout order.
- Normalized unsafe control and bidirectional characters in custom battery scheme names before storage, including older saved names, so they do not break configuration backups.
- Restored the exact original feature preferences and change timestamp if a configuration import fails after writing visual settings.
- Compensated preference-store exceptions during configuration restore and preserved original/rollback failure details.
- Corrected 5GA suffix spacing using native font ink bounds and a bounded optical allowance, including heavier weights.
- Settled Liquid navigation drag-stop transitions through a single animation owner.
- Rebound late or replaced native airplane-mode and no-SIM Control Center targets on existing layout events, retaining the current reservation/retarget policy.

### Security
- Restricted configuration import to document-provider content URIs, rejecting local-file and resource URI inputs.

### Changed
- Shared display and transition settings across single/dual signal presentation and content layouts, with migration for stored profiles.
- Detect each verified SystemUI mobile-type and battery-number variable font's actual `fvar.wght` range, independently checking their native Typeface/file identities. Display the real minimum and maximum, provide continuous (non-snapping) MIUIX slider adjustment and precise number entry, and align slider value typography with MIUIX. Use the authenticated, read-only native axis report; an unverified source cannot inherit MiSans bounds and retains its native-safe drawing fallback. Preserve saved weight values, backup compatibility and native Control Center endpoints.

- Shared the content layout across single- and dual-row signal modes, preserving the active layout from existing configurations, and enabled the battery number by default only for unconfigured battery-centered profiles.
- Rebuilt the optional liquid-glass finish on the pinned MIUIX Blur API with locally adapted upstream lens and inner-shadow effects. The three finishes are Blur (8 dp, 40% theme-colored surface), Balanced (4 dp, 40%) and Clear (2 dp, 30%). Balanced is the unconfigured choice; stored Default and older Blur values continue to select the same 4 dp finish, while the 8 dp option retains its distinct stored value. Both use a 24 / 24 dp lens, a gravity-responsive dual-light edge highlight, a 10 dp cast shadow and a separate pressure-responsive selected capsule with depth refraction and chromatic dispersion. Preserved independent tab content settings and the native navigation fallback; removed the no-longer-used Backdrop and Shapes runtime libraries.
- Updated AndroidX Core from 1.17.0 to 1.19.1.
- Refined the Home runtime-status card with a single settled gradient and consistent switch interactions.
- Improved normal-path API 102 Hot Reload generation retirement and preserved native recovery on unsupported handoffs.
- Clarified the Chinese and English Features-section titles: Network icons → Network & signal, and Management → Configuration management.

### Engineering
- Updated the verified Gradle Wrapper to 9.8.2 and kotlinx.serialization core/JSON to 1.12.0; retained the pinned MIUIX snapshot and Modern Xposed API 102.
- Strengthened CI checks for independent dev commits and authenticated Dependabot MIUIX package access; a fresh bot-origin cold-cache resolution of all five pinned artifacts is not yet proven.
- Confirmed consecutive same-Build Hot Reload handoffs and bounded Sandbox force-stop restoration on the tested device; other SystemUI/LSPosed failure paths remain unverified.

## [0.8.0] - 2026-10-19

### Added
- Added bounded Wi-Fi and airplane-mode stroke-weight adjustment (60–130%) on native icons, with Control Center transition matching, independent profiles and backup support.
- Added JSON backup and restore for feature settings, separate single/dual profiles and custom battery schemes, with versioned format and strict validation.
- Added independent Wi-Fi and mobile-type vertical offsets for battery-centered layouts, preserving users' existing offset values.
- Added companion-app interface scaling from 80% to 110% without changing system display scaling or the SystemUI renderer.
- Recorded the actual Modern Xposed framework name and version in runtime diagnostics when exposed by API 102.

### Changed
- Updated MIUIX UI, Preference, Icons, Nav and Blur modules to published `0.9.4-8972f941-SNAPSHOT`, including Pager gesture handling and the upstream Spec2026 palette change; saved Spec2026 selections fall back to Spec2025.
- Aligned battery number and mobile type to native font-weight requests (200–1000, 50-point steps, default 600), displayed as weight values instead of percentages, without synthetic upper-range strokes; retained native Control Center endpoints.
- Converted historical 1001–1400 battery and mobile thickness values in saved profiles and validated V1 backup imports without accepting previously invalid archives.
- Aligned secondary settings-page scrolling and rebound with the MIUIX top bar.
- Let the triggered About logo wave finish after finger release, refined semantic setting icons, and clarified the Home Status simulation and Preview Sandbox descriptions in Chinese and English.

### Fixed
- Hardened untrusted configuration imports with strict JSON and UTF-8 validation, canonical scheme keys, scoped preference updates and exact rollback of affected values.
- Clarified Static Scope authorization wording; connection state is not displayed.
- Suppressed scroll-end haptics on short secondary Settings pages without scrollable content.
- Unregistered Control Center layout/pre-draw listeners from their registration observer during session cleanup, including when the View tree observer changes.
- Invalidated cached Control Center mobile targets when an active single-display session observes changed direct native mobile roots after a global layout event.
- Prevented remote preference listener lock inversion during rebind and kept in-flight old-generation feature/visual callbacks from overwriting current configuration.
- Revalidated cached Control Center native icon targets against their current native FINAL slot and optical-child ancestry before reuse.
- Matched hotspot Wi-Fi drawing to its own verified native optical bounds instead of borrowing ordinary Wi-Fi fitting.

## [0.7.0] - 2026-10-15

### Added
- Added optional dual-row four-dot signals for two verified SIMs, retaining the original single-row default and safe fallback when the second SIM or its identity is unavailable.
- Added independently saved single-row and dual-row layouts, sizing and color preferences; the dual profile initially inherits existing settings.
- Extended Preview Sandbox with four SIM-presence states, per-SIM signal/type controls, data-SIM selection, and independent cellular strength across None, Mobile and Wi-Fi modes.
- Added a live-preview continuous battery-color picker with common colors, HSV adjustment and HEX/RGB entry.
- Added an About scope detail page for host purpose, installed version and authorization state.
- Added bounded Control Center handoff for verified dual-root and composite dual-signal targets, without guessing which physical SIM an unverified native row represents.

### Changed
- Redesigned the four-dot Control Center transition to retract to the leading dot, move to the native anchor, unfold, and grow into the verified native signal shapes, with reversible motion.
- Reworked the Home runtime status indicator with a MIUIX rotating/progress ring and reversible state transitions.
- Matched the About title/version glint to the expanding logo ripple.
- Reorganized Preview Sandbox network controls and refined the Home/Preview signal and battery summaries, bilingual text and dual-SIM icon.
- Matched native 5G/5GA labels by verified glyph and data-SIM source; improved the native FAKE/FINAL spacing and charging-time geometry handoff.

### Fixed
- Corrected scope authorization reporting when the Settings process is not running; authorization remains distinct from current process connection.
- Refreshed 4G/5G network text when mobile data is switched off without a default-network change, while retaining the SIM signal display.
- Guarded SIM-slot, default-data-SIM and native mobile binding differences; uncertain dual-SIM input preserves the appropriate native/single-row fallback.
- Corrected native peer-icon spacing during Wi-Fi + separate dual-SIM Control Center transitions and charging changes during the gesture.
- Preserved battery-ring retraction and prevented incorrect number morphing when no matching native battery digit target exists.
- Invalidated cached native icon geometry/drawables on configuration changes and native mobile-target cache entries after verified rebinding.
- Preserved Hot Reload progress across app navigation and validated old/new generation handoff before installing replacement hooks; unsafe takeover requires SystemUI restart.
- Removed redundant native dual-target resolution from an individual Control Center draw.

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
