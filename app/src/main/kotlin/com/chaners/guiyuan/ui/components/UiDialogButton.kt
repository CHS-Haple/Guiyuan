package com.chaners.guiyuan.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.glass.GlassDialogButton
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Keep dialog actions in one style policy; MIUIX owns modal dismissal and geometry. */
@Composable
internal fun UiDialogButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    role: GlassDialogButtonRole = GlassDialogButtonRole.Normal,
) {
    GlassDialogButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        role = role,
    ) {
        Text(text = text, style = MiuixTheme.textStyles.button)
    }
}

internal fun dialogActionGap(): Dp = 12.dp
