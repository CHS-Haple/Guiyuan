package com.chaners.guiyuan.ui.components.liquid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.chaners.guiyuan.settings.LiquidMode
import com.chaners.guiyuan.settings.NavContent
import com.chaners.guiyuan.settings.UiStyle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.material.drawEdgeStroke
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.sign

/** One Liquid material/gesture source; only compact vs spacious layout follows the UI style. */
internal object LiquidRailSpec {
    val Margin = 12.dp
    val ItemHeight = 72.dp
    fun width(uiStyle: UiStyle): Dp = if (uiStyle == UiStyle.OS4) 184.dp else 96.dp
    fun height(count: Int): Dp = ItemHeight * count + 8.dp
}

internal data class LiquidRailItem(val icon: ImageVector, val label: String)

internal fun canPlaceLiquidRail(
    window: DpSize,
    uiStyle: UiStyle,
    count: Int,
    startInset: Dp,
    endInset: Dp,
    verticalInsets: Dp,
): Boolean {
    if (count <= 0) return false
    val occupiedWidth = LiquidRailSpec.width(uiStyle) + LiquidRailSpec.Margin * 2
    val minimumHeight = maxOf(320.dp, LiquidRailSpec.height(count) + LiquidRailSpec.Margin * 2)
    return window.width >= occupiedWidth + startInset + endInset + 360.dp &&
        window.height >= minimumHeight + verticalInsets
}

/**
 * Vertical Liquid layout adapted from the accepted horizontal B1842 optics.
 *
 * This is Guiyuan-owned geometry (MIUIX has no published LiquidNavigationRail).
 * It deliberately reuses LiquidDragMotion, LiquidHighlight, theme color, tilt,
 * lens/edge stroke and the recorded background, not OS4 Glass material.
 */
@Composable
internal fun LiquidNavRail(
    selectedIndex: () -> Int,
    onSelected: (Int) -> Unit,
    backdrop: Backdrop,
    items: List<LiquidRailItem>,
    dark: Boolean,
    mode: LiquidMode,
    uiStyle: UiStyle,
    contentMode: NavContent,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val isOs4 = uiStyle == UiStyle.OS4
    val density = LocalDensity.current
    val itemHeightPx = with(density) { LiquidRailSpec.ItemHeight.toPx() }
    val railHeightPx = with(density) { LiquidRailSpec.height(items.size).toPx() }
    val insetPx = with(density) { 4.dp.toPx() }
    val params = mode.params()
    val shape = remember { CircleShape }
    val surfaceColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = params.surfaceAlpha)
    val selectedColor = MiuixTheme.colorScheme.primary
    val normalColor = MiuixTheme.colorScheme.onSurface
    val tabsBackdrop = rememberLayerBackdrop()
    val combinedBackdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop)
    val tilt = rememberGlassTilt(interactive = true)
    val panelHighlight = remember(tilt) { derivedStateOf { litHighlight(tilt.value, -45f) } }
    val pillHighlight = remember(tilt) { derivedStateOf { litHighlight(tilt.value, 90f) } }
    val scope = rememberCoroutineScope()
    var currentIndex by remember(items.size) {
        mutableIntStateOf(selectedIndex().coerceIn(0, items.lastIndex))
    }
    val onSelectedUpdated by rememberUpdatedState(onSelected)
    val bounce = remember { Animatable(0f) }
    val bounceOffset by remember(railHeightPx) {
        derivedStateOf {
            val fraction = (bounce.value / railHeightPx).fastCoerceIn(-1f, 1f)
            insetPx * fraction.sign * EaseOut.transform(abs(fraction))
        }
    }
    val motion = remember(scope, items.size, itemHeightPx) {
        LiquidDragMotion(
            scope = scope,
            initialValue = selectedIndex().toFloat(),
            range = 0f..items.lastIndex.toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 78f / 56f,
            canDrag = { position -> position.y in 0f..railHeightPx },
            onStarted = { position ->
                updateValue(((position.y - insetPx) / itemHeightPx).toInt()
                    .coerceIn(0, items.lastIndex).toFloat())
            },
            onStopped = {
                val target = targetValue.fastRoundToInt().fastCoerceIn(0, items.lastIndex)
                if (currentIndex != target) {
                    currentIndex = target
                    onSelectedUpdated(target)
                }
                updateValue(target.toFloat())
                scope.launch { bounce.animateTo(0f, spring(1f, 300f, 0.5f)) }
            },
            onCancelled = {
                updateValue(currentIndex.toFloat())
                scope.launch { bounce.animateTo(0f, spring(1f, 300f, 0.5f)) }
            },
            onDrag = { _, delta ->
                if (delta.y != 0f) {
                    updateValue((targetValue + delta.y / itemHeightPx)
                        .fastCoerceIn(0f, items.lastIndex.toFloat()))
                    scope.launch { bounce.snapTo(bounce.value + delta.y) }
                }
            },
        )
    }
    LaunchedEffect(selectedIndex, motion) {
        snapshotFlow { selectedIndex() }.collectLatest { index ->
            val target = index.coerceIn(0, items.lastIndex)
            if (currentIndex != target) {
                currentIndex = target
                motion.animateToValue(target.toFloat())
            }
        }
    }
    val highlight = remember(scope, motion, itemHeightPx) {
        LiquidHighlight(scope) { size, _ ->
            Offset(size.width / 2f, (motion.value + 0.5f) * itemHeightPx + bounceOffset)
        }
    }
    Box(modifier.width(LiquidRailSpec.width(uiStyle)).height(LiquidRailSpec.height(items.size))) {
        Column(
            modifier = Modifier
                .selectableGroup()
                .graphicsLayer { translationY = bounceOffset }
                .dropShadow(
                    shape = shape,
                    shadow = Shadow(
                        radius = 10.dp,
                        color = Color.Black,
                        alpha = if (dark) 0.20f else 0.10f,
                    ),
                )
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        padding = maxOf(padding, 40.dp.toPx())
                        vibrancy()
                        blur(params.blur.toPx(), params.blur.toPx())
                        lens(params.refractionHeight.toPx(), params.refractionAmount.toPx())
                    },
                    onDrawFront = {
                        drawEdgeStroke(shape = shape, stroke = panelHighlight.value, alpha = 0.75f)
                    },
                    layerBlock = {
                        val progress = motion.pressProgress
                        val scale = lerp(1f, 1f + 16.dp.toPx() / size.width, progress)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = { drawRect(surfaceColor) },
                )
                .then(highlight.modifier)
                .then(highlight.gestureModifier)
                .then(motion.modifier)
                .fillMaxSize()
                .padding(4.dp),
        ) {
            CompositionLocalProvider(LocalContentColor provides normalColor) {
                items.forEachIndexed { index, item ->
                    LiquidRailEntry(
                        item = item,
                        selected = index == currentIndex,
                        uiStyle = uiStyle,
                        showLabel = isOs4 || contentMode == NavContent.IconAndText,
                        onClick = { onSelectedUpdated(index) },
                    )
                }
            }
        }
        CompositionLocalProvider(LocalContentColor provides selectedColor) {
            Column(
                modifier = Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { shape },
                        effects = {
                            vibrancy()
                            blur(params.blur.toPx(), params.blur.toPx())
                            lens(params.refractionHeight.toPx(), params.refractionAmount.toPx())
                        },
                        onDrawSurface = { drawRect(surfaceColor) },
                    )
                    .then(highlight.modifier)
                    .fillMaxSize()
                    .padding(4.dp),
            ) {
                items.forEach { item ->
                    LiquidRailEntry(
                        item = item,
                        selected = false,
                        uiStyle = uiStyle,
                        showLabel = isOs4 || contentMode == NavContent.IconAndText,
                        onClick = {},
                        interactive = false,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .padding(4.dp)
                .graphicsLayer {
                    translationY = motion.value * itemHeightPx + bounceOffset
                }
                .drawBackdrop(
                    backdrop = combinedBackdrop,
                    shape = { shape },
                    effects = {
                        val progress = motion.pressProgress
                        lens(10.dp.toPx() * progress, 14.dp.toPx() * progress,
                            depthEffect = true, chromaticAberration = 0.5f)
                    },
                    onDrawFront = {
                        drawEdgeStroke(
                            shape = shape,
                            stroke = pillHighlight.value,
                            alpha = motion.pressProgress,
                        )
                    },
                    layerBlock = {
                        scaleX = motion.scaleX
                        scaleY = motion.scaleY
                        val velocity = motion.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = motion.pressProgress
                        drawRect(
                            if (dark) Color.White.copy(alpha = 0.10f)
                            else Color.Black.copy(alpha = 0.10f),
                            alpha = 1f - progress,
                        )
                        drawRect(Color.Black.copy(alpha = 0.03f * progress))
                    },
                )
                .innerShadow(shape = shape) {
                    InnerShadow(
                        radius = 8.dp * motion.pressProgress,
                        color = Color.Black.copy(alpha = 0.15f),
                        alpha = motion.pressProgress,
                    )
                }
                .fillMaxWidth()
                .height(LiquidRailSpec.ItemHeight),
        )
    }
}

@Composable
private fun LiquidRailEntry(
    item: LiquidRailItem,
    selected: Boolean,
    uiStyle: UiStyle,
    showLabel: Boolean,
    onClick: () -> Unit,
    interactive: Boolean = true,
) {
    val input = if (interactive) {
        Modifier
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                this.selected = selected
                onClick { onClick(); true }
            }
            .onKeyEvent { event ->
                val activate = event.key == Key.Enter ||
                    event.key == Key.NumPadEnter || event.key == Key.Spacebar
                if (activate) {
                    if (event.type == KeyEventType.KeyUp) onClick()
                    true
                } else false
            }
            .focusable()
    } else Modifier.clearAndSetSemantics {}
    val modifier = Modifier.fillMaxWidth().height(LiquidRailSpec.ItemHeight)
        .clip(CircleShape).then(input)
    if (uiStyle == UiStyle.OS4) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(12.dp))
            Icon(imageVector = item.icon, contentDescription = if (showLabel) null else item.label,
                modifier = Modifier.size(28.dp))
            if (showLabel) Text(item.label, fontSize = 14.sp)
        }
    } else {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(imageVector = item.icon, contentDescription = if (showLabel) null else item.label,
                modifier = Modifier.size(28.dp))
            if (showLabel) Text(item.label, fontSize = 12.sp)
        }
    }
}
