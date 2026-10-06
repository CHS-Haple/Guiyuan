# Current Development State

## Repository / build

- Product / promotion candidate: Guiyuan 0.2.1.
- Stable `main` remains Guiyuan 0.2.0 / Build 742 (`20261006-742`) until the dev-to-main promotion completes.
- Integration `dev`: Guiyuan 0.2.1 / Build 746 (`20261006-746`). Build 746 changes only version/release metadata over the accepted Build 744 runtime.
- PR #221 is merged after exact-head Runtime CI, Work-branch Canary, focused maintainer device validation, and integrated `dev` validation.
- PR #223 is merged as repository / exact-target compatibility-contract maintenance without changing runtime behavior or Build identity.
- PR #224 / Build 745 was a bounded Control Center alpha-layer performance experiment. Device validation exposed mobile-signal clipping, so the PR was closed unmerged and the experiment is rejected.
- Historical PR #197 remains superseded and must not be restored as an active implementation route.

## Accepted runtime baseline

- Home remains an independent presentation owner.
- Keyguard and AOD share one host-scoped family renderer/presentation owner and retarget scene semantics without duplicate mutable owners.
- AOD is not a Control Center transition source.
- QS_FAKE is the bounded Control Center bridge; the fully expanded Control Center remains native.
- HyperOS remains authoritative for native scene, island, appearance, motion, alpha, visibility and translation timing.
- One transition-reservation writer remains. Fake-carrier capacity is a bounded visible-cycle lease and fail-native restores only the affected surface.
- The Home steady-peer mirror is Home-source data. Build 744 rejects it for Keyguard/UNKNOWN Control Center sources and restores it only when Home becomes authoritative again.
- Build 689-693 removed or bounded the previously identified diagnostic/reflection hot-path costs. Detailed diagnostics remain observational and must not change functional behavior.
- Build 745 does not belong to the accepted runtime line. Do not reintroduce bounded per-component alpha layers or compensate the observed clipping with guessed padding/margins.

## Active objective

The recent Keyguard / Control Center performance line is **closed at Build 744**. Guiyuan 0.2.1 / Build 746 is the stable-promotion checkpoint and does not reopen that runtime line.

Focused device evidence no longer supports treating the former Keyguard + active island smoothness gap as a blocker, and the next compositing experiment introduced a real visual regression. Further optimization in this area is therefore not justified without new reproducible device evidence.

Normal development may continue from the current `dev` state. Performance work should reopen only for a concrete regression, reproducible hotspot, or new evidence that identifies a bounded root cause.

Current priorities:

1. complete the Guiyuan 0.2.1 / Build 746 dev-to-main stable promotion;
2. keep CURRENT / ROADMAP / public repository facts synchronized with merged code, CI and device evidence;
3. preserve established internal `CombinedStatus*` preference, Hook, diagnostic and compatibility identities unless a concrete migration benefit justifies changing them;
4. do not spend runtime complexity or visual correctness for marginal benchmark-only gains.

## Non-negotiable bounds

- Root-cause first; prefer verified native/upstream contracts.
- Keep one owner/writer per mutable surface.
- Do not add polling, delay, retry loops, custom gesture clocks, guessed thresholds or fixed device geometry.
- Do not take over native translation, alpha, visibility or final-QS appearance ownership.
- Do not weaken fail-native or restoration boundaries to gain smoothness.
- Do not trade accepted Home, Keyguard/AOD, charging-island or Control Center geometry for speculative performance work.
- A branch name alone does not make a route active; require code/PR/CI/device evidence that agrees with the current objective.

## Immediate next

- Treat Build 744 as the accepted runtime baseline; Build 746 is release metadata only.
- Leave PR #224 closed and unmerged; Build 745 is a rejected experiment, not a fallback branch.
- Run the dev-to-main stable promotion boundary with Full validation, then synchronize long-lived `dev` to the promoted `main` head.
- External version is 0.2.1 for this explicitly authorized promotion.
