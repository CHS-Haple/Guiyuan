# Guiyuan layout policy

## Purpose

Guiyuan keeps four responsibilities separate:

1. native SystemUI layout and occupancy;
2. Guiyuan visual geometry;
3. transition and motion geometry;
4. optical adjustment.

A value from one responsibility must not silently become the control value for another.

## Home ownership

The accepted Home path uses the existing native end-side host:

`MiuiNotificationStatusContainer / system_icon_area -> MiuiStatusBatteryContainer / system_icons -> Guiyuan child -> logical viewport -> HomeLayoutResolver -> LayoutPolicy -> renderer`

SystemUI owns:

- native Battery composition and hide state;
- native peer measurement/layout;
- scene and appearance facts;
- native tint authority;
- island/Folme motion;
- native alpha, visibility and translation.

Guiyuan owns:

- its child View and renderer;
- the logical replacement viewport;
- transparent drawing overflow above that viewport when required;
- replacement-slot intent;
- bounded represented-slot exclusion;
- reversible presentation masks;
- one conflict-checked end reservation.

Guiyuan must not write native Battery measurement/layout width, native peer translation, native peer alpha, or native peer visibility.

## Render modes

### PROJECTED

Guiyuan renders against verified native host geometry while SystemUI remains authoritative for surrounding layout and motion.

Home uses this model. Keyguard/AOD use the same principle through their own verified family host. The bounded Control Center bridge uses projection only during an eligible transition.

### NATIVE_ONLY

Guiyuan does not replace the surface. SystemUI retains presentation and motion ownership.

Notification Shade and the fully expanded Control Center are native-only.

## Resolved layout contract

The shared resolver may consume Guiyuan presentation intent:

- canonical visual size;
- user visual scale;
- neighbor optical gap;
- per-glyph scale;
- bounded optical adjustment.

A host adapter may supply verified native facts:

- host height;
- end anchor;
- stable carrier width;
- scene capability;
- motion owner;
- source geometry needed by a projection layer.

The resolver keeps separate:

- render eligibility;
- visual bounds;
- requested optical gap;
- requested replacement-slot width;
- applied carrier width;
- visual-to-slot relationship;
- per-glyph scale;
- optical adjustment;
- stable source bounds;
- motion ownership.

Native transition progress, native duration/interpolators, and target View translation remain outside this layout resolver.

### Home carrier width

Home resolves the live `battery_icon_container` under the bound `MiuiBatteryMeterView` and uses its stable body width as carrier authority.

The full Battery root width is not a safe replacement-width authority because charging presentation can make the root wider than the stable body.

## Represented-slot exclusion

Represented Wi-Fi/mobile/airplane/no-SIM slots may be excluded only through the verified native status-icon contract.

For a temporary native call scope:

1. read the existing ignored-slot set;
2. add only missing Guiyuan-owned entries;
3. let native measure/layout run;
4. remove exactly those entries in `finally`.

For a verified presentation whose native motion consumes the ignored-slot state beyond one measure/layout call, the owned delta may remain for the presentation session and must be restored when that owner ends.

Never clear or replace the platform collection wholesale. Never extend one scene's owned exclusion into an unrelated scene.

## Reversible visual masking

Represented native Views remain attached so they can continue receiving lifecycle, state and tint updates.

For each masked View:

- snapshot the existing `clipBounds`;
- apply only the module-owned empty clip;
- restore only if the live value still matches the module-applied value.

Do not replace this with permanent `GONE`, alpha racing or translation writes.

Visual suppression is not layout ownership.

## End reservation

The Guiyuan render child does not participate in native measurement. A narrow, reversible `MiuiStatusIconContainer.paddingEnd` reservation exposes the compact replacement occupancy to native peer layout.

For steady Home:

- native Battery present: `requestedSlotWidth - actualBatteryWidth`;
- native Battery released: `requestedSlotWidth`.

A negative delta is valid when native charging presentation is wider than the stable replacement carrier. The value must come from live geometry, never a fixed charging offset.

The reservation must:

- remain host-scoped;
- snapshot the pre-session padding;
- react only to bounded state/layout events;
- reject a competing writer;
- restore only the exact module-applied state;
- fall back to native presentation if carrier, width, hide state, or writer ownership is ambiguous.

## QS_FAKE capacity lease

The bounded Control Center bridge may temporarily use otherwise-unused width already available inside the verified QS_FAKE parent.

This is a capacity lease, not motion:

- acquire it only from a verified end-anchored carrier with compatible parent geometry;
- apply it before compact cutover;
- never animate carrier width as gesture motion;
- keep `paddingEnd` as the sole per-progress layout reservation writer;
- release the width lease at the native visible-to-hidden boundary;
- treat an unexpected live width change as a competing writer and fall back to native presentation.

Transition geometry must not interpret leased capacity as carrier displacement.

## Motion ownership

Motion ownership is independent from layout size.

Current production paths either need no independent Guiyuan motion or keep positioning/transition motion under SystemUI. No current scene grants Guiyuan ownership of native motion.

Home inherits native end-side motion from its carrier and must not add a second Battery-translation follower, animator or timing curve.

For Control Center, Guiyuan may read verified native source/target transforms for its own transition-only visual, but it must not write native peer geometry, appearance or motion.

## Logical viewport and transparent overflow

The logical viewport is the replacement slot and remains the basis for steady and transition geometry.

If current top content crosses the logical top edge, Guiyuan may enlarge only its own child by the exact transparent overflow required. The child must be positioned so the logical viewport itself does not move.

Transparent overflow must never become:

- native slot height;
- status-bar height;
- peer padding;
- transition target geometry;
- native motion input.

The same rule applies to the verified Keyguard family render child.

## Top-slot optical avoidance

The Battery-ring opening is driven by the currently visible optical envelope.

- Battery information uses current measured text/charging-glyph bounds.
- Network content uses the current rendered asset or text bounds.
- During semantic cross-fade, previous/current envelopes are scaled by their visible appearance and unioned.
- Once an old semantic reaches zero appearance, it contributes no gap.
- Transparent overflow may reserve draw capacity but must not change the animated ring gap or native occupancy.

## Size and spacing controls

User scale or gap settings must change shared presentation inputs only.

They must not introduce:

- scene-specific geometry hooks;
- charging-specific constants;
- translation compensation;
- duplicate native slot writers.

If a requested size cannot fit a verified host safely, the affected capability should fall back to native presentation rather than conceal the mismatch with a correction chain.

## Prohibited ownership patterns

Do not reintroduce these as current architecture without new target evidence and a fresh writer/lifecycle review:

- a permanent second status participant as the default carrier;
- zero/full-width occupancy handoff between duplicate participants;
- overriding native Battery-hide decisions to preserve module geometry;
- native Battery-descendant translation/visibility as steady Home authority;
- charging-inflated Battery root width as compact visual-width authority;
- fixed pixel correction chains;
- per-frame/pre-draw native translation or pivot races;
- peer alpha/visibility/translation compensation;
- project-owned fake/final appearance thresholds;
- overlay-local screen coordinates as a substitute for the actual native transform chain.

Historical experiments belong in Git history or the project decision record, not in this policy.

## New native geometry writes

Before taking ownership of another native geometry property, verify:

1. the exact SystemUI owner and lifecycle;
2. the current writer set;
3. steady and transition geometry separately;
4. adjacent-icon behavior;
5. relevant Home, panel, Keyguard/AOD and island paths;
6. restoration and failure behavior;
7. host replacement, SystemUI recreation and Hot Reload cleanup;
8. compatibility scope;
9. that taking ownership is safer than leaving the property native-owned.

Reusable evidence under `docs/reference/` may justify an investigation direction, but it does not grant write ownership.
