# Current Development State

## Repository / build

- Product / stable checkpoint: Guiyuan 0.2.1.
- Stable `main`: Guiyuan 0.2.1 / Build 746 (`20261006-746`) at `9ee18c3`.
- Integration `dev`: PR #262 is merged at runtime checkpoint `e077d08`; version remains Guiyuan 0.2.1 / Build 746.
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

PR #260 completed the repository-wide maintainability sweep beyond Xposed/runtime plumbing. All 44 non-Xposed Kotlin main sources and 63 unit-test files were screened, flagged areas received targeted semantic review, and the accepted cleanup is integrated on `dev`.

PR #262 completed the residual maintainability pass with the same behavior-neutral boundary. It simplified diagnostic state/logging, removed string-driven retry policy and proof-only helpers, shortened remaining ceremonial naming, clarified independent lifecycle facts, and added the mandatory pre-commit maintainability/naturalness review. Runtime CI #2955 passed after the final compile-boundary correction; no device-only evidence is currently required.

## Non-negotiable bounds

- Root-cause first; prefer verified native/upstream contracts.
- Keep one owner/writer per mutable surface.
- Do not add polling, delay, retry loops, custom gesture clocks, guessed thresholds or fixed device geometry.
- Do not take over native translation, alpha, visibility or final-QS appearance ownership.
- Do not weaken fail-native or restoration boundaries.
- Do not trade accepted Home, Keyguard/AOD, charging-island or Control Center geometry for cleanup-only work.
- Keep version 0.2.1 / Build 746 unchanged unless explicitly requested.

## Immediate next

- Treat the current maintainability pass as integrated on `dev`; do not reopen cleanup without a concrete maintainability finding.
- Keep `main` at the existing stable checkpoint until a separate promotion is explicitly requested.
- Keep version 0.2.1 / Build 746 unchanged. Request a device Canary only when future changes cross a runtime behavior boundary that automated review cannot settle.
