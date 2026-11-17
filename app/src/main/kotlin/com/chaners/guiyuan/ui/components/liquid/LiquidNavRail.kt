package com.chaners.guiyuan.ui.components.liquid

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
import androidx.compose.runtime.State
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.chaners.guiyuan.settings.LiquidMode
import com.chaners.guiyuan.settings.NavContent
import com.chaners.guiyuan.settings.UiStyle
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.material.drawEdgeStroke
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** One Liquid material/gesture source; only compact vs spacious layout follows the UI style. */
internal object LiquidRailSpec {
    val Margin = 12.dp
    val ItemHeight = 72.dp
    fun width(uiStyle: UiStyle): Dp = if (uiStyle == UiStyle.OS4) 184.dp else 96.dp
    fun height(count: Int): Dp = ItemHeight * count + 8.dp
}

internal data class LiquidRailItem(val icon: ImageVector, val label: String)

internal data class LiquidRailSafeArea(val start: Dp, val end: Dp, val vertical: Dp)

internal data class LiquidRailAppearance(
    val items: List<LiquidRailItem>,
    val mode: LiquidMode,
    val dark: Boolean,
    val uiStyle: UiStyle,
    val contentMode: NavContent,
)

private data class LiquidRailTab(
    val item: LiquidRailItem,
    val selected: Boolean,
    val isOs4: Boolean,
    val showLabel: Boolean,
    val interactive: Boolean,
)

internal fun canPlaceLiquidRail(
    window: DpSize,
    uiStyle: UiStyle,
    count: Int,
    safe: LiquidRailSafeArea,
): Boolean {
    if (count <= 0) return false
    val occupiedWidth = LiquidRailSpec.width(uiStyle) + LiquidRailSpec.Margin * 2
    val minimumHeight = maxOf(320.dp, LiquidRailSpec.height(count) + LiquidRailSpec.Margin * 2)
    return window.width >= occupiedWidth + safe.start + safe.end + 360.dp &&
        window.height >= minimumHeight + safe.vertical
}

private data class RailPaint(
    val backdrop: Backdrop,
    val tabsBackdrop: LayerBackdrop,
    val combinedBackdrop: Backdrop,
    val params: LiquidParams,
    val surfaceColor: Color,
    val normalColor: Color,
    val selectedColor: Color,
    val panelHighlight: State<top.yukonga.miuix.kmp.material.EdgeStroke>,
    val pillHighlight: State<top.yukonga.miuix.kmp.material.EdgeStroke>,
    val dark: Boolean,
)

/** Pointer position, selected-page mirroring and bounce form one gesture owner. */
@Composable
internal fun LiquidNavRail(
    selectedIndex: () -> Int,
    onSelected: (Int) -> Unit,
    backdrop: Backdrop,
    appearance: LiquidRailAppearance,
    modifier: Modifier = Modifier,
) {
    if (appearance.items.isEmpty()) return
    val gesture = rememberRailGesture(selectedIndex, onSelected, appearance.items.size)
    val params = appearance.mode.params()
    val colorScheme = MiuixTheme.colorScheme
    val tilt = rememberGlassTilt(interactive = true)
    val tabsBackdrop = rememberLayerBackdrop()
    val paint = RailPaint(
        backdrop = backdrop,
        tabsBackdrop = tabsBackdrop,
        combinedBackdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
        params = params,
        surfaceColor = colorScheme.surfaceContainer.copy(alpha = params.surfaceAlpha),
        normalColor = colorScheme.onSurface,
        selectedColor = colorScheme.primary,
        panelHighlight = remember(tilt) { derivedStateOf { litHighlight(tilt.value, -45f) } },
        pillHighlight = remember(tilt) { derivedStateOf { litHighlight(tilt.value, 90f) } },
        dark = appearance.dark,
    )
    Box(modifier.width(LiquidRailSpec.width(appearance.uiStyle))
        .height(LiquidRailSpec.height(appearance.items.size))) {
        LiquidRailBody(appearance, gesture, paint)
        LiquidRailCapturedTabs(appearance, paint)
        LiquidRailSelectedLens(gesture, paint)
    }
}

@Composable
private fun LiquidRailBody(
    appearance: LiquidRailAppearance,
    gesture: RailGesture,
    paint: RailPaint,
) {
    val motion = gesture.motion
    Column(
        modifier = Modifier
            .selectableGroup()
            .graphicsLayer { translationY = gesture.bounceOffset.value }
            .dropShadow(
                shape = CircleShape,
                shadow = Shadow(
                    radius = 10.dp,
                    color = Color.Black,
                    alpha = if (paint.dark) 0.20f else 0.10f,
                ),
            )
            .drawBackdrop(
                backdrop = paint.backdrop,
                shape = { CircleShape },
                effects = {
                    padding = maxOf(padding, 40.dp.toPx())
                    vibrancy()
                    blur(paint.params.blur.toPx(), paint.params.blur.toPx())
                    lens(paint.params.refractionHeight.toPx(), paint.params.refractionAmount.toPx())
                },
                onDrawFront = {
                    drawEdgeStroke(shape = CircleShape, stroke = paint.panelHighlight.value, alpha = 0.75f)
                },
                layerBlock = {
                    val progress = motion.pressProgress
                    val scale = lerp(1f, 1f + 16.dp.toPx() / size.width, progress)
                    scaleX = scale
                    scaleY = scale
                },
                onDrawSurface = { drawRect(paint.surfaceColor) },
            )
            .then(gesture.highlight.modifier)
            .then(gesture.highlight.gestureModifier)
            .then(motion.modifier)
            .fillMaxSize()
            .padding(4.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides paint.normalColor) {
            LiquidRailItems(appearance, gesture.selectedIndex.value, interactive = true, gesture.onSelect)
        }
    }
}

@Composable
private fun LiquidRailCapturedTabs(
    appearance: LiquidRailAppearance,
    paint: RailPaint,
) {
    CompositionLocalProvider(LocalContentColor provides paint.selectedColor) {
        Column(
            modifier = Modifier
                .clearAndSetSemantics {}
                .alpha(0f)
                .layerBackdrop(paint.tabsBackdrop)
                .drawBackdrop(
                    backdrop = paint.backdrop,
                    shape = { CircleShape },
                    effects = {
                        vibrancy()
                        blur(paint.params.blur.toPx(), paint.params.blur.toPx())
                        lens(paint.params.refractionHeight.toPx(), paint.params.refractionAmount.toPx())
                    },
                    onDrawSurface = { drawRect(paint.surfaceColor) },
                )
                .fillMaxSize()
                .padding(4.dp),
        ) {
            LiquidRailItems(appearance, selectedIndex = -1, interactive = false, onSelect = {})
        }
    }
}

@Composable
private fun LiquidRailItems(
    appearance: LiquidRailAppearance,
    selectedIndex: Int,
    interactive: Boolean,
    onSelect: (Int) -> Unit,
) {
    appearance.items.forEachIndexed { index, item ->
        LiquidRailEntry(
            tab = LiquidRailTab(
                item = item,
                selected = index == selectedIndex,
                isOs4 = appearance.uiStyle == UiStyle.OS4,
                showLabel = appearance.uiStyle == UiStyle.OS4 ||
                    appearance.contentMode == NavContent.IconAndText,
                interactive = interactive,
            ),
            onClick = { onSelect(index) },
        )
    }
}

@Composable
private fun LiquidRailSelectedLens(gesture: RailGesture, paint: RailPaint) {
    val motion = gesture.motion
    Box(
        modifier = Modifier
            .padding(4.dp)
            .graphicsLayer {
                translationY = motion.value * gesture.itemHeightPx + gesture.bounceOffset.value
            }
            .drawBackdrop(
                backdrop = paint.combinedBackdrop,
                shape = { CircleShape },
                effects = {
                    val progress = motion.pressProgress
                    lens(10.dp.toPx() * progress, 14.dp.toPx() * progress,
                        depthEffect = true, chromaticAberration = 0.5f)
                },
                onDrawFront = {
                    drawEdgeStroke(
                        shape = CircleShape,
                        stroke = paint.pillHighlight.value,
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
                        if (paint.dark) Color.White.copy(alpha = 0.10f)
                        else Color.Black.copy(alpha = 0.10f),
                        alpha = 1f - progress,
                    )
                    drawRect(Color.Black.copy(alpha = 0.03f * progress))
                },
            )
            .innerShadow(shape = CircleShape) {
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

@Composable
private fun LiquidRailEntry(
    tab: LiquidRailTab,
    onClick: () -> Unit,
) {
    val item = tab.item
    val showLabel = tab.showLabel
    val modifier = Modifier.fillMaxWidth().height(LiquidRailSpec.ItemHeight)
        .clip(CircleShape).liquidNavTabInput(tab.selected, tab.interactive, onClick)
    if (tab.isOs4) {
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
