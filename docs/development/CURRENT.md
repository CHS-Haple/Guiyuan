# Current Development State

## Repository / build

- Product / stable checkpoint: Guiyuan 0.2.1.
- Stable `main`: Guiyuan 0.2.1 / Build 746 (`20261006-746`), with the behavior-neutral maintainability cleanup promoted through PR #247 after exact-head Full validation.
- Integration `dev`: synchronized with stable `main` at Guiyuan 0.2.1 / Build 746 after PR #247 promotion closeout. Runtime behavior and Build identity remain unchanged.
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

PR #247 is merged after exact-head Full validation, and the long-lived `dev` branch has been restored/synchronized to the promoted stable state. The behavior-neutral maintainability review through PR #246 is closed.

The remaining large runtime files are ownership/lifecycle-dense, tooling-bound, or have no clear tested stateless boundary. Do not continue splitting or renaming them merely to reduce line count or standardize names; start another maintenance batch only when a concrete readability, ownership, dead-indirection, or compatibility problem has a clear net benefit.

Current priorities:

1. start future work from the synchronized `dev` branch;
2. preserve the accepted runtime ownership/lifecycle/fail-native contracts and Build 746 identity until a real behavior change or explicit version decision requires otherwise;
3. prefer high-value structural simplification over cosmetic churn;
4. request device evidence only when a future diff can plausibly change runtime behavior or an engineering decision.

## Non-negotiable bounds

- Root-cause first; prefer verified native/upstream contracts.
- Keep one owner/writer per mutable surface.
- Do not add polling, delay, retry loops, custom gesture clocks, guessed thresholds or fixed device geometry.
- Do not take over native translation, alpha, visibility or final-QS appearance ownership.
- Do not weaken fail-native or restoration boundaries to gain smoothness.
- Do not trade accepted Home, Keyguard/AOD, charging-island or Control Center geometry for speculative performance work.
- A branch name alone does not make a route active; require code/PR/CI/device evidence that agrees with the current objective.

## Immediate next

- Treat Build 744 as the accepted runtime baseline; Build 746 remains the stable 0.2.1 release/build identity.
- `main` and `dev` must remain synchronized at promotion closeout before the next work branch starts.
- Begin the next coherent task from current `dev`; do not reopen the closed alpha-layer performance experiment or resume low-value cosmetic cleanup without new evidence.
- Keep comments concise and natural, and apply the cross-file extraction review rule for imports, annotations, visibility, top-level constants, and receiver context.
- External version remains 0.2.1 until the maintainer explicitly authorizes another display-version bump.
