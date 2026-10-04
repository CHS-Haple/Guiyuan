package com.chaners.guiyuan.ui.screens

import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.BuildConfig
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.AppThemeMode
import com.chaners.guiyuan.settings.AppearanceSettings
import com.chaners.guiyuan.settings.DiagnosticsLevel
import com.chaners.guiyuan.settings.FloatingNavigationContent
import com.chaners.guiyuan.settings.FloatingNavigationStyle
import com.chaners.guiyuan.settings.DiagnosticsSettings
import com.chaners.guiyuan.settings.DiagnosticsSettingsRepository
import com.chaners.guiyuan.system.DiagnosticsLogReader
import com.chaners.guiyuan.system.DiagnosticsReportBuilder
import com.chaners.guiyuan.system.DiagnosticsReportFiles
import com.chaners.guiyuan.system.RuntimeEnvironmentInfo
import com.chaners.guiyuan.ui.components.FloatingNavigationContentItem
import com.chaners.guiyuan.ui.components.MiuixBlurredTopBar
import com.chaners.guiyuan.ui.components.floatingNavigationMaterial
import com.chaners.guiyuan.ui.components.rememberTopBarBackdrop
import com.chaners.guiyuan.ui.components.topBarBackdropSource
import com.chaners.guiyuan.ui.components.requiresTextureBackdrop
import com.chaners.guiyuan.ui.layout.pageContentPadding
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun AppearanceScreen(
    settings: AppearanceSettings,
    darkMode: Boolean,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onDynamicColorEnabledChange: (Boolean) -> Unit,
    onFloatingNavigationBarEnabledChange: (Boolean) -> Unit,
    onFloatingNavigationStyleChange: (FloatingNavigationStyle) -> Unit,
    onFloatingNavigationContentChange: (FloatingNavigationContent) -> Unit,
    onBack: () -> Unit,
) {
    val themeOptions =
        listOf(
            stringResource(R.string.theme_system),
            stringResource(R.string.theme_light),
            stringResource(R.string.theme_dark),
        )
    val floatingStyleOptions =
        listOf(
            stringResource(R.string.floating_navigation_style_standard),
            stringResource(R.string.floating_navigation_style_blur),
            stringResource(R.string.floating_navigation_style_glass),
        )
    val floatingContentOptions =
        listOf(
            stringResource(R.string.floating_navigation_content_icon_only),
            stringResource(R.string.floating_navigation_content_icon_and_text),
        )

    SettingsPage(title = stringResource(R.string.appearance_title), onBack = onBack) {
        item {
            AppearanceThemePreview(
                settings = settings,
                darkMode = darkMode,
            )
        }

        Section(R.string.section_theme) {
            OverlayDropdownPreference(
                items = themeOptions,
                selectedIndex = settings.themeMode.ordinal,
                title = stringResource(R.string.theme_mode),
                summary = stringResource(R.string.theme_mode_summary),
                startAction = {
                    SemanticLeadingIcon(
                        iconRes = R.drawable.ic_material_symbol_contrast,
                        visualSize = 22.dp,
                    )
                },
                showValue = true,
                onSelectedIndexChange = { index ->
                    AppThemeMode.entries.getOrNull(index)?.let { mode ->
                        if (mode != settings.themeMode) {
                            onThemeModeChange(mode)
                        }
                    }
                },
            )
            SwitchPreference(
                title = stringResource(R.string.dynamic_color),
                summary = stringResource(R.string.dynamic_color_summary),
                startAction = {
                    SemanticLeadingIcon(
                        iconRes = R.drawable.ic_material_symbol_palette,
                        visualSize = 22.dp,
                    )
                },
                checked = settings.dynamicColorEnabled,
                onCheckedChange = onDynamicColorEnabledChange,
            )
        }

        Section(R.string.section_visual_effects) {
            SwitchPreference(
                title = stringResource(R.string.floating_navigation_bar),
                summary = stringResource(R.string.floating_navigation_bar_summary),
                startAction = {
                    SemanticLeadingIcon(
                        iconRes = R.drawable.ic_material_symbol_bottom_navigation,
                        visualSize = 22.dp,
                    )
                },
                checked = settings.floatingNavigationBarEnabled,
                onCheckedChange = onFloatingNavigationBarEnabledChange,
            )
            AnimatedPreferenceGroup(visible = settings.floatingNavigationBarEnabled) {
                OverlayDropdownPreference(
                    items = floatingStyleOptions,
                    selectedIndex = settings.floatingNavigationStyle.ordinal,
                    title = stringResource(R.string.floating_navigation_style),
                    summary = stringResource(R.string.floating_navigation_style_summary),
                    startAction = {
                        SemanticLeadingIcon(
                            iconRes = R.drawable.ic_material_symbol_style,
                            visualSize = 22.dp,
                        )
                    },
                    showValue = true,
                    onSelectedIndexChange = { index ->
                        FloatingNavigationStyle.entries.getOrNull(index)?.let { style ->
                            if (style != settings.floatingNavigationStyle) {
                                onFloatingNavigationStyleChange(style)
                            }
                        }
                    },
                )
                OverlayDropdownPreference(
                    items = floatingContentOptions,
                    selectedIndex = settings.floatingNavigationContent.ordinal,
                    title = stringResource(R.string.floating_navigation_content),
                    summary = stringResource(R.string.floating_navigation_content_summary),
                    startAction = {
                        SemanticLeadingIcon(
                            iconRes = R.drawable.ic_material_symbol_format_list_bulleted,
                            visualSize = 22.dp,
                        )
                    },
                    showValue = true,
                    onSelectedIndexChange = { index ->
                        FloatingNavigationContent.entries.getOrNull(index)?.let { content ->
                            if (content != settings.floatingNavigationContent) {
                                onFloatingNavigationContentChange(content)
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun AppearanceThemePreview(
    settings: AppearanceSettings,
    darkMode: Boolean,
) {
    Card(
        modifier =
            Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.theme_preview),
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceContainer,
            )
            Text(
                text = stringResource(R.string.theme_preview_summary),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )

            AppearanceMiniPreview(
                settings = settings,
                darkMode = darkMode,
            )
        }
    }
}

@Composable
private fun AppearanceMiniPreview(
    settings: AppearanceSettings,
    darkMode: Boolean,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        shape = RoundedCornerShape(18.dp),
        color = MiuixTheme.colorScheme.surface,
        border =
            BorderStroke(
                width = 1.dp,
                color = MiuixTheme.colorScheme.outline.copy(alpha = 0.18f),
            ),
    ) {
        ScaledPreviewContent(
            scale = MiniPreviewScale,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, top = 13.dp, end = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MiniPreviewHeader()
                MiniSwitchSettingPreview()
                MiniSliderSettingPreview()
                MiniNavigationPreview(
                    floating = settings.floatingNavigationBarEnabled,
                    style = settings.floatingNavigationStyle,
                    content = settings.floatingNavigationContent,
                    darkMode = darkMode,
                )
            }
        }
    }
}

@Composable
private fun MiniPreviewHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MiniTextBar(width = 88.dp, role = MiniTextRole.Title)
            MiniTextBar(width = 54.dp, role = MiniTextRole.Body2)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            MiniThemeSwatch(MiuixTheme.colorScheme.primary)
            MiniThemeSwatch(MiuixTheme.colorScheme.secondary)
            MiniThemeSwatch(MiuixTheme.colorScheme.surfaceContainerHigh)
        }
    }
}

@Composable
private fun MiniSwitchSettingPreview() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MiniTextBar(width = 82.dp, role = MiniTextRole.Body1)
                MiniTextBar(width = 56.dp, role = MiniTextRole.Body2)
            }
            Switch(
                checked = true,
                onCheckedChange = null,
            )
        }
    }
}

@Composable
private fun MiniSliderSettingPreview() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Surface(
                    modifier = Modifier.size(14.dp),
                    shape = RoundedCornerShape(7.dp),
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant.copy(alpha = 0.20f),
                ) {}
                MiniTextBar(width = 72.dp, role = MiniTextRole.Body1)
            }
            Slider(
                value = 0.43f,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                colors =
                    SliderDefaults.sliderColors(
                        disabledForegroundColor = MiuixTheme.colorScheme.primary,
                        disabledBackgroundColor = MiuixTheme.colorScheme.sliderBackground,
                        disabledThumbColor = MiuixTheme.colorScheme.onPrimary,
                    ),
            )
        }
    }
}

private enum class MiniTextRole {
    Title,
    Body1,
    Body2,
}

@Composable
private fun MiniTextBar(
    width: androidx.compose.ui.unit.Dp,
    role: MiniTextRole,
    modifier: Modifier = Modifier,
) {
    val fontSize =
        when (role) {
            MiniTextRole.Title -> MiuixTheme.textStyles.title3.fontSize
            MiniTextRole.Body1 -> MiuixTheme.textStyles.body1.fontSize
            MiniTextRole.Body2 -> MiuixTheme.textStyles.body2.fontSize
        }
    val height = (fontSize.value * 0.35f).dp
    val alpha =
        when (role) {
            MiniTextRole.Title -> 0.34f
            MiniTextRole.Body1 -> 0.27f
            MiniTextRole.Body2 -> 0.17f
        }

    Surface(
        modifier =
            modifier
                .width(width)
                .height(height),
        shape = RoundedCornerShape(height / 2f),
        color = MiuixTheme.colorScheme.onSurfaceContainerVariant.copy(alpha = alpha),
    ) {}
}

@Composable
private fun MiniThemeSwatch(color: Color) {
    Surface(
        modifier = Modifier.size(14.dp),
        shape = CircleShape,
        color = color,
        border =
            BorderStroke(
                width = 1.dp,
                color = MiuixTheme.colorScheme.outline.copy(alpha = 0.26f),
            ),
    ) {}
}

private const val MiniPreviewScale = 0.82f
private val MiniNavigationViewportHeight = 76.dp

@Composable
private fun ScaledPreviewContent(
    scale: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier,
        content = content,
    ) { measurables, constraints ->
        val maxWidth =
            if (constraints.hasBoundedWidth) {
                (constraints.maxWidth / scale).roundToInt()
            } else {
                constraints.maxWidth
            }
        val placeable =
            measurables.single().measure(
                Constraints(
                    minWidth = 0,
                    maxWidth = maxWidth,
                    minHeight = 0,
                    maxHeight = Constraints.Infinity,
                ),
            )
        val scaledHeight = (placeable.height * scale).roundToInt()
        val layoutHeight =
            scaledHeight.coerceIn(
                constraints.minHeight,
                if (constraints.hasBoundedHeight) constraints.maxHeight else scaledHeight,
            )

        layout(constraints.maxWidth, layoutHeight) {
            placeable.placeRelativeWithLayer(0, 0) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
}

@Composable
private fun MiniNavigationPreview(
    floating: Boolean,
    style: FloatingNavigationStyle,
    content: FloatingNavigationContent,
    darkMode: Boolean,
) {
    val materialActive =
        floating &&
            style.requiresTextureBackdrop &&
            isRuntimeShaderSupported()
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop =
        if (materialActive) {
            rememberLayerBackdrop {
                drawRect(surfaceColor)
                drawContent()
            }
        } else {
            null
        }
    val floatingModifier =
        if (backdrop != null) {
            Modifier.floatingNavigationMaterial(
                backdrop = backdrop,
                darkMode = darkMode,
                style = style,
            )
        } else {
            Modifier
        }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(MiniNavigationViewportHeight),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(
                        if (backdrop != null) {
                            Modifier.layerBackdrop(backdrop)
                        } else {
                            Modifier
                        },
                    ),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MiuixTheme.colorScheme.surface,
            ) {}
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                MiniTextBar(width = 92.dp, role = MiniTextRole.Body1)
                MiniTextBar(width = 58.dp, role = MiniTextRole.Body2)
            }
        }

        if (floating) {
            MiniNavigationViewportContent {
                FloatingNavigationBar(
                    modifier = floatingModifier,
                    color =
                        if (backdrop != null) {
                            Color.Transparent
                        } else {
                            MiuixTheme.colorScheme.surfaceContainer
                        },
                    defaultWindowInsetsPadding = false,
                ) {
                    FloatingNavigationContentItem(
                        content = content,
                        selected = false,
                        onClick = {},
                        icon = MiuixIcons.Normal.Home,
                        label = stringResource(R.string.nav_home),
                    )
                    FloatingNavigationContentItem(
                        content = content,
                        selected = false,
                        onClick = {},
                        icon = MiuixIcons.Normal.Tune,
                        label = stringResource(R.string.nav_features),
                    )
                    FloatingNavigationContentItem(
                        content = content,
                        selected = true,
                        onClick = {},
                        icon = MiuixIcons.Medium.Settings,
                        label = stringResource(R.string.nav_settings),
                    )
                }
            }
        } else {
            MiniNavigationViewportContent {
                NavigationBar(
                    color = MiuixTheme.colorScheme.surface,
                    showDivider = true,
                    defaultWindowInsetsPadding = false,
                ) {
                    MiniStandardNavigationItem(selected = false, icon = MiuixIcons.Normal.Home)
                    MiniStandardNavigationItem(selected = false, icon = MiuixIcons.Normal.Tune)
                    MiniStandardNavigationItem(selected = true, icon = MiuixIcons.Medium.Settings)
                }
            }
        }
    }
}

@Composable
private fun MiniNavigationViewportContent(
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = Modifier.fillMaxSize(),
        content = content,
    ) { measurables, constraints ->
        val placeable =
            measurables.single().measure(
                constraints.copy(
                    minHeight = 0,
                    maxHeight = Constraints.Infinity,
                ),
            )
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.placeRelative(
                x = ((constraints.maxWidth - placeable.width) / 2).coerceAtLeast(0),
                y = (constraints.maxHeight - placeable.height).coerceAtLeast(0),
            )
        }
    }
}

@Composable
private fun RowScope.MiniStandardNavigationItem(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    val contentColor =
        MiuixTheme.colorScheme.onSurface.copy(
            alpha = if (selected) 1f else 0.4f,
        )

    Column(
        modifier =
            Modifier
                .height(NavigationBarDefaults.ItemHeight)
                .weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(NavigationBarDefaults.IconTopPadding))
        Icon(
            modifier = Modifier.size(NavigationBarDefaults.IconSize),
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
        )
        Spacer(modifier = Modifier.height(3.dp))
        MiniTextBar(
            width = if (selected) 30.dp else 26.dp,
            role = MiniTextRole.Body2,
        )
    }
}

@Composable
internal fun DiagnosticsScreen(onBack: () -> Unit) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedScope by rememberSaveable { mutableIntStateOf(0) }
    var refreshGeneration by rememberSaveable { mutableIntStateOf(0) }
    var snapshot by remember { mutableStateOf<DiagnosticsLogReader.Snapshot?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(refreshGeneration) {
        loading = true
        snapshot = DiagnosticsLogReader.read()
        loading = false
    }

    val scopeOptions =
        listOf(
            stringResource(R.string.diagnostics_log_scope_session),
            stringResource(R.string.diagnostics_log_scope_all),
        )
    val visibleLines =
        snapshot
            ?.let { current ->
                if (selectedScope == 0) {
                    current.latestSessionLines
                } else {
                    current.lines
                }
            }
            .orEmpty()
            .let { lines ->
                val query = searchQuery.trim()
                if (query.isEmpty()) {
                    lines
                } else {
                    lines.filter { line -> line.contains(query, ignoreCase = true) }
                }
            }
            .asReversed()
    val sourceLabel =
        when (snapshot?.source) {
            DiagnosticsLogReader.Source.LsposedModules ->
                stringResource(R.string.diagnostics_log_source_lsposed)
            DiagnosticsLogReader.Source.LogcatFallback ->
                stringResource(R.string.diagnostics_log_source_logcat)
            null -> stringResource(R.string.diagnostics_log_source_unknown)
        }
    val sourceLineCount = snapshot?.lines?.size ?: 0
    val refreshSummary =
        if (loading) {
            stringResource(R.string.diagnostics_log_loading)
        } else {
            stringResource(
                R.string.diagnostics_log_refresh_summary,
                sourceLabel,
                sourceLineCount,
            )
        }

    SettingsPage(
        title = stringResource(R.string.diagnostics_title),
        onBack = onBack,
    ) {
        Section(R.string.section_diagnostic_logs) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 12.dp),
                label = stringResource(R.string.diagnostics_log_search),
                singleLine = true,
            )
            TabRowWithContour(
                tabs = scopeOptions,
                selectedTabIndex = selectedScope,
                onTabSelected = { index -> selectedScope = index },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 10.dp),
            )
            BasicComponent(
                title = stringResource(R.string.diagnostics_log_refresh),
                summary = refreshSummary,
                enabled = !loading,
                onClick = { refreshGeneration += 1 },
            )
        }

        if (visibleLines.isEmpty()) {
            item {
                DiagnosticsLogStateCard(
                    text =
                        when {
                            loading -> stringResource(R.string.diagnostics_log_loading)
                            searchQuery.isNotBlank() ->
                                stringResource(R.string.diagnostics_log_no_matches)
                            else -> stringResource(R.string.diagnostics_log_empty)
                        },
                )
            }
        } else {
            itemsIndexed(
                items = visibleLines,
                key = { index, line -> index.toString() + ":" + line.hashCode() },
            ) { _, line ->
                DiagnosticsLogEntryCard(line)
            }
        }
    }
}

@Composable
private fun DiagnosticsLogStateCard(text: String) {
    Card(
        modifier =
            Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
        insideMargin = PaddingValues(16.dp),
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
        )
    }
}

@Composable
private fun DiagnosticsLogEntryCard(line: String) {
    val parsed = DiagnosticLogTimestampRegex.matchEntire(line)
    val timestamp = parsed?.groupValues?.getOrNull(1)
    val message = parsed?.groupValues?.getOrNull(2) ?: line

    Card(
        modifier =
            Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 8.dp),
        insideMargin = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (!timestamp.isNullOrBlank()) {
            Text(
                text = timestamp,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )
            Spacer(modifier = Modifier.height(3.dp))
        }
        Text(
            text = message,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceContainer,
        )
    }
}

private val DiagnosticLogTimestampRegex =
    Regex("""^(\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d+)\s+(.*)$""")

@Composable
internal fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val environment by
        produceState(
            initialValue = RuntimeEnvironmentInfo.basic(),
            key1 = context.applicationContext,
        ) {
            value = RuntimeEnvironmentInfo.resolve(context.applicationContext)
        }
    val diagnosticsRepository =
        remember(context.applicationContext) {
            DiagnosticsSettingsRepository(context.applicationContext)
        }
    val diagnosticsSettings by
        diagnosticsRepository.settings.collectAsState(
            initial = DiagnosticsSettings(level = diagnosticsRepository.currentLevel()),
        )
    val diagnosticsLevelOptions =
        listOf(
            stringResource(R.string.diagnostics_mode_basic),
            stringResource(R.string.diagnostics_mode_detailed),
        )
    var reportInProgress by rememberSaveable { mutableStateOf(false) }
    var exportPickerOpen by rememberSaveable { mutableStateOf(false) }

    val reportShareTitle = stringResource(R.string.share_diagnostic_report)
    val exportSucceededMessage = stringResource(R.string.diagnostic_report_exported)
    val exportFailedMessage = stringResource(R.string.diagnostic_report_export_failed)
    val shareFailedMessage = stringResource(R.string.diagnostic_report_share_failed)

    fun buildReport(onReady: suspend (String) -> Unit) {
        if (reportInProgress) return
        reportInProgress = true
        scope.launch {
            try {
                onReady(DiagnosticsReportBuilder.build(context.applicationContext))
            } finally {
                reportInProgress = false
            }
        }
    }

    val exportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("text/plain"),
        ) { uri ->
            exportPickerOpen = false
            if (uri != null) {
                buildReport { report ->
                    val success =
                        DiagnosticsReportFiles.writeExport(
                            context = context,
                            uri = uri,
                            report = report,
                        )
                    snackbarHostState.showSnackbar(
                        if (success) exportSucceededMessage else exportFailedMessage,
                    )
                }
            }
        }

    SettingsPage(
        title = stringResource(R.string.about_title),
        onBack = onBack,
        snackbarHost = { SnackbarHost(state = snackbarHostState) },
    ) {
        Section(R.string.section_diagnostics_app) {
            DiagnosticsCardHeader(
                title = stringResource(R.string.product_name),
                subtitle = stringResource(R.string.app_description),
                leadingContent = {
                    GuiyuanAnimatedIdentityMark()
                },
            )
            DiagnosticsInfoDivider()
            DiagnosticsInfoValue(
                value = BuildConfig.VERSION_NAME,
                label = stringResource(R.string.diagnostics_version_label),
                iconRes = R.drawable.ic_material_symbol_tag,
            )
            DiagnosticsInfoValue(
                value = BuildConfig.BUILD_ID,
                label = stringResource(R.string.diagnostics_build_label),
                iconRes = R.drawable.ic_material_symbol_deployed_code,
            )
            DiagnosticsInfoValue(
                value = BuildConfig.APPLICATION_ID,
                label = stringResource(R.string.diagnostics_package_label),
                iconRes = R.drawable.ic_material_symbol_data_object,
            )
        }

        Section(R.string.section_device_system) {
            DiagnosticsCardHeader(title = environment.deviceName)
            DiagnosticsInfoValue(
                value = environment.modelAndCodename,
                label = stringResource(R.string.device_model_label),
                iconRes = R.drawable.ic_material_symbol_smartphone,
            )
            DiagnosticsInfoValue(
                value = environment.androidDisplay,
                label = stringResource(R.string.android_version_label),
                iconRes = R.drawable.ic_material_symbol_android,
            )
            DiagnosticsInfoValue(
                value = environment.osVersion,
                label = stringResource(R.string.os_version_label),
                iconRes = R.drawable.ic_material_symbol_layers,
            )
            DiagnosticsInfoValue(
                value = environment.systemUiDisplay,
                label = stringResource(R.string.systemui_version_label),
                iconRes = R.drawable.ic_material_symbol_dashboard,
            )
        }

        Section(R.string.section_module_runtime) {
            Spacer(modifier = Modifier.height(8.dp))
            DiagnosticsInfoValue(
                value = stringResource(R.string.runtime_framework_summary),
                label = stringResource(R.string.runtime_framework_title),
                iconRes = R.drawable.ic_material_symbol_extension,
            )
            DiagnosticsInfoValue(
                value = stringResource(R.string.runtime_scope_summary),
                label = stringResource(R.string.runtime_scope_title),
                iconRes = R.drawable.ic_material_symbol_target,
            )
            DiagnosticsInfoValue(
                value = stringResource(R.string.runtime_target_summary),
                label = stringResource(R.string.runtime_target_title),
                iconRes = R.drawable.ic_material_symbol_fact_check,
            )
            if (BuildConfig.DEVELOPMENT_PROBES) {
                DiagnosticsInfoValue(
                    value = stringResource(R.string.runtime_inventory_summary),
                    label = stringResource(R.string.runtime_inventory_title),
                    iconRes = R.drawable.ic_material_symbol_inventory_2,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Section(R.string.section_diagnostic_report) {
            OverlayDropdownPreference(
                items = diagnosticsLevelOptions,
                selectedIndex = diagnosticsSettings.level.ordinal,
                title = stringResource(R.string.diagnostics_mode_title),
                summary = stringResource(R.string.diagnostics_mode_summary),
                startAction = {
                    SemanticLeadingIcon(
                        iconRes = R.drawable.ic_material_symbol_troubleshoot,
                        visualSize = 22.dp,
                    )
                },
                showValue = true,
                onSelectedIndexChange = { index ->
                    DiagnosticsLevel.entries.getOrNull(index)?.let { level ->
                        if (level != diagnosticsSettings.level) {
                            diagnosticsRepository.setLevel(level)
                        }
                    }
                },
            )
            DiagnosticsActionRow(
                title = stringResource(R.string.export_diagnostic_report),
                summary = stringResource(R.string.export_diagnostic_report_summary),
                iconRes = R.drawable.ic_material_symbol_file_export,
                iconVisualSize = 22.dp,
                enabled = !reportInProgress && !exportPickerOpen,
                onClick = {
                    exportPickerOpen = true
                    exportLauncher.launch(DiagnosticsReportFiles.suggestedFileName())
                },
            )
            DiagnosticsActionRow(
                title = stringResource(R.string.share_diagnostic_report),
                summary = stringResource(R.string.share_diagnostic_report_summary),
                iconRes = R.drawable.ic_material_symbol_share,
                iconVisualSize = 22.dp,
                enabled = !reportInProgress && !exportPickerOpen,
                onClick = {
                    buildReport { report ->
                        val prepared =
                            DiagnosticsReportFiles.prepareShare(
                                context = context,
                                report = report,
                            )
                        if (prepared == null) {
                            snackbarHostState.showSnackbar(shareFailedMessage)
                            return@buildReport
                        }

                        val sendIntent =
                            Intent(Intent.ACTION_SEND).apply {
                                type = DiagnosticsReportFiles.ShareMimeType
                                putExtra(Intent.EXTRA_STREAM, prepared.uri)
                                clipData =
                                    ClipData.newUri(
                                        context.contentResolver,
                                        reportShareTitle,
                                        prepared.uri,
                                    )
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                        DiagnosticsReportFiles.logShareIntent(
                            context = context,
                            intent = sendIntent,
                            uri = prepared.uri,
                        )

                        val chooserIntent =
                            Intent.createChooser(
                                sendIntent,
                                reportShareTitle,
                            ).apply {
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }

                        runCatching {
                            context.startActivity(chooserIntent)
                        }.onSuccess {
                            DiagnosticsReportFiles.logChooserLaunch(context)
                        }.onFailure { error ->
                            DiagnosticsReportFiles.logChooserLaunch(context, error)
                            DiagnosticsReportFiles.discardShare(context, prepared)
                            snackbarHostState.showSnackbar(shareFailedMessage)
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun GuiyuanAnimatedIdentityMark() {
    val orbitRotation by
        rememberInfiniteTransition(label = "guiyuanIdentityOrbit").animateFloat(
            initialValue = 0f,
            targetValue = -360f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            durationMillis = 20_000,
                            easing = LinearEasing,
                        ),
                ),
            label = "guiyuanIdentityOrbitRotation",
        )
    val painter = painterResource(R.drawable.ic_launcher_foreground)
    val tint = MiuixTheme.colorScheme.onSurfaceContainer

    Box(
        modifier = Modifier.size(64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier.size(64.dp),
        ) {
            val targetSize =
                Size(
                    width = size.width * 1.8f,
                    height = size.height * 1.8f,
                )
            val left = (size.width - targetSize.width) / 2f
            val top = (size.height - targetSize.height) / 2f

            rotate(
                degrees = orbitRotation,
                pivot = center,
            ) {
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

@Composable
private fun DiagnosticsCardHeader(
    title: String,
    subtitle: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(
                    top = 13.dp,
                    bottom = if (leadingContent != null) 11.dp else 7.dp,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingContent != null) {
            leadingContent()
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                style =
                    if (leadingContent != null) {
                        MiuixTheme.textStyles.title3
                    } else {
                        MiuixTheme.textStyles.title2
                    },
                color = MiuixTheme.colorScheme.onSurfaceContainer,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    modifier = Modifier.padding(top = 2.dp),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                )
            }
        }
    }
}

@Composable
private fun DiagnosticsInfoDivider() {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .height(1.dp),
        color = MiuixTheme.colorScheme.onSurfaceContainerVariant.copy(alpha = 0.16f),
    ) {}
}

@Composable
private fun DiagnosticsInfoValue(
    value: String,
    label: String,
    @DrawableRes iconRes: Int? = null,
    iconVisualSize: Dp = 22.dp,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconRes != null) {
            SemanticLeadingIcon(
                iconRes = iconRes,
                visualSize = iconVisualSize,
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = value.ifBlank { "—" },
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceContainer,
            )
            Text(
                text = label,
                modifier = Modifier.padding(top = 1.dp),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )
        }
    }
}

@Composable
private fun DiagnosticsActionRow(
    title: String,
    summary: String,
    @DrawableRes iconRes: Int,
    iconVisualSize: Dp,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    BasicComponent(
        title = title,
        summary = summary,
        startAction = {
            SemanticLeadingIcon(
                iconRes = iconRes,
                visualSize = iconVisualSize,
                enabled = enabled,
            )
        },
        onClick = onClick,
        onClickLabel = title,
        enabled = enabled,
    )
}

@Composable
private fun SemanticLeadingIcon(
    @DrawableRes iconRes: Int,
    visualSize: Dp,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(visualSize),
            colorFilter =
                ColorFilter.tint(
                    MiuixTheme.colorScheme.onSurfaceContainer.copy(
                        alpha = if (enabled) 1f else 0.38f,
                    ),
                ),
        )
    }
}

@Composable
private fun DiagnosticsLeadingIcon(
    icon: ImageVector,
    visualSize: Dp,
    enabled: Boolean = true,
) {
    Box(
        modifier =
            Modifier
                .size(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(visualSize),
            tint =
                if (enabled) {
                    MiuixTheme.colorScheme.onSurfaceContainer
                } else {
                    MiuixTheme.colorScheme.onSurfaceContainerVariant.copy(alpha = 0.38f)
                },
        )
    }
}

@Composable
private fun SettingsPage(
    title: String,
    onBack: () -> Unit,
    snackbarHost: @Composable () -> Unit = {},
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val topBarBackdrop = rememberTopBarBackdrop()

    Scaffold(
        snackbarHost = snackbarHost,
        topBar = {
            MiuixBlurredTopBar(
                backdrop = topBarBackdrop,
                scrollBehavior = scrollBehavior,
            ) { barColor ->
                SmallTopAppBar(
                    title = title,
                    color = barColor,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                MiuixIcons.Back,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .topBarBackdropSource(topBarBackdrop),
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = pageContentPadding(
                    innerPadding = paddingValues,
                    extraBottom = 12.dp,
                ),
                content = content,
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.Section(
    @StringRes titleRes: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    item {
        SmallTitle(stringResource(titleRes))
        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
            content = content,
        )
    }
}
