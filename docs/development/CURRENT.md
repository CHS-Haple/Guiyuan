# Current Development State

This file records accepted runtime behavior and near-term guardrails, not a Build-by-Build log. Read the actual branch refs, Gradle identity and CI before merging or distributing an APK.

## Current 0.7 line

- **Source checkpoint:** Guiyuan **0.7.0 / Build 20261015-1024** (Android versionCode `261015024`) promotes the accepted 0.6.1 B1023 runtime. Version identity is defined only in `gradle.properties`. The stable `main` boundary requires independent Full CI and a signed artifact; this description does not substitute for live branch/CI checks.
- **Device evidence:** the maintainer reported 0.6.1 B1023 **normal** on Xiaomi 15 Pro / Android 17 / SystemUI `17.03.260226.r`. The final B1023 branch `4bb6a666` completed Runtime CI [#38726385425](https://github.com/CHS-Haple/Guiyuan/actions/runs/38726385425); its post-acceptance edit only reuses a native mobile witness list inside one CC drawing frame. The previously signed B1023 Canary [#38722771150](https://github.com/CHS-Haple/Guiyuan/actions/runs/38722771150) covers the original accepted runtime. Neither proves unrelated SystemUI targets or hypothetical failure paths.
- **Integration:** [#531](https://github.com/CHS-Haple/Guiyuan/pull/531) squash-merged as `b8a0f33`, combining accepted B1016, B1017 configuration cache invalidation, B1018 native mobile bind invalidation, B1019 failed Hot Reload transfer handling and the original B1001 one-root aggregate morph. Abandoned B1021/B1022 experiments were not imported. Historical B1002 peer-spacing work was superseded by already merged B1007/B1011 native geometry repairs.

## Accepted behavior

- Home uses the verified native SystemUI end-side carrier; Keyguard and AOD share one host-scoped family owner; Notification Shade and fully expanded Control Center remain native. AOD never serves as a Control Center transition source.
- The standard mobile presentation remains four dots. Dual-row presentation is opt-in and requires two authoritative active subscriptions and complete SIM-slot mapping. Missing identity uses the normal single-row/native-safe fallback without discarding the dual preference; single/dual settings banks remain separately remembered.
- During eligible partial Control Center expansion, the four-dot source collapses toward its first dot, moves to a verified native anchor, spreads and grows into native geometry. Two independent roots require separately verified subscription identity. A one-root composite may use **shape-only** grouped BARS/DOTS morph if exactly two four-glyph rows are optically verified; that does not prove each native row belongs to a given SIM.
- Center 5G/5GA morphing requires a matching native glyph and source/provenance checks. Verified aggregated-root presentation can differ from the data SIM; unknown or disagreeing targets retain the bounded no-target/native fallback.
- Native FAKE/FINAL width and peer spacing are measured from their own native frames. Charging changes retarget the existing reservation writer; native final layout, alpha, translation and visibility remain SystemUI-owned.

## Ownership, cache and Hot Reload

- Every mutable native presentation property has a single owner; `SysUiPresentationOwner` owns only bounded masking/reservation changes, and releases the module's changes on session end. CC projections are host/session-scoped.
- `StatusPainter` native-resource caches and drawable optical snapshots include native resource configuration; CC mobile-target witnesses invalidate on verified mobile bind events. No polling or per-frame complete View-tree scan is added merely to cover theoretical third-party changes.
- Hot Reload validates the transfer before taking over the old Hook handles; successful generation handoff is explicit and bounded. Companion UI progress is owned by the Application, not a navigated page. Invalid transfer or failed takeover reports restart required rather than inventing a safe handoff.
- Preview Sandbox remains local simulation and does not inject fake network state into SystemUI.

## Open compatibility questions

- [#519](https://github.com/CHS-Haple/Guiyuan/issues/519): abnormal Hot Reload takeover/host-detach/partial-failure disposal still depends on Modern Xposed/framework contracts not fully proven. Do not blindly clean up old-generation owners on an unknown thread or create a second native writer.
- [#521](https://github.com/CHS-Haple/Guiyuan/issues/521): mobile child replacement that bypasses verified native binder/presentation events may leave a cached CC target until another authoritative update. The accepted event-driven cache repair does not prove universal third-party coverage.

These are explicit compatibility limitations, not observed B1023 regressions. Investigate only with reproducible evidence or an authoritative upstream contract; preserve Fail native.

## Next

Finish the separate 0.7.0 integration/stable validation and signed-APK evidence, then continue issue-driven compatibility work. After version promotion, keep `dev` and `main` branch roles intact; do not retain redundant active PRs or obsolete description claiming B1023 is unmerged. Update CHANGELOG only for durable release changes.

