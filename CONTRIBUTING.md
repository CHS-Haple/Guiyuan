# Contributing to Guiyuan

This document is the engineering source of truth for Guiyuan. Use the smallest process that preserves runtime safety, traceability, and a clean stable branch.

## 1. Baseline

These rules apply to app code, SystemUI integration, compatibility, diagnostics, UI/resources, dependencies, build/release logic, CI, and engineering documentation.

English is canonical for source code, engineering documentation, contribution governance, pull requests, and repository templates. User-facing documentation may also provide Simplified Chinese.

Unless explicitly stated otherwise, contributions intentionally submitted for inclusion in Guiyuan are licensed under `GPL-3.0-only`, matching the project license.

Local baseline: JDK 21, Android SDK 37 / Build Tools 37.0.0, Modern Xposed API 102, the checked-in Gradle Wrapper, and the MIUIX revision pinned by project build files.

For an ordinary code checkpoint:

~~~bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
~~~

Keep credentials, signing material, local SDK paths, generated APK/AAB files, and machine-specific configuration out of the repository.

## 2. Engineering principles

### Standardized
Prefer Android, HyperOS, MIUIX, and Modern Xposed contracts over project-local reinvention. Reuse verified HyperOS state, behavior, resources, and motion when they already express the required semantics. Prefer runtime resource references over copying proprietary assets.

### Lightweight
Avoid duplicate hooks/listeners/state machines, polling, repeated View-tree traversal, hot-path reflection, resident Root work, per-frame diagnostics, and unnecessary caching.

### Modern
Prefer maintained APIs and project-pinned dependencies when they satisfy the requirement. Newer is not automatically better; compatibility and lifecycle evidence still matter.

### Fail native
If Guiyuan cannot safely establish its replacement contract, preserve or restore native HyperOS behavior. Compatibility failure should disable the smallest affected feature.

## 3. Change method

Before editing:
1. confirm the actual requirement or defect;
2. identify the relevant owner, state source, lifecycle, API contract, or layout rule;
3. define the smallest useful change boundary;
4. identify behavior that must remain unchanged;
5. decide what evidence is needed to accept the result.

For defects, investigate in this order:
1. root cause;
2. responsible source/owner;
3. project and authoritative upstream guidance;
4. established implementation patterns when needed;
5. a narrow workaround only when a direct fix is not practical.

Do not add geometry, timing, alpha, visibility, polling, or delayed patches merely because they hide one symptom. When several explanations remain plausible, prefer a bounded single-variable diagnostic. New evidence may invalidate the current hypothesis.

## 4. Runtime safety

### Ownership and lifecycle
Every long-lived runtime object needs one clear owner. For runtime-sensitive work, know who creates/owns it, when it becomes invalid, who cleans it up, and what happens on host replacement, SystemUI recreation, and Hot Reload.

Prefer host-scoped sessions over unrelated global mutable state.

### State flow and writers
Prefer:

~~~text
native/event source -> domain state -> scene/presentation policy -> renderer
~~~

Use the highest verified native semantic source available. Do not maintain a parallel parser/state machine for a fact SystemUI already exposes authoritatively unless a verified compatibility boundary requires it.

Observation is not ownership. One live mutable property should have one runtime writer. Width, position, translation, alpha, visibility, tint, animation state, and attachment are ownership-sensitive.

Hooks are integration points, not architecture. Each hook should have one responsibility, an owner, failure behavior, and a bounded lifecycle.

### Native resource integration
When reusing a verified HyperOS/SystemUI drawable, preserve its resource identity, authored alpha/coverage, viewport relationships, and final native/vector rendering semantics. Do not add per-resource grayscale multipliers, hard-coded replacement grays, source-alpha edits, raster preprocessing/resampling, or other magic-number compensation merely to force a visual match unless exact-target evidence proves that transformation belongs to the native path.

Do not hide a native representation until the Guiyuan replacement is valid for the current session.

### Geometry and motion
Keep separate:
1. native SystemUI layout/slot ownership;
2. Guiyuan drawing geometry;
3. transition/animation geometry;
4. optical adjustment.

A visual sizing issue does not automatically justify native layout mutation. A transition issue does not automatically justify changing steady geometry. Prefer verified native progress/endpoints/motion ownership; do not create a second gesture timeline merely to imitate HyperOS.

### Compatibility and fallback
The pinned SystemUI profile is evidence for the current target, not proof for every HyperOS build. Live runtime topology is authoritative when other modules or system variants can alter the View tree. Failure should restore the smallest affected surface to native.

## 5. Maintainability

Optimize for the next human maintainer. Prefer the shortest name or structure that stays obvious in its actual scope; do not optimize for maximum verbosity or for clever brevity.

### Naming
- Public identity remains Guiyuan / 归元 with package `com.chaners.guiyuan`; internal shortening must not change public or compatibility identity.
- Name to the scope. Local variables and private members may be short when nearby context already carries the meaning; cross-file and architecture-boundary names must retain enough context to stay searchable and unambiguous.
- When the original meaning stays obvious, shorten aggressively with familiar project/platform forms such as `SysUi`, `Diag`, `Env`, `Repo`, `Prefs`, `Ctx`, `Cfg`, `AOD`, `QS`, and `CC`. Use natural Kotlin casing such as `SysUi` for a type segment and `sysUi` for a value.
- Remove repeated product or domain wording when the package, file, receiver, or owner already establishes it. Use `Gy` only where Guiyuan identity is actually useful.
- Keep words such as `Owner`, `Source`, `Policy`, `Session`, `Contract`, and `Probe` only when they carry real responsibility, lifecycle, authority, or compatibility meaning. Do not keep them merely to make a name sound formal.
- File names should normally match the primary type. Cohesive helper files may keep a broader domain name when splitting them would make navigation worse.
- Persisted preference keys, protocol/event names, log schema fields, reflection/class/member targets, resource identities, Xposed-facing identifiers, and other externally consumed names are compatibility surfaces. Do not rename them as cleanup without an explicit migration reason.
- Do not keep obsolete aliases solely to preserve old internal names. Retain an alias only when a real compatibility or migration boundary requires it.
- Do not use broad search/replace as the renaming method. Inspect each symbol, its call sites, and same-text uses first; edit the intended references explicitly, then search again for stale old names and accidental changes.
- Remove dead helpers only after confirming they have no normal references and are not reached through reflection, serialization, resources, generated code, or another external contract.

### Comments
- Add a comment only when it helps a future maintainer understand something the code cannot express cleanly.
- Prefer short, natural comments that explain **why**, an invariant, ownership, lifecycle, fallback behavior, or a non-obvious Android/HyperOS/Xposed limitation.
- Do not narrate the next line, repeat names/types, document obvious control flow, or leave Build-by-Build debugging history in source comments.
- Do not stamp the same comment template across similar files. Different code may need different explanation, and many locations need no comment at all.
- Keep the comment beside the invariant it protects. Update or remove it when that invariant changes so comments do not become a second, stale implementation.

### Refactor discipline
- Maintainability refactors are behavior-neutral by default. A behavior change should be isolated and reviewed as a behavior change rather than hidden inside cleanup.
- Work in coherent, reviewable batches: large enough to keep related type/file/test changes together, but not so large that a reviewer cannot reason about the base→HEAD diff.
- Before committing a non-trivial batch, review the complete base→HEAD diff for accidental compatibility-string changes, incomplete renames, mismatched source/test filenames, semantic drift, lifecycle/ownership changes, and unnecessary churn.
- Prefer deleting proven dead code and redundant indirection over renaming it.
- Do not run CI for every micro-edit. Validate at meaningful checkpoints; request device evidence only when the refactor can plausibly change runtime behavior.

## 6. Diagnostics, UI, and text

Diagnostics should be event-driven and bounded:

~~~text
event -> bounded snapshot -> report
~~~

Canary/Release may retain low-frequency operational diagnostics. Detailed geometry/topology probes must stay behind development/Detailed diagnostics and out of hot paths.

Build-channel diagnostic flags such as `RUNTIME_DIAGNOSTICS` and `DEVELOPMENT_PROBES` are **observation gates only**. They must not decide whether a functional hook/state source is installed, which runtime state is authoritative, who owns a surface/property, when ownership is acquired/released, or which fail-native fallback applies. Canary and Release must share the same functional control flow; only logging, bounded probes, diagnostic preferences, and optional diagnostic event callbacks may differ. If a diagnostic source later becomes functional authority, move its functional installation/state path outside the diagnostic gate and leave only observation behind the flag.

For app UI, prefer current MIUIX components and conventions for spacing, typography, shape, state feedback, dialogs, navigation, back behavior, themes, and localization. Persist real user preferences only; Preview/Sandbox state must not become runtime module state.


For Material Symbols used as semantic row-leading icons:
- this policy is global across the companion app. It applies to Appearance, About, Diagnostics, settings/information rows, and any future explanatory leading icon that uses Material Symbols; do not create page-specific optical rules;
- use Material Symbols for explanatory row semantics; keep MIUIX icons for MIUIX-owned controls, navigation, menus, and action affordances;
- align Material Symbols with the visual language of the pinned MIUIX icon set. MIUIX default aliases are Regular/line-oriented and its static preference-row examples normally use non-filled icons, so Outline/Regular is the default starting style for semantic row-leading Material Symbols;
- choose the glyph from the row's semantic meaning, not from a previous icon shape, incidental text in the row value, or a desire to make neighboring silhouettes mechanically identical;
- before accepting a new Material Symbol, inspect the actual official glyph shape/rendering rather than inferring suitability from its name. Judge the silhouette, recognizable details, optical center, negative space, and how the glyph reads at the final 22 dp visual size; use repository vectors, official previews, or device screenshots as evidence;
- semantic correctness has priority over visual neatness. First select the glyph whose actual rendered shape best communicates the row meaning; only after that choice should style/weight and optical-mass normalization be applied. Do not pick a semantically weaker glyph merely because it looks more balanced beside neighboring icons;
- within one visual group or card, style coherence has priority over a single glyph's isolated local optimum. Start from the semantically correct Outline Material Symbol at the standard optical weight, then normalize perceived mass using official glyph/weight variants;
- W400 is the default starting weight. Use an official heavier weight such as W500 when the actual Outline glyph is perceptually too light because of thin, linear, ring-like, or highly open construction;
- negative space does not by itself mean a glyph is visually too light. Judge the actual occupied mass, optical center, and neighboring glyphs at the final 22 dp visual size;
- if an Outline glyph cannot reach acceptable legibility or perceived mass with a semantically correct official glyph/weight, a Filled variant may be used as a documented exception. Filled is also appropriate when fill itself communicates a real selected/active/stateful meaning, matching MIUIX's use of explicit Fill variants;
- before choosing Filled for a static semantic row, first try another semantically correct Outline glyph and/or an official heavier weight. Do not use Filled merely to make one icon look darker in isolation;
- normalize perceived visual mass with official glyph/variant/weight selection, not geometry hacks. Do not compensate with per-icon scale, translation, custom stroke, alpha, viewport edits, or ad-hoc padding;
- all such icons share the same renderer contract: a 24 dp optical box, 22 dp visual size, common alignment, and common tint. A page must not override those dimensions merely to make one glyph look larger or smaller;
- compare neighboring glyphs in both light and dark themes. Identical variant/weight values are not the goal; coherent perceived mass, optical center, and MIUIX-consistent style are the goal.


`SemanticLeadingIcon` is the current shared renderer for these Material Symbols. New semantic left-side icons should use that shared path unless a different semantic component has a stronger platform/MIUIX owner.

## 7. Git workflow

Use the lightest route that keeps the change attributable.

Branch roles:
- feat/* — one coherent capability or behavior change;
- fix/* — one bounded correction;
- refactor/* — behavior-neutral maintainability work with a coherent review boundary;
- dev — integration;
- main — accepted stable baseline;
- hotfix/* — urgent correction from main;
- dependabot/* — bot-managed dependency proposal.

There is no validation marker branch and no promotion branch in the normal workflow.

A work branch represents one coherent change boundary, not every tweak, diagnostic Build, or device response. Continue an existing branch while it still owns the same objective.

### Product/runtime path

~~~text
feat/*, fix/*, or refactor/* -> dev -> dev-to-main PR -> main
~~~

Create work branches from current dev.

A PR may stay Draft while implementation is moving. Mark it ready once it reaches the final automated-review checkpoint; do not repeatedly toggle Draft/Ready after every device response.

Merge to dev when the change is complete, deterministic blockers are resolved, required automated validation passes, and required focused device evidence has passed. If a remaining test genuinely depends on integrated dev state, note it explicitly.

Use squash merge for normal feat/fix/refactor -> dev work.

### Stable promotion
The dev-to-main PR is the promotion boundary. Do not create promote/*.

Before merge: required CI is green, the latest runtime-affecting state has applicable maintainer device acceptance, no runtime blocker remains, and CHANGELOG/public documentation reflects the resulting stable state when needed.

A documentation-only delta after an accepted runtime checkpoint does not invalidate device evidence if it cannot affect APK/runtime behavior.

Because repository-level automatic head-branch deletion may treat `dev` like an ordinary merged PR head, every dev-to-main merge must immediately verify that the long-lived `dev` branch still exists and matches the promoted `main` commit. If GitHub removed it, recreate `dev` at the promoted main SHA before any new work branch is created. Do not continue development from a stale or missing integration branch.

### Repository-only maintenance
Pure docs/metadata/governance work may use the shortest safe path. Keep shared policy/current-state documents aligned between main and dev when divergence would mislead development.

CI/build/release workflow changes require Full validation because they alter the validation mechanism. They do not require device testing unless they also alter APK/runtime behavior.

### Hotfix
Urgent stable defects may use hotfix/* from main. Keep the fix narrow, validate it appropriately, merge to main, then reconcile it into dev before the next promotion.

## 8. CI and device validation

CI has three developer-facing scopes.

### Light
Draft PRs and proven repository-only/mechanical work. Cheap repository/diff checks only.

### Runtime
Ordinary ready app/SystemUI PRs and trusted runtime integration on dev.
- Ready runtime PRs run target-profile checks, tests, Debug build, and Xposed metadata validation.
- Runtime pushes to dev validate the integrated source and produce the signed Canary integration artifact.

### Full
Main/stable boundaries and dependency, Gradle/build, CI/workflow, tooling, signing, or release changes. Full validates Debug and signed Canary where applicable.

CI proves configured source/build checks, not SystemUI runtime correctness.

For an active work-branch PR, synchronize the PR title to the exact next CI-triggering checkpoint before moving the branch ref. Use a concise one-line conventional-commit title such as `feat: refine battery color drawer layout` or `fix: center scheme preview`, keep CI-facing titles in English, and never reuse a stale title for later checkpoints. Pull-request Build and comment-triggered Work Branch Canary surface that checkpoint title as the Actions run name; push-triggered Build surfaces the head commit subject.

Pull-request validation must remain safe for untrusted forks. Secret-independent checks are allowed; signing credentials and project-signed artifacts stay on trusted maintainer/default-branch workflows and must never be exposed to contributor-controlled workflow definitions.

### Work-branch Canary
Signed Canary is demand-driven. When device evidence is needed, the repository owner may request /canary on any open same-repository feat/* or fix/* PR, including Draft. The trusted default-branch workflow resolves the exact head SHA and independently performs target-profile checks, unit tests, Canary build, metadata/signature validation, and non-debuggable verification.

A prior ready-state Runtime build is not required. Manual workflow dispatch for a trusted same-repository feat/* or fix/* branch remains the fallback.

### Device-test trigger
Request real-device testing when its result can change an engineering decision or acceptance state: first meaningful runtime checkpoint, competing hypotheses, ownership/lifecycle/scene/transition/geometry change, integrated dev validation, or a runtime-affecting stable candidate.

Do not request device testing merely because a new commit or APK exists. Group related changes when failure attribution remains clear.

## 9. Versions, dependencies, and release

Current display/build identity comes from project build configuration. ROADMAP records the first formal-release target; no separate version-status document is required.

Normal development checkpoints may advance internal build identity without changing display version.

CHANGELOG records durable net state, not failed experiments or Build-by-Build debugging history.

Dependency updates require relevance and exact-revision evidence where applicable. Add device testing only when runtime/UI behavior may change.

Formal stable releases publish from prepared main state and must satisfy release workflow, signing, metadata, version, tag, and changelog checks.

## 10. Development memory

Daily recovery is intentionally short.

Before normal development, read:
1. CONTRIBUTING.md;
2. docs/development/CURRENT.md.

Then load only what the task needs:
- ROADMAP for future sequencing/scope;
- relevant architecture/reference entries for SystemUI work;
- relevant DEVLOG history for past decisions, rejected routes, or device evidence.

Repository state is authoritative over remembered chat context.

### CURRENT
The single day-to-day recovery point. Keep only accepted baseline, active objective/PR, current confirmed conclusions, current validation/blocker state, non-negotiable boundaries, and immediate next step. Do not copy CI history or Build chronology.

### DEVLOG
A decision/evidence history, not a required record for every APK checkpoint. Add an entry when a meaningful root cause is established, important reasoning is rejected/superseded, architecture/ownership/lifecycle/compatibility/fallback changes, device evidence materially changes a decision, or a durable lesson is likely to prevent regression.

A concise default entry is enough:

~~~text
Problem
Evidence
Conclusion
Change
Validation
~~~

Add ownership/lifecycle details only when relevant. Preserve historical entries and append corrections rather than rewriting history.

### ROADMAP
Contains phases, future direction, prerequisites, deferred work, and release exit criteria. It must not become a second DEVLOG.

### Architecture/reference
Update only when a reusable architecture contract or reusable evidence changes. Ordinary checkpoints do not require a documentation cascade.

### CHANGELOG
Update only for durable net behavior, compatibility, public/contributor-facing engineering state, or release changes.

## 11. Definition of done

Apply only the checks relevant to the change.

Runtime-sensitive work: review applicable ownership/writer conflicts, lifecycle/cleanup, fail-native behavior, compatibility, performance, geometry/motion ownership, and focused device acceptance.

UI work: review affected hierarchy, MIUIX behavior, themes, localization, accessibility, and interaction.

Repository/CI work: review triggers, secret boundaries, artifact semantics, and branch protection.

A completed non-trivial change should leave enough information in the PR/commit and, when warranted, CURRENT/DEVLOG to answer what changed and why, how it was validated, and what limitation or next step remains.
