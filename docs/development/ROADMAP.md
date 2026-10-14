# Guiyuan Development Roadmap

This file describes future direction only. Current truth belongs in [CURRENT.md](CURRENT.md); durable engineering decisions belong in [DECISIONS.md](DECISIONS.md).

## Established baseline

The current pre-1.0 line already has:

- projected Home presentation with native-owned surrounding layout and motion;
- optional Keyguard and AOD presentation through one host-scoped family owner;
- a bounded QS_FAKE Control Center transition bridge while the final Control Center remains native;
- event-driven battery, Wi-Fi, mobile network, airplane, SIM/data and tint state;
- fail-native replacement/suppression behavior;
- Hot Reload with generation cleanup;
- local diagnostics and export/share;
- MIUIX companion app navigation, preview/configuration and appearance controls;
- battery-top information, visual sizing/weight controls and battery color schemes.

## Near-term development

Prioritize only work backed by a concrete product need, compatibility requirement or reproducible defect.

Likely areas:
- adaptive size/spacing compatibility when live native geometry demonstrates a need;
- broader target profile validation without assuming compatibility from the current SystemUI baseline;
- Preview/Sandbox scenarios that materially improve configuration usability;
- bounded diagnostics when they answer a real unresolved question;
- remaining companion app polish where MIUIX or Android platform behavior provides a clear owner.

Do not add controls merely because a renderer parameter exists.

## Optional dual-SIM signal presentation (in progress)

**Product intent:** Preserve the current four-dot **single-SIM presentation as the default
and unchanged baseline**. The first approved *visual prototype* is now available only
inside Preview Sandbox (B980, #467): four dots each for physical SIM1 inner
and SIM2 outer, with uniform corresponding-pair spacing and weight-aware
optical geometry. These positions
never swap when the default data SIM changes; the center network-type label
follows the default data SIM only when that subscription's native type is known.
Read-only active-subscription slot mapping and a cautious eight-component native-shape
classifier were also integrated as groundwork. **None of these enable live dual-SIM
rendering or authorize taking over native final Control Center.** A later user opt-in
must remain separate from the sandbox's saved preview state.

Preparation and acceptance should be incremental:

1. **Separate identities before appearances.** The existing subscription-keyed
   `StatusStateStore.mobile`, `SysUiDefaultDataSubSource`, and
   `NativePresentationResolver` already provide relevant sources. Maintain
   distinct facts for *active subscription identity*, *default data subscription*,
   *per-subscription signal availability*, *native aggregation mode*, and
   *display selection*. The optional dual input now refuses to borrow another
   SIM's signal or an aggregate root's identity when data-SIM authority is
   unknown. The shipped single-SIM fallback is unchanged; future live dual
   selection still needs focused SystemUI verification.
2. **Opt-in configuration, only when designs are ready.** Use one stable,
   versioned persisted choice with `single` as the fallback/default; only add
   selectable dual-style values after the actual layouts and capability checks
   exist. Prefer an MIUIX single/dual presentation selection plus a conditional
   dual-style selector; do not add a speculative switch/flag matrix. Existing
   single-SIM settings, saved keys, and render path must not change.
3. **Presentation-only variants.** Reuse the event-driven per-subscription
   state; derive a small immutable display input with explicit data-SIM/other-SIM
   identity, known/unavailable/unknown signal states and SIM removal. Keep style
   selection separate from signal observation and from network-type authority.
   No duplicated SystemUI hooks, timers, telephony observers or global state
   spoofing. Treat one SIM, airplane mode, hot-swap, eSIM changes and uncertain
   native binding as defined fallback cases.
4. **Bounded geometry and transitions.** The original four-dot
   dot-to-native morph remains a single-SIM contract. Optional eight-dot source
   bounds and whole-group no-target fade now share the steady dual geometry
   without claiming a per-SIM native endpoint or reserving space for one.
   Verify width, AOD/Keyguard and QS_FAKE ownership before enabling each live
   style. If the native target does not prove per-card correspondence, retain
   the bounded group fade or fall back to single/native; never invent target
   geometry or modify the final Control Center.
5. **Extend the accepted sandbox prototype only when needed.** B984 supports
   independent SIM1/SIM2 strength and network types, data-SIM selection, and
   both/SIM1-only/SIM2-only/no-SIM insertion simulation. Its eight-dot group is
   1.0 canvas unit lower than accepted B982. Remaining pre-runtime scenarios
   include unknown (not zero-strength) signal input and theme variants.
   Preview state must never become live SystemUI state. Preserve saved enum
   ordinals and the existing single-SIM behavior.
6. **Evidence required before enabling.** Cover data-SIM swap while connected,
   two active subscriptions with different strength/network types, independent
   versus aggregated native icons, mobile-data off with calls available, VPN,
   Wi-Fi, airplane and restoration, one SIM removed or disabled, unknown signal,
   SystemUI/Hot Reload and Home→QS_FAKE→native QS, Keyguard and AOD. Verify both
   selected identity and drawn bars, not only screenshots. If safe native
   presentation ownership cannot be established, revert the optional variant
   to the existing single-SIM path (Fail native for uncertain host ownership).

**Not yet approved:** runtime dual-SIM preference keys, new live callbacks,
native icon suppression, a catalog of untested styles or a dual-row CC transition.
Observe actual SystemUI per-subscription targets and verify live data identity
before enabling any of these. No abstractions for hypothetical future styles.

## Compatibility work

For a new SystemUI target:

- revalidate host identity, lifecycle, native writers and scene topology;
- verify fail-native behavior before enabling replacement;
- preserve native final Control Center ownership;
- revalidate Keyguard/AOD family ownership rather than copying the current target's assumptions;
- prefer platform/native state and resources over project-local replicas.

Compatibility expansion must not weaken the current target's ownership and restoration contracts.

## 1.0.0 qualification

Before the first formal release:

- define and document the supported SystemUI/device scope;
- close acceptance for the advertised Home, Keyguard, AOD and Control Center transition surfaces;
- verify fail-native behavior on unsupported or ambiguous conditions;
- review dependency, signing and release metadata;
- remove obsolete probes/branches that no longer carry engineering value;
- confirm README, privacy, security, support, third-party notices and changelog are release-ready;
- complete a final runtime, maintainability and release-workflow review.

1.0.0 remains an explicit maintainer decision.

## Permanent guardrails

Do not reintroduce without new target evidence and a fresh ownership review:

- polling-based state or geometry ownership;
- copied Battery/peer animation timelines;
- timer/delay lifecycle fixes;
- a permanent duplicate status participant when a verified native carrier exists;
- global represented-slot suppression outside the owning host/session;
- screenshot-fitted geometry or per-resource visual compensation;
- historical Build-specific patches as current architecture.
