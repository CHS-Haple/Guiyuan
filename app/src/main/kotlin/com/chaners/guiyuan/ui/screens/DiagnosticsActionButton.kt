package com.chaners.guiyuan.ui.screens

import androidx.compose.runtime.Composable
import com.chaners.guiyuan.ui.glass.GlassIconButton

@Composable
internal fun DiagActionButton(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    longClickLabel: String,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    GlassIconButton(
        onClick = onClick,
        onLongClick = onLongClick,
        onLongClickLabel = longClickLabel,
        enabled = enabled,
        content = content,
    )
}
