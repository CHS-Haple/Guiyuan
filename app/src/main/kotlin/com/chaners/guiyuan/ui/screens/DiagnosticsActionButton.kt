package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import com.chaners.guiyuan.settings.UiStyle
import com.chaners.guiyuan.ui.LocalUiStyle
import com.chaners.guiyuan.ui.glass.GlassIconButton
import top.yukonga.miuix.kmp.basic.IconButtonDefaults
import top.yukonga.miuix.kmp.squircle.squircleSurface

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
