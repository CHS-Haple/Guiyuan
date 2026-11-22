# Engineering Decisions

This file keeps only durable decisions that are likely to matter to future
design or regression work. It is not a build log.

Detailed implementation history, rejected experiments, CI runs, and device
investigation notes remain available in Git history.

## Verified native contracts before dynamic DEX lookup

**Decision:** prefer direct access to verified native SystemUI contracts and
binary-specific compatibility profiles. Do not ship runtime DEX scanning solely
because a future ROM might rename compiler-generated classes.

**Why:** matching a coroutine class by field names and method shape cannot
establish its signal semantics, subscription authority or lifecycle. Scanning
adds another runtime failure path, JNI distribution and licensing obligations
without reducing the need to inspect the actual target. A broader module that
hooks many unrelated, obfuscated applications has a different cost-benefit
balance from Guiyuan's bounded SystemUI integration.

**Consequence:** preserve the [DexKit experiment #1801](https://github.com/CHS-Haple/Guiyuan/pull/1801)
as reviewable evidence, not an accepted dependency. For a newly supported ROM,
first verify its exact SystemUI binary, native event sources and owner lifetime.
Consider narrowly scoped runtime discovery only after a concrete name-drift
case shows a unique, semantically justified match and measurable maintenance
benefit over updating the verified profile. Keep a single Hook owner and fail
native for missing or ambiguous matches. Developer-side binary analysis tools
need not become APK runtime dependencies.

## Fail native protects ownership, not observer completeness

**Decision:** absent network/battery/tint observer data or partially installed
mobile collectors are diagnosable visual degradation, not proof that the native
host or its writer is unsafe. A steady Home replacement may be visible when
its complete render model and verified native owner can safely cut over.
Never fabricate a subscription or convert unknown signal into an observed
zero-strength value. A **throwing runtime source callback is different**:
existing source hooks invoke downstream state and render callbacks, so do not
treat an arbitrary exception from that combined chain as a harmless read
error. Until origin is proven, it remains a global safety failure.

**Boundary:** native host identity, exact represented slots, lifecycle,
masking, capacity and rollback receipts remain hard prerequisites. Ordinary
Home, shared Keyguard/AOD family and CC may each release only their invalid
surface. CC and Keyguard/AOD source failures must release their affected family
and dependent projection. If any actual native owner retirement is
unconfirmed, escalate to the whole-module runtime stop instead of asserting
successful fallback.

**Global stop:** a true cross-scene state/AOD callback failure or unconfirmed
local native release atomically fences further Home, Keyguard/AOD and CC
acquisition before independent, main-thread owner cleanup. All surviving native
receipts must be gone to report success. A complete manual OFF then ON is the
only same-generation retry. Incomplete cleanup requires a SystemUI restart;
runtime stop never writes the saved user preference or disables LSPosed.
Retired Hot Reload callbacks cannot release a new-generation owner.

**Hot Reload exception:** early Home prearm is a native masking operation
before renderer readiness. It still needs verified mobile and battery
installation; this is deliberately stricter than the ordinary steady
Home cutover and must not be relaxed merely because data degradation is allowed.

## Native host over duplicate participant

**Decision:** reuse a verified native SystemUI host whenever it already owns the
required layout and motion.

**Why:** a second permanent status participant creates a second layout identity
and forces width/position/animation handoff between two writers.

**Consequence:** Home renders through the verified native end-side carrier. A
new permanent participant requires new evidence that the native-host model
cannot satisfy the requirement safely.

## Native facts stay authoritative

**Decision:** consume native scene, hide, appearance, connectivity and lifecycle
facts instead of rewriting them to preserve Guiyuan state.

**Why:** overriding a native fact usually creates a second state machine and
then requires geometry/timing compensation downstream.

**Consequence:** native facts feed Guiyuan policy. Unsupported or ambiguous
state falls back to native presentation rather than being repaired with guessed
thresholds or delays.

## One writer per mutable surface

**Decision:** a live layout, translation, alpha, visibility, tint, animation or
attachment property has one runtime writer.

**Why:** parallel writers produce ordering-dependent behavior that is difficult
to restore and debug.

**Consequence:** observation does not grant ownership. A new native write
requires an explicit owner, lifecycle, conflict check and exact restoration
path.

## Geometry responsibilities stay separate

**Decision:** native occupancy, Guiyuan visual geometry, transition geometry and
optical adjustment are distinct contracts.

**Why:** using one width/offset for several responsibilities caused
scene-specific compensation chains and unstable transition behavior.

**Consequence:** visual scale does not automatically become native slot width;
optical adjustment is not native translation; transparent drawing overflow does
not change native slot geometry.

## Represented native views stay reversible

**Decision:** suppress only the minimum represented native presentation and
preserve the native View lifecycle whenever possible.

**Why:** native Views still carry useful state, tint and lifecycle behavior even
when Guiyuan replaces their pixels.

**Consequence:** ignored-slot changes and clip masks are scoped to the verified
consuming lifetime, preserve pre-existing platform state, restore only
module-owned deltas and fall back to native presentation on writer ambiguity.

## Keyguard and AOD share one family owner

**Decision:** verified same-host Keyguard/AOD presentation uses one host-scoped
owner and one renderer that retarget scene semantics.

**Why:** restoring native state between two semantics on the same host creates
an artificial native interval and duplicate ownership.

**Consequence:** same-host retarget may keep valid owned suppression/reservation
state. Home remains a separate owner; cross-host handoff cannot borrow
source-host layout readiness.

## AOD never becomes a Control Center source

**Decision:** AOD is a projected family scene but not a source for Control
Center transition projection.

**Why:** the verified transition contract is based on eligible Home or Keyguard
presentation and native Control Center routing.

**Consequence:** unknown/AOD source conditions fall back to native presentation
for the bridge instead of inventing a source.

## Dual-SIM row shape and SIM identity are separate facts

**Decision:** show two Guiyuan signal rows only after an explicit user choice
and two authoritative active SIM subscriptions. When only one SIM is recognized,
use the existing single-row presentation without clearing the user's optional
preference. For Control Center targets, read native View structure and classify
each visible row's actual geometry independently; bind a row to a SIM only when
a trusted per-subscription View/semantic link proves it.

**Why:** a composite icon's upper/lower row order is not an authoritative
SIM1/SIM2 mapping. Native View hierarchy, renderer shape, row index, and
dynamically generated child IDs do not independently prove a row's subscription
identity. SystemUI variants and modified View trees can present the data SIM
first, reorder rows, or collapse them into one drawable without per-SIM
provenance. Only an independently verified per-subscription semantic link can
establish SIM ownership.

**Consequence:** never infer a SIM identity from row index, bar/dot shape,
third-party package name or generated ID. Two separately verified subscription
roots may use independent per-row exact morph. A single composite root with two
verified four-glyph optical rows may use **grouped geometry-only** morph with
shared X and row-specific native Y; it must not assign SIM identity to either
native row. If geometry or source provenance is uncertain, retain the bounded
no-target/native fallback. Do not add a second SystemUI signal observer or
mutate final Control Center.

## Battery colors do not depend on SIM layout

**Decision:** keep one shared battery color scheme catalog, one applied scheme
and one set of saved colors for single- and dual-SIM presentation.

**Why:** SIM count changes the signal layout, not the meaning of charging, low
battery or power-saving colors. Separate color banks caused scheme-selection and
actual-color disagreement.

**Consequence:** retain independent signal/layout geometry but read and write
battery colors through one global preference bank. On upgrade or legacy V1
restore, the currently selected mode's colors win; remove obsolete dual-color
keys without modifying other profile settings.

## Control Center final surface remains native

**Decision:** Guiyuan may project its compact semantics only through the bounded
QS_FAKE transition bridge; the fully expanded Control Center remains native.

**Why:** SystemUI already owns fake/final surface membership, appearance, motion
and final-only participants.

**Consequence:** expansion progress is motion context, not a project visibility
threshold. Guiyuan does not suppress the final native surface or take over final
alpha/translation.

## QS_FAKE native width is a SystemUI-only property

**Decision:** The fake status-bar carrier width, measured icon-row reservation
and visual motion have separate owners. Guiyuan does not resize or restore
`QS_FAKE.system_icon_area.layoutParams.width`; SystemUI determines it from
`realSystemIcons`. Guiyuan may only reserve measured native peer space through
its independently reversible `paddingEnd` receipt.

**Evidence:** B1898/B1899 rotation logs show native width 587 → 1361 while
Guiyuan held a prior 818-wide lease, followed by local Fail native and
reacquisition. The verified native controller's
`updateFakeStatusIconsSize()` already writes that width, making the competing
lease fundamentally unsafe.

**Consequence:** Remove the fake width lease, hidden-cycle release/restore,
writer-conflict fallback and capacity-as-motion logic. Wait for native
layout completion instead of interpreting a native orientation rewrite as
a competing Guiyuan owner; preserve independent restoration checks for
ignored slots, clip masks and end padding. CI checks alone do not establish
whether the first real-device swipe is visually smooth.

## Native QS_FAKE pre-draw cutover after an explicit icon-row relayout

**Decision:** When a CC session activates ignored slots on an already-sized
native fake carrier, request the native icon-row layout explicitly and resolve
visual cutover after the complete native traversal at pre-draw. Do not treat
a child's `onLayout` as evidence that its parent's `isLayoutRequested`
flag has already cleared. The one pending pre-draw listener is scoped to the
session and removed on readiness or cleanup; it does not change native
width, motion, final CC surface or persistent preferences.

**Evidence:** The B1900 device video showed a persistent native-only
pressed-header state. Its detailed log recorded
`preLayoutVisualMask active=false` at 18:47:49 but no `layoutReady`,
CC activation, or Fail native afterward. B1899 had immediately triggered
a native icon-row layout by writing fake width; B1900 correctly removed that
unsafe width writer but did not replace the layout event dependency.

**Consequence:** Keep SystemUI sole width owner. Recover the bounded
native-layout event, validate native row capacity before masking, and retain
exact rollback receipts. Unverified native frame geometry still releases
only its affected presentation. A passing CI is not a claim of device
acceptance.

## Home carrier position follows native geometry, not container size

**Decision:** Use the actual `battery_icon_container` native window location
relative to `MiuiStatusBatteryContainer` for the compact Home overlay x.
The previously assumed end-aligned `BatteryContainer.width - slotWidth`
is an invalid location source if the native carrier translates within an
unchanged host during Control Center rotation/handoff. Keep the separate
native Home privacy-chip anchor behavior and all native ownership rules.

**Evidence:** B1901 video shows the correct left-to-right sequence
network-speed, Bluetooth, silent, Guiyuan before opening CC; rotating inside
CC then swiping up draws the original network-speed and peers across the
Home Guiyuan ring. In B1901 logs, the CC fake owner had activated via
native pre-draw and continued reporting successful transition-reservation
clears; no Fail native event explains the Home overlay displacement.
The Home overlay used `SteadyLayoutResolver` relative to host width and
observed onLayout but not native view-only translations.

**Consequence:** Observe a single session-owned native pre-draw event and
reposition only Guiyuan's overlay when the physically measured native
carrier anchor changes. Also re-read the native position on Home eligibility
return and battery-parent layout. Do not write native View translation,
width, padding beyond existing owned reservation, or animations. Reject
an invalid/out-of-bounds carrier geometry instead of inventing a pixel
offset. The exact ROM4.0.4 runtime transform remains a device-test
evidence gap, not a proven static SystemUI binary behavior.

## CC zero-fraction is a Home ownership boundary

**Decision:** The CC projected owner hides Home only when native panel
visibility, positive native expansion progress, and projected readiness
all agree. A terminal native progress of zero returns Home ownership
immediately even if `onVisibleChanged(false)` has not arrived yet.
A subsequent late `ready=true` cannot suppress Home while native
progress remains zero. Hidden visibility resets motion/readiness state
for the next cycle. This remains a UI-thread transition and introduces
neither polling nor an extra hook.

**Evidence:** The B1901 real-device regression is gesture-speed dependent:
partial pulls preserve the combined icon, fast full upward swipes usually
work, slow upward swipes reaching the closed endpoint reveal the native
status icons. In the source, `CcTransitionOwner` already stops at native
fraction zero, while `GyModule.onCcExpansion` did not return Home
eligibility there and `onCcProjectionReadyChanged(true)` could continue
hiding it until a later visible-false event.

**Consequence:** One pure `CcHomeHandoffPolicy` is used by the native
fraction, visibility and projected-readiness callbacks. Keep Home
mask/slot ownership separate from CC projection ownership and maintain
the existing native fallback on actual unsafe conditions. Tests cover
slow-close, late-ready, partial gesture and fast-close/reopen traces.
Real-device validation still required.

## CC terminal visual exit and Home visibility are separate owners

**Decision:** During a confirmed native CC closing gesture from HOME,
read the actual native fake/final header root visibility. If BOTH roots
are no longer visible, retire only the temporary QS_FAKE transition
session and return Home visibility authority to its existing coordinator,
even if the CC expansion delegate still reports a stale positive fraction.
Keep the terminal witness latched until the next native visible cycle,
including across duplicate `visible=true` events. Use existing CC and
Home pre-draw observation boundaries; do not invent time/alpha thresholds,
change native geometry or add another Xposed hook.

**Evidence:** After CC landscape→portrait, B1902/B1903 device feedback
showed native Home peer icons before the combined icon returned.
B1903 follow-up runtime report did not record the actual blank frames.
A source audit disproved the earlier claim that the transition masks
the Home RenderView. `CcRenderSession.transitionSourceSnapshot()` supplies
the **QS_FAKE** RenderView to `CcTransitionOwner`, which clips and restores
only that source. The Home RenderView is controlled separately by
`HomeRenderSession.controlCenterAllowsHome`, updated by
`GyModule.reconcileCcHomeAuthority()`. B1903's CC-only pre-draw
terminal observer can cease running after the CC root stops drawing,
even while Home's native row remains visible.

**Consequence:** Reuse Home's existing native pre-draw to check the
same read-only terminal predicate that CC observes, authenticate that
the drawing View belongs to the live Home session (NOT the QS_FAKE
source), and notify one Home visibility coordinator. The scoped
transition session still restores its QS_FAKE clip, reservation and
overlay on retirement. A bounded terminal event reports whether the
witness was `home-pre-draw` or `cc-pre-draw`. The B1904 device test
must establish that the actual blank interval meets this predicate;
if not, collect the native visible/fraction/owner state at that exact
boundary rather than adding speculative fallback.

## CC frozen source geometry is viewport-scoped

**Decision:** A live CC transition session with frozen Home source and
cached fake-carrier root offset is reusable only inside the same native
root viewport geometry. A changed portrait/landscape root width/height
invalidates the old temporary owner, restores its scoped masks and
reservations, then recaptures the new source/final/carrier coordinates.
Do not mutate native geometry to force the original root size.

**Evidence:** Previously `CcTransitionOwner.Session.matches()` compared
only View identities, whereas `FrozenSourceGeometry` and
`fakeCarrierRootOffsetX` were captured once per session. Native
SystemUI can retain these Views across orientation, leaving the
interpolator with a portrait origin and landscape targets. B1901
device video reproduced a position error after rotating within CC
before swiping upward; detailed diagnostics did not include the
post-rotation root coordinates, so device validation remains required.

**Consequence:** Reset the temporary source frame when native root
dimensions change; the native View tree, final coordinates and
original interpolation remain authoritative. Unit tests cover
portrait→landscape and landscape→portrait reuse rejection.

## Native resource identity is preferred

**Decision:** when a verified native drawable/resource expresses the needed
semantic, preserve its resource/presentation path instead of recreating it
through project-owned raster processing.

**Why:** custom rasterization, grayscale/alpha remaps and screenshot-fitted
constants diverge from native rendering and are hard to maintain across
variants.

**Consequence:** keep semantic identity, native Light/Dark/Tint selection,
optical measurement, Guiyuan placement and final Drawable rendering as separate
concerns.

## Diagnostics are observation only

**Decision:** diagnostic flags, probes and log levels never decide functional
hook installation, state authority, ownership or fallback.

**Why:** Canary/Release must not have different runtime semantics merely because
one build logs more.

**Consequence:** diagnostics are event-driven and bounded. Fields must come from
real observations/calculations; do not manufacture proof values, pass rates, or
synthetic success events.

## Diagnostics loads on entry

**Decision:** diagnostics waits for the incoming navigation transition to settle
before collecting logs, retains the last in-process snapshot, and uses MIUIX
pull-to-refresh for manual refresh feedback.

**Why:** collecting logs during navigation can compete with page motion. The
previous completed snapshot remains visible until the next capture is ready.

**Consequence:** keep the last completed snapshot in `GyApp`'s remembered state.
On the first capture, display a MIUIX progress indicator and loading label until
a snapshot exists. The summary and visible log rows then enter with the same
Folme spring, a 28 dp upward reveal and progressive alpha, staggered by row
without a separate delay. Existing entries remain visible during manual refresh,
and the pull-to-refresh control supplies refresh feedback.

## Hot Reload retires the old generation first

**Decision:** Follow Modern Xposed API 102: save neutral V10 state and release
old View, callback and native presentation owners on the SystemUI main thread
before accepting generation retirement. Reuse the verified host Hook only after
the old owner has left; never transfer an old module callback to fake continuous
rendering.

**Why:** An old-generation callback cannot safely outlive its ClassLoader
or substitute for a framework-owned frame-atomic visual handoff. Carrying old
listeners through the V9 transfer payload breaks lifetime ownership even if
the UI appears seamless.

**Consequence:** new owners revalidate live host identity before acquiring
runtime resources; native battery/network may briefly display between
generations. A refused/failed transfer must report restart rather than claim
rollback of partial native teardown. Queued cleanup may be canceled; cleanup
already running must finish. V9's payload embeds an old-generation Runnable and
is rejected before restoring Views; upgrading from V9 requires a SystemUI
restart rather than running that callback in the new ClassLoader. Older neutral
payload formats retain their existing validation rules.

## Maintainability favors useful boundaries over uniform structure

**Decision:** names, helpers and types exist for maintenance value, not
architectural symmetry.

**Why:** modifier-heavy names, suffix-stacked types, wrapper layers, boolean
walls and proof-only tests increase edit cost without improving correctness.

**Consequence:** prefer concise scope-aware names, natural comments, explicit
lifecycle state when necessary, and deletion/inlining when an abstraction
carries no real ownership, compatibility, reuse or policy boundary.
