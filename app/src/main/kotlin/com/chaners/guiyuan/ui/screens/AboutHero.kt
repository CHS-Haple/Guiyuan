package com.chaners.guiyuan.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
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
                .clipToBounds()
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
            text = "${BuildConfig.VERSION_NAME}-${BuildConfig.BUILD_ID.substringAfterLast('-')}",
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
    val speed by animateFloatAsState(
        targetValue = if (boosted) 10f else 1f,
        animationSpec = folmeSpring(damping = 1f, response = if (boosted) 0.48f else 0.95f),
        label = "aboutOrbitSpeed",
    )
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
                        try {
                            tryAwaitRelease()
                        } finally {
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
        modifier = Modifier.size(markSize).then(pressModifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier.size(markSize).then(effectModifier),
        ) {
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
