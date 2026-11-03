# Contributing to Guiyuan

This is the engineering and contribution source of truth for Guiyuan. Use the smallest process that preserves runtime safety, traceability and a clean stable branch. AI coding assistants also follow [AGENTS.md](AGENTS.md) for assistant-specific instructions.

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

For non-trivial decisions, record the alternatives considered, their evidence and trade-offs, and why the chosen approach is preferable before implementation; keep this proportional to the risk and scope.

For defects, investigate root cause before adding a workaround. Do not add geometry, timing, alpha, visibility, polling, retry or delayed patches merely because they hide one symptom.

When several explanations remain plausible, prefer a bounded single-variable diagnostic.

For MIUIX-facing UI adaptations, record before editing whether upstream provides a released API, an experimental PR/sample or no implementation; the exact source/revision and relevant layout, motion, state and lifecycle contract; the current Guiyuan mismatch; and the proposed adaptation and verification boundary. An upstream sample is evidence, not a mandatory public API. Do not replace an accepted control merely to match an illustrative demo.

## Runtime safety

### Ownership and lifecycle

Every long-lived runtime object needs a clear owner, invalidation point and cleanup path. Review host replacement, SystemUI recreation and Hot Reload when relevant.

Prefer host-scoped sessions over unrelated global mutable state.

For SystemUI call chains, hooks, state/lifecycle and Hot Reload changes, inspect the decompiled SystemUI of the corresponding version first. Verify native classes/methods, ownership and cleanup; cite the evidence and explain deviations. Mark inaccessible references as unverified.

### SystemUI reference source

Before repeating target-specific reverse engineering, consult the maintainer-controlled private `SystemUI-Reference` repository **when authorized and accessible**. It owns detailed target-binary identities, DEX method/instruction indexes, investigation notes and device-specific traces. The public `docs/reference/systemui-contracts.md` is limited to reusable integration constraints, not a substitute for the private evidence or a complete proof of platform behavior.

If the private reference is unavailable, record the evidence gap and independently verify an authorized copy of the exact target SystemUI/Framework as required. Do not assume an old binary still matches or substitute generic AOSP internals for a vendor-specific contract. Access to the private reference is not required for unrelated public contributions. Keep decompiled code/binaries, raw device logs, local reverse-tool/MCP configurations and sensitive device details outside this public repository. The reference repository is development-only, never a Gradle/CI/runtime dependency.

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
- metrics, pass rates or proof fields unsupported by actual observations;
- comments that narrate code or preserve debugging history;
- structures that make a small future change require touching unrelated layers.

Keep an unusual structure when a real platform/maintenance constraint justifies it; otherwise simplify it. Add only the smallest useful explanation for a necessary non-obvious structure.

Record each applicable pre-commit check with the touched code/call path, validation method, concrete evidence and conclusion. Explain non-applicable or unverified checks. For a failed check, state the root cause, fix and recheck evidence; resolve deterministic findings before committing. A review conclusion must identify what was actually inspected or executed.

Prefer deleting proven dead code/indirection over renaming it. Do not run CI for every micro-edit; validate meaningful checkpoints. Device evidence is required only when runtime behavior can plausibly change.

## Diagnostics and UI

Diagnostics should be event-driven and bounded:

~~~text
event -> bounded snapshot -> report
~~~

Log useful state transitions, decisions, failures and observations. Structured fields are facts, not mandatory template slots.

A metric, readiness field, or health signal must come from a real runtime observation or calculation. Do not fabricate verification values or inferred coverage statistics.

Build-channel diagnostic flags are observation gates only. They must not control functional hook installation, state authority, ownership, acquisition/release or fail-native fallback. Canary and Release share functional control flow.

For app UI, prefer current MIUIX components and conventions. Persist real user preferences only; Preview/Sandbox state must not become runtime module state.

Before UI changes, inspect applicable MIUIX upstream code, examples and PRs/issues. Record whether an upstream solution exists, its source, rules, applicable scenarios, current differences and intended approach before implementation. Adapt the full affected UI chain (layout, states, interactions, motion/glass, navigation, lifecycle and relevant OS3/OS4 behavior), not just controls. Recheck after editing, resolve deterministic issues and record evidence.

For every MIUIX UI change, record an OS3 / OS4 / both / neither **UI-style** impact decision, including shared components and style switching. These style names do not broaden 1.0.0's HyperOS 4-only runtime support. For each affected style, verify the **corresponding** upstream revision and relevant layout, material, state, motion, interaction and lifecycle rules before editing, then compare the final implementation against them. Do not apply one style's rules to another. Cite the source and outcome in the PR; missing or unpublished guidance is an explicit evidence gap, not an invented upstream standard. Keep verification proportional to the affected call paths.

For OS4-related changes, also classify the upstream surface as the accepted pinned implementation or a new/materially revised PR or component. Explore promising improvements, but assess practical benefit, maturity, transitive dependencies/ABI, compatibility, performance, UI regression, rollback and maintenance cost before adopting new upstream code. Record an adopt / isolated experiment / defer decision. An upstream update never automatically advances the accepted revision; experimental work stays in ordinary feat/fix branches until focused source-matched CI and any necessary device validation pass. This adds a risk gate, not a separate OS4 development or release pipeline.

### UI iconography

For settings-row icon choices and current `SemanticLeadingIcon` sizing, follow [UI iconography](docs/architecture/ui-iconography.md) and recheck its implementation when editing UI. The MIUIX source/evidence and OS3/OS4 style review requirements above still apply.

### Version and build identity

The external `versionName` changes only when the maintainer requests promotion. **Reserve a new Build for an explicit installable APK checkpoint**, not for code changes, PRs, merges, CI runs or branch synchronization. Non-device CI validates source without issuing a new distribution Build even if runtime behavior changed.

For an intended device-test or distribution candidate, compare it with the last delivered candidate. A materially different installable APK needs one new Build: advance Android `versionCode` and `buildId` together before submitting the checkpoint. Rebuilds, source SHAs and branch copies of the same material APK do not each consume a Build. The protected-branch Build-identity pair is the deliberate signal for automatic unsigned Canary/Release artifact production and trusted signing; no new pair means the distribution steps are skipped while required source/build checks continue.

Before delivery, check previously distributed APKs, including historical IT versions, so the intended upgrade has a strictly higher `versionCode`; channel-specific functional differences cannot silently share a Build identity. Use the exact Git SHA and branch/channel to trace every CI result and signed artifact. Keep trusted signing isolated from candidate code. Work-branch device tests still require an explicitly reviewed same-repository PR and a separately authorized signing request; never allow unreviewed PRs to trigger privileged signing automatically.

## Git workflow

Branch roles:
- `feat/*` — coherent capability/behavior change;
- `fix/*` — bounded correction;
- `refactor/*` — behavior-neutral maintainability work;
- `dev` — public/shared integration;
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

### Retired IT channel

The historical `it` channel was retired from routine development and distribution after the v1.0.0 stable release. Do not start IT feature work, synchronize `dev` to `it`, or issue new IT APKs. Preserve historical Git, CI and release evidence.

The historical `it` branch remains for traceability, but current `dev` and `main` Build/signing workflows no longer admit IT APK sources (see [#1189](https://github.com/CHS-Haple/Guiyuan/pull/1189) and [#1190](https://github.com/CHS-Haple/Guiyuan/pull/1190)). Keep historical `app/src/internal/` and `it-only/` content out of public branches, and maintain a public `versionCode` above previously distributed IT versions.

All distributable APKs use the accepted pinned MIUIX source graph. Active channels: `dev` Debug validation and demand-driven signed Canary; `main` Release only. New issue-specific diagnostics require explicitly authorized, time-limited builds and removal plans. Canary remains non-debuggable, release-optimized, and defaults to General diagnostics. No ordinary Release assembly on `dev` or routine Debug/Canary on `main`. Trusted work-branch signing accepts only reviewed exact-SHA source-built artifacts; candidate code never runs with signing credentials. Never substitute the published MIUIX fallback for the accepted source-built OS4 graph.

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

For shared build/signing workflow, pinned-action or contribution-rule changes, compare current `main` and `dev` during the same review. Carry validated common changes through branch-targeted PRs, preserving trust boundaries. Assess legacy IT entries for safe retirement, not synchronization. Do not bump Build identities or merge unrelated branch histories solely to synchronize governance.

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

The **same pre-build device-test decision** applies to `dev` and eligible work branches. For `dev`, a deliberately changed Build identity compiles Canary and uses the trusted inline signer; for eligible `feat/*`, `fix/*`, `refactor/*` and `hotfix/*` PRs, a paired Build identity plus the owner's prior exact-SHA authorization chains successful Build to Work Branch APK signing. The workflow run title identifies the source branch and signing is named `Sign Canary`; `main` keeps an independent stable Release signing boundary. Changes not needing device evidence validate source without signing or allocating a new Build.

Signed work-branch Canary is demand-driven. Decide whether device evidence is needed before the CI checkpoint; an ordinary source-validation run does not require a signed APK. Active protected `dev` and `main` use a deliberate new versionCode/buildId pair as the installable-distribution signal; without one, ordinary validation does not sign.

For automatic work-branch signing, keep the PR Draft until the intended HEAD SHA is ready. The repository owner must post `B<build> Canary SHA <full HEAD SHA>` on that PR **before** source CI begins, then mark it ready. After successful exact-SHA PR Build, the trusted workflow automatically signs its verified unsigned Canary. Without that exact prior authorization, CI validates without signing; changing the PR HEAD requires new authorization. For a later signing decision, Work Branch APK can sign an unexpired exact-SHA unsigned Canary from successful source CI without rebuilding. If the original source-only CI did not produce a Canary artifact, first create an explicitly reviewed paired versionCode/buildId installable checkpoint and run normal source validation; never execute candidate code in the trusted signer. An exact-SHA preauthorization comment records consent before CI; it does not itself start signing and its comment-triggered job is intentionally skipped. The successful Build event triggers the trusted sign check.

The low-privilege pull_request Build compiles the unsigned candidate. The default-branch Work Branch APK selects only successful exact-SHA Builds and unexpired artifacts, then verifies and signs in an isolated trusted runner without checking out or executing candidate source. Work-branch Canary requires a ready, open same-repository PR; signing comments apply only to open PRs. Never run Gradle or restore untrusted Actions caches in the signer. Legacy IT signing paths are retired and must not be requested.

A device test is useful for ownership/lifecycle/scene/transition/geometry changes, first meaningful runtime checkpoints, competing hypotheses, integrated runtime validation or a runtime-affecting stable candidate.

Do not request device testing merely because a commit or APK exists.

Canary is non-debuggable, release-optimized and retains bounded runtime diagnostics. Any new issue-specific native probe requires an independently reviewed, explicitly authorized temporary diagnostic path, and removal after sufficient issue evidence.

## Versions, dependencies and release

Current version/build identity comes from Gradle configuration. Change the displayed version only when explicitly requested; ordinary development retains the existing Build until a device-test or distribution APK checkpoint is deliberately selected (see Version and build identity above).

CHANGELOG records durable release-level changes, not failed experiments or Build-by-Build debugging history.

Dependency updates require relevance and exact-revision evidence where applicable. Add device testing only when runtime/UI behavior may change.

Formal stable releases publish from prepared `main` state and must satisfy release workflow, signing, metadata, version, tag and changelog checks.

## Documentation and source of truth

For the contributor reading order and document responsibilities, use the [documentation index](docs/README.md). Current repository code, CI and verified device evidence take precedence over stale documentation or remembered history. Do not duplicate build-by-build records across long-lived reference documents.

## Definition of done

Apply only checks relevant to the change.

Runtime-sensitive work: review ownership/writer conflicts, lifecycle/cleanup, fail-native behavior, compatibility, performance, geometry/motion ownership and focused device evidence.

UI work: review hierarchy, MIUIX behavior, themes, localization, accessibility and interaction.

Privacy and terms documents require formal, precise language rather than routine UI copyediting. Preserve necessary legal qualifications and exact meanings. For substantive changes, verify disclosures against actual source/runtime behavior, keep Chinese/English meaning equivalent, and synchronize in-app articles with public `PRIVACY.md` and `TERMS.md`. Do not introduce unsupported maintainer identities, private contact methods, consent mechanisms, retention guarantees or legal assurances.

Repository/CI work: review triggers, secret boundaries, artifact semantics and branch protection.

All non-trivial code work: complete the pre-commit maintainability review and resolve deterministic findings before the final commit/CI checkpoint.

A completed non-trivial change should leave enough information in the PR/commit and, when warranted, CURRENT/DECISIONS to answer what changed and why, how it was validated, and what limitation or next step remains.
