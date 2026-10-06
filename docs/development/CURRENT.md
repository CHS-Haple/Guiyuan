# Current Development State

## Repository / build

- Product: Guiyuan 0.2.0.
- Stable `main`: Guiyuan 0.2.0 / Build 742 (`20261006-742`).
- Integration `dev`: Guiyuan 0.2.0 / Build 743 (`20261006-743`). Resolve the live `dev` ref from GitHub at startup rather than persisting a branch SHA here.
- PR #220 is merged into `dev`. Exact-head Runtime CI #2770, Work-branch Canary #784, focused device validation, and integrated `dev` Runtime CI #2771 all passed.
- Build 743 fixes the rare Home pull-down -> swipe-up/collapse native fallback by bounding only the QS_FAKE native peer reservation to verified live carrier capacity. Guiyuan semantic transition width, targets, progress, motion and renderer geometry remain unchanged.
- Historical PR #197 is closed as superseded and must not be restored as an active implementation route.
- Parallel repository maintenance: `fix/pre-release-consistency-audit` performs a pre-release-grade consistency audit while Guiyuan remains in active development. It is documentation/repository-only, does not change Build 743 runtime or version identity, and must not be interpreted as a release-candidate freeze or 1.0.0 qualification.

## Accepted runtime baseline

- Home remains an independent presentation owner.
- Keyguard and AOD share one host-scoped family renderer/presentation owner and retarget scene semantics without duplicate mutable owners.
- AOD is not a Control Center transition source.
- QS_FAKE is the bounded Control Center bridge; the fully expanded Control Center remains native.
- HyperOS remains authoritative for native scene, island, appearance, motion, alpha, visibility and translation timing.
- One transition-reservation writer remains. Fake-carrier capacity is a bounded session lease and fail-native restores only the affected surface.
- Build 689-693 removed or bounded the previously identified diagnostic/reflection hot-path costs. Remaining Keyguard-island smoothness work must not assume diagnostics are still the primary cause without new evidence.

## Active objective

The remaining device-visible performance gap is:

**Keyguard + active island + repeated full Control Center pull-down / swipe-up remains slightly less smooth than the equivalent Home path.**

Treat this as a residual ownership/draw/compositing audit, not as permission to change native animation timing.

Current investigation priorities:

1. verify whether the Home-derived steady-peer mirror is a valid authority during a Keyguard-origin Control Center cycle;
2. compare Keyguard-origin and Home-origin native-layout / mirror / clip work;
3. audit TransitionDrawable per-component alpha layers, residual allocations and repeated draw passes;
4. change only behavior that can be proven redundant or semantically equivalent.

## Non-negotiable bounds

- Root-cause first; prefer verified native/upstream contracts.
- Keep one owner/writer per mutable surface.
- Do not add polling, delay, retry loops, custom gesture clocks, guessed thresholds or fixed device geometry.
- Do not take over native translation, alpha, visibility or final-QS appearance ownership.
- Do not weaken fail-native or restoration boundaries to gain smoothness.
- Do not trade accepted Home, Keyguard/AOD, charging-island or Control Center geometry for a benchmark-only optimization.
- Keep Detailed diagnostics observational; diagnostic level must not change functional behavior.

## Immediate next

- Reconstruct the original steady-peer-mirror requirement from DEVLOG and exact-target evidence.
- If Home mirror authority is Home-origin only, source-scope it so a Keyguard-origin cycle does not scan or propagate background Home island-peer state.
- If that path is required, retain it and move to residual TransitionDrawable/compositing work.
- Before any runtime commit, review the complete `dev...HEAD` diff and add focused tests for the exact ownership rule being changed.
- Advance the internal Build only when a coherent runtime checkpoint is ready; external version remains 0.2.0.
- Request device validation only after static/automated evidence has narrowed the change to a meaningful runtime checkpoint.
