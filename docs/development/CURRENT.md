# Current Development State

## Repository / build

- Product / stable checkpoint: Guiyuan 0.2.1.
- Stable `main`: Guiyuan 0.2.1 / Build 746 (`20261006-746`).
- Integration `dev`: Guiyuan 0.2.1 / Build 746 at `6812a2b1`, with Canary workflow support for `refactor/*` merged through PR #258 after Full CI #2929. PR #257 was previously integrated after Runtime CI #2925.
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

The active work branch is `refactor/repo-maintainability-sweep` from current `dev`. It extends the earlier Xposed audit to the companion UI, settings, system/diagnostics, unit tests, tools and CI. The 44 non-Xposed Kotlin main sources and 63 unit-test files have had a first-pass structural/placeholder sweep; targeted semantic review is ongoing. Current work is limited to internal app-layer naming and clear type-safety improvements. Runtime behavior remains frozen unless a concrete defect is found.

## Non-negotiable bounds

- Root-cause first; prefer verified native/upstream contracts.
- Keep one owner/writer per mutable surface.
- Do not add polling, delay, retry loops, custom gesture clocks, guessed thresholds or fixed device geometry.
- Do not take over native translation, alpha, visibility or final-QS appearance ownership.
- Do not weaken fail-native or restoration boundaries.
- Do not trade accepted Home, Keyguard/AOD, charging-island or Control Center geometry for cleanup-only work.
- Keep version 0.2.1 / Build 746 unchanged unless explicitly requested.

## Immediate next

- Finish targeted review of flagged app/UI/diagnostics files beyond the first-pass repository sweep.
- Prioritize app UI/settings, diagnostics/system code, tests, build tooling and CI scripts.
- Change only deterministic issues with a clear maintenance benefit; do not create churn for uniformity.
- Use one coherent work branch/PR and run one suitable validation checkpoint after complete base-to-HEAD review.
