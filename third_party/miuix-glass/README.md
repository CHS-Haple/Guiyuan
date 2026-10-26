# Experimental MIUIX OS4 Glass (PR #423)

Source: https://github.com/compose-miuix-ui/miuix/pull/423
Pinned upstream commit: b9f70838509fbc3f63f1a972777a7e6ce07f578f
License: Apache-2.0 (see LICENSE; original per-file headers retained).

This experimental app-only source subset lives in
`app/src/main/kotlin/top/yukonga/miuix/kmp/glass/` and comprises the upstream material,
shader, silhouette, stroke, shadow, motion, navigation and tab primitives. Source files are
copied unchanged. OS3 UI remains on the normal published MIUIX modules.

Do not copy unsupported new popup-host APIs into old MIUIX dependencies, or mix two MIUIX
release revisions. When `miuix-glass-android` is officially published, remove these vendored
sources and replace them with a pinned module dependency after parity/CI/device review.

Known non-parity: the pending PR's `GlassTopAppBar`, `GlassDialog`, and `GlassBottomSheet`
need upstream UI interfaces not in the currently published main snapshot. Do not label
OS4 experimental as complete until those components and OS4 icon contracts are integrated.
