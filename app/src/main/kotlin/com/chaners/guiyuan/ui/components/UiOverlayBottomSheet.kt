package com.chaners.guiyuan.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize

import com.chaners.guiyuan.ui.LocalGlassBackdrop
import top.yukonga.miuix.kmp.glass.GlassBottomSheet

@Composable
internal fun UiOverlayBottomSheet(
    show: Boolean,
    modifier: Modifier,
    title: String?,
    backgroundColor: Color,
    insideMargin: DpSize,
    startAction: @Composable (() -> Unit)? = null,
    enableWindowDim: Boolean = true,
    allowDismiss: Boolean = true,
    defaultWindowInsetsPadding: Boolean = true,
    onDismissRequest: () -> Unit,
    onDismissFinished: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    GlassBottomSheet(
        show = show,
        modifier = modifier,
        title = title,
        containerColor = backgroundColor,
        insideMargin = insideMargin,
        startAction = startAction,
        enableWindowDim = enableWindowDim,
        allowDismiss = allowDismiss,
        defaultWindowInsetsPadding = defaultWindowInsetsPadding,
        backdrop = LocalGlassBackdrop.current,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        content = content,
    )
}
