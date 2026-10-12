package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.BuildConfig
import com.chaners.guiyuan.GyApplication
import com.chaners.guiyuan.R
import com.chaners.guiyuan.system.RuntimeEnv
import com.chaners.guiyuan.system.XposedStatus
import com.chaners.guiyuan.ui.screens.about.BgEffectBackground
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val ABOUT_PROJECT_URL = "https://github.com/CHS-Haple/Guiyuan"
private const val ABOUT_LICENSE_URL = "https://github.com/CHS-Haple/Guiyuan/blob/main/LICENSE"
private const val GPL_LICENSE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"
private const val ABOUT_VALUE_SEPARATOR = "｜"

private data class AboutDependency(
    val name: String,
    val version: String?,
    val license: String,
    val upstreamUrl: String,
)

@Composable
internal fun AboutScreen(
    onBack: () -> Unit,
    onOpenThirdParty: () -> Unit,
    onOpenScopes: () -> Unit,
) {
    val context = LocalContext.current
    val app = remember(context.applicationContext) {
        context.applicationContext as GyApplication
    }
    val xposedStatus by app.xposedStatus.collectAsState()
    val uriHandler = LocalUriHandler.current
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val backdrop = rememberLayerBackdrop()
    val blur = remember { isRuntimeShaderSupported() }
    val isDark = MiuixTheme.colorScheme.surface.luminance() < 0.5f
    val logoBlend = remember(isDark) {
        if (isDark) {
            listOf(
                BlendColorEntry(Color(0xe6a1a1a1), BlurBlendMode.ColorDodge),
                BlendColorEntry(Color(0x4de6e6e6), BlurBlendMode.LinearLight),
                BlendColorEntry(Color(0xff1af500), BlurBlendMode.Lab),
            )
        } else {
            listOf(
                BlendColorEntry(Color(0xcc4a4a4a), BlurBlendMode.ColorBurn),
                BlendColorEntry(Color(0xff4f4f4f), BlurBlendMode.LinearLight),
                BlendColorEntry(Color(0xff1af200), BlurBlendMode.Lab),
            )
        }
    }
    var heroHeight by remember { mutableStateOf(200.dp) }
    var spacerBottom by remember { mutableFloatStateOf(0f) }
    var iconBottom by remember { mutableFloatStateOf(0f) }
    var nameBottom by remember { mutableFloatStateOf(0f) }
    var versionBottom by remember { mutableFloatStateOf(0f) }
    val spacerHeight = heroHeight + 218.dp
    // The content may exceed one screen; use a minimum, not a fixed parent height.
    val contentMinHeight by remember(density, listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            val available = (
                info.viewportSize.height - info.beforeContentPadding - info.afterContentPadding
            ).coerceAtLeast(0)
            with(density) { available.toDp() }
        }
    }
    val scrollProgress by remember(density) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset /
                with(density) { (heroHeight + 218.dp).toPx() }).coerceIn(0f, 1f)
        }
    }
    val visible by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val collapse by remember {
        derivedStateOf {
            val offset = listState.firstVisibleItemScrollOffset.toFloat()
            if (listState.firstVisibleItemIndex > 0) {
                Triple(1f, 1f, 1f)
            } else if (spacerBottom <= versionBottom ||
                versionBottom <= nameBottom || nameBottom <= iconBottom
            ) {
                Triple(0f, 0f, 0f)
            } else {
                val first = spacerBottom - versionBottom
                val second = versionBottom - nameBottom
                val third = nameBottom - iconBottom
                Triple(
                    ((offset - first * 0.5f) / (first * 0.5f)).coerceIn(0f, 1f),
                    ((offset - first) / second).coerceIn(0f, 1f),
                    ((offset - first - second) / third).coerceIn(0f, 1f),
                )
            }
        }
    }
    var showRepoUnavailable by rememberSaveable { mutableStateOf(false) }
    val env by
        produceState(
            initialValue = RuntimeEnv.basic(),
            key1 = context.applicationContext,
        ) {
            value = RuntimeEnv.resolve(context.applicationContext)
        }

    SettingsPage(
        title = stringResource(R.string.about_title),
        onBack = onBack,
        listState = listState,
        titleAlpha = ((scrollProgress - 0.35f) / 0.65f).coerceIn(0f, 1f),
        background = {
            BgEffectBackground(
                dynamicBackground = visible,
                modifier = Modifier.fillMaxSize(),
                bgModifier = if (blur) Modifier.layerBackdrop(backdrop) else Modifier,
                isFullSize = true,
                alpha = { 1f - scrollProgress },
            ) {}
        },
        foreground = { padding ->
            AboutHero(
                topPadding = padding.calculateTopPadding(),
                active = visible,
                backdrop = if (blur) backdrop else null,
                logoBlend = logoBlend,
                versionProgress = collapse.first,
                nameProgress = collapse.second,
                iconProgress = collapse.third,
                onHeight = { if (heroHeight != it) heroHeight = it },
                onIconBottom = {
                    if (listState.firstVisibleItemIndex == 0 &&
                        listState.firstVisibleItemScrollOffset == 0 && iconBottom != it
                    ) iconBottom = it
                },
                onNameBottom = {
                    if (listState.firstVisibleItemIndex == 0 &&
                        listState.firstVisibleItemScrollOffset == 0 && nameBottom != it
                    ) nameBottom = it
                },
                onVersionBottom = {
                    if (listState.firstVisibleItemIndex == 0 &&
                        listState.firstVisibleItemScrollOffset == 0 && versionBottom != it
                    ) versionBottom = it
                },
            )
        },
        overlay = {
            OverlayDialog(
                title = stringResource(R.string.about_repo_unavailable_title),
                summary = stringResource(R.string.about_repo_unavailable_summary),
                show = showRepoUnavailable,
                onDismissRequest = { showRepoUnavailable = false },
            ) {
                TextButton(
                    text = stringResource(R.string.confirm),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = { showRepoUnavailable = false },
                )
            }
        },
    ) {
        item(key = "logoSpacer") {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(spacerHeight)
                    .onGloballyPositioned { coords ->
                        if (listState.firstVisibleItemIndex == 0 &&
                            listState.firstVisibleItemScrollOffset == 0
                        ) {
                            val bottom = coords.positionInWindow().y + coords.size.height
                            if (spacerBottom != bottom) spacerBottom = bottom
                        }
                    },
            )
        }
        item(key = "aboutContent") {
            Column(Modifier.fillMaxWidth().heightIn(min = contentMinHeight)) {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    ArrowPreference(
                        title = stringResource(R.string.about_project_home_title),
                        summary = stringResource(R.string.about_project_home_summary),
                        startAction = {
                            SemanticLeadingIcon(
                                iconRes = R.drawable.ic_material_symbol_folder_code,
                            )
                        },
                        onClick = {
                            if (BuildConfig.REPO_LINK_ENABLED) {
                                uriHandler.openUri(ABOUT_PROJECT_URL)
                            } else {
                                showRepoUnavailable = true
                            }
                        },
                    )
                    ArrowPreference(
                        title = stringResource(R.string.about_open_source_license_title),
                        summary = stringResource(R.string.about_open_source_license_summary),
                        startAction = {
                            SemanticLeadingIcon(
                                iconRes = R.drawable.ic_material_symbol_license,
                            )
                        },
                        onClick = {
                            uriHandler.openUri(
                                if (BuildConfig.REPO_LINK_ENABLED) ABOUT_LICENSE_URL else GPL_LICENSE_URL,
                            )
                        },
                    )
                    ArrowPreference(
                        title = stringResource(R.string.about_third_party_title),
                        summary = stringResource(R.string.about_third_party_summary),
                        startAction = {
                            SemanticLeadingIcon(
                                iconRes = R.drawable.ic_material_symbol_account_tree,
                            )
                        },
                        onClick = onOpenThirdParty,
                    )
                }
                SmallTitle(stringResource(R.string.section_device_system))
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    val unavailable = stringResource(R.string.about_value_unavailable)
                    val deviceSummary =
                        listOf(
                            env.device.trim(),
                            env.model
                                .trim()
                                .takeIf { model ->
                                    model.isNotBlank() &&
                                        !model.equals(env.device.trim(), ignoreCase = true)
                                },
                        )
                            .filterNotNull()
                            .filter(String::isNotBlank)
                            .ifEmpty { listOf(unavailable) }
                            .joinToString(separator = ABOUT_VALUE_SEPARATOR)
                    val androidSummary =
                        if (env.androidVersion.isNotBlank()) {
                            buildString {
                                append("Android ")
                                append(env.androidVersion)
                                append(ABOUT_VALUE_SEPARATOR)
                                append("API ")
                                append(env.sdk)
                            }
                        } else {
                            unavailable
                        }

                    BasicComponent(
                        title = stringResource(R.string.device_name_label),
                        summary = deviceSummary,
                        startAction = {
                            SemanticLeadingIcon(
                                iconRes = R.drawable.ic_material_symbol_smartphone,
                            )
                        },
                    )
                    BasicComponent(
                        title = stringResource(R.string.android_version_label),
                        summary = androidSummary,
                        startAction = {
                            SemanticLeadingIcon(
                                iconRes = R.drawable.ic_material_symbol_android,
                            )
                        },
                    )
                    BasicComponent(
                        title = stringResource(R.string.os_version_label),
                        summary = env.osVersion.ifBlank { unavailable },
                        startAction = {
                            SemanticLeadingIcon(
                                iconRes = R.drawable.ic_material_symbol_layers,
                            )
                        },
                    )
                    val serviceSummary = when (val status = xposedStatus) {
                        is XposedStatus.Connected ->
                            stringResource(R.string.runtime_service_connected, status.apiVersion)
                        XposedStatus.Checking -> stringResource(R.string.runtime_service_checking)
                        XposedStatus.FrameworkUnavailable -> stringResource(R.string.runtime_service_disconnected)
                        XposedStatus.QueryUnavailable -> stringResource(R.string.runtime_service_query_unavailable)
                    }
                    ArrowPreference(
                        title = stringResource(R.string.runtime_scope_title),
                        summary = stringResource(R.string.static_scope_entry_summary),
                        startAction = {
                            SemanticLeadingIcon(
                                iconRes = R.drawable.ic_material_symbol_target,
                            )
                        },
                        onClick = onOpenScopes,
                    )
                }
            }
        }
    }
}

@Composable
internal fun AboutThirdPartyScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val miuixVersion = BuildConfig.MIUIX_VERSION
    val runtimeDependencies =
        remember(miuixVersion) {
            listOf(
                AboutDependency(
                    name = "MIUIX",
                    version = miuixVersion,
                    license = "Apache-2.0",
                    upstreamUrl = "https://github.com/compose-miuix-ui/miuix",
                ),
                AboutDependency(
                    name = "AndroidLiquidGlass (Backdrop)",
                    version = BuildConfig.BACKDROP_VERSION,
                    license = "Apache-2.0",
                    upstreamUrl = "https://github.com/Kyant0/AndroidLiquidGlass",
                ),
                AboutDependency(
                    name = "Kyant Shapes",
                    version = BuildConfig.SHAPES_VERSION,
                    license = "Apache-2.0",
                    upstreamUrl = "https://github.com/Kyant0/Shapes",
                ),
                AboutDependency(
                    name = "libxposed API",
                    version = BuildConfig.LIBXPOSED_VERSION,
                    license = "Apache-2.0",
                    upstreamUrl = "https://github.com/libxposed/api",
                ),
                AboutDependency(
                    name = "libxposed service",
                    version = BuildConfig.LIBXPOSED_VERSION,
                    license = "Apache-2.0",
                    upstreamUrl = "https://github.com/libxposed/service",
                ),
                AboutDependency(
                    name = "AndroidX Activity Compose",
                    version = BuildConfig.ACTIVITY_COMPOSE_VERSION,
                    license = "Apache-2.0",
                    upstreamUrl = "https://github.com/androidx/androidx",
                ),
                AboutDependency(
                    name = "AndroidX Navigation Event Compose",
                    version = BuildConfig.NAVIGATION_EVENT_COMPOSE_VERSION,
                    license = "Apache-2.0",
                    upstreamUrl = "https://github.com/androidx/androidx",
                ),
                AboutDependency(
                    name = "AndroidX DataStore Preferences",
                    version = BuildConfig.DATASTORE_PREFERENCES_VERSION,
                    license = "Apache-2.0",
                    upstreamUrl = "https://github.com/androidx/androidx",
                ),
                AboutDependency(
                    name = "kotlinx.serialization core",
                    version = BuildConfig.KOTLINX_SERIALIZATION_CORE_VERSION,
                    license = "Apache-2.0",
                    upstreamUrl = "https://github.com/Kotlin/kotlinx.serialization",
                ),
            )
        }
    val developmentDependencies =
        remember {
            listOf(
                AboutDependency(
                    name = "Gradle Wrapper",
                    version = BuildConfig.GRADLE_VERSION,
                    license = "Apache-2.0",
                    upstreamUrl = "https://github.com/gradle/gradle",
                ),
                AboutDependency(
                    name = "JUnit 4",
                    version = BuildConfig.JUNIT_VERSION,
                    license = "EPL-1.0",
                    upstreamUrl = "https://github.com/junit-team/junit4",
                ),
            )
        }

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
            append(dependency.license)
        }
    ArrowPreference(
        title = dependency.name,
        summary = summary,
        onClick = onClick,
    )
}

