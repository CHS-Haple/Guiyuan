// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.chaners.guiyuan.ui.screens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.State
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassShadows
import top.yukonga.miuix.kmp.glass.popupShadowOutsets
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.LayerOutsets
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.chaners.guiyuan.R
import com.chaners.guiyuan.ui.icons.Os4GlassClose
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

private const val CANCEL_BLUR_PX = 50f

// PR423 GlassSearchOverlay uses the nested EaseManager spring, not Compose's default spring.
private val SearchEasing = run {
    val omega = 2.0 * PI / 0.75
    val stiffness = (omega * omega).toFloat()
    val damping = (0.98 * 2.0 * omega).toFloat()
    val decay = -damping / 2f
    val frequency = sqrt(-(damping.toDouble() * damping - stiffness.toDouble() * 4)).toFloat() / 2f
    val sine = decay / frequency
    Easing { fraction ->
        (1.0 + exp((decay * fraction).toDouble()) *
            (-cos((frequency * fraction).toDouble()) + sine * sin((frequency * fraction).toDouble()))).toFloat()
    }
}

internal data class Os4SearchMotion(
    val progress: Float,
    val cancelAlpha: Float,
    val mounted: Boolean,
)

@Composable
internal fun rememberOs4SearchMotion(expanded: Boolean): Os4SearchMotion {
    val transition = updateTransition(targetState = expanded, label = "os4Search")
    val progress by transition.animateFloat(
        transitionSpec = { tween(400, easing = SearchEasing) },
        label = "searchGeometry",
    ) { if (it) 1f else 0f }
    val cancelAlpha by transition.animateFloat(
        transitionSpec = {
            tween(150, delayMillis = if (targetState) 100 else 0, easing = LinearEasing)
        },
        label = "cancelMaterial",
    ) { if (it) 1f else 0f }
    return Os4SearchMotion(progress, cancelAlpha, expanded || transition.currentState || transition.isRunning)
}

@Composable
internal fun Os4SearchMode(
    motion: Os4SearchMotion,
    expanded: Boolean,
    query: String,
    backdrop: Backdrop?,
    materialProgress: State<Float>,
    shadowProgress: State<Float>,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClose: () -> Unit,
) {
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = expanded,
        onBackCompleted = onClose,
    )

    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val keyboard = LocalSoftwareKeyboardController.current
    val cancelInteraction = remember { MutableInteractionSource() }
    val cancelPressed = cancelInteraction.collectIsPressedAsState()
    val statusInset = WindowInsets.statusBars.getTop(density)
    val shadowOutsets = remember(density) { with(density) { popupShadowOutsets(GlassShadows.Regular) } }
    val cancelOutsets = remember(density, shadowOutsets) {
        // Match PR423's three-radius allowance for its Cancel blur.
        val halo = with(density) { (CANCEL_BLUR_PX * 3f).toDp() }
        LayerOutsets(
            left = shadowOutsets.left + halo,
            top = shadowOutsets.top + halo,
            right = shadowOutsets.right + halo,
            bottom = shadowOutsets.bottom + halo,
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                outsets = shadowOutsets
                alpha = motion.progress
                translationY = -statusInset * (1f - motion.progress)
            },
    ) {
        // A fading search row must still consume clicks until its exit animation finishes.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(interactionSource = null, indication = null) { keyboard?.hide() },
        )
        // One progress controls the native PR423 54dp end reservation and the cancel surface.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                Os4SearchField(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearch = onSearch,
                    active = expanded,
                    backdrop = backdrop,
                    materialProgress = materialProgress,
                    shadowProgress = shadowProgress,
                )
            }
            Spacer(Modifier.width(54.dp * motion.progress.coerceIn(0f, 1f)))
        }
        val label = stringResource(R.string.cancel)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .graphicsLayer {
                    outsets = cancelOutsets
                    val scale = motion.progress.coerceIn(0f, 1f)
                    scaleX = scale
                    scaleY = scale
                    alpha = motion.cancelAlpha
                    val blur = CANCEL_BLUR_PX * (1f - motion.cancelAlpha.coerceIn(0f, 1f))
                    renderEffect = if (blur > 0.5f) {
                        BlurEffect(blur, blur, TileMode.Decal)
                    } else null
                    translationX = 66.dp.toPx() * (1f - scale) *
                        (if (direction == LayoutDirection.Ltr) 1f else -1f)
                },
        ) {
            TooltipBox(text = label) {
                Os4SearchSurface(
                    modifier = Modifier.size(44.dp),
                    backdrop = backdrop,
                    materialProgress = materialProgress,
                    shadowProgress = shadowProgress,
                    pressed = cancelPressed,
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(
                                enabled = expanded,
                                role = Role.Button,
                                interactionSource = cancelInteraction,
                                indication = null,
                                onClick = onClose,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Os4GlassClose,
                            contentDescription = label,
                            modifier = Modifier.size(24.dp),
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

/** Keep the native bar mounted; search only fades its overlapping action. */
@Composable
internal fun rememberOs4BarAlpha(expanded: Boolean): State<Float> {
    val transition = updateTransition(expanded, label = "os4SearchBarOpacity")
    return transition.animateFloat(
        transitionSpec = {
            tween(
                150,
                easing = if (targetState) CubicBezierEasing(0f, 0f, 0.67f, 1f)
                    else CubicBezierEasing(0.26f, 0f, 0.6f, 0.2f),
            )
        },
        label = "os4SearchBarAlpha",
    ) { if (it) 0f else 1f }
}
