package com.chaners.guiyuan.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.ColumnScope
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import com.chaners.guiyuan.R
import com.chaners.guiyuan.ui.screens.about.GyBgEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.squircle.squircleSurface

private class BrandGuideMotion {
    val wave = Animatable(0f)
    var phase by mutableFloatStateOf(0f)
    var titleDistance by mutableFloatStateOf(0f)
    var completionDistance by mutableFloatStateOf(0f)
}

private data class BrandGuideColors(
    val fallback: Brush,
    val action: GuideActionPalette,
    val glint: Color,
    val accent: Color,
)

private data class BrandGuideGlass(
    val enabled: Boolean,
    val backdrop: LayerBackdrop,
    val mark: Modifier,
    val action: Modifier,
)

private data class BrandGuideMaterials(
    val colors: BrandGuideColors,
    val glass: BrandGuideGlass,
)

@Composable
internal fun GuiyuanBrandGuide(completed: Boolean, onNext: () -> Unit) {
    // The MIUIX navigation entry lifecycle pauses covered pages below RESUMED.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> resumed = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> resumed = false
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val materials = rememberBrandGuideMaterials()
    val motion = remember { BrandGuideMotion() }
    Box(modifier = Modifier.fillMaxSize().background(materials.colors.fallback)) {
        GyBgEffect(
            playing = resumed,
            bgModifier = if (materials.glass.enabled) {
                Modifier.layerBackdrop(materials.glass.backdrop)
            } else Modifier,
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandGuideIdentity(completed, resumed, motion, materials)
            if (completed) {
                BrandGuideFinishAction(onNext, materials)
            } else {
                BrandGuideContinueAction(onNext, materials)
            }
        }
    }
}

@Composable
private fun rememberBrandGuideMaterials(): BrandGuideMaterials {
    val surface = MiuixTheme.colorScheme.surface
    val accent = MiuixTheme.colorScheme.primary
    // Keep the existing theme-aware scrim, logo blend and fallback colors.
    val palette = guideActionPalette(
        surface = surface,
        onSurface = MiuixTheme.colorScheme.onSurface,
        logoTint = MiuixTheme.colorScheme.onSurfaceContainer,
    )
    val fallback = Brush.linearGradient(
        listOf(
            lerp(surface, accent, 0.25f),
            lerp(surface, accent, 0.06f),
            lerp(surface, accent, 0.19f),
        ),
    )
    val enabled = remember { isRuntimeShaderSupported() }
    val backdrop = rememberLayerBackdrop()
    val blend = rememberBrandLogoBlend()
    val mark = if (!enabled) Modifier else Modifier.textureBlur(
        backdrop = backdrop,
        shape = RoundedCornerShape(0.dp),
        blurRadius = 150f,
        colors = BlurColors(blendColors = blend),
        contentBlendMode = BlendMode.DstIn,
    )
    // The CTA samples the same animated MIUIX blend, at its own screen position.
    val action = if (!enabled) Modifier else Modifier.textureBlur(
        backdrop = backdrop,
        shape = RoundedCornerShape(0.dp),
        blurRadius = 56f,
        colors = BlurColors(blendColors = blend),
        contentBlendMode = BlendMode.DstIn,
    )
    val glint = if (surface.luminance() < 0.5f) {
        lerp(accent, Color.Black, 0.35f)
    } else {
        lerp(accent, Color.White, 0.70f)
    }
    return BrandGuideMaterials(
        colors = BrandGuideColors(fallback, palette, glint, accent),
        glass = BrandGuideGlass(enabled, backdrop, mark, action),
    )
}

@Composable
private fun ColumnScope.BrandGuideIdentity(
    completed: Boolean,
    resumed: Boolean,
    motion: BrandGuideMotion,
    materials: BrandGuideMaterials,
) {
    val diameter = with(LocalDensity.current) { 100.dp.toPx() }
    Box(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GuiyuanAnimatedIdentityMark(
                markSize = 100.dp,
                wave = motion.wave,
                wavePhase = motion.phase,
                onWaveRelease = { motion.phase = it },
                interactive = true,
                active = resumed,
                effectModifier = materials.glass.mark,
            )
            Spacer(Modifier.height(12.dp))
            BrandGuideTitle(motion, diameter, materials)
            if (completed) {
                Spacer(Modifier.height(36.dp))
                BrandGuideCompletion(motion, diameter, materials)
            }
        }
    }
}

@Composable
private fun BrandGuideTitle(
    motion: BrandGuideMotion,
    diameter: Float,
    materials: BrandGuideMaterials,
) {
    Text(
        text = stringResource(R.string.product_name),
        modifier = Modifier
            .onGloballyPositioned { coords ->
                val measured = coords.positionInParent().y +
                    coords.size.height / 2f - diameter / 2f
                if (motion.titleDistance != measured) motion.titleDistance = measured
            }
            .graphicsLayer {
                val lift = waveLift(motion.wave.value, motion.titleDistance, diameter, motion.phase)
                compositingStrategy = if (motion.wave.isRunning) {
                    CompositingStrategy.Offscreen
                } else {
                    CompositingStrategy.Auto
                }
                scaleX = 1f + lift * 0.055f
                scaleY = 1f + lift * 0.035f
                translationY = -lift * 7.dp.toPx()
            }
            .waveGlint(motion.wave, motion.titleDistance, diameter, motion.phase,
                materials.colors.glint, materials.colors.accent)
            .then(materials.glass.mark),
        fontSize = 40.sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        color = MiuixTheme.colorScheme.onBackground,
    )
}

@Composable
private fun BrandGuideCompletion(
    motion: BrandGuideMotion,
    diameter: Float,
    materials: BrandGuideMaterials,
) {
    Text(
        text = stringResource(R.string.guide_done_title),
        modifier = Modifier
            .onGloballyPositioned { coords ->
                val measured = coords.positionInParent().y +
                    coords.size.height / 2f - diameter / 2f
                if (motion.completionDistance != measured) motion.completionDistance = measured
            }
            .graphicsLayer {
                val lift = waveLift(motion.wave.value, motion.completionDistance, diameter, motion.phase)
                compositingStrategy = if (motion.wave.isRunning) {
                    CompositingStrategy.Offscreen
                } else {
                    CompositingStrategy.Auto
                }
                scaleX = 1f + lift * 0.045f
                scaleY = 1f + lift * 0.025f
                translationY = -lift * 5.dp.toPx()
            }
            .waveGlint(motion.wave, motion.completionDistance, diameter, motion.phase,
                materials.colors.glint, materials.colors.accent),
        fontSize = 24.sp,
        textAlign = TextAlign.Center,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    )
}

@Composable
private fun ColumnScope.BrandGuideFinishAction(
    onNext: () -> Unit,
    materials: BrandGuideMaterials,
) {
    Box(Modifier.widthIn(max = 336.dp).fillMaxWidth().heightIn(min = 50.dp)) {
        GuideBrandedActionBackdrop(
            modifier = Modifier.matchParentSize(),
            cornerRadius = 16.dp,
            palette = materials.colors.action,
            material = materials.glass.action,
            enabled = materials.glass.enabled,
        )
        Button(
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
            cornerRadius = 16.dp,
            colors = ButtonDefaults.buttonColors(
                color = Color.Transparent,
                contentColor = materials.colors.action.content,
            ),
            onClick = onNext,
        ) {
            Text(
                text = stringResource(R.string.guide_start),
                style = MiuixTheme.textStyles.button,
            )
        }
    }
    Spacer(Modifier.height(44.dp))
}

@Composable
private fun ColumnScope.BrandGuideContinueAction(
    onNext: () -> Unit,
    materials: BrandGuideMaterials,
) {
    val arrowRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val continueLabel = stringResource(R.string.guide_continue)
    TooltipBox(text = continueLabel) {
        Box(Modifier.size(70.dp)) {
            GuideBrandedActionBackdrop(
                modifier = Modifier.matchParentSize(),
                cornerRadius = 35.dp,
                palette = materials.colors.action,
                material = materials.glass.action,
                enabled = materials.glass.enabled,
            )
            IconButton(
                onClick = onNext,
                modifier = Modifier.size(70.dp).semantics { contentDescription = continueLabel },
                backgroundColor = Color.Transparent,
                cornerRadius = 35.dp,
                minWidth = 70.dp,
                minHeight = 70.dp,
            ) {
                Canvas(Modifier.width(31.dp).height(22.dp)) {
                    val middle = size.height / 2f
                    val tip = size.width * if (arrowRtl) 0.04f else 0.96f
                    val tail = size.width * if (arrowRtl) 0.96f else 0.04f
                    val wing = size.height * 0.42f
                    val base = tip + if (arrowRtl) wing else -wing
                    drawLine(materials.colors.action.content, Offset(tail, middle),
                        Offset(tip, middle), strokeWidth = 2.6.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(materials.colors.action.content, Offset(base, middle - wing),
                        Offset(tip, middle), strokeWidth = 2.6.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(materials.colors.action.content, Offset(base, middle + wing),
                        Offset(tip, middle), strokeWidth = 2.6.dp.toPx(), cap = StrokeCap.Round)
                }
            }
        }
    }
    Spacer(Modifier.weight(0.28f))
}

@Composable
private fun GuideBrandedActionBackdrop(
    modifier: Modifier,
    cornerRadius: Dp,
    palette: GuideActionPalette,
    material: Modifier,
    enabled: Boolean,
) {
    Box(modifier) {
        if (enabled) {
            // Keep live captured MIUIX material as the main fill, just as on the Logo.
            // The original nearly opaque gradient concealed 88%+ of the animation.
            Box(
                Modifier.matchParentSize()
                    .then(material)
                    .squircleSurface(color = Color.White, cornerRadius = cornerRadius),
            )
            // Scrim is translucent, not a second animated color owner.
            Box(
                Modifier.matchParentSize()
                    .squircleClip(cornerRadius)
                    .background(palette.scrim),
            )
        } else {
            // Shader-off devices fall back to an ordinary high-contrast theme pair.
            Box(
                Modifier.matchParentSize()
                    .squircleSurface(color = palette.fallback, cornerRadius = cornerRadius),
            )
        }
    }
}

internal data class GuideActionPalette(
    val scrim: Color,
    val fallback: Color,
    val content: Color,
)

internal fun guideActionPalette(
    surface: Color,
    onSurface: Color,
    logoTint: Color,
): GuideActionPalette = GuideActionPalette(
    scrim = lerp(onSurface, logoTint, 0.35f).copy(alpha = 0.34f),
    fallback = onSurface,
    content = surface,
)
