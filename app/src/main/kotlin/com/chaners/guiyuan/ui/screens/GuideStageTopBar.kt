package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.R
import com.chaners.guiyuan.ui.LocalTopBarProgressiveBlur
import com.chaners.guiyuan.ui.components.UiTopBarNavigationIcon
import com.chaners.guiyuan.ui.glass.GlassIconButton
import com.chaners.guiyuan.ui.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.symbol.Refresh
import top.yukonga.miuix.kmp.symbol.rememberSymbolPainter

internal data class GuideStageBarFrame(
    val step: Int,
    val contentScrolled: Boolean,
    val backdrop: LayerBackdrop?,
    val scrollBehavior: ScrollBehavior,
)

@Composable
internal fun GuideStageTopBar(
    frame: GuideStageBarFrame,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    val refreshAction: @Composable RowScope.() -> Unit = {
        if (frame.step == 1) GuideStageRefreshAction(onRefresh)
    }
    val navigation: @Composable () -> Unit = {
        UiTopBarNavigationIcon(onBack = onBack)
    }
    // Match the in-app Settings title behavior.
    GlassTopAppBar(
        progressiveBlur = LocalTopBarProgressiveBlur.current,
        title = "",
        isContentScrolled = frame.contentScrolled,
        backdrop = frame.backdrop,
        scrollBehavior = frame.scrollBehavior,
        compact = true,
        navigationIcon = navigation,
        actions = refreshAction,
        unclippedNavigationIcon = true,
        unclippedActions = frame.step == 1,
    )
}

@Composable
private fun GuideStageRefreshAction(onRefresh: () -> Unit) {
    val label = stringResource(R.string.guide_refresh_status)
    TooltipBox(text = label) {
        GlassIconButton(onClick = onRefresh) {
            Icon(
                painter = rememberSymbolPainter(MiuixIcons.Symbol.Refresh),
                contentDescription = label,
            )
        }
    }
}
