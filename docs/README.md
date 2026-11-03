# Documentation / 开发文档

The repository root [README](../README.md) is the **user-facing project homepage**. User policies live at the repository root. Developer records are grouped here by ownership, not by build number.

| Document | Responsibility |
| --- | --- |
| [CONTRIBUTING](../CONTRIBUTING.md) | Contribution rules, code review, CI, branching, and device-evidence requirements |
| [AI coding assistants](../AGENTS.md) | Assistant-only evidence, communications, permissions and verification boundaries |
| [Current state](development/CURRENT.md) | The accepted runtime architecture, verified constraints and outstanding work (not a build diary) |
| [Decisions](development/DECISIONS.md) | Stable design decisions and their technical rationale |
| [Historical 1.0.0 qualification](development/RELEASE_READINESS.md) | Dated first-release evidence, historical gates and original sign-off scope; not a current release checklist |
| [Architecture](architecture/README.md) | Runtime ownership, layout, scene, and native fallback contracts |
| [UI iconography](architecture/ui-iconography.md) | Current settings-row icon semantics, weight and optical geometry |
| [SystemUI reference](reference/systemui-contracts.md) | Public and reusable host contracts; sensitive binary analysis remains private |
| [Framework references](reference/lsposed-7927-hot-reload.md) | Framework compatibility evidence (see related 7930 and 7936 files) |
| [OS4 glass integration](../third_party/miuix-glass/README.md) | Pinned upstream provenance, adaptation boundary and license upkeep |

User-facing policies: [CHANGELOG](../CHANGELOG.md), [TERMS](../TERMS.md), [PRIVACY](../PRIVACY.md), [SECURITY](../SECURITY.md), [THIRD_PARTY_NOTICES](../THIRD_PARTY_NOTICES.md), [Support](SUPPORT.md).

## Reading order

For ongoing development, read [CONTRIBUTING](../CONTRIBUTING.md) → this index → [Current state](development/CURRENT.md). AI assistants additionally read [AGENTS.md](../AGENTS.md). Then consult architecture, decisions, release evidence and third-party references as needed. Live repository/CI and verified device evidence override stale text.

## Maintenance rules

- Do not duplicate long histories across README, CURRENT and RELEASE_READINESS. Build-by-build logs belong in commits, PRs or CI.
- A verified source or host observation takes precedence over stale documentation; update the **owning** document when behavior changes.
- Keep real dependency attribution and licenses. Keep private ROM binaries, raw logs and local analysis tools out of this public repository.
- The detailed [OS4 conformance matrix](../third_party/miuix-glass/CONFORMANCE.md) and [UI audit](../third_party/miuix-glass/UI_AUDIT.md) are **historical engineering evidence**, not app-level documentation or a checklist that overrides current source. Use their dated context before reusing an observation.
