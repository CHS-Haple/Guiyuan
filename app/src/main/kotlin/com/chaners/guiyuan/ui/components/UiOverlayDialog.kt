package com.chaners.guiyuan.ui.components

import androidx.compose.runtime.Composable
import com.chaners.guiyuan.settings.UiStyle
import com.chaners.guiyuan.ui.LocalGlassBackdrop
import com.chaners.guiyuan.ui.LocalUiStyle
import top.yukonga.miuix.kmp.glass.GlassDialog
import top.yukonga.miuix.kmp.overlay.OverlayDialog

/** Only compiled against PR #423's matched DialogSurfaceScope ABI. */
@Composable
internal fun UiOverlayDialog(
    show: Boolean,
    title: String? = null,
    summary: String? = null,
    onDismissRequest: () -> Unit,
    onDismissFinished: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (LocalUiStyle.current == UiStyle.OS4) {
        GlassDialog(
            show = show,
            title = title,
            summary = summary,
            backdrop = LocalGlassBackdrop.current,
            onDismissRequest = onDismissRequest,
            onDismissFinished = onDismissFinished,
            content = content,
        )
    } else {
        OverlayDialog(
            show = show,
            title = title,
            summary = summary,
            onDismissRequest = onDismissRequest,
            onDismissFinished = onDismissFinished,
            content = content,
        )
    }
}
