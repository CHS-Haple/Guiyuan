package com.chaners.guiyuan.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.TooltipBox
import com.chaners.guiyuan.ui.glass.GlassIconButton
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.symbol.ChevronBackward
import top.yukonga.miuix.kmp.symbol.rememberSymbolPainter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/** Shared in-app top bar back control, following MIUIX's Glass bar. */
@Composable
internal fun UiTopBarNavigationIcon(
    onBack: () -> Unit,
    navigationActions: @Composable RowScope.() -> Unit = {},
) {
    val layoutDirection = LocalLayoutDirection.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TooltipBox(text = stringResource(R.string.back)) {
            GlassIconButton(onClick = onBack) {
                Icon(
                    painter = rememberSymbolPainter(MiuixIcons.Symbol.ChevronBackward),
                    contentDescription = stringResource(R.string.back),
                    modifier = Modifier.size(28.dp).graphicsLayer {
                        if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
                    },
                )
            }
        }
        navigationActions()
    }
}
