package com.chaners.guiyuan.ui.screens

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.chaners.guiyuan.system.DiagnosticLogCategory
import com.chaners.guiyuan.system.DiagnosticLogEntry
import com.chaners.guiyuan.system.DiagnosticLogLevel
import com.chaners.guiyuan.system.DiagnosticsSnapshot
import com.chaners.guiyuan.system.DiagnosticsSnapshotProvider
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
import com.chaners.guiyuan.ui.theme.RuntimeWarningAccent
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Badge
import top.yukonga.miuix.kmp.basic.BadgedBox
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDefaults
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.menu.WindowIconCascadingDropdownMenu
import top.yukonga.miuix.kmp.preference.ArrowPreference
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val diagnosticsRepository =
        remember(context.applicationContext) {
            DiagnosticsSettingsRepository(context.applicationContext)
        }
    val diagnosticsSettings by
        diagnosticsRepository.settings.collectAsState(
            initial = DiagnosticsSettings(level = diagnosticsRepository.currentLevel()),
        )

    var snapshot by remember { mutableStateOf<DiagnosticsSnapshot?>(null) }
    var loading by remember { mutableStateOf(true) }
    var pullRefreshing by remember { mutableStateOf(false) }
    var viewCleared by rememberSaveable { mutableStateOf(false) }
    var expandedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var refreshGeneration by rememberSaveable { mutableIntStateOf(0) }
    var reportInProgress by rememberSaveable { mutableStateOf(false) }
    var exportPickerOpen by rememberSaveable { mutableStateOf(false) }
    var levelFilter by rememberSaveable {
        mutableIntStateOf(DiagnosticsFilterLevelAll)
    }
    var categoryFilter by rememberSaveable {
        mutableIntStateOf(DiagnosticsFilterCategoryAll)
    }

    val exportSucceededMessage = stringResource(R.string.diagnostic_report_exported)
    val exportFailedMessage = stringResource(R.string.diagnostic_report_export_failed)
    val shareFailedMessage = stringResource(R.string.diagnostic_report_share_failed)
    val reportShareTitle = stringResource(R.string.share_diagnostic_report)
    val reportExportTitle = stringResource(R.string.export_diagnostic_report)
    val moreActionsTitle = stringResource(R.string.diagnostics_more_actions)
    val filterTitle = stringResource(R.string.diagnostics_filter)
    val pullRefreshTexts =
        listOf(
            stringResource(R.string.diagnostics_pull_to_refresh),
            stringResource(R.string.diagnostics_release_to_refresh),
            stringResource(R.string.diagnostics_refreshing),
            stringResource(R.string.diagnostics_refresh_complete),
        )

    fun requestRefresh() {
        if (loading) return
        loading = true
        pullRefreshing = true
        refreshGeneration += 1
    }

    fun withCurrentReport(onReady: suspend (String) -> Unit) {
        val captured = snapshot ?: return
        if (reportInProgress) return
        reportInProgress = true
        scope.launch {
            try {
                onReady(DiagnosticsReportBuilder.build(captured))
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
                withCurrentReport { report ->
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

    LaunchedEffect(refreshGeneration) {
        loading = true
        try {
            snapshot = DiagnosticsSnapshotProvider.capture(context.applicationContext)
            expandedKey = null
            viewCleared = false
        } finally {
            loading = false
            pullRefreshing = false
        }
    }

    val reportActionsEnabled =
        snapshot != null &&
            !loading &&
            !reportInProgress &&
            !exportPickerOpen
    val runtimeEntries =
        if (viewCleared) {
            emptyList()
        } else {
            snapshot
                ?.sessionEntries
                .orEmpty()
                .asSequence()
                .filter(::diagnosticLogIsRuntimeEntry)
                .toList()
                .asReversed()
        }
    val usefulEntries =
        runtimeEntries
            .asSequence()
            .filter { entry ->
                diagnosticsFilterMatches(
                    entry = entry,
                    levelMask = levelFilter,
                    categoryMask = categoryFilter,
                )
            }
            .take(MaxDiagnosticsUsefulEvents)
            .toList()
    val filterActive =
        levelFilter != DiagnosticsFilterLevelAll ||
            categoryFilter != DiagnosticsFilterCategoryAll
    SettingsPage(
        title = stringResource(R.string.diagnostics_title),
        onBack = onBack,
        snackbarHost = { SnackbarHost(state = snackbarHostState) },
        navigationActions = {
            DiagnosticsFilterMenu(
                title = filterTitle,
                levelMask = levelFilter,
                categoryMask = categoryFilter,
                onLevelMaskChange = {
                    levelFilter = it
                    expandedKey = null
                },
                onCategoryMaskChange = {
                    categoryFilter = it
                    expandedKey = null
                },
                onReset = {
                    levelFilter = DiagnosticsFilterLevelAll
                    categoryFilter = DiagnosticsFilterCategoryAll
                    expandedKey = null
                },
            )
        },
        actions = {
            TooltipBox(text = reportShareTitle) {
                IconButton(
                    onClick = {
                        withCurrentReport { report ->
                            val prepared =
                                DiagnosticsReportFiles.prepareShare(
                                    context = context,
                                    report = report,
                                )
                            if (prepared == null) {
                                snackbarHostState.showSnackbar(shareFailedMessage)
                                return@withCurrentReport
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
                            DiagnosticsReportFiles.logShareIntent(context, sendIntent, prepared.uri)
                            val chooserIntent =
                                Intent.createChooser(sendIntent, reportShareTitle).apply {
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                            runCatching { context.startActivity(chooserIntent) }
                                .onSuccess { DiagnosticsReportFiles.logChooserLaunch(context) }
                                .onFailure { error ->
                                    DiagnosticsReportFiles.logChooserLaunch(context, error)
                                    DiagnosticsReportFiles.discardShare(context, prepared)
                                    snackbarHostState.showSnackbar(shareFailedMessage)
                                }
                        }
                    },
                    enabled = reportActionsEnabled,
                ) {
                    Icon(MiuixIcons.Share, contentDescription = reportShareTitle)
                }
            }
            TooltipBox(text = reportExportTitle) {
                IconButton(
                    onClick = {
                        exportPickerOpen = true
                        exportLauncher.launch(DiagnosticsReportFiles.suggestedFileName())
                    },
                    enabled = reportActionsEnabled,
                ) {
                    Icon(MiuixIcons.Download, contentDescription = reportExportTitle)
                }
            }
            DiagnosticsMoreMenu(
                title = moreActionsTitle,
                diagnosticsLevel = diagnosticsSettings.level,
                refreshEnabled = !loading && !exportPickerOpen,
                canScrollTop = !loading && !viewCleared && listState.canScrollBackward,
                canScrollBottom = !loading && !viewCleared && listState.canScrollForward,
                canClear = snapshot != null && !viewCleared,
                onDiagnosticsLevelChange = { level ->
                    if (level != diagnosticsSettings.level) {
                        diagnosticsRepository.setLevel(level)
                        requestRefresh()
                    }
                },
                onRefresh = ::requestRefresh,
                onScrollTop = {
                    scope.launch { listState.animateScrollToItem(0) }
                },
                onScrollBottom = {
                    scope.launch {
                        val lastIndex = listState.layoutInfo.totalItemsCount - 1
                        if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
                    }
                },
                onClear = {
                    viewCleared = true
                    expandedKey = null
                    scope.launch { listState.scrollToItem(0) }
                },
            )
        },
        listState = listState,
        pullToRefresh =
            SettingsPullToRefresh(
                refreshing = pullRefreshing,
                onRefresh = ::requestRefresh,
                texts = pullRefreshTexts,
            ),
    ) {
        when {
            viewCleared -> {
                item(key = "diagnostics-state-cleared") {
                    DiagnosticsLogStateCard(
                        text = stringResource(R.string.diagnostics_view_cleared),
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            loading && snapshot == null -> {
                item(key = "diagnostics-state-loading") {
                    DiagnosticsLogStateCard(
                        text = stringResource(R.string.diagnostics_log_loading),
                        loading = true,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            usefulEntries.isEmpty() -> {
                item(key = "diagnostics-state-empty") {
                    DiagnosticsLogStateCard(
                        text =
                            stringResource(
                                if (runtimeEntries.isNotEmpty() && filterActive) {
                                    R.string.diagnostics_filter_empty
                                } else {
                                    R.string.diagnostics_events_empty
                                },
                            ),
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            else -> {
                item(key = "diagnostics-summary") {
                    Text(
                        text =
                            stringResource(
                                R.string.diagnostics_events_summary,
                                usefulEntries.size,
                            ),
                        modifier =
                            Modifier
                                .animateItem()
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(top = 6.dp, bottom = 8.dp),
                        style = MiuixTheme.textStyles.subtitle,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    )
                }
                itemsIndexed(
                    items = usefulEntries,
                    key = { _, entry -> entry.stableKey },
                ) { _, entry ->
                    DiagnosticsUsefulEventCard(
                        context = context,
                        entry = entry,
                        expanded = expandedKey == entry.stableKey,
                        onToggle = {
                            expandedKey =
                                if (expandedKey == entry.stableKey) {
                                    null
                                } else {
                                    entry.stableKey
                                }
                        },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

}

private const val MaxDiagnosticsUsefulEvents = 40

private const val DiagnosticsFilterLevelInfo = 1 shl 0
private const val DiagnosticsFilterLevelWarning = 1 shl 1
private const val DiagnosticsFilterLevelError = 1 shl 2
private const val DiagnosticsFilterLevelAll =
    DiagnosticsFilterLevelInfo or DiagnosticsFilterLevelWarning or DiagnosticsFilterLevelError

private const val DiagnosticsFilterCategoryModuleCompatibility = 1 shl 0
private const val DiagnosticsFilterCategoryNetwork = 1 shl 1
private const val DiagnosticsFilterCategoryDisplayTransition = 1 shl 2
private const val DiagnosticsFilterCategoryPerformance = 1 shl 3
private const val DiagnosticsFilterCategorySettingsMaintenance = 1 shl 4
private const val DiagnosticsFilterCategoryOther = 1 shl 5
private const val DiagnosticsFilterCategoryAll =
    DiagnosticsFilterCategoryModuleCompatibility or
        DiagnosticsFilterCategoryNetwork or
        DiagnosticsFilterCategoryDisplayTransition or
        DiagnosticsFilterCategoryPerformance or
        DiagnosticsFilterCategorySettingsMaintenance or
        DiagnosticsFilterCategoryOther

private fun diagnosticsFilterMatches(
    entry: DiagnosticLogEntry,
    levelMask: Int,
    categoryMask: Int,
): Boolean {
    val levelBit =
        when (entry.level) {
            DiagnosticLogLevel.Warning -> DiagnosticsFilterLevelWarning
            DiagnosticLogLevel.Error,
            DiagnosticLogLevel.Fatal,
            -> DiagnosticsFilterLevelError
            else -> DiagnosticsFilterLevelInfo
        }
    val categoryBit =
        when (entry.category) {
            DiagnosticLogCategory.Module,
            DiagnosticLogCategory.Native,
            -> DiagnosticsFilterCategoryModuleCompatibility
            DiagnosticLogCategory.Network -> DiagnosticsFilterCategoryNetwork
            DiagnosticLogCategory.Display,
            DiagnosticLogCategory.Transition,
            -> DiagnosticsFilterCategoryDisplayTransition
            DiagnosticLogCategory.Performance -> DiagnosticsFilterCategoryPerformance
            DiagnosticLogCategory.Settings -> DiagnosticsFilterCategorySettingsMaintenance
            DiagnosticLogCategory.Other -> DiagnosticsFilterCategoryOther
        }
    return levelMask and levelBit != 0 && categoryMask and categoryBit != 0
}

private fun diagnosticsToggleMask(
    mask: Int,
    bit: Int,
    checked: Boolean,
): Int =
    if (checked) {
        mask or bit
    } else {
        mask and bit.inv()
    }

private data class DiagnosticsFilterOption(
    @StringRes val titleRes: Int,
    val bit: Int,
)

private val DiagnosticsLevelFilterOptions =
    listOf(
        DiagnosticsFilterOption(R.string.diagnostics_filter_info, DiagnosticsFilterLevelInfo),
        DiagnosticsFilterOption(R.string.diagnostics_filter_warning, DiagnosticsFilterLevelWarning),
        DiagnosticsFilterOption(R.string.diagnostics_filter_error, DiagnosticsFilterLevelError),
    )

private val DiagnosticsCategoryFilterOptions =
    listOf(
        DiagnosticsFilterOption(
            R.string.diagnostics_filter_module_compatibility,
            DiagnosticsFilterCategoryModuleCompatibility,
        ),
        DiagnosticsFilterOption(R.string.diagnostics_filter_network, DiagnosticsFilterCategoryNetwork),
        DiagnosticsFilterOption(
            R.string.diagnostics_filter_display_transition,
            DiagnosticsFilterCategoryDisplayTransition,
        ),
        DiagnosticsFilterOption(R.string.diagnostics_filter_performance, DiagnosticsFilterCategoryPerformance),
        DiagnosticsFilterOption(
            R.string.diagnostics_filter_settings_maintenance,
            DiagnosticsFilterCategorySettingsMaintenance,
        ),
        DiagnosticsFilterOption(R.string.diagnostics_filter_other, DiagnosticsFilterCategoryOther),
    )

@Composable
private fun DiagnosticsFilterMenu(
    title: String,
    levelMask: Int,
    categoryMask: Int,
    onLevelMaskChange: (Int) -> Unit,
    onCategoryMaskChange: (Int) -> Unit,
    onReset: () -> Unit,
) {
    val filterActive =
        levelMask != DiagnosticsFilterLevelAll ||
            categoryMask != DiagnosticsFilterCategoryAll
    val levelItems =
        DiagnosticsLevelFilterOptions.map { option ->
            val selected = levelMask and option.bit != 0
            DropdownItem(
                text = stringResource(option.titleRes),
                selected = selected,
                onClick = {
                    onLevelMaskChange(
                        diagnosticsToggleMask(
                            mask = levelMask,
                            bit = option.bit,
                            checked = !selected,
                        ),
                    )
                },
            )
        }
    val categoryItems =
        DiagnosticsCategoryFilterOptions.map { option ->
            val selected = categoryMask and option.bit != 0
            DropdownItem(
                text = stringResource(option.titleRes),
                selected = selected,
                onClick = {
                    onCategoryMaskChange(
                        diagnosticsToggleMask(
                            mask = categoryMask,
                            bit = option.bit,
                            checked = !selected,
                        ),
                    )
                },
            )
        }
    val entries =
        listOf(
            DropdownEntry(
                items =
                    listOf(
                        DropdownItem(
                            text = stringResource(R.string.diagnostics_filter_levels),
                            children = levelItems,
                        ),
                        DropdownItem(
                            text = stringResource(R.string.diagnostics_filter_categories),
                            children = categoryItems,
                        ),
                    ),
            ),
            DropdownEntry(
                items =
                    listOf(
                        DropdownItem(
                            text = stringResource(R.string.diagnostics_filter_reset),
                            enabled = filterActive,
                            onClick = onReset,
                        ),
                    ),
            ),
        )

    TooltipBox(text = title) {
        WindowIconCascadingDropdownMenu(
            entries = entries,
            collapseOnSelection = false,
        ) {
            BadgedBox(
                badge = {
                    AnimatedVisibility(
                        visible = filterActive,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Badge(
                            containerColor = MiuixTheme.colorScheme.primary,
                        )
                    }
                },
            ) {
                Icon(
                    MiuixIcons.Filter,
                    contentDescription = title,
                )
            }
        }
    }
}

@Composable
private fun DiagnosticsMoreMenu(
    title: String,
    diagnosticsLevel: DiagnosticsLevel,
    refreshEnabled: Boolean,
    canScrollTop: Boolean,
    canScrollBottom: Boolean,
    canClear: Boolean,
    onDiagnosticsLevelChange: (DiagnosticsLevel) -> Unit,
    onRefresh: () -> Unit,
    onScrollTop: () -> Unit,
    onScrollBottom: () -> Unit,
    onClear: () -> Unit,
) {
    val currentLevelLabel =
        stringResource(
            if (diagnosticsLevel == DiagnosticsLevel.Detailed) {
                R.string.diagnostics_mode_detailed
            } else {
                R.string.diagnostics_mode_basic
            },
        )
    val levelItems =
        DiagnosticsLevel.entries.map { level ->
            DropdownItem(
                text =
                    stringResource(
                        if (level == DiagnosticsLevel.Detailed) {
                            R.string.diagnostics_mode_detailed
                        } else {
                            R.string.diagnostics_mode_basic
                        },
                    ),
                selected = diagnosticsLevel == level,
                onClick = { onDiagnosticsLevelChange(level) },
            )
        }
    val entries =
        listOf(
            DropdownEntry(
                items =
                    listOf(
                        DropdownItem(
                            text = stringResource(R.string.diagnostics_mode_title),
                            summary = currentLevelLabel,
                            children = levelItems,
                        ),
                        DropdownItem(
                            text = stringResource(R.string.diagnostics_refresh),
                            enabled = refreshEnabled,
                            onClick = onRefresh,
                        ),
                    ),
            ),
            DropdownEntry(
                items =
                    listOf(
                        DropdownItem(
                            text = stringResource(R.string.diagnostics_scroll_top),
                            enabled = canScrollTop,
                            onClick = onScrollTop,
                        ),
                        DropdownItem(
                            text = stringResource(R.string.diagnostics_scroll_bottom),
                            enabled = canScrollBottom,
                            onClick = onScrollBottom,
                        ),
                    ),
            ),
            DropdownEntry(
                items =
                    listOf(
                        DropdownItem(
                            text = stringResource(R.string.diagnostics_clear_view),
                            enabled = canClear,
                            onClick = onClear,
                        ),
                    ),
            ),
        )

    TooltipBox(text = title) {
        WindowIconCascadingDropdownMenu(entries = entries) {
            Icon(MiuixIcons.More, contentDescription = title)
        }
    }
}

@Composable
private fun DiagnosticsUsefulEventCard(
    context: Context,
    entry: DiagnosticLogEntry,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = diagnosticLogTitle(context, entry)
    val summary = diagnosticLogSummary(context, entry)
    val category = diagnosticLogCategoryLabel(context, entry.category)

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 6.dp)
                .heightIn(min = 86.dp),
        insideMargin = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
        showIndication = true,
        onClick = onToggle,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DiagnosticsLogLevelTag(entry.level)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = category,
                modifier = Modifier.weight(1f),
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            entry.timeText?.let { time ->
                Text(
                    text = time,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style =
                MiuixTheme.textStyles.headline1.copy(
                    fontWeight = FontWeight.Medium,
                ),
            color = MiuixTheme.colorScheme.onSurfaceContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = summary,
            modifier = Modifier.animateContentSize(),
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            maxLines = if (expanded) 2 else 1,
            overflow = TextOverflow.Ellipsis,
        )

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        ) {
            Column {
                Spacer(modifier = Modifier.height(10.dp))
                DiagnosticLogDetailRow(
                    label = "Event",
                    value = entry.event ?: "—",
                )
                entry.component?.let { component ->
                    DiagnosticLogDetailRow(
                        label = "Component",
                        value = component,
                    )
                }
                entry.state?.let { state ->
                    DiagnosticLogDetailRow(
                        label = "State",
                        value = state,
                    )
                }

                entry.fields
                    .filterKeys { key -> key !in DiagnosticLogMetadataFields }
                    .forEach { (key, value) ->
                        DiagnosticLogDetailRow(label = key, value = value)
                    }
            }
        }
    }
}

@Composable
private fun DiagnosticsLogStateCard(
    text: String,
    loading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
        insideMargin = PaddingValues(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                InfiniteProgressIndicator(
                    color = MiuixTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = text,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )
        }
    }
}

@Composable
private fun DiagnosticsLogLevelTag(level: DiagnosticLogLevel) {
    val colors = MiuixTheme.colorScheme
    val warningColor = RuntimeWarningAccent
    val containerColor =
        when (level) {
            DiagnosticLogLevel.Error,
            DiagnosticLogLevel.Fatal,
            -> colors.errorContainer
            DiagnosticLogLevel.Warning -> warningColor.copy(alpha = 0.14f)
            DiagnosticLogLevel.Info -> colors.tertiaryContainer
            else -> colors.secondaryContainerVariant
        }
    val contentColor =
        when (level) {
            DiagnosticLogLevel.Error,
            DiagnosticLogLevel.Fatal,
            -> colors.error
            DiagnosticLogLevel.Warning -> warningColor
            DiagnosticLogLevel.Info -> colors.onTertiaryContainer
            else -> colors.onSurfaceContainerVariant
        }

    Surface(
        modifier = Modifier.heightIn(min = 20.dp),
        shape = RoundedCornerShape(5.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = diagnosticLogLevelLabel(level),
                style =
                    MiuixTheme.textStyles.footnote2.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DiagnosticLogDetailRow(
    label: String,
    value: String,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
    ) {
        Text(
            text = label,
            style =
                MiuixTheme.textStyles.footnote2.copy(
                    fontWeight = FontWeight.Medium,
                ),
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = value,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceContainer,
        )
    }
}

private fun diagnosticLogIsRuntimeEntry(entry: DiagnosticLogEntry): Boolean {
    if (
        entry.level == DiagnosticLogLevel.Warning ||
        entry.level == DiagnosticLogLevel.Error ||
        entry.level == DiagnosticLogLevel.Fatal
    ) {
        return true
    }
    if (!entry.structured) {
        return false
    }
    return entry.event in DiagnosticRuntimeEvents
}

private fun diagnosticLogLevelLabel(level: DiagnosticLogLevel): String =
    when (level) {
        DiagnosticLogLevel.Verbose -> "VERBOSE"
        DiagnosticLogLevel.Debug -> "DEBUG"
        DiagnosticLogLevel.Info -> "INFO"
        DiagnosticLogLevel.Warning -> "WARN"
        DiagnosticLogLevel.Error -> "ERROR"
        DiagnosticLogLevel.Fatal -> "FATAL"
        DiagnosticLogLevel.Unknown -> "LOG"
    }

private fun diagnosticLogCategoryLabel(
    context: Context,
    category: DiagnosticLogCategory,
): String =
    context.getString(
        when (category) {
            DiagnosticLogCategory.Module -> R.string.diagnostics_log_category_module
            DiagnosticLogCategory.Network -> R.string.diagnostics_log_category_network
            DiagnosticLogCategory.Display -> R.string.diagnostics_log_category_display
            DiagnosticLogCategory.Native -> R.string.diagnostics_log_category_native
            DiagnosticLogCategory.Transition -> R.string.diagnostics_log_category_transition
            DiagnosticLogCategory.Performance -> R.string.diagnostics_log_category_performance
            DiagnosticLogCategory.Settings -> R.string.diagnostics_log_category_settings
            DiagnosticLogCategory.Other -> R.string.diagnostics_log_category_other
        },
    )

private fun diagnosticLogTitle(
    context: Context,
    entry: DiagnosticLogEntry,
): String {
    val res =
        when (entry.event) {
            "module.loaded" -> R.string.diagnostics_log_event_module_loaded
            "module.reloaded" -> R.string.diagnostics_log_event_module_reloaded
            "compatibility.probe",
            "compatibility.revalidated",
            -> R.string.diagnostics_log_event_compatibility
            "hook.install",
            "hook.replace",
            -> R.string.diagnostics_log_event_hook
            "source.install",
            "source.attach",
            -> R.string.diagnostics_log_event_source
            "runtime.attach" -> R.string.diagnostics_log_event_runtime_attach
            "runtime.teardown" -> R.string.diagnostics_log_event_runtime_teardown
            "runtimePreferences.bind" -> R.string.diagnostics_log_event_runtime_preferences
            "diagnostics.bind" -> R.string.diagnostics_log_event_diagnostics_bind
            "host.capture" -> R.string.diagnostics_log_event_host_capture
            "host.restore" -> R.string.diagnostics_log_event_host_restore
            "session.attach" -> R.string.diagnostics_log_event_session_attach
            "presentation.cutover" -> R.string.diagnostics_log_event_presentation_cutover
            "presentation.failNative" -> R.string.diagnostics_log_event_fail_native
            "presentation.resolve" -> R.string.diagnostics_log_event_presentation_resolve
            "renderer.attach" -> R.string.diagnostics_log_event_renderer_attach
            "diagnostics.snapshot" -> R.string.diagnostics_log_event_diagnostics_snapshot
            "diagnostics.level" -> R.string.diagnostics_log_event_diagnostics_level
            "pipeline.latency" -> R.string.diagnostics_log_event_pipeline_latency
            "visualSettings.changed" -> R.string.diagnostics_log_event_visual_settings
            "featureSettings.changed" -> R.string.diagnostics_log_event_feature_settings
            "hotReload.prepare" -> R.string.diagnostics_log_event_hot_reload_prepare
            "hotReload.generationHandoff" -> R.string.diagnostics_log_event_hot_reload_handoff
            "hotReload.migration" -> R.string.diagnostics_log_event_hot_reload_migration
            "hotReload.restore" -> R.string.diagnostics_log_event_hot_reload_restore
            "hotReload.complete" -> R.string.diagnostics_log_event_hot_reload
            "mobile.recovery" -> R.string.diagnostics_log_event_mobile_recovery
            "scene.stableFamily" -> R.string.diagnostics_log_event_scene_stable
            "aod.state" -> R.string.diagnostics_log_event_aod_state
            "aod.target" -> R.string.diagnostics_log_event_aod_target
            "connectivity" -> R.string.diagnostics_log_event_connectivity
            "networkPipeline.wifi.iconEvent" -> R.string.diagnostics_log_event_wifi_icon
            "statusIconPresentation" -> R.string.diagnostics_log_event_status_icon_presentation
            "tintCommit" -> R.string.diagnostics_log_event_tint_commit
            "homeRenderTint" -> R.string.diagnostics_log_event_home_tint
            else -> null
        }
    return res?.let(context::getString)
        ?: entry.event
        ?: context.getString(R.string.diagnostics_log_event_generic)
}

private fun diagnosticLogSummary(
    context: Context,
    entry: DiagnosticLogEntry,
): String {
    if (entry.event == "connectivity") {
        return buildList {
            entry.fields["transport"]?.let { add(diagnosticTransportLabel(it)) }
            if (entry.fields["validated"] == "true") {
                add(context.getString(R.string.diagnostics_log_summary_validated))
            }
            if (entry.fields["internetCapability"] == "true") {
                add(context.getString(R.string.diagnostics_log_summary_internet))
            }
            entry.fields["mobileDataEnabled"]?.let { enabled ->
                add(
                    context.getString(
                        if (enabled == "true") {
                            R.string.diagnostics_log_summary_mobile_data_on
                        } else {
                            R.string.diagnostics_log_summary_mobile_data_off
                        },
                    ),
                )
            }
        }.joinToString(context.getString(R.string.diagnostics_log_summary_separator)).ifBlank { entry.message }
    }

    if (entry.event == "pipeline.latency") {
        return buildList {
            entry.fields["sourceToDrawUs"]?.toLongOrNull()?.let { micros ->
                add(
                    context.getString(
                        R.string.diagnostics_log_summary_total_time,
                        formatDiagnosticMicros(micros),
                    ),
                )
            }
            entry.fields["source"]?.let { source ->
                add(
                    context.getString(
                        R.string.diagnostics_log_summary_source,
                        diagnosticTransportLabel(source),
                    ),
                )
            }
        }.joinToString(context.getString(R.string.diagnostics_log_summary_separator)).ifBlank { entry.message }
    }

    if (entry.event in setOf("tintCommit", "homeRenderTint")) {
        return buildList {
            (entry.fields["applied"] ?: entry.fields["statusIcon"])?.let(::add)
            entry.fields["source"]?.let { source ->
                add(
                    context.getString(
                        R.string.diagnostics_log_summary_source,
                        source,
                    ),
                )
            }
        }.joinToString(context.getString(R.string.diagnostics_log_summary_separator)).ifBlank { entry.message }
    }

    val summary =
        buildList {
            entry.state?.let { add(diagnosticStateLabel(context, it)) }
            entry.fields["source"]?.let { source ->
                add(
                    context.getString(
                        R.string.diagnostics_log_summary_source,
                        diagnosticTransportLabel(source),
                    ),
                )
            }
            entry.fields["reason"]?.let { add(it) }
        }.take(3)
            .joinToString(context.getString(R.string.diagnostics_log_summary_separator))

    if (summary.isNotBlank()) {
        return summary
    }
    return if (entry.structured || entry.event != null) {
        context.getString(R.string.diagnostics_log_summary_recorded)
    } else {
        entry.message
    }
}

private fun diagnosticStateLabel(
    context: Context,
    state: String,
): String =
    when (state.lowercase()) {
        "ready" -> context.getString(R.string.diagnostics_log_state_ready)
        "observed" -> context.getString(R.string.diagnostics_log_state_observed)
        "disabled" -> context.getString(R.string.diagnostics_log_state_disabled)
        "unavailable" -> context.getString(R.string.diagnostics_log_state_unavailable)
        "partial" -> context.getString(R.string.diagnostics_log_state_partial)
        "error" -> context.getString(R.string.diagnostics_log_state_error)
        "scheduled" -> context.getString(R.string.diagnostics_log_state_scheduled)
        "restart-required" -> context.getString(R.string.diagnostics_log_state_restart_required)
        else -> state
    }

private fun diagnosticTransportLabel(value: String): String =
    when (value.lowercase()) {
        "wifi" -> "Wi-Fi"
        "mobile" -> "Mobile"
        "hotreload",
        "hotreloadrestore",
        -> "Hot Reload"
        else -> value
    }

private fun formatDiagnosticMicros(micros: Long): String =
    if (micros >= 1_000L) {
        String.format(Locale.US, "%.2f ms", micros / 1_000.0)
    } else {
        "$micros μs"
    }

private val DiagnosticRuntimeEvents =
    setOf(
        "module.loaded",
        "module.reloaded",
        "compatibility.probe",
        "compatibility.revalidated",
        "hook.install",
        "hook.replace",
        "source.install",
        "source.attach",
        "runtime.attach",
        "runtime.teardown",
        "runtimePreferences.bind",
        "diagnostics.bind",
        "diagnostics.level",
        "host.capture",
        "host.restore",
        "session.attach",
        "presentation.cutover",
        "presentation.failNative",
        "renderer.attach",
        "featureSettings.changed",
        "visualSettings.changed",
        "hotReload.prepare",
        "hotReload.generationHandoff",
        "hotReload.migration",
        "hotReload.restore",
        "hotReload.complete",
        "mobile.recovery",
        "scene.stableFamily",
        "aod.state",
        "aod.target",
    )

private val DiagnosticLogMetadataFields =
    setOf(
        "sequence",
        "sessionId",
        "uptimeMs",
        "traceId",
        "healthSnapshot",
        "sampling",
    )

private const val ABOUT_PROJECT_URL = "https://github.com/CHS-Haple/Guiyuan"
private const val ABOUT_LICENSE_URL = "https://github.com/CHS-Haple/Guiyuan/blob/main/LICENSE"

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
    val environment by
        produceState(
            initialValue = RuntimeEnvironmentInfo.basic(),
            key1 = context.applicationContext,
        ) {
            value = RuntimeEnvironmentInfo.resolve(context.applicationContext)
        }

    SettingsPage(
        title = stringResource(R.string.about_title),
        onBack = onBack,
    ) {
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
                onClick = { uriHandler.openUri(ABOUT_PROJECT_URL) },
            )
            ArrowPreference(
                title = stringResource(R.string.about_open_source_license_title),
                summary = stringResource(R.string.about_open_source_license_summary),
                startAction = {
                    SemanticLeadingIcon(
                        iconRes = R.drawable.ic_material_symbol_license,
                    )
                },
                onClick = { uriHandler.openUri(ABOUT_LICENSE_URL) },
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
                    environment.deviceName.trim(),
                    environment.model
                        .trim()
                        .takeIf { model ->
                            model.isNotBlank() &&
                                !model.equals(environment.deviceName.trim(), ignoreCase = true)
                        },
                )
                    .filterNotNull()
                    .filter(String::isNotBlank)
                    .ifEmpty { listOf(unavailable) }
                    .joinToString(separator = " ")
            val androidSummary =
                if (environment.androidVersion.isNotBlank()) {
                    buildString {
                        append("Android ")
                        append(environment.androidVersion)
                        append(' ')
                        append("API ")
                        append(environment.sdk)
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
                summary = environment.osVersion.ifBlank { unavailable },
                startAction = {
                    SemanticLeadingIcon(
                        iconRes = R.drawable.ic_material_symbol_layers,
                    )
                },
            )
            BasicComponent(
                title = stringResource(R.string.systemui_version_label),
                summary = environment.systemUiVersionName.ifBlank { unavailable },
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
                    name = "JUnit 4",
                    version = BuildConfig.JUNIT_VERSION,
                    license = "EPL-1.0",
                    upstreamUrl = "https://github.com/junit-team/junit4",
                ),
                AboutDependency(
                    name = "Gradle Wrapper",
                    version = BuildConfig.GRADLE_VERSION,
                    license = "Apache-2.0",
                    upstreamUrl = "https://github.com/gradle/gradle",
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
private fun SemanticLeadingIcon(
    @DrawableRes iconRes: Int,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            colorFilter =
                ColorFilter.tint(
                    MiuixTheme.colorScheme.onSurfaceContainer.copy(
                        alpha = if (enabled) 1f else 0.38f,
                    ),
                ),
        )
    }
}

private data class SettingsPullToRefresh(
    val refreshing: Boolean,
    val onRefresh: () -> Unit,
    val texts: List<String>,
)

@Composable
private fun SettingsPage(
    title: String,
    onBack: () -> Unit,
    snackbarHost: @Composable () -> Unit = {},
    navigationActions: @Composable RowScope.() -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    listState: LazyListState? = null,
    pullToRefresh: SettingsPullToRefresh? = null,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val resolvedListState = listState ?: rememberLazyListState()
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    MiuixIcons.Back,
                                    contentDescription = stringResource(R.string.back),
                                )
                            }
                            navigationActions()
                        }
                    },
                    actions = actions,
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
            val contentPadding =
                pageContentPadding(
                    innerPadding = paddingValues,
                    extraBottom = 12.dp,
                )

            @Composable
            fun SettingsList() {
                LazyColumn(
                    state = resolvedListState,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = contentPadding,
                    content = content,
                )
            }

            if (pullToRefresh != null) {
                PullToRefresh(
                    isRefreshing = pullToRefresh.refreshing,
                    onRefresh = pullToRefresh.onRefresh,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    topAppBarScrollBehavior = scrollBehavior,
                    refreshTexts = pullToRefresh.texts,
                ) {
                    SettingsList()
                }
            } else {
                SettingsList()
            }
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
