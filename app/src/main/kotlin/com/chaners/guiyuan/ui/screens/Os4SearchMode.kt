// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.chaners.guiyuan.ui.screens

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.layout.LayoutDirection
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
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

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
    surface: Modifier,
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
    val statusInset = WindowInsets.statusBars.getTop(density)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = motion.progress
                translationY = -statusInset * (1f - motion.progress)
            },
    ) {
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
                    surface = surface,
                )
            }
            Spacer(Modifier.width(54.dp * motion.progress.coerceIn(0f, 1f)))
        }
        val label = stringResource(R.string.cancel)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .graphicsLayer {
                    val scale = motion.progress.coerceIn(0f, 1f)
                    scaleX = scale
                    scaleY = scale
                    alpha = motion.cancelAlpha
                    translationX = 66.dp.toPx() * (1f - scale) *
                        (if (direction == LayoutDirection.Ltr) 1f else -1f)
                },
        ) {
            TooltipBox(text = label) {
                IconButton(
                    onClick = onClose,
                    enabled = expanded,
                    modifier = Modifier.size(44.dp).then(surface),
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
