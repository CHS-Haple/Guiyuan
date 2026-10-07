# SystemUI integration contracts

## Purpose

This note records reusable contracts derived from the verified target SystemUI and accepted runtime behavior.

It intentionally excludes product-comparison history, Build chronology, reverse-engineering tooling notes and abandoned implementation candidates. Those details are not required to understand the current contract.

Current architecture authority remains `docs/architecture/` plus `docs/development/CURRENT.md`.

## Target scope

Current target evidence is scoped to Xiaomi HyperOS SystemUI `17.03.260226.r`.

A contract verified here must not be assumed valid on another SystemUI build without revalidation.

## 1. Reuse an existing native host

**Observed.**

When a suitable native end-side host already owns layout and motion, composing Guiyuan inside that host avoids creating a second permanent layout identity.

Reusable rule:

`verified native host -> Guiyuan presentation`

is preferred over:

`native host + permanent custom participant -> occupancy handoff`.

**Guiyuan rule.** Home uses the existing native end-side carrier. Keyguard/AOD use their own verified family host. Other scenes require their own host and lifecycle proof.

## 2. Represented-slot suppression follows the consuming lifetime

**Observed.**

The target status-icon container exposes an ignored-slot contract consumed by native measure/layout.

Safe mutation requires:

- preserve the platform's existing ignored slots;
- add only the represented entries Guiyuan owns;
- restore only that owned delta;
- use the shortest lifetime that still covers the native presentation that consumes the state;
- fail native if another writer makes ownership ambiguous.

For Home, native-call scope is sufficient for the relevant layout boundary. For a verified transition-capable host, the owned ignored-slot delta may need to live for the presentation session because native motion can consume layout state outside one measure/layout invocation.

The reusable principle is therefore lifecycle-scoped ownership, not universally temporary or universally persistent suppression.

## 3. Visual masking is separate from layout suppression

**Observed.**

A represented native View may remain attached and lifecycle-active while its pixels are hidden through a reversible clip mask.

Safe masking:

- snapshots the prior clip;
- applies only the module-owned empty clip;
- restores only the value it owns;
- leaves native state/tint/lifecycle delivery intact.

Do not use permanent `GONE`, alpha racing or translation writes merely to suppress duplicate pixels.

A mask never proves compact layout readiness.

## 4. Runtime state is host-scoped

**Guiyuan rule.**

Prefer:

`Host -> HostSession -> owned resources`.

A host/session boundary should identify:

- the authoritative native host;
- the native participants it observes;
- the presentation mode;
- owned masks/exclusions/reservation state;
- cleanup/restoration behavior.

Detached or replaced hosts must not leave geometry or mutable ownership in a global cache.

### Same-host semantic retarget

When Keyguard and AOD resolve to the same verified native family host, one presentation owner may retarget semantics without creating a second mutable owner.

### Cross-host handoff

A distinct target host may prepare a bounded reversible visual claim, but it cannot inherit source-host layout readiness.

## 5. Slot size and visible glyph size are independent

**Observed.**

Keep separate:

- native slot occupancy;
- Guiyuan logical viewport;
- visible glyph/ink bounds;
- per-glyph scale;
- optical adjustment;
- transition endpoint geometry.

One width value must not silently serve all of these purposes.

User scale belongs in shared presentation geometry. It must not automatically become a native slot-width or native translation write.

## 6. Native hide and scene facts remain authoritative input

**Observed.**

Native Battery hide, scene and appearance state are inputs to eligibility and reservation decisions.

Do not rewrite a native hide request merely to preserve Guiyuan's previous geometry. Repairing the downstream effects of such a rewrite creates a second scene/layout authority.

Prefer:

`native fact -> Guiyuan policy`

over:

`native fact -> module override -> geometry repair`.

## 7. Native motion and appearance are distinct contracts

**Observed.**

On the verified Control Center path, translation/progress and fake/final appearance are owned by separate native callbacks.

Consequences:

- expansion progress is motion context, not a visibility threshold;
- final native appearance must not be replaced by `fraction >= x`;
- a Guiyuan transition visual may inherit or sample native transforms without becoming the native motion owner;
- project-local geometry must not double-apply a translation already present on the parent native surface.

## 8. QS_FAKE and final-QS remain different surfaces

**Observed.**

The verified target exposes a fake Control Center status surface and a distinct final native surface.

Guiyuan's bounded bridge uses the fake surface as transition context but yields to the native final surface.

The final surface remains authoritative for:

- final native participant membership;
- final alpha/visibility;
- final peer placement;
- final-only participants.

Guiyuan must not suppress the final native surface merely to preserve transition continuity.

## 9. Projected transition geometry uses real endpoints

**Guiyuan rule.**

For an eligible compact-to-native transition:

`verified source geometry + verified target geometry + native progress -> draw-only Guiyuan projection`.

Prefer this to:

`fixed offset + custom duration + independent interpolator`.

Source position must come from the verified native source carrier plus Guiyuan's stable logical basis. A retained Guiyuan View may provide size/basis evidence, but it is not automatically global-position authority after its steady scene has yielded.

Target position must come from verified native final participants. Internal child geometry may refine optical shape but must not erase a valid top-level participant trajectory.

## 10. Semantic correspondence does not transfer native ownership

**Guiyuan rule.**

The transition visual may represent:

- 1 -> 1 correspondence, such as one compact semantic converging on one native target;
- 1 -> N split when one compact semantic represents multiple verified final native participants;
- 0 -> 1 reveal for a final-only semantic.

This correspondence exists only in Guiyuan's transition drawing.

Native final Views retain their own alpha, visibility, translation and lifecycle.

If required target identity or geometry is unavailable, omit the projection and fail native where necessary rather than inventing a target.

## 11. Reservation is occupancy, not a second gesture timeline

**Guiyuan rule.**

A transition may need changing native peer occupancy while SystemUI owns motion.

The one verified reservation writer may expose projected semantic occupancy to native layout, but:

- it must not run an independent timing curve;
- it must not translate peers directly;
- it must not derive width from temporary drawable folds/morphs;
- represented native slots remain under their verified session suppression contract;
- reservation cleanup follows the owning transition lifecycle.

Carrier-capacity leasing and per-progress reservation are different responsibilities. Width leased only for measurement capacity must not be sampled as transition displacement.

## 12. Tint authority follows the visible presentation

**Observed / Guiyuan rule.**

A represented native View may remain attached as a state/lifecycle carrier after its pixels are replaced. That does not automatically make it the best tint authority for Guiyuan's visible pixels.

Prefer a genuinely visible, non-represented native peer or the verified native tint manager/global authority. Do not force a refresh or invent a color when no valid visible peer exists.

For Control Center transition drawing, use the verified native peer/final presentation tint facts rather than creating a second tint state machine.

## 13. Cleanup is part of compatibility

Every owned mutation needs a restoration boundary.

Cleanup must cover the applicable cases:

- feature disable;
- host detach/replacement;
- SystemUI recreation;
- scene/family exit;
- transition failure;
- Hot Reload generation replacement.

Restore only module-owned state. If a live value no longer equals the value Guiyuan applied, treat that as another writer and avoid destructive restoration.

## 14. Fail-native boundary

Reference evidence never justifies speculative ownership.

Fail native when any required contract is unavailable or ambiguous, including:

- host identity;
- participant identity;
- geometry;
- lifecycle;
- writer ownership;
- source/target eligibility;
- compatibility.

Failure should disable only the smallest affected presentation while leaving unrelated accepted surfaces intact.

## Evidence limits

This document establishes reusable ownership/lifecycle/geometry principles for the verified target. It does not establish universal compatibility across HyperOS versions or devices.

Exact class/member/resource identities belong only where they are necessary to describe the pinned target. Historical experiments remain available in Git history and should not be reintroduced into current policy without new evidence.
