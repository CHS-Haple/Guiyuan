package com.chaners.guiyuan.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet

import com.chaners.guiyuan.settings.UiStyle
import com.chaners.guiyuan.ui.LocalGlassBackdrop
import com.chaners.guiyuan.ui.LocalUiStyle
import top.yukonga.miuix.kmp.glass.GlassBottomSheet

/** Only this source variant links PR423's native BottomSheetSurfaceScope ABI. */
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
    if (LocalUiStyle.current == UiStyle.OS4) {
        GlassBottomSheet(
            show = show,
            modifier = modifier,
            title = title,
            containerColor = backgroundColor,
            insideMargin = insideMargin,
            startAction = startAction,
            enableWindowDim = enableWindowDim,
            allowDismiss = allowDismiss,
            backdrop = LocalGlassBackdrop.current,
            onDismissRequest = onDismissRequest,
            onDismissFinished = onDismissFinished,
            content = content,
        )
    } else {
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
}
