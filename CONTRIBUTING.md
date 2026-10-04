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

## 5. Diagnostics, UI, and text

Diagnostics should be event-driven and bounded:

~~~text
event -> bounded snapshot -> report
~~~

Canary/Release may retain low-frequency operational diagnostics. Detailed geometry/topology probes must stay behind development/Detailed diagnostics and out of hot paths.

Build-channel diagnostic flags such as `RUNTIME_DIAGNOSTICS` and `DEVELOPMENT_PROBES` are **observation gates only**. They must not decide whether a functional hook/state source is installed, which runtime state is authoritative, who owns a surface/property, when ownership is acquired/released, or which fail-native fallback applies. Canary and Release must share the same functional control flow; only logging, bounded probes, diagnostic preferences, and optional diagnostic event callbacks may differ. If a diagnostic source later becomes functional authority, move its functional installation/state path outside the diagnostic gate and leave only observation behind the flag.

For app UI, prefer current MIUIX components and conventions for spacing, typography, shape, state feedback, dialogs, navigation, back behavior, themes, and localization. Persist real user preferences only; Preview/Sandbox state must not become runtime module state.


For semantic row-leading icons:
- use Material Symbols for explanatory row semantics; keep MIUIX icons for MIUIX-owned controls, navigation, and action affordances;
- choose the glyph from the row's semantic meaning, not from a previous icon shape or incidental text in the row value;
- prefer Filled variants and begin from the standard optical weight;
- normalize perceived visual mass rather than numeric weight: line-constructed or hollow-looking glyphs may use a heavier official weight, while already-solid glyphs should remain lighter;
- when Filled becomes too dense or harms legibility, an Outline variant with suitable weight compensation is allowed;
- do not enlarge the whole glyph merely to compensate for low visual mass; preserve the shared optical box, alignment, and tint contract;
- compare neighboring glyphs in both light and dark themes; identical `FILL` / `wght` values are not a goal by themselves.

Current semantic leading icons use a 24 dp optical box with a 22 dp visual size. W400 is the normal starting point; W500 is a common compensation for visually light linear glyphs, not a universal target.

Public identity is Guiyuan / 归元, package com.chaners.guiyuan. Existing CombinedStatus* internal implementation names may remain.

## 6. Git workflow

Use the lightest route that keeps the change attributable.

Branch roles:
- feat/* — one coherent capability or behavior change;
- fix/* — one bounded correction;
- dev — integration;
- main — accepted stable baseline;
- hotfix/* — urgent correction from main;
- dependabot/* — bot-managed dependency proposal.

There is no validation marker branch and no promotion branch in the normal workflow.

A work branch represents one coherent change boundary, not every tweak, diagnostic Build, or device response. Continue an existing branch while it still owns the same objective.

### Product/runtime path

~~~text
feat/* or fix/* -> dev -> dev-to-main PR -> main
~~~

Create work branches from current dev.

A PR may stay Draft while implementation is moving. Mark it ready once it reaches the final automated-review checkpoint; do not repeatedly toggle Draft/Ready after every device response.

Merge to dev when the change is complete, deterministic blockers are resolved, required automated validation passes, and required focused device evidence has passed. If a remaining test genuinely depends on integrated dev state, note it explicitly.

Use squash merge for normal feat/fix -> dev work.

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

## 7. CI and device validation

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

## 8. Versions, dependencies, and release

Current display/build identity comes from project build configuration. ROADMAP records the first formal-release target; no separate version-status document is required.

Normal development checkpoints may advance internal build identity without changing display version.

CHANGELOG records durable net state, not failed experiments or Build-by-Build debugging history.

Dependency updates require relevance and exact-revision evidence where applicable. Add device testing only when runtime/UI behavior may change.

Formal stable releases publish from prepared main state and must satisfy release workflow, signing, metadata, version, tag, and changelog checks.

## 9. Development memory

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

## 10. Definition of done

Apply only the checks relevant to the change.

Runtime-sensitive work: review applicable ownership/writer conflicts, lifecycle/cleanup, fail-native behavior, compatibility, performance, geometry/motion ownership, and focused device acceptance.

UI work: review affected hierarchy, MIUIX behavior, themes, localization, accessibility, and interaction.

Repository/CI work: review triggers, secret boundaries, artifact semantics, and branch protection.

A completed non-trivial change should leave enough information in the PR/commit and, when warranted, CURRENT/DEVLOG to answer what changed and why, how it was validated, and what limitation or next step remains.
