# Current Development State

## Repository / build

- Product / stable checkpoint: Guiyuan 0.2.1.
- Stable `main`: Guiyuan 0.2.1 / Build 746 (`20261006-746`), promoted through PR #226 after the dev-to-main Full validation boundary passed.
- Integration `dev`: Guiyuan 0.2.1 / Build 746 plus merged maintainability PRs #228–#238. Runtime behavior and Build identity remain unchanged from the promoted checkpoint.
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

The active work is `refactor/maintainability-sysui-helpers`, a behavior-neutral cleanup of the small SystemUI integration helpers around host capture, native carrier geometry, island observation, and Keyguard host resolution.

This batch fixes names where responsibility or scope was misleading: the host hook is not a runtime owner, the carrier helper is shared across Home / Keyguard / QS fake rather than Home-only, and the remaining helper types use the established `SysUi` form. It also replaces stale or overly formal source comments with short maintenance notes that explain real ownership, lifecycle, and platform constraints.

Runtime strings, persisted keys, Hook IDs, reflection/resource targets, diagnostics schema, source ownership, mutable writers, and behavior remain unchanged. Build identity stays at 746; no device evidence is required while the diff remains behavior-neutral.

Current priorities:

1. complete the helper rename/comment pass with full base→HEAD review and automated validation;
2. keep comments concise, natural, and useful to the next maintainer rather than documenting obvious code or old Build history;
3. preserve real `Owner` / `Source` / `Probe` / `Resolver` responsibility terms where they still carry meaning;
4. keep the closed performance line closed unless new reproducible evidence appears.

## Non-negotiable bounds

- Root-cause first; prefer verified native/upstream contracts.
- Keep one owner/writer per mutable surface.
- Do not add polling, delay, retry loops, custom gesture clocks, guessed thresholds or fixed device geometry.
- Do not take over native translation, alpha, visibility or final-QS appearance ownership.
- Do not weaken fail-native or restoration boundaries to gain smoothness.
- Do not trade accepted Home, Keyguard/AOD, charging-island or Control Center geometry for speculative performance work.
- A branch name alone does not make a route active; require code/PR/CI/device evidence that agrees with the current objective.

## Immediate next

- Treat Build 744 as the accepted runtime baseline; Build 746 is the promoted 0.2.1 version/release checkpoint.
- Finish `refactor/maintainability-sysui-helpers` with stale-name/comment review and automated validation before integration.
- Do not request Canary/device testing unless a later edit crosses into runtime behavior.
- After integration, close the active branch reference in CURRENT before starting another maintenance batch.
- External version remains 0.2.1 until the maintainer explicitly authorizes another display-version bump.
