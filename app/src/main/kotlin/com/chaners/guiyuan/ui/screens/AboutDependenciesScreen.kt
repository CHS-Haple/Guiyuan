package com.chaners.guiyuan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.BuildConfig
import com.chaners.guiyuan.R
import top.yukonga.miuix.kmp.preference.ArrowPreference

@Composable
internal fun AboutThirdPartyScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val miuixVersion = BuildConfig.MIUIX_VERSION
    val runtimeDependencies = remember(miuixVersion) { aboutRuntimeDependencies(miuixVersion) }
    val compileTimeDependencies = remember { aboutCompileTimeDependencies() }
    val developmentDependencies = remember { aboutDevelopmentDependencies() }

    SettingsPage(
        title = stringResource(R.string.about_third_party_title),
        onBack = onBack,
    ) {
        Section(R.string.section_runtime_dependencies) {
            runtimeDependencies.forEach { dependency ->
                AboutDependencyPreference(
                    dependency = dependency,
                    onClick = { uriHandler.openUri(dependency.upstreamUrl) },
                )
            }
        }
        Section(R.string.section_compile_time_dependencies) {
            compileTimeDependencies.forEach { dependency ->
                AboutDependencyPreference(
                    dependency = dependency,
                    onClick = { uriHandler.openUri(dependency.upstreamUrl) },
                )
            }
        }
        Section(R.string.section_embedded_assets) {
            ArrowPreference(
                title = "Material Symbols",
                summary = stringResource(R.string.about_embedded_asset_summary),
                onClick = {
                    uriHandler.openUri("https://github.com/google/material-design-icons")
                },
            )
        }
        Section(R.string.section_development_dependencies) {
            developmentDependencies.forEach { dependency ->
                AboutDependencyPreference(
                    dependency = dependency,
                    onClick = { uriHandler.openUri(dependency.upstreamUrl) },
                )
            }
        }
    }
}

@Composable
private fun AboutDependencyPreference(
    dependency: AboutDependency,
    onClick: () -> Unit,
) {
    val summary =
        buildString {
            dependency.version?.let {
                append(it)
                append('\n')
            }
            append(dependency.maintainer)
            append('｜')
            append(dependency.license)
        }
    ArrowPreference(
        title = dependency.name,
        summary = summary,
        onClick = onClick,
    )
}
