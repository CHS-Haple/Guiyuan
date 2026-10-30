# Contributing to Guiyuan

This is the engineering and contribution source of truth for Guiyuan. Use the smallest process that preserves runtime safety, traceability and a clean stable branch.

## Baseline

These rules apply to app code, SystemUI integration, compatibility, diagnostics, UI/resources, dependencies, build/release logic, CI and engineering documentation.

English is canonical for source code, engineering documentation, contribution governance, pull requests and repository templates. User-facing documentation may also provide Simplified Chinese.

Contributions intentionally submitted for inclusion are licensed under `GPL-3.0-only`.

Local baseline:
- JDK 21;
- Android SDK 37 / Build Tools 37.0.0;
- Modern Xposed API 102;
- checked-in Gradle Wrapper;
- MIUIX revision pinned by project build files.

For an ordinary code checkpoint:

~~~bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
~~~

Keep credentials, signing material, local SDK paths, generated APK/AAB artifacts and machine-specific configuration out of the repository.

## Engineering principles

**Standardized.** Prefer Android, HyperOS, MIUIX and Modern Xposed contracts over project-local reinvention. Reuse verified platform state, resources and motion when they already express the required semantics.

**Lightweight.** Avoid duplicate hooks/listeners/state machines, polling, hot-path reflection, persistent Root work, per-frame diagnostics and unnecessary caching.

**Modern.** Prefer maintained APIs and pinned dependencies when they satisfy the requirement. Newer is not automatically safer; compatibility and lifecycle evidence still matter.

**Fail native.** If Guiyuan cannot safely establish a replacement contract, preserve or restore native behavior for the smallest affected feature.

## Change method

Before editing:
1. confirm the requirement or defect;
2. identify the responsible owner/state source/lifecycle/API/layout rule and verify the root cause when fixing a defect;
3. investigate relevant native/platform, upstream and existing-repository solutions; continue looking for credible alternatives even if the first option appears suitable;
4. compare viable approaches against evidence, correctness, ownership/lifecycle, compatibility, security, performance, regression/rollback risk and maintenance cost; select the best fit rather than the first workable fix;
5. choose a coherent scope that removes the cause without unrelated churn;
6. identify behavior that must remain unchanged and evidence needed for acceptance.

For non-trivial decisions, record the alternatives considered, their evidence and trade-offs, and why the chosen approach is preferable before implementation; keep this proportional to the risk and scope. Do not invent alternatives or evidence for simple mechanical changes.

For defects, investigate root cause before adding a workaround. Do not add geometry, timing, alpha, visibility, polling, retry or delayed patches merely because they hide one symptom.

When several explanations remain plausible, prefer a bounded single-variable diagnostic.

Before each MIUIX-facing UI adaptation, report to the maintainer **before editing**: whether upstream provides a released API, an experimental PR/sample, or no corresponding implementation; the exact upstream source/revision and its layout, motion, state, and lifecycle contract; the current Guiyuan mismatch; the minimal adaptation and its verification boundary. An upstream sample is evidence, not a mandatory public API. Do not replace an accepted control merely to match an illustrative demo.

## Runtime safety

### Ownership and lifecycle

Every long-lived runtime object needs a clear owner, invalidation point and cleanup path. Review host replacement, SystemUI recreation and Hot Reload when relevant.

Prefer host-scoped sessions over unrelated global mutable state.

For SystemUI call chains, hooks, state/lifecycle and Hot Reload changes, inspect the decompiled SystemUI of the corresponding version first. Verify native classes/methods, ownership and cleanup; cite the evidence and explain deviations. Mark inaccessible references as unverified.

### State and writers

Prefer:

~~~text
native/event source -> domain state -> scene/presentation policy -> renderer
~~~

Use the highest verified native semantic source available. Do not maintain a parallel parser/state machine for a fact SystemUI already exposes authoritatively unless compatibility requires it.

Observation is not ownership. One live mutable property should have one writer. Width, position, translation, alpha, visibility, tint, animation and attachment are ownership-sensitive.

Hooks are integration points, not architecture. Each hook needs one responsibility, an owner, failure behavior and a bounded lifecycle.

### Native resources

When reusing a verified SystemUI drawable/resource, preserve its semantic identity and native presentation behavior. Do not add per-resource grayscale/alpha correction, raster preprocessing/resampling or screenshot-fitted constants merely to force visual similarity.

Do not suppress a native representation until the Guiyuan replacement is valid for the current session.

### Geometry and motion

Keep separate:
1. native layout/slot ownership;
2. Guiyuan drawing geometry;
3. transition/motion geometry;
4. optical adjustment.

A sizing issue does not automatically justify native layout mutation. A transition issue does not automatically justify changing steady geometry. Prefer verified native progress/endpoints and motion ownership.

### Compatibility

For SystemUI-owned behavior, inspect the exact pinned target APK's DEX/resources and native method call sites before inferring from AOSP or third-party implementations. Verify the input binary, and distinguish SystemUI overrides from inherited Android framework behavior; neither static disassembly nor generic source proves exceptional device timing.

The pinned SystemUI profile proves only the current target. Live topology is authoritative when system versions or other modules alter the View tree.

A compatibility failure should fail native for the smallest affected surface.

## Maintainability

Optimize for the next human maintainer. Prefer concise scope-aware names and structures; avoid both modifier-heavy verbosity and cryptic shortening.

When a package becomes crowded, group files by problem domain rather than technical role. Keep source paths aligned with Kotlin packages and move cohesive areas incrementally instead of churning unrelated code.

### Naming

- Public identity remains Guiyuan / 归元 with package `com.chaners.guiyuan`.
- Local/private names may be short when context already carries the meaning; cross-file/architecture names must remain searchable and unambiguous.
- Familiar forms such as `SysUi`, `Diag`, `Repo`, `Prefs`, `Cfg`, `AOD`, `QS` and `CC` are preferred when they reduce typing without reducing clarity.
- Remove repeated product/domain wording when the package/file/receiver already establishes it.
- Keep `Owner`, `Source`, `Policy`, `Session`, `Contract`, `Probe` or similar suffixes only when they carry real lifecycle, authority, compatibility, reuse or policy meaning.
- Persisted keys, protocol/log fields, reflection targets, resource identities, Xposed-facing IDs and other externally consumed names are compatibility surfaces; do not rename them as cosmetic cleanup.
- Prefer semantic compression over mechanical shortening. Do not use broad search-and-replace for a rename without inspecting call sites and same-text uses.
- Remove dead helpers only after checking reflection, serialization, resources, generated code and other external reachability.

### Comments

Add comments only when they explain a non-obvious reason, invariant, ownership/lifecycle boundary, fallback or platform limitation. Keep them short and natural.

Do not narrate code, repeat names/types, stamp comment templates across files or preserve Build-by-Build debugging history in source comments.

### Refactor discipline

Maintainability refactors are behavior-neutral by default.

Do not:
- add a type/helper merely to name an obvious expression, pass through a value or wrap one caller;
- invent a success/failure model without a real failure source;
- stack Result/State wrappers around the same outcome without a real boundary;
- split one cohesive operation into ceremonial Source/Owner/Policy/Probe/Resolver layers;
- flatten one mutually exclusive lifecycle into invalid combinations of `pending/ready/active` booleans;
- force genuinely independent facts into one state machine for symmetry.

Work in coherent reviewable batches. Before a non-trivial commit, review the complete base→HEAD diff for incomplete renames, compatibility-string changes, semantic drift, lifecycle/writer changes and unnecessary churn.

### Pre-commit maintainability review

Every non-trivial code batch gets a separate maintainability review before the final commit/CI checkpoint.

Check touched code and relevant adjacent call sites for:
- unnecessarily long or suffix-stacked names;
- boolean/nullable fields that actually encode one lifecycle;
- string/reason-code control flow where a direct model is clearer;
- one-caller wrappers, pass-through helpers and mirrored abstractions;
- synthetic failure branches, proof-only helpers/tests or duplicated Result/State layers;
- diagnostics that fill templates rather than report facts;
- invented metrics, pass rates or proof fields;
- comments that narrate code or preserve debugging history;
- structures that make a small future change require touching unrelated layers.

Keep an unusual structure when a real platform/maintenance constraint justifies it; otherwise simplify it. Add only the smallest useful explanation for a necessary non-obvious structure.

Record each applicable pre-commit check with the touched code/call path, validation method, concrete evidence and conclusion. Explain non-applicable or unverified checks. For a failed check, state the root cause, fix and recheck evidence; resolve deterministic findings before committing. A bare `PASS`, unrun test or invented metric is not evidence.

Prefer deleting proven dead code/indirection over renaming it. Do not run CI for every micro-edit; validate meaningful checkpoints. Device evidence is required only when runtime behavior can plausibly change.

## Diagnostics and UI

Diagnostics should be event-driven and bounded:

~~~text
event -> bounded snapshot -> report
~~~

Log useful state transitions, decisions, failures and observations. Structured fields are facts, not mandatory template slots.

A metric, readiness field, or health signal must come from a real runtime observation or calculation. Do not hard-code values merely to prove an invariant or produce self-derived `N/N`, `100%`, or coverage-looking output.

Build-channel diagnostic flags are observation gates only. They must not control functional hook installation, state authority, ownership, acquisition/release or fail-native fallback. Canary and Release share functional control flow.

For app UI, prefer current MIUIX components and conventions. Persist real user preferences only; Preview/Sandbox state must not become runtime module state.

Before UI changes, inspect applicable MIUIX upstream code, examples and PRs/issues. Explain **before editing** whether an upstream solution exists, its source, rules, applicable scenarios, current differences and intended approach. Adapt the full affected UI chain (layout, states, interactions, motion/glass, navigation, lifecycle and relevant OS3/OS4 behavior), not just controls. Recheck after editing, resolve deterministic issues and record evidence.

For every MIUIX UI change, record an OS3 / OS4 / both / neither style-impact decision, including shared components and style switching. For each affected style, verify the **corresponding** upstream revision and relevant layout, material, state, motion, interaction and lifecycle rules before editing, then compare the final implementation against them. Do not apply one style's rules to another. Cite the source and outcome in the PR; missing or unpublished guidance is an explicit evidence gap, not an invented upstream standard. Keep verification proportional to the affected call paths.

For OS4-related changes, also classify the upstream surface as the accepted pinned implementation or a new/materially revised PR or component. Explore promising improvements, but assess practical benefit, maturity, transitive dependencies/ABI, compatibility, performance, UI regression, rollback and maintenance cost before adopting new upstream code. Record an adopt / isolated experiment / defer decision. An upstream update never automatically advances the accepted revision; experimental work stays in ordinary feat/fix branches until focused source-matched CI and any necessary device validation pass. This adds a risk gate, not a separate OS4 development or release pipeline.

### Semantic leading icons

Use Material Symbols for explanatory row semantics and MIUIX icons for MIUIX-owned controls/navigation/actions.

For Material Symbols:
- select the glyph by semantic meaning after inspecting the actual shape;
- default to Outlined/Regular W400 to match the pinned MIUIX visual language;
- use W500 only when the real glyph is perceptually too light;
- use Filled only when state semantics or legibility justify it;
- normalize perceived mass through official glyph/weight choices, not per-icon scale/translation/stroke/alpha hacks;
- use the shared `SemanticLeadingIcon` geometry: 24 dp optical box, 22 dp visual size, common alignment and tint;
- first normalize subordinate glyphs using official outlined W500 variants while preserving primary glyph weights; then use the shared 12 dp badge inside the 24 dp optical box (one quarter of its area), independent of the main glyph's shape; keep the existing contour cutout and 1 dp gap unchanged;
- review neighboring glyphs in light and dark themes.

### Version and build identity

The external `versionName` changes only when the maintainer requests promotion. **Reserve a new Build for an explicit installable APK checkpoint**, not for code changes, PRs, merges, CI runs or branch synchronization. Non-device CI validates source without issuing a new distribution Build even if runtime behavior changed.

For an intended device-test or distribution candidate, compare it with the last delivered candidate. A materially different installable APK needs one new Build: advance Android `versionCode` and `buildId` together before submitting the checkpoint. Rebuilds, source SHAs and branch copies of the same material APK do not each consume a Build. The protected-branch Build-identity pair is the deliberate signal for automatic unsigned Canary/Release artifact production and trusted signing; no new pair means the distribution steps are skipped while required source/build checks continue.

Before delivery, check other public/IT installed versions so the intended upgrade has a strictly higher `versionCode`; channel-specific functional differences cannot silently share a Build identity. Use the exact Git SHA and branch/channel to trace every CI result and signed artifact. Keep trusted signing isolated from candidate code. Work-branch device tests still require an explicitly reviewed same-repository PR and a separately authorized signing request; never allow unreviewed PRs to trigger privileged signing automatically.

## Git workflow

Branch roles:
- `feat/*` — coherent capability/behavior change;
- `fix/*` — bounded correction;
- `refactor/*` — behavior-neutral maintainability work;
- `dev` — public/shared integration;
- `it` — unstable internal-test integration, not a promotion source;
- `main` — accepted stable baseline;
- `hotfix/*` — urgent stable correction;
- `dependabot/*` — dependency proposal.

Normal runtime path:

~~~text
feat/*, fix/*, or refactor/* -> dev -> dev-to-main PR -> main
~~~

Create work branches from current `dev`. One branch represents one coherent objective, not every tweak or device response. The trusted work-branch APK workflow accepts only same-repository `feat/*`, `fix/*`, `refactor/*` and `hotfix/*` sources; use an eligible prefix for dependency proposals requiring a signed Canary, not `build/*`.

A PR may remain Draft while implementation is moving. Mark it ready at the final automated-review checkpoint. Merge to `dev` only when deterministic blockers are resolved, required CI is green and required focused device evidence has passed.

Use squash merge for ordinary work branches.

### IT synchronization and boundaries

- `it` is a long-lived test branch. Develop IT-only experiments from an `it` base and merge them into `it` by reviewed PR. The IT-owned `it-only/RESTRICT_PROJECT_LINK` marker disables in-app project navigation for Debug and Canary without changing their build-type semantics. This does not hide the entry, remove embedded strings, or make the APK confidential.
- Sync public fixes **dev → it** through reviewed integration PRs. For **it → dev**, create a fresh branch from current `dev`, selectively port only generally applicable fixes and submit a separate PR. Never merge the whole `it` branch into `dev` or `main`. Stable fixes go `main → dev → it`.
- IT-only sources must stay under `app/src/internal/` or `it-only/` and must not enter public branches. Shared-path changes require semantic review: a file-path guard cannot detect all IT-only behavior. No public branch can inherit IT behavior just because its fix was observed during IT testing.
- Both channels retain package/signing identity. Plan `versionCode` monotonicity so installation of a later public version after IT is possible. No Build bump for branch creation or CI/governance only.
- The public PR CI rejects direct `it` heads and exclusive paths. Protect `it` via PR + required official Build check, not an in-PR check alone. The protected IT Canary is distributable only after its exact-SHA push Build and the separate main-trusted signed-APK verification have succeeded.


All distributable CI APKs use the same accepted, pinned MIUIX source graph. The build matrix is: `dev` Debug validation and demand-driven signed Canary; `it` Debug validation and daily-use optimized Canary; `main` Release only. Any new issue-specific diagnostic package requires a separate explicitly authorized, time-limited build/signing path with a removal plan. Canary remains non-debuggable, release-optimized and defaults to General diagnostics on every branch. Detailed diagnostics must be user-selected, while issue-specific probes require an explicit removal plan and must not enter public `dev`. No ordinary Release assembly on `dev` or `it`; no routine Debug/Canary on `main`. The trusted Work Branch APK signs only successful exact-SHA source-built artifacts; candidate code never executes with Haple credentials. The old Internal producer/distribution role is retired once ordinary protected IT Canary signing has passed. Never substitute the published MIUIX fallback for the accepted source-built OS4 graph.

### Stable promotion

The dev-to-main PR is the stable promotion boundary.

Before merge:
- required CI is green;
- the latest runtime-affecting state has applicable device acceptance;
- no runtime blocker remains;
- public documentation/changelog reflects the resulting stable state when needed.

Documentation-only commits do not invalidate prior device evidence when they cannot affect the APK/runtime.

After a dev-to-main merge, verify that long-lived `dev` still exists; recreate it at the promoted `main` SHA if repository auto-delete removed it.

### Repository-only maintenance

Pure docs/metadata/governance may use the shortest safe path. Keep shared policy/current-state documents aligned across long-lived branches when divergence would mislead development.

CI/build/release workflow changes require Full validation because they change the validation mechanism; device testing is needed only if APK/runtime behavior also changes.

### Hotfix

Hotfix from `main`, keep it narrow, validate it, merge to `main`, then reconcile it into `dev`.

## CI and device validation

Developer-facing scopes:

- **Light** — Draft and proven repository-only/mechanical changes.
- **Runtime** — ordinary app/SystemUI PRs and trusted runtime integration on `dev`; validates target profile, tests, Debug build and Xposed metadata as configured.
- **Full** — stable admission plus dependency/build/CI/tooling/signing/release changes; compiles and validates only the required target-branch APK variants. Signing remains a separate trust step.

CI proves configured source/build checks, not SystemUI runtime correctness.
Preserve each pushed `dev`/`main` commit's required integration check: a later documentation-only push must not cancel an unfinished Runtime/Full validation. Superseded PR revisions may cancel their older checks.

Use concise PR titles, typically `type(scope): description`. GitHub Actions appends `| CI #PR` for PR checks. For a Build-labeled APK run, an owner comment such as `B850 Canary` produces `type(scope): description | B850 Canary #PR`; the workflow verifies the requested Build against the checked-out Gradle source. The older `/canary` comment remains compatible but cannot show a Build in the run title.

Do not expose signing credentials or project-signed artifacts to untrusted fork workflows.

### Canary

Signed work-branch Canary is demand-driven. Decide whether device evidence is needed before the CI checkpoint; an ordinary source-validation run does not require a signed APK. Protected `dev`, `it` and `main` use a deliberate new versionCode/buildId pair as the installable-distribution signal; without one, their ordinary validation does not sign.

For automatic work-branch signing, keep the PR Draft until the intended HEAD SHA is ready. The repository owner must post `B<build> Canary SHA <full HEAD SHA>` on that PR **before** source CI begins, then mark it ready. After successful exact-SHA PR Build, the trusted workflow automatically signs its verified unsigned Canary. Without that exact prior authorization, CI validates without signing; changing the PR HEAD requires new authorization. For a later signing decision, use the existing Work Branch APK manual/owner-comment path to sign an eligible validated artifact without rebuilding.

The low-privilege pull_request Build compiles the unsigned candidate. The default-branch Work Branch APK only selects a successful exact-SHA Build and its unexpired artifact, then verifies and signs it in an isolated trusted runner without checking out or executing candidate source. Work-branch Canary requires a ready, open same-repository PR. Protected `it` Canary requires the current protected IT SHA and successful exact-SHA Push Build; the owner may dispatch signing or request `B<build> Canary` on its just-merged current-head IT PR. Stale, foreign and unmerged IT PRs are not eligible; ordinary work-branch signing comments apply only to open PRs. Never run Gradle or restore untrusted Actions caches in the signer.

A device test is useful for ownership/lifecycle/scene/transition/geometry changes, first meaningful runtime checkpoints, competing hypotheses, integrated runtime validation or a runtime-affecting stable candidate.

Do not request device testing merely because a commit or APK exists.

Canary is non-debuggable, release-optimized and retains bounded runtime diagnostics. IT-only policy is branch-owned, not a new build type; Debug and Canary must share its app-side navigation restrictions without altering SystemUI behavior or Xposed scope. The former Internal build type is retired from routine distribution. IT-specific native probes must not enter normal Canary. They require an independently reviewed, explicitly authorized temporary diagnostic path and removal after sufficient issue evidence.

## Versions, dependencies and release

Current version/build identity comes from Gradle configuration. Change the displayed version only when explicitly requested; ordinary development retains the existing Build until a device-test or distribution APK checkpoint is deliberately selected (see Version and build identity above).

CHANGELOG records durable release-level changes, not failed experiments or Build-by-Build debugging history.

Dependency updates require relevance and exact-revision evidence where applicable. Add device testing only when runtime/UI behavior may change.

Formal stable releases publish from prepared `main` state and must satisfy release workflow, signing, metadata, version, tag and changelog checks.

## Development memory

For normal recovery read:
1. `CONTRIBUTING.md`;
2. `docs/development/CURRENT.md`.

Then use:
- ROADMAP for future direction;
- DECISIONS for durable rationale;
- architecture docs for current ownership/layout policy;
- SystemUI reference for reusable target evidence.

Repository state is authoritative over remembered chat context.

### Document ownership

- **CURRENT** — accepted baseline, current blockers/guardrails and immediate next step; no Build/CI diary.
- **ROADMAP** — future direction and release exit criteria; no debugging history.
- **DECISIONS** — durable decisions/rationale only; no checkpoint log.
- **Architecture/reference** — reusable current policy/evidence only.
- **CHANGELOG** — durable release-level behavior and public engineering changes.

Do not synchronize every fact everywhere.

## Definition of done

Apply only checks relevant to the change.

Runtime-sensitive work: review ownership/writer conflicts, lifecycle/cleanup, fail-native behavior, compatibility, performance, geometry/motion ownership and focused device evidence.

UI work: review hierarchy, MIUIX behavior, themes, localization, accessibility and interaction.

Repository/CI work: review triggers, secret boundaries, artifact semantics and branch protection.

All non-trivial code work: complete the pre-commit maintainability review and resolve deterministic findings before the final commit/CI checkpoint.

A completed non-trivial change should leave enough information in the PR/commit and, when warranted, CURRENT/DECISIONS to answer what changed and why, how it was validated, and what limitation or next step remains.
