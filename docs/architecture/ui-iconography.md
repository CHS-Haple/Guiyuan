# UI iconography

This document records Guiyuan's current shared settings-row leading-icon
conventions. It is a UI design contract, not an upstream MIUIX API guarantee.
The implementation in
[`SemanticLeadingIcon`](../../app/src/main/kotlin/com/chaners/guiyuan/ui/screens/SettingsScreenLayout.kt)
is authoritative for actual geometry; recheck the component and applicable MIUIX
upstream behavior when changing it.

Use Material Symbols for explanatory row semantics and MIUIX icons for
MIUIX-owned controls/navigation/actions.

For Material Symbols:

- select the glyph by semantic meaning after inspecting the actual shape;
- default to Outlined/Regular W400 to match the pinned MIUIX visual language;
- use W500 only when the real glyph is perceptually too light;
- use Filled only when state semantics or legibility justify it;
- normalize perceived mass through official glyph/weight choices, not per-icon
  scale/translation/stroke/alpha hacks;
- use the shared `SemanticLeadingIcon` geometry: 24 dp optical box, 22 dp visual
  size, common alignment and tint;
- first normalize subordinate glyphs using official outlined W500 variants while
  preserving primary glyph weights; then use the shared 12 dp badge inside the
  24 dp optical box (one quarter of its area), independent of the main glyph's
  shape; keep the existing contour cutout and 1 dp gap unchanged;
- review neighboring glyphs in light and dark themes.
