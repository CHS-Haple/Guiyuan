# LSPosed 7936 Compatibility Evidence (pre-0.9.2)

This note records the user-supplied `LSPosed-v2.2.1-7936-release.zip` as **candidate framework evidence**, not a recommendation to install it, a Guiyuan runtime change, or verified publisher provenance. The accepted target SystemUI is still the pinned `17.03.260226.r`; see [SystemUI contracts](systemui-contracts.md). For the accepted 7927/7930 generation handoff evidence and limits, see [7927 API 102](lsposed-7927-hot-reload.md) and [7930 comparison](lsposed-7930-compatibility.md).

## Verified inputs and archive integrity

Inspected the exact uploaded 7936 archive against the previously uploaded 7927 and 7930 release archives. Its ZIP SHA-256 is `7d75048c8ce40df37f1aeb4e11cc1a865c31887e75775d5ba8ef7eb94c4371b3`, and `module.prop` declares `version=v2.2.1 (7936)` and `versionCode=7936`. Its Android 9–17 declaration is a manifest claim, not a successful installation test.

All 82 non-directory ZIP entries were decoded (including method 95, XZ), their uncompressed lengths and ZIP CRC32 values were checked, and all 41 bundled SHA-256 sidecars matched their corresponding bytes. This establishes **archive internal integrity**, not an independently authenticated signature or the bytes of the installed framework.

| Supplied payload | 7930 bytes / SHA-256 | 7936 bytes / SHA-256 |
| --- | --- | --- |
| `framework.dex` | 236,600 / `e84ca1e7e35f7f268c50ff49b1beaff162b0475b9c12f50fa3654bf85b625269` | 237,532 / `b2014f38bfeff962da3e0fd04356145af49ce7cbf444d3e29b0217706859f4be` |
| `daemon.apk` | 4,676,025 / `3eca20d99b521c222b85f2272fab493052d800c78e3afebac729bfcbc9c9f7ee` | 4,979,513 / `a71bd9c0dd59152ca6fd58d12f97c1088b7bff6030c003f1e54c62bea26e7e62` |
| `lib/arm64-v8a/libframework.so` | 2,013,088 / `967e368f068ac74e75020a742f43f71503d2e2ef66f9cfef451ad1e7c7580957` | 2,121,312 / `ce3e82f0cdbd6ec18b52c3c01d74f28c64e8fdff77eadf69679fce7a13d1c9ec` |

The archive contains the same 82 entry names as 7930, but changed payloads include `framework.dex`, `daemon.apk`, `manager.apk`, all four architectures' `libframework.so` / `libpreload.so` and `dex2oat`, plus associated hashes. **7936 is not a version-only rebuild.**

## Java framework bytecode (original baksmali 3.0.10 + JADX 1.5.6)

Independently disassembled the verified 7930 and 7936 `framework.dex` payloads with `baksmali-3.0.10-fat-release.jar` and inspected the modified class-name parser with JADX 1.5.6. Both DEX files yield **181 matching class paths**, without added or removed classes. Many `.source` map IDs and R8-generated encoded-string index constants differ; these are not by themselves evidence of different hook/lifecycle semantics. The substantive Java changes include the array/class-name parsing helper `s` (new regex-based array-suffix validation (bytecode pattern `(?:\\[\\])+`)), associated map-field reference shifts in `o2`, `q`, `u1`, and changes to `s0`'s string handling. Do not assume all class-name input and exception paths have identical semantics.

For `y0.smali` (the module loader and Hot Reload callback owner), the control-flow and instructions remain identical after excluding R8 string-index constants and the expected `getFrameworkVersionCode()` value change from `0x1efa` (7930) to `0x1f00` (7936). Both invoke `XposedModuleInterface.onHotReloading` at the same disassembly line and `onHotReloaded` at the same line, with no structural change to the surrounding Java callback and class-loader handoff. Thus the **7930/7936 Java Hot Reload control path** is consistent with the accepted previous generation ordering; this is narrower than a claim of whole-framework runtime equivalence.

## Native and device evidence limits

The supplied ARM64 `libframework.so` is genuinely changed and stripped. The ELF Build IDs are `06c22587f30aa9d3d042e9c44b5899ffb5f3e279` (7930) and `e7e9fdc469fb9f109e9a79e3ebf9ddb38e6a2eb3` (7936). Both declare the same five dynamic dependencies (`libz`, `liblog`, `libm`, `libdl`, `libc`) and Android 28 minimum native target. **Matching loader call sites and dependencies do not verify** native hook engine behavior, concurrency, old-generation cleanup or Zygisk injection timing. The archive cannot prove the exact installed bytes; no 7936 Guiyuan device log or controlled reboot/hot-reload acceptance is available from this inspection.

## Decision and next gate

**Defer any Guiyuan source adjustment or assertion of 7936 compatibility.** Guiyuan remains on the existing pinned API 102/target-SystemUI contracts; no Hook, state writer, receiver, listener, timer, retry, build channel or version change is justified by the static differences alone. If the maintainer intentionally updates the device's framework to 7936, collect a framework-version-tagged General/Detailed diagnostic on (1) clean SystemUI startup, (2) ordinary V10 Hot Reload and repeated reload, (3) changing Wi-Fi/mobile-data SIM/charging during reload, (4) Home–Control Center–Keyguard/AOD return and native fallback, and (5) SystemUI restart/late-load. Treat failures as real defects only when backed by matching session, owner and native/source evidence, not version correlation alone. Exceptional failure injection and all other OEM frameworks remain unverified; any necessary behavioral fix needs a separate scoped `fix/*` PR and applicable device acceptance before stable promotion.

**Verification boundary:** this documentation-only evidence update does not justify a new APK/Build, IT build, release promotion or SystemUI decompilation beyond the separately verified pinned target.
