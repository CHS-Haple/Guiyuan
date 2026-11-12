package com.chaners.guiyuan.ui.screens

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.DiagLevel
import com.chaners.guiyuan.settings.UiStyle
import com.chaners.guiyuan.ui.LocalUiStyle
import com.chaners.guiyuan.ui.icons.Os4GlassFilter
import com.chaners.guiyuan.ui.icons.Os4GlassMore
import top.yukonga.miuix.kmp.basic.Badge
import top.yukonga.miuix.kmp.basic.BadgedBox
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButtonDefaults
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.menu.WindowIconCascadingDropdownMenu
import top.yukonga.miuix.kmp.squircle.squircleSurface
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

private fun toggleMask(
    mask: Int,
    bit: Int,
    checked: Boolean,
): Int =
    if (checked) {
        mask or bit
    } else {
        mask and bit.inv()
    }

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
internal fun DiagActionButton(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    longClickLabel: String,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    if (LocalUiStyle.current == UiStyle.OS4) {
        GlassIconButton(
            onClick = onClick,
            onLongClick = onLongClick,
            onLongClickLabel = longClickLabel,
            enabled = enabled,
            content = content,
        )
    } else {
        // MIUIX's IconButton is click-only; this retains its surface and one gesture owner.
        Box(
            modifier = Modifier
                .defaultMinSize(
                    minWidth = IconButtonDefaults.MinWidth,
                    minHeight = IconButtonDefaults.MinHeight,
                )
                .squircleSurface(color = Color.Unspecified, cornerRadius = IconButtonDefaults.CornerRadius)
                .combinedClickable(
                    enabled = enabled,
                    role = Role.Button,
                    onLongClickLabel = longClickLabel,
                    onLongClick = onLongClick,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

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
                    toggleMask(
                        mask = mask,
                        bit = option.bit,
                        checked = !selected,
                    ),
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

@Composable
internal fun LogFilterMenu(
    title: String,
    levelMask: Int,
    categoryMask: Int,
    onLevelMaskChange: (Int) -> Unit,
    onCategoryMaskChange: (Int) -> Unit,
    onReset: () -> Unit,
) {
    val filterActive =
        levelMask != LEVEL_ALL ||
            categoryMask != CAT_ALL
    val levelItems = filterOptionItems(LevelOptions, levelMask, onLevelMaskChange)
    val categoryItems = filterOptionItems(CategoryOptions, categoryMask, onCategoryMaskChange)
    val entries = logFilterEntries(levelItems, categoryItems, filterActive, onReset)

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
                imageVector = if (LocalUiStyle.current == UiStyle.OS4) Os4GlassFilter else MiuixIcons.Filter,
                contentDescription = title,
                modifier = Modifier.size(if (LocalUiStyle.current == UiStyle.OS4) 28.dp else 24.dp),
            )
        }
    }

    DiagToolbarMenu(title = title, entries = entries, collapseOnSelection = false, content = icon)
}

@Composable
private fun DiagToolbarMenu(
    title: String,
    entries: List<DropdownEntry>,
    collapseOnSelection: Boolean = true,
    content: @Composable () -> Unit,
) {
    TooltipBox(text = title) {
        if (LocalUiStyle.current == UiStyle.OS4 && isRuntimeShaderSupported()) {
            Os4CascadingMenu(
                entries = entries,
                collapseOnSelection = collapseOnSelection,
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

@Composable
private fun diagLevelEntry(
    diagnosticsLevel: DiagLevel,
    canChangeLevel: Boolean,
    onDiagLevelChange: (DiagLevel) -> Unit,
): DropdownEntry {
    val currentLevelLabel =
        stringResource(
            if (diagnosticsLevel == DiagLevel.Detailed) {
                R.string.diagnostics_mode_detailed
            } else {
                R.string.diagnostics_mode_basic
            },
        )
    val levelItems =
        DiagLevel.entries.map { level ->
            DropdownItem(
                text =
                    stringResource(
                        if (level == DiagLevel.Detailed) {
                            R.string.diagnostics_mode_detailed
                        } else {
                            R.string.diagnostics_mode_basic
                        },
                    ),
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

@Composable
internal fun DiagMenu(
    title: String,
    diagnosticsLevel: DiagLevel,
    canChangeLevel: Boolean,
    canScrollTop: Boolean,
    canScrollBottom: Boolean,
    canClear: Boolean,
    onDiagLevelChange: (DiagLevel) -> Unit,
    onScrollTop: () -> Unit,
    onScrollBottom: () -> Unit,
    onClear: () -> Unit,
) {
    val entries =
        listOf(
            diagLevelEntry(diagnosticsLevel, canChangeLevel, onDiagLevelChange),
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
                            text = stringResource(R.string.diagnostics_clear_history),
                            enabled = canClear,
                            onClick = onClear,
                        ),
                    ),
            ),
        )

    val icon: @Composable () -> Unit = {
        Icon(
            imageVector = if (LocalUiStyle.current == UiStyle.OS4) Os4GlassMore else MiuixIcons.More,
            contentDescription = title,
            modifier = Modifier.size(if (LocalUiStyle.current == UiStyle.OS4) 28.dp else 24.dp),
        )
    }
    DiagToolbarMenu(title = title, entries = entries, content = icon)
}

