# LSPosed 7927 Hot Reload Contract

This reference records the module-generation lifecycle verified from the supplied **LSPosed v2.2.1 (7927)** release binary. It does not establish that an arbitrary installed LSPosed variant has identical bytes or provide a visual handoff API.

## Evidence and scope

The released `LSPosed-v2.2.1-7927-release.zip` reports `versionCode=7927` in `module.prop`. All **41** accompanying SHA-256 sidecars matched after extraction.

| Binary | SHA-256 |
| --- | --- |
| Release ZIP | `d4b90d0f6c447e018c5eb80f81194d29b4dcf066cffa5416ece0a7db35c73753` |
| `framework.dex` | `455155d12173462d51bea31465fe1278fcf91da4ef1d5427dc9beec0c07ccdb4` |
| `daemon.apk` | `10607f6c39a4c0d3a88cba41f3993bd6c4555af4358a2f4958b1de064face45d` |
| ARM64 `libframework.so` | `cece272f75e76e56252ed995414bac3fc5c6c3223cab7016ea3299800839a04c` |

**Observed in the published Java binaries** (JADX 1.5.6; R8-obfuscated class names):

- `framework.dex` `y0.loadModule(w1, long, Bundle)` invokes the reload path `y0.b(w1, w0)`. The old module's `onHotReloading(w0)` is called **before** allocating the new `o1` ClassLoader and invoking the new module's `onHotReloaded(w0)`.
- After successful old retirement, `y0.h=true` freezes old hook mutation; the framework enumerates existing callback stores and exposes transferable hook handles. New-generation `z.replaceHook()` is tied to the new module owner. Old callback stores are cleaned after the new module callback returns.
- `w0.setSavedInstanceState(Object)` compares the **top-level object's** ClassLoader against the old module loader. It does **not** recursively validate the elements of arrays or other containers. The absence of a recursive runtime check does **not** relax API 102's prohibition on retaining old-module objects.
- Refusal, ClassLoader/module-instantiation and new-callback exceptions have distinct failure paths. A new-module callback is not guaranteed to run when an earlier loading stage fails; no per-module visual overlay rollback is offered by this interface.
- `daemon.apk` `j8.b(i8, p9, Bundle, x7)` submits the process reload request using an asynchronous binder transaction (`transact(1, parcel, null, 1)`). Completion is reported via an `IHotReloadCallback` binder interface. This is not a frame-synchronized SystemUI draw/present transaction.

**Not established:** exact corresponding upstream Git revision, exhaustive native ARM64 scheduling internals, a guarantee about the installed device's exact framework binary bytes, or a framework-owned frame-atomic UI transition. The large obfuscated `y0.b` method has JADX inconsistent-decompilation warnings; the normal-path callback and loader ordering above is explicit in the recovered statements and agrees with the observed worker/main-thread reload trace. The native ELF is stripped and was hashed/inspected, **not** completely disassembled.

## Guiyuan ownership implications

- The old module must release module-owned View objects, listeners and other callbacks before approving API 102 retirement; transferring an old-generation `Runnable` inside a neutral `Array` is **not** a safe exception.
- The new module can reconstruct Home presentation only after its own loader and callback run. API 102/7927 gives modules no verified visual-frame-atomic overlap between those owners. Native signal and battery icons may therefore be visible briefly while native presentation is restored.
- A new neutral visual bridge would need an **independent** owner and a proven cleanup on successful reload, refusal, ClassLoader/constructor failure, callback failure and restart. A static retained frame removed *only* by `onHotReloaded` is not Fail native.
- Optimizing the new generation's verified Home critical path may shorten the visual gap, but must preserve authoritative tint, scene, connectivity, subscription, Hook and renderer readiness. Internal old-generation cleanup ordering cannot create a frame overlap across completed main-thread tasks.
- [Issue #519](https://github.com/CHS-Haple/Guiyuan/issues/519) holds measurements and experimental history. The accepted runtime baseline and unmerged experiments are tracked in [CURRENT](../development/CURRENT.md), not in this reference.

## Native frame drawing: exact SystemUI profile

The target `SystemUI 17.03.260226.r` APK (SHA-256 `a0e738e41fe599b97950cbf52a9e2ddc6ae2ceff986efbacb1c9840bea78768d`) was decompiled with JADX 1.5.6 to assess whether a native-owned drawing Hook can replace the old generation's custom Home View:

- `MiuiStatusIconContainer` explicitly calls **`setWillNotDraw(true)`** in its constructor. Its declared `onDraw(Canvas)` only calls the superclass, and the normal Android skip-draw path can bypass that method. It is **not** a usable native drawing Hook as-is.
- `MiuiNotificationStatusContainer`, `MiuiStatusBatteryContainer`, `MiuiPhoneStatusBarView` and `StatusBarWindowView` have **no class-declared `onDraw(Canvas)` or `dispatchDraw(Canvas)` implementation** suitable for a narrowly hooked after-children drawing step. Hooking inherited framework `View.draw` or `ViewGroup.dispatchDraw` would intercept unrelated app/SystemUI View trees and is not acceptable without a separate, proven scope/performance argument.
- `MiuiBatteryMeterIconView.onDraw(Canvas)` exists, but its `onMeasure` fixes icon width to `battery_meter_width`; the Home battery carrier and Wi-Fi/mobile siblings have separate geometry and drawing lifecycles. It is not a complete Home renderer.
- The accepted `SysUiPresentationOwner` hides the native battery and represented Wi-Fi/mobile children with *persistent* `clipBounds=Rect(0,0,0,0)`, native `ignoredSlots` and end-reservation updates. Its `stop()` must restore them. Moving only custom painting into a transferable Hook **does not make these native mutations frame-scoped**. If the new generation never runs, they still need deterministic rollback.

The Android `View.draw` source confirms the normal `onDraw`-then-`dispatchDraw` ordering and its skip-draw branch: https://android.googlesource.com/platform/frameworks/base/+/8360153/core/java/android/view/View.java . This Android source establishes drawing semantics, while the Xiaomi constructors and methods above are verified from the exact target APK. No successful device implementation or atomic frame presentation is claimed.

**Design gate:** before a new runtime branch, identify a *normally invoked, narrowly scoped* native drawing callback that covers the actual combined surface; demonstrate native battery, Wi-Fi and dual-mobile **per-frame** suppression without persistent layout/clip/alpha residue; verify ordinary handoff and rejection/constructor/Hook failure, host replacement, CC and AOD all fall back natively. If such a callback is unavailable, a framework-owned two-phase visual transaction with guaranteed rollback is the remaining coherent direction, not another timer or retained old-ClassLoader callback. See [#519](https://github.com/CHS-Haple/Guiyuan/issues/519#issuecomment-6237650381).
