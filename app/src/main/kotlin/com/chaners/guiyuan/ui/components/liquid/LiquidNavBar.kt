package com.chaners.guiyuan.ui.components.liquid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.chaners.guiyuan.settings.LiquidMode
import com.chaners.guiyuan.settings.NavContent
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.sensor.rememberDeviceTilt
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

// Liquid optics adapted from the MIUIX Liquid Glass example (Apache-2.0).
// Guiyuan retains navigation content, gestures and fallback ownership.
private val glassHighlight = Highlight(
    width = 1.dp,
    alpha = 1f,
    style = BloomStroke(
        color = Color.White.copy(alpha = 0.12f),
        innerBlurRadius = 2.dp,
        primaryLight = LightSource(
            position = LightPosition(0.5f, -0.3f, -0.05f),
            color = Color.White,
            intensity = 1f,
        ),
        secondaryLight = LightSource(
            position = LightPosition(0.5f, 0.8f, -0.5f),
            color = Color.White,
            intensity = 0.4f,
        ),
        dualPeak = true,
    ),
)

private const val GRAVITY_THRESHOLD_SQ = 0.01f
private val GRAVITY_STEP = (3.0 * PI / 180.0).toFloat()

@Composable
private fun rememberGravityAngle(interactive: Boolean): State<Float> {
    if (!interactive) return remember { mutableFloatStateOf((-PI / 2).toFloat()) }
    // MIUIX owns the sensor listener; static previews do not register it.
    val tilt = rememberDeviceTilt()
    return remember(tilt) {
        derivedStateOf {
            val gx = tilt.value.gravityX
            val gy = tilt.value.gravityY
            if (gx * gx + gy * gy > GRAVITY_THRESHOLD_SQ) {
                (atan2(gy, gx) / GRAVITY_STEP).roundToInt() * GRAVITY_STEP
            } else {
                (-PI / 2).toFloat()
            }
        }
    }
}

private fun litHighlight(angle: Float, offsetDegrees: Float): Highlight {
    val style = glassHighlight.style as BloomStroke
    val rad = angle + (offsetDegrees * PI / 180.0).toFloat()
    return glassHighlight.copy(
        style = style.copy(
            primaryLight = style.primaryLight.copy(
                position = LightPosition(
                    0.5f + cos(rad),
                    0.7f + sin(rad),
                    style.primaryLight.position.z,
                ),
            ),
        ),
    )
}

internal object LiquidNavSpec {
    val height = 64.dp
    val sidePadding = 36.dp
}

private data class LiquidParams(
    val blur: Dp,
    val refractionHeight: Dp,
    val refractionAmount: Dp,
    val surfaceAlpha: Float,
)

private fun LiquidMode.params(): LiquidParams =
    when (this) {
        LiquidMode.Blur ->
            LiquidParams(
                blur = 8.dp,
                refractionHeight = 24.dp,
                refractionAmount = 24.dp,
                surfaceAlpha = 0.40f,
            )
        LiquidMode.Default ->
            LiquidParams(
                blur = 4.dp,
                refractionHeight = 24.dp,
                refractionAmount = 24.dp,
                surfaceAlpha = 0.40f,
            )
        LiquidMode.Clear ->
            LiquidParams(
                blur = 2.dp,
                refractionHeight = 24.dp,
                refractionAmount = 24.dp,
                surfaceAlpha = 0.30f,
            )
    }

@Composable
internal fun liquidNavBottomPadding(): Dp {
    val inset =
        WindowInsets.navigationBars
            .only(WindowInsetsSides.Bottom)
            .asPaddingValues()
            .calculateBottomPadding()
    return if (inset != 0.dp) 26.dp + inset else 36.dp
}

@Composable
internal fun LiquidNavBar(
    selectedIndex: () -> Int,
    onSelected: (Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    dark: Boolean,
    mode: LiquidMode = LiquidMode.Default,
    interactive: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    require(tabsCount > 0)

    val selectedColor = MiuixTheme.colorScheme.primary
    val normalColor = MiuixTheme.colorScheme.onSurface
    val params = mode.params()
    val containerColor =
        MiuixTheme.colorScheme.surfaceContainer.copy(alpha = params.surfaceAlpha)
    val tabsBackdrop = rememberLayerBackdrop()
    val pillShape = remember { CircleShape }
    val gravity = rememberGravityAngle(interactive)
    val panelHighlight = remember(gravity) {
        derivedStateOf { litHighlight(gravity.value, -45f) }
    }
    val pillHighlight = remember(gravity) {
        derivedStateOf { litHighlight(gravity.value, 90f) }
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart,
    ) {
        val density = LocalDensity.current
        val tabWidth =
            with(density) {
                (constraints.maxWidth.toFloat() - 8.dp.toPx()) / tabsCount
            }

        val widthPx = rememberUpdatedState(constraints.maxWidth)
        val tabWidthPx = rememberUpdatedState(tabWidth)
        val offsetAnim = remember { Animatable(0f) }
        val panelOffset by remember(density) {
            derivedStateOf {
                val fraction =
                    (offsetAnim.value / widthPx.value)
                        .fastCoerceIn(-1f, 1f)
                with(density) {
                    4.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }

        val ltr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val currentLtr = rememberUpdatedState(ltr)
        val scope = rememberCoroutineScope()
        var currentIndex by remember(selectedIndex) {
            mutableIntStateOf(selectedIndex())
        }
        val motion =
            // The tab count owns the animation range; dimensions remain live.
            remember(scope, tabsCount) {
                LiquidDragMotion(
                    scope = scope,
                    initialValue = selectedIndex().toFloat(),
                    range = 0f..(tabsCount - 1).toFloat(),
                    visibilityThreshold = 0.001f,
                    initialScale = 1f,
                    pressedScale = 78f / 56f,
                    onStopped = {
                        val target =
                            targetValue
                                .fastRoundToInt()
                                .fastCoerceIn(0, tabsCount - 1)
                        currentIndex = target
                        animateToValue(target.toFloat())
                        scope.launch {
                            offsetAnim.animateTo(
                                0f,
                                spring(1f, 300f, 0.5f),
                            )
                        }
                    },
                    onDrag = { _, dragAmount ->
                        updateValue(
                            (
                                targetValue +
                                    dragAmount.x / tabWidthPx.value * if (currentLtr.value) 1f else -1f
                            ).fastCoerceIn(0f, (tabsCount - 1).toFloat()),
                        )
                        scope.launch {
                            offsetAnim.snapTo(offsetAnim.value + dragAmount.x)
                        }
                    },
                )
            }

        LaunchedEffect(selectedIndex) {
            snapshotFlow { selectedIndex() }
                .collectLatest { index ->
                    currentIndex = index.fastCoerceIn(0, tabsCount - 1)
                }
        }
        LaunchedEffect(motion) {
            snapshotFlow { currentIndex }
                .drop(1)
                .collectLatest { index ->
                    motion.animateToValue(index.toFloat())
                    onSelected(index)
                }
        }

        val highlight =
            remember(scope, motion, density) {
                LiquidHighlight(
                    scope = scope,
                    position = { size, _ ->
                        Offset(
                            x =
                                if (currentLtr.value) {
                                    (motion.value + 0.5f) * tabWidthPx.value + panelOffset
                                } else {
                                    size.width -
                                        (motion.value + 0.5f) * tabWidthPx.value +
                                        panelOffset
                                },
                            y = size.height / 2f,
                        )
                    },
                )
            }

        Row(
            modifier =
                Modifier
                    .graphicsLayer {
                        translationX = panelOffset
                    }
                    .dropShadow(
                        shape = pillShape,
                        shadow = Shadow(
                            radius = 10.dp,
                            color = Color.Black,
                            alpha = if (dark) 0.20f else 0.10f,
                        ),
                    )
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { pillShape },
                        effects = {
                            padding = maxOf(padding, 40.dp.toPx())
                            vibrancy()
                            blur(params.blur.toPx(), params.blur.toPx())
                            lens(params.refractionHeight.toPx(), params.refractionAmount.toPx())
                        },
                        highlight = { panelHighlight.value.copy(alpha = 0.75f) },
                        layerBlock = {
                            val progress = motion.pressProgress
                            val scale =
                                lerp(
                                    1f,
                                    1f + 16.dp.toPx() / size.width,
                                    progress,
                                )
                            scaleX = scale
                            scaleY = scale
                        },
                        onDrawSurface = {
                            drawRect(containerColor)
                        },
                    )
                    .then(highlight.modifier)
                    .height(LiquidNavSpec.height)
                    .fillMaxWidth()
                    .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = {
                CompositionLocalProvider(LocalContentColor provides normalColor) {
                    content()
                }
            },
        )

        CompositionLocalProvider(
            LocalLiquidItemScale provides {
                lerp(1f, 1.2f, motion.pressProgress)
            },
            LocalContentColor provides selectedColor,
        ) {
            Row(
                modifier =
                    Modifier
                        .clearAndSetSemantics {}
                        .alpha(0f)
                        .layerBackdrop(tabsBackdrop)
                        .graphicsLayer {
                            translationX = panelOffset
                        }
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { pillShape },
                            effects = {
                                vibrancy()
                                blur(params.blur.toPx(), params.blur.toPx())
                                lens(params.refractionHeight.toPx(), params.refractionAmount.toPx())
                            },
                            onDrawSurface = { drawRect(containerColor) },
                        )
                        .then(highlight.modifier)
                        .height(56.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }

        Box(
            modifier =
                Modifier
                    .padding(horizontal = 4.dp)
                    .graphicsLayer {
                        translationX =
                            if (ltr) {
                                motion.value * tabWidth + panelOffset
                            } else {
                                size.width -
                                    (motion.value + 1f) * tabWidth +
                                    panelOffset
                            }
                    }
                    .then(if (interactive) highlight.gestureModifier else Modifier)
                    .then(if (interactive) motion.modifier else Modifier)
                    .drawBackdrop(
                        backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                        shape = { pillShape },
                        effects = {
                            val progress = motion.pressProgress
                            lens(
                                10.dp.toPx() * progress,
                                14.dp.toPx() * progress,
                                depthEffect = true,
                                chromaticAberration = 0.5f,
                            )
                        },
                        highlight = { pillHighlight.value.copy(alpha = motion.pressProgress) },
                        layerBlock = {
                            scaleX = motion.scaleX
                            scaleY = motion.scaleY
                            val velocity = motion.velocity / 10f
                            scaleX /=
                                1f -
                                    (velocity * 0.75f)
                                        .fastCoerceIn(-0.2f, 0.2f)
                            scaleY *=
                                1f -
                                    (velocity * 0.25f)
                                        .fastCoerceIn(-0.2f, 0.2f)
                        },
                        onDrawSurface = {
                            val progress = motion.pressProgress
                            drawRect(
                                if (dark) {
                                    Color.White.copy(alpha = 0.10f)
                                } else {
                                    Color.Black.copy(alpha = 0.10f)
                                },
                                alpha = 1f - progress,
                            )
                            drawRect(
                                Color.Black.copy(alpha = 0.03f * progress),
                            )
                        },
                    )
                    .innerShadow(shape = pillShape) {
                        InnerShadow(
                            radius = 8.dp * motion.pressProgress,
                            color = Color.Black.copy(alpha = 0.15f),
                            alpha = motion.pressProgress,
                        )
                    }
                    .height(56.dp)
                    .fillMaxWidth(1f / tabsCount),
        )
    }
}

private val LocalLiquidItemScale =
    staticCompositionLocalOf { { 1f } }

@Composable
internal fun RowScope.LiquidNavItem(
    onClick: () -> Unit,
    interactive: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scale = LocalLiquidItemScale.current
    Column(
        modifier =
            modifier
                .clip(CircleShape)
                .then(
                    if (interactive) {
                        Modifier.clickable(
                            interactionSource = null,
                            indication = null,
                            role = Role.Tab,
                            onClick = onClick,
                        )
                    } else {
                        Modifier.clearAndSetSemantics {}
                    },
                )
                .fillMaxHeight()
                .weight(1f)
                .graphicsLayer {
                    val itemScale = scale()
                    scaleX = itemScale
                    scaleY = itemScale
                },
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

@Composable
internal fun RowScope.LiquidNavEntry(
    contentMode: NavContent,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    interactive: Boolean = true,
) {
    LiquidNavItem(onClick = onClick, interactive = interactive) {
        Icon(
            imageVector = icon,
            contentDescription =
                if (contentMode == NavContent.IconOnly) label else null,
            modifier = Modifier.size(28.dp),
        )
        if (contentMode == NavContent.IconAndText) {
            Text(
                text = label,
                fontSize = 12.sp,
            )
        }
    }
}
