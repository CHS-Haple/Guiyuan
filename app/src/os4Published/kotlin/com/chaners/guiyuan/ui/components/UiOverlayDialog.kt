package com.chaners.guiyuan.ui.components

import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.overlay.OverlayDialog

/** Published MIUIX has no DialogSurfaceScope; preserve its native popup host. */
@Composable
internal fun UiOverlayDialog(
    show: Boolean,
    title: String? = null,
    summary: String? = null,
    onDismissRequest: () -> Unit,
    onDismissFinished: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    OverlayDialog(
        show = show,
        title = title,
        summary = summary,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        content = content,
    )
}
