package com.chaners.guiyuan.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.ui.icons.Os4GlassBack
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.TooltipBox
import com.chaners.guiyuan.ui.glass.GlassIconButton

/** Shared in-app top bar back control, following MIUIX's Glass bar. */
@Composable
internal fun UiTopBarNavigationIcon(
    onBack: () -> Unit,
    navigationActions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TooltipBox(text = stringResource(R.string.back)) {
            GlassIconButton(onClick = onBack) {
                Icon(Os4GlassBack, contentDescription = stringResource(R.string.back))
            }
        }
        navigationActions()
    }
}
