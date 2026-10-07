# Runtime reference library

This directory records reusable Android/SystemUI integration evidence that may inform Guiyuan development.

It is an engineering reference, not a dependency declaration, implementation lineage statement, or archive of investigation history.

## Rules

- Record reusable platform behavior and ownership contracts, not incidental third-party product identities.
- Do not copy third-party source code, proprietary assets or implementation-specific constants.
- Keep platform-specific identifiers only when they are necessary to describe the verified target contract.
- Separate observed target behavior from Guiyuan design decisions.
- Reference evidence does not automatically authorize a runtime write. Revalidate the host, lifecycle, writer set, fallback and device behavior before adoption.
- Keep the reusable conclusion, not Build-by-Build chronology, tooling notes or abandoned candidate implementations.
- When evidence changes, update the current conclusion and preserve historical detail through Git history or a dedicated decision record.

## Current entries

- [SystemUI integration contracts](systemui-contracts.md) — host reuse, slot suppression, masking, lifecycle ownership, sizing separation, transition projection, reservation and cleanup boundaries.
- [Native status-icon resource rendering](native-icon-rendering.md) — target resource/tint/rendering evidence and the Guiyuan reuse boundary.

## Evidence language

- **Observed** — directly supported by target source/resource inspection or runtime evidence.
- **Strong inference** — supported by multiple observations but not exposed as one explicit platform contract.
- **Guiyuan rule** — an adopted project constraint based on current evidence.
- **Not established** — insufficient evidence to use as a design premise.
