# Third-Party Notices / 第三方声明

Guiyuan uses third-party libraries and build tooling. This file provides a
human-readable summary of the project's direct dependencies and does not replace
the license text or notices distributed by each upstream project.

The exact resolved dependency graph is defined by Gradle and may include
additional transitive dependencies. The in-app third-party screen groups modules
by upstream project to avoid duplicate entries; individual module identities and
versions remain listed here.

## Runtime / application dependencies

| Component | Version | Purpose | Upstream license |
| --- | ---: | --- | --- |
| libxposed service | 102.0.0 | Modern Xposed service integration | Apache-2.0 |
| AndroidX Core | 1.19.1 | Android compatibility and core APIs | Apache-2.0 |
| AndroidX Activity Compose | 1.13.0 | Android/Compose activity integration | Apache-2.0 |
| AndroidX Navigation Event Compose | 1.2.0 | Predictive/navigation event integration | Apache-2.0 |
| AndroidX DataStore Preferences | 1.3.0 | Local application preferences | Apache-2.0 |
| MIUIX UI / Preference / Icons / Glass Icons / Nav / Blur / Material | Stable `main` and currently accepted `dev`: `0.9.4-45645854-SNAPSHOT` (`45645854`); isolated PR #1440 candidate: `0.9.4-ca674992-SNAPSHOT` (`ca674992`) | Xiaomi HyperOS-style UI. The Material module replaces removed `miuix-shader`; released branch and experimental branch remain separately pinned. | Apache-2.0 |
| kotlinx.serialization core / JSON | 1.12.0 | Versioned configuration file serialization | Apache-2.0 |

## Compile-only API

| Component | Version | Purpose | Upstream license |
| --- | ---: | --- | --- |
| libxposed API | 102.0.0 | Framework-provided Xposed interfaces compiled against via `compileOnly`; not packaged as an application implementation | Apache-2.0 |

## Adapted optical code

Guiyuan's liquid navigation includes Apache-2.0 optical helpers adapted from the
pinned [MIUIX Liquid Glass example](https://github.com/compose-miuix-ui/miuix/tree/8972f941c194bd3c4e14c34543d5ebc7b39e40da/example/shared/src/commonMain/kotlin/component/liquid),
which credits AndroidLiquidGlass. Source copyright and license headers are
retained in the adapted files. Neither Backdrop nor Kyant Shapes is a direct
runtime dependency in this experimental implementation.

## Experimental OS4 glass source

`app/src/main/kotlin/top/yukonga/miuix/kmp/glass/` contains a pinned, adapted
subset of the
[MIUIX miuix-glass PR #423](https://github.com/compose-miuix-ui/miuix/pull/423),
at the accepted development checkout revision `45645854cd46c32275c5666e3aafa9980d8f104e`. The isolated PR #1440 evaluates `ca674992c9645395cdb0d5109d66504ff53d018c`, retaining app-local compatibility adapters rather than claiming byte-for-byte identity with upstream `miuix-glass`. The released main branch uses its own pinned source and artifacts (Apache-2.0; [license text](third_party/miuix-glass/LICENSE)).
`GlassPopupSurface.kt` and `GlassPopupColumn.kt` were initially based on PR #423
commit `8d52b45f223d4bec9a719d3d6ca52972cc7ea5fb` to use upstream popup
measurement, scroll and outside-gesture ownership. This is provenance, not a
claim
that those files remain byte-identical to the current upstream revision. The
remaining compatibility
bridge is not claimed to be the latest complete `miuix-glass` module.
The adapted source includes documented compatibility bridges (including title rendering), so it is not a byte-for-byte copy of the entire upstream module. The menu migration in Guiyuan PR #1316 also selectively ports the material-opacity behavior of upstream commit `799d3d09f996d66cb2b1c2318deed8242c4f440e` into app-local `GlassIconButton` and `GlassTransformPopup`; this does not advance the complete matched-source dependency graph. Accepted OS4 source-mode CI checks out the branch-pinned MIUIX source graph; ordinary Gradle builds retain published-module compatibility paths.
These app-local primitives remain until a qualified upstream public API can
preserve their host contracts; they must not be silently
updated independently from the corresponding MIUIX UI/Blur contracts. See
`third_party/miuix-glass/README.md` for source boundaries and removal
instructions.

## Experimental MIUIX background effect source

The `effect.bg` source under
`app/src/main/kotlin/top/yukonga/miuix/kmp/effect/bg/` is pinned to
[MIUIX PR #321](https://github.com/compose-miuix-ui/miuix/pull/321)
at `73bdd49b4ea965dc7822e276b0a6ce29a5564f54` (Apache-2.0). Original copyrights
are retained.
`GyBgEffect` is Guiyuan's thin About/Onboarding adapter; the single local
upstream-node change handles theme/device/state-driven color-job refresh.
See [source provenance](third_party/miuix-effect/README.md).
This is a local source snapshot, not a published `miuix-effect` dependency.

## Embedded icon assets

| Component | Variant | Purpose | Upstream license |
| --- | --- | --- | --- |
| Google Material Symbols | Local embedded vectors; no library version | Semantic leading glyphs for companion-app information surfaces | Apache-2.0 |
| MIUIX OS4 Glass icons | Official `MiuixIcons.Glass.Regular` from the current same-revision `miuix-glass-icons-android` artifact (accepted `45645854`; PR #1440 candidate `ca674992`) | Native icon aliases for OS4 navigation, menus and diagnostics; the OS3 UI style uses standard MIUIX icons where applicable | Apache-2.0 |

The selected Material Symbols are stored as local Android vector drawables
sourced from Google's `google/material-design-icons` repository. They are app
resources, not a runtime library dependency.

Both OS4 source variants now share `app/src/main/kotlin/com/chaners/guiyuan/ui/icons/Os4GlassIcons.kt`, which aliases the published MIUIX Glass Regular vectors for Back, Search, Close, Refresh, Filter, More, Edit, Copy, Delete and Share. The previous six local `os4Published` vector copies and the separate upstream alias were removed by #1370. Diagnostics export uses `Glass.Regular.Share` in the OS4 UI style and ordinary `MiuixIcons.Share` in OS3; the input clear control continues to use MIUIX `Basic.SearchCleanup`, while Glass.Close is used for outside Cancel. This module does not import or publish the entire upstream `miuix-glass` runtime implementation.

## Test dependencies

| Component | Version | Purpose | Upstream license |
| --- | ---: | --- | --- |
| JUnit 4 | 4.13.2 | Local unit tests | EPL-1.0 |

## Build tooling

The repository includes the official Gradle Wrapper with the verified Gradle
9.9.0 binary distribution. Gradle 9.9.0 is distributed under Apache-2.0.

Android Gradle Plugin and Kotlin Gradle plugins are resolved through their
standard upstream repositories and retain their respective upstream licenses.

## Upstream projects

- libxposed API (published upstream):
  [libxposed/api](https://github.com/libxposed/api)
- libxposed service (published upstream):
  [libxposed/service](https://github.com/libxposed/service)
- AndroidX: [androidx/androidx](https://github.com/androidx/androidx)
- MIUIX: [compose-miuix-ui/miuix](https://github.com/compose-miuix-ui/miuix)
- AndroidLiquidGlass:
  [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)
  (Apache-2.0; source of adapted optics, not a direct runtime dependency)
- Material Symbols:
  [google/material-design-icons](https://github.com/google/material-design-icons)
- kotlinx.serialization:
  [Kotlin/kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)
- JUnit 4: [junit-team/junit4](https://github.com/junit-team/junit4)
- Gradle: [gradle/gradle](https://github.com/gradle/gradle)

## Packaging note

The Android build may remove duplicate dependency license resources from
packaged `META-INF` entries to avoid resource conflicts. That packaging behavior
does not remove or alter the upstream license obligations.

Before each release, maintainers should verify the resolved dependency set and
applicable license/notice requirements of the distributed APK.

## Project license

Guiyuan itself is licensed under the
[GNU General Public License v3.0 only](LICENSE) (`GPL-3.0-only`). Third-party
components remain governed by their respective upstream licenses and notice
requirements.

---

## 简体中文

本文件列出归元使用的主要第三方依赖、适配的源代码及图标素材，并说明其来源和许可证。具体组件、版本与上游许可证以本页上方的英文清单为准；实际解析的依赖还可能包括间接依赖。

- **应用与编译依赖**：包括 Modern Xposed、AndroidX、MIUIX、Kotlin 序列化及
  JUnit。各自的版本和许可证见上方对应表格。
- **MIUIX 适配**：部分界面和液态玻璃相关代码基于注明版本的上游项目进行适配，保留相关版权和许可证声明；这些代码不等于完整上游模块。
- **图标素材**：使用注明来源的 Material Symbols 及 MIUIX 图标素材，原始许可仍然适用。
- **构建工具**：Gradle、Android Gradle Plugin 与 Kotlin 构建工具遵循各自上游许可。

构建过程中可能为避免冲突而移除重复的 `META-INF` 许可资源，但不改变第三方许可义务。每次发布前仍需核对实际依赖及应随附的许可证和声明。

归元自身源代码采用 [GPL-3.0-only](LICENSE) 许可证；第三方组件分别受其上游许可证和声明要求约束。
