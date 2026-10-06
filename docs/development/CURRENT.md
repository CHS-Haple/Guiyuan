# Current Development State

## Repository / build

- Product: Guiyuan 0.2.0.
- Stable `main`: Guiyuan 0.2.0 / Build 742 (`20261006-742`).
- Integration `dev`: Guiyuan 0.2.0 / Build 743 (`20261006-743`). PR #220 is merged; exact-head Runtime CI #2770, Work-branch Canary #784, focused device validation and integrated `dev` Runtime CI #2771 passed.
- Active work: PR #221 / `fix/keyguard-island-home-mirror-scope`, Build 744 / `20261006-744`.
- Build 743 fixes the rare Home pull-down -> swipe-up/collapse native fallback by bounding only the QS_FAKE native peer reservation to verified live carrier capacity.
- Historical PR #197 is closed as superseded and must not be restored as an active implementation route.

## Accepted runtime baseline

- Home remains an independent presentation owner.
- Keyguard and AOD share one host-scoped family renderer/presentation owner and retarget scene semantics without duplicate mutable owners.
- AOD is not a Control Center transition source.
- QS_FAKE is the bounded Control Center bridge; the fully expanded Control Center remains native.
- HyperOS remains authoritative for native scene, island, appearance, motion, alpha, visibility and translation timing.
- One transition-reservation writer remains. Fake-carrier capacity is a bounded session lease and fail-native restores only the affected surface.
- Build 689-693 removed or bounded the previously identified diagnostic/reflection hot-path costs. Remaining Keyguard-island smoothness work must not assume diagnostics are still the primary cause without new evidence.

## Active objective

The remaining device-visible performance gap is:

**Keyguard + active island + repeated full Control Center pull-down / swipe-up remains slightly less smooth than the equivalent Home path.**

Build 744 corrects the first proven ownership mismatch before deeper draw/compositing work: the steady-peer mirror samples only Home native island state, so QS_FAKE may consume it only when Home is the authoritative Control Center source. Keyguard/UNKNOWN sources keep native QS_FAKE island authority and no longer scan background Home peer state.

Current priorities:

1. validate Build 744 against Build 743 for Keyguard + active island repeated full pull-down / swipe-up;
2. smoke-test Home + island continuity;
3. if a residual gap remains, audit TransitionDrawable group-alpha layers, repeated draw passes and allocations without changing native timing or geometry.

## Non-negotiable bounds

- Root-cause first; prefer verified native/upstream contracts.
- Keep one owner/writer per mutable surface.
- Do not add polling, delay, retry loops, custom gesture clocks, guessed thresholds or fixed device geometry.
- Do not take over native translation, alpha, visibility or final-QS appearance ownership.
- Do not weaken fail-native or restoration boundaries to gain smoothness.
- Do not trade accepted Home, Keyguard/AOD, charging-island or Control Center geometry for a benchmark-only optimization.
- Keep Detailed diagnostics observational; diagnostic level must not change functional behavior.

## Immediate next

- Review the rebased Build 744 `dev...HEAD` diff and run one exact-head Runtime CI.
- Generate one Work-branch Canary only after CI passes.
- Device A/B: Keyguard + active island repeated full pull-down / swipe-up; then Home + island smoke test.
- If Build 744 removes or materially narrows the gap, merge the ownership correction before any compositing optimization.
- Otherwise retain it only if semantics remain cleaner and continue residual draw/compositing audit separately.
- External version remains 0.2.0.
