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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
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
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun rememberBrandLogoBlend(): List<BlendColorEntry> {
    val isDark = MiuixTheme.colorScheme.surface.luminance() < 0.5f
    return remember(isDark) {
        if (isDark) {
            listOf(
                BlendColorEntry(Color(0xe6a1a1a1), BlurBlendMode.ColorDodge),
                BlendColorEntry(Color(0x4de6e6e6), BlurBlendMode.LinearLight),
                BlendColorEntry(Color(0xff1af500), BlurBlendMode.Lab),
            )
        } else {
            listOf(
                BlendColorEntry(Color(0xcc4a4a4a), BlurBlendMode.ColorBurn),
                BlendColorEntry(Color(0xff4f4f4f), BlurBlendMode.LinearLight),
                BlendColorEntry(Color(0xff1af200), BlurBlendMode.Lab),
            )
        }
    }
}

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
    val markActive = active && iconProgress < 1f
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
                interactive = markActive,
                active = markActive,
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
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
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
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
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
