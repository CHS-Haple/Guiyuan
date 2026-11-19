package com.chaners.guiyuan.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import top.yukonga.miuix.kmp.anim.folmeSpring
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.PI

@Composable
internal fun GuiyuanAnimatedIdentityMark(
    markSize: Dp = 64.dp,
    wave: Animatable<Float, AnimationVector1D>,
    wavePhase: Float,
    onWaveRelease: (Float) -> Unit,
    interactive: Boolean = false,
    active: Boolean = true,
    effectModifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val waveScope = rememberCoroutineScope()
    var boosted by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    val speed by animateFloatAsState(
        targetValue = if (boosted) 20f else 1f,
        animationSpec = folmeSpring(damping = 1f, response = if (boosted) 0.48f else 0.95f),
        label = "aboutOrbitSpeed",
    )
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = folmeSpring(damping = 0.85f, response = if (pressed) 0.23f else 0.38f),
        label = "aboutLogoPress",
    )
    var orbitRotation by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(boosted, active) {
        if (boosted && active) {
            // Wait for peak speed only while held; the triggered wave owns its playback.
            snapshotFlow { speed }.first { it >= 19f }
            onWaveRelease(orbitRotation * (PI / 180).toFloat())
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            waveScope.launch {
                wave.snapTo(0f)
                wave.animateTo(1f, tween(durationMillis = 1150, easing = FastOutSlowInEasing))
            }
        }
    }
    val currentSpeed by rememberUpdatedState(speed)

    LaunchedEffect(active) {
        if (active) {
            var previous = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                // Do not turn a resumed frame after a pause into an angular jump.
                val delta = (now - previous).coerceIn(0L, 50_000_000L) / 1_000_000_000f
                previous = now
                orbitRotation = (orbitRotation - 18f * currentSpeed * delta) % 360f
            }
        }
    }
    val pressModifier =
        if (interactive) {
            Modifier.pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        try {
                            tryAwaitRelease()
                        } finally {
                            pressed = false
                            boosted = false
                        }
                    },
                    onLongPress = { boosted = true },
                )
            }
        } else {
            Modifier
        }

    val painter = painterResource(R.drawable.ic_launcher_foreground)
    val tint = MiuixTheme.colorScheme.onSurfaceContainer
    val airflow = lerp(tint, MiuixTheme.colorScheme.primary, 0.55f)
    Box(
        modifier = Modifier
            .size(markSize)
            .then(pressModifier)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            },
        contentAlignment = Alignment.Center,
    ) {
        if (wave.isRunning) {
            // Only sample the larger backdrop while the one-shot ripple is moving.
            Canvas(Modifier.requiredSize(markSize * 3.8f).then(effectModifier)) {
                val progress = wave.value
                val diameter = markSize.toPx()
                drawWave(airflow, progress, 0.46f, diameter, wavePhase)
                drawWave(airflow, (progress - 0.2f) / 0.8f, 0.10f, diameter, wavePhase)
            }
        }
        Box(Modifier.matchParentSize().clipToBounds()) {
            Canvas(Modifier.matchParentSize().then(effectModifier)) {
                val targetSize = Size(size.width * 1.8f, size.height * 1.8f)
                val left = (size.width - targetSize.width) / 2f
                val top = (size.height - targetSize.height) / 2f
                rotate(degrees = orbitRotation, pivot = center) {
                    translate(left = left, top = top) {
                        with(painter) {
                            draw(
                                size = targetSize,
                                colorFilter = ColorFilter.tint(tint),
                            )
                        }
                    }
                }
            }
        }
    }
}
