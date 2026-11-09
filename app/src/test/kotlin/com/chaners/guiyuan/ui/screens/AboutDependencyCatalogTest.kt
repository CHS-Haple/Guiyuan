package com.chaners.guiyuan.ui.screens

import com.chaners.guiyuan.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AboutDependencyCatalogTest {
    @Test
    fun sectionsRetainIndependentDependencyOrderAndBuildVersions() {
        val runtime = aboutRuntimeDependencies("miuix-review-version")
        assertEquals(
            listOf(
                "libxposed service",
                "MIUIX",
                "AndroidX Core",
                "AndroidX Activity Compose",
                "AndroidX Navigation Event Compose",
                "AndroidX DataStore Preferences",
                "kotlinx.serialization",
            ),
            runtime.map { it.name },
        )
        assertEquals("miuix-review-version", runtime[1].version)
        assertEquals(BuildConfig.LIBXPOSED_VERSION, runtime.first().version)
        assertEquals(listOf("libxposed API"), aboutCompileTimeDependencies().map { it.name })
        assertEquals(
            listOf("Gradle Wrapper", "JUnit 4"),
            aboutDevelopmentDependencies().map { it.name },
        )
    }

    @Test
    fun disclosedDependenciesHaveSourceLinksAndDeclaredLicense() {
        val dependencies = aboutRuntimeDependencies(BuildConfig.MIUIX_VERSION) +
            aboutCompileTimeDependencies() + aboutDevelopmentDependencies()
        assertEquals(10, dependencies.size)
        assertTrue(dependencies.all { it.upstreamUrl.startsWith("https://github.com/") })
        assertTrue(dependencies.all { it.name.isNotBlank() && it.maintainer.isNotBlank() })
        assertTrue(dependencies.all { it.license.isNotBlank() && !it.version.isNullOrBlank() })
        assertEquals("EPL-1.0", dependencies.last().license)
        assertTrue(dependencies.dropLast(1).all { it.license == "Apache-2.0" })
    }
}
