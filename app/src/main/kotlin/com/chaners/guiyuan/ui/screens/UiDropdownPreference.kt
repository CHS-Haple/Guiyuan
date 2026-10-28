package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.chaners.guiyuan.settings.UiStyle
import com.chaners.guiyuan.ui.LocalGlassBackdrop
import com.chaners.guiyuan.ui.LocalUiStyle
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.glass.GlassDropdownPreference
import top.yukonga.miuix.kmp.glass.GlassPopupSizing
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference

@Composable
internal fun UiDropdownPreference(
    items: List<String>,
    selectedIndex: Int,
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    startAction: @Composable (() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    showValue: Boolean = true,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    maxHeight: Dp? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    onSelectedIndexChange: ((Int) -> Unit)? = null,
) {
    if (LocalUiStyle.current == UiStyle.OS4) {
        GlassDropdownPreference(
            items = items,
            selectedIndex = selectedIndex,
            title = title,
            backdrop = LocalGlassBackdrop.current,
            modifier = modifier,
            summary = summary,
            startAction = startAction,
            bottomAction = bottomAction,
            enabled = enabled,
            showValue = showValue,
            insideMargin = insideMargin,
            sizing = GlassPopupSizing(maxHeight = maxHeight ?: GlassPopupSizing().maxHeight),
            onExpandedChange = onExpandedChange,
            onSelectedIndexChange = { onSelectedIndexChange?.invoke(it) },
        )
    } else {
        OverlayDropdownPreference(
            items = items,
            selectedIndex = selectedIndex,
            title = title,
            modifier = modifier,
            summary = summary,
            startAction = startAction,
            bottomAction = bottomAction,
            enabled = enabled,
            showValue = showValue,
            insideMargin = insideMargin,
            maxHeight = maxHeight,
            onExpandedChange = onExpandedChange,
            onSelectedIndexChange = onSelectedIndexChange,
        )
    }
}
