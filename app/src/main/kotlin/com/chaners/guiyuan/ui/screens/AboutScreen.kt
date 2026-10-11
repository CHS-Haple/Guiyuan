package com.chaners.guiyuan.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.chaners.guiyuan.BuildConfig
import com.chaners.guiyuan.R
import com.chaners.guiyuan.system.RuntimeEnv
import com.chaners.guiyuan.ui.screens.about.BgEffectBackground
import top.yukonga.miuix.kmp.anim.folmeSpring
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
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
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val listState = rememberLazyListState()
    val backgroundActive by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val heroHeightPx = with(density) { 316.dp.toPx() }
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
        background = {
            BgEffectBackground(
                dynamicBackground = backgroundActive,
                modifier = Modifier.fillMaxSize(),
                isFullSize = true,
                alpha = {
                    if (listState.firstVisibleItemIndex > 0) 0f
                    else 1f - (listState.firstVisibleItemScrollOffset / heroHeightPx).coerceIn(0f, 1f)
                },
            ) {}
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
        item(key = "aboutHero") {
            AboutHero()
        }
        Section(R.string.section_app) {
            BasicComponent(
                title = stringResource(R.string.product_name),
                summary = stringResource(R.string.app_description),
                startAction = {
                    GuiyuanAnimatedIdentityMark()
                },
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            BasicComponent(
                title = stringResource(R.string.diagnostics_version_label),
                summary = BuildConfig.VERSION_NAME,
                startAction = {
                    SemanticLeadingIcon(
                        iconRes = R.drawable.ic_material_symbol_tag,
                    )
                },
            )
            BasicComponent(
                title = stringResource(R.string.diagnostics_build_label),
                summary = BuildConfig.BUILD_ID,
                startAction = {
                    SemanticLeadingIcon(
                        iconRes = R.drawable.ic_material_symbol_deployed_code,
                    )
                },
            )
            BasicComponent(
                title = stringResource(R.string.diagnostics_package_label),
                summary = BuildConfig.APPLICATION_ID,
                startAction = {
                    SemanticLeadingIcon(
                        iconRes = R.drawable.ic_material_symbol_package_2,
                    )
                },
            )
        }

        Section(R.string.section_about_project) {
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

        Section(R.string.section_device_system) {
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
            BasicComponent(
                title = stringResource(R.string.systemui_version_label),
                summary = env.sysUiVersion.ifBlank { unavailable },
                startAction = {
                    SemanticLeadingIcon(
                        iconRes = R.drawable.ic_material_symbol_dashboard,
                    )
                },
            )
        }

        Section(R.string.section_module_runtime) {
            BasicComponent(
                title = stringResource(R.string.runtime_framework_title),
                summary = stringResource(R.string.runtime_framework_summary),
                startAction = {
                    SemanticLeadingIcon(
                        iconRes = R.drawable.ic_material_symbol_extension,
                    )
                },
            )
            BasicComponent(
                title = stringResource(R.string.runtime_scope_title),
                summary = stringResource(R.string.runtime_scope_summary),
                startAction = {
                    SemanticLeadingIcon(
                        iconRes = R.drawable.ic_material_symbol_target,
                    )
                },
            )
            BasicComponent(
                title = stringResource(R.string.runtime_target_title),
                summary = stringResource(R.string.runtime_target_summary),
                startAction = {
                    SemanticLeadingIcon(
                        iconRes = R.drawable.ic_material_symbol_fact_check,
                    )
                },
            )
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

@Composable
private fun AboutHero() {
    Column(
        modifier = Modifier.fillMaxWidth().height(316.dp).padding(top = 28.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        GuiyuanAnimatedIdentityMark(markSize = 116.dp, interactive = true)
        Text(
            text = stringResource(R.string.product_name),
            modifier = Modifier.padding(top = 12.dp),
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.onSurface,
        )
        Text(
            text = "v${BuildConfig.VERSION_NAME}",
            modifier = Modifier.padding(top = 4.dp),
            fontSize = 14.sp,
            color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Text(
            text = stringResource(R.string.about_logo_hold_hint),
            modifier = Modifier.padding(top = 10.dp),
            fontSize = 12.sp,
            color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        )
    }
}

@Composable
private fun GuiyuanAnimatedIdentityMark(
    markSize: Dp = 64.dp,
    interactive: Boolean = false,
) {
    var boosted by remember { mutableStateOf(false) }
    val speed by animateFloatAsState(
        targetValue = if (boosted) 4f else 1f,
        animationSpec = folmeSpring(damping = 1f, response = if (boosted) 0.65f else 0.9f),
        label = "aboutOrbitSpeed",
    )
    val currentSpeed by rememberUpdatedState(speed)
    var orbitRotation by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var previous = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            // A resumed frame must not turn a long pause into an angular jump.
            val delta = (now - previous).coerceIn(0L, 50_000_000L) / 1_000_000_000f
            previous = now
            orbitRotation = (orbitRotation - 18f * currentSpeed * delta) % 360f
        }
    }
    val pressModifier =
        if (interactive) {
            Modifier.pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        try {
                            tryAwaitRelease()
                        } finally {
                            boosted = false
                        }
                    },
                    onLongPress = { boosted = true },
                )
            }
        } else {
            Modifier
        }

    val painter = painterResource(R.drawable.ic_launcher_foreground)
    val tint = MiuixTheme.colorScheme.onSurfaceContainer

    Box(
        modifier = Modifier.size(markSize).then(pressModifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier.size(markSize),
        ) {
            val targetSize =
                Size(
                    width = size.width * 1.8f,
                    height = size.height * 1.8f,
                )
            val left = (size.width - targetSize.width) / 2f
            val top = (size.height - targetSize.height) / 2f

            rotate(degrees = orbitRotation, pivot = center) {
                translate(left = left, top = top) {
                    with(painter) {
                        draw(
                            size = targetSize,
                            colorFilter = ColorFilter.tint(tint),
                        )
                    }
                }
            }
        }
    }
}
