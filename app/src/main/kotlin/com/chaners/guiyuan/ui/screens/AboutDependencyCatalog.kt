package com.chaners.guiyuan.ui.screens

import com.chaners.guiyuan.BuildConfig

// Keep the distributed license/attribution roster independent of Compose UI rendering.
private const val APACHE_LICENSE_ID = "Apache-2.0"
private const val ANDROIDX_MAINTAINER = "AndroidX"
private const val ANDROIDX_UPSTREAM_URL = "https://github.com/androidx/androidx"

internal data class AboutDependency(
    val name: String,
    val version: String?,
    val license: String,
    val maintainer: String,
    val upstreamUrl: String,
)

internal fun aboutRuntimeDependencies(miuixVersion: String): List<AboutDependency> =
    listOf(
        AboutDependency(
            name = "libxposed service",
            version = BuildConfig.LIBXPOSED_VERSION,
            license = APACHE_LICENSE_ID,
            maintainer = "libxposed",
            upstreamUrl = "https://github.com/libxposed/service",
        ),
        AboutDependency(
            name = "MIUIX",
            version = miuixVersion,
            license = APACHE_LICENSE_ID,
            maintainer = "compose-miuix-ui",
            upstreamUrl = "https://github.com/compose-miuix-ui/miuix",
        ),
        AboutDependency(
            name = "AndroidX Core",
            version = BuildConfig.ANDROIDX_CORE_VERSION,
            license = APACHE_LICENSE_ID,
            maintainer = ANDROIDX_MAINTAINER,
            upstreamUrl = ANDROIDX_UPSTREAM_URL,
        ),
        AboutDependency(
            name = "AndroidX Activity Compose",
            version = BuildConfig.ACTIVITY_COMPOSE_VERSION,
            license = APACHE_LICENSE_ID,
            maintainer = ANDROIDX_MAINTAINER,
            upstreamUrl = ANDROIDX_UPSTREAM_URL,
        ),
        AboutDependency(
            name = "AndroidX Navigation Event Compose",
            version = BuildConfig.NAVIGATION_EVENT_COMPOSE_VERSION,
            license = APACHE_LICENSE_ID,
            maintainer = ANDROIDX_MAINTAINER,
            upstreamUrl = ANDROIDX_UPSTREAM_URL,
        ),
        AboutDependency(
            name = "AndroidX DataStore Preferences",
            version = BuildConfig.DATASTORE_PREFERENCES_VERSION,
            license = APACHE_LICENSE_ID,
            maintainer = ANDROIDX_MAINTAINER,
            upstreamUrl = ANDROIDX_UPSTREAM_URL,
        ),
        AboutDependency(
            name = "kotlinx.serialization",
            version = BuildConfig.KOTLINX_SERIALIZATION_CORE_VERSION,
            license = APACHE_LICENSE_ID,
            maintainer = "Kotlin",
            upstreamUrl = "https://github.com/Kotlin/kotlinx.serialization",
        ),
    )

internal fun aboutCompileTimeDependencies(): List<AboutDependency> =
    listOf(
        AboutDependency(
            name = "libxposed API",
            version = BuildConfig.LIBXPOSED_VERSION,
            license = APACHE_LICENSE_ID,
            maintainer = "libxposed",
            upstreamUrl = "https://github.com/libxposed/api",
        ),
    )

internal fun aboutDevelopmentDependencies(): List<AboutDependency> =
    listOf(
        AboutDependency(
            name = "Gradle Wrapper",
            version = BuildConfig.GRADLE_VERSION,
            license = APACHE_LICENSE_ID,
            maintainer = "Gradle",
            upstreamUrl = "https://github.com/gradle/gradle",
        ),
        AboutDependency(
            name = "JUnit 4",
            version = BuildConfig.JUNIT_VERSION,
            license = "EPL-1.0",
            maintainer = "junit-team",
            upstreamUrl = "https://github.com/junit-team/junit4",
        ),
    )
