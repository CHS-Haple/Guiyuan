# Current Development State

## Repository / build

- Product / stable checkpoint: Guiyuan 0.2.1.
- Stable `main`: Guiyuan 0.2.1 / Build 746 (`20261006-746`), with the behavior-neutral maintainability cleanup promoted through PR #247 after exact-head Full validation.
- Integration `dev`: Guiyuan 0.2.1 / Build 746 with PR #248 runtime-plumbing maintainability cleanup merged after exact-head Full validation. Runtime behavior and Build identity remain unchanged; `main` is still at the PR #247 stable checkpoint.
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

PR #248 is merged to `dev` after full base→HEAD review and exact-head Full CI #2879. The runtime-plumbing maintenance batch is closed.

The accepted cleanup now includes:

- synthetic diagnostic metrics and fixed self-proof tags removed from the touched runtime paths;
- full-AOD and Keyguard boundary handoff state consolidated where the fields described one lifecycle;
- thin Policy/Result wrappers, duplicate result hierarchies and a dead detach path removed;
- internal plumbing names shortened only where scope already carries the context;
- CONTRIBUTING updated to prohibit synthetic metrics and low-value abstraction wrappers.

No device validation is required for this batch: static lifecycle comparison, compile/tests, target-profile validation and exact-head Full CI resolved the engineering questions without leaving a device-only uncertainty.

Current priorities:

1. start the next coherent task from current `dev`;
2. preserve accepted ownership/lifecycle/fail-native contracts and Build 746 identity;
3. do not reopen this cleanup just to chase shorter names, fewer lines or zero compiler warnings;
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
- Begin future work from current `dev` at PR #248 merge commit `6f7c3a1`.
- Do not reopen the rejected alpha-layer experiment or continue runtime-plumbing cleanup without a concrete maintenance or compatibility problem.
- Keep comments concise and natural; explain lifecycle or platform constraints, not obvious code.
- External version remains 0.2.1 until the maintainer explicitly authorizes another display-version bump.
