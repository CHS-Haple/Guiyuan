package com.chaners.guiyuan.ui.components

import androidx.compose.runtime.Composable
import com.chaners.guiyuan.ui.LocalGlassBackdrop
import top.yukonga.miuix.kmp.glass.GlassDialog

@Composable
internal fun UiOverlayDialog(
    show: Boolean,
    title: String? = null,
    summary: String? = null,
    onDismissRequest: () -> Unit,
    onDismissFinished: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    GlassDialog(
        show = show,
        title = title,
        summary = summary,
        backdrop = LocalGlassBackdrop.current,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        content = content,
    )
}
