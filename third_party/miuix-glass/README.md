# OS4 glass integration — current source boundary

Guiyuan's optional HyperOS 4 visual style incorporates an adapted subset of upstream MIUIX glass components. This folder documents **provenance and maintenance boundaries**, not an alternative build pipeline or a standalone library.

## Provenance and current dependency graph

- Published MIUIX baseline: `0.9.4-8972f941-SNAPSHOT` at `8972f941c194bd3c4e14c34543d5ebc7b39e40da`.
- Matched-source OS4 integration: [MIUIX glass PR #423](https://github.com/compose-miuix-ui/miuix/pull/423) at `b9f70838509fbc3f63f1a972777a7e6ce07f578f`, with explicitly reviewed compatible source adjustments.
- The accepted CI composite build checks out the pinned source through [the repository action](../../.github/actions/miuix-source/action.yml). Release and Canary builds use that matched-source graph; ordinary published-binary builds **must not** be treated as equivalent proof of OS4 compatibility.
- Local bridging source resides under `app/src/main/kotlin/top/yukonga/miuix/kmp/glass/` and the two `app/src/os4Published` / `app/src/os4Upstream` variants. It is not a complete redistributed upstream library.
- Licensing and individual upstream attribution are recorded in [THIRD_PARTY_NOTICES](../../THIRD_PARTY_NOTICES.md) and preserved source headers. Do not remove SPDX/copyright notices from adapted files.

## Maintenance contract

1. Check whether the upstream component is released, experimental or absent before changing a MIUIX-facing control.
2. Review its layout, state, gesture, scroll, inset, lifecycle and animation behavior against the **exact revision** selected for the build. Do not mix unrelated UI, Blur, Nav and glass implementations.
3. Compare OS3 and OS4 *UI styles* separately. The OS3 visual selector does **not** expand the advertised system support below HyperOS 4.
4. When the upstream module becomes suitable for adoption, compare ABI, transitive dependencies, screenshot/gesture parity, runtime fallback, performance and licensing before replacing local bridge code. Never auto-upgrade on upstream activity.
5. Keep experimental tests and source-matched signing separated from trusted publishing. Do not revive superseded OS4-only workflows.

## Historical conformance evidence

- [CONFORMANCE.md](CONFORMANCE.md): source-backed component and interaction matrix from earlier validation checkpoints.
- [UI_AUDIT.md](UI_AUDIT.md): dated deeper investigations, old trial results and adoption decisions.

These long-form records are **historical**, not current build instructions. For current behavior use the repository source and [CURRENT](../../docs/development/CURRENT.md); for release admission use [RELEASE_READINESS](../../docs/development/RELEASE_READINESS.md). The original build and experimental references are retained in Git history and PR records rather than being reasserted here.
