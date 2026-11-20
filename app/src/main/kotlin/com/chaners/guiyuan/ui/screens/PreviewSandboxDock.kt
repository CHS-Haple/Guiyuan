package com.chaners.guiyuan.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.VisualCfg
import com.chaners.guiyuan.ui.glass.GlassIconButton
import com.chaners.guiyuan.ui.glass.GlassTopAppBarDefaults
import com.chaners.guiyuan.xposed.RenderModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.glass.GlassMotion
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal data class SandboxDockDisplay(
    val visible: Boolean,
    val os4: Boolean,
    val renderModel: RenderModel,
    val visual: VisualCfg,
    val backdrop: LayerBackdrop?,
)

internal data class SandboxDockActions(
    val onScrollToTop: () -> Unit,
    val onHeightChanged: (Int) -> Unit,
)

@Composable
internal fun SandboxFloatingDock(
    display: SandboxDockDisplay,
    actions: SandboxDockActions,
) {
    val os4 = display.os4
    // Preserve native MIUIX 52dp title center and 4dp lower clearance.
    val dockPadding = if (os4) {
        val inset = (TopAppBarDefaults.CollapsedHeight - TopAppBarDefaults.SmallTopAppBarCenterHeight) / 2
        PaddingValues(top = inset, bottom = inset + TopAppBarDefaults.LargeTitleBottomPadding)
    } else {
        PaddingValues(vertical = 6.dp)
    }
    AnimatedVisibility(
        visible = display.visible,
        modifier = Modifier.onSizeChanged { actions.onHeightChanged(it.height) },
        enter = if (os4) {
            expandVertically(
                animationSpec = GlassMotion.barExpand(),
                expandFrom = Alignment.Top,
                clip = false,
            )
        } else fadeIn() + expandVertically(expandFrom = Alignment.Top),
        exit = if (os4) {
            shrinkVertically(
                animationSpec = GlassMotion.barCollapse(),
                shrinkTowards = Alignment.Top,
                clip = false,
            )
        } else fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
    ) {
        // Keep the native shadow outside the animated row rather than offscreen-alpha clipping.
        val opacityModifier = if (os4) {
            val opacity by transition.animateFloat(
                transitionSpec = {
                    if (targetState == EnterExitState.Visible) GlassMotion.fadeIn()
                    else GlassMotion.fadeOut()
                },
                label = "dockOpacity",
            ) { state ->
                if (state == EnterExitState.Visible) 1f else 0f
            }
            Modifier.graphicsLayer {
                alpha = opacity
                compositingStrategy = CompositingStrategy.ModulateAlpha
            }
        } else Modifier
        SandboxFloatingDockRow(display, actions, dockPadding, opacityModifier)
    }
}

@Composable
private fun SandboxFloatingDockRow(
    display: SandboxDockDisplay,
    actions: SandboxDockActions,
    dockPadding: PaddingValues,
    opacityModifier: Modifier,
) {
    Row(
        modifier = Modifier.fillMaxWidth().then(opacityModifier).padding(dockPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SandboxDockTitle()
        SandboxDockIcon(display.renderModel, display.visual)
        SandboxDockScrollTop(display, actions.onScrollToTop)
    }
}

@Composable
private fun RowScope.SandboxDockTitle() {
    Box(
        modifier = Modifier.weight(1f).padding(start = TopAppBarDefaults.TitlePadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = stringResource(R.string.home_preview_live_title),
            style = MiuixTheme.textStyles.title4,
            fontWeight = FontWeight.Medium,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RowScope.SandboxDockScrollTop(
    display: SandboxDockDisplay,
    onScrollToTop: () -> Unit,
) {
    Box(
        modifier = Modifier.weight(1f).padding(
            end = if (display.os4) GlassTopAppBarDefaults.HorizontalPadding else TopAppBarDefaults.ActionIconPadding,
        ),
        contentAlignment = Alignment.CenterEnd,
    ) {
        val backTop = stringResource(R.string.diagnostics_scroll_top)
        TooltipBox(text = backTop) {
            if (display.os4) {
                GlassIconButton(
                    onClick = onScrollToTop,
                    backdrop = display.backdrop,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_material_symbol_vertical_align_top),
                        contentDescription = backTop,
                    )
                }
            } else IconButton(
                onClick = onScrollToTop,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_material_symbol_vertical_align_top),
                    contentDescription = backTop,
                )
            }
        }
    }
}
