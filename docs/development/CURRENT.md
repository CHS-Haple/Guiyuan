# Current Development State

This file records accepted runtime behavior and near-term guardrails, not a Build-by-Build log. Read the actual branch refs, Gradle identity and CI before merging or distributing an APK.

## Current 0.7 line

- **Source checkpoint:** `dev` now carries Guiyuan **0.7.0 / Build 20261015-1025** (Android versionCode `261015025`) after the B1025 device acceptance and [#541](https://github.com/CHS-Haple/Guiyuan/pull/541). `main` remains the accepted **0.7.0 / B1024** checkpoint; no stable promotion was requested. Each branch's `gradle.properties` is the Build authority.
- **Release evidence:** `main` Full CI [#38729068886](https://github.com/CHS-Haple/Guiyuan/actions/runs/38729068886) and Release workflow [#38729068985](https://github.com/CHS-Haple/Guiyuan/actions/runs/38729068985) succeeded. The 0.7.0-internal / B1024 signed distribution APK passed [#38727695573](https://github.com/CHS-Haple/Guiyuan/actions/runs/38727695573). No formal GitHub Release or 1.0.0 version was published.
- **Device evidence:** the maintainer reported 0.6.1 B1023 **normal** on Xiaomi 15 Pro / Android 17 / SystemUI `17.03.260226.r`. The final B1023 branch `4bb6a666` completed Runtime CI [#38726385425](https://github.com/CHS-Haple/Guiyuan/actions/runs/38726385425); its post-acceptance edit only reuses a native mobile witness list inside one CC drawing frame. The previously signed B1023 Canary [#38722771150](https://github.com/CHS-Haple/Guiyuan/actions/runs/38722771150) covers the original accepted runtime. Neither proves unrelated SystemUI targets or hypothetical failure paths.
- **Integration:** [#531](https://github.com/CHS-Haple/Guiyuan/pull/531) integrated the accepted B1023 runtime and [#532](https://github.com/CHS-Haple/Guiyuan/pull/532) promoted B1024 release identity. [#541](https://github.com/CHS-Haple/Guiyuan/pull/541) subsequently integrated the B1025 CC native-target cache membership repair to `dev` after focused device acceptance and successful Runtime CI [#38754181997](https://github.com/CHS-Haple/Guiyuan/actions/runs/38754181997) and signed Canary [#38754302770](https://github.com/CHS-Haple/Guiyuan/actions/runs/38754302770). Abandoned standalone experiments were not shipped.

## Accepted behavior

- Home uses the verified native SystemUI end-side carrier; Keyguard and AOD share one host-scoped family owner; Notification Shade and fully expanded Control Center remain native. AOD never serves as a Control Center transition source.
- The standard mobile presentation remains four dots. Dual-row presentation is opt-in and requires two authoritative active subscriptions and complete SIM-slot mapping. Missing identity uses the normal single-row/native-safe fallback without discarding the dual preference; single/dual settings banks remain separately remembered.
- During eligible partial Control Center expansion, the four-dot source collapses toward its first dot, moves to a verified native anchor, spreads and grows into native geometry. Two independent roots require separately verified subscription identity. A one-root composite may use **shape-only** grouped BARS/DOTS morph if exactly two four-glyph rows are optically verified; that does not prove each native row belongs to a given SIM.
- Center 5G/5GA morphing requires a matching native glyph and source/provenance checks. Verified aggregated-root presentation can differ from the data SIM; unknown or disagreeing targets retain the bounded no-target/native fallback.
- Native FAKE/FINAL width and peer spacing are measured from their own native frames. Charging changes retarget the existing reservation writer; native final layout, alpha, translation and visibility remain SystemUI-owned.

## Ownership, cache and Hot Reload

- Every mutable native presentation property has a single owner; `SysUiPresentationOwner` owns only bounded masking/reservation changes, and releases the module's changes on session end. CC projections are host/session-scoped.
- `StatusPainter` native-resource caches and drawable optical snapshots include native resource configuration; CC mobile-target witnesses invalidate on verified mobile bind events. B1025 additionally rechecks cached native FINAL slot membership, slot identity and optical-child ancestry before reuse. No polling or per-frame complete View-tree scan is added merely to cover theoretical third-party changes.
- Hot Reload validates the transfer before taking over the old Hook handles; successful generation handoff is explicit and bounded. Companion UI progress is owned by the Application, not a navigated page. Invalid transfer or failed takeover reports restart required rather than inventing a safe handoff.
- Preview Sandbox remains local simulation and does not inject fake network state into SystemUI.

## Open compatibility questions

- [#519](https://github.com/CHS-Haple/Guiyuan/issues/519): the V9 Hot Reload payload directly carries an old-generation cleanup `Runnable`. API 102 requires classloader-neutral saved state and old-generation external-callback retirement; the inspected Vector implementation rejects such a direct array element. This is a source-grounded compatibility gap, **not** a reproduced B1024 regression or evidence of a leak on the installed framework. Preserve single-writer, main-thread and fail-native contracts; do not remove the deferred cleanup without a safe handoff design.
- [#521](https://github.com/CHS-Haple/Guiyuan/issues/521): B1025 rejects moved/retagged cached targets, but an entirely new native mobile target introduced without an authoritative binder/presentation event while existing witnesses stay valid can still evade invalidation. No universal third-party coverage is claimed.

These are explicit compatibility limitations, not observed B1025 regressions. Investigate only with reproducible evidence or an authoritative upstream contract; preserve Fail native.

## Next

Keep B1025 on `dev` as the accepted CC cache checkpoint while B1024 remains on `main` until explicitly promoted. For [#519](https://github.com/CHS-Haple/Guiyuan/issues/519), resolve the API 102 transfer-contract gap against the actual installed framework and design a safe generation handoff before changing runtime. Pursue [#521](https://github.com/CHS-Haple/Guiyuan/issues/521) only with reproducible native membership/event evidence. Do not reopen superseded experiments; preserve the current target's native ownership and fail-native behavior. 1.0.0 remains a separate decision.

