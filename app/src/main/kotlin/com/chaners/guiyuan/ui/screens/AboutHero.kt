package com.chaners.guiyuan.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaners.guiyuan.BuildConfig
import com.chaners.guiyuan.R
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

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


private val AboutHeroMarkSize = 100.dp
private val AboutHeroTopGap = 92.dp

internal class AboutHeroWave(val pressMotion: MarkPressMotion) {
    val animation = Animatable(0f)
    var phase by mutableFloatStateOf(0f)
}

internal data class AboutHeroMotion(
    val active: Boolean,
    val versionProgress: Float,
    val nameProgress: Float,
    val iconProgress: Float,
)

/** Geometry feedback remains owned by AboutScreen's list and foreground clipping. */
internal data class AboutHeroMeasurements(
    val onHeight: (Dp) -> Unit,
    val onIconBottom: (Float) -> Unit,
    val onNameBottom: (Float) -> Unit,
    val onVersionBottom: (Float) -> Unit,
)

private data class HeroWave(
    val animation: Animatable<Float, AnimationVector1D>,
    val phase: Float,
    val diameter: Float,
    val radius: Float,
    val pressMotion: MarkPressMotion,
)

private data class HeroMaterial(
    val glint: Color,
    val accent: Color,
    val blur: Modifier,
)

internal fun aboutHeroBlur(
    backdrop: LayerBackdrop?,
    logoBlend: List<BlendColorEntry>,
): Modifier = if (backdrop == null) Modifier else Modifier.textureBlur(
    backdrop = backdrop,
    shape = RoundedCornerShape(0.dp),
    blurRadius = 150f,
    colors = BlurColors(blendColors = logoBlend),
    contentBlendMode = BlendMode.DstIn,
)

// The ripple belongs to the page background, not the clipped mark/text foreground.
// Keep the same center and collapse geometry without shrinking its canvas to the mark.
@Composable
internal fun AboutHeroRipple(
    topPadding: Dp,
    ripple: AboutHeroWave,
    iconProgress: Float,
    blur: Modifier,
) {
    if (!ripple.animation.isRunning || iconProgress >= 1f) return
    Box(
        modifier = Modifier.fillMaxWidth()
            .padding(top = topPadding + AboutHeroTopGap)
            .height(AboutHeroMarkSize),
        contentAlignment = Alignment.Center,
    ) {
        IdentityMarkWave(
            markSize = AboutHeroMarkSize,
            wave = ripple.animation,
            wavePhase = ripple.phase,
            effectModifier = blur,
            modifier = Modifier.graphicsLayer {
                alpha = 1f - iconProgress
                val scale = (1f - iconProgress * 0.05f) * ripple.pressMotion.scale.value
                scaleX = scale
                scaleY = scale
            },
        )
    }
}

@Composable
internal fun AboutHero(
    topPadding: Dp,
    blur: Modifier,
    ripple: AboutHeroWave,
    motion: AboutHeroMotion,
    measurements: AboutHeroMeasurements,
) {
    val density = LocalDensity.current
    val markSize = AboutHeroMarkSize
    val diameter = with(density) { markSize.toPx() }
    val accent = MiuixTheme.colorScheme.primary
    val glint = brandWaveGlintColor(MiuixTheme.colorScheme.surface, accent)
    val wave = HeroWave(ripple.animation, ripple.phase, diameter, diameter / 2f, ripple.pressMotion)
    val material = HeroMaterial(glint, accent, blur)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding + AboutHeroTopGap)
            .onSizeChanged { size -> measurements.onHeight(with(density) { size.height.toDp() }) },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AboutHeroMark(
            wave = wave,
            material = material,
            motion = motion,
            onRelease = { ripple.phase = it },
            onBottom = measurements.onIconBottom,
        )
        AboutHeroName(wave, material, motion.nameProgress, measurements.onNameBottom)
        AboutHeroVersion(wave, material, motion.versionProgress, measurements.onVersionBottom)
    }
}

@Composable
private fun AboutHeroMark(
    wave: HeroWave,
    material: HeroMaterial,
    motion: AboutHeroMotion,
    onRelease: (Float) -> Unit,
    onBottom: (Float) -> Unit,
) {
    val size = AboutHeroMarkSize
    val active = motion.active && motion.iconProgress < 1f
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                alpha = 1f - motion.iconProgress
                scaleX = 1f - motion.iconProgress * 0.05f
                scaleY = 1f - motion.iconProgress * 0.05f
            }
            .onGloballyPositioned { coords ->
                onBottom(coords.positionInWindow().y + coords.size.height)
            },
        contentAlignment = Alignment.Center,
    ) {
        GuiyuanAnimatedIdentityMark(
            markSize = size,
            wave = wave.animation,
            wavePhase = wave.phase,
            onWaveRelease = onRelease,
            interactive = active,
            active = active,
            effectModifier = material.blur,
            renderWave = false,
            pressMotion = wave.pressMotion,
        )
    }
}

@Composable
private fun AboutHeroName(
    wave: HeroWave,
    material: HeroMaterial,
    progress: Float,
    onBottom: (Float) -> Unit,
) {
    var titleDistance by remember { mutableFloatStateOf(0f) }
    Text(
        text = stringResource(R.string.product_name),
        modifier = Modifier
            .padding(top = 12.dp, bottom = 5.dp)
            .onGloballyPositioned { coords ->
                onBottom(coords.positionInWindow().y + coords.size.height)
                val distance = coords.positionInParent().y + coords.size.height / 2f - wave.radius
                if (titleDistance != distance) titleDistance = distance
            }
            .graphicsLayer {
                val lift = waveLift(wave.animation.value, titleDistance, wave.diameter, wave.phase)
                alpha = 1f - progress
                compositingStrategy = if (wave.animation.isRunning) CompositingStrategy.Offscreen
                else CompositingStrategy.Auto
                scaleX = (1f - progress * 0.05f) * (1f + lift * 0.055f)
                scaleY = (1f - progress * 0.05f) * (1f + lift * 0.035f)
                translationY = -lift * 7.dp.toPx()
            }
            .waveGlint(wave.animation, titleDistance, wave.diameter, wave.phase, material.glint, material.accent)
            .then(material.blur),
        fontSize = 40.sp,
        fontWeight = FontWeight.ExtraBold,
        color = MiuixTheme.colorScheme.onBackground,
    )
}

@Composable
private fun AboutHeroVersion(
    wave: HeroWave,
    material: HeroMaterial,
    progress: Float,
    onBottom: (Float) -> Unit,
) {
    var versionDistance by remember { mutableFloatStateOf(0f) }
    Text(
        text = "v${BuildConfig.VERSION_NAME}-${BuildConfig.BUILD_ID}",
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                onBottom(coords.positionInWindow().y + coords.size.height)
                val distance = coords.positionInParent().y + coords.size.height / 2f - wave.radius
                if (versionDistance != distance) versionDistance = distance
            }
            .graphicsLayer {
                val lift = waveLift(wave.animation.value, versionDistance, wave.diameter, wave.phase)
                alpha = 1f - progress
                compositingStrategy = if (wave.animation.isRunning) CompositingStrategy.Offscreen
                else CompositingStrategy.Auto
                scaleX = (1f - progress * 0.05f) * (1f + lift * 0.045f)
                scaleY = (1f - progress * 0.05f) * (1f + lift * 0.025f)
                translationY = -lift * 5.dp.toPx()
            }
            .waveGlint(wave.animation, versionDistance, wave.diameter, wave.phase, material.glint, material.accent),
        textAlign = TextAlign.Center,
        fontSize = 17.sp,
        fontWeight = FontWeight.Medium,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    )
}
