# Current Development State

## Repository / build

- Product / stable checkpoint: Guiyuan 0.2.1.
- Stable `main`: Guiyuan 0.2.1 / Build 746 (`20261006-746`), with the behavior-neutral maintainability cleanup promoted through PR #247 after exact-head Full validation.
- Integration `dev`: Guiyuan 0.2.1 / Build 746 at `fa0e32c5`, with #248-#251 maintenance and documentation closeout merged. Runtime behavior and Build identity remain unchanged; `main` is still at the PR #247 stable checkpoint.
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

A bounded residual runtime-plumbing cleanup is active on `refactor/runtime-plumbing-residuals`, based on accepted `dev@fa0e32c5`.

The pass targets only concrete maintainability residue left after #248/#250 rather than reopening a broad cleanup. Current branch changes:

- remove remaining synthetic/self-proof handoff write reporting and an unreachable battery restore failure branch;
- keep Battery/Network suppression results state-only while retaining real detailed observations at the operation site;
- fold two tiny Battery geometry Policy files into one cohesive geometry helper;
- remove the generic `PresentationPolicy` shell and keep stable-model/tint rules with their actual owners/domain type;
- replace the mutually exclusive Home native-AOD fallback booleans with one explicit `NONE / CANDIDATE / ACTIVE` phase;
- shorten internal names where scope already carries the missing context, including native suppressors/participant runtime, active-subscription source, native-status inventory, Wi-Fi optical reference and `SysUi` compatibility/parser types;
- rename the private ignored-slot lifetime enum to the shorter `IgnoreScope.CALL / SESSION`;
- add a CONTRIBUTING rule forbidding success/failure contracts when no real failure source exists.

Review deliberately keeps independent Home-AOD prearm/origin facts and Keyguard readiness/lease facts as separate booleans. It also keeps larger transition, slot-reservation, runtime-access and install-result types where they carry real domain, lifecycle, partial-readiness or fail-native contracts.

Pre-PR source review caught and fixed one malformed Boolean-to-enum replacement and restored an accidental file-mode change before validation. No display-version, Build, dependency, Hook-count contract, geometry constant, transition clock or native writer change is intended.

Current priorities:

1. finish exact-head Runtime validation for the current branch;
2. merge to `dev` only if target-profile checks, Kotlin compilation, unit tests and APK/metadata checks are green;
3. require device testing only if validation or review leaves a real device-only runtime question;
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
- Use accepted `dev@fa0e32c5` as the integration base until this residual-plumbing PR is accepted.
- Do not expand this pass into a broad rename or abstraction purge; the remaining longer names and independent booleans were reviewed and retained where they carry real meaning.
- Keep comments concise and natural; explain lifecycle or platform constraints, not obvious code.
- External version remains 0.2.1 until the maintainer explicitly authorizes another display-version bump.
