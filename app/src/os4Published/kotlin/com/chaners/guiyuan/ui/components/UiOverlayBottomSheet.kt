package com.chaners.guiyuan.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet

/** Native MIUIX sheet host while the published ABI is active. */
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
    onDismissRequest: () -> Unit,
    onDismissFinished: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    OverlayBottomSheet(
        show = show,
        modifier = modifier,
        title = title,
        backgroundColor = backgroundColor,
        insideMargin = insideMargin,
        startAction = startAction,
        enableWindowDim = enableWindowDim,
        allowDismiss = allowDismiss,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        content = content,
    )
}
