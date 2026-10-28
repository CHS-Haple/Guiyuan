# Documentation

Guiyuan keeps current policy, current development state, durable decisions, and platform evidence separate.

For normal development, start with:

1. [CONTRIBUTING.md](../CONTRIBUTING.md) — engineering and workflow rules.
2. [development/CURRENT.md](development/CURRENT.md) — the current accepted development state.

Load the rest only when the task needs it:

- [development/ROADMAP.md](development/ROADMAP.md) — future direction and release exit criteria.
- [development/DECISIONS.md](development/DECISIONS.md) — durable engineering decisions and the reasons behind them.
- [architecture/README.md](architecture/README.md) — current runtime ownership and scene/layout policy.
- [reference/systemui-contracts.md](reference/systemui-contracts.md) — reusable target SystemUI evidence and integration constraints.
- [reference/lsposed-7927-hot-reload.md](reference/lsposed-7927-hot-reload.md) — binary-verified LSPosed generation handoff and lifecycle limits.
- [reference/lsposed-7930-compatibility.md](reference/lsposed-7930-compatibility.md) — bounded 7930 device Hot Reload evidence, archive integrity and remaining compatibility gates.
- [CHANGELOG.md](../CHANGELOG.md) — public release-level changes.
- [OS4 integration audit](../third_party/miuix-glass/UI_AUDIT.md) — source-backed OS4 UI conformance, B1536 matched-source acceptance and remaining variant-specific visual/evidence limits. The ordinary published-MIUIX fallback is not equivalent to the accepted source-built Canary.

## Authority

| Question | Source |
| --- | --- |
| How should code/workflow be handled? | `CONTRIBUTING.md` |
| What is the current accepted development state? | `development/CURRENT.md` |
| What is planned next? | `development/ROADMAP.md` |
| Why is a durable architecture choice in place? | `development/DECISIONS.md` |
| What is the current runtime ownership/layout policy? | `architecture/` |
| What target SystemUI behavior has been verified? | `reference/systemui-contracts.md` |
| What changed for users or releases? | `CHANGELOG.md` |
| What version/build is authoritative? | Gradle project configuration |

Do not copy the same fact into several documents. Current source and verified runtime evidence override stale prose; when a durable rule changes, update the one document that owns that rule.
