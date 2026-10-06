# Current Development State

## Repository / build

- Product / stable checkpoint: Guiyuan 0.2.1.
- Stable `main`: Guiyuan 0.2.1 / Build 746 (`20261006-746`), with the behavior-neutral maintainability cleanup promoted through PR #247 after exact-head Full validation.
- Integration `dev`: Guiyuan 0.2.1 / Build 746 at `8cf504c1`, with #248/#249 runtime-plumbing maintenance and #250 coverage-gap audit merged. Runtime behavior and Build identity remain unchanged; `main` is still at the PR #247 stable checkpoint.
- PR #221 is merged after exact-head Runtime CI, Work-branch Canary, focused maintainer device validation, and integrated `dev` validation.
- PR #223 is merged as repository / exact-target compatibility-contract maintenance without changing runtime behavior or Build identity.
- PR #224 / Build 745 was a bounded Control Center alpha-layer performance experiment. Device validation exposed mobile-signal clipping, so the PR was closed unmerged and the experiment is rejected.
- Historical PR #197 remains superseded and must not be restored as an active implementation route.

## Accepted runtime baseline

- Home remains an independent presentation owner.
- Keyguard and AOD share one host-scoped family renderer/presentation owner and retarget scene semantics without duplicate mutable owners.
- AOD is not a Control Center transition source.
- QS_FAKE is the bounded Control Center bridge; the fully expanded Control Center remains native.
- HyperOS remains authoritative for native scene, island, appearance, motion, alpha, visibility and translation timing.
- One transition-reservation writer remains. Fake-carrier capacity is a bounded visible-cycle lease and fail-native restores only the affected surface.
- The Home steady-peer mirror is Home-source data. Build 744 rejects it for Keyguard/UNKNOWN Control Center sources and restores it only when Home becomes authoritative again.
- Build 689-693 removed or bounded the previously identified diagnostic/reflection hot-path costs. Detailed diagnostics remain observational and must not change functional behavior.
- Build 745 does not belong to the accepted runtime line. Do not reintroduce bounded per-component alpha layers or compensate the observed clipping with guessed padding/margins.

## Active objective

The maintainability coverage-gap audit is closed.

PR #250 was reviewed against accepted `dev@50f5aa2`, passed exact-head Runtime CI #2883, and was squash-merged to `dev` as `8cf504c1`. Integrated `dev` validation #2884 then passed the signed Canary path, including target-profile verification, unit tests, APK build, Modern Xposed metadata, Haple signature and non-debuggable checks.

The audit used #217 and #228-#248 changed-files plus intervening merged feature/fix work to identify only genuinely uncovered or invalidated areas. It found three bounded issues: an unused active-subscription result shell, unconditional `ready` wording in a compatibility summary, and one self-proof duplicate geometry test. Caller-level review excluded the remaining candidates where their complexity or abstraction carried a real process, transition, optical-geometry, protocol or runtime-observation contract.

No device validation is required: no renderer, geometry, Hook, ownership, transition timing or fail-native behavior changed, and both exact-head and integrated-dev automated validation passed.

Current priorities:

1. start the next coherent task from current `dev@8cf504c1`;
2. do not reopen the coverage-gap audit just to chase shorter names, fewer classes, zero warnings or stylistic uniformity;
3. continue treating synthetic metrics / fixed self-proof diagnostics as high-priority defects if newly introduced;
4. keep `main` unchanged until a separate dev-to-main promotion is explicitly chosen.

## Non-negotiable bounds

- Root-cause first; prefer verified native/upstream contracts.
- Keep one owner/writer per mutable surface.
- Do not add polling, delay, retry loops, custom gesture clocks, guessed thresholds or fixed device geometry.
- Do not take over native translation, alpha, visibility or final-QS appearance ownership.
- Do not weaken fail-native or restoration boundaries to gain smoothness.
- Do not trade accepted Home, Keyguard/AOD, charging-island or Control Center geometry for speculative performance work.
- A branch name alone does not make a route active; require code/PR/CI/device evidence that agrees with the current objective.

## Immediate next

- Treat Build 744 as the accepted runtime-behavior baseline; Build 746 remains the current 0.2.1 build identity.
- Use accepted `dev@8cf504c1` as the integration base; #250 is merged and its maintainability coverage-gap audit is closed.
- Do not reopen the rejected alpha-layer experiment or continue runtime-plumbing cleanup without a concrete maintenance or compatibility problem.
- Keep comments concise and natural; explain lifecycle or platform constraints, not obvious code.
- External version remains 0.2.1 until the maintainer explicitly authorizes another display-version bump.
