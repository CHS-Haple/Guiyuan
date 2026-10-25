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

## Optional dual-row signal presentation (integrated, compatibility ongoing)

The default is still the accepted single four-dot presentation. [#502](https://github.com/CHS-Haple/Guiyuan/pull/502) integrated an opt-in eight-dot live mode (Build 993): physical SIM1 upper/SIM2 lower, provided **two authoritative active subscriptions** and complete slot mapping are available. The center network-type label follows the verified data SIM; native Control Center remains SystemUI-owned. Missing/ambiguous identity or unavailable signal data uses existing single/native-safe behavior without silently changing the user's opt-in preference. Single/dual visual configurations use separate saved profiles; Preview Sandbox data never becomes SystemUI input.

The one-root aggregate shape morph from historic [#511](https://github.com/CHS-Haple/Guiyuan/pull/511) was selectively reintroduced and device-accepted through [#531](https://github.com/CHS-Haple/Guiyuan/pull/531). Two distinct native subscriber roots still support separately verified row morphing. A single composite root can supply two exact optical row shapes and grouped motion, **not** independent per-SIM identity; its top/bottom positions must never be treated as an authoritative SIM mapping. Native Control Center remains SystemUI-owned.

Remaining work is evidence-led:
1. Revisit third-party native child/rebind paths only when authoritative callbacks or reproducible device evidence show a new membership change. The previous [#521](https://github.com/CHS-Haple/Guiyuan/issues/521) audit is **closed** after the accepted native-slot/cache invalidation and observer-cleanup fixes; it is not a standing implementation task.
2. When relevant hardware is available, exercise data-SIM switching, SIM/eSIM changes, one-SIM recovery, unknown signal, VPN/mobile-data disabled, airplane, Keyguard/AOD and Home→CC reversals on new topologies; keep this separate from B1023 focused acceptance.
3. The optional Duo eight-dot style is integrated. Consider any further style only with a concrete UX need, a single-choice selector and demonstrated compatibility value.
4. Reconsider single/dual **settings-bank** selection only if a transient SIM fallback produces an observed unwanted visual shift. The chosen dual preference must survive SIM loss.
5. Revisit exceptional Hot Reload generation cleanup only with a reproducible failure or new authoritative framework contract, without guessed cleanup, polling or a second writer.



## Compatibility work

For a new SystemUI target:

- revalidate host identity, lifecycle, native writers and scene topology;
- verify fail-native behavior before enabling replacement;
- preserve native final Control Center ownership;
- revalidate Keyguard/AOD family ownership rather than copying the current target's assumptions;
- prefer platform/native state and resources over project-local replicas.

Compatibility expansion must not weaken the current target's ownership and restoration contracts.

## 0.9.1 checkpoint qualification

0.9.1 is a **pre-1.0 development checkpoint**, not the first formal stable release. Before promoting `dev` to `main`:

- complete source-proven native callback ownership and exception recovery in [#758](https://github.com/CHS-Haple/Guiyuan/issues/758), retaining exactly-once native methods, single writer and fail-native cleanup; do not treat passing CI as proof of injected failures;
- accept the **last integrated runtime candidate** on the target for Home, Keyguard, AOD, Control Center, tint/charging, network and Hot Reload. An untested new runtime change invalidates the corresponding earlier device acceptance;
- independently review the full `main → dev` net diff, source ownership/lifecycle, security, cache/performance, configuration/backup compatibility, latest dependency pins and release assets; resolve deterministic issues before promotion;
- move the final durable user-visible `[Unreleased]` notes to a dated `[0.9.1]` section, set `combinedStatus.versionName=0.9.1` with a monotonic internal Build, and validate the exact `dev → main` PR using **Full CI** and the signed APK;
- preserve [#769](https://github.com/CHS-Haple/Guiyuan/issues/769) as an evidence-only third-party font compatibility limitation if the required real font cannot be tested, and do not advertise compatibility that was not verified.

The current Release workflow reserves `stable` publishing for version **1.0.0 or later**. If an explicit 0.9.1 GitHub Release is requested, use the documented **test/prerelease** channel from the accepted source; do not bypass the workflow by relabeling it stable. Main promotion and any release publication require their own explicit acceptance.

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
