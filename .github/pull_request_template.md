## Summary / 变更概述

Describe the problem, change and scope. / 简述问题、改动内容及边界。

> CI success is validation evidence, not merge approval. Final acceptance is a maintainer decision, and runtime-sensitive changes may require maintainer-side device validation.

## Change type and boundary / 变更类型与范围

- Type: docs / feature / fix / repository automation / other
- Intended target: dev / main
- Objective:
- Explicitly unchanged:
- Why these changes belong together:

> Normal product/runtime contributions target `dev`. Promotion and hotfix pull requests are maintainer-managed.

## MIUIX UI impact / MIUIX 界面影响（涉及界面时填写）

- Affected style: OS3 / OS4 / both / neither; include shared controls and style
  switching, with rationale.
- Corresponding upstream references/revisions **for each affected style** and
  relevant layout, material, motion, state, interaction and lifecycle contracts:
- Current implementation versus upstream; post-change verification and
  unverified gaps:
- If OS4 is involved: accepted pinned implementation or new/materially changed
  PR/component?
- If new/changed: expected benefit, maturity, dependency/ABI and
  compatibility/performance risks, rollback cost, and adopt / isolated
  experiment / defer decision:

## Runtime impact / 运行时影响

Complete only when runtime-sensitive:

- Owner / state source / call chain:
- New or changed hook/listener/session/writer:
- Cleanup / replacement path:
- Fallback behavior:

## Validation / 验证

- CI/local checks:
- Real-device validation: passed / awaiting device validation / N/A
- Device scenario(s) and tested build/SHA when applicable:
- Changelog: updated / not required

## Maintainer-only release section / 维护者发布检查

Complete only for promotion or hotfix pull requests.

For promotion:

- Candidate dev SHA:
- [ ] Candidate matches the validated dev state.
- [ ] No affected item remains awaiting device validation.
- [ ] No new feature/fix/cleanup is included.

For hotfix:

- Source main SHA:
- Dev sync-back plan:

## Known limitations / 已知限制

List only remaining compatibility or validation gaps. / 仅列出尚未解决的兼容性或验证问题。
