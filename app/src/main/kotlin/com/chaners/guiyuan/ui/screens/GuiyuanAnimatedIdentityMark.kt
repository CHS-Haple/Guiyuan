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
import androidx.compose.runtime.State
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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

private class MarkPressState {
    var boosted by mutableStateOf(false)
    var pressed by mutableStateOf(false)
}

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
    val press = remember { MarkPressState() }
    val speed = animateFloatAsState(
        targetValue = if (press.boosted) 20f else 1f,
        animationSpec = folmeSpring(damping = 1f, response = if (press.boosted) 0.48f else 0.95f),
        label = "aboutOrbitSpeed",
    )
    val pressScale by animateFloatAsState(
        targetValue = if (press.pressed) 0.92f else 1f,
        animationSpec = folmeSpring(damping = 0.85f, response = if (press.pressed) 0.23f else 0.38f),
        label = "aboutLogoPress",
    )
    val rotation = rememberMarkRotation(active, speed)
    TriggerMarkWave(wave, press.boosted, active, speed, rotation, onWaveRelease)

    val pressModifier = if (interactive) {
        Modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    press.pressed = true
                    try {
                        tryAwaitRelease()
                    } finally {
                        press.pressed = false
                        press.boosted = false
                    }
                },
                onLongPress = { press.boosted = true },
            )
        }
    } else Modifier
    Box(
        modifier = Modifier.size(markSize).then(pressModifier).graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
        },
        contentAlignment = Alignment.Center,
    ) {
        IdentityMarkArtwork(markSize, wave, wavePhase, effectModifier, rotation.value)
    }
}

@Composable
private fun rememberMarkRotation(active: Boolean, speed: State<Float>): State<Float> {
    val angle = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(active) {
        if (active) {
            var previous = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                // Paused frames must not create a discontinuous jump on resume.
                val delta = (now - previous).coerceIn(0L, 50_000_000L) / 1_000_000_000f
                previous = now
                angle.floatValue = (angle.floatValue - 18f * speed.value * delta) % 360f
            }
        }
    }
    return angle
}

@Composable
private fun TriggerMarkWave(
    wave: Animatable<Float, AnimationVector1D>,
    boosted: Boolean,
    active: Boolean,
    speed: State<Float>,
    rotation: State<Float>,
    onWaveRelease: (Float) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(boosted, active) {
        if (boosted && active) {
            snapshotFlow { speed.value }.first { it >= 19f }
            onWaveRelease(rotation.value * (PI / 180).toFloat())
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch {
                wave.snapTo(0f)
                wave.animateTo(1f, tween(durationMillis = 1150, easing = FastOutSlowInEasing))
            }
        }
    }
}

@Composable
private fun BoxScope.IdentityMarkArtwork(
    markSize: Dp,
    wave: Animatable<Float, AnimationVector1D>,
    wavePhase: Float,
    effectModifier: Modifier,
    rotation: Float,
) {
    val painter = painterResource(R.drawable.ic_launcher_foreground)
    val tint = MiuixTheme.colorScheme.onSurfaceContainer
    val airflow = lerp(tint, MiuixTheme.colorScheme.primary, 0.55f)
    if (wave.isRunning) {
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
            rotate(degrees = rotation, pivot = center) {
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
