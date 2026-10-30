# SystemUI Integration Contracts

This reference records reusable target SystemUI evidence that may inform Guiyuan work. It is not a dependency declaration, implementation lineage statement or archive of investigation history.

## Reference rules

- Keep reusable platform behavior and ownership contracts, not incidental third-party project identities.
- Do not copy third-party source code, proprietary assets or implementation-specific constants.
- Keep target-specific identifiers only when they are necessary to describe a verified SystemUI contract.
- Separate observed platform behavior from Guiyuan policy.
- Evidence does not grant write ownership by itself; revalidate host, lifecycle, writer set, fallback and device behavior before adopting a new write.
- Keep the current reusable conclusion here. Build chronology, tooling notes and superseded candidates belong in Git history or [DECISIONS.md](../development/DECISIONS.md).

Evidence language:
- **Observed** — directly supported by target source/resource inspection or runtime evidence.
- **Strong inference** — supported by multiple observations but not exposed as one explicit platform contract.
- **Guiyuan rule** — an adopted project constraint based on current evidence.
- **Not established** — insufficient evidence to use as a design premise.

## Target scope

Current target evidence is scoped to Xiaomi HyperOS SystemUI `17.03.260226.r`. The previously supplied `系统界面_17.03.260226.r(6).apk` was byte-verified (SHA-256 `a0e738e41fe599b97950cbf52a9e2ddc6ae2ceff986efbacb1c9840bea78768d`, three DEX files).

**Verified target DEX boundary.** `MiuiStatusIconContainer` extends `AlphaOptimizedLinearLayout`, which extends Android `LinearLayout`; neither target class declares a `setPaddingRelative` override. The target `MiuiStatusIconContainer.onLayout(ZIIII)V` invokes inherited `getPaddingStart()` and `getPaddingEnd()`; native `addIgnoredSlots(List)` calls `List.add` and `requestLayout`, while `setIgnoredSlots(List)` calls `List.clear` before `addIgnoredSlots`. These are verified method definitions/invocations, **not** proof of the inherited Android setter's internal exception ordering, synchronous reentry, other modules' writers or actual runtime failure. Inspect the matching Android framework binary when that behavior becomes decision-critical; AOSP is secondary context.

**Observed native Padding consumers (DEX call-site offsets).** Re-reading the same SHA-256-verified APK's `classes3.dex` gives the following method invocation positions, measured in DEX *16-bit code units* from the enclosing method's first instruction:

- `MiuiStatusIconContainer.onLayout(ZIIII)V`: inherited `getPaddingEnd()` at offset **108**, `getPaddingStart()` at **114**.
- `MiuiNotificationStatusContainer.onLayout(ZIIII)V`: `getPaddingStart()` at **13** and **20**; its `onMeasure(II)V` reads `getPaddingStart()` at **195** and `getPaddingEnd()` at **200** and **210**.
- `MiuiStatusBatteryContainer.onLayout(ZIIII)V`: `getPaddingEnd()` at **33**, **56**, **109**, **132**, **163** and **177**; `getPaddingStart()` at **167** and **173**.
- `MiuiStatusIconContainer.addIgnoredSlots(Ljava/util/List;)V`: `List.contains` at **18**, `List.add` at **26**, and `requestLayout` at **30**. `setIgnoredSlots(Ljava/util/List;)V`: `List.clear` at **2**, then `addIgnoredSlots` at **5**.

These are invocation references in the target bytecode, not a full path-condition or runtime callback trace. In particular, none establishes the actual inherited Android `setPaddingRelative()` exception/reentry order. Native padding influences more than the Guiyuan glyph bounds, so any session-owned padding acquisition/rollback must account for the native measure/layout consumers and preserve single-writer and foreign-change boundaries.

**Observed native Padding writers (DEX and resource table).** On the same SHA-256-verified target, `MiuiPhoneStatusBarView.updatePaddings()V` in `classes2.dex` invokes inherited `setPaddingRelative(IIII)V` on three recipients:

- `findViewById(0x7f0b0b53)`, named `status_bar_contents` in the packaged `resources.arsc`: call at instruction offset **97**.
- `findViewById(0x7f0b0be6)`, named `system_icons`: call at offset **107**.
- Its `mPrivacyArea: LinearLayout` field: call at offset **139**.

**Additional native initialization writer (exact receiver).** `MiuiPhoneStatusBarView.onFinishInflate()V` in `classes2.dex` obtains resource `0x7f0b0be5` (`system_icon_area`) at offsets **86–93**, stores it in `mSystemIconArea: View`, and, in the `MiuiConfigs.IS_PAD` branch, calls inherited `View.setPaddingRelative(IIII)V` on that field at offset **180**. The receiver is the `system_icon_area` View, **not** the separately loaded `mStatusBarStatusIcons: MiuiStatusIconContainer` (`statusIcons`, `0x7f0b0b4e`). This is an initialization-time native Padding write to a different recorded View; it does not establish a same-View writer collision, inherited Android setter exception order, or live host identity.

These calls and resource names were verified from DEX invoke registers, field references and APK resource-entry names. The same target's `MiuiPhoneStatusBarView.onFinishInflate()V` loads `statusIcons` (`0x7f0b0b4e`) as `MiuiStatusIconContainer` into `mStatusBarStatusIcons`, while `system_icons` (`0x7f0b0be6`) is cast to the distinct `MiuiStatusBatteryContainer` and stored as `mStatusBatteryContainer`; `system_icon_area` (`0x7f0b0be5`) is another View. The observed native Padding writes on these recipients do **not** prove a second writer to Guiyuan's checked `MiuiStatusIconContainer`. Parent/peer Padding can still influence native measurement.

**Observed CC FAKE Padding writer (target DEX).** In the same SHA-256-verified APK, `CcFakeStatusBarIcons.updateStatusBarLayout()V` in `classes.dex` reads `statusBarInner: LinearLayout`, updates that View's `FrameLayout.LayoutParams.height` from `SystemBarUtils.getStatusBarHeight`, and invokes its `setLayoutParams` at code-unit offset **31**. The method then reads `statusBarArea: MiuiStatusBatteryContainer` at **34**, loads a dimension from resource `0x7f071937` at **38–44**, and calls inherited `ViewGroup.setPaddingRelative(IIII)V` on that `statusBarArea` at **46** (other three arguments are zero). This is an observed **CC FAKE-specific receiver**, not Guiyuan Home's `MiuiStatusIconContainer` and not evidence of a same-View Padding collision. The target DEX alone does not reveal the inherited Android setter's exception/reentrancy behavior, the dimension's semantic name or the actual runtime View identity across scene recreation.

**Observed Home manager construction (DEX).** In `classes3.dex`, `StatusBarRootKt$$ExternalSyntheticLambda3.invoke(Object)` reads `statusIcons` (`0x7f0b0b4e`) as `MiuiStatusIconContainer` (offsets **171–178**) and passes that same register to `SwitchingProvider$37.create` at **182**. The factory in `classes2.dex` forwards the group to `IconManager.<init>` at **69**; that constructor stores the parameter in `mGroup` at **26**. The callback installs the manager on `MiuiPhoneStatusBarView` through `setDarkIconManager` at **193**. Guiyuan's `NativeParticipantAccess.groupFor` reads that `mGroup`, establishing the original Home `statusIcons` View as its construction-time source.

**Observed Home root initialization on the *same* icon-group View (DEX).** In `StatusBarRootKt$$ExternalSyntheticLambda3.invoke(Object)`, the native builder resolves `R.id.statusIcons` as `MiuiStatusIconContainer` for `DarkIconManager` and later resolves that **same resource ID** as `LinearLayout`. At `classes3.dex` code-unit offset **279**, it invokes inherited `setPaddingRelative(IIII)V` on the latter after reading that very View's current start/top/end/bottom Padding; the preceding `HomeStatusBarIconBlockListBinder.bind` has been called, while `HomeStatusBarViewBinderImpl.bind` and the initializer callback follow. This **is** an original-SystemUI setter on Guiyuan's Home icon-group receiver, but it reapplies live values, not a distinct target Padding. The timing of actual Guiyuan session acquisition, setter reentrancy and any conflicting native/third-party mutation is not established by this static sequence.

**Observed Home manager lifecycle (DEX).** `HomeStatusBarIconBlockListBinder.bind` in `classes3.dex` calls `RepeatWhenAttachedKt.repeatWhenAttached$default` at **7**. Its coroutine registers the manager through `StatusBarIconControllerImpl.addIconGroup` at **33**, enters `repeatOnLifecycle(STARTED)` at **51**, and on exit invokes `DarkIconManager.destroy` at **67** or **87** before removing the manager from `mIconGroups` at **72** or **92**. In `classes2.dex`, `DarkIconManager.destroy()` calls `mGroup.removeAllViews()` at **27** without clearing the `mGroup` field. A retained group reference therefore does not by itself prove that native icon binding is still active.

**Not established:** live group/manager identity after all host recreation or third-party changes, whether an attached host can temporarily retain a retired manager, inherited Android `setPaddingRelative` exception/reentry order, or a divergent desired Padding value / runtime race at the verified same-View initialization setter. Verify the exact native View, manager membership and generation before changing rollback; do not infer write ownership from a resource name, ancestor or equal numeric Padding.

A contract verified here must not be assumed valid on another SystemUI build without revalidation.

## Existing native host

**Observed.** When a suitable native end-side host already owns layout and motion, composing Guiyuan inside that host avoids creating a second permanent layout identity.

**Guiyuan rule:** prefer:

`verified native host -> Guiyuan presentation`

over:

`native host + permanent custom participant -> occupancy handoff`.

Home uses the existing native end-side carrier. Keyguard/AOD use their own verified family host. Other scenes need independent host/lifecycle proof.

## Represented-slot suppression

**Observed.** The target status-icon container exposes an ignored-slot contract consumed by native measure/layout. On the verified `MiuiStatusIconContainer`, `addIgnoredSlots` inserts missing entries one by one and requests layout, while `setIgnoredSlots` first clears the entire native list before calling that add method. The setter is therefore a whole-list replacement, not an atomic delta rollback.

**Observed native ignored-slot callers (same target DEX).** Other SystemUI components also write native ignored lists; method ownership must be traced to the actual container:

- Home `MiuiPhoneStatusBarView.updateCutoutLocation()V` invokes `mStatusBarStatusIcons.setIgnoredSlots(MiuiIconManagerUtils.RIGHT_BLOCK_LIST)` at `classes2.dex` code-unit offset **9**. This is the Home icon container, and the call replaces its list when the native cutout-space check permits the method to proceed.
- `MiuiQSHeaderView.onAttachedToWindow()V` invokes `mStatusIconContainer.setIgnoredSlots(CONTROL_CENTER_BLOCK_LIST)` at `classes2.dex` offset **144**. `ControlCenterStatusBarIcon.onAttachedToWindow()V` invokes `delegate.statusIcons.addIgnoredSlots(CONTROL_CENTER_BLOCK_LIST)` at `classes.dex` offset **73**, after the native icon manager registration. Both use their respective QS/CC container fields, not the Home `mStatusBarStatusIcons` field.
- The target `MiuiIconManagerUtils` static `RIGHT_BLOCK_LIST` and `CONTROL_CENTER_BLOCK_LIST` contain none of Guiyuan's five represented slot names (`wifi`, `mobile`, `stacked_mobile`, `airplane`, `no_sim`). This verifies those *static inputs*, not live list contents after callbacks, same-View writer overlap, or execution timing.

**Not established:** whether a native list replacement can interleave an active Guiyuan mutation on the same live View. Retain identity-scoped delta restoration and the shortest justified ownership lifetime; do not infer that separate callers share a View or that disjoint static inputs prove exceptional-path safety.

**Guiyuan rule:** if a native add fails, derive any rollback target from the current native list minus only the session-owned additions. A pre-call whole-list snapshot can discard unrelated SystemUI entries added in reentrant callbacks. Recheck the native result and fail native if restoration cannot be verified; do not claim protection against concurrent writers of the same slot name.

Safe mutation:
- preserves existing platform ignored slots;
- adds only the represented entries Guiyuan owns;
- restores only that owned delta;
- uses the shortest lifetime that still covers the native presentation consuming the state;
- falls back to native presentation if another writer makes ownership ambiguous.

For Home, native call scope is sufficient for the relevant layout boundary. A verified transition-capable host may require the owned delta for the presentation session because native motion can consume layout state outside one measure/layout call.

The reusable principle is lifecycle-scoped ownership, not universally temporary or universally persistent suppression.

## Reversible visual masking

**Observed.** A represented native View may remain attached and lifecycle-active while its pixels are hidden through a reversible clip mask.

Safe masking:
- snapshots the prior clip;
- applies only the module-owned empty clip;
- restores only the value it owns;
- leaves native state/tint/lifecycle delivery intact.

Visual masking is not compact layout readiness and should not be replaced with permanent `GONE`, alpha racing or translation writes.

## Host-scoped runtime state

**Guiyuan rule:** prefer:

`Host -> HostSession -> owned resources`.

A host/session boundary identifies the authoritative host, observed native participants, presentation mode, owned mutations and cleanup behavior. Detached or replaced hosts must not leave mutable ownership or host-derived geometry reusable across unrelated hosts.

When Keyguard/AOD resolve to one verified host, a single presentation owner may retarget semantics. A distinct target host may prepare a bounded reversible visual claim but cannot inherit source-host layout readiness.

## Sizing responsibilities

**Observed.** Native slot occupancy, Guiyuan logical viewport, visible glyph bounds, per-glyph scale, optical adjustment and transition endpoint geometry are distinct values.

**Guiyuan rule:** one width or scale must not silently control all of them. User visual scale is presentation geometry, not automatically a native slot-width or native-translation write.

## Native hide and scene facts

**Observed.** Native Battery hide, scene and appearance state are authoritative inputs.

**Guiyuan rule:** prefer:

`native fact -> Guiyuan policy`

over:

`native fact -> module override -> downstream repair`.

Do not rewrite a native scene/hide request just to preserve previous Guiyuan geometry.

## Native motion and appearance

**Observed.** On the verified Control Center path, translation/progress and fake/final appearance are owned by separate native callbacks.

Consequences:
- expansion progress is motion context, not a visibility threshold;
- final appearance must not be replaced by a project-defined `fraction >= x` rule;
- a Guiyuan transition visual may inherit/read native transforms without becoming the native motion owner;
- project-local geometry must not double-apply translation already present on its parent native surface.

## QS_FAKE and final Control Center

**Observed.** The verified target exposes a fake transition surface and a distinct final native surface.

**Guiyuan rule:** use the fake surface only as bounded transition context and yield to the native final surface. Final participant membership, alpha/visibility, peer placement and final-only participants remain native-owned.

**Target DEX: native FAKE layout lifecycle (2026-10-30 verification).** On the SHA-256-pinned `17.03.260226.r` APK (`a0e738e41fe599b97950cbf52a9e2ddc6ae2ceff986efbacb1c9840bea78768d`), direct instruction-aligned disassembly establishes:

- `ControlCenterFakeViewController$statusBarStateListener$1.onStateChanged(I)V` writes `statusBarState` and invokes `adjustRealSystemIcons()` at DEX code-unit offset **19**. That method selects `statusBarSystemIcons` in status state **0**, `keyguardSystemIcons` in state **1**, or null for other states; on a changed source, it removes the previous `OnLayoutChangeListener` at **23**, assigns the new source at **26**, registers the new listener at **30** and immediately calls `updateFakeStatusIconsSize()` at **33**. A retained old source is not a current size authority.
- `ControlCenterFakeViewController$onLayoutChangeListener$1.onLayoutChange` invokes `updateFakeStatusIconsSize()` at **12** only when the observed real icon View's **width or height** changed. `updateFakeStatusIconsSize` reads the current real icon width at **15**, assigns it to `CcFakeStatusBarIcons.statusBarArea`'s `LayoutParams.width` at **33**, calls `setLayoutParams` at **35**, checks `isInLayout` at **38** and `isLayoutRequested` at **44**. When a deferred layout is necessary it calls `post(Runnable)` at **73**; otherwise it may directly call `requestLayout` at **82**. The posted runnable calls `requestLayout` at offset **2**. These conditions prove that native layout convergence can cross a later message-loop boundary; they do **not** establish its maximum number of passes or timing.
- The native `ControlCenterFakeStatusIcons.onFinishInflate` resolves `statusIcons`, `battery`, `statusBarInner` and `statusBarArea` from its own root, while `onAttachedToWindow` registers the QS_FAKE icon group through `StatusBarIconControllerImpl.addIconGroup` at **90**. Thus inflated descendants, attached native manager and measured layout are distinct readiness facts.
- The present `CcRenderSession.PendingPrearm` adds a one-shot FAKE root layout/attach listener and requests an initial layout. Its `MAX_PREARM_LAYOUT_ATTEMPTS = 2` and repeat `host.requestLayout()` are **Guiyuan retry policies**, not constants or guarantees supplied by these native methods. In **24 distinct retained diagnostic TXT files**, **11** explicit `prearm state=armed attempt=1` records were observed and **zero** `prearm state=deferred`, `prearm state=failed` or `attempt=2` records; some reports contain transient `fake-carrier-native-layout-pending` followed by successful compact preparation. These are limited, user-supplied sessions, not exhaustive device coverage.

**Current decision:** retain host identity, attachment and native-layout-readiness checks as genuine contracts. Treat the two-attempt cap and self-requested layout as **unresolved heuristics**, not as a demonstrated production defect or native standard. Do not remove, increase or replace them without a failed/retry-exhausted same-session trace or another direct timing/ownership proof. Any future redesign must preserve one listener owner, successful-unregister receipts, detached-host cancellation, main-thread semantics, and native fallback. Direct DEX disassembly is not a fresh JADX/DexSQL execution or physical Android framework callback trace.

## Projection uses real endpoints

**Guiyuan rule:**

`verified source geometry + verified target geometry + native progress -> draw-only Guiyuan projection`

is preferred to:

`fixed offset + custom duration + independent interpolator`.

Source position comes from the verified native source carrier plus Guiyuan's stable logical basis. A retained Guiyuan View may provide size/basis evidence but is not automatically global-position authority after its steady scene yields.

Target position comes from verified final native participants. Internal child geometry may refine optical shape but must not erase a valid top-level participant trajectory.

## Semantic correspondence

**Guiyuan rule.** Transition drawing may represent 1→1 correspondence, a verified 1→N split, or a 0→1 final-only reveal.

That correspondence exists only in Guiyuan drawing. Native final Views keep their own alpha, visibility, translation and lifecycle. If required target identity/geometry is unavailable, omit the projection or fail native rather than inventing a target.

## Reservation is occupancy, not motion

**Guiyuan rule.** The one verified reservation writer may expose projected semantic occupancy to native layout while SystemUI owns motion.

It must not:
- run an independent gesture curve;
- translate peers directly;
- derive native width from temporary drawable folds/morphs;
- release represented slots mid-transition solely to animate occupancy.

Carrier-capacity leasing and per-progress reservation are separate. Width leased only for measurement capacity must not become transition displacement.

## Tint authority

**Observed / Guiyuan rule.** A represented native View may remain a state/lifecycle carrier after its pixels are replaced; that does not automatically make it the best tint authority for Guiyuan's visible pixels.

Prefer a genuinely visible non-represented peer or verified native tint manager/global authority. Do not force a refresh or invent a color when no valid authority exists.

## Native Wi-Fi binding across Hot Reload

**Observed — exact target DEX and device diagnostics.** `ModernStatusBarWifiView` inherits `ModernStatusBarView.binding`. After `MiuiWifiViewBinder.bind`, that native binding retains a `LocationBasedWifiViewModel`; the Home implementation is `HomeWifiViewModel`. Its `getWifiIcon(): StateFlow` exposes the current `WifiIcon.Visible` or `WifiIcon.Hidden` semantic value. The native binding's visibility query reads this same state. An existing View may survive an Xposed generation change without invoking its binder again.

**Guiyuan rule:** an attached, uniquely identified Wi-Fi View under the current verified Home host may be used as a **read-only** bridge to the existing native ViewModel during the main-thread Hot Reload restore. Verify the root, binding and model identities before reading the current Wi-Fi state. Preserve `Hidden` as a semantic fact: a visible ImageView or retained drawable tag does not establish current Wi-Fi visibility. Do not reuse a detached or ambiguous root, create simulated binder registrations, mutate the native binding, or infer signal level from connectivity transport alone.

**Not established:** exact binding shape on other SystemUI builds, an API 102 frame-atomic handoff, and universal recovery of a `Visible` Wi-Fi snapshot when no authentic transferable binding exists during reload preparation. A missing source must fail native rather than transfer an unverified live presentation.

## Native resource rendering

**Observed.** The target exposes semantic status-icon resource identity before final presentation transformation. Hidden semantic state remains authoritative even if a bound ImageView still retains old drawable/tag state.

On the verified Wi-Fi family:
- native slot height is `20dp`;
- resource geometry uses the same `20dp x 20dp` intrinsic/viewport basis;
- Light/base, Dark and Tint variants share geometry while presentation color/alpha differs;
- native presentation state selects the resource variant, and Tint mode additionally applies the current ImageView tint.

The steady native draw path is effectively:

`semantic resource -> native presentation variant -> ImageView Drawable -> final bounds -> framework draw`.

No Wi-Fi-specific final bitmap-resample layer was found in the verified path.

**Guiyuan rule:** when reusing a verified native resource, prefer:

`semantic resource -> native-compatible variant -> cloned Drawable -> resolved Guiyuan bounds -> direct Drawable draw`.

Keep semantic identity, native variant selection, optical measurement, Guiyuan placement, final Drawable rasterization and tint authority separate. A bounded raster probe may measure optical bounds, but it should not become the final rendered bitmap without separate evidence.

Do not use per-resource grayscale multipliers, alpha/coverage remaps, source-asset edits or screenshot-fitted constants as a substitute for the native presentation contract.

## Cleanup

Every owned mutation needs a restoration boundary covering the applicable cases: feature disable, host detach/replacement, SystemUI recreation, family/scene exit, transition failure and Hot Reload replacement.

Restore only module-owned state. If a live value no longer equals the value Guiyuan applied, treat it as another writer and avoid destructive restoration.

## Fail-native boundary

Fail native when a required host, participant identity, geometry, lifecycle, writer, source/target eligibility or compatibility contract is unavailable or ambiguous.

Failure should disable only the smallest affected presentation while leaving unrelated accepted surfaces intact.

## Evidence limits

This reference establishes reusable ownership, lifecycle, geometry and rendering principles for the verified target. It does not establish universal compatibility across HyperOS versions or devices.
