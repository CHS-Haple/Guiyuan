package com.chaners.guiyuan.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaners.guiyuan.BuildConfig
import com.chaners.guiyuan.R
import top.yukonga.miuix.kmp.anim.folmeSpring
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.flow.first
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun AboutHero(
    topPadding: Dp,
    active: Boolean,
    backdrop: LayerBackdrop?,
    logoBlend: List<BlendColorEntry>,
    versionProgress: Float,
    nameProgress: Float,
    iconProgress: Float,
    onHeight: (Dp) -> Unit,
    onIconBottom: (Float) -> Unit,
    onNameBottom: (Float) -> Unit,
    onVersionBottom: (Float) -> Unit,
) {
    val density = LocalDensity.current
    val markSize = 100.dp
    val markDiameter = with(density) { markSize.toPx() }
    val markRadius = markDiameter / 2f
    val wave = remember { Animatable(0f) }
    var wavePhase by remember { mutableFloatStateOf(0f) }
    val dark = MiuixTheme.colorScheme.surface.luminance() < 0.5f
    val accent = MiuixTheme.colorScheme.primary
    val glint = if (dark) lerp(accent, Color.Black, 0.35f) else lerp(accent, Color.White, 0.70f)
    var titleDistance by remember { mutableFloatStateOf(0f) }
    var versionDistance by remember { mutableFloatStateOf(0f) }
    val blur = if (backdrop == null) Modifier else Modifier.textureBlur(
        backdrop = backdrop,
        shape = RoundedCornerShape(0.dp),
        blurRadius = 150f,
        colors = BlurColors(blendColors = logoBlend),
        contentBlendMode = BlendMode.DstIn,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding + 92.dp)
            .onSizeChanged { size -> onHeight(with(density) { size.height.toDp() }) },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(markSize)
                .graphicsLayer {
                    alpha = 1f - iconProgress
                    scaleX = 1f - iconProgress * 0.05f
                    scaleY = 1f - iconProgress * 0.05f
                }
                .onGloballyPositioned { coords ->
                    onIconBottom(coords.positionInWindow().y + coords.size.height)
                },
            contentAlignment = Alignment.Center,
        ) {
            GuiyuanAnimatedIdentityMark(
                markSize = markSize,
                wave = wave,
                wavePhase = wavePhase,
                onWaveRelease = { wavePhase = it },
                interactive = active && iconProgress < 1f,
                active = active,
                effectModifier = blur,
            )
        }
        Text(
            text = stringResource(R.string.product_name),
            modifier = Modifier
                .padding(top = 12.dp, bottom = 5.dp)
                .onGloballyPositioned { coords ->
                    onNameBottom(coords.positionInWindow().y + coords.size.height)
                    val distance = coords.positionInParent().y + coords.size.height / 2f - markRadius
                    if (titleDistance != distance) titleDistance = distance
                }
                .graphicsLayer {
                    val lift = waveLift(wave.value, titleDistance, markDiameter, wavePhase)
                    alpha = 1f - nameProgress
                    compositingStrategy = if (wave.isRunning) CompositingStrategy.Offscreen
                    else CompositingStrategy.Auto
                    scaleX = (1f - nameProgress * 0.05f) * (1f + lift * 0.055f)
                    scaleY = (1f - nameProgress * 0.05f) * (1f + lift * 0.035f)
                    translationY = -lift * 7.dp.toPx()
                }
                .waveGlint(wave, titleDistance, markDiameter, wavePhase, glint, accent)
                .then(blur),
            fontSize = 35.sp,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.onBackground,
        )
        Text(
            text = "v${BuildConfig.VERSION_NAME}-${BuildConfig.BUILD_ID}",
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    onVersionBottom(coords.positionInWindow().y + coords.size.height)
                    val distance = coords.positionInParent().y + coords.size.height / 2f - markRadius
                    if (versionDistance != distance) versionDistance = distance
                }
                .graphicsLayer {
                    val lift = waveLift(wave.value, versionDistance, markDiameter, wavePhase)
                    alpha = 1f - versionProgress
                    compositingStrategy = if (wave.isRunning) CompositingStrategy.Offscreen
                    else CompositingStrategy.Auto
                    scaleX = (1f - versionProgress * 0.05f) * (1f + lift * 0.045f)
                    scaleY = (1f - versionProgress * 0.05f) * (1f + lift * 0.025f)
                    translationY = -lift * 5.dp.toPx()
                }
                .waveGlint(wave, versionDistance, markDiameter, wavePhase, glint, accent),
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
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
        wave.snapTo(0f)
        if (boosted && active) {
            // One release at peak speed, with a delayed echo; never loop while held.
            snapshotFlow { speed }.first { it >= 19f }
            onWaveRelease(orbitRotation * (PI / 180).toFloat())
            wave.animateTo(1f, tween(durationMillis = 1150, easing = FastOutSlowInEasing))
        }
    }
    val currentSpeed by rememberUpdatedState(speed)

    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        var previous = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            // Do not turn a resumed frame after a pause into an angular jump.
            val delta = (now - previous).coerceIn(0L, 50_000_000L) / 1_000_000_000f
            previous = now
            orbitRotation = (orbitRotation - 18f * currentSpeed * delta) % 360f
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
                drawWave(airflow, progress, 0.38f, diameter, wavePhase)
                drawWave(airflow, (progress - 0.2f) / 0.8f, 0.22f, diameter, wavePhase)
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

// Rotational airflow: partial wavefronts keep the icon's release angle and lose momentum outward.
private fun DrawScope.drawWave(
    color: Color,
    progress: Float,
    strength: Float,
    diameter: Float,
    phase: Float,
) {
    if (progress <= 0f || progress >= 1f) return

    val sector = (2 * PI / 6).toFloat()
    val feather = diameter * (0.10f + 0.055f * progress)
    val fade = strength * sin((PI * progress).toFloat())
    val drift = phase - 0.55f * progress

    repeat(6) { arc ->
        val path = Path()
        val span = sector * (0.77f + 0.14f * sin(arc * 2.1f + phase))
        val start = drift + arc * sector - span / 2f +
            0.055f * sin(arc * 1.7f + phase)
        for (step in 0..16) {
            val angle = start + span * step / 16f
            val radius = flowRadius(progress, angle, diameter, phase)
            val x = center.x + radius * cos(angle)
            val y = center.y + radius * sin(angle)
            if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val intensity = fade * (0.74f + 0.26f * sin(arc * 2.1f + phase + progress * 2f))
        drawPath(
            path = path,
            color = color.copy(alpha = (intensity * 0.24f).coerceIn(0f, 1f)),
            style = Stroke(width = feather * 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawPath(
            path = path,
            color = color.copy(alpha = (intensity * 0.82f).coerceIn(0f, 1f)),
            style = Stroke(width = feather * 0.42f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

private fun flowRadius(progress: Float, angle: Float, diameter: Float, phase: Float): Float {
    val base = diameter * (0.55f + 1.15f * progress)
    val drift = phase - 0.55f * progress
    return base * (
        1f + 0.042f * sin(3f * angle - drift) +
            0.017f * sin(7f * angle + drift * 0.6f)
    )
}

// Two-tone refraction stays inside the text strokes and follows the same displaced front.
private fun Modifier.waveGlint(
    wave: Animatable<Float, AnimationVector1D>,
    distance: Float,
    diameter: Float,
    phase: Float,
    glint: Color,
    accent: Color,
): Modifier = drawWithContent {
    drawContent()
    if (!wave.isRunning || distance <= 0f) return@drawWithContent
    val progress = wave.value
    drawGlint(progress, distance, diameter, phase, glint, accent, 0.95f)
    drawGlint((progress - 0.2f) / 0.8f, distance, diameter, phase, glint, accent, 0.50f)
}

private fun DrawScope.drawGlint(
    progress: Float,
    distance: Float,
    diameter: Float,
    phase: Float,
    glint: Color,
    accent: Color,
    strength: Float,
) {
    if (progress <= 0f || progress >= 1f) return
    val radius = flowRadius(progress, (PI / 2).toFloat(), diameter, phase)
    val feather = diameter * (0.10f + 0.055f * progress)
    val front = size.height / 2f + radius - distance
    if (front < -feather || front > size.height + feather) return
    val intensity = strength * sin((PI * progress).toFloat()).coerceAtLeast(0f)
    drawRect(
        brush = Brush.linearGradient(
            0f to Color.Transparent,
            0.25f to accent.copy(alpha = intensity * 0.22f),
            0.43f to glint.copy(alpha = intensity * 0.90f),
            0.53f to glint.copy(alpha = intensity),
            0.67f to accent.copy(alpha = intensity * 0.32f),
            1f to Color.Transparent,
            start = Offset(0f, front - feather),
            end = Offset(size.width * 0.20f, front + feather),
        ),
        blendMode = BlendMode.SrcAtop,
    )
}

private fun waveLift(progress: Float, distance: Float, diameter: Float, phase: Float): Float {
    if (distance <= 0f || diameter <= 0f) return 0f
    return maxOf(
        wavefrontLift(progress, distance, diameter, phase),
        wavefrontLift((progress - 0.2f) / 0.8f, distance, diameter, phase) * 0.6f,
    )
}

private fun wavefrontLift(progress: Float, distance: Float, diameter: Float, phase: Float): Float {
    if (progress <= 0f || progress >= 1f) return 0f
    val radius = flowRadius(progress, (PI / 2).toFloat(), diameter, phase)
    val feather = diameter * (0.10f + 0.055f * progress)
    val crossing = (1f - abs(distance - radius) / feather).coerceIn(0f, 1f)
    return crossing * sin((PI * progress).toFloat())
}
