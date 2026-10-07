# Current Development State

## Repository / build

- Product / stable checkpoint: Guiyuan 0.2.1.
- Stable `main`: Guiyuan 0.2.1 / Build 746 (`20261006-746`), with the behavior-neutral maintainability cleanup promoted through PR #247 after exact-head Full validation.
- Integration `dev`: Guiyuan 0.2.1 / Build 746 at `77b2490c`; runtime behavior remains the validated `9799dc50` state, with #255 adding documentation-only closeout. `main` is still at the PR #247 stable checkpoint.
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

PR #257 (`refactor/residual-maintainability-gaps` -> `dev`) is the active work line on Guiyuan 0.2.1 / Build 746. The objective is behavior-neutral maintainability cleanup of the remaining plumbing: self-reporting diagnostics, redundant result wrappers, overlong internal naming, lifecycle state that can form invalid boolean combinations, and narrow abstractions that do not earn an independent boundary.

This work keeps SystemUI ownership, hooks, rendering, geometry, transition semantics, state authority, and fail-native behavior unchanged. It does not bump the external version or Build. Device validation is not a gate unless a later change can plausibly alter runtime behavior.

Current priorities:

1. finish the residual maintainability audit across runtime plumbing rather than only the originally cited examples;
2. keep real lifecycle/ownership contracts explicit while collapsing duplicate wrappers and ceremonial layers;
3. keep diagnostics tied to observed runtime facts and useful decision boundaries;
4. review the complete PR diff, run one suitable CI checkpoint, then merge to `dev` only if deterministic review/validation stays clean;
5. keep `main` unchanged until the maintainer explicitly chooses another stable promotion.

## Non-negotiable bounds

- Root-cause first; prefer verified native/upstream contracts.
- Keep one owner/writer per mutable surface.
- Do not add polling, delay, retry loops, custom gesture clocks, guessed thresholds or fixed device geometry.
- Do not take over native translation, alpha, visibility or final-QS appearance ownership.
- Do not weaken fail-native or restoration boundaries to gain smoothness.
- Do not trade accepted Home, Keyguard/AOD, charging-island or Control Center geometry for speculative performance work.
- A branch name alone does not make a route active; require code/PR/CI/device evidence that agrees with the current objective.

## Immediate next

- Continue PR #257 from the current `dev` base; do not revive stale maintainability branches.
- Complete the remaining naming/state/logging/abstraction audit with behavior-neutral changes only.
- Run full base-to-HEAD review before the next validation checkpoint.
- Keep Build 746 / version 0.2.1 unchanged unless the maintainer explicitly requests a bump.
