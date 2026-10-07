# Current Development State

## Repository / build

- Product / stable checkpoint: Guiyuan 0.2.1.
- Stable `main`: Guiyuan 0.2.1 / Build 746 (`20261006-746`) at `c6748e6a`; default-branch Canary now accepts trusted `refactor/*` PRs through #259.
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

Draft PR #260 (`refactor/repo-maintainability-sweep` -> `dev`) extends the earlier Xposed audit to the companion UI, settings, system/diagnostics, unit tests, tooling and build scripts. The 44 non-Xposed Kotlin main sources and all 63 unit-test files have been screened, flagged areas have had targeted semantic review, and implementation is now frozen pending final Full CI plus a maintainer-requested Canary spot check. Runtime/SystemUI behavior remains unchanged.

## Non-negotiable bounds

- Root-cause first; prefer verified native/upstream contracts.
- Keep one owner/writer per mutable surface.
- Do not add polling, delay, retry loops, custom gesture clocks, guessed thresholds or fixed device geometry.
- Do not take over native translation, alpha, visibility or final-QS appearance ownership.
- Do not weaken fail-native or restoration boundaries.
- Do not trade accepted Home, Keyguard/AOD, charging-island or Control Center geometry for cleanup-only work.
- Keep version 0.2.1 / Build 746 unchanged unless explicitly requested.

## Immediate next

- Keep PR #260 implementation frozen.
- Run final Full CI on the complete base-to-HEAD diff.
- Build one exact-head Work Branch Canary for maintainer spot testing.
- If validation and the spot check remain clean, record the durable result in DEVLOG and merge #260 to `dev`.
