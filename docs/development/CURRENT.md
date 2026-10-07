# Current Development State

## Repository / build

- Product / stable checkpoint: Guiyuan 0.2.1.
- Stable `main`: Guiyuan 0.2.1 / Build 746 (`20261006-746`).
- Integration `dev`: Guiyuan 0.2.1 / Build 746 at `af976eb1`, with PR #257 merged after exact-head Runtime CI #2925.
- PR #224 / Build 745 remains a rejected Control Center alpha-layer experiment and must not be restored without new exact-target evidence.
- Historical PR #197 remains superseded.

## Accepted runtime baseline

- Home remains an independent presentation owner.
- Keyguard and AOD share one host-scoped family renderer/presentation owner and retarget scene semantics without duplicate mutable owners.
- AOD is not a Control Center transition source.
- QS_FAKE is the bounded Control Center bridge; the fully expanded Control Center remains native.
- HyperOS remains authoritative for native scene, island, appearance, motion, alpha, visibility and translation timing.
- One transition-reservation writer remains. Fake-carrier capacity is a bounded visible-cycle lease and fail-native restores only the affected surface.
- Detailed diagnostics are observational and must not change functional behavior.

## Current maintainability state

PR #257 completed the first residual runtime-plumbing cleanup across the Xposed/SystemUI integration layer.

Accepted cleanup boundaries:

- remove redundant product/domain wording when the package or receiver already carries it;
- keep `Owner` / `Source` / `Policy` / `Probe` / `Session` only when they carry real lifecycle, authority, compatibility, reuse or policy value;
- do not stack duplicate Result/State wrappers around the same outcome;
- model one mutually-exclusive lifecycle as one explicit state, but keep genuinely independent readiness facts independent;
- diagnostics must report observed runtime facts, not hard-coded proof fields or symmetric success events;
- inline single-caller helper types when the separate abstraction does not earn a boundary.

The next maintainability pass expands this audit beyond the Xposed package to the rest of the repository. Runtime behavior remains frozen unless a concrete defect is found.

## Non-negotiable bounds

- Root-cause first; prefer verified native/upstream contracts.
- Keep one owner/writer per mutable surface.
- Do not add polling, delay, retry loops, custom gesture clocks, guessed thresholds or fixed device geometry.
- Do not take over native translation, alpha, visibility or final-QS appearance ownership.
- Do not weaken fail-native or restoration boundaries.
- Do not trade accepted Home, Keyguard/AOD, charging-island or Control Center geometry for cleanup-only work.
- Keep version 0.2.1 / Build 746 unchanged unless explicitly requested.

## Immediate next

- Audit the rest of the repository for the same maintainability problems already addressed in Xposed/runtime plumbing.
- Prioritize app UI/settings, diagnostics/system code, tests, build tooling and CI scripts.
- Change only deterministic issues with a clear maintenance benefit; do not create churn for uniformity.
- Use one coherent work branch/PR and run one suitable validation checkpoint after complete base-to-HEAD review.
