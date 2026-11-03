# OS4 glass integration — current source boundary

Guiyuan's optional HyperOS 4 visual style incorporates an adapted subset of upstream MIUIX glass components. This folder documents **provenance and maintenance boundaries**, not an alternative build pipeline or a standalone library.

## Provenance and current dependency graph

- Published MIUIX baseline: `0.9.4-8972f941-SNAPSHOT` at `8972f941c194bd3c4e14c34543d5ebc7b39e40da`.
- Experimental **source-graph qualification only**: [MIUIX glass PR #423](https://github.com/compose-miuix-ui/miuix/pull/423) at `f4cfe9b0adaa5151b68ec169ed18d470b957a6f6` (compared with previously accepted `b9f70838509fbc3f63f1a972777a7e6ce07f578f`). This is not a published or maintainer-approved Glass release, and the app's local Glass compatibility sources have **not** yet been replaced or revalidated. Do not interpret a successful source build as Glass parity or release clearance.
- The CI composite build checks out the exact pinned core/UI/Blur/Preference/Nav source through [the repository action](../../.github/actions/miuix-source/action.yml); the same checkout contains Glass and Glass Icons. **This candidate still uses app-local Glass UI bridges**, so source resolution/compilation is not end-to-end adoption. Matched-source builds now use native Glass action icons from the same-revision `miuix-glass-icons` module; ordinary published-binary fallback retains pinned local vectors in `os4Published`. A later transition must remove duplicate Glass implementations and validate native API, animation, accessibility, insets, gestures and shader fallback together. No production signing or distribution is authorized by this experiment.
- Local Glass UI bridging source remains in `app/src/main/kotlin/top/yukonga/miuix/kmp/glass/`. OS4 action icons use `app/src/os4Upstream/kotlin/com/chaners/guiyuan/ui/icons/Os4NativeGlassIcons.kt` for native same-revision vectors and preserve pinned vector fallback under `app/src/os4Published/kotlin/com/chaners/guiyuan/ui/icons/`. This is not a complete redistributed upstream Glass UI library.
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
