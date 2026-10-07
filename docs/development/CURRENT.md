# Current Development State

## Repository / build

- Product / stable checkpoint: Guiyuan 0.2.1.
- Stable `main`: Guiyuan 0.2.1 / Build 746 (`20261006-746`), with the behavior-neutral maintainability cleanup promoted through PR #247 after exact-head Full validation.
- Integration `dev`: Guiyuan 0.2.1 / Build 746 at `9799dc50`; runtime behavior remains unchanged, with #254 adding a validated release-workflow safety gate. `main` is still at the PR #247 stable checkpoint.
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

Phase 5 / 1.0.0 qualification is active.

The first release-safety blocker is closed. PR #254 adds a stable-only preflight that rejects both an existing GitHub Release and an existing exact remote `v$VERSION_NAME` tag before publishing. This prevents `gh release create` from silently reusing a stale/manual tag instead of creating the version tag from the prepared `main` commit.

Validation:

- exact-head Full CI #2908 passed on `adae300e`;
- PR #254 squash-merged to `dev` as `9799dc50`;
- integrated `dev` Full CI #2909 passed target-profile verification, tests, Debug/Canary builds, Modern Xposed metadata, Haple signature, non-debuggable verification and artifact upload.

The broader Phase 5 source audit has also confirmed that README/PRIVACY/SECURITY/THIRD_PARTY_NOTICES, manifest network policy, Root entry points, direct dependency versions, Gradle Wrapper identity, CODEOWNERS/Dependabot and signing-file ignore rules materially match the current implementation. Private Vulnerability Reporting account state is still not verified because the available repository connector does not expose that setting.

The next release-boundary question is whether the stable channel should explicitly reject pre-1.0 version names. Project documentation consistently treats 1.0.0 as the first planned formal release, while the current stable workflow otherwise accepts any prepared `main` version with a dated changelog section. Review that boundary separately rather than folding it into #254.

Current priorities:

1. verify the formal-release version boundary before changing it;
2. continue Phase 5 only from concrete release/compatibility/security evidence;
3. keep stale historical branches classified as cleanup candidates, but do not restore their old implementation routes;
4. keep `main` unchanged until an explicit dev-to-main promotion is chosen.

## Non-negotiable bounds

- Root-cause first; prefer verified native/upstream contracts.
- Keep one owner/writer per mutable surface.
- Do not add polling, delay, retry loops, custom gesture clocks, guessed thresholds or fixed device geometry.
- Do not take over native translation, alpha, visibility or final-QS appearance ownership.
- Do not weaken fail-native or restoration boundaries to gain smoothness.
- Do not trade accepted Home, Keyguard/AOD, charging-island or Control Center geometry for speculative performance work.
- A branch name alone does not make a route active; require code/PR/CI/device evidence that agrees with the current objective.

## Immediate next

- Review whether stable GitHub releases must require version 1.0.0 or later, consistent with the current first-formal-release policy.
- Keep Build 744 as the accepted runtime-behavior baseline; Build 746 remains the current 0.2.1 build identity.
- Use `dev@9799dc50` as the current integration base.
- Do not add release/privacy/security documents merely to fill a checklist; verify existing claims against source and workflow behavior.
- External version remains 0.2.1 until the maintainer explicitly authorizes another display-version bump.
