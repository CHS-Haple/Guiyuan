# Changelog

All notable changes to Guiyuan are documented in this file.

The project follows a Keep a Changelog-style structure. During pre-release development, `[Unreleased]` describes the **current net state intended to progress toward the first formal 1.0.0 release**. Current `0.0.x` display versions are development lines, not formal-release milestones. The final release-preparation commit freezes applicable changes into a dated version section immediately before publication. Intermediate experiments, superseded implementations, CI-by-CI adjustments, and diagnostic investigation history belong in Git history or dedicated development documentation.

## [Unreleased]

### Added
- Settings now includes an “Other” card with a Project address row linking to the Guiyuan repository and showing the project’s GPL-3.0-only license identity.
- Optional battery-top percentage readout can reserve a measured opening in the ring, reuse the HyperOS-selected native charging glyph, and expose MIUIX controls for number size/weight/vertical position and charging-glyph size; the feature defaults off and remains inside the existing Battery transition ownership.
- Optional opt-in lock-screen Guiyuan uses a separate Keyguard host/render/presentation adapter, while Home and Keyguard retain independent mutable View ownership. Keyguard-originated QS_FAKE is enabled only after the steady Keyguard presentation is ready.
- Keyguard projection is guarded by native HyperOS AOD lifecycle authority from `MiuiBatteryMeterView`. An independent default-off AOD preference projects Guiyuan in AOD. Keyguard and AOD now share one host-scoped presentation owner and one render View on the verified Keyguard-family host, retargeting scene semantics without a restore/reacquire native interval; Home-to-AOD may use bounded reversible target pre-mask while compact readiness still waits for native layout. HyperOS continues to own AOD animation timing, native alpha/visibility/translation, and unresolved host/topology contracts fail native without affecting the accepted Home/QS_FAKE path.

- Battery-ring color now follows HyperOS battery semantic states: charging, power-save, performance, and low-battery use the colors already loaded by SystemUI, while normal state keeps the native status-icon tint. The color policy is structured so every state can later choose System default, status-icon tint, or a custom color without changing the native state-source path.

- Global Guiyuan master switch controls replacement presentation through one runtime authority. Enable/disable preserves SystemUI-owned layout/motion semantics, updates native suppression only when the replacement is ready, and restores native status presentation on cold, invalidated, or unsupported hosts.
- Modern Xposed API 102 module baseline scoped to `com.android.systemui`, with a single Java entry point, verified SystemUI compatibility profile, host lifecycle capture, and hot-reload support.
- Event-driven Guiyuan state pipeline for battery, Wi-Fi, mobile network, airplane mode, default-data subscription, connectivity, and native SystemUI tint.
- Home status-bar Guiyuan rendering based on verified SystemUI hosts and native state sources while preserving conservative SystemUI geometry ownership.
- Shared scene-capability and layout-policy models for Home, notification-shade transitions, Control Center, keyguard, and AOD, with charging represented as render state rather than scene identity.
- Deterministic HyperOS Wi-Fi and mobile signal parsing into semantic levels, with unit coverage for the core mappings.
- Bounded structured runtime diagnostics for compatibility, lifecycle, state propagation, rendering, host topology, geometry ownership, and hot reload.
- Built-in diagnostic report export/share using LSPosed module logs with logcat fallback, without a resident collection service.
- Debug, Canary, and Release build channels with diagnostics depth separated from core feature behavior; Canary is non-debuggable and release-optimized while retaining bounded runtime diagnostics.
- Manual, explicitly confirmed SystemUI restart using bounded Root execution without a resident Root service.
- MIUIX application shell with Home, Features, and Settings navigation, predictive back, direction-aware swipe-back, and adaptive launcher icons.
- Appearance settings for light/dark mode, dynamic color, standard/floating navigation, Blur/Glass floating-navigation material, icon-only or icon-with-label floating-navigation content, and in-app swipe-back behavior.
- Android 13+ per-app language selection for system default, English, and Simplified Chinese.
- Optional launcher-icon hiding while retaining a non-launcher app entry point.
- Runtime diagnostics UI for app/build, device/system, module compatibility, diagnostics level, and report actions.
- Guiyuan visual color-link controls can independently make the four mobile-signal dots/unavailable mark and the center network icon follow the battery ring’s final resolved color; both links default off and synchronize to SystemUI through event-driven Modern Xposed remote preferences.

### Changed

- Battery-ring avoidance now handles optical components that sit entirely to one side of the ring center, so HyperOS hotspot-link and no-internet badge components reserve only their actual right/left shoulder arc instead of being skipped by the legacy center-crossing assumption.

- Profile defaults now use 120% battery-number size for Network centered and 140% battery-number / 80% mobile-type size for Battery centered; the former battery-content vertical-offset control is now a global-section “Top information vertical offset” whose target follows the active layout (number + charging glyph for Network centered, network content for Battery centered).

- Visual sizing ranges now allow 60%-100% overall size and 40%-125% Wi-Fi/mobile-type size while preserving 5% slider increments; mobile-type labels (5G/5GA/etc.) follow the user overall-size scale, and visual preference commits are serialized on the SystemUI main thread with stale slider callbacks coalesced.

- Battery-center Wi-Fi ring avoidance now follows the actual disconnected optical components of the rendered Wi-Fi drawable instead of reserving the empty corners of one union rectangle; the existing visual clearance and numeric readout avoidance remain unchanged.

- Development display version advanced to **0.0.5** after integrating the accepted Build 617 product/runtime line into `dev`; this remains a pre-release development checkpoint and does not change the planned first formal release target of 1.0.0.

- Project licensing changed from Apache License 2.0 to GNU General Public License v3.0 only (`GPL-3.0-only`); third-party components retain their existing upstream licenses and notice requirements. This repository-only change does not alter APK/runtime behavior.


- Home / Keyguard -> Control Center transition presentation now follows the verified HyperOS native expansion/appearance timeline while Guiyuan bridges only its owned QS_FAKE interval. Final role-6 targets remain read-only root-space witnesses; QS_FAKE may take one fixed, reversible session lease on already-unused native parent width so the existing `statusIcons.paddingEnd` progress writer no longer forces native peer underflow before the HyperOS appearance handoff. Lease-only leading capacity is excluded from transition motion by sampling an end-anchored logical carrier frozen to the native source-carrier width, preventing capacity growth from shifting Guiyuan's motion origin. Mobile exact four-bar geometry remains shape-local rather than stretching the whole participant. Accepted non-charging and charging Home paths preserve native peer motion and final alignment, while unsupported/ambiguous topology fails native.

- Contributor workflow now uses CONTRIBUTING + CURRENT as the daily recovery path, a decision-oriented DEVLOG, direct dev-to-main promotion, and three CI scopes (Light / Runtime / Full); signed work-branch Canary remains demand-driven and independently validates the requested source SHA.


- Refined Preview Sandbox hierarchy so all setting titles (slider and segmented-field titles) share the same primary MIUIX role as native preference titles, while control option rendering stays owned by MIUIX and soft spacing separates groups; simulation and production-renderer behavior are unchanged.
- Diagnostics keeps the balanced mid-density information-card rhythm and Module runtime edge breathing room while leaving Diagnostics & reports unchanged. The background-free Guiyuan identity still uses the same launcher vector and 20-second counterclockwise motion, but is now drawn directly at its final optical size and only rotated, avoiding any post-draw or Canvas scale-up path.


- Unified Preview Sandbox segmented controls to one balanced 300 dp maximum width so hierarchy is conveyed by labels and spacing rather than different control sizes.

- Restored MIUIX-native spacing ownership in Preview Sandbox, introduced compact hierarchical width caps for segmented controls with equal same-level distribution, and renamed the diagnostics framework display to `Modern Xposed API 102`.

- Optically normalized Diagnostics leading icons across level/export/share using MIUIX Normal-weight symbols in a shared slot, and lowered the Home runtime status mark slightly to improve spacing below the master Switch.

- Refined companion-app visual hierarchy: Runtime status marks now balance ring and inner-symbol weight, Diagnostics report actions use MIUIX Normal leading icons, Preview Sandbox spacing follows a compact consistent vertical rhythm, and Features removes the duplicate master switch while separating lock-screen behavior from color-link controls.

- Redesigned the Home Preview Sandbox and its detail screen around compact MIUIX segmented choices, continuous sliders, uninterrupted Network/Battery cards, and fixed preview geometry while preserving the shared production renderer.

- Preview Sandbox now uses a mutually-exclusive MIUIX Mobile/Wi-Fi selector with context-dependent subordinate controls while preserving valid no-SIM + Wi-Fi simulation; no-Internet Wi-Fi resolves the exact HyperOS unavailable drawable family, and the shared renderer places the 5G-Advanced `A` at lower-right in both previews and the real status bar.

- Home now uses a two-card runtime/preview hierarchy: the Runtime card separates user intent from actual Xposed state with fixed-height semantic feedback, heavier unified rounded status marks, and a compact single-line version/short-build identity. Home keeps only one MIUIX Preview Sandbox navigation row with a production-rendered leading preview and navigates to a dedicated secondary page whose contour-tab controls adjust simulated center/signal/battery state; app previews scale mobile-type labels proportionally with the preview viewport while production SystemUI sizing remains unchanged.

- Launcher branding now uses an abstract converging-orbit mark as a true Android adaptive icon: the geometry lives in the foreground vector, the default palette is ink-black on clean white, launcher masks remain system-owned, and Android monochrome/themed icons reuse the same silhouette.

- Floating-navigation Glass follows the pinned MIUIX example material baseline, and the Appearance preview consumes the same production material and content settings as the live bottom navigation. The preview keeps fixed outer bounds and bottom-anchors the navigation sample so switching label modes does not move surrounding settings.
- Home status-icon tint now remains synchronized with the live HyperOS/SystemUI status-icon authority across module Hot Reload and repeated light/dark app/Home transitions, avoiding stale cross-scene tint snapshots while preserving native Battery semantic colors and fail-native behavior.


- Center network presentation reuses authoritative HyperOS Wi-Fi/hotspot, airplane, and no-SIM drawable resources. Native center drawables resolve the verified SystemUI tint presentation variant when externally tinted, are measured with a bounded optical probe, and are drawn directly at final resolved bounds without a final bitmap-resample or project-owned alpha-normalization stage. Battery semantic states continue to use the native battery color policy, while the optional center/mobile color links can intentionally follow the battery ring’s final resolved color. Proportional center size/text-weight parameters remain reserved for later user customization.
- Outer Guiyuan visual weight uses the maintainer-preferred 8.25-unit battery-ring stroke with the accepted 5.39-unit mobile-dot radius and native-style ROUND endpoints; the lower opening is recomputed so ring-to-dot and dot-to-dot edge spacing remains balanced. A shared proportional weight scale remains reserved for future user-facing thickness control.

- Home network replacement keeps native mobile suppression continuous through airplane-mode transitions and event-driven Home re-entry/rebinds, using reversible visual suppression only while the represented Home topology is verified; if a dynamic rebind no longer satisfies the replacement contract, the session fails native instead of leaving a partially active replacement.

- Center mobile-network type labels keep their accepted physical size while using heavier typography and glyph-ink centering for a more balanced `5G` / enhanced-type presentation inside the Guiyuan composition.
- Island motion diagnostics now start bounded frame sampling only when development/Detailed diagnostics are active, stop on the UI thread when Detailed is disabled, and cancel their timeout callback during cleanup; General Canary diagnostics no longer pay the per-frame probe cost.
- Native Wi-Fi replacement now uses one semantic-readiness policy across rendering and suppression: SystemUI Wi-Fi semantics lead, Connectivity only fills an unknown Internet state when Wi-Fi is the current default network, obsolete freshness timestamps are removed, and native Wi-Fi remains visible whenever Guiyuan cannot safely reproduce the current Wi-Fi presentation.
- Connectivity state now consumes authoritative default-network capability callbacks directly on the registered main-thread Handler and ignores stale loss events, avoiding redundant callback reposting and synchronous capability re-query during network transitions.
- Product identity is unified as **Guiyuan / 归元** across the companion app, diagnostics, public documentation, repository-facing text, and distributable APK names. Android application identity moves to `com.chaners.guiyuan`; existing `CombinedStatus*` internal code symbols remain valid implementation names and are not mechanically renamed.
- Home replacement suppresses native Wi-Fi and single-subscription mobile presentation only when Guiyuan can safely reproduce the represented state; unsupported or incomplete multi-subscription presentation remains SystemUI-owned and fails native rather than hiding information.
- Bottom navigation now differentiates selected and unselected items with MIUIX icon weights while preserving the existing navigation colors, layout, and interaction behavior.
- Top app bars now use MIUIX progressive backdrop blur while scrolling content beneath them on supported devices, with the standard solid surface retained as fallback.
- Companion-app transient feedback now uses MIUIX Snackbar, and icon-only SystemUI reload exposes a native MIUIX long-press tooltip without changing the action layout.
- Top-level page navigation now uses MIUIX Cross-Axis pager gesture ownership so horizontal page switching remains available while vertical child content is settling, without adding a second app-owned gesture recognizer.
- Companion-app MIUIX dependencies now track the validated published main-canary snapshot `0.9.4-2afdbb39-SNAPSHOT` from upstream revision `2afdbb39f1aac5747165cc354cafd4b918fa55a5`, with one shared dependency identity used across all MIUIX modules.
- Diagnostic reports now limit log collection to Guiyuan-related runtime/share diagnostics and no longer collect broad third-party application/system share logs.
- Runtime state acquisition now favors authoritative event-driven platform/SystemUI sources and cached process-scoped state instead of repeated querying or polling.
- Wi-Fi and mobile semantic updates are committed before their verified SystemUI emitters proceed so Guiyuan can enter the same UI frame as native icon changes.
- Render-state commits are atomic: incomplete candidates retain the last stable frame, while explicit Hidden/Unavailable states update immediately.
- Home rendering is independent of `BuildConfig.DEBUG`; build channels control diagnostics capability rather than core rendering behavior.
- SystemUI integration preserves native layout, translation, visibility, and animation ownership wherever practical; Guiyuan-specific appearance remains in its own presentation/rendering layer.
- Diagnostics use bounded event-driven snapshots rather than continuous collection, and General mode avoids detailed per-transition diagnostic allocation.
- Hot reload rotates runtime-session identity, replaces the previous hook generation, revalidates compatibility, and restores current host health instead of stacking duplicate generations.
- The app uses native MIUIX components and shared production material definitions for navigation and appearance previews instead of separate visual approximations.
- Diagnostics, app descriptions, and user-facing copy consistently identify Xiaomi HyperOS as the target and use **mobile network / 移动网络** terminology.
- Build and release tooling separates Debug, Canary, and formal Release signing/CI responsibilities; distributable APK filenames use application version/build identity rather than GitHub Actions run numbers, while test-release tags may retain the run number as CI execution metadata.

### Fixed

- Refined the Home runtime-card status mark without altering its established semantic colors: kept the master Switch in its original position, enlarged and strengthened the circular mark, and moved the mark slightly upward.

- Fixed the companion-app Sandbox airplane center disappearing outside the SystemUI process by resolving the native flight-mode drawable from the SystemUI package context; Mobile airplane mode now correctly overrides no-SIM center presentation while retaining the bottom unavailable mark.

- Home Guiyuan now yields its overlay to native notification-shade and Control Center presentation using SystemUI scene-lifetime callbacks, while keeping the structurally valid Home owner persistent underneath. Notification-shade ownership follows actual native motion: active tracking or any positive shade fraction leaves Home, while non-tracking at the native closed boundary (`fraction<=0`) permits Home even when HyperOS asserts `expanded=true` for a heads-up notification. Control Center reacquires Home only after native `visible=false`. This removes HUN disappearance plus shallow/return overlay leakage without arbitrary thresholds, delays, polling, destructive owner teardown, or custom transition motion.
- No-SIM is now a persistent cellular-layer state: the lower mobile signal orbit keeps its unavailable `×` whenever HyperOS reports native no-SIM, even when the center simultaneously shows the native no-SIM glyph or Wi-Fi. This prevents Wi-Fi from erasing SIM-absence semantics while preserving the center's native-priority presentation.
- Home `no_sim` replacement consumes the native `StatusBarIconView.isIconVisible()` result in the same visibility event HyperOS uses to present the icon. The native no-SIM drawable is resolved before replacement suppression is enabled; unresolved resources remain fail-native.
- Home mobile replacement distinguishes live active subscriptions from stale bound mobile roots using Android's active-subscription authority, with existing SystemUI semantic state retained only as a fail-soft fallback. Suppression is scoped to the active Home status-icon group; Control Center / shade mobile presentation remains untouched. During transient duplicate-root rebinds, the existing Home replacement remains stable until topology settles, after which the normal multi-SIM fail-native policy applies without timers or delayed release.
- Static airplane/no-SIM suppression is scoped to the active Home status-icon group instead of globally intercepting every matching `StatusBarIconView`; notification-shade and Control Center steady-state icons therefore remain native while Home replacement is active.
- Native airplane and no-SIM center drawables continue to use their own visible alpha bounds for optical fit. Connected, no-Internet and hotspot Wi-Fi variants now use the same-level connected Wi-Fi drawable as the shared optical-fit reference, so the base Wi-Fi glyph keeps one visual scale while each HyperOS drawable retains its authored viewport and badge relationship. Missing/incompatible reference geometry falls back to the existing per-resource fit. Stable native center assets remain drawn directly at final View pixel bounds with the authoritative HyperOS tint; transition frames retain the existing canonical animation contract.
- HyperOS Wi-Fi resource handling now preserves native no-Internet variants and accepts a narrowly verified applied-hotspot resource when the Wi-Fi semantic model temporarily reports Hidden; ordinary Hidden Wi-Fi never reuses a stale normal-Wi-Fi tag.
- Home Guiyuan monochrome tint follows the live native status-icon tint authority for normal battery-ring state, center network icon, and mobile layer; battery-applied tint remains only a bootstrap/fail-soft fallback when the primary Home tint source is unavailable. Battery semantic states use the SystemUI-resolved native semantic color when available, and the two user color-link controls can explicitly make center/mobile follow the final battery-ring color.
- Center network presentation keeps the validated 100 ms SystemUI-style icon-appearance transition only when the presentation family changes between Wi-Fi, mobile type, airplane mode, and empty/search state. Changes within one family—such as Wi-Fi level/Internet markers or 4G/5G/5GA mobile-type updates—redraw in place without replaying the whole center animation.

- Airplane-mode exit now enters an event-driven mobile reacquisition state: the center airplane/cross clears immediately, four signal dots remain unavailable while HyperOS reports no fresh signal, and cached pre-airplane mobile type/strength is not reused.



- Native mobile suppression now also treats one-root dual-aggregated presentations as replaceable, preventing duplicate dual-row mobile visuals from remaining beside Guiyuan while preserving separate dual-root presentations.
- Airplane-mode center presentation reuses the left-facing HyperOS `stat_sys_signal_flightmode` shape family for parity with the live Home status bar, with its default visual box rebalanced to the accepted Wi-Fi-family width while retaining the shared center `sizeScale`, unavailable mobile dots/cross, and Wi-Fi precedence when Wi-Fi remains active.
- VPN-backed default networks no longer suppress an authoritative HyperOS mobile-type label at startup: Wi-Fi/cellular transports retain precedence, while VPN-only fallback waits for authoritative Wi-Fi absence before showing the mobile type.
- Native Guiyuan tint updates now accept only the currently bound HyperOS status-bar battery view, preventing transient tint states from other `MiuiBatteryMeterView` instances from flashing through during light/dark inversion changes.
- Wi-Fi fallback rendering now uses the same semantic-readiness gate as native Wi-Fi suppression, so unknown OEM/VPN Wi-Fi variants remain fully native instead of being duplicated by an uncertain Guiyuan Wi-Fi projection.
- Wi-Fi strength presentation now preserves all four SystemUI signal levels (0–3) as four distinct visual states using the existing three-path renderer, instead of collapsing native levels 2 and 3 into the same fully lit icon.
- Wi-Fi rendering now consumes the authoritative SystemUI `WifiIcon` resource emitted by the modern Wi-Fi pipeline instead of re-reading the bound `ImageView` tag; signal-level changes and SystemUI no-internet variants therefore update immediately even when Android selects cellular as the default network.
- Hot Reload restore carries only stable presentation ownership/state needed to avoid a transient duplicate Home representation while the new runtime generation re-establishes its active host.
- Hot Reload keeps SystemUI View mutation on the SystemUI main thread: cross-generation transfer carries only stable runtime state and live SystemUI references, while the new generation re-resolves presentation resources instead of synchronously moving module-owned View state from the framework callback thread.
- Hot Reload re-resolves current SystemUI host resources from the live host rather than retaining temporary resolver state across generations, preventing stale or GC-sensitive presentation handles during prepare/detach.
- Runtime health now evaluates presentation-source readiness through the owning `presentationRuntime` subsystem, so tint/scene states that are legitimately not yet observed no longer mark an otherwise healthy runtime as degraded.
- Network hook installation is fail-soft per source so failure in Wi-Fi or mobile resolution no longer tears down the other source.
- Verified network emitter resolution no longer depends on resolving Kotlin `Continuation` by name through the SystemUI ClassLoader, preventing optimized/Canary builds from losing network hooks.
- Wi-Fi state tracking follows the verified HyperOS Wi-Fi collector and registers the relevant root before the native binder proceeds.
- Guiyuan remains visible when HyperOS temporarily hides the native battery container during Wi-Fi/mobile status transitions.
- Airplane-mode presentation follows the authoritative global setting through one event-driven ContentObserver owner; the mobile signal path no longer re-reads or writes airplane state.
- Center presentation now represents the active data connection only: Wi-Fi or mobile type when active, otherwise an explicit empty center, while cellular service state remains in the signal-dot area; transport-first handoff logic avoids transient Wi-Fi/mobile mismatch frames.
- Transparent or uninitialized tint samples no longer blank the Guiyuan renderer.
- Per-app language selection preserves an explicit language choice even when it currently matches the system locale.
- CI signing verification remains pinned to the expected certificate while accepting current Android Build Tools signer output.

### Removed

- Removed the experimental battery-anchor charging-island follower after runtime evidence showed that the battery anchor was not the transition owner.
- Removed the experimental Home owned-slot padding mutation after validation showed that it altered native battery geometry and leaked layout effects into other scenes.

### Engineering

- Build-channel diagnostic flags are now observation-only for the transition stack: Release and Canary share functional hooks, state authority, ownership/lifecycle, and fail-native behavior. Panel callback failures are contained so native HyperOS callbacks still proceed, and Island status authority is installed independently of diagnostic logging.

- Runtime diagnostics preference listening now has explicit lifecycle ownership outside `CombinedStatusModule`, keeping remote-preference registration and cleanup bounded across Hot Reload generations.
- Battery state acquisition moves from an app-owned `ACTION_BATTERY_CHANGED` receiver to the verified HyperOS `MiuiBatteryMeterView.onBatteryLevelChanged` callback with a dedicated runtime owner, leaving `StatusBarStableSession` responsible only for host/anchor diagnostics.
- Public documentation and contribution surfaces use **Guiyuan / 归元** as the product identity; repository/artifact identity follows `Guiyuan`, while existing `CombinedStatus*` internal code symbols remain unchanged where they describe implementation concepts.
- Pull-request CI now classifies ready `main` changes by affected paths, keeping documentation-only maintenance on Light validation while preserving Full validation for build, CI, dependency, tooling, and runtime-affecting stable-boundary changes.
- Stable GitHub Release notes now omit the changelog's Engineering section while retaining the complete engineering record in `CHANGELOG.md`; the dev-to-main readiness workflow is labeled explicitly in Actions.
- Contributor rules define MUST/SHOULD/MAY boundaries, fail-native fallback, staged ownership migration, changelog discipline, the normal `dev` contribution target, and private security-reporting expectations.
- Pull requests are explicitly treated as proposals: automated checks provide validation evidence, while final acceptance and any required maintainer-side device validation remain maintainer decisions.
- Merged PR branches now rely on GitHub's repository-level automatic head-branch deletion instead of a duplicate project-maintained cleanup workflow.
- Release automation now restricts signed test releases to `dev` or `main` and stable releases to `main`, keeping experiment/work branches in CI artifacts rather than GitHub Releases.
- Promotion readiness now runs as a lightweight post-Build workflow, so readiness infrastructure failures cannot turn an otherwise successful APK Build red; readiness still gates `dev -> main` through the same CI/device/changelog conditions.
- CI validation now separates Light, Fast, Integration, and Full scopes: ordinary work branches prove changes with Debug, trusted `dev` runtime integration builds signed Canary only, and full Debug+Canary validation is reserved for build-system or stable-boundary risk.
- Dependabot version updates now target `dev`; minor/patch updates are grouped per ecosystem to reduce PR noise, major updates remain individually reviewable, and generated dependency PRs are not auto-merged by default.
- Contribution governance uses risk-based routing: repository text/governance and repository automation may move independently of runtime promotion when their own validation passes, shared `main` changes are history-preserving back-synced into `dev`, normal work reuses bounded active `feat/*`/`fix/*` branches instead of creating one branch per sub-task, device validation is checkpoint-based, hotfixes return to `dev`, and merged short-lived branches are cleaned up automatically.
- Upstream dependency adoption uses relevance classes, explicit maturity levels, exact-revision CI/artifact gates, isolated Canary validation, and a bounded work branch only when the branch-admission rules require one.
- Project source and contributions are licensed under Apache License 2.0, with third-party components retaining their upstream license obligations.
- Public/reproducible development now uses the checked-in official Gradle 9.7.1 Wrapper with distribution/integrity validation, commit-pinned GitHub Actions, secret-free pull-request validation, hardened ignore rules for local signing/environment artifacts, least-privilege workflow credentials, dependency-update automation, and explicit third-party dependency notices.
- Stable release automation is fail-closed: formal releases must come from a prepared `main` commit with a matching dated changelog section, pass target-profile/tests/Xposed-metadata/non-debuggable/signature checks, and use application release/build identity rather than CI run numbers for distributable APKs.
- Runtime architecture is moving toward explicit `Host -> HostSession -> owned resources` boundaries with required cleanup across host replacement, SystemUI recreation, and hot reload.
- Live SystemUI properties follow a single-writer rule; native layout geometry, Guiyuan visual geometry, transition geometry, and optical adjustment remain separate responsibilities, and observation does not itself grant write ownership.
- Compatibility-sensitive hooks are tied to verified members from the pinned HyperOS SystemUI `17.03.260226.r` target profile and are validated against the live runtime when ownership matters.
