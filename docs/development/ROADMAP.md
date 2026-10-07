# Guiyuan Development Roadmap

This file describes future direction only. Current truth belongs in [CURRENT.md](CURRENT.md); durable engineering decisions belong in [DECISIONS.md](DECISIONS.md).

## Established baseline

The current pre-1.0 line already has:

- projected Home presentation with native-owned surrounding layout and motion;
- optional Keyguard and AOD presentation through one host-scoped family owner;
- a bounded QS_FAKE Control Center transition bridge while the final Control Center remains native;
- event-driven battery, Wi-Fi, mobile-network, airplane, SIM/data and tint state;
- fail-native replacement/suppression behavior;
- Hot Reload with generation cleanup;
- local diagnostics and export/share;
- MIUIX companion-app navigation, preview/configuration and appearance controls;
- battery-top information, visual sizing/weight controls and battery color schemes.

## Near-term development

Prioritize only work backed by a concrete product need, compatibility requirement or reproducible defect.

Likely areas:
- adaptive size/spacing compatibility when live native geometry demonstrates a need;
- broader target-profile validation without assuming compatibility from the current SystemUI baseline;
- Preview/Sandbox scenarios that materially improve configuration usability;
- bounded diagnostics when they answer a real unresolved question;
- remaining companion-app polish where MIUIX or Android platform behavior provides a clear owner.

Do not add controls merely because a renderer parameter exists.

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
