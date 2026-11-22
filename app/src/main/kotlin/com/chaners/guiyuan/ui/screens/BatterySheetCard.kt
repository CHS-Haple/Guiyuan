package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.chaners.guiyuan.settings.UiStyle
import com.chaners.guiyuan.ui.LocalUiStyle
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.glass.GlassCard
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/** Battery sheets use a translucent card on OS4 glass, without changing OS3-style native cards. */
@Composable
internal fun BatterySheetCard(
    modifier: Modifier = Modifier,
    insideMargin: PaddingValues = CardDefaults.InsideMargin,
    nativeColors: CardColors = CardDefaults.defaultColors(),
    pressFeedbackType: PressFeedbackType = PressFeedbackType.None,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (LocalUiStyle.current == UiStyle.OS4) {
        GlassCard(
            modifier = modifier,
            insideMargin = insideMargin,
            pressFeedbackType = pressFeedbackType,
            onClick = onClick,
            content = content,
        )
    } else {
        Card(
            modifier = modifier,
            insideMargin = insideMargin,
            colors = nativeColors,
            pressFeedbackType = pressFeedbackType,
            onClick = onClick,
            content = content,
        )
    }
}
