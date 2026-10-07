# Native status-icon resource rendering

## Scope

This note records reusable rendering evidence for native status-icon resources on the verified HyperOS target.

It is rendering evidence, not permission to change SystemUI-owned layout, tint, visibility or animation.

## Target

- SystemUI: `17.03.260226.r`
- APK SHA-256: `a0e738e41fe599b97950cbf52a9e2ddc6ae2ceff986efbacb1c9840bea78768d`
- Verified native status-icon height: `20dp` on the inspected target resources.

## Wi-Fi semantic source

The target Home Wi-Fi pipeline exposes a semantic resource identity before final presentation transformation.

For a visible state:

- the model provides a raw resource identity;
- hidden state remains authoritative even if a previously bound ImageView still retains old drawable/tag state.

The raw semantic resource is therefore useful as state identity, but it is not automatically the final resource that SystemUI draws.

## Native View and geometry path

The verified Home Wi-Fi View is an ImageView-based native status participant hosted at the standard status-icon height.

The vector resources use the same `20dp x 20dp` intrinsic/viewport basis on the inspected family.

Steady native rendering therefore does not require an intermediate bitmap merely to bridge vector intrinsic size to the native status slot.

## Light, dark and tint variants

The target contains corresponding Light/base, Dark and Tint resource variants for the verified Wi-Fi family.

For example, the level-3 resources are:

- Light/base: `stat_sys_wifi_signal_3`;
- Dark: `stat_sys_wifi_signal_3_darkmode`;
- Tint: `stat_sys_wifi_signal_3_tint`.

Their geometry is equivalent, while color/alpha presentation differs.

The target resource transformation selects the appropriate variant from native presentation state. Tint mode additionally applies the current ImageView tint.

This transformation belongs to the native presentation contract. A raw semantic resource ID and the final presentation resource ID are intentionally different concepts.

## Final native draw path

The verified steady path is effectively:

`semantic resource -> native presentation variant -> ImageView Drawable -> final ImageView bounds -> framework draw`.

No Wi-Fi-specific final bitmap-resample layer was found in this path.

Neighboring native status icons follow the same broader principle: SystemUI selects/assigns a Drawable and framework ImageView drawing handles the final rasterization.

## Guiyuan boundary

When Guiyuan reuses a verified native center resource, prefer:

`semantic resource -> native-compatible presentation selection -> cloned Drawable -> resolved Guiyuan bounds -> direct Drawable draw`.

Keep separate:

1. semantic resource identity;
2. native state-dependent presentation transformation;
3. Guiyuan optical measurement;
4. Guiyuan placement;
5. final Drawable rasterization;
6. tint authority.

If optical measurement needs a bounded probe, that probe should remain measurement-only. It should not become the final bitmap rendered to screen unless separate evidence establishes that behavior.

Do not use per-resource grayscale multipliers, alpha/coverage remaps, source-asset edits or screenshot-fitted constants as a substitute for the verified native rendering contract.

## Validation

When visual parity differs, isolate the boundary being tested:

- final Drawable/vector rendering;
- state-dependent resource variant;
- tint authority;
- Guiyuan optical placement/scale.

Avoid multi-variable visual changes that make the cause ambiguous.

## Confidence and limits

- Semantic Wi-Fi state/resource identity: target source plus runtime evidence.
- View/layout/resource dimensions: target resource and DEX inspection.
- Light/Dark/Tint transformation: target source/resource inspection.
- Direct Drawable/ImageView steady rendering: directed target inspection.
- Guiyuan owns a different compact composition, so native `20dp` geometry is rendering evidence rather than a requirement to copy the native slot size into Guiyuan.
