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
import com.chaners.guiyuan.ui.components.MiuixBlurredTopBar
import com.chaners.guiyuan.ui.glass.GlassTopAppBar
import com.chaners.guiyuan.ui.glass.GlassTopAppBarDefaults
import com.chaners.guiyuan.ui.glass.GlassIconButton
import com.chaners.guiyuan.ui.icons.Os4GlassBack
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal data class SandboxTopBarFrame(
    val os4: Boolean,
    val backdrop: LayerBackdrop?,
    val scrollBehavior: ScrollBehavior,
    val listState: LazyListState,
    val dockHeight: Dp,
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
        SandboxTopBackAction(frame.os4, actions.onBack)
    }
    val barActions: @Composable RowScope.() -> Unit = {
        SandboxTopThemeAction(frame.os4, actions.dark, actions.onToggleTheme)
    }
    Box(Modifier.onSizeChanged { onHeightChanged(it.height) }) {
        if (frame.os4) {
            GlassTopAppBar(
                progressiveBlur = LocalTopBarProgressiveBlur.current,
                isContentScrolled = frame.listState.canScrollBackward,
                title = stringResource(R.string.home_preview_sandbox_title),
                backdrop = frame.backdrop,
                scrollBehavior = frame.scrollBehavior,
                bandOverhang = GlassTopAppBarDefaults.BandOverhang + frame.dockHeight,
                navigationIcon = backAction,
                actions = barActions,
                unclippedNavigationIcon = true,
                unclippedActions = true,
            )
        } else {
            MiuixBlurredTopBar(
                backdrop = frame.backdrop,
                scrollBehavior = frame.scrollBehavior,
                fadeExtension = frame.dockHeight,
            ) { barColor ->
                SmallTopAppBar(
                    title = stringResource(R.string.home_preview_sandbox_title),
                    color = barColor,
                    scrollBehavior = frame.scrollBehavior,
                    navigationIcon = backAction,
                    actions = barActions,
                )
            }
        }
    }
}

@Composable
private fun SandboxTopBackAction(
    os4: Boolean,
    onBack: () -> Unit,
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
}

@Composable
private fun RowScope.SandboxTopThemeAction(
    os4: Boolean,
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
        if (os4) {
            GlassIconButton(onClick = onToggleTheme) {
                Image(
                    painter = painter,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurface),
                )
            }
        } else {
            IconButton(onClick = onToggleTheme) {
                Image(
                    painter = painter,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurface),
                )
            }
        }
    }
}
