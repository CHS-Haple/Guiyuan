package com.chaners.guiyuan.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
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
                .size(100.dp)
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
                markSize = 100.dp,
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
                }
                .graphicsLayer {
                    alpha = 1f - nameProgress
                    scaleX = 1f - nameProgress * 0.05f
                    scaleY = 1f - nameProgress * 0.05f
                }
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
                }
                .graphicsLayer {
                    alpha = 1f - versionProgress
                    scaleX = 1f - versionProgress * 0.05f
                    scaleY = 1f - versionProgress * 0.05f
                },
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

@Composable
internal fun GuiyuanAnimatedIdentityMark(
    markSize: Dp = 64.dp,
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
    val halo = remember { Animatable(0f) }
    LaunchedEffect(boosted, active) {
        halo.snapTo(0f)
        if (boosted && active) {
            // One pulse at peak speed, then stay idle even if the hold continues.
            snapshotFlow { speed }.first { it >= 19f }
            halo.animateTo(1f, tween(durationMillis = 850, easing = FastOutSlowInEasing))
        }
    }
    val currentSpeed by rememberUpdatedState(speed)
    var orbitRotation by remember { mutableFloatStateOf(0f) }

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
        Canvas(Modifier.matchParentSize()) {
            val progress = halo.value
            if (progress > 0f && progress < 1f) {
                val strength = sin((PI * progress).toFloat())
                // Diffuse behind the full mark, not a stroke emerging from its inner ring.
                val radius = size.minDimension * (0.72f + 0.55f * progress)
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to tint.copy(alpha = 0.21f * strength),
                        0.35f to tint.copy(alpha = 0.18f * strength),
                        0.7f to tint.copy(alpha = 0.08f * strength),
                        1f to tint.copy(alpha = 0f),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                )
                val waveRadius = size.minDimension * (0.8f + 0.65f * progress)
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to tint.copy(alpha = 0.08f * strength),
                        0.55f to tint.copy(alpha = 0.11f * strength),
                        0.8f to tint.copy(alpha = 0.06f * strength),
                        1f to tint.copy(alpha = 0f),
                        center = center,
                        radius = waveRadius,
                    ),
                    radius = waveRadius,
                )
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
