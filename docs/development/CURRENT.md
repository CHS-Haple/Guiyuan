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

Phase 5 / 1.0.0 qualification continues on `ci/formal-release-version-boundary`, based on `dev@77b2490c`.

The first release-safety gate is already integrated: #254 prevents stable publishing from reusing an existing bare version tag.

The next confirmed policy mismatch is the formal-release version boundary. README and CHANGELOG consistently define 1.0.0 as the first planned formal release, while CONTRIBUTING treats the stable channel as the formal stable-release path. The current stable workflow still accepts a prepared 0.x `main` version if its changelog section exists.

This branch keeps pre-1.0 publishing available through the existing `test` channel/prerelease path, but rejects `stable` when the version major is below 1. It does not change the current 0.2.1 version, Build identity, test-release behavior, runtime/APK code, signing certificate, or device behavior.

Current priorities:

1. run Full validation for the stable version-boundary change;
2. merge only if workflow/build validation stays green;
3. continue Phase 5 from concrete compatibility/security/release evidence;
4. keep `main` unchanged until the maintainer explicitly chooses a formal promotion.

## Non-negotiable bounds

- Root-cause first; prefer verified native/upstream contracts.
- Keep one owner/writer per mutable surface.
- Do not add polling, delay, retry loops, custom gesture clocks, guessed thresholds or fixed device geometry.
- Do not take over native translation, alpha, visibility or final-QS appearance ownership.
- Do not weaken fail-native or restoration boundaries to gain smoothness.
- Do not trade accepted Home, Keyguard/AOD, charging-island or Control Center geometry for speculative performance work.
- A branch name alone does not make a route active; require code/PR/CI/device evidence that agrees with the current objective.

## Immediate next

- Validate the stable major-version gate on `ci/formal-release-version-boundary`.
- Keep Build 744 as the accepted runtime-behavior baseline; Build 746 remains the current 0.2.1 build identity.
- Use `dev@77b2490c` as the current integration head and `9799dc50` as the latest non-documentation validated state.
- Keep pre-1.0 explicit publishing on the test/prerelease channel; do not promote 0.x through formal stable release automation.
- External version remains 0.2.1 until the maintainer explicitly authorizes another display-version bump.
