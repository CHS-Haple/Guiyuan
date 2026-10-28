# Third-Party Notices

Guiyuan uses third-party libraries and build tooling. This file provides a human-readable summary of the project's direct dependencies and does not replace the license text or notices distributed by each upstream project.

The exact resolved dependency graph is defined by Gradle and may include additional transitive dependencies. The in-app third-party screen groups modules by upstream project to avoid duplicate entries; individual module identities and versions remain listed here.

## Runtime / application dependencies

| Component | Version | Purpose | Upstream license |
| --- | ---: | --- | --- |
| libxposed service | 102.0.0 | Modern Xposed service integration | Apache-2.0 |
| AndroidX Core | 1.19.1 | Android compatibility and core APIs | Apache-2.0 |
| AndroidX Activity Compose | 1.13.0 | Android/Compose activity integration | Apache-2.0 |
| AndroidX Navigation Event Compose | 1.2.0 | Predictive/navigation event integration | Apache-2.0 |
| AndroidX DataStore Preferences | 1.3.0 | Local application preferences | Apache-2.0 |
| MIUIX UI / Preference / Icons / Nav / Blur | Published baseline `0.9.4-8972f941-SNAPSHOT` (`8972f941`); OS4 matched-source Canary and Release candidates use PR423 source (`b9f70838`) | Xiaomi HyperOS-style UI; signed OS4 Canary and Release candidates substitute a verified same-revision MIUIX source graph rather than the published baseline binaries | Apache-2.0 |
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

`app/src/main/kotlin/top/yukonga/miuix/kmp/glass/` contains a pinned, adapted subset of the
[MIUIX miuix-glass PR #423](https://github.com/compose-miuix-ui/miuix/pull/423),
at baseline commit `b9f70838509fbc3f63f1a972777a7e6ce07f578f` (Apache-2.0).
`GlassPopupSurface.kt` and `GlassPopupColumn.kt` are now taken unmodified from PR #423
commit `8d52b45f223d4bec9a719d3d6ca52972cc7ea5fb` to use upstream popup
measurement, scroll and outside-gesture ownership. The remaining compatibility
bridge is not claimed to be the latest complete `miuix-glass` module.
The adapted source includes documented compatibility bridges (including title rendering), so it is not a byte-for-byte copy of the entire upstream module. The signed OS4 Canary and Release candidates use the matching PR423 UI/Blur source modules; plain Gradle builds retain published-module compatibility paths.
These primitives are temporary until the official module is published; they must not be silently
updated independently from the corresponding MIUIX UI/Blur contracts. See
`third_party/miuix-glass/README.md` for source boundaries and removal instructions.

## Embedded icon assets

| Component | Variant | Purpose | Upstream license |
| --- | --- | --- | --- |
| Google Material Symbols | Local embedded vectors; no library version | Semantic leading glyphs for companion-app information surfaces | Apache-2.0 |
| MIUIX OS4 glass icon vectors | Exact Regular Back, Search, Close, Filter, Share and More vectors from [PR #423](https://github.com/compose-miuix-ui/miuix/pull/423), `b9f70838509fbc3f63f1a972777a7e6ce07f578f` | Temporary OS4 action-icon compatibility pending a matching `miuix-glass-icons` release | Apache-2.0 |

The selected Material Symbols are stored as local Android vector drawables sourced from Google's `google/material-design-icons` repository. They are app resources, not a runtime library dependency.

The upstream MIUIX OS4 Regular paths are preserved in `Os4SearchIcons.kt`, `Os4BackIcon.kt` and `Os4DiagnosticIcons.kt` with upstream copyright and SPDX; neither `miuix-glass-icons` nor an independently assembled replacement module is an added runtime dependency. Remove these local icon sources when the matching official MIUIX icon artifact can be consumed. The input's clear icon now uses the published native MIUIX `Basic.SearchCleanup`; the local Glass.Close remains in the outside Cancel control.

## Test dependencies

| Component | Version | Purpose | Upstream license |
| --- | ---: | --- | --- |
| JUnit 4 | 4.13.2 | Local unit tests | EPL-1.0 |

## Build tooling

The repository includes the official Gradle Wrapper for Gradle 9.8.2. Gradle 9.8.2 is distributed under Apache-2.0.

Android Gradle Plugin and Kotlin Gradle plugins are resolved through their standard upstream repositories and retain their respective upstream licenses.

## Upstream projects

- libxposed API (browsable source): https://github.com/JingMatrix/libxposed-api
- libxposed service (browsable source): https://github.com/JingMatrix/libxposed-service
- AndroidX: https://github.com/androidx/androidx
- MIUIX: https://github.com/compose-miuix-ui/miuix
- AndroidLiquidGlass: https://github.com/Kyant0/AndroidLiquidGlass (Apache-2.0; source of adapted optics, not a direct runtime dependency)
- Material Symbols: https://github.com/google/material-design-icons
- kotlinx.serialization: https://github.com/Kotlin/kotlinx.serialization
- JUnit 4: https://github.com/junit-team/junit4
- Gradle: https://github.com/gradle/gradle

## Packaging note

The Android build may remove duplicate dependency license resources from packaged `META-INF` entries to avoid resource conflicts. That packaging behavior does not remove or alter the upstream license obligations.

Before a formal release, maintainers should verify the resolved dependency set and any notice requirements applicable to the distributed APK.

## Project license

Guiyuan itself is licensed under the [GNU General Public License v3.0 only](LICENSE) (`GPL-3.0-only`). Third-party components remain governed by their respective upstream licenses and notice requirements.
