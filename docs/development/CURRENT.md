# Current Development State

## Repository / build

- Product / stable checkpoint: Guiyuan 0.2.1.
- Stable `main`: Guiyuan 0.2.1 / Build 746 (`20261006-746`), promoted through PR #226 after the dev-to-main Full validation boundary passed.
- Integration `dev`: Guiyuan 0.2.1 / Build 746 plus merged maintainability PRs #228–#237. Runtime behavior and Build identity remain unchanged from the promoted checkpoint.
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

The active work is `refactor/maintainability-runtime-names`, a behavior-neutral maintainability batch from the latest `dev`. It removes misleading `Owner` suffixes from five SystemUI runtime coordinators that install, aggregate, reset, or transfer state but do not own the final mutable presentation/session authority. The established `SysUi` abbreviation is used where the call site remains immediate; real ownership types keep `Owner`.

The same batch closes two maintenance-process gaps exposed by PRs #228–#237: adjacent cleanup with one review/validation boundary should normally stay in one coherent PR, and CURRENT must not keep a merged/deleted branch described as active.

Runtime strings, persisted keys, reflection/resource targets, Hook IDs, diagnostics schema, lifecycle, ownership, writers, and behavior remain unchanged. Build identity stays at 746; device evidence is not required unless a later edit can plausibly affect runtime behavior.

The recent Keyguard / Control Center performance line remains **closed at Build 744**. Build 745 is a rejected experiment and must not be restored without new reproducible evidence.

Current priorities:

1. complete this runtime-coordinator naming/governance batch with full base→HEAD review and automated validation before integration;
2. keep subsequent behavior-neutral cleanup coherent instead of serializing it into micro-PRs;
3. preserve persisted, reflection, resource, protocol/log-schema and Xposed compatibility identities;
4. reopen performance work only for a concrete regression, reproducible hotspot, or new root-cause evidence.

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
- Leave PR #224 closed and unmerged; Build 745 is a rejected experiment, not a fallback branch.
- Finish `refactor/maintainability-runtime-names` review and automated validation, then integrate it into `dev`; no Canary/device gate is needed while the diff stays behavior-neutral.
- Start subsequent work from the live synchronized `dev` branch and resolve current GitHub refs instead of persisting branch SHAs here.
- External version remains 0.2.1 until the maintainer explicitly authorizes another display-version bump.
