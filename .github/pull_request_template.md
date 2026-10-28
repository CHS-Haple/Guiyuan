## Summary

Describe the problem or engineering need, the resulting change, and its boundaries.

> CI success is validation evidence, not merge approval. Final acceptance is a maintainer decision, and runtime-sensitive changes may require maintainer-side device validation.

## Change type and boundary

- Type: docs / feature / fix / repository automation / other
- Intended target: dev / main
- Objective:
- Explicitly unchanged:
- Why these changes belong together:

> Normal product/runtime contributions target `dev`. Promotion and hotfix pull requests are maintainer-managed.

## MIUIX UI impact (when UI is affected)

- Affected style: OS3 / OS4 / both / neither; include shared controls and style switching, with rationale.
- Corresponding upstream references/revisions **for each affected style** and relevant layout, material, motion, state, interaction and lifecycle contracts:
- Current implementation versus upstream; post-change verification and unverified gaps:
- If OS4 is involved: accepted pinned implementation or new/materially changed PR/component?
- If new/changed: expected benefit, maturity, dependency/ABI and compatibility/performance risks, rollback cost, and adopt / isolated experiment / defer decision:

## Runtime impact

Complete only when runtime-sensitive:

- Owner / state source / call chain:
- New or changed hook/listener/session/writer:
- Cleanup / replacement path:
- Fallback behavior:

## Validation

- CI/local checks:
- Real-device validation: passed / awaiting device validation / N/A
- Device scenario(s) and tested build/SHA when applicable:
- Changelog: updated / not required

## Maintainer-only release section

Complete only for promotion or hotfix pull requests.

For promotion:
- Candidate dev SHA:
- [ ] Candidate matches the validated dev state.
- [ ] No affected item remains awaiting device validation.
- [ ] No new feature/fix/cleanup is included.

For hotfix:
- Source main SHA:
- Dev sync-back plan:

## Known limitations

List only remaining compatibility, validation, or follow-up boundaries.
