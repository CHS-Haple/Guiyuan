# MIUIX background effect source

The files in `app/src/main/kotlin/top/yukonga/miuix/kmp/effect/bg/` are a
source-pinned copy of MIUIX's experimental
[`miuix-effect` PR #321](https://github.com/compose-miuix-ui/miuix/pull/321)
at commit `73bdd49b4ea965dc7822e276b0a6ce29a5564f54`, licensed Apache-2.0.
Preserve the original
upstream packages, names and copyright/SPDX headers.

The seven `effect.bg` files are copied as-is except `BgEffectModifier.kt`:
- On a theme, device preset or effect state change, restart the existing color
  animation job with the new preset (keeping the current color-stage value).
- Cancel the prior color job before selecting a non-animating preset.

Guiyuan owns only `GyBgEffect` in `ui/screens/about`, shared by About and
Onboarding. About pauses both upstream animation jobs when the gradient is fully
transparent; Onboarding follows its existing lifecycle `resumed` gate.
Shader parameters, drawing and animation jobs remain MIUIX-owned.
The source contains upstream's unused OS2 option solely to keep the snapshot
easy to compare; it does not add HyperOS 2 or 3 runtime support to Guiyuan.

When PR #321 is merged and the effect API is officially published and
compatible,
replace the entire source copy with its published dependency atomically.
Retain only the app-owned `GyBgEffect` adapter, and remove the vendored classes
and this snapshot record. Validate Android/Compose ABI and OS3/OS4 *UI styles*,
the About collapse/restore, Onboarding resume, theme changes and Backdrop
capture.
This is an isolated experimental migration, not the accepted dependency graph.
