# Current Development State

## Repository / build

- Product: Guiyuan 0.2.0.
- Stable `main`: Guiyuan 0.2.0 / Build 742 (`20261006-742`).
- Integration `dev`: Guiyuan 0.2.0 / Build 744 (`20261006-744`). PR #221 is merged after exact-head Runtime CI #2778, Work-branch Canary #788, focused device validation, and integrated `dev` Runtime CI #2779.
- Build 743 bounded only the QS_FAKE native peer reservation to verified live carrier capacity, fixing the rare Home pull-down -> swipe-up/collapse native fallback without changing Guiyuan semantic transition geometry.
- Build 744 scopes the Home-derived steady-peer mirror to Home-origin Control Center cycles. Keyguard/UNKNOWN sources keep native QS_FAKE island authority instead of consuming background Home peer state.
- Repository consistency audit #222 is merged into `dev`. Its pre-release-grade review standard applies while Guiyuan remains in active development; this is not a release-candidate freeze or 1.0.0 qualification.
- Exact-target compatibility claims must track the real runtime hook surface. Static profile entries are added only where the pinned SystemUI artifact has matching reference evidence; runtime-only seams must not be mislabeled as statically verified.
- Parallel runtime work: PR #224 / `fix/control-center-alpha-layer-bounds`, Build 745 / `20261006-745`, is a separate compositing-cost candidate based on bounded `saveLayerAlpha` work. It is not yet accepted and remains behind its own Runtime CI, Canary, and device gate.
- Historical PR #197 is closed as superseded and must not be restored as an active implementation route.

## Accepted runtime baseline

- Home remains an independent presentation owner.
- Keyguard and AOD share one host-scoped family renderer/presentation owner and retarget scene semantics without duplicate mutable owners.
- AOD is not a Control Center transition source.
- QS_FAKE is the bounded Control Center bridge; the fully expanded Control Center remains native.
- HyperOS remains authoritative for native scene, island, appearance, motion, alpha, visibility and translation timing.
- One transition-reservation writer remains. Fake-carrier capacity is a bounded visible-cycle lease and fail-native restores only the affected surface.
- The Home steady-peer mirror is Home-source data. Build 744 rejects it for Keyguard/UNKNOWN Control Center sources and restores it only when Home becomes authoritative again.
- Build 689-693 removed or bounded the previously identified diagnostic/reflection hot-path costs. Detailed diagnostics remain observational and must not change functional behavior.

## Active objective

No current device evidence keeps the former **Keyguard + active island + repeated full Control Center pull-down / swipe-up** gap open as a blocker after the Build 744 ownership correction passed focused validation and was integrated into `dev`.

Current work has two independent active lines while normal development continues:

1. PR #223 performs the pre-release-grade repository / compatibility-contract consistency audit without changing runtime behavior or Build identity;
2. PR #224 evaluates a bounded TransitionDrawable/compositing optimization as Build 745, with its own runtime/device acceptance gate;
3. keep CURRENT / ROADMAP / public repository facts synchronized with merged code, current PRs, CI and device evidence;
4. preserve established internal `CombinedStatus*` preference, Hook, diagnostic and compatibility identities unless a concrete migration benefit justifies changing them;
5. do not infer a reopened ownership defect merely because a separate performance candidate exists.

## Non-negotiable bounds

- Root-cause first; prefer verified native/upstream contracts.
- Keep one owner/writer per mutable surface.
- Do not add polling, delay, retry loops, custom gesture clocks, guessed thresholds or fixed device geometry.
- Do not take over native translation, alpha, visibility or final-QS appearance ownership.
- Do not weaken fail-native or restoration boundaries to gain smoothness.
- Do not trade accepted Home, Keyguard/AOD, charging-island or Control Center geometry for benchmark-only optimization.
- A branch name alone does not make a route active; require code/PR/CI/device evidence that agrees with the current objective.

## Immediate next

- Complete PR #223 exact-target contract coverage and repository-selected CI without changing runtime or Build identity.
- Keep PR #224 separate: accept Build 745 only after its Runtime CI, signed Canary, and focused clipping/alpha/performance device gate; otherwise retain Build 744 as the runtime baseline.
- Continue normal development from the live `dev` state after each line is independently disposed.
- External version remains 0.2.0. Promotion to `main` remains an explicit maintainer decision.
