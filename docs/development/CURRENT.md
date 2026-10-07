# Current Development State

## Repository / build

- Product / stable checkpoint: Guiyuan 0.2.1.
- Stable `main`: Guiyuan 0.2.1 / Build 746 (`20261006-746`), with the behavior-neutral maintainability cleanup promoted through PR #247 after exact-head Full validation.
- Integration `dev`: Guiyuan 0.2.1 / Build 746 at `06635a7e`, with #248-#252 maintenance integrated and validated. Runtime behavior and Build identity remain unchanged; `main` is still at the PR #247 stable checkpoint.
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

The residual runtime-plumbing cleanup is closed.

PR #252 was reviewed against accepted `dev@fa0e32c5`, then squash-merged to `dev` as `06635a7e`. The accepted net cleanup removes remaining synthetic/self-proof plumbing, keeps suppression results state-only, models the mutually exclusive Home native-AOD fallback as one explicit phase, folds low-value Policy wrappers, and shortens internal names where surrounding scope already supplies the missing context.

The review explicitly retained independent Home-AOD origin/prearm facts, Keyguard readiness/lease facts, larger transition/slot/runtime-access types, partial-install results, and fail-native reasons where they carry real lifecycle or domain meaning. Do not reopen those merely to reduce character counts, Boolean counts, class counts, or compiler warnings.

Validation history:

- Full CI #2903 exposed one missed `ControlCenterRenderSession` reference after `SysUiPresentationOwner.StateResult -> Result`; no runtime or test failure was involved.
- the missed reference was corrected at `5dcf8814`;
- exact-head Full CI #2904 passed target-profile verification, Kotlin compilation, unit tests, Debug build, Modern Xposed metadata and non-debuggable checks;
- #252 squash-merged as `06635a7e`;
- integrated `dev` Full CI #2905 passed target-profile verification, tests, Debug/Canary builds, Modern Xposed metadata, Haple signature, non-debuggable checks and artifact upload.

No separate device gate is required for this batch because review and CI leave no unresolved device-only behavior question. No display-version or Build bump was made.

Current priorities:

1. start the next coherent task from current `dev@06635a7e`;
2. treat the residual plumbing cleanup as closed unless a concrete maintenance, compatibility or runtime defect appears;
3. continue rejecting synthetic metrics, self-proof diagnostics and success/failure contracts without a real failure source;
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

- Keep Build 744 as the accepted runtime-behavior baseline; Build 746 remains the current 0.2.1 build identity.
- Use `dev@06635a7e` as the accepted integration base.
- Do not reopen the completed plumbing pass just to shorten already-meaningful names, collapse independent state, or chase zero warnings.
- Keep comments concise and natural; explain lifecycle or platform constraints, not obvious code.
- External version remains 0.2.1 until the maintainer explicitly authorizes another display-version bump.
