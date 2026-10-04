package com.chaners.guiyuan.ui.screens

import android.content.ClipData
import android.content.Context
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
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
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Badge
import top.yukonga.miuix.kmp.basic.BadgedBox
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
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
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
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
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.preference.CheckboxPreference
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
    var viewCleared by rememberSaveable { mutableStateOf(false) }
    var expandedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var refreshGeneration by rememberSaveable { mutableIntStateOf(0) }
    var reportInProgress by rememberSaveable { mutableStateOf(false) }
    var exportPickerOpen by rememberSaveable { mutableStateOf(false) }
    var filterSheetVisible by rememberSaveable { mutableStateOf(false) }
    var appliedLevelFilter by rememberSaveable {
        mutableIntStateOf(DiagnosticsFilterLevelAll)
    }
    var appliedCategoryFilter by rememberSaveable {
        mutableIntStateOf(DiagnosticsFilterCategoryAll)
    }
    var draftLevelFilter by rememberSaveable {
        mutableIntStateOf(DiagnosticsFilterLevelAll)
    }
    var draftCategoryFilter by rememberSaveable {
        mutableIntStateOf(DiagnosticsFilterCategoryAll)
    }

    val exportSucceededMessage = stringResource(R.string.diagnostic_report_exported)
    val exportFailedMessage = stringResource(R.string.diagnostic_report_export_failed)
    val shareFailedMessage = stringResource(R.string.diagnostic_report_share_failed)
    val reportShareTitle = stringResource(R.string.share_diagnostic_report)
    val reportExportTitle = stringResource(R.string.export_diagnostic_report)
    val moreActionsTitle = stringResource(R.string.diagnostics_more_actions)
    val filterTitle = stringResource(R.string.diagnostics_filter)

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
        snapshot = DiagnosticsSnapshotProvider.capture(context.applicationContext)
        expandedKey = null
        viewCleared = false
        loading = false
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
                .take(MaxDiagnosticsUsefulEvents)
        }
    val usefulEntries =
        runtimeEntries.filter { entry ->
            diagnosticsFilterMatches(
                entry = entry,
                levelMask = appliedLevelFilter,
                categoryMask = appliedCategoryFilter,
            )
        }
    val filterActive =
        appliedLevelFilter != DiagnosticsFilterLevelAll ||
            appliedCategoryFilter != DiagnosticsFilterCategoryAll
    SettingsPage(
        title = stringResource(R.string.diagnostics_title),
        onBack = onBack,
        snackbarHost = { SnackbarHost(state = snackbarHostState) },
        navigationActions = {
            TooltipBox(text = filterTitle) {
                IconButton(
                    onClick = {
                        draftLevelFilter = appliedLevelFilter
                        draftCategoryFilter = appliedCategoryFilter
                        filterSheetVisible = true
                    },
                ) {
                    if (filterActive) {
                        BadgedBox(
                            badge = { Badge() },
                        ) {
                            Icon(
                                MiuixIcons.Filter,
                                contentDescription = filterTitle,
                            )
                        }
                    } else {
                        Icon(
                            MiuixIcons.Filter,
                            contentDescription = filterTitle,
                        )
                    }
                }
            }
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
                        refreshGeneration += 1
                    }
                },
                onRefresh = { refreshGeneration += 1 },
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
    ) {
        when {
            viewCleared -> {
                item {
                    DiagnosticsLogStateCard(
                        text = stringResource(R.string.diagnostics_view_cleared),
                    )
                }
            }
            loading -> {
                item {
                    DiagnosticsLogStateCard(
                        text = stringResource(R.string.diagnostics_log_loading),
                    )
                }
            }
            usefulEntries.isEmpty() -> {
                item {
                    DiagnosticsLogStateCard(
                        text =
                            stringResource(
                                if (runtimeEntries.isNotEmpty() && filterActive) {
                                    R.string.diagnostics_filter_empty
                                } else {
                                    R.string.diagnostics_events_empty
                                },
                            ),
                    )
                }
            }
            else -> {
                item {
                    Text(
                        text =
                            stringResource(
                                R.string.diagnostics_events_summary,
                                usefulEntries.size,
                            ),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(top = 6.dp, bottom = 8.dp),
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    )
                }
                itemsIndexed(
                    items = usefulEntries,
                    key = { index, entry -> entry.stableKey + ":" + index },
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
                    )
                }
            }
        }
    }

    DiagnosticsFilterSheet(
        show = filterSheetVisible,
        levelMask = draftLevelFilter,
        categoryMask = draftCategoryFilter,
        onLevelMaskChange = { draftLevelFilter = it },
        onCategoryMaskChange = { draftCategoryFilter = it },
        onReset = {
            draftLevelFilter = DiagnosticsFilterLevelAll
            draftCategoryFilter = DiagnosticsFilterCategoryAll
        },
        onApply = {
            appliedLevelFilter = draftLevelFilter
            appliedCategoryFilter = draftCategoryFilter
            expandedKey = null
            filterSheetVisible = false
        },
        onDismiss = {
            filterSheetVisible = false
        },
    )
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

private val DiagnosticsSideSubmenuPositionProvider =
    object : PopupPositionProvider {
        override fun calculatePosition(
            anchorBounds: IntRect,
            windowBounds: IntRect,
            layoutDirection: LayoutDirection,
            popupContentSize: IntSize,
            popupMargin: IntRect,
            alignment: PopupPositionProvider.Align,
        ): IntOffset {
            val rightX = anchorBounds.right + popupMargin.right
            val leftX = anchorBounds.left - popupContentSize.width - popupMargin.left
            val fitsRight = rightX + popupContentSize.width <= windowBounds.right
            val fitsLeft = leftX >= windowBounds.left
            val preferRight = layoutDirection == LayoutDirection.Ltr
            val x =
                when {
                    preferRight && fitsRight -> rightX
                    !preferRight && fitsLeft -> leftX
                    fitsLeft -> leftX
                    else -> rightX.coerceAtMost(windowBounds.right - popupContentSize.width)
                }
            val maxY = (windowBounds.bottom - popupContentSize.height).coerceAtLeast(windowBounds.top)
            return IntOffset(
                x = x.coerceIn(windowBounds.left, (windowBounds.right - popupContentSize.width).coerceAtLeast(windowBounds.left)),
                y = anchorBounds.top.coerceIn(windowBounds.top, maxY),
            )
        }

        override fun getMargins(): PaddingValues =
            PaddingValues(horizontal = 8.dp)
    }

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

@Composable
private fun DiagnosticsFilterSheet(
    show: Boolean,
    levelMask: Int,
    categoryMask: Int,
    onLevelMaskChange: (Int) -> Unit,
    onCategoryMaskChange: (Int) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    OverlayBottomSheet(
        show = show,
        title = stringResource(R.string.diagnostics_filter),
        onDismissRequest = onDismiss,
    ) {
        SmallTitle(
            text = stringResource(R.string.diagnostics_filter_levels),
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        )
        Card(
            modifier = Modifier.padding(bottom = 10.dp),
        ) {
            DiagnosticsFilterCheckbox(
                title = stringResource(R.string.diagnostics_filter_info),
                checked = levelMask and DiagnosticsFilterLevelInfo != 0,
                onCheckedChange = { checked ->
                    onLevelMaskChange(
                        diagnosticsToggleMask(
                            levelMask,
                            DiagnosticsFilterLevelInfo,
                            checked,
                        ),
                    )
                },
            )
            DiagnosticsFilterCheckbox(
                title = stringResource(R.string.diagnostics_filter_warning),
                checked = levelMask and DiagnosticsFilterLevelWarning != 0,
                onCheckedChange = { checked ->
                    onLevelMaskChange(
                        diagnosticsToggleMask(
                            levelMask,
                            DiagnosticsFilterLevelWarning,
                            checked,
                        ),
                    )
                },
            )
            DiagnosticsFilterCheckbox(
                title = stringResource(R.string.diagnostics_filter_error),
                checked = levelMask and DiagnosticsFilterLevelError != 0,
                onCheckedChange = { checked ->
                    onLevelMaskChange(
                        diagnosticsToggleMask(
                            levelMask,
                            DiagnosticsFilterLevelError,
                            checked,
                        ),
                    )
                },
            )
        }

        SmallTitle(
            text = stringResource(R.string.diagnostics_filter_categories),
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        )
        Card(
            modifier = Modifier.padding(bottom = 12.dp),
        ) {
            DiagnosticsFilterCheckbox(
                title = stringResource(R.string.diagnostics_filter_module_compatibility),
                checked = categoryMask and DiagnosticsFilterCategoryModuleCompatibility != 0,
                onCheckedChange = { checked ->
                    onCategoryMaskChange(
                        diagnosticsToggleMask(
                            categoryMask,
                            DiagnosticsFilterCategoryModuleCompatibility,
                            checked,
                        ),
                    )
                },
            )
            DiagnosticsFilterCheckbox(
                title = stringResource(R.string.diagnostics_filter_network),
                checked = categoryMask and DiagnosticsFilterCategoryNetwork != 0,
                onCheckedChange = { checked ->
                    onCategoryMaskChange(
                        diagnosticsToggleMask(
                            categoryMask,
                            DiagnosticsFilterCategoryNetwork,
                            checked,
                        ),
                    )
                },
            )
            DiagnosticsFilterCheckbox(
                title = stringResource(R.string.diagnostics_filter_display_transition),
                checked = categoryMask and DiagnosticsFilterCategoryDisplayTransition != 0,
                onCheckedChange = { checked ->
                    onCategoryMaskChange(
                        diagnosticsToggleMask(
                            categoryMask,
                            DiagnosticsFilterCategoryDisplayTransition,
                            checked,
                        ),
                    )
                },
            )
            DiagnosticsFilterCheckbox(
                title = stringResource(R.string.diagnostics_filter_performance),
                checked = categoryMask and DiagnosticsFilterCategoryPerformance != 0,
                onCheckedChange = { checked ->
                    onCategoryMaskChange(
                        diagnosticsToggleMask(
                            categoryMask,
                            DiagnosticsFilterCategoryPerformance,
                            checked,
                        ),
                    )
                },
            )
            DiagnosticsFilterCheckbox(
                title = stringResource(R.string.diagnostics_filter_settings_maintenance),
                checked = categoryMask and DiagnosticsFilterCategorySettingsMaintenance != 0,
                onCheckedChange = { checked ->
                    onCategoryMaskChange(
                        diagnosticsToggleMask(
                            categoryMask,
                            DiagnosticsFilterCategorySettingsMaintenance,
                            checked,
                        ),
                    )
                },
            )
            DiagnosticsFilterCheckbox(
                title = stringResource(R.string.diagnostics_filter_other),
                checked = categoryMask and DiagnosticsFilterCategoryOther != 0,
                onCheckedChange = { checked ->
                    onCategoryMaskChange(
                        diagnosticsToggleMask(
                            categoryMask,
                            DiagnosticsFilterCategoryOther,
                            checked,
                        ),
                    )
                },
            )
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                text = stringResource(R.string.diagnostics_filter_reset),
                onClick = onReset,
            )
            TextButton(
                text = stringResource(R.string.diagnostics_filter_done),
                onClick = onApply,
            )
        }
    }
}

@Composable
private fun DiagnosticsFilterCheckbox(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    CheckboxPreference(
        title = title,
        checked = checked,
        onCheckedChange = onCheckedChange,
    )
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
    var showPrimary by remember { mutableStateOf(false) }
    var showLevel by remember { mutableStateOf(false) }
    val currentLevelLabel =
        stringResource(
            if (diagnosticsLevel == DiagnosticsLevel.Detailed) {
                R.string.diagnostics_mode_detailed
            } else {
                R.string.diagnostics_mode_basic
            },
        )
    val primaryItems = 5

    TooltipBox(text = title) {
        Box {
            IconButton(
                onClick = {
                    showPrimary = !showPrimary
                    if (!showPrimary) showLevel = false
                },
            ) {
                Icon(MiuixIcons.More, contentDescription = title)
            }
            OverlayListPopup(
                show = showPrimary,
                alignment = PopupPositionProvider.Align.TopEnd,
                onDismissRequest = {
                    showLevel = false
                    showPrimary = false
                },
            ) {
                ListPopupColumn {
                    Box {
                        DropdownImpl(
                            item =
                                DropdownItem(
                                    text = stringResource(R.string.diagnostics_mode_title),
                                    summary = currentLevelLabel,
                                ),
                            optionSize = primaryItems,
                            isSelected = false,
                            index = 0,
                            hasSubmenu = true,
                            isFirst = true,
                            isLast = false,
                            onSelectedIndexChange = {
                                showLevel = true
                            },
                        )
                        OverlayListPopup(
                            show = showLevel,
                            popupPositionProvider = DiagnosticsSideSubmenuPositionProvider,
                            alignment = PopupPositionProvider.Align.TopStart,
                            enableWindowDim = false,
                            minWidth = 160.dp,
                            onDismissRequest = { showLevel = false },
                        ) {
                            ListPopupColumn {
                                DiagnosticsLevel.entries.forEachIndexed { index, level ->
                                    DropdownImpl(
                                        text =
                                            stringResource(
                                                if (level == DiagnosticsLevel.Detailed) {
                                                    R.string.diagnostics_mode_detailed
                                                } else {
                                                    R.string.diagnostics_mode_basic
                                                },
                                            ),
                                        optionSize = DiagnosticsLevel.entries.size,
                                        isSelected = diagnosticsLevel == level,
                                        index = index,
                                        onSelectedIndexChange = {
                                            onDiagnosticsLevelChange(level)
                                            showLevel = false
                                            showPrimary = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                    DropdownImpl(
                        text = stringResource(R.string.diagnostics_refresh),
                        optionSize = primaryItems,
                        isSelected = false,
                        index = 1,
                        enabled = refreshEnabled,
                        onSelectedIndexChange = {
                            onRefresh()
                            showPrimary = false
                        },
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    DropdownImpl(
                        text = stringResource(R.string.diagnostics_scroll_top),
                        optionSize = primaryItems,
                        isSelected = false,
                        index = 2,
                        enabled = canScrollTop,
                        onSelectedIndexChange = {
                            onScrollTop()
                            showPrimary = false
                        },
                    )
                    DropdownImpl(
                        text = stringResource(R.string.diagnostics_scroll_bottom),
                        optionSize = primaryItems,
                        isSelected = false,
                        index = 3,
                        enabled = canScrollBottom,
                        onSelectedIndexChange = {
                            onScrollBottom()
                            showPrimary = false
                        },
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    DropdownImpl(
                        text = stringResource(R.string.diagnostics_clear_view),
                        optionSize = primaryItems,
                        isSelected = false,
                        index = 4,
                        enabled = canClear,
                        onSelectedIndexChange = {
                            onClear()
                            showPrimary = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsUsefulEventCard(
    context: Context,
    entry: DiagnosticLogEntry,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val title = diagnosticLogTitle(context, entry)
    val summary = diagnosticLogSummary(context, entry)
    val category = diagnosticLogCategoryLabel(context, entry.category)

    Card(
        modifier =
            Modifier
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
            DiagnosticsLogLevelBadge(entry.level)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = category,
                modifier = Modifier.weight(1f),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            entry.timeText?.let { time ->
                Text(
                    text = time,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurfaceContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = summary,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            maxLines = if (expanded) 2 else 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (expanded) {
            Spacer(modifier = Modifier.height(10.dp))
            DiagnosticLogDetailRow(
                label = stringResource(R.string.diagnostics_log_detail_event),
                value = entry.event ?: "—",
            )
            entry.component?.let { component ->
                DiagnosticLogDetailRow(
                    label = stringResource(R.string.diagnostics_log_detail_component),
                    value = component,
                )
            }
            entry.state?.let { state ->
                DiagnosticLogDetailRow(
                    label = stringResource(R.string.diagnostics_log_detail_state),
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

@Composable
private fun DiagnosticsLogStateCard(text: String) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
        insideMargin = PaddingValues(18.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )
        }
    }
}

@Composable
private fun DiagnosticsLogLevelBadge(level: DiagnosticLogLevel) {
    val colors = MiuixTheme.colorScheme
    val containerColor =
        when (level) {
            DiagnosticLogLevel.Error,
            DiagnosticLogLevel.Fatal,
            -> colors.errorContainer
            DiagnosticLogLevel.Warning -> colors.tertiaryContainer
            DiagnosticLogLevel.Info -> colors.primaryContainer
            else -> colors.secondaryContainerVariant
        }
    val contentColor =
        when (level) {
            DiagnosticLogLevel.Error,
            DiagnosticLogLevel.Fatal,
            -> colors.onErrorContainer
            DiagnosticLogLevel.Warning -> colors.onTertiaryContainer
            DiagnosticLogLevel.Info -> colors.onPrimaryContainer
            else -> colors.onSecondaryContainerVariant
        }

    Badge(
        containerColor = containerColor,
        contentColor = contentColor,
    ) {
        Text(diagnosticLogLevelLabel(level))
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
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = value,
            style = MiuixTheme.textStyles.body2,
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
    return if (entry.structured) {
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

@Composable
internal fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
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
private fun SettingsPage(
    title: String,
    onBack: () -> Unit,
    snackbarHost: @Composable () -> Unit = {},
    navigationActions: @Composable RowScope.() -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    listState: LazyListState? = null,
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
            LazyColumn(
                state = resolvedListState,
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
