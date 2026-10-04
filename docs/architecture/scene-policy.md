# Scene capability policy

This document complements [layout-policy.md](layout-policy.md).

The layout policy owns shared Guiyuan visual calculations. The scene policy owns only scene capability classification and motion ownership.

## 0.0.3 architecture status

The capability map below describes the verified runtime-scene contract for the current 0.0.3 development line. Historical carrier experiments remain evidence only and do not override the accepted 0.0.3 ownership model.

The permanent extra-participant / occupancy-handoff architecture explored by Builds 386-393 remains **superseded for current work**. Its runtime observations remain valid historical evidence.

Current 0.0.3 work must follow `docs/development/CURRENT.md`, `docs/development/ROADMAP.md`, and `docs/architecture/README.md`. Capability changes must describe the architecture actually validated by the current carrier/presentation contract.

## Rule

A scene capability may define:

- whether Guiyuan renders on the surface;
- whether rendering is projected against native geometry;
- who owns motion;
- the current evidence maturity.

A scene capability must not define per-scene size formulas, width-difference corrections, offsets, or translation compensation.

Classification is not permission to mutate SystemUI. Runtime integration still requires a verified host, lifecycle, state source, and failure path.

## Current capability map

| Scene | Render mode | Motion ownership | Evidence |
| --- | --- | --- | --- |
| Home stable | PROJECTED | NONE | Runtime verified |
| Notification-shade transition | NATIVE_ONLY | SYSTEM_UI | Runtime lifetime verified |
| Control Center transition bridge | PROJECTED | SYSTEM_UI | Build 430 device-verifies top-level ControlCenterFakeStatusIcons fake/final ownership; Build 431 projects on its overlay |
| Control Center fully expanded | NATIVE_ONLY | SYSTEM_UI | Exact-target fake/final appearance ownership is verified; accepted runtime keeps the final surface native-only |
| Keyguard | PROJECTED | SYSTEM_UI | Build 456 is maintainer device-accepted with a separate opt-in Keyguard host/render/presentation adapter |
| AOD | PROJECTED | SYSTEM_UI | Build 625 candidate; exact-target host/authority verified, device continuity pending |

The map fails closed outside the verified Home / opt-in Keyguard steady paths and the bounded Control Center transition bridge. Unsupported scenes remain native rather than receiving a partial Guiyuan implementation. A verified transition carrier is not, by itself, permission to keep Guiyuan visible as a fully expanded panel surface.

## Home stable

Home stable remains the primary persistent rendering scene. Build 420 additionally runtime-verifies a usable Control Center carrier and readiness-ordered handoff mechanism; that evidence is now scoped to the transition bridge rather than the fully expanded endpoint.

Its PROJECTED mode means the Guiyuan visual is anchored from verified native geometry while the native slot, native motion, and surrounding layout remain SystemUI-owned.

## Notification shade and Control Center

The pinned target separates these two panel paths.

### Notification Shade

Notification Shade remains **NATIVE_ONLY**: this target does not present the status-icon row there, so Guiyuan must not invent one.

The Home render must inherit the native Home end-side presentation lifecycle instead of deriving its own visibility from panel motion. Exact-target source verifies:

`StatusBarVisibilityInteractor.shouldHomeStatusBarBeVisible`
→ `HomeStatusBarViewModelImpl.isSystemInfoVisible`
→ `systemInfoCombinedVis`
→ `HomeStatusBarViewBinderInjector`
→ `mEndSideContent = R.id.system_icons`.

`showEndSideContent()/hideEndSideContent()` owns the native alpha / visibility / translation transition of `system_icons`. The exact `system_icons` root is `MiuiStatusBatteryContainer`.

Current rules:
- Home Guiyuan renders as one module-owned direct child of `MiuiStatusBatteryContainer(system_icons)`; the child has no native measurement authority and inherits the carrier's native end-side alpha/visibility/translation naturally;
- Notification Header expansion remains useful motion evidence but is **not** a Guiyuan Home-visibility authority;
- Battery `MiuiBatteryMeterView.mStatusBarState`, global Keyguard state, generic Shade expansion state, and local fraction thresholds are not Home-visibility authorities;
- no project-local Notification-Shade visibility Hook, timing threshold, delay, polling loop, or reconstructed panel state machine is permitted;
- the parent `MiuiNotificationStatusContainer / system_icon_area` remains the HostSession discovery/ownership boundary, while the visual carrier is the verified animated `system_icons` child.


### Control Center

Control Center is split into two ownership phases.

**Partial pull / transition bridge — PROJECTED**
- the source steady scene may be Home now and Keyguard later;
- Build 420 proves that source geometry plus readiness-ordered handoff can preserve continuity, but Build 428 proves the selected `realSystemIcons` source itself is hidden during current Control Center ownership and cannot be the active display host;
- Builds 425-427 place the compact presentation inside child `QS_FAKE.system_icon_area`; device evidence rejects that child-carrier implementation;
- exact-target review still verifies the distinct top-level `ControlCenterFakeStatusIcons` presentation and SystemUI-owned Header translation/fake-to-final alpha;
- Build 430 device evidence verifies that the top-level fake View remains visible in normal and charging-island transitions and that HyperOS performs fake->final handoff by changing the root alpha while the child statusBarArea stays visible;
- Build 431 uses `ControlCenterFakeStatusIcons.overlay` as the transition visual host but device evidence rejects clip-only suppression because represented Wi-Fi/mobile layout occupancy remains and creates a large gap;
- Build 432 keeps the **root overlay** as the Combined visual carrier and proves the fake child can use the shared reversible compact-layout owner, but device evidence rejects preparing that owner from each Control Center visible cycle; Build 433 moves preparation into the Fake-root lifetime, Build 434 establishes compact readiness after first native root layout, and Build 435 separates render readiness from compact lifetime so transient layout loss on an attached, already-prepared Fake root may hide the Combined overlay but must not restore raw native Fake; cleanup remains Fake-root detach, feature disable, host replacement, Hot Reload, or a genuinely unprepared failure state;
- Build 439 separates Hot Reload restoration from cold-start prearm: a transferred Fake root that is already attached and laid out is restored directly from the main-thread Hot Reload task while outside native layout, so the existing compact owner can request its native status-icon layout from a valid scheduling boundary. Hosts without that lifecycle contract fall back to the existing first-native-layout prearm; no timing threshold or extra presentation writer is introduced.
- Build 440 separates pre-compact **visual suppression** from compact **layout ownership**. A deferred QS_FAKE session immediately applies its existing reversible native clip masks while keeping the projected Combined overlay not-ready; Home remains the visual fallback until the next native status-icon measure/layout establishes compact slot exclusion, after which ownership hands off to the QS_FAKE Combined overlay. This avoids raw/mixed native visuals without exposing the stale-occupancy clip-only layout rejected in Build 431.
- Build 441 makes Hot Reload a **generation-to-generation presentation handoff** rather than a native fallback cycle. The old generation remains presentation owner until the new generation has installed its hooks and reaches the main-thread restore transaction; old visual/mask/reservation state is then released without an intermediate layout request, the new owner attaches in the same main-thread turn, and transferred QS_FAKE compact readiness may be adopted when it was already proven before reload. This removes the deliberate native/layout intermediate state that caused Home flashing, peer-icon reflow and immediate-pull blanking, while preserving normal detach/Fail-native restoration outside Hot Reload.
- when fake Battery is natively hidden, the compact session reserves the same stable Battery logical slot width used in the non-island case; exact-target SystemUI review verifies that Header-owned `batteryWidthDiff` is an **unscaled QS_FAKE-root translation term** paired with progress-scaled `normalControlStatusIconsTranslationX`, not a child-slot correction. Combined already inherits that parent motion, so `batteryWidthDiff` must never be applied a second time or converted into a project-owned 30 px compensation;
- no additional status-icon measure/layout/battery-hide Hook set, project alpha/visibility/translation writer, timer, polling loop, or competing native motion owner is permitted; one low-frequency Fake-root attach Hook plus a temporary root layout listener may own bootstrap/readiness because they follow the native host/layout lifetime, and that listener must be removed after success/final failure/detach;
- exact-target HyperOS and KeiMi 2.5.0 review allows a transition-only window-root overlay **only for Guiyuan Trinity**, because compact presentation replaces the native Wi-Fi/mobile/Battery visual correspondence. Its source matrix is sampled from the real role-5 Battery/carrier after the Header callback has applied native transforms;
- all other native status icons, including network speed, remain on SystemUI's own QS_FAKE/final surfaces. Guiyuan does not clip, redraw, pair or assign trajectories to them;
- fake/final icon membership remains native: QS_FAKE follows `RIGHT_BLOCK_LIST`, final QS follows `CONTROL_CENTER_BLOCK_LIST`. A slot absent from QS_FAKE but present in final QS is a native final-only participant and enters only through the final surface;
- transition Trinity follows the actual fake-root alpha for its overlay lifetime. Center/mobile transition tint is read from the final role-6 native peer group at session/appearance boundaries so inversion uses the same native peer-color fact without a second tint state machine; Battery retains its semantic color policy;
- SystemUI remains the sole native motion/geometry/appearance/tint owner.

**Fully expanded endpoint — NATIVE_ONLY / verified**
- exact-target `ControlCenterHeaderExpandController$controlCenterCallback$1.onAppearanceChanged(appearance, animate)` owns the fake/final alpha handoff;
- `appearance=true` drives final `ControlCenterStatusBarIcon` alpha to 1 and `ControlCenterFakeStatusIcons` alpha to 0; `appearance=false` reverses that ownership;
- `onExpansionChanged(progress)` owns translation only and must not be repurposed as a project visibility threshold;
- Build-441 device diagnostics reach fraction 1.0 and observe the QS_FAKE root at alpha 0 before the return transition, matching the exact-target source contract;
- the accepted steady Guiyuan renderer remains attached to `ControlCenterFakeStatusIcons.overlay`; transition adaptation may additionally use a temporary **window-root overlay** only for the interval where native expansion progress is strictly between its endpoints;
- fully expanded ownership remains native-only. Build 482 keeps role-6 native target masks absent; final Wi-Fi/mobile/Battery and final-only status icons stay on the native final surface throughout the gesture. The final role-6 top-level slot View is the read-only occupancy witness for order, width, spacing and position. Internal children such as `wifi_signal`, mobile type, `mobile_signal`, and the Battery icon may refine optical geometry only; a missing or 0×0 child must not erase the top-level slot trajectory;
- Build-455 device evidence still rejects overlay-local screen coordinates as a motion authority. Build 470-473 later confirmed the same boundary by failing with Home-source/RectF projection. Build 478 samples the Trinity source from the real role-5 Battery/carrier transform and uses role-6 matrices only as read-only Trinity targets;
- the native fake/final Folme contract remains the endpoint appearance authority. Build 482 uses raw native expansion for external component motion and does not define a project release window. Wi-Fi moves by translation plus uniform scale toward the native optical glyph; Battery alone may fold within its own envelope; Mobile uses the native fake-root alpha handoff as the local morph signal. The real fake/final surface handoff remains owned by native `onAppearanceChanged()`;
- Build 483 adds **layout occupancy without taking peer-motion ownership**. Exact-target `MiuiStatusIconContainer.onMeasure()/onLayout()` proves that end padding participates in native measured width and native end-side child positioning. Guiyuan therefore reuses the already-established QS_FAKE `statusIcons.paddingEnd` reservation writer instead of adding a Battery-measure Hook or writing peer translations;
- during one transition gesture, source semantic spans and role-6 top-level target slot spans are frozen and normalized to a logical end-axis. Raw native expansion interpolates those semantic spans; their union, bounded below by the compact slot width, is the requested reservation. Drawable folds/scales/morphs do not feed back into this width;
- represented native slots stay session-ignored while the reservation expands/shrinks. They are never released mid-gesture; native peers move only because SystemUI remeasures/re-lays out around the continuous reservation;
- Build 483 keeps external Mobile motion unchanged from Build 482 and makes the local fake-alpha-driven morph explicitly two-stage: orbit dots -> horizontal row -> vertical signal bars. This local morph owns no layout width.

No polling/frame follower, per-peer native geometry write, second layout/suppression owner, or project-owned native alpha/translation/visibility writer is permitted. The one existing QS_FAKE layout-reservation writer may vary its owned end padding during the verified transition bridge.


## Keyguard and AOD

**Build 625 candidate ownership boundary.** Build 623 device evidence rejects the separate Keyguard/AOD presentation/render-session model: even with direction-independent routing, the outgoing session restored represented-slot suppression before the target session completed its next native layout, creating a visible native interval. Build 625 keeps a single host-scoped Keyguard-family presentation Session and one module RenderView for the verified shared host. Keyguard<->AOD changes retarget scene semantics without restoring/reacquiring the native ignored-slot delta, clip mask or end reservation and without detaching/re-adding a second render View.

Presentation claim and compact readiness remain separate. A same-host family retarget can preserve an already-established layout contract. Home->AOD is cross-host and therefore cannot inherit Home layout ownership; during explicit AOD prearm it may apply only the existing reversible represented-view mask while the AOD family owner waits for native compact-layout readiness. Renderer cutover remains blocked until that readiness is real. Failure, feature disable, host replacement, SystemUI recreation and Hot Reload still restore only module-owned state. HyperOS continues to own AOD timing, native alpha/visibility/translation and the native lifecycle clock.

**Build 667 lifecycle refinement.** Keyguard-family visual ownership is not identical to stable-family readiness. For a single enabled Keyguard child transitioning toward native AOD, `animateIconContainer(false)` is the start of the native status-icon fade, not an owner-cleanup boundary: an already-valid Guiyuan Keyguard presentation remains outgoing owner while the exact native Keyguard status-icon presentation alpha is greater than zero and yields at the fully hidden endpoint or stable AOD. In the reverse AOD -> Keyguard direction, a precommitted compact presentation may become visually valid at the native status-icon reveal boundary before stable Keyguard state commits. That bounded incoming presentation-ready fact may feed Keyguard-originated Control Center eligibility/lease retention, but it does not set stable `keyguardRuntimeReady`, create a second owner, or override host detach/feature/fail-native cleanup. This preserves the Build-488 lease principle across the verified incoming family handoff without inventing timing or motion ownership.
**Build 668 Home-origin refinement.** A direct screen-off path may be routed by HyperOS through a transient Keyguard Full-AOD target before native AOD animation begins. This transient target is not a stable Keyguard endpoint and must not automatically inherit stable Keyguard -> AOD ownership. When Full-AOD starts from authoritative HOME while Home still owns represented slots, Keyguard replacement is enabled, and AOD replacement is disabled, Guiyuan records a bounded Home-native-AOD candidate. The candidate survives an intermediate Keyguard target, promotes to native-AOD authority only when native AOD animation begins before stable Keyguard, and is cleared if stable Keyguard wins first. Once promoted, native remains authoritative through stable AOD. This is lifecycle provenance only; it does not own timing or motion.

For incoming AOD -> Keyguard Control Center, the existing boundary-presentation-ready fact also acts as source-conflict authority. If that fact is true and at least one native source witness says KEYGUARD, stale HOME panel state cannot demote Control Center to native during the handoff. Expansion-fraction callbacks may use the same bounded fact before visible/source callbacks arrive. Outside that incoming boundary, ordinary HOME/KEYGUARD resolution is unchanged.



Build 456 is the current **device-accepted opt-in PROJECTED steady Keyguard implementation**. Build 455 proves the corrected AOD authority can reach steady Keyguard Guiyuan but is rejected for a shared Keyguard/QS_FAKE peer-layout/motion inconsistency caused by temporary ignored-slot state. Build 456 keeps AOD NATIVE_ONLY and makes Keyguard/QS_FAKE represented-slot exclusion session-scoped through the verified native container API; focused maintainer device validation accepted the resulting steady Keyguard and transition behavior.

Build 537 candidate changes only the module-owned Keyguard render surface: because the verified `mSystemIconsContainer` is also `MiuiStatusBatteryContainer`, Keyguard reuses the Build-536 logical-viewport / direct-child top-overflow policy so swapped or enlarged top content is not clipped. The Keyguard Session remains separate from Home; native represented-slot handling, tint authority, AOD blocking, carrier motion and the Control Center source router are unchanged. This carrier change is pending exact-head device validation.

Exact-target review underlying the accepted Keyguard adapter establishes:
- `MiuiKeyguardStatusBarView.mSystemIconsContainer` / `@id/system_icons_container` is the native Keyguard end-side `MiuiStatusBatteryContainer` registered into `ControlCenterFakeViewController.keyguardSystemIcons`;
- HyperOS itself selects `statusBarSystemIcons` for status-bar state 0 and `keyguardSystemIcons` for state 1, then feeds the selected `realSystemIcons` into Control Center Header geometry. Guiyuan must reuse that native router rather than duplicate it;
- Keyguard steady must use a **separate host/session adapter** from Home. Shared renderer/domain semantics are reusable, but mutable Home View/session ownership is not;
- `MiuiKeyguardStatusBarView.updateIconsAndTextColors()` is the native Keyguard tint authority and also forwards the same Keyguard tint semantics to QS_FAKE;
- the base Keyguard status-bar visibility lifecycle resets `mSystemIconsContainer` translation when hidden, while Keyguard-specific status-icon animations target the child `mStatusIconContainer`; these are distinct ownership layers and must not be collapsed;
- Build 442 observes only the steady Keyguard host/source identity through the already-installed Battery scene callback. It does not install Keyguard lifecycle/tint/AOD hooks and does not draw, hide, compact, reserve, or translate Keyguard content.

AOD remains a separate native-only surface and is not implied by Keyguard support. Exact-target `KeyguardStatusBarViewControllerInject.animateFullAod()` separately drives Battery alpha/AOD mode plus status-icon alpha/visibility/`setIsAodAnimate()`, proving that a steady Keyguard adapter cannot silently own AOD as a boolean sub-state.

Build 442 establishes the structural host/source boundary. Build 456 uses that boundary with a separate mutable Keyguard adapter and a default-off feature switch. The adapter reuses the existing class-wide status-icon presentation Hook substrate by exact View identity; it does not add a Keyguard lifecycle state machine. For transition-capable Keyguard/QS_FAKE containers, represented ignored slots remain present for the whole presentation session through native `addIgnoredSlots/setIgnoredSlots`, so native measure/layout and native motion observe the same slot-state fact. HyperOS still owns carrier visibility/alpha/translation and the shared Control Center source router.

The candidate deliberately separates steady-host identity from transition-router timing: `realSystemIcons` does not need to have switched to Keyguard before the steady Keyguard host can be resolved. Conversely, Keyguard-originated QS_FAKE is not permitted until the steady Keyguard adapter is actually ready.

Build 488 adds a lifecycle rule for Keyguard-originated Control Center transitions: once the verified Keyguard compact presentation has entered a native expansion with fraction greater than zero, that already-owned presentation state may remain leased across transient Keyguard host-layout/readiness loss until native expansion returns to zero. This is not a timing grace period. The lease exists only while the source remains KEYGUARD, the feature remains enabled, the Keyguard host remains attached, and AOD is not active; authoritative source change, AOD, host/runtime failure, feature disable, teardown, or Hot Reload releases it immediately. Its purpose is to prevent restoring/re-laying out the outgoing Keyguard native status row underneath HyperOS's fake-to-final Control Center handoff. It does not keep Guiyuan visible on the fully-expanded final Control Center surface and does not write native peer geometry/alpha/visibility.
Build 492 separates steady-presentation readiness from transition-source geometry lifetime. An already-laid-out Home/Keyguard render View may remain a read-only handoff witness after that steady presentation has yielded, provided its host is attached and its retained bounds are non-zero. If source and Control Center transition surfaces use distinct window roots, their coordinate spaces are reconciled from native screen/window origins before mapping into the transition root. Sampling remains one-shot at transition Session creation; this does not extend steady rendering ownership or add a second geometry writer.
Build 493 formalizes transition correspondence multiplicity. Existing Trinity components remain 1→1 where a compact semantic maps to one final native semantic. A distinct second final mobile subscription is a 1→N split from the compact Mobile source; a final airplane slot coexisting with compact Wi-Fi is a 0→1 reveal. These are transition-overlay representations only: final native Views retain their own alpha/visibility/translation ownership and take over through the existing native final-surface handoff. Additional projected semantics are admitted only from verified final slot identity/geometry and, for Mobile, real subscription signal state; unavailable evidence omits the projection rather than inventing one. The same final semantics participate in the existing reservation union so layout occupancy and visual projection remain consistent.
Build 494 refines retained-source geometry ownership: a steady projected render View is a basis/size witness, not a global-position authority. Home/Keyguard transition witnesses therefore pair the stable render View with the native battery-body carrier. Session creation samples both once and composes native carrier position with stable render basis before any interpolation. Build 494 also preserves HyperOS as the only peer-motion authority during charging Super-Island: when the existing native island callback reports showing and the render model is charging, Guiyuan does not add progress-synchronous transition reservation on top of the compact carrier reservation. Outside that conjunction, the existing reservation path is unchanged.

AOD remains a separate gate. Build 456 retains Build 455's corrected native AOD-state observation Hooks and does not claim AOD alpha/visibility/translation/animation ownership. Any AOD leakage or failed restoration during Canary validation rejects the candidate rather than being patched with timing or alpha thresholds.

## Charging

Charging, quick charging, and super charging are render-state variants, not scenes.

They must not create a second scene geometry policy or a separate slot-width rule.

## Motion ownership

Unlocked steady currently uses `NONE`: Guiyuan has no independent motion requirement there. Its end-side visual inherits native `system_icons` motion when SystemUI transitions that carrier.

Notification Shade and all Control Center transition/destination motion stay under `SYSTEM_UI`; inheritance/projection does not transfer motion ownership to Guiyuan. Whether Guiyuan renders on a given verified carrier is a separate capability decision from who owns motion.

`COMBINED_STATUS` remains reserved for a future transition that is demonstrated to be genuinely owned by Guiyuan from start state through cleanup.

## Promotion rule

Changing a scene from NATIVE_ONLY to PROJECTED or introducing any new geometry/motion ownership requires:

1. exact target-SystemUI evidence;
2. runtime host and lifecycle verification;
3. a single-writer analysis;
4. fail-native behavior;
5. bounded diagnostics;
6. focused real-device validation;
7. an updated capability table and changelog entry.

If any of those are missing, the scene stays NATIVE_ONLY.


## Working scene concept matrix

This matrix records the maintainer's current product-intent partition. It is **not** yet an architecture contract. Exact-target lifecycle/source review may produce a better grouping; any such change should be reviewed and discussed before implementation.

| Source context | Steady state | Partial Control Center pull | Fully expanded Control Center |
| --- | --- | --- | --- |
| Unlocked / Home | Guiyuan on verified Home carrier | Guiyuan transition bridge follows native HyperOS motion | Native SystemUI status bar only |
| Locked / Keyguard | Guiyuan on the verified opt-in Keyguard carrier | Guiyuan transition bridge follows native HyperOS motion from the Keyguard source | Native SystemUI status bar only |

Design consequences:
- source-scene ownership and transition ownership should be evaluated separately;
- Home and Keyguard use separate steady host/session adapters while reusing shared renderer/domain semantics; do not merge their mutable View/session ownership;
- a shared transition coordinator is a candidate only if source/runtime evidence supports it without creating a third state machine;
- the fully expanded Control Center endpoint is verified and accepted as native-only;
- reverse motion restores the correct source scene before bridge cleanup;
- no source adapter may infer the other source scene from Battery state, global Keyguard booleans, or timing.


### Build 455 AOD exclusion authority — correction of rejected Build 453

Steady Keyguard projection is not equivalent to AOD ownership. Device-rejected Build 453 attempted this gate but incorrectly resolved `toggleAodMode` as zero-argument, so its AOD authority installed zero Hooks and Keyguard failed native. Build 455 corrects the pinned contract: a unique `setIsAodAnimate(boolean): void` and `toggleAodMode(boolean): void` plus Boolean `mToAod` / `mIsAodAnimate` are required before Keyguard projection is allowed. `mToAod || mIsAodAnimate` blocks Keyguard projection and restores the native represented presentation. `mAnimToAod` is diagnostic-only.

If that contract cannot be resolved uniquely, Keyguard remains native while Home/QS_FAKE continues on the accepted Build-446 path. Guiyuan does not write AOD alpha, visibility, translation, animation or geometry.

### Build 620 stable-AOD candidate — independent scene ownership

Build 620 promotes only **stable AOD** to a projected candidate. It does not reinterpret AOD as Keyguard. The existing pinned AOD authority remains the scene boundary:

- `mToAod == true && mIsAodAnimate == false` is the only candidate stable-AOD state;
- any enter/exit animation state remains native-only and releases Guiyuan AOD ownership before HyperOS animation proceeds;
- Keyguard and AOD use separate mutable render/presentation sessions and are mutually exclusive on the shared verified Keyguard-family host;
- the global Guiyuan enable is the parent runtime gate, while Keyguard and AOD display preferences are independent child values;
- AOD reuses the existing reversible represented-slot/presentation substrate instead of adding a second native status-icon writer;
- AOD cannot provide a Control Center transition-source witness;
- no timer, polling source, custom AOD animator, or native alpha/translation/visibility writer is introduced;
- any missing/ambiguous AOD state or host contract restores native Keyguard/AOD presentation.

This is a candidate ownership change and remains pending focused device validation. The accepted Build 619 baseline still treats AOD as native-only.

### Build 652 lifecycle-boundary correction

Build-651 device evidence refines the Keyguard/AOD family contract without adding a new owner:

- a disabled child is a hard **stable-state** boundary; steady Keyguard cannot be occupied by AOD solely because Home presentation ownership is still observable, and steady AOD cannot be occupied when the AOD child is disabled;
- Home -> AOD prearm requires an actual native AOD animation, UNKNOWN prior family history, an enabled AOD child, and still-owned Home presentation. Home ownership by itself is not transition direction;
- during a native Keyguard <-> AOD animation, the single family owner may retain the enabled outgoing child when the destination child is disabled. This retention ends at stable-target evidence, where the disabled destination fails native. HyperOS remains the only alpha/visibility/translation/timing owner;
- a Control Center HOME/KEYGUARD conflict is resolved from the existing stable-family lifecycle latch rather than assigning permanent priority to either callback source: latched KEYGUARD/AOD history selects HOME as the unlock target, while UNKNOWN history selects KEYGUARD as the lock/AOD-entry target;
- the one resolved Control Center source is shared by projection eligibility and TransitionOwner.

This supersedes Build-651 assumptions that a disabled destination must force Native at animation start and that steady scene identity always outranks the panel source. It does not restore mutable presentation ownership as direction evidence.

### Build 654 lifecycle correction

Build-653 device evidence rejects host `isShown` as a steady-scene authority. HyperOS can keep or animate Keyguard/AOD hosts independently of the stable scene represented by native status-bar state. Current policy therefore restores structural ancestry + native status-bar-state as the steady scene authority; host visibility is not allowed to remove a valid steady Keyguard owner.

Build-652's direction-aware Control Center source arbitration remains unchanged. Build-653 device evidence shows the failing immediate Home pull already resolves effective source HOME before presentation failure, so lifecycle correction must not reopen that source policy.

QS_FAKE presentation ownership remains long-lived across fake-root lifetime as established by Builds 433-440. Only its bounded carrier-width capacity lease is scoped to one actual visible Control Center cycle: when requested visibility transitions from true to false, that width lease is released or acknowledges a native hidden-boundary restoration while represented-slot exclusion, clip masks and compact readiness remain prearmed. Hidden-state reservation sync cannot reacquire the width lease; the next visible cycle resumes the lease from the current native baseline before reuse. Genuine live width changes during an active visible-cycle lease still fail native.

Single-child Keyguard/AOD cutover observes the native Keyguard status-icons layer only. Its local visibility/alpha is read-only evidence for when that layer has yielded/taken over; Battery AOD alpha is a separate native animation and is not a whole-scene lifetime signal. Dual-enabled Keyguard/AOD continues to use the single family presentation/renderer owner without project-owned alpha timing.

No timer, polling, copied native animator, or native alpha/visibility/translation writer is introduced. Missing evidence remains conservative/fail-native.

### Build 655 candidate — full-AOD target event is a boundary, not a clock

Build-654 device evidence separates native animation **start/end flags** from the visual handoff boundary needed by a single enabled Keyguard/AOD child. `mIsAodAnimate=true` can arrive before the desired Keyguard -> AOD handoff, while waiting for `mIsAodAnimate=false` makes AOD -> Keyguard visibly late.

The pinned target exposes a narrower event contract:
- `KeyguardStatusBarViewControllerInject.animateFullAod(boolean, boolean)` owns the native full-AOD transition;
- `MiuiKeyguardStatusBarView.mToLockScreen` is retained native target state on the Keyguard-family host.

Build 655 treats the callback only as an event that native target state has been committed. The raw boolean arguments are diagnostics only. After the callback, policy reads `mToLockScreen` and, for **single-child mode with a known stable family origin**, selects the enabled target child or Native. It does not derive progress, duration, interpolation, or geometry from that event.

Priority remains:
1. Home/UNKNOWN-origin AOD prearm;
2. exact native full-AOD target for single-child Keyguard/AOD transition when available;
3. local native status-icons alpha fallback;
4. stable-family conservative routing.

Dual-enabled Keyguard/AOD continues to use the existing one-owner retarget path. Missing full-AOD target evidence falls back to Build-654 behavior rather than inventing timing.



### Build 656 candidate — direction and visual cutover are separate native facts

Build-655 device evidence rejects the full-AOD target commit as the visual handoff boundary: both single-child directions become visibly early when `mToLockScreen` is consumed immediately.

Build 656 keeps `animateFullAod` / `mToLockScreen` only as native **direction** evidence. A separate exact-target callback, `MiuiKeyguardStatusBarView.animateIconContainer(boolean)`, supplies the native status-icon **visual lifecycle** event; its Boolean parameter is not assigned product semantics.

For a known single-child Keyguard/AOD transition:
1. full-AOD target commit marks the target pending;
2. while pending, retain only the enabled outgoing child (otherwise Native);
3. on the native status-icon animation event, consume the already-committed target and switch once;
4. non-animating AOD state clears the pending latch.

This prevents both the early target-commit cut and the late animation-end cut without adding a local duration, delay, progress clock, polling loop, or native View writer. If the visual-event hook is unavailable, routing falls back to the prior status-icons-alpha compatibility path.


**Ordering refinement:** the pending scope begins before native `animateFullAod` executes because `animateIconContainer` may be called from inside that native method. The full-AOD return callback never performs ownership transfer. This keeps the event relationship native-driven even when the callbacks are nested. The icon-container method is treated as a candidate lifecycle boundary, not as a presumed 50% animation point; only device evidence can promote that assumption.


### Build 657 candidate — target prearm is a bounded handoff lease

Build-656 device evidence shows that one generic event cannot own all three transitions.

For AOD -> Keyguard, the native status-icon event is still the desired cutover, but Build 656 accidentally clears its pending direction lease before that event arrives. Build 657 retains the lease through the post-full-AOD stale state and lets the native status-icon event consume it.

For Home -> AOD, native target evidence can precede the generic AOD-animation flag. Build 657 therefore treats an authoritative AOD target as **preparation authority only** when all of the following were true at arm time: AOD feature enabled, steady Home origin, UNKNOWN Keyguard/AOD family history, and Home still owning represented slots. That establishes a bounded `homeAodTargetPrearm` lease.

The lease does not select arbitrary scenes and does not write native visibility/alpha. It only allows the existing AOD presentation/renderer to prepare early and use the existing pre-mask + compact-layout cutover. Once armed it may survive transient Keyguard ancestry so outgoing Home ownership can yield naturally without creating a no-owner interval. Stable AOD/non-AOD state, reverse target, runtime teardown, or feature ineligibility closes the lease.

Keyguard -> AOD keeps the Build-656 `animateIconContainer` cutover unchanged.
