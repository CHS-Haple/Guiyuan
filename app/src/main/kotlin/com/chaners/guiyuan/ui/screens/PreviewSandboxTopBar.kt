package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.ui.LocalTopBarProgressiveBlur
import com.chaners.guiyuan.ui.glass.GlassTopAppBar
import com.chaners.guiyuan.ui.glass.GlassTopAppBarDefaults
import com.chaners.guiyuan.ui.glass.GlassIconButton
import com.chaners.guiyuan.ui.icons.Os4GlassBack
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal data class SandboxTopBarFrame(
    val backdrop: LayerBackdrop?,
    val scrollBehavior: ScrollBehavior,
    val listState: LazyListState,
    val dockHeight: Dp,
    val contentAlpha: () -> Float,
)

internal data class SandboxTopBarActions(
    val dark: Boolean,
    val onBack: () -> Unit,
    val onToggleTheme: () -> Unit,
)

@Composable
internal fun PreviewSandboxTopBar(
    frame: SandboxTopBarFrame,
    actions: SandboxTopBarActions,
    onHeightChanged: (Int) -> Unit,
) {
    val backAction: @Composable () -> Unit = {
        SandboxTopBackAction(actions.onBack)
    }
    val barActions: @Composable RowScope.() -> Unit = {
        SandboxTopThemeAction(actions.dark, actions.onToggleTheme)
    }
    Box(Modifier.onSizeChanged { onHeightChanged(it.height) }) {
        GlassTopAppBar(
            progressiveBlur = LocalTopBarProgressiveBlur.current,
            isContentScrolled = frame.listState.canScrollBackward,
            title = stringResource(R.string.home_preview_sandbox_title),
            titleAlpha = frame.contentAlpha,
            backdrop = frame.backdrop,
            scrollBehavior = frame.scrollBehavior,
            bandOverhang = GlassTopAppBarDefaults.BandOverhang + frame.dockHeight,
            navigationIcon = backAction,
            actions = barActions,
            unclippedNavigationIcon = true,
            unclippedActions = true,
        )
    }
}

@Composable
private fun SandboxTopBackAction(
    onBack: () -> Unit,
) {
    TooltipBox(text = stringResource(R.string.back)) {
        GlassIconButton(onClick = onBack) {
            Icon(Os4GlassBack, contentDescription = stringResource(R.string.back))
        }
    }
}

@Composable
private fun RowScope.SandboxTopThemeAction(
    dark: Boolean,
    onToggleTheme: () -> Unit,
) {
    val title = stringResource(
        if (dark) R.string.home_preview_switch_light else R.string.home_preview_switch_dark,
    )
    val painter = painterResource(
        if (dark) R.drawable.ic_material_symbol_light_mode else R.drawable.ic_material_symbol_dark_mode,
    )
    TooltipBox(text = title) {
        GlassIconButton(onClick = onToggleTheme) {
            Image(
                painter = painter,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurface),
            )
        }
    }
}
