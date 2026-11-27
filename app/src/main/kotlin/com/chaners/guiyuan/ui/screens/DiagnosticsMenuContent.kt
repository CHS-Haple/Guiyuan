package com.chaners.guiyuan.ui.screens

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.ui.icons.Os4GlassFilter
import com.chaners.guiyuan.ui.icons.Os4GlassMore
import com.chaners.guiyuan.ui.icons.Os4GlassShare
import com.chaners.guiyuan.settings.DiagLevel
import top.yukonga.miuix.kmp.basic.Badge
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.BadgedBox
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.menu.WindowIconCascadingDropdownMenu
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal const val LEVEL_INFO = 1 shl 0
internal const val LEVEL_WARN = 1 shl 1
internal const val LEVEL_ERROR = 1 shl 2
internal const val LEVEL_ALL =
    LEVEL_INFO or LEVEL_WARN or LEVEL_ERROR

internal const val CAT_MODULE = 1 shl 0
internal const val CAT_NETWORK = 1 shl 1
internal const val CAT_DISPLAY = 1 shl 2
internal const val CAT_PERF = 1 shl 3
internal const val CAT_SETTINGS = 1 shl 4
internal const val CAT_OTHER = 1 shl 5
internal const val CAT_ALL =
    CAT_MODULE or
        CAT_NETWORK or
        CAT_DISPLAY or
        CAT_PERF or
        CAT_SETTINGS or
        CAT_OTHER

private data class FilterOption(
    @StringRes val titleRes: Int,
    val bit: Int,
)

private val LevelOptions =
    listOf(
        FilterOption(R.string.diagnostics_filter_info, LEVEL_INFO),
        FilterOption(R.string.diagnostics_filter_warning, LEVEL_WARN),
        FilterOption(R.string.diagnostics_filter_error, LEVEL_ERROR),
    )

private val CategoryOptions =
    listOf(
        FilterOption(
            R.string.diagnostics_filter_module_compatibility,
            CAT_MODULE,
        ),
        FilterOption(R.string.diagnostics_filter_network, CAT_NETWORK),
        FilterOption(
            R.string.diagnostics_filter_display_transition,
            CAT_DISPLAY,
        ),
        FilterOption(R.string.diagnostics_filter_performance, CAT_PERF),
        FilterOption(
            R.string.diagnostics_filter_settings_maintenance,
            CAT_SETTINGS,
        ),
        FilterOption(R.string.diagnostics_filter_other, CAT_OTHER),
    )

@Composable
private fun filterOptionItems(
    options: List<FilterOption>,
    mask: Int,
    onMaskChange: (Int) -> Unit,
): List<DropdownItem> =
    options.map { option ->
        val selected = mask and option.bit != 0
        DropdownItem(
            text = stringResource(option.titleRes),
            selected = selected,
            onClick = {
                onMaskChange(
                    if (selected) mask and option.bit.inv() else mask or option.bit,
                )
            },
        )
    }

@Composable
private fun logFilterEntries(
    levelItems: List<DropdownItem>,
    categoryItems: List<DropdownItem>,
    filterActive: Boolean,
    onReset: () -> Unit,
): List<DropdownEntry> =
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

internal data class LogFilterState(
    val levelMask: Int,
    val categoryMask: Int,
)

internal data class LogFilterActions(
    val onLevelMaskChange: (Int) -> Unit,
    val onCategoryMaskChange: (Int) -> Unit,
    val onReset: () -> Unit,
)

@Composable
internal fun LogFilterMenu(
    title: String,
    state: LogFilterState,
    actions: LogFilterActions,
) {
    val levelMask = state.levelMask
    val categoryMask = state.categoryMask
    val filterActive =
        levelMask != LEVEL_ALL ||
            categoryMask != CAT_ALL
    val levelItems = filterOptionItems(LevelOptions, levelMask, actions.onLevelMaskChange)
    val categoryItems = filterOptionItems(CategoryOptions, categoryMask, actions.onCategoryMaskChange)
    val entries = logFilterEntries(levelItems, categoryItems, filterActive, actions.onReset)

    val icon: @Composable () -> Unit = {
        BadgedBox(
            badge = {
                AnimatedVisibility(
                    visible = filterActive,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Badge(containerColor = MiuixTheme.colorScheme.primary)
                }
            },
        ) {
            Icon(
                imageVector = Os4GlassFilter,
                contentDescription = title,
                modifier = Modifier.size(28.dp),
            )
        }
    }

    DiagToolbarMenu(
        title = title,
        entries = entries,
        collapseOnSelection = false,
        horizontalAlignment = Alignment.Start,
        content = icon,
    )
}

@Composable
private fun DiagToolbarMenu(
    title: String,
    entries: List<DropdownEntry>,
    collapseOnSelection: Boolean = true,
    horizontalAlignment: Alignment.Horizontal = Alignment.End,
    content: @Composable () -> Unit,
) {
    TooltipBox(text = title) {
        if (isRuntimeShaderSupported()) {
            Os4CascadingMenu(
                entries = entries,
                collapseOnSelection = collapseOnSelection,
                horizontalAlignment = horizontalAlignment,
                content = content,
            )
        } else {
            WindowIconCascadingDropdownMenu(
                entries = entries,
                collapseOnSelection = collapseOnSelection,
                content = content,
            )
        }
    }
}

@StringRes
private fun DiagLevel.labelRes(): Int =
    if (this == DiagLevel.Detailed) R.string.diagnostics_mode_detailed else R.string.diagnostics_mode_basic

@Composable
private fun diagLevelEntry(
    diagnosticsLevel: DiagLevel,
    canChangeLevel: Boolean,
    onDiagLevelChange: (DiagLevel) -> Unit,
): DropdownEntry {
    val currentLevelLabel = stringResource(diagnosticsLevel.labelRes())
    val levelItems =
        DiagLevel.entries.map { level ->
            DropdownItem(
                text = stringResource(level.labelRes()),
                selected = diagnosticsLevel == level,
                enabled = canChangeLevel,
                onClick = { onDiagLevelChange(level) },
            )
        }
    return DropdownEntry(
        items =
            listOf(
                DropdownItem(
                    text = stringResource(R.string.diagnostics_mode_title),
                    summary = currentLevelLabel,
                    enabled = canChangeLevel,
                    children = levelItems,
                ),
            ),
    )
}

internal data class DiagMenuStatus(
    val diagnosticsLevel: DiagLevel,
    val canChangeLevel: Boolean,
    val canScrollTop: Boolean,
    val canScrollBottom: Boolean,
    val canClear: Boolean,
)

internal data class DiagMenuActions(
    val onDiagLevelChange: (DiagLevel) -> Unit,
    val onScrollTop: () -> Unit,
    val onScrollBottom: () -> Unit,
    val onClear: () -> Unit,
)

@Composable
internal fun DiagMenu(
    title: String,
    status: DiagMenuStatus,
    actions: DiagMenuActions,
) {
    val diagnosticsLevel = status.diagnosticsLevel
    val canChangeLevel = status.canChangeLevel
    val canScrollTop = status.canScrollTop
    val canScrollBottom = status.canScrollBottom
    val canClear = status.canClear
    val entries =
        listOf(
            diagLevelEntry(diagnosticsLevel, canChangeLevel, actions.onDiagLevelChange),
            DropdownEntry(
                items =
                    listOf(
                        DropdownItem(
                            text = stringResource(R.string.diagnostics_scroll_top),
                            enabled = canScrollTop,
                            onClick = actions.onScrollTop,
                        ),
                        DropdownItem(
                            text = stringResource(R.string.diagnostics_scroll_bottom),
                            enabled = canScrollBottom,
                            onClick = actions.onScrollBottom,
                        ),
                    ),
            ),
            DropdownEntry(
                items =
                    listOf(
                        DropdownItem(
                            text = stringResource(R.string.diagnostics_clear_history),
                            enabled = canClear,
                            onClick = actions.onClear,
                        ),
                    ),
            ),
        )

    val icon: @Composable () -> Unit = {
        Icon(
            imageVector = Os4GlassMore,
            contentDescription = title,
            modifier = Modifier.size(28.dp),
        )
    }
    DiagToolbarMenu(title = title, entries = entries, content = icon)
}

/** Rendering only; the screen owns report capture, export and share lifecycles. */
@Composable
internal fun DiagnosticsReportAction(
    enabled: Boolean,
    shareTitle: String,
    onExport: () -> Unit,
    onShare: () -> Unit,
) {
    DiagActionButton(
        onClick = onExport,
        onLongClick = onShare,
        longClickLabel = shareTitle,
        enabled = enabled,
    ) {
        Icon(
            imageVector = Os4GlassShare,
            contentDescription = stringResource(R.string.export_diagnostic_report),
            modifier = Modifier.size(28.dp),
        )
    }
}

/** Content selected by the diagnostics state; retry behavior remains screen-owned. */
internal data class LogEmptyGuideContent(
    val title: String,
    val description: String,
    val note: String?,
    val icon: ImageVector,
)

@Composable
internal fun LogEmptyGuide(
    content: LogEmptyGuideContent,
    busy: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (title, description, note, icon) = content
    Box(
        modifier = modifier.fillMaxWidth().padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 340.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LogEmptyGuideHeader(icon, title)
            LogEmptyGuideBody(description, note)
            LogEmptyRetryAction(busy = busy, onRetry = onRetry)
        }
    }
}

@Composable
private fun LogEmptyGuideHeader(icon: ImageVector, title: String) {
    Box(
        modifier = Modifier.size(72.dp).background(
            color = MiuixTheme.colorScheme.surfaceContainerHighest,
            shape = CircleShape,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MiuixTheme.colorScheme.onSurfaceContainerHighest,
        )
    }
    Spacer(Modifier.height(24.dp))
    Text(
        text = title,
        style = MiuixTheme.textStyles.title3.copy(fontWeight = FontWeight.SemiBold),
        color = MiuixTheme.colorScheme.onSurfaceContainer,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun LogEmptyGuideBody(description: String, note: String?) {
    Spacer(Modifier.height(20.dp))
    Text(
        text = description,
        modifier = Modifier.fillMaxWidth(),
        style = MiuixTheme.textStyles.body1,
        color = MiuixTheme.colorScheme.onSurfaceSecondary,
        textAlign = TextAlign.Start,
    )
    note?.let {
        Spacer(Modifier.height(16.dp))
        Text(
            text = it,
            modifier = Modifier.fillMaxWidth(),
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            textAlign = TextAlign.Start,
        )
    }
}

@Composable
private fun LogEmptyRetryAction(
    busy: Boolean,
    onRetry: () -> Unit,
) {
    Spacer(Modifier.height(32.dp))
    Button(
        onClick = onRetry,
        enabled = !busy,
        modifier = Modifier.widthIn(min = 176.dp),
        colors = ButtonDefaults.buttonColorsPrimary(),
    ) {
        Text(
            text = stringResource(R.string.diagnostics_retry_logs),
            style = MiuixTheme.textStyles.button,
        )
    }
}
