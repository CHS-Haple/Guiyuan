# Current Development State

## Repository / build

- Product / stable checkpoint: Guiyuan 0.2.1.
- Stable `main`: Guiyuan 0.2.1 / Build 746 (`20261006-746`), with the behavior-neutral maintainability cleanup promoted through PR #247 after exact-head Full validation.
- Integration `dev`: Guiyuan 0.2.1 / Build 746 at `50f5aa2`, with PR #248 runtime-plumbing cleanup and #249 documentation closeout merged. Runtime behavior and Build identity remain unchanged; `main` is still at the PR #247 stable checkpoint.
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

The maintainability coverage-gap audit is active on `refactor/maintainability-gap-audit`, based on accepted `dev@50f5aa2`. This is not another broad cleanup pass.

Coverage was rebuilt from the changed-files of #217 and #228-#248, then checked against the merged functional work between those maintenance batches. That left 15 production Kotlin files and 11 test/tooling/workflow entries that had not received equivalent maintenance coverage.

Confirmed gaps are intentionally small:

- collapse the unused `SystemActiveSubscriptionSource.Snapshot/Authority/reason` shell to the nullable active-subscription set already consumed by the resolver;
- remove the unconditional `ready` claim from the plain compatibility summary while keeping the separately observed compatibility state unchanged;
- remove one duplicate geometry test whose name claimed a battery-expansion fact that its inputs did not model.

Several tempting candidates are retained after caller-level review: `RootShell.Result` carries real process outcomes; `CenterTransitionPolicy` and the Wi-Fi optical-reference policy protect real transition/geometry contracts; render-latency and native-status inventory fields are runtime observations; navigation/resource/protocol constants retain compatibility meaning.

No ownership, geometry, transition timing, fail-native behavior, external version or Build identity is intentionally changed.

Current priorities:

1. finish exact base→HEAD review for this bounded gap batch;
2. run one exact-head Runtime CI checkpoint rather than per-edit CI;
3. merge to `dev` only if compile/tests and repository validation pass;
4. require device evidence only if automated validation exposes a runtime-only question.

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
- Use accepted `dev@50f5aa2` as the integration base; #249 is documentation-only but is still part of the current branch head.
- Do not reopen the rejected alpha-layer experiment or continue runtime-plumbing cleanup without a concrete maintenance or compatibility problem.
- Keep comments concise and natural; explain lifecycle or platform constraints, not obvious code.
- External version remains 0.2.1 until the maintainer explicitly authorizes another display-version bump.
