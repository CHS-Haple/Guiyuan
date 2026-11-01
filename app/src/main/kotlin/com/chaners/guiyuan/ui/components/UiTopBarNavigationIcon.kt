package com.chaners.guiyuan.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.UiStyle
import com.chaners.guiyuan.ui.LocalUiStyle
import com.chaners.guiyuan.ui.icons.Os4GlassBack
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

/** Shared in-app top bar back control, following the active MIUIX UI style. */
@Composable
internal fun UiTopBarNavigationIcon(
    onBack: () -> Unit,
    navigationActions: @Composable RowScope.() -> Unit = {},
) {
    val os4 = LocalUiStyle.current == UiStyle.OS4
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (os4) Arrangement.spacedBy(8.dp) else Arrangement.Start,
    ) {
        TooltipBox(text = stringResource(R.string.back)) {
            if (os4) {
                GlassIconButton(onClick = onBack) {
                    Icon(Os4GlassBack, contentDescription = stringResource(R.string.back))
                }
            } else {
                IconButton(onClick = onBack) {
                    Icon(MiuixIcons.Back, contentDescription = stringResource(R.string.back))
                }
            }
        }
        navigationActions()
    }
}
