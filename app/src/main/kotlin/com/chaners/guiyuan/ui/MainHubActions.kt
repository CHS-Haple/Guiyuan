package com.chaners.guiyuan.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.LiquidMode
import com.chaners.guiyuan.ui.components.liquid.LiquidNavSpec
import com.chaners.guiyuan.ui.components.liquid.LiquidNavPressLink
import com.chaners.guiyuan.ui.components.liquid.LiquidFeedbackSpec
import com.chaners.guiyuan.ui.components.liquid.liquidRailSurface
import com.chaners.guiyuan.ui.icons.Os4GlassSearch
import com.chaners.guiyuan.ui.icons.Os4GlassRefresh
import com.chaners.guiyuan.ui.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassMotion
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

/** Both page actions share one callback owner and the same busy-state checks. */
internal class NavigationActionSurface(
    val backdrop: Backdrop,
    val liquidMode: LiquidMode?,
    val dark: Boolean,
    val size: Dp,
)

internal class NavigationActionControls(
    val hotReloadEnabled: Boolean,
    val hotReloadInProgress: Boolean,
    val onHotReload: () -> Unit,
    val onSearch: () -> Unit,
)

/**
 * OS4 Liquid keeps its own optical surface and interaction; MIUIX owns action layout/transition.
 * This is a separate button, never another destination or a second pager writer.
 */
@Composable
private fun LiquidNavigationAction(
    action: NavigationAction,
    surface: NavigationActionSurface,
    mode: LiquidMode,
    controls: NavigationActionControls,
    pressLink: LiquidNavPressLink? = null,
) {
    val isReload = action == NavigationAction.HotReload
    val label = stringResource(if (isReload) R.string.hot_reload else R.string.feature_search)
    val icon = if (isReload) Os4GlassRefresh else Os4GlassSearch
    val enabled = !isReload || controls.hotReloadEnabled
    val holdDown = isReload && controls.hotReloadInProgress
    val onClick = if (isReload) controls.onHotReload else controls.onSearch
    val direction = LocalLayoutDirection.current
    val interactionSource = remember { MutableInteractionSource() }
    val pointerPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pointerPressed || holdDown) 0.9f else 1f,
        // Follow the published MIUIX floating action's press/release timing,
        // retaining Guiyuan's Liquid surface rather than its primary-tinted material.
        animationSpec = GlassMotion.springOf(
            damping = 1f, response = if (pointerPressed || holdDown) 0.15f else 0.3f,
        ),
        label = "liquidNavigationActionPressScale",
    )
    Box(
        modifier = Modifier
            // Portrait uses the 64dp Liquid capsule; a Rail action follows Rail's height.
            .size(surface.size)
            .then(
                if (pressLink != null) Modifier.offset {
                    // Read the same lens, panel and drag frame as the nav bar;
                    // the link owns no extra animation or gesture writer.
                    val offset = pressLink.actionOffsetX(
                        LiquidFeedbackSpec.PressedPanelGrowth.toPx(),
                        LiquidNavSpec.contentPadding.toPx(), direction,
                    )
                    IntOffset(offset.roundToInt(), 0)
                } else Modifier
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .dropShadow(
                shape = CircleShape,
                shadow = Shadow(
                    radius = 10.dp,
                    color = Color.Black,
                    alpha = if (surface.dark) 0.20f else 0.10f,
                ),
            )
            .liquidRailSurface(surface.backdrop, CircleShape, mode)
            .clickable(
                enabled = enabled && !holdDown,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) MiuixTheme.colorScheme.onBackground
                else MiuixTheme.colorScheme.disabledOnSecondaryVariant,
        )
    }
}

/** The one navigation action slot is shared by Home and Features. */
@Composable
internal fun NavigationTrailingAction(
    action: NavigationAction,
    surface: NavigationActionSurface,
    controls: NavigationActionControls,
    pressLink: LiquidNavPressLink? = null,
) {
    val reload = action == NavigationAction.HotReload
    val label = stringResource(if (reload) R.string.hot_reload else R.string.feature_search)
    val icon = if (reload) Os4GlassRefresh else Os4GlassSearch
    val enabled = !reload || controls.hotReloadEnabled
    val holdDown = reload && controls.hotReloadInProgress
    val onClick = if (reload) controls.onHotReload else controls.onSearch
    TooltipBox(text = label) {
        val mode = surface.liquidMode
        if (mode != null) {
            LiquidNavigationAction(
                action = action,
                surface = surface,
                mode = mode,
                controls = controls,
                pressLink = pressLink,
            )
        } else {
            GlassIconButton(
                onClick = { if (enabled && !holdDown) onClick() },
                backdrop = surface.backdrop,
                size = surface.size,
                enabled = enabled,
                holdDownState = holdDown,
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = if (enabled) MiuixTheme.colorScheme.onSurface
                        else MiuixTheme.colorScheme.disabledOnSecondaryVariant,
                )
            }
        }
    }
}
