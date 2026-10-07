# SystemUI Integration Contracts

This reference records reusable target-SystemUI evidence that may inform Guiyuan work. It is not a dependency declaration, implementation-lineage statement or archive of investigation history.

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

Current target evidence is scoped to Xiaomi HyperOS SystemUI `17.03.260226.r`.

A contract verified here must not be assumed valid on another SystemUI build without revalidation.

## Existing native host

**Observed.** When a suitable native end-side host already owns layout and motion, composing Guiyuan inside that host avoids creating a second permanent layout identity.

**Guiyuan rule:** prefer:

`verified native host -> Guiyuan presentation`

over:

`native host + permanent custom participant -> occupancy handoff`.

Home uses the existing native end-side carrier. Keyguard/AOD use their own verified family host. Other scenes need independent host/lifecycle proof.

## Represented-slot suppression

**Observed.** The target status-icon container exposes an ignored-slot contract consumed by native measure/layout.

Safe mutation:
- preserves existing platform ignored slots;
- adds only the represented entries Guiyuan owns;
- restores only that owned delta;
- uses the shortest lifetime that still covers the native presentation consuming the state;
- fails native if another writer makes ownership ambiguous.

For Home, native-call scope is sufficient for the relevant layout boundary. A verified transition-capable host may require the owned delta for the presentation session because native motion can consume layout state outside one measure/layout call.

The reusable principle is lifecycle-scoped ownership, not universally temporary or universally persistent suppression.

## Reversible visual masking

**Observed.** A represented native View may remain attached and lifecycle-active while its pixels are hidden through a reversible clip mask.

Safe masking:
- snapshots the prior clip;
- applies only the module-owned empty clip;
- restores only the value it owns;
- leaves native state/tint/lifecycle delivery intact.

Visual masking is not compact-layout readiness and should not be replaced with permanent `GONE`, alpha racing or translation writes.

## Host-scoped runtime state

**Guiyuan rule:** prefer:

`Host -> HostSession -> owned resources`.

A host/session boundary identifies the authoritative host, observed native participants, presentation mode, owned mutations and cleanup behavior. Detached or replaced hosts must not leave mutable ownership or host-derived geometry globally reusable.

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
- final appearance must not be replaced by a project `fraction >= x` rule;
- a Guiyuan transition visual may inherit/read native transforms without becoming the native motion owner;
- project-local geometry must not double-apply translation already present on its parent native surface.

## QS_FAKE and final Control Center

**Observed.** The verified target exposes a fake transition surface and a distinct final native surface.

**Guiyuan rule:** use the fake surface only as bounded transition context and yield to the native final surface. Final participant membership, alpha/visibility, peer placement and final-only participants remain native-owned.

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
