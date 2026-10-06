# Current Development State

## Repository / build

- Product: Guiyuan 0.2.0.
- Stable `main`: Guiyuan 0.2.0 / Build 742 (`20261006-742`).
- Integration `dev`: Guiyuan 0.2.0 / Build 744 (`20261006-744`). PR #221 is merged after exact-head Runtime CI #2778 and focused maintainer device acceptance.
- Build 744 scopes the Home-derived steady-peer mirror to Home-origin QS_FAKE only. The accepted device capture stayed healthy across repeated Keyguard Control Center cycles and did not consume the Home mirror from Keyguard.
- Active work: `fix/control-center-alpha-layer-bounds`, Build 745 / `20261006-745`. No PR is open yet.
- Historical PR #197 remains superseded and must not be restored as an active implementation route.

## Accepted runtime baseline

- Home remains an independent presentation owner.
- Keyguard and AOD share one host-scoped family renderer/presentation owner and retarget scene semantics without duplicate mutable owners.
- AOD is not a Control Center transition source.
- QS_FAKE is the bounded Control Center bridge; the fully expanded Control Center remains native.
- HyperOS remains authoritative for native scene, island, appearance, motion, alpha, visibility and translation timing.
- One transition-reservation writer remains. Fake-carrier capacity is a bounded visible-cycle lease and fail-native restores only the affected surface.
- The Home steady-peer mirror is Home-source authority only; Keyguard/UNKNOWN keep native QS_FAKE island authority.
- Build 689-693 removed or bounded the previously identified diagnostic/reflection hot-path costs.

## Active objective

The remaining device-visible performance gap is:

**Keyguard + active island + repeated full Control Center pull-down / swipe-up can still be slightly less smooth than the equivalent Home path.**

Build 745 addresses the next proven compositing cost without changing group-alpha semantics. `ControlCenterTransitionOwner` currently applies per-component group alpha with `Canvas.saveLayerAlpha(null, ...)`, so each transition component can allocate an offscreen target as broad as the current root clip. Android documents `saveLayerAlpha` as an expensive offscreen operation and specifically warns about large bounds.

The candidate keeps `saveLayerAlpha`, component order, matrices and paint alpha unchanged. After applying the existing component matrix, the offscreen layer is bounded to the full local source viewport plus any real component overflow already expressed by transition/clip bounds. This avoids relying on optical bounds as a clipping boundary while still removing full-window alpha layers.

## Non-negotiable bounds

- Root-cause first; prefer verified native/upstream contracts.
- Keep one owner/writer per mutable surface.
- Do not add polling, delay, retry loops, custom gesture clocks, guessed thresholds or fixed device geometry.
- Do not take over native translation, alpha, visibility or final-QS appearance ownership.
- Do not replace group alpha with per-primitive alpha; overlapping draw semantics must remain unchanged.
- Do not change transition endpoints, reservation/capacity, component geometry, clip policy, tint authority or motion progress in this checkpoint.
- Do not use optical bounds alone as the new offscreen clipping boundary.
- Keep Detailed diagnostics observational.

## Immediate next

- Complete Build 745 implementation and full `dev...HEAD` review.
- Run repository-selected Runtime CI.
- Because the checkpoint changes offscreen compositing bounds, request one Work-branch Canary after CI passes.
- Device gate: compare Keyguard + active island repeated complete pull-down/swipe-up against Build 744; smoke-test Home + island, charging island, Battery top text/charging glyph, mobile morph, airplane and no-SIM reveal for clipping or alpha differences.
- External version remains 0.2.0.
