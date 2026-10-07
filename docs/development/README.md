# Development documentation

This directory has three development-state documents:

- [CURRENT.md](CURRENT.md) — day-to-day recovery point and current source of truth.
- [ROADMAP.md](ROADMAP.md) — phases, future direction, prerequisites, deferred work, and release exit criteria.
- [DEVLOG.md](DEVLOG.md) — historical engineering decisions and meaningful evidence.

Writing/synchronization rules live directly in [CONTRIBUTING.md](../../CONTRIBUTING.md).

## Daily workflow

Start with CONTRIBUTING + CURRENT. Read ROADMAP, architecture/reference material, or historical DEVLOG entries only when the active task needs them.

Do not synchronize every fact everywhere:
- CURRENT changes when baseline, active objective, blocker, validation state, or next step materially changes.
- DEVLOG changes for durable root causes, rejected/superseded reasoning, architecture/ownership/lifecycle changes, or meaningful device evidence.
- ROADMAP changes only when future direction changes.
- CHANGELOG changes only for durable net project/release state.

## CI model

- Light — Draft and repository-only/mechanical work.
- Runtime — ordinary app/SystemUI validation; ready PRs build Debug, trusted runtime integration on dev produces signed Canary.
- Full — main/stable boundaries and build/dependency/CI/tooling/release changes.

Signed work-branch Canary is demand-driven. /canary may be requested on any open same-repository feat/*, fix/* or refactor/* PR; the trusted workflow independently validates the exact requested SHA.

Normal runtime path:

~~~text
feat/*, fix/* or refactor/* -> dev -> dev-to-main PR -> main
~~~

There is no validation/dev marker or promote/* stage.
