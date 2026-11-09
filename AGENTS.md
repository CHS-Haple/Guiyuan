# AI Coding Assistant Instructions

For AI coding assistants working on Guiyuan. This file supplements [CONTRIBUTING.md](CONTRIBUTING.md), which remains the authoritative engineering, review and security standard for everyone. Human contributors do not need assistant-specific dialogue and tool instructions.

## Verify the actual state

- Read CONTRIBUTING.md → docs/README.md → docs/development/CURRENT.md. Verify
  current main/dev heads, Gradle version/build identity, relevant open
  PRs/issues and source-matched CI before making changes.
- Treat current GitHub/source, corresponding MIUIX/SystemUI versions and actual
  device evidence as authoritative. Do not silently reuse old conversation
  summaries, dated build reports or superseded workflows.
- Never claim to have opened a reference repository, private SystemUI binary,
  APK, decompilation, device trace, CI log or source that the available tools
  did not read. Record missing evidence and ask for the specific authorized
  file, log or device observation needed.

## Investigate and communicate

- For defects, distinguish observations from hypotheses. Trace relevant callers,
  state sources, writers and lifecycle, then compare verified alternatives. Do
  not invent alternative designs, evidence or tests to fill a template.
- **Before MIUIX-facing UI edits**, briefly report the inspected upstream
  revision and whether an API is released, experimental or absent; its layout,
  state, motion, interaction and lifecycle contracts; Guiyuan's mismatch; the
  chosen adaptation and its verification boundary. Compare OS3 and OS4 styles
  separately when applicable.
- Before non-trivial commits, inspect full base→HEAD diff and relevant adjacent
  callers. Give a proportional report with code locations, actual verification
  evidence, unsupported items, risks and gating conditions. Mark unrun tests as
  unverified. A historical CI pass or signed older Build does not validate the
  changed HEAD.
- Do not fabricate PASS claims, N/N percentages, test coverage, runtime metrics
  or status fields, and do not create proof-only abstractions, tests or verbose
  checklist tables purely to make work look thorough.

## Authorization and tools

- Treat upstream and third-party repositories as **read-only research by
  default**. General permission to continue developing, investigate or follow
  upstream does not authorize comments, issues, reviews, reactions, PRs, commits
  or any other external repository write under the maintainer's account. Obtain
  prior explicit approval naming the external repository and operation;
  otherwise keep findings in Guiyuan.
- Refer to private SystemUI-Reference only when authorized and accessible. Do
  not infer exact Xiaomi contracts from generic Android code or publish ROM
  binaries, raw traces, local reverse-engineering tools/configuration or signing
  secrets.
- Apply CONTRIBUTING branch and CI boundaries. Do not infer permission for
  stable release or privileged signing from an ordinary development request.
  Historical IT was retired after v1.0.0; never start IT synchronization, new IT
  work or IT signing.
- First check supplied attachments and authorized sources. If a proposed
  operation requires a specific artifact, device observation, or permission that
  remains unavailable, stop only that dependent operation; state the missing
  item, what it blocks, and the exact help needed. Continue independent reviews
  and fixes. Do not conceal blockers, claim background progress or promise a
  later result without a running authorized workflow.

## Preserve useful project context

- Keep durable decisions in DECISIONS, current accepted limits in CURRENT,
  architecture contracts in architecture/reference, and patch/test evidence in
  PRs and CI. Do not duplicate historical checklists across permanent documents.
- Preserve actual third-party copyright notices and licenses; do not expose
  private data or claim an external maintainer's approval without evidence.
- Do not apply ordinary UI copy-shortening or semicolon-avoidance preferences to
  formal privacy and terms documents. Preserve necessary legal precision;
  compare the actual in-app and public policy text when editing.
- AI instructions never relax the common root-cause, lifecycle, single-writer,
  fail-native, review, signing and security rules.
