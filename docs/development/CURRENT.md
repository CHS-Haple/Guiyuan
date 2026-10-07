# Current Development State

## Repository / build

- Product / stable checkpoint: Guiyuan 0.2.1.
- Stable `main`: Guiyuan 0.2.1 / Build 746 (`20261006-746`), with the behavior-neutral maintainability cleanup promoted through PR #247 after exact-head Full validation.
- Integration `dev`: Guiyuan 0.2.1 / Build 746 at `fa2af0a7`; runtime code remains the validated `06635a7e` state, with #253 adding documentation-only closeout. `main` is still at the PR #247 stable checkpoint.
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

Phase 5 release qualification is active on `ci/stable-tag-boundary`, based on current `dev@fa2af0a7`.

The first release-safety audit found one concrete fail-closed gap in `.github/workflows/release.yml`: stable publishing rejects an existing GitHub Release for `v$VERSION_NAME`, but did not reject a pre-existing bare remote tag with the same name. GitHub CLI only creates the tag from `--target` when the matching tag does not already exist, so an old/manual bare tag could otherwise be reused by `gh release create`.

This branch adds one stable-only preflight: fail when either the Release already exists or the exact remote version tag already exists. Normal test releases are unchanged. No APK/runtime code, version, Build, dependency, signing certificate, changelog content, or device behavior changes.

The broader Phase 5 audit has also confirmed that README/PRIVACY/SECURITY/THIRD_PARTY_NOTICES and direct dependency versions are present and materially aligned with the current project. The manifest does not declare `INTERNET`; the checked-in Wrapper is Gradle 9.8.0 as documented. Private Vulnerability Reporting account state could not be read through the available repository connector, so it is not treated as verified evidence here.

Current priorities:

1. run Full validation for the release-workflow change;
2. merge only if repository/tooling validation stays green;
3. continue Phase 5 with concrete release/compatibility gaps rather than adding ceremonial documentation;
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

- Validate the stable-tag fail-closed gate on `ci/stable-tag-boundary`.
- Keep Build 744 as the accepted runtime-behavior baseline; Build 746 remains the current 0.2.1 build identity.
- Treat `dev@fa2af0a7` as the current integration head and `06635a7e` as the latest runtime-affecting validated state beneath its documentation-only closeout.
- Continue Phase 5 by verifying existing release/privacy/security/notice claims against source and workflow behavior; do not add documents merely to fill a checklist.
- External version remains 0.2.1 until the maintainer explicitly authorizes another display-version bump.
