package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.glass.GlassCard
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/** Battery sheet groups use the upstream translucent card. */
@Composable
internal fun BatterySheetCard(
    modifier: Modifier = Modifier,
    insideMargin: PaddingValues = CardDefaults.InsideMargin,
    pressFeedbackType: PressFeedbackType = PressFeedbackType.None,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassCard(
        modifier = modifier,
        insideMargin = insideMargin,
        pressFeedbackType = pressFeedbackType,
        onClick = onClick,
        content = content,
    )
}
