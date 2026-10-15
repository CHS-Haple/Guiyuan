# Engineering Decisions

This file keeps only durable decisions that are likely to matter to future design or regression work. It is not a build log.

Detailed implementation history, rejected experiments, CI runs, and device investigation notes remain available in Git history.

## Native host over duplicate participant

**Decision:** reuse a verified native SystemUI host whenever it already owns the required layout and motion.

**Why:** a second permanent status participant creates a second layout identity and forces width/position/animation handoff between two writers.

**Consequence:** Home renders through the verified native end-side carrier. A new permanent participant requires new evidence that the native-host model cannot satisfy the requirement safely.

## Native facts stay authoritative

**Decision:** consume native scene, hide, appearance, connectivity and lifecycle facts instead of rewriting them to preserve Guiyuan state.

**Why:** overriding a native fact usually creates a second state machine and then requires geometry/timing compensation downstream.

**Consequence:** native facts feed Guiyuan policy. Unsupported or ambiguous state falls back to native presentation rather than being repaired with guessed thresholds or delays.

## One writer per mutable surface

**Decision:** a live layout, translation, alpha, visibility, tint, animation or attachment property has one runtime writer.

**Why:** parallel writers produce ordering-dependent behavior that is difficult to restore and debug.

**Consequence:** observation does not grant ownership. A new native write requires an explicit owner, lifecycle, conflict check and exact restoration path.

## Geometry responsibilities stay separate

**Decision:** native occupancy, Guiyuan visual geometry, transition geometry and optical adjustment are distinct contracts.

**Why:** using one width/offset for several responsibilities caused scene-specific compensation chains and unstable transition behavior.

**Consequence:** visual scale does not automatically become native slot width; optical adjustment is not native translation; transparent drawing overflow does not change native slot geometry.

## Represented native views stay reversible

**Decision:** suppress only the minimum represented native presentation and preserve the native View lifecycle whenever possible.

**Why:** native Views still carry useful state, tint and lifecycle behavior even when Guiyuan replaces their pixels.

**Consequence:** ignored-slot changes and clip masks are scoped to the verified consuming lifetime, preserve pre-existing platform state, restore only module-owned deltas and fall back to native presentation on writer ambiguity.

## Keyguard and AOD share one family owner

**Decision:** verified same-host Keyguard/AOD presentation uses one host-scoped owner and one renderer that retarget scene semantics.

**Why:** restoring native state between two semantics on the same host creates an artificial native interval and duplicate ownership.

**Consequence:** same-host retarget may keep valid owned suppression/reservation state. Home remains a separate owner; cross-host handoff cannot borrow source-host layout readiness.

## AOD never becomes a Control Center source

**Decision:** AOD is a projected family scene but not a source for Control Center transition projection.

**Why:** the verified transition contract is based on eligible Home or Keyguard presentation and native Control Center routing.

**Consequence:** unknown/AOD source conditions fall back to native presentation for the bridge instead of inventing a source.

## Dual-SIM row shape and SIM identity are separate facts

**Decision:** show two Guiyuan signal rows only after an explicit user choice and two authoritative active SIM subscriptions. When only one SIM is recognized, use the existing single-row presentation without clearing the user's optional preference. For Control Center targets, read native View structure and classify each visible row's actual geometry independently; bind a row to a SIM only when a trusted per-subscription View/semantic link proves it.

**Why:** a composite icon's upper/lower order is not necessarily physical SIM1/SIM2. In [HyperCeiler's public dual-row implementation](https://github.com/ReChronoRain/HyperCeiler/blob/5e4686069dd7ab1f3697e256d5fc7d68fb73e317/library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/statusbar/mobile/DualRowSignalHookV.kt), `getSignalLevelsForRender` orders the **data SIM first**, then the physical slot; [its base hook](https://github.com/ReChronoRain/HyperCeiler/blob/5e4686069dd7ab1f3697e256d5fc7d68fb73e317/library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/appbase/systemui/StatusBarHook.kt) assigns injected child IDs with `View.generateViewId()`. These are implementation details, not a native per-SIM View contract. Other modules may invert dot/bar order or provide a single composite drawable with no individual subscription provenance.

**Consequence:** never infer a SIM identity from row index, bar/dot shape, third-party package name or generated ID. Two separately verified subscription roots may use independent per-row exact morph. A single composite root with two verified four-glyph optical rows may use **grouped geometry-only** morph with shared X and row-specific native Y; it must not assign SIM identity to either native row. If geometry or source provenance is uncertain, retain the bounded no-target/native fallback. Do not add a second SystemUI signal observer or mutate final Control Center.

## Control Center final surface remains native

**Decision:** Guiyuan may project its compact semantics only through the bounded QS_FAKE transition bridge; the fully expanded Control Center remains native.

**Why:** SystemUI already owns fake/final surface membership, appearance, motion and final-only participants.

**Consequence:** expansion progress is motion context, not a project visibility threshold. Guiyuan does not suppress the final native surface or take over final alpha/translation.

## Capacity and motion are different

**Decision:** QS_FAKE carrier capacity, peer reservation and visual motion are separate responsibilities.

**Why:** using leased measurement width as motion geometry moves the visual carrier and creates scene-wide jumps.

**Consequence:** capacity may be leased only inside the verified parent and released at the native lifecycle boundary; the one reservation writer may change occupancy, but leased leading capacity never becomes motion displacement.

## Native resource identity is preferred

**Decision:** when a verified native drawable/resource expresses the needed semantic, preserve its resource/presentation path instead of recreating it through project-owned raster processing.

**Why:** custom rasterization, grayscale/alpha remaps and screenshot-fitted constants diverge from native rendering and are hard to maintain across variants.

**Consequence:** keep semantic identity, native Light/Dark/Tint selection, optical measurement, Guiyuan placement and final Drawable rendering as separate concerns.

## Diagnostics are observation only

**Decision:** diagnostic flags, probes and log levels never decide functional hook installation, state authority, ownership or fallback.

**Why:** Canary/Release must not have different runtime semantics merely because one build logs more.

**Consequence:** diagnostics are event-driven and bounded. Fields must come from real observations/calculations; do not manufacture proof values, pass rates, or synthetic success events.

## Diagnostics loads on entry

**Decision:** diagnostics waits for the incoming navigation transition to settle before collecting logs, retains the last in-process snapshot, and uses MIUIX pull-to-refresh for manual refresh feedback.

**Why:** collecting logs during navigation can compete with page motion. The previous completed snapshot remains visible until the next capture is ready.

**Consequence:** keep the last completed snapshot in `GyApp`'s remembered state. On the first capture, display a MIUIX progress indicator and loading label until a snapshot exists. The summary and visible log rows then enter with the same Folme spring, a 28 dp upward reveal and progressive alpha, staggered by row without a separate delay. Existing entries remain visible during manual refresh, and the pull-to-refresh control supplies refresh feedback.

## Hot Reload is a generation handoff

**Decision:** Hot Reload replaces one bounded runtime generation with another rather than stacking hooks/owners or intentionally returning through an intermediate native cycle.

**Why:** duplicate generations and unnecessary restore/reacquire intervals create flashes, stale references and competing writers.

**Consequence:** old-generation state is released at the handoff boundary, new state re-resolves live hosts/resources, and normal detach/failure still restores only module-owned state.

## Maintainability favors useful boundaries over uniform structure

**Decision:** names, helpers and types exist for maintenance value, not architectural symmetry.

**Why:** modifier-heavy names, suffix-stacked types, wrapper layers, boolean walls and proof-only tests increase edit cost without improving correctness.

**Consequence:** prefer concise scope-aware names, natural comments, explicit lifecycle state when necessary, and deletion/inlining when an abstraction carries no real ownership, compatibility, reuse or policy boundary.
