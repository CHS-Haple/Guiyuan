package com.chaners.guiyuan.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.R
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh

@Composable
internal fun HotReloadAction(
    inProgress: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(R.string.hot_reload)

    TooltipBox(text = label) {
        IconButton(
            onClick = { if (enabled && !inProgress) onClick() },
            enabled = enabled,
            holdDownState = inProgress,
        ) {
            Icon(
                imageVector = MiuixIcons.Refresh,
                contentDescription = label,
            )
        }
    }
}
