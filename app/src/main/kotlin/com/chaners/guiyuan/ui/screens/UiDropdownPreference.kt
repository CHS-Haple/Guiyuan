package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.chaners.guiyuan.ui.LocalGlassBackdrop
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.glass.GlassDropdownPreference
import top.yukonga.miuix.kmp.glass.GlassPopupSizing

@Composable
internal fun UiDropdownPreference(
    items: List<String>,
    selectedIndex: Int,
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    startAction: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    maxHeight: Dp? = null,
    onSelectedIndexChange: (Int) -> Unit,
) {
    GlassDropdownPreference(
        items = items,
        selectedIndex = selectedIndex,
        title = title,
        backdrop = LocalGlassBackdrop.current,
        modifier = modifier,
        summary = summary,
        startAction = startAction,
        enabled = enabled,
        showValue = true,
        insideMargin = insideMargin,
        sizing = GlassPopupSizing(maxHeight = maxHeight ?: GlassPopupSizing().maxHeight),
        onSelectedIndexChange = onSelectedIndexChange,
    )
}
