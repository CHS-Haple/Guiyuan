# Current Development State

## Repository / build

- Product / stable checkpoint: Guiyuan 0.2.1.
- Stable `main`: Guiyuan 0.2.1 / Build 746 (`20261006-746`), promoted through PR #226 after the dev-to-main Full validation boundary passed.
- Integration `dev`: Guiyuan 0.2.1 / Build 746 plus merged maintainability PRs #228–#246. Runtime behavior and Build identity remain unchanged from the promoted checkpoint.
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

The behavior-neutral maintainability review is complete through PR #246. The remaining large runtime files are ownership/lifecycle-dense, tooling-bound, or have no clear tested stateless boundary; further cosmetic splitting/renaming would add churn without a clear maintenance gain.

The current objective is stable promotion of the existing Guiyuan 0.2.1 / Build 746 source from `dev` to `main`. Runtime behavior, Build identity, persisted keys, hooks, diagnostics protocol, native ownership, and fail-native boundaries are unchanged from the accepted checkpoint. No new device gate is required unless promotion review or Full validation finds evidence of a runtime-affecting change.

Current priorities:

1. review the complete `main` → `dev` promotion diff for accidental compatibility/runtime drift;
2. run the required dev-to-main Full validation on the exact promotion head;
3. merge only if Full is green and no runtime blocker appears;
4. after merge, verify the long-lived `dev` branch still exists and matches promoted `main`.

## Non-negotiable bounds

- Root-cause first; prefer verified native/upstream contracts.
- Keep one owner/writer per mutable surface.
- Do not add polling, delay, retry loops, custom gesture clocks, guessed thresholds or fixed device geometry.
- Do not take over native translation, alpha, visibility or final-QS appearance ownership.
- Do not weaken fail-native or restoration boundaries to gain smoothness.
- Do not trade accepted Home, Keyguard/AOD, charging-island or Control Center geometry for speculative performance work.
- A branch name alone does not make a route active; require code/PR/CI/device evidence that agrees with the current objective.

## Immediate next

- Treat Build 744 as the accepted runtime baseline; Build 746 remains the promoted 0.2.1 build identity.
- Open the dev-to-main stable-promotion PR from the current `dev` tree and require Full validation.
- Do not request Canary/device testing unless review or Full uncovers a plausible runtime-affecting delta.
- After promotion, verify/recreate `dev` at the promoted `main` SHA before new development.
- External version remains 0.2.1 until the maintainer explicitly authorizes another display-version bump.
