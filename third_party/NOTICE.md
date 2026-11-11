# Third-party notices / 第三方授权声明

Guiyuan uses third-party libraries, adapted source and icon assets.
This record identifies their sources and license families; it does not
replace original license texts, copyright headers or required notices.
Resolved dependency versions come from Gradle, not this document.

## Libraries and tooling

- [libxposed API and service](https://github.com/libxposed): Apache-2.0
- [AndroidX](https://github.com/androidx/androidx): Apache-2.0
- [MIUIX](https://github.com/compose-miuix-ui/miuix): Apache-2.0
- [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization):
  Apache-2.0
- [JUnit](https://github.com/junit-team/junit4): EPL-1.0
- [Gradle](https://github.com/gradle/gradle): Apache-2.0
- [Android Gradle Plugin](https://developer.android.com/build): Apache-2.0
- [Kotlin Gradle plugins](https://github.com/JetBrains/kotlin): Apache-2.0

## Included and adapted source

- **MIUIX Glass:** Adapted local source under
  `app/src/main/kotlin/top/yukonga/miuix/kmp/glass/` and
  `app/src/os4Upstream/kotlin/top/yukonga/miuix/kmp/glass/`.
  Source: [MIUIX Glass](https://github.com/compose-miuix-ui/miuix/pull/423).
  Apache-2.0; [license copy](miuix-glass/LICENSE) and
  [source attribution](miuix-glass/README.md).
- **MIUIX background effect:** Local source under
  `app/src/main/kotlin/top/yukonga/miuix/kmp/effect/bg/` is adapted from
  [MIUIX effect](https://github.com/compose-miuix-ui/miuix/pull/321),
  Apache-2.0; [source attribution](miuix-effect/README.md).
- **Liquid navigation optics:** Adapted from the
  [MIUIX Liquid Glass example](https://github.com/compose-miuix-ui/miuix),
  which credits
  [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass).
  Apache-2.0; source attribution is retained.
- **Embedded Material Symbols:**
  [Material Symbols](https://github.com/google/material-design-icons),
  Apache-2.0; original attribution is retained in local vector assets.
  MIUIX Glass icons provided by the dependency retain MIUIX attribution.

Original copyright/SPDX annotations remain in adapted source and vector
assets. Guiyuan itself is [GPL-3.0-only](../LICENSE); that license does
not supersede third-party obligations. Before distribution, verify the
resolved dependencies and preserve any additional upstream NOTICE texts
required for the actually distributed artifacts.

## 简体中文

本文件仅列出第三方组件、改编源码和图标素材的来源及许可证。
实际依赖版本以 Gradle 为准，不在此记录旧 Build、迁移过程或
UI 调整历史。原始版权声明、许可证及适用的 NOTICE 义务必须保留。
