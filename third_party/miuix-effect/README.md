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

On 2026-11-28, upstream PR #321 was **closed without merging**. No
`miuix-effect` module appears in upstream main's module inventory. The
pinned snapshot remains an experimental, app-compiled source integration,
not a published MIUIX contract. The closure provided no stated replacement
or reason in the upstream PR discussion; do not remove or alter these
classes solely to satisfy historical parameter-count Codacy findings.

If a maintained, published effect API becomes available, assess that
implementation and its compatibility independently. Only then replace
the source copy atomically, retain the app-owned `GyBgEffect` adapter,
and remove the vendored classes and this snapshot record. Validate
Android/Compose ABI, OS3/OS4 *UI styles*, About collapse/restore,
Onboarding resume, theme changes and Backdrop capture. This remains an
isolated experimental migration outside the accepted dependency graph.
