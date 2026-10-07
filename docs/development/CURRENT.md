# Current Development State

## Baseline

- Stable product line: Guiyuan 0.2.1 / Build 746 (`20261006-746`).
- Stable `main`: support/documentation checkpoint based on the accepted 0.2.1 runtime.
- Integration runtime checkpoint: `d6cd1d85`, after the second repository-wide maintainability audit.
- Later documentation-only commits do not change that runtime checkpoint.
- No runtime blocker is currently recorded.

## Accepted runtime contract

- Home has its own projected presentation owner.
- Keyguard and AOD share one host-scoped family presentation owner; same-host scene changes retarget that owner instead of creating duplicate mutable owners.
- AOD is never a Control Center transition source.
- Notification Shade and the fully expanded Control Center remain native.
- QS_FAKE is the bounded Control Center transition bridge.
- SystemUI owns native scene state, layout, appearance, alpha, visibility, translation and motion timing.
- Guiyuan owns only its renderer plus the minimum verified suppression, masking, reservation and transition-projection state.
- Any ownership, host, geometry or compatibility ambiguity fails native for the smallest affected surface.
- Diagnostic/build-channel flags are observational only and must not alter functional runtime control flow.

## Maintainability baseline

Repository-wide maintainability review is considered complete unless a concrete new finding appears.

Current rules:
- prefer concise, scope-appropriate names over modifier/suffix stacking;
- keep abstractions only when they carry real ownership, lifecycle, compatibility, reuse or policy value;
- model one mutually exclusive lifecycle as one state rather than a wall of invalid boolean combinations;
- keep genuinely independent facts independent;
- diagnostics report observed facts rather than invented proof fields, pass rates or symmetry-only events;
- preserve compatibility identities when they are externally consumed.

## Guardrails

- Root cause before workaround.
- One writer per mutable surface.
- No polling, delay/retry repair loops, guessed thresholds or fixed device geometry.
- Do not take over native translation, alpha, visibility or final Control Center appearance.
- Do not weaken exact restoration or fail-native behavior.
- Keep version 0.2.1 / Build 746 unchanged unless a version/build change is explicitly part of the task.

## Next

Choose the next task from [ROADMAP.md](ROADMAP.md) or a concrete reported defect/feature request. Request a Canary or device check only when the change crosses a runtime boundary that automated review cannot settle.
