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

PRs #239–#246 are integrated and their work branches are closed. The latest passes extracted stateless presentation, transition, and native-combined decisions, consolidated pure network suppression decisions into the existing policy, shortened redundant native owner names, and renamed the multi-surface owner to `SysUiPresentationOwner`; runtime behavior and Build identity remain unchanged.

Continue the maintainability review from live `dev`, but only where a name, comment, structure, or dead indirection has a clear maintenance benefit. Keep names concise at the call site, keep real ownership terms when they describe actual authority/lifecycle, and leave short natural comments only where they explain a non-obvious contract or “why”.

Current priorities:

1. audit the remaining SystemUI/runtime layer for misleading responsibility names, receiver-redundant APIs, stale comments, and proven dead indirection;
2. prefer structural simplification over cosmetic renames when a large type is hard to maintain;
3. preserve persisted, reflection, resource, protocol/log-schema and Xposed compatibility identities;
4. keep Build identity at 746 for behavior-neutral maintenance and keep the closed performance line closed without new evidence.

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
- Audit from the live `dev` tree before choosing the next coherent maintenance batch.
- Keep comments concise and natural; add them only where they protect ownership, lifecycle, fallback, or a non-obvious platform contract.
- Request device evidence only if a later diff can plausibly affect runtime behavior.
- External version remains 0.2.1 until the maintainer explicitly authorizes another display-version bump.
