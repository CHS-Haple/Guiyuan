# Third-Party Notices

Guiyuan uses third-party libraries and build tooling. This file provides a human-readable summary of the project's direct dependencies and does not replace the license text or notices distributed by each upstream project.

The exact resolved dependency graph is defined by Gradle and may include additional transitive dependencies.

## Runtime / application dependencies

| Component | Version | Purpose | Upstream license |
| --- | ---: | --- | --- |
| libxposed API | 102.0.0 | Modern Xposed API surface | Apache-2.0 |
| libxposed service | 102.0.0 | Modern Xposed service integration | Apache-2.0 |
| AndroidX Activity Compose | 1.13.0 | Android/Compose activity integration | Apache-2.0 |
| AndroidX Navigation Event Compose | 1.1.2 | Predictive/navigation event integration | Apache-2.0 |
| AndroidX DataStore Preferences | 1.2.1 | Local application preferences | Apache-2.0 |
| MIUIX UI / Preference / Icons / Nav / Blur | 0.9.4-0657575a-SNAPSHOT (`0657575a0259f89b3863166ec2cef4223ea01156`) | Xiaomi HyperOS-style companion-app UI; published snapshot from the MIUIX main branch | Apache-2.0 |
| Backdrop | 2.0.1 | Liquid Glass backdrop, blur and refraction effects for the companion-app navigation bar | Apache-2.0 |
| Kyant Shapes | 1.2.1 | Capsule geometry used by the Liquid Glass navigation bar | Apache-2.0 |
| kotlinx.serialization core | 1.11.0 | Kotlin serialization support | Apache-2.0 |

## Embedded icon assets

| Component | Variant | Purpose | Upstream license |
| --- | --- | --- | --- |
| Google Material Symbols | Local embedded vectors; no library version | Semantic leading glyphs for companion-app information surfaces | Apache-2.0 |

The selected Material Symbols are stored as local Android vector drawables sourced from Google's `google/material-design-icons` repository. They are app resources, not a runtime library dependency.

## Test dependencies

| Component | Version | Purpose | Upstream license |
| --- | ---: | --- | --- |
| JUnit 4 | 4.13.2 | Local unit tests | EPL-1.0 |

## Build tooling

The repository includes the official Gradle Wrapper for Gradle 9.8.0. Gradle 9.8.0 is distributed under Apache-2.0.

Android Gradle Plugin and Kotlin Gradle plugins are resolved through their standard upstream repositories and retain their respective upstream licenses.

## Upstream projects

- libxposed API: https://github.com/libxposed/api
- libxposed service: https://github.com/libxposed/service
- AndroidX: https://github.com/androidx/androidx
- MIUIX: https://github.com/compose-miuix-ui/miuix
- Backdrop: https://github.com/Kyant0/AndroidLiquidGlass
- Kyant Shapes: https://github.com/Kyant0/Shapes
- Material Symbols: https://github.com/google/material-design-icons
- kotlinx.serialization: https://github.com/Kotlin/kotlinx.serialization
- JUnit 4: https://github.com/junit-team/junit4
- Gradle: https://github.com/gradle/gradle

## Packaging note

The Android build may remove duplicate dependency license resources from packaged `META-INF` entries to avoid resource conflicts. That packaging behavior does not remove or alter the upstream license obligations.

Before a formal release, maintainers should verify the resolved dependency set and any notice requirements applicable to the distributed APK.

## Project license

Guiyuan itself is licensed under the [GNU General Public License v3.0 only](LICENSE) (`GPL-3.0-only`). Third-party components remain governed by their respective upstream licenses and notice requirements.
