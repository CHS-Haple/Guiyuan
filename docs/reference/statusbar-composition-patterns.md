# Status-bar composition and scene-projection patterns

## Purpose

This note preserves reusable architecture findings from a mature SystemUI status-composition implementation without importing its product identity, source code, assets, or implementation-specific naming.

The findings are useful because they address the same class of problems Guiyuan must solve:

- replacing several native status visuals with one compact representation;
- avoiding duplicate layout occupancy;
- keeping native status Views alive for lifecycle/tint/state ownership;
- surviving platform-owned scene changes;
- transitioning between compact and native representations;
- supporting later size/spacing controls without coupling drawing size to platform slot geometry.

These patterns are **reference evidence**. Items adopted by the production architecture are labeled explicitly; current runtime authority remains `docs/architecture/` plus `docs/development/CURRENT.md`.

---

## 1. Existing-host composition instead of an extra persistent participant

**Observed.**

The inspected implementation does not create a second permanent status-icon participant for the compact representation. It reuses an existing native end-side host and its already-established layout lifecycle.

The compact visual is drawn from that host while the surrounding SystemUI layout remains authoritative.

### Reusable principle

When the platform already owns an appropriate end-side layout host, prefer:

`native host -> compact presentation`

over:

`native host + second custom participant -> occupancy reconciliation`.

This avoids creating two independent layout identities that must later exchange width, position, and animation ownership.

### Guiyuan applicability

**Adopted for the current Phase-2A Home path on the pinned target.**

The earlier work-branch history showed repeated failure modes when a second permanent participant changed occupancy around native Battery-slot release. The accepted Home path instead renders through the existing native Home host/carrier hierarchy rooted at `MiuiNotificationStatusContainer / system_icon_area`.

This adoption is Home-specific. Other scenes require their own verified hosts and ownership boundaries; Keyguard/AOD now use the accepted host-scoped family owner, and Control Center uses its bounded QS_FAKE transition bridge.

---

## 2. Scoped native slot suppression during native measure/layout

**Observed.**

When the compact representation stands in for native status items, the implementation temporarily adds only the represented native slots to the platform's existing ignored-slot collection.

The mutation is tightly scoped:

1. determine which represented slots are not already ignored by SystemUI;
2. add only those missing slots;
3. allow the native measure/layout call to run;
4. remove exactly the entries added by the module;
5. perform the same restoration on exceptional exit.

The implementation does not clear or replace the platform collection wholesale.

### Reusable principle

A layout mutation can be safe only when all of the following are true:

- it occurs at the platform-owned layout boundary;
- it is limited to the minimum represented slots;
- the pre-existing platform state is preserved;
- restoration is exact and unconditional;
- no persistent parallel layout state is created.

Prefer a restoration-token model over long-lived mutation.

### Guiyuan applicability

**Adopted for Home on the pinned target; refined for transition-capable Keyguard/QS_FAKE.**

Exact-target inspection verified `MiuiStatusIconContainer.ignoredSlots`, public final `addIgnoredSlots(...)` / `setIgnoredSlots(...)`, and native measure/layout consumption. Home keeps the device-accepted temporary native-call scope: represented entries exist only around the hooked native measure/layout invocation and are then restored.

Build-455 device evidence proves that temporary scope is not sufficient for Keyguard/QS_FAKE, because those surfaces have native motion/animation ownership that can run outside the measure/layout call. Build 456 therefore uses the same native ignored-slot contract as **session state** on those two surfaces: add only the absent represented entries through the native API, keep them present until that presentation session ends, then restore only the owned delta through the native setter. Native add/set remains responsible for layout invalidation; Guiyuan does not add a parallel motion writer.

The reusable rule is therefore not "always temporary" or "always persistent": the ignored-slot lifetime must match the complete native presentation owner that consumes the layout state. In every case the mutation stays host/session-scoped, minimal, reversible and fail-native on writer ambiguity.

This remains fingerprint-scoped. Other SystemUI builds/scenes and unexpected competing state must be revalidated or fail native.

---

## 3. Reversible visual masking without changing native layout identity

**Observed.**

Native Views whose visuals are replaced remain attached. Their current clip bounds are saved once, an empty clip is applied while the compact representation is active, and the exact original clip is restored afterward.

The reference path does not need to use permanent `GONE`, alpha racing, or repeated translation writes to suppress those visuals.

### Reusable principle

When a native View must retain layout, lifecycle, tint, and state ownership but should not draw:

- prefer a reversible presentation mask;
- preserve the exact prior state;
- restore only module-owned changes;
- do not confuse visual suppression with layout removal.

### Guiyuan applicability

**Adopted for the current Home path.**

Exact-target writer review found no competing Home Wi-Fi/mobile/Battery `clipBounds` writers in the directed target audit. The active Home session snapshots each native clip, applies an empty clip while replacement is ready, and restores only its own applied state.

The pattern remains scene- and target-scoped; later surfaces must repeat the writer/lifecycle review.

---

## 4. Host-scoped runtime state

**Observed.**

The implementation keeps an identity-based map from native host to a host-specific session/state object.

Each host state owns the runtime resources that belong to that host, including presentation geometry, native-view references, represented slots, overlay content, and cleanup responsibility.

Detached hosts are removed and cleaned rather than leaving their geometry or references globally reusable.

### Reusable principle

Prefer:

`Host -> HostSession -> owned resources`

and keep all host-derived geometry/state inside that session.

A host/session boundary should answer:

- what native host is authoritative;
- which native Views belong to it;
- which presentation mode is active;
- which temporary mutations are currently owned;
- how cleanup restores native state.

### Guiyuan applicability

**Directly compatible with project rules.**

This remains the lifecycle foundation for Home and the verified Keyguard/AOD family owner even though their concrete hosts differ.

Multi-host awareness alone never proves another scene compatible. Each newly supported target scene still requires explicit host mapping, ownership review, and device validation.

---

### Same-host scene retargeting and cross-host pre-mask

**Adopted for the current Keyguard/AOD family contract. Build 625 introduced the same-host retargeting boundary after Build 623 rejected separate family sessions; later lifecycle validation refined but retained this ownership model.**

When two scene semantics resolve to the same verified native host and consume the same represented-slot suppression/layout contract, switching between two project Session objects can create an artificial native interval even though SystemUI never changed the underlying host. In that case, one host-scoped presentation owner may retarget scene semantics while retaining its exact owned ignored-slot delta, visual mask and reservation. The render layer should likewise retain one module child View and retarget scene-specific visibility/tint semantics rather than creating simultaneous writers.

This does not generalize across distinct native hosts. For a cross-host handoff such as Home -> AOD, target visual suppression may be prepared before compact layout only when it is reversible and explicitly scoped to the handoff; layout readiness and renderer ownership must remain false until the native target layout is actually valid. Visual masking is not layout ownership.

Reusable boundary:
- same verified host + same owned presentation contract -> retarget one owner;
- distinct host -> acquire a bounded target claim without borrowing source layout readiness;
- restore native only when the owning host family is exited, invalidated or fails;
- never keep two project overlays or two mutable presentation writers alive merely to hide a handoff gap.

## 5. Slot size, glyph size, per-glyph scale, and optical adjustment are independent

**Observed.**

The inspected sizing model carries separate values for:

- layout slot size;
- visual/glyph size;
- per-glyph scale;
- optical adjustment.

User scaling resolves through this sizing model instead of rewriting hook behavior.

### Reusable principle

Do not use one width value for all of:

- native occupancy;
- renderer canvas size;
- actual visible glyph bounds;
- neighbor optical gap;
- transition endpoint.

A future user scale should feed one shared resolved-layout/sizing object. Scene adapters consume the result; they should not invent their own scale offsets.

### Guiyuan applicability

**Required design direction for 0.0.2.**

This matches the planned adaptive size/spacing feature. The UI can remain deferred while the runtime geometry contract is established now.

---

## 6. Platform hide/scene semantics remain authoritative input

**Observed.**

The reference path reads the platform's native hide/scene state as an input to compact-presentation eligibility. No evidence was found of intercepting the native battery-hide setter to force the platform slot to remain present.

### Reusable principle

Do not preserve a host by fighting a platform-owned scene decision unless target evidence proves that ownership transfer is safe.

Prefer:

`native scene/hide fact -> presentation policy`

over:

`native hide request -> module rewrites request -> repair downstream geometry`.

### Guiyuan applicability

**Important negative guidance.**

The project rejected both the earlier global battery-hide-preservation idea and the later narrow Build-395 layout-time hide override. The current Home path treats native Battery hide as a read-only fact and adjusts only its own verified replacement-space reservation.

Guiyuan differs from a battery-only compact representation because network information must remain represented during charging-island behavior.

---

## 7. Exact-target QS_FAKE Battery-Island translation contract

**Observed and statically verified on the pinned target.**

Source artifact:
- HyperOS SystemUI `17.03.260226.r`
- versionCode `202602260`
- SHA-256 `a0e738e41fe599b97950cbf52a9e2ddc6ae2ceff986efbacb1c9840bea78768d`
- reviewed with JADX 1.5.6

The target separates Control Center status presentation into:
- `ControlCenterFakeStatusIcons` / `StatusBarLocation.QS_FAKE` / layout-from tag 5;
- `ControlCenterStatusBarIcon` / `StatusBarLocation.QS` / layout-from tag 6.

`ControlCenterHeaderExpandController.updateLocation()` computes ordinary status-icon translation from the real-status-bar and Control Center endpoints. For Battery Island it deliberately overrides the ordinary Battery width difference with the negative **full Control Center Battery View width**.

The expansion callback then applies:
- final QS surface: progress-scaled normal status-icon translation;
- QS_FAKE surface: the same progress-scaled translation **plus the unscaled `batteryWidthDiff`**.

Device evidence `normalStatusIconsTx=181`, `batteryWidthDiff=-135` is therefore internally consistent: at progress 0 the fake endpoint is `181 - 135 = 46`, while at progress 1 it becomes `-135`. The non-island `46/0` case moves `46 -> 0`. A visibly larger leftward Battery-island trajectory is native behavior, not proof of project-owned drift.

`MiuiBatteryMeterView.updateIslandChanged()` independently confirms the same ownership model: Battery Island calls `MiuiStatusBatteryContainer.setIsHideBattery(true)`, makes the Battery view non-visible, and the Battery island animation uses the full `getWidth()`. `MiuiStatusBatteryContainer` then excludes hidden Battery width from normal occupied layout.

### Fully expanded fake/final appearance endpoint

Exact-target review also verifies that endpoint visibility is not owned by expansion fraction.

`ControlCenterHeaderExpandController$controlCenterCallback$1.onAppearanceChanged(appearance, animate)` owns the alpha handoff between the two native Control Center status-bar surfaces:

- `appearance=true`: final `ControlCenterStatusBarIcon` is driven to alpha `1`, while `ControlCenterFakeStatusIcons` is driven to alpha `0`;
- `appearance=false`: final alpha returns to `0`, while QS_FAKE returns to `1`.

By contrast, `onExpansionChanged(progress)` updates translation only. It is motion/geometry evidence, not a project visibility authority.

Build-441 target diagnostics independently match that contract: native fraction reaches `1.0`, then the QS_FAKE root is observed at alpha `0.0` before the return transition. The same relationship is visible in ordinary and Battery-Island pulls.

Guiyuan attaches only to `ControlCenterFakeStatusIcons.overlay`. Exact-target source and Build-441 diagnostics verify the **native root** alpha/translation and fake/final appearance contract; the project does not mask, hide, translate, or otherwise mutate final `ControlCenterStatusBarIcon`.

Build-453 device feedback reopens one narrower derived assumption: the project overlay render View can probabilistically appear at the fully-expanded endpoint position before the expected fake trajectory is visually complete. The native fake/final contract remains verified, but project-overlay coordinate inheritance is no longer accepted merely from the root alpha observation. Build 455 therefore samples QS_FAKE root, status-area, carrier and render screen/local geometry at the existing 8 diagnostic progress buckets. No new writer is introduced.

The fully expanded Control Center endpoint remains native-only through HyperOS appearance ownership. Do **not** add a project `fraction >= x` hide threshold, custom fake-to-final fade, final-QS suppression, or fixed-position compensation without owner-level geometry evidence.

### Reusable principle

Treat native root motion and project-local replacement geometry as separate contracts:

`native QS_FAKE root motion -> inherited by overlay`

`stable logical replacement slot -> owned by Guiyuan presentation`

Do **not** turn the difference between full native Battery presentation width and stable Combined carrier width into a magic compensation. In particular, do not add `batteryWidthDiff` to Guiyuan translation: the parent QS_FAKE root already carries it.

---

## 7. Native progress + real endpoints for scene projection

**Observed.**

Scene transition progress is consumed from an existing native expansion callback.

For Notification Shade on the pinned HyperOS target, Build-422 root-cause review supersedes the earlier reliance on generic `ShadeExpansionStateManager.onPanelExpansionChanged(...)` for Home handoff timing. Exact-target source shows that the native Notification Header is driven from `NotificationPanelExpansionAnimator.expansion`, exposed through `NotificationPanelExpandController.expansionState`, then delivered to `NotificationHeaderExpandController$notificationCallback$1.onExpansionChanged(float)`.

Build-423 device evidence later established that this Header callback is valid **motion context**, but not the Guiyuan Home-visibility authority. Do not reconstruct Home ownership from Header fraction, Battery status state, global Keyguard state, generic Shade expanded/tracking flags, or a local timing threshold.

Exact-target device diagnostics also verify a separate Control Center lifetime contract on `com.miui.systemui.controlcenter.container.ControlCenterExpandControllerDelegate`:
- `onVisibleChanged(boolean)` brackets Control Center ownership and remains true throughout the outward/return transition;
- `onExpansionChanged(float)` supplies native progress but is not required to decide whether Home owns the scene.

Guiyuan consequence: compose the semantic visibility callback into Home eligibility; keep Control Center fraction for diagnostics/future projection and do not invent a fraction threshold.

Projection endpoints are derived from actual View screen coordinates. The visual transition is then drawn using canvas translation/scale/alpha rather than by taking ownership of the native Views' live translation.

The projection layer is updated only when relevant source/target/progress state changes.

### Reusable principle

For a transition between compact and native representations:

`real source geometry + real target geometry + native progress -> draw-only projection`

is preferable to:

`fixed offset + custom duration + independent interpolator`.

### Guiyuan applicability

**Strong candidate for Home -> shade / Control Center.**

This aligns with the project requirement that SystemUI own transition timing and target placement while Guiyuan owns only its composed visual projection.

For Home, Build-424 exact-target review distinguishes the parent `system_icon_area` HostSession from the actual animated end-side child `system_icons`. The Home visual belongs on the latter carrier so it inherits native end-side motion directly. The real-endpoint/native-progress principle remains applicable to a genuine second projected surface such as Control Center, not to Notification Shade where no status-icon projection target exists.

---

## Keyguard steady-source contract on the pinned target

**Static exact-target evidence; Build 442 performs only the first steady-host/source runtime verification.**

The Keyguard source is not the Home carrier reused under a different global flag. HyperOS exposes a distinct native source:

- `MiuiKeyguardStatusBarView.mSystemIconsContainer` resolves `@id/system_icons_container` as a `MiuiStatusBatteryContainer`;
- `MiuiKeyguardStatusBarView.initCallback()` registers that View into `ControlCenterFakeViewController.keyguardSystemIcons`;
- `ControlCenterFakeViewController.adjustRealSystemIcons()` selects Home `statusBarSystemIcons` for status-bar state 0, Keyguard `keyguardSystemIcons` for state 1, and no source for other states;
- native status-bar-state and bouncer callbacks re-run the same selection, so Guiyuan must not reconstruct a second Home/Keyguard router;
- Control Center Header consumes the selected `realSystemIcons` as its source geometry reference.

Keyguard also has independent lifecycle and tint authority:

- `onAttachedToWindow()/onDetachedFromWindow()` register and release Keyguard callbacks/icon groups;
- base `KeyguardStatusBarView.setVisibility()` owns root visibility and resets the system-icons translation on exit;
- `updateIconsAndTextColors()` derives Keyguard light/dark colors and applies them to Keyguard icon/battery presentation while forwarding the same source tint to QS_FAKE;
- child `animateIconContainer()` targets `mStatusIconContainer`, not the whole system-icons carrier.

AOD is separate. `KeyguardStatusBarViewControllerInject.animateFullAod()` independently changes Battery alpha/AOD mode and status-icon alpha/visibility/animation flags. Therefore Keyguard ownership must remain inactive whenever the native AOD authority reports an AOD transition or stable AOD.

**Build 620 candidate implication:** reuse the verified Keyguard-family host structure, but not the mutable Keyguard session. Stable AOD (`mToAod=true && mIsAodAnimate=false`) gets a separate bounded render/presentation session; AOD enter/exit remains native HyperOS, and AOD never becomes a Control Center source. This candidate still requires focused device verification before becoming accepted runtime evidence.

---

## 8. Transition masking and cleanup are separate from steady layout ownership

**Observed.**

The transition path has its own overlay/presentation lifetime. Native target Views can be temporarily masked while their real layout remains intact. At transition end or failure:

- overlay content is removed;
- temporary listeners are removed;
- original native visual state is restored;
- projection tracking state is cleared.

This cleanup is separate from steady compact-host cleanup.

### Reusable principle

Keep three lifetimes separate:

1. steady host/session lifetime;
2. scoped native layout-mutation lifetime;
3. scene-projection lifetime.

Do not use a single global flag or View width to stand in for all three.

---

## 9. Cleanup is a first-class compatibility contract

**Observed.**

Host cleanup removes owned overlay content, restores tracked native presentation state, clears transient compact state, and requests native relayout where necessary.

Global cleanup also removes transition overlays/listeners and clears host tracking.

### Reusable principle

A replacement architecture is incomplete until it can reliably return to untouched native behavior after:

- feature disable;
- host replacement;
- SystemUI recreation;
- Hot Reload;
- transition cancellation;
- reflection/hook incompatibility;
- unexpected runtime failure.

Fail-native cleanup should restore the exact prior state, not merely set a guessed default.

---

## 10. What the reference evidence does not establish

The following remain **not established** for Guiyuan:

- that one Home host can also be reused for keyguard or AOD;
- that the target SystemUI exposes identical ignored-slot behavior in every scene;
- that compact presentation should disappear whenever the native battery host disappears;
- that the reference scene policy is appropriate for Guiyuan network semantics;
- that the exact sizing constants or source implementation details should be copied;
- that an overlay alone is sufficient for every native APPEAR/DISAPPEAR requirement;
- that any private/obfuscated implementation identifier is a stable platform contract.

These must be verified independently.

---

## 11. 0.0.2 architecture / regression checklist

Use this checklist for current Home regression review and before promoting the same ideas into a new scene or target.

### Steady Home

- exactly one effective end-side layout responsibility;
- no duplicate Wi-Fi/mobile/battery occupancy;
- native state/tint sources remain live;
- Guiyuan visual masking is reversible;
- scale/spacing derives from one resolved layout.

### Charging / island

- native battery hide remains platform-owned unless new target evidence proves otherwise;
- no 0 -> full-width participant identity switch;
- network information remains continuously represented;
- island entry/exit uses a verified carrier or projection;
- native peers keep platform-owned motion.

### Shade / Control Center

- use native expansion/progress authority;
- resolve real source/target endpoints;
- no custom timing system;
- no first/last-frame compensation offsets;
- transition cleanup restores native visuals.

### Keyguard / AOD

- separate host adapters;
- same domain state and sizing policy;
- explicit lifecycle and fallback;
- do not infer support from Home host behavior.

### Future size / spacing

- setting changes sizing/layout inputs only;
- no scene-specific hook rewrite;
- slot/glyph/gap/optical values stay independent;
- larger visuals request only geometry that the verified host contract can safely own.

---

## 12. Architecture implication for the current work branch

The reference evidence successfully redirected the project away from the permanent extra-participant / occupancy-handoff route.

Current target-specific status:
- existing-host Home composition is implemented;
- scoped represented-slot exclusion and reversible clip masking are implemented;
- HostSession-scoped cleanup/fail-native boundaries are implemented;
- native charging/Super-Island motion is inherited from `system_icon_area`;
- Build 397 device validation accepted the corrected charging-carrier behavior;
- Build 398 strengthens the stable width source to the live `battery_icon_container`;
- Build 399 is a painter-only battery-intensity checkpoint and does not reopen these architecture decisions.

Builds 386-396 remain useful historical evidence but are not the current design premise.

The reference library should continue to guide Phase 2B and later scene work at the level of ownership, lifecycle, restoration and projection patterns. Exact target contracts must still be proven independently before new writes are introduced.


## 13. Retained state carrier is not visible Tint authority

Build-416 Hot Reload device evidence adds a presentation-ownership distinction to the existing masking pattern.

A represented native status View may remain attached, measured, event-driven and tint-capable because Guiyuan deliberately preserves its SystemUI lifecycle. That does **not** make the represented View authoritative for the tint of what the user currently sees after its visual has been replaced/masked.

For Home monochrome tint resolution:
- represented Guiyuan slots (`wifi`, `mobile`, `stacked_mobile`, `airplane`, `no_sim`, and any future `combined_status` participant itself) are state/lifecycle carriers, not visible peer/anchor candidates;
- prefer a genuinely visible, non-represented Home peer as the location-aware `DarkIconDispatcher.getTint(...)` anchor and static/applied tint peer;
- require the peer to be visible with positive layout geometry;
- when no eligible visible peer exists, fall back to the native manager/global/cached authority rather than forcing a dark-mode refresh or inventing a color.

This rule is especially important across module Hot Reload: represented native Views can temporarily retain presentation state that a full SystemUI recreation would rebuild, while neighboring visible SystemUI icons already reflect the current surface tint.


## 14. Tint event trigger and visible Tint authority must share one snapshot

Builds 415-417 establish that Home monochrome Tint has two distinct concepts that must not be conflated:

- a **native event trigger** such as Battery `onDarkChangedInternal`;
- the **visible Home status-icon authority** used to decide the monochrome direction of the composition.

A trigger may be timely while a cached authority value is stale. Likewise, a valid status-icon authority observation can occur before a new renderer generation is attached. Combining values from different event generations produces a mixed-scene snapshot even when each individual value is valid.

For Guiyuan:
- resolve the current visible Home status-icon Tint synchronously when committing a Battery-triggered renderer Tint update;
- a status-icon observation updates only status-icon authority and must not replay unrelated Battery state;
- after Hot Reload, transfer is a temporary visual-continuity seed only; fresh new-generation native authority supersedes transferred status Tint before visible ownership begins;
- if live status-icon authority is unavailable, fail toward the current native Battery applied tint rather than reuse a stale embedded status-icon field;
- do not add polling, delayed retries, forced DarkIcon refreshes, or a second native Tint writer to compensate for ordering.

The exact-target SystemUI-Reference currently verifies the Home host/status-icon/Battery/scene contracts used by this path, but does **not** establish a stable `DarkIconDispatcher.addDarkReceiver/removeDarkReceiver` registration contract for this artifact. Therefore direct receiver registration is not introduced without separate DEX/runtime verification.


### Build-418 acceptance evidence

Build 418 device validation accepts the snapshot rule above on the pinned HyperOS target.

Observed in the accepted Detailed session:
- Hot Reload restores the Home host without a SystemUI restart and the new generation resolves a visible non-represented status-icon anchor;
- the renderer begins with matching `appliedTint` and `statusIconTint`;
- across repeated native dark/light transitions, `appliedTint`, `statusIconTint`, and the live SystemUI status-icon authority advance together through the same intermediate values;
- repeated entry/exit no longer reproduces the stale white/black inversion seen in Builds 415-417.

This supports the ownership rule that a native event may trigger a renderer update, but the visible status-icon authority must be resolved for the same commit generation. It does not justify a second color writer, polling, delayed retry, or direct DarkIcon registration on an unverified contract.


## 15. Notification-shade target evidence from Build 414

The completed Build-414 bounded probe is historical evidence for Phase 2B; the probe itself is retired from the active runtime after Build 418 integration.

Observed on the pinned target:
- `NotificationHeaderExpandController$notificationCallback$1.onExpansionChanged(float)` follows the native notification-header expansion path;
- captures at native boundary buckets 0/1/7/8 did **not** expose a direct status-icon target View from the controller;
- the only direct Android `View` discovered on the controller was `realClockIcons`;
- controller-level `notificationTranslationX=2` and `notificationTranslationY=-109` remained stable in those captures and therefore are not sufficient by themselves to define Guiyuan target geometry;
- the one-time controller field inventory exposed narrower ownership seams including `headerController: Lazy` and `notification: NotificationShadeWrapper`.

### Consequence

Do not interpolate Guiyuan toward `realClockIcons` or the two controller translation scalars. They are evidence about the header controller, not a verified status-icon projection endpoint.

The next notification-shade review should follow the controller/wrapper ownership chain and identify:
1. the actual status-icon/native-header surface that owns target layout;
2. its target bounds/location at native endpoints;
3. its native tint authority;
4. the lifecycle boundary for attaching/removing a draw-only Guiyuan projection.

Only if exact-target source/reference review cannot resolve one of those facts should a new bounded runtime diagnostic be added. A completed diagnostic hook should not remain resident after its evidence is captured.

---

## 16. Build-420/421 panel ownership correction — host lifecycle over Battery scene inference

Later Phase-2B device evidence supersedes the open Notification-Shade continuation described in section 15.

### Verified target behavior

- The pinned Notification Shade does not expose/present the status-icon row as a Guiyuan projection target; it remains native-only.
- Build 420 device validation accepts Control Center projection through the native `realSystemIcons` / `MiuiStatusBatteryContainer` carrier and its readiness-ordered handoff.
- Build 421 device diagnostics show `MiuiBatteryMeterView.updateState()` can emit raw status-bar state `1` at a Notification-Shade boundary while `KeyguardManager.isKeyguardLocked` is also `true`.
- That Battery/global-Keyguard combination fires before the verified shade-fraction owner and therefore cannot be used as a second Home-visibility authority.
- Build 423 still drew Home Guiyuan in the parent `MiuiNotificationStatusContainer.overlay`; Build-424 exact-target review shows the native end-side visibility animation is instead applied to child `R.id.system_icons`, so the parent overlay does not inherit that child-specific transition.

### Reusable principle

Do not reconstruct a global scene state machine from a retained native presentation carrier when the real host and panel owners already expose their lifecycles.

For the current target:
1. **Home HostSession:** native `MiuiNotificationStatusContainer / system_icon_area`.
2. **Home visual carrier:** child `R.id.system_icons` / `MiuiStatusBatteryContainer.overlay`, which is the exact `mEndSideContent` animated by the native Home binder.
3. **Notification Shade:** no Guiyuan projection or project-local visibility writer; Home departure/return is inherited from the native end-side carrier.
4. **Control Center:** verified projected carrier + coordinator handoff.
5. **Battery status state:** read-only presentation/tint event context; not Home visibility.
6. **Keyguard/AOD:** separate native hosts/adapters; no inference from Home Battery state.

This removes a competing writer instead of refining it with another boolean. It also keeps future Keyguard/AOD support explicit and host-scoped rather than coupling those scenes to Home's Battery presentation internals.


---

## 17. Build-424 exact-target Home end-side carrier correction

**Observed evidence.**

Exact SystemUI `17.03.260226.r` source/resources establish the Home visibility chain:

`StatusBarVisibilityInteractor.shouldHomeStatusBarBeVisible`
→ `HomeStatusBarViewModelImpl.isSystemInfoVisible`
→ `systemInfoCombinedVis`
→ `HomeStatusBarViewBinderInjector`
→ `mEndSideContent`.

`HomeStatusBarViewBinderImpl.bind()` resolves `mEndSideContent` from `R.id.system_icons`. In the exact `status_bar.xml`, `MiuiNotificationStatusContainer @id/system_icon_area` includes `@layout/system_icons`; the exact `system_icons.xml` root is `MiuiStatusBatteryContainer @id/system_icons` and contains the native status-icon group and Battery.

`HomeStatusBarViewBinderInjector.showEndSideContent()/hideEndSideContent()` drives that end-side carrier through native alpha / visibility / translation animation. The Build-423 Guiyuan Home visual was attached one level above it in `MiuiNotificationStatusContainer.overlay`, so it could not naturally inherit a transition written specifically to the child `system_icons` View. The project-local Header-fraction gate therefore became a competing visibility decision and exposed first/last-frame ordering differences.

**Reusable principle.**

When a native View subtree already owns presentation visibility and transition motion, place a draw-only replacement inside that exact animated carrier when its geometry/lifecycle contract is verified. Prefer:

`native visibility semantic -> native animated carrier -> module-owned overlay child`

over:

`native motion callback -> project-local visibility state -> parent overlay`.

Observing a native animation callback does not grant ownership of the visibility property it helps animate.

**Guiyuan adoption.**

Build 424 moves only the Home render overlay to `MiuiStatusBatteryContainer(system_icons).overlay` and removes the Notification Header runtime Hook and Home eligibility state. Slot reservation, native represented-slot masking, Control Center projection, domain state, tint, and native geometry ownership remain separate and unchanged.

The legacy Hot Reload shade-eligibility payload field may remain null for transfer-format compatibility; it is not an active state source.

**Not established.**

This exact Home carrier contract does not establish Keyguard/AOD support and does not authorize reusing the Home carrier for those surfaces. Control Center remains a real second-host projection and keeps its accepted readiness-ordered handoff.


---

## 17. Build-424 correction — render inside the native Home visibility owner

This section supersedes section 16 only where section 16 describes the active Home drawing carrier or Notification-Shade handoff authority. Section 16 remains historical evidence for the Build-420/421 investigation.

### Exact-target visibility chain

The pinned SystemUI target already owns the complete Home system-information visibility decision:

`StatusBarVisibilityInteractor.shouldHomeStatusBarBeVisible`
-> `HomeStatusBarViewModelImpl.isSystemInfoVisible`
-> `HomeStatusBarViewModelImpl.systemInfoCombinedVis`
-> `HomeStatusBarViewBinderInjector.bindSystemInfoVisibility`
-> `mEndSideContent = R.id.system_icons`.

The exact `system_icons` layout root is `MiuiStatusBatteryContainer`. Native `showEndSideContent()/hideEndSideContent()` applies the end-side alpha/visibility/transition behavior to that owner.

### Build-423 failure mechanism

The Build-423 Home renderer was attached to the parent `MiuiNotificationStatusContainer.overlay`. That kept Guiyuan outside the child `system_icons` visibility/alpha owner. A project-local Notification Header fraction gate therefore became a second visibility system and could switch at a different first/last frame than native Home system information.

Header expansion progress remains valid motion evidence, but device evidence proves it is not the complete Home-visibility authority.

### Build-424 rule

Use the parent `MiuiNotificationStatusContainer / system_icon_area` as the Home HostSession discovery boundary, but attach visible Guiyuan drawing to `MiuiStatusBatteryContainer(system_icons).overlay`.

Consequences:
1. Notification Shade needs no Guiyuan visibility Hook or reconstructed scene state.
2. Home Guiyuan inherits native `system_icons` draw/alpha/visibility lifetime.
3. Existing Battery-derived slot width remains the local layout authority; the carrier correction does not justify geometry changes.
4. Control Center remains a real second-host projection through `realSystemIcons` and keeps readiness-ordered handoff.
5. Keyguard/AOD remain separate native surfaces.

Reusable principle: when SystemUI already exposes the rendered owner that receives the authoritative visibility decision, place project-owned visual composition inside that owner rather than observing a lower-level animation signal and recreating the visibility decision.

## Exact-target Control Center source / transition / destination ownership evidence

**Status:** verified reference evidence for HyperOS SystemUI `17.03.260226.r`; not yet a production Guiyuan contract.

Exact-target decompilation shows that HyperOS separates three different roles rather than treating Control Center as one status-bar surface:

1. **Source status-bar owner**
   - `MiuiPhoneStatusBarView` registers its `MiuiStatusBatteryContainer` as `ControlCenterFakeViewController.statusBarSystemIcons`.
   - `MiuiKeyguardStatusBarView` registers its lockscreen `mSystemIconsContainer` as `keyguardSystemIcons`.
   - `ControlCenterFakeViewController.adjustRealSystemIcons()` selects the current source from `StatusBarState`; Guiyuan should not duplicate that unlocked/keyguard routing logic.

2. **Control Center transition owner**
   - `ControlCenterFakeStatusIcons` is a complete native status-bar presentation, not only an anchor.
   - It owns an independent `StatusBarLocation.QS_FAKE` icon group, `MiuiStatusIconContainer`, `MiuiBatteryMeterView`, and `MiuiStatusBatteryContainer(system_icon_area)`.
   - It registers native dark/tint/config/island lifecycle on attach and removes the corresponding callbacks/icon group on detach.
   - Its width is synchronized from the selected real source by `ControlCenterFakeViewController.updateFakeStatusIconsSize()`.
   - `setStatusBarState()` and `setKeyguardStatusBarColors()` give the fake presentation native unlocked/keyguard tint semantics.

3. **Fully expanded Control Center owner**
   - `CombinedHeaderController.controlCenterStatusBar` / `controlCenterSystemIcons` is the independent native Control Center status-bar presentation.
   - `ControlCenterHeaderExpandController.controlCenterCallback.onExpansionChanged(float)` applies native translation to both fake and real Control Center status-bar presentations.
   - `onAppearanceChanged(boolean, boolean)` is the native visual-ownership switch: appearance=true shows the real Control Center status bar and fades the fake bar out; appearance=false does the inverse.
   - `PanelExpandController` exposes `getAppearance()`, so the current native ownership value is queryable without reconstructing it from a local expansion threshold.

Additional Home evidence:
- `ControlCenterContainerController.expandStateForStatusBar` drives `HomeStatusBarViewBinderInjector.mControlPanelExpand`.
- While that value is true, `updateSystemInfoIconVisibilities()` calls `hideEndSideContent(false)`; when false, it calls `showEndSideContent(false)`.
- Therefore a Guiyuan visual placed inside the native Home `system_icons` carrier can inherit Home departure/return without a project-local Control Center visibility writer.

### Candidate implication for later review

A lower-maintenance transition architecture may be possible:

`source steady carrier -> native QS_FAKE transition carrier -> native QS destination`

with Guiyuan rendering only inside verified source/fake carriers and inheriting native translation/alpha/tint, instead of maintaining a project-owned Control Center motion/appearance state machine.

This is **not yet an implementation decision**. Before promotion, review:
- first-frame readiness if the fake carrier is prepared before gesture start;
- exact ownership/masking of represented native Wi-Fi/mobile/Battery inside `QS_FAKE`;
- Hot Reload / recreation behavior;
- whether one low-frequency fake-view lifecycle attachment is preferable to the current Build-420 visible-event projection;
- interaction with the later lockscreen steady adapter.

Do not derive the fake-to-real switch from fraction/epsilon/timer logic when the native appearance lifecycle is available.


### QS_FAKE readiness and suppression refinement

Existing Build-423 device diagnostics provide a useful ordering sample for the current target:
- Control Center `visible=true` reached the project at 07:51:32.034; the current projection completed attach/readiness in that same timestamp.
- The first later captured Control Center expansion sample at 07:51:32.205 already shows native Home `mEndSideContent` at alpha 0 / INVISIBLE.
- This is evidence that the existing low-frequency Control Center visibility seam can become ready before the native Home end-side carrier leaves in this observed sequence. It weakens the case for adding a separate fake-view lifecycle Hook solely for first-frame readiness.
- The ordering, not the measured millisecond gap, is the reusable fact; reverse/close ordering still requires focused validation for any future QS_FAKE implementation.

QS_FAKE does not block Wi-Fi/mobile/Battery through its native `RIGHT_BLOCK_LIST`. Its `system_icons.xml` is the same `MiuiStatusBatteryContainer + MiuiStatusIconContainer + MiuiBatteryMeterView` structure used by Home.

Exact `MiuiStatusIconContainer.onMeasure()` confirms that `ignoredSlots`, not visual clipping, determines whether represented slots participate in native measurement/underflow. Therefore a future compact QS_FAKE carrier must use scoped/reversible slot exclusion in addition to visual masking.

The existing Home suppression mechanism already installs one global set of three class Hooks:
- `MiuiStatusIconContainer.onMeasure`;
- `MiuiStatusIconContainer.onLayout`;
- `MiuiStatusBatteryContainer.setIsHideBattery`.

A lower-overhead generalization candidate is to retain those same Hooks and route only owned carrier instances through an identity-keyed session registry, rather than installing a second Hook set for QS_FAKE.

The Home end-reservation formula is also structurally carrier-local: it replaces the native Battery end reservation with the compact requested slot width. Build-423 normal-state evidence had stable carrier width = actual Battery width = requested compact width = 105, yielding zero padding delta. For charging/island variants, `MiuiBatteryMeterView` writes island hide state to its own associated `MiuiStatusBatteryContainer`; QS_FAKE binds its Battery to its own container. This supports evaluating the same reservation policy from each carrier's local native state rather than copying Home state into QS_FAKE.

These are architecture candidates, not authorization to refactor Build 424.


---

## HyperOS Control Center source / transition / destination ownership

**Exact-target evidence — SystemUI 17.03.260226.r.**

The pinned target already separates the Control Center status-bar lifecycle into distinct native responsibilities. This is stronger evidence than deriving scene state from panel fraction.

### Source registration

Unlocked source:
- `MiuiPhoneStatusBarView.onFinishInflate()` resolves `R.id.system_icons` as `mStatusBatteryContainer`.
- `MiuiPhoneStatusBarView.initDependence(...)` assigns that exact `MiuiStatusBatteryContainer` to `ControlCenterFakeViewController.statusBarSystemIcons`.
- The assignment is followed by `adjustRealSystemIcons()`.

Keyguard source:
- `MiuiKeyguardStatusBarView` registers its `mSystemIconsContainer` as `ControlCenterFakeViewController.keyguardSystemIcons`.
- That assignment is also followed by `adjustRealSystemIcons()`.
- Keyguard color changes are forwarded to `controlCenterFakeStatusBar.setKeyguardStatusBarColors(...)`.

`ControlCenterFakeViewController` therefore owns the native source selection for Control Center transitions; Guiyuan should not duplicate an unlocked-vs-keyguard routing state machine.

### Transition representation

The Control Center fake status bar is a complete native status-bar representation rather than a geometry-only shell:
- it owns a `MiuiStatusBatteryContainer`, status-icon container and Battery View;
- it uses the native `StatusBarLocation.QS_FAKE` icon group;
- it participates in native tint, island and attach/detach lifecycle;
- the source status-bar state is used to select unlocked/keyguard presentation semantics.

`ControlCenterHeaderExpandController.onExpansionChanged(...)` applies native transition geometry to the Control Center-side fake/real status-bar representations while reading the current source `realSystemIcons` as the anchor.

### Destination ownership

The fully expanded Control Center has its own native status-bar representation (`StatusBarLocation.QS`), distinct from the source Home/Keyguard `realSystemIcons`.

Native `appearance` is a separate semantic from `visible`, `expansion`, and `tracking`:
- `appearance=false` selects the fake Control Center status bar visually;
- `appearance=true` selects the real Control Center status bar visually;
- expansion motion and visual ownership are therefore separate native axes.

The concrete plugin-side producer of `appearance` is outside the SystemUI APK reviewed here. Do not infer its internal threshold or reproduce it from fraction. The verified SystemUI consumer contract is sufficient to establish that fake/real visual ownership already exists natively.

### Reusable architecture implication

A strong candidate lifecycle is:

`source native carrier (Home or Keyguard)`
→ `native QS_FAKE transition carrier`
→ `native QS destination carrier`.

This is **evidence, not yet a production decision**. Before replacing the current Build-420 transition mechanism:
- verify the fake carrier at runtime on the pinned device;
- verify first/last-frame continuity and Hot Reload/bootstrap behavior;
- review whether attaching Guiyuan inside the fake carrier can inherit native translation/alpha/tint without adding a second appearance/fraction writer.

No project-local six-state scene machine is justified by the current evidence.


### Android ViewOverlay inheritance note

AOSP `ViewOverlay` / `ViewGroupOverlay` is rendered from the host View's own draw path after the host content/children. The internal overlay group redirects invalidation to the host rather than acting as an independent window/surface.

Architecture implication for the pinned target:
- an overlay attached to `MiuiStatusBatteryContainer(system_icons)` participates in that host's render-node visibility/alpha/translation lifecycle;
- an overlay attached one level above, on `MiuiNotificationStatusContainer`, does **not** automatically inherit child-specific animations applied only to `system_icons`;
- this supports Build 424's carrier correction and explains why moving the same visual between those two overlays changes lifecycle behavior without adding a new scene writer.

This Android framework behavior is supporting platform evidence; exact HyperOS ownership still comes from the target SystemUI binder/source chain.


### Post-Build-424 QS_FAKE integration candidate

The exact-target review now supports a bounded candidate for a later checkpoint. This is not part of Build 424.

Use the existing native axes rather than a project-owned scene machine:

1. **Source capability**
   - HyperOS selects `ControlCenterFakeViewController.realSystemIcons`.
   - For the current implementation, compact transition is eligible only when that object is the Home `MiuiStatusBatteryContainer` already structurally owned by `SysUiPresentationOwner`.
   - Do not add a generalized source registry before a second compact source (Keyguard) actually exists.

2. **Transition surface activation**
   - Keep the existing low-frequency `ControlCenterExpandControllerDelegate.onVisibleChanged(boolean)` seam as the transition-surface active/inactive signal.
   - It is not Home visibility authority and must not write source-scene visibility.
   - On entry, resolve the already-selected native source and the native QS_FAKE carrier; on exit, restore QS_FAKE native visuals and stop transition-renderer work.
   - If runtime evidence shows source identity can change while the Control Center remains visible, `ControlCenterFakeViewController.adjustRealSystemIcons()` is the verified low-frequency source-change seam. Do not substitute Battery status state.

3. **QS_FAKE presentation**
   - resolve through the existing Header object chain:
     `ControlCenterHeaderExpandController.headerController -> CombinedHeaderController.controlCenterFakeStatusBar -> delegate.statusBarArea`;
   - render in the native fake `MiuiStatusBatteryContainer.overlay`;
   - preserve the parent fake-status-bar translation/alpha lifecycle as SystemUI-owned;
   - use the same bounded presentation-layer mechanism proven on Home: temporary represented-slot exclusion only during the exact target `MiuiStatusIconContainer.onMeasure/onLayout` call plus reversible View clip masks;
   - do **not** rely on `MiuiLightDarkIconManager` block-list entries for modern Wi-Fi/mobile: the target `ModernStatusBarView.setBlocked(boolean)` implementation is a no-op;
   - do **not** assume end reservation is unnecessary merely because HyperOS synchronizes fake `statusBarArea` width from `realSystemIcons`: `MiuiStatusBatteryContainer` still measures Battery separately and subtracts its width from `statusIcons`. A future fake-carrier adapter must verify a local reservation contract against exact target geometry before writing it.

4. **Native motion / visual ownership**
   - native expansion code continues to own fake-bar translation;
   - native `appearance` continues to own fake-vs-real Control Center alpha;
   - the real QS destination remains untouched/native.

5. **Existing hook reuse**
   - no new tint hook is needed: `SysUiTintSource` already receives all `MiuiBatteryMeterView` tint events and the fake session can filter by its own Battery instance;
   - no second status-icon layout hook is needed: the existing `MiuiStatusIconContainer.onMeasure/onLayout` interception can route explicitly registered host-scoped presentation sessions by target identity;
   - prefer extracting the current Home presentation-layer slot-exclusion / clip-mask mechanism over reviving the older binding-level network suppression owner as the default transition implementation.

The transition cutover should remain readiness ordered locally:
- entry: fake compact renderer ready -> apply fake native clip masks;
- exit/failure: restore fake native clips -> stop/hide compact renderer.

A QS_FAKE failure therefore degrades only that transition surface to native SystemUI and does not deactivate the Home compact owner.


### QS_FAKE batteryWidthDiff independence

Exact-target `ControlCenterHeaderExpandController` computes `batteryWidthDiff` from the selected source anchor Battery width and the **real QS destination Battery** width. During island handling it may replace that difference with the negative real-QS Battery width. `onExpansionChanged(float)` then adds the resulting value to the whole `controlCenterFakeStatusBar.translationX`.

The calculation does **not** read QS_FAKE `ignoredSlots`, local `statusIcons.paddingEnd`, or a Guiyuan compact reservation. Therefore a future carrier-local QS_FAKE exclusion/reservation policy does not feed back into the native parent translation formula, provided Guiyuan never writes/cancels the fake parent translation.

This narrows the future island/charging device gate to local compact-edge/layout continuity; native Control Center motion ownership remains structurally independent.


### QS_FAKE modern-network suppression constraint

Further exact-target review closes an important false lead.

`MiuiLightDarkIconManager.setBlockList(...)` copies its input into an instance-local block list and calls `StatusBarIconControllerImpl.refreshIconGroup(...)`. That refresh does invoke `StatusIconDisplayable.setBlocked(...)` for matching children. However the target's modern Wi-Fi/mobile views inherit from `ModernStatusBarView`, whose `setBlocked(boolean)` override is an empty implementation. Therefore the icon-manager block list does **not** suppress the modern Wi-Fi/mobile pipeline used on this target.

Implication:
- do not use the QS_FAKE block list as the Guiyuan network replacement mechanism;
- the leading lightweight route is to generalize the **current presentation-layer** mechanism already used by Home: temporary `ignoredSlots` ownership around native measure/layout plus reversible clip masks for represented Wi-Fi/mobile/airplane/no-SIM/Battery Views;
- the older binding-identity suppression path remains historical/fallback evidence, not the default QS_FAKE design;
- keep one global status-icon/Battery hook set where possible and route only explicitly registered host-scoped presentation sessions; do not duplicate network state machines.


---

## External implementation cross-check: root-space transition geometry

**Reviewed from user-supplied APKs with JADX 1.5.6; architectural evidence only.**

Legacy CombinedStatus 1.3.6-mod.5 and 1.4.3 decompile to byte-identical core geometry sources for `ClosedAnchor`, `MotionHandoff`, `CompactGeometry` and `KeyguardHandoff`. The newer legacy package therefore does not prove that the old closed-anchor/project-correction route fixed its historical endpoint behavior.

KeiMi 2.5.0 provides a useful independent contrast:
- participant Views are transformed with `View.transformMatrixToGlobal(...)` and then the chosen root's `transformMatrixToLocal(...)`;
- source and target participants are stored as six root-space geometry components and interpolated directly;
- unmatched native participants are rendered from their native View, preserving internal native optical scaling rather than assuming the outer View box is the glyph box.

Reusable Guiyuan principle:

`verified steady source anchor + native source-carrier translation -> absolute root-space role-6 target`

is safer than:

`source relative to carrier A -> target relative to carrier B`

when carrier A/B have independent layout/translation ownership.

The external code is not copied. Guiyuan keeps its own source witness, rendering model, reservation, lifecycle and fail-native contracts.


---

## Transition reservation lifecycle: semantic occupancy must not become a second motion system

**Build-506 finding, corrected by Build 507 device evidence.**

Build 506 correctly identified the risk: a semantic reservation must not invent a second gesture timeline beside the native QS_FAKE motion. Its proposed one-shot final-width cutover was subsequently device-rejected because it jumped the native peer row to its final horizontal layout at gesture entry.

The current contract is:

`resolve/freeze final total semantic width -> interpolate compact-to-final width from raw HyperOS expansion progress -> commit before HyperOS consumes the same sample`.

The reservation writer may therefore update `MiuiStatusIconContainer.paddingEnd` across native expansion samples, but it does **not** own an independent timing curve, delayed phase, per-participant union trajectory, or animator. Build-609 device evidence adds one QS_FAKE capacity requirement: the 478px fake status-icon row can underflow a native peer before HyperOS transfers appearance ownership to the final row. Build 611 therefore leases only the fake carrier's already-empty parent capacity before visual cutover: HyperOS first establishes the native carrier width, Guiyuan expands that sole child once to the parent content width, then `statusIcons.paddingEnd` remains the only progress-driven property. Build-611 device evidence then proves that the expanded `MiuiStatusIconContainer` width must not itself become transition motion geometry: 478 -> 728px shifts its raw center about 125px left and made the whole Guiyuan transition jump left. Build 612 keeps the capacity lease but samples motion from an end-anchored logical sub-carrier whose width is frozen from the native source motion carrier. Thus lease-only leading capacity is invisible to the motion matrix. Any later native/third-party carrier-width write is treated as an ownership conflict and fails native; Guiyuan does not write peer visibleState, alpha, visibility, translation, or appearance timing.

This differs from both rejected extremes:

- one-shot final occupancy, which causes an immediate final-x jump;
- independently evolving/per-span occupancy, which can create a horizontal dead zone followed by late reflow.

For latent 0→1 / 1→N participants, layout occupancy and pixel reveal remain separate. Reservation follows the frozen semantic-width contract; pixels appear only when the real target visual envelope is spatially available. This keeps **layout occupancy**, **native motion**, and **visual reveal** as distinct responsibilities without creating a second animation engine.


---

## Participant visual snapshot: topology, not provider identity

A transition target must be adapted from what is actually rendered, not from the package/module that produced the View hierarchy.

The reusable target path is:

`semantic participant -> visual snapshot -> optical envelope/components/topology -> target geometry / morph capability`.

The snapshot contract is read-only:
- ImageView: clone the current drawable from `ConstantState`, preserve state/level, raster-probe alpha, then map the resulting geometry through the ImageView drawable frame/imageMatrix into View coordinates.
- ViewGroup/composite: recursively collect visible drawable-bearing descendants and transform their visual components into one parent coordinate space.
- cache cloned-drawable probe results by `ConstantState + level + drawable state + layoutDirection`;
- never tint, resize or rasterize the live SystemUI drawable.

Topology is structural:
- four aligned ascending vertical components may expose a `FOUR_VERTICAL_BARS` capability;
- extra rows/dots/components keep the target composite;
- single/unknown structures expose only the geometry that is reliably measured.

Do not discard small secondary components merely because they are small relative to a dominant bar. Filtering uses an absolute probe-pixel floor so legitimate lower dots remain available to topology classification.

Consumers share the same evidence:
- generic target projection uses the snapshot optical envelope in participant View space;
- Mobile may consume exact component rectangles when four-bar topology is reliable;
- unsupported topology falls back instead of creating a provider-specific ratio;
- latent reveal may use real visual width for its short reveal phase while native slot occupancy remains SystemUI layout authority.

Provider names such as HyperCeiler may remain in historical diagnostics, but must not choose the primary runtime geometry algorithm.

---

## Exact component topology and transition basis

**Build-509 evidence, corrected by Build 510 device acceptance.**

When a target snapshot exposes exact sub-components, its topology, optical envelope and component rectangles are one evidence set. Build 509 proved that the four native bars must be measured individually, but applying that evidence by non-uniformly stretching the entire Mobile component while the four dots also morphed into bars produced a visible double-deformation / rubber-band effect.

The current `FOUR_VERTICAL_BARS` contract is therefore:

- the **outer Mobile participant** keeps the existing carrier/similarity projection and its normal scale policy;
- the **inner four-bar morph** owns the exact measured x/width/top/bottom changes;
- measured target-envelope width/height ratios compensate inside the bar geometry for any uniform outer shrink, so the final native rectangles remain reachable without anisotropically stretching the whole component canvas.

Exact component evidence is shape-local, not permission for a full affine transform of the whole Mobile participant.

Composite, single-glyph and unknown topologies remain on the conservative similarity/fallback path. Provider or module identity must not select the geometry algorithm.

Latent 0→1 / 1→N reveal remains spatial. The existing end reservation opens a real interval from the Battery end; the target visual envelope is revealed continuously as that reservation covers it, while root-space target proximity remains a second safety bound. Do not replace this with a duration, expansion-fraction threshold or delayed runnable.



### QS_FAKE Battery-island peer-spacing refinements

Build 679 adds one narrow Battery-island exception to the **native peer-spacing adapter**, not to logical occupancy. Device evidence shows that when HyperOS keeps the Battery-island QS_FAKE row visibly authoritative, committing future total semantic width to that row creates an empty gap before Guiyuan has visually occupied the space. While native Battery island is active, the native peer reservation may therefore use the current union of the same frozen spans at the same raw HyperOS progress, bounded by the logical semantic reservation. The logical reservation itself still follows the frozen-final-total-width contract above, and latent reveal still consumes that logical reservation.

This does not restore per-span occupancy as the general transition policy. It is a scene-specific projection from existing semantic spans to the one native `statusIcons.paddingEnd` writer so native peers remain adjacent to currently occupied Guiyuan space while HyperOS retains island/root/appearance authority.


Build 680 corrects the remaining coordinate-frame error in that adapter. The frozen target spans are expressed relative to the final QS Battery end, while `statusIcons.paddingEnd` is consumed relative to the currently translated QS_FAKE end. Under Battery-island motion those ends are not the same frame. The adapter must therefore project the final target end into the live fake-row end frame before interpolating the span union. This uses the observed native carrier positions; it does **not** read, duplicate or cancel HyperOS' `batteryWidthDiff` formula, and it does not create a new translation writer. Logical semantic reservation remains unchanged.


Build 682 narrows the adapter one step further: a projected span union is not itself a valid `paddingEnd` value. Once target spans are expressed in the live QS_FAKE end frame, logical `x=0` is the peer/end boundary. Only occupancy at `x<=0` can displace native peers; any projected extent at `x>0` is on the end side and remains drawable occupancy only. Battery-island peer reservation therefore uses the depth from `x=0` to the left-most projected span, with the runtime compact width as floor and semantic reservation as cap. This is a coordinate-semantic rule, not a device compensation; no Battery width or island offset is hard-coded.
