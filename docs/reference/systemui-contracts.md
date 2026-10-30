# SystemUI Integration Contracts

This is the **public, reusable implementation boundary** for Guiyuan's SystemUI integration. It is deliberately not an exhaustive DEX report, source dump or substitute for verifying the actual platform. Detailed binary indexes, instruction offsets, private device observations and investigation records belong in the maintainer-controlled private `SystemUI-Reference` repository; see `CONTRIBUTING.md` for the access and verification rule. No private repository is an app/build dependency.

## Evidence and compatibility

- The accepted target evidence concerns Xiaomi HyperOS SystemUI `17.03.260226.r`. These conclusions are **not universal** across SystemUI versions, devices, other LSPosed modules or ROM updates.
- Separate **observed binary contracts**, **device-observed runtime behavior**, **derived inferences**, **Guiyuan implementation rules** and **unknowns**. Static call sites do not prove runtime order, callback reentry, competing writers or fault occurrence.
- Revalidate exact binary identity, receiver, call sites and lifetime before relying on a target-specific contract. AOSP is context, not proof of a vendor implementation; unavailable private evidence must be treated as unknown.

## Native host and lifecycle

- Prefer rendering within a **verified native host** over adding a second permanent status participant. Home, Keyguard/AOD and Control Center have distinct host and lifecycle contracts; a retained View is not evidence that its native manager remains registered.
- Give each native mutation one owner and one valid lifetime. A HostSession retains only resources belonging to that host. Validate live host identity, attachment, generation and participant membership before acquiring or transferring an owner.
- During Hot Reload, release old-generation View/callback owners on the main thread before admitting replacement owners; use neutral transferred state, not callbacks captured from an old ClassLoader. If exact host identity cannot be re-established, yield to native.

## Ignored native slots

- Native ignored-slot APIs can operate on **the entire list**. In particular, `setIgnoredSlots` replaces the current list; an old snapshot is not an atomic delta rollback.
- Suppress only the represented slots actually owned by Guiyuan. Preserve pre-existing entries, and restore only Guiyuan's verified additions against the **live** list. A native write failure must not erase unrelated entries introduced by SystemUI or other participants.
- Ignore-list reads and mutations require the real current container, not a similarly named ancestor/peer or resource ID. Unresolved ownership fails native for that surface.

## Padding, reservation and visual masking

- Native Padding is consumed by parent/peer measurement and layout. Treat it as layout ownership, separate from Guiyuan drawing width, optical offset, capacity leasing and transition motion.
- The inspected target Framework `View.setPaddingRelative` updates internal user/resolved Padding state before returning. `ViewGroup` delegates internal Padding updates to `View`, which may request layout synchronously. This is a **write-before-return opportunity**, **not** a verified exception, nested Guiyuan callback or competing same-View mutation.
- Record an owned Padding application only after the setter returns and the live values match; restore only when the live value still matches the session-owned receipt. A foreign or ambiguous write must never be overwritten by an unconditional rollback. Do not add speculative timers, pre-written receipts or retries without path-specific evidence.
- For represented native Views, favor a reversible clip mask: preserve original clip, acquire only the module-owned mask and restore only if it is still owned. Never assume clipping implies capacity or native semantic readiness.

## Native scene, motion and geometry

- Treat native Battery hide, network visibility, status-bar scene and appearance as authoritative **inputs**. Do not rewrite these facts to preserve Guiyuan's old geometry.
- Control Center FAKE and final surfaces are separate native participants. Use QS_FAKE only as verified transition context; the fully expanded final Control Center remains native-owned. AOD is not a Control Center transition source.
- Use verified native source/target geometry and progress to draw Guiyuan transitions. Never invent a progress threshold, double-apply native translation or use leased measurement capacity as animated displacement.
- Native FAKE layout may converge after a posted layout request; inflated Views, attached icon manager and measured geometry are distinct readiness facts. The current two-attempt prearm cap is a **Guiyuan heuristic**, not a native guarantee. Do not change its retry count without same-session failure/ownership evidence.
- A visual morph may follow validated 1:1 or 1:N correspondence without mutating final native icon views. Unknown target identity or geometry must omit that projection or fail native.

## State, color and resource authority

- An existing native Wi-Fi binder's semantic state is more authoritative than a still-visible View or retained Drawable tag. A semantic `Hidden` state must stay hidden even if its ImageView has not yet been cleared.
- For Hot Reload, a uniquely verified, attached native Wi-Fi binding can be read as an observation bridge; do not fabricate binding registrations or transfer an old callback.
- Prefer current native resource identity, light/dark/tint variant and compatible Drawable rendering over screenshot-fitted constants, grayscale/alpha hacks or permanent custom raster outputs.
- Derive visible tint from a genuinely visible native authority where possible, not a represented-but-hidden View or an invented fallback color.

## Fail-native and evidence limits

All ownership, suppression, capacity, visibility and projection changes must restore or yield appropriately on disable, host replacement/detach, SystemUI recreation, scene exit, transition failure and Hot Reload. Restore **only Guiyuan-owned state**; when the owner or host is uncertain, fail native on the smallest affected surface.

These are implementation guardrails for the accepted target, not a promise of tested exceptional setter behavior, cross-device compatibility or support for arbitrary third-party modules. Preserve those limits in future code review and 1.0.0 release qualification.
