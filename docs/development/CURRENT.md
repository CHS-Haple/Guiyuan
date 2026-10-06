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

Current work is on `refactor/runtime-plumbing`, based on synchronized `dev` at Guiyuan 0.2.1 / Build 746. This is a behavior-neutral maintainability pass for concrete runtime-plumbing problems found after the previous cleanup, not another line-count or naming-standardization sweep.

The current batch is limited to changes with clear maintenance value:

- remove synthetic diagnostic fields that were hard-coded rather than observed;
- collapse boolean fields that describe one lifecycle into explicit local state;
- remove one-line Policy/Result wrappers and dead lifecycle entry points;
- reuse one result type where two sealed hierarchies carried the same states;
- shorten plumbing names only where surrounding scope already supplies the missing context;
- keep ownership, fail-native, native geometry, transition timing and accepted Build 746 behavior unchanged.

Current priorities:

1. finish the full `dev -> refactor/runtime-plumbing` diff review and exact-head CI before integration;
2. preserve accepted ownership/lifecycle/fail-native contracts and Build 746 identity;
3. stop the cleanup when the remaining abstractions carry real domain, compatibility or lifecycle meaning;
4. request device evidence only if the final diff or CI leaves a runtime question that device evidence can actually resolve.

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
- Review the complete maintenance diff, run the branch CI once at the coherent checkpoint, and fix only concrete findings.
- Do not reopen the rejected alpha-layer experiment or broaden this branch into unrelated cosmetic cleanup.
- Keep comments concise and natural; explain lifecycle or platform constraints, not obvious code.
- External version remains 0.2.1 until the maintainer explicitly authorizes another display-version bump.
