package com.chaners.guiyuan.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.settings.UiStyle
import com.chaners.guiyuan.ui.LocalUiStyle
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
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
    if (LocalUiStyle.current == UiStyle.OS4) {
        GlassDialogButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            role = role,
        ) {
            Text(text = text, style = MiuixTheme.textStyles.button)
        }
    } else {
        val colors = when (role) {
            GlassDialogButtonRole.Normal -> ButtonDefaults.textButtonColors()
            GlassDialogButtonRole.Primary -> ButtonDefaults.textButtonColorsPrimary()
            GlassDialogButtonRole.Destructive -> ButtonDefaults.textButtonColors(
                textColor = MiuixTheme.colorScheme.error,
            )
        }
        TextButton(
            text = text,
            modifier = modifier,
            enabled = enabled,
            colors = colors,
            onClick = onClick,
        )
    }
}

@Composable
internal fun dialogActionGap(): Dp = if (LocalUiStyle.current == UiStyle.OS4) 12.dp else 20.dp
