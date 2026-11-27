// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
// Modified from upstream (2026): preview interactivity and bottom hit-area extension.

package com.chaners.guiyuan.ui.glass
import top.yukonga.miuix.kmp.glass.GlassNavigationItem
import top.yukonga.miuix.kmp.glass.GlassNavigationBarDefaults

import top.yukonga.miuix.kmp.glass.GlassMotion

import top.yukonga.miuix.kmp.glass.glassPanel

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import com.chaners.guiyuan.ui.components.navTouchExtension
import com.chaners.guiyuan.ui.components.NavTouchStripGeometry
import com.chaners.guiyuan.ui.components.NavTouchTrailingAction
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.computedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import top.yukonga.miuix.kmp.basic.BadgedBox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import androidx.compose.ui.util.fastRoundToInt
import top.yukonga.miuix.kmp.material.GlassShape
import top.yukonga.miuix.kmp.material.GlassDefaults
import top.yukonga.miuix.kmp.material.GlassShadows

/**
 * A floating bottom bar on glass.
 *
 * The indicator follows the pointer while dragging and settles from its current position and
 * velocity on release, without ever overshooting past the outermost destinations.
 *
 * The material, dimensions and palette use the pinned MIUIX defaults.
 *
 * @param items The destinations, in order.
 * @param selectedIndex The index of the current destination.
 * @param onSelect Commits on normal pointer release; keyboard/accessibility activates immediately.
 * @param backdrop The [Backdrop] supplying the content behind the glass.
 * @param modifier The modifier applied to the bar.
 * @param touchExtension Whether safe bottom whitespace also accepts destination taps.
 * @param trailingTouch The independent action's reserved hit width, button size and tap callback.
 * @param interactive Whether destination gestures, keyboard actions and accessibility actions are enabled.
 *   Noninteractive is for a visual preview, not for the real bottom navigation.
 * @param trailingAction Optional independent action outside the glass capsule. It owns its size
 *   and material. Adding or removing it animates its
 *   size, opacity and reserved space. Use [GlassIconButton] for a round action.
 */
@Composable
internal fun GlassNavigationBar(
    items: List<GlassNavigationItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    touchExtension: Boolean = false,
    trailingAction: (@Composable () -> Unit)? = null,
    trailingTouch: NavTouchTrailingAction = NavTouchTrailingAction(),
    previewLabel: (@Composable (Boolean) -> Unit)? = null,
) {
    val shape = GlassShape(CornerSize(50), CornerSize(50), CornerSize(50), CornerSize(50))
    val stroke = GlassNavigationBarDefaults.stroke()
    val height = GlassNavigationBarDefaults.Height
    val material = GlassNavigationBarDefaults.material()
    val indicatorColor = GlassNavigationBarDefaults.indicatorColor()
    val indicatorPressedColor = GlassNavigationBarDefaults.indicatorPressedColor()
    val selectedColor = if (MiuixTheme.isDynamicColor) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onBackground
    val unselectedColor = MiuixTheme.colorScheme.onBackground
    if (items.isEmpty()) return
    val index = selectedIndex.coerceIn(0, items.lastIndex)
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val labelSize = with(density) {
        if (fontScale >= 1.6f) GlassNavigationBarDefaults.LargeLabelSize.toSp() else GlassNavigationBarDefaults.LabelSize.toSp()
    }
    val contentInset = with(density) { GlassNavigationBarDefaults.ContentPaddingHorizontal.toPx() }
    val overhang = with(density) { GlassNavigationBarDefaults.IndicatorOverhang.toPx() }

    val barWidthState = remember { mutableIntStateOf(0) }
    val pressedIndexState = remember { mutableIntStateOf(-1) }
    val dragging = remember { mutableStateOf(false) }
    var settleGeneration by remember { mutableIntStateOf(0) }

    // The hub mounts/unmounts the bar; a mounted preview differs only in input.
    val scope = rememberCoroutineScope()
    val left = remember { Animatable(0f) }
    val right = remember { Animatable(0f) }
    fun animateIndicator(
        newLeft: Float,
        newRight: Float,
        leftSpring: SpringSpec<Float> = GlassMotion.edgeSpring(newLeft <= left.value),
        rightSpring: SpringSpec<Float> = GlassMotion.edgeSpring(newLeft > left.value),
    ) {
        if (left.targetValue == newLeft && right.targetValue == newRight) return
        // Shared springs preserve velocity when the target changes.
        scope.launch { left.animateTo(newLeft, leftSpring) }
        scope.launch { right.animateTo(newRight, rightSpring) }
    }
    // The layout owns width; observe it only while settling an explicit destination.
    LaunchedEffect(index, settleGeneration, items.size, contentInset, overhang) {
        snapshotFlow { barWidthState.intValue }.collectLatest { barWidth ->
            if (!dragging.value && barWidth > contentInset * 2f) {
                val slotWidth = ((barWidth - contentInset * 2f) / items.size).coerceAtLeast(0f)
                val targetLeft = contentInset - overhang + index * slotWidth
                val targetRight = contentInset + overhang + (index + 1) * slotWidth
                if (right.value <= left.value) {
                    left.snapTo(targetLeft)
                    right.snapTo(targetRight)
                } else {
                    animateIndicator(targetLeft, targetRight)
                }
            }
        }
    }

    val chipPressed by remember(pressedIndexState) {
        computedStateOf { pressedIndexState.intValue >= 0 }
    }

    val onSelectState by rememberUpdatedState(onSelect)
    val navigationBars = WindowInsets.navigationBars
    val navigationBarBottom = with(density) { navigationBars.getBottom(this).toDp() }
    val captionBarBottom = with(density) { WindowInsets.captionBar.getBottom(this).toDp() }
    val animatedCaptionBarHeight by animateDpAsState(
        targetValue = captionBarBottom.coerceAtLeast(0.dp),
        animationSpec = tween(durationMillis = 300),
        label = "glassNavigationCaptionBarHeight",
    )
    // Use upstream's maximum effective system inset, not the old additive
    // inset that doubled bottom whitespace when caption and navigation coexist.
    val systemBottom = maxOf(navigationBarBottom, animatedCaptionBarHeight)
    val bottomPadding = when {
        systemBottom <= 16.dp -> 20.dp
        systemBottom >= 28.dp -> systemBottom
        else -> 12.dp + systemBottom
    }

    val cutout = WindowInsets.displayCutout
    val horizontalInsets = remember(cutout, navigationBars) {
        cutout.union(navigationBars).only(WindowInsetsSides.Horizontal)
    }
    val horizontalInsetsModifier = Modifier.windowInsetsPadding(horizontalInsets)
    val wrappedTrailingAction: (@Composable () -> Unit)? = remember(trailingAction, interactive) {
        trailingAction?.let { action ->
            {
                Box(
                    modifier = Modifier
                        .then(if (interactive) Modifier else Modifier.clearAndSetSemantics { })
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) {
                                if (interactive) placeable.placeRelative(0, 0)
                            }
                        },
                ) { action() }
            }
        }
    }
    GlassNavigationBarLayout(
        modifier = Modifier
            .then(horizontalInsetsModifier)
            .then(modifier)
            .fillMaxWidth()
            .then(
                if (touchExtension && interactive &&
                    (trailingAction == null || trailingTouch.width > 0.dp)
                ) {
                    Modifier.navTouchExtension(
                        geometry = NavTouchStripGeometry(
                            bottomPadding = bottomPadding,
                            horizontalPadding = 0.dp,
                            contentPadding = GlassNavigationBarDefaults.ContentPaddingHorizontal,
                            itemCount = items.size,
                            rtl = layoutDirection == LayoutDirection.Rtl,
                        ),
                        onSelect = onSelect,
                        trailing = if (trailingAction != null) trailingTouch else NavTouchTrailingAction(),
                    )
                } else {
                    Modifier
                },
            )
            .padding(bottom = bottomPadding),
        trailingAction = wrappedTrailingAction,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = height)
                .glassPanel(
                    backdrop = backdrop,
                    shape = shape,
                    style = GlassDefaults.Style,
                    alpha = 1f,
                    material = material,
                    stroke = stroke,
                    shadow = GlassShadows.Float,
                    shading = false,
                )
                .onSizeChanged { barWidthState.intValue = it.width }
                .then(
                    if (interactive) {
                        Modifier.pointerInput(items.size, contentInset, overhang, layoutDirection) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val selectionWidth = size.width.toFloat()
                                if (selectionWidth <= contentInset * 2f) return@awaitEachGesture
                                // Freeze hit regions until release; only the indicator previews selection.
                                val selectionSlot = (selectionWidth - contentInset * 2f) / items.size
                                fun logicalX(x: Float) = if (layoutDirection == LayoutDirection.Rtl) size.width - x else x
                                fun indexAt(x: Float): Int = ((x - contentInset) / selectionSlot).toInt().coerceIn(0, items.lastIndex)

                                val minLeft = contentInset - overhang
                                val downX = logicalX(down.position.x)
                                var selectionX = downX
                                var current = indexAt(downX)
                                pressedIndexState.intValue = current
                                dragging.value = true
                                val pressedLeft = minLeft + current * selectionSlot
                                animateIndicator(pressedLeft, pressedLeft + selectionSlot + overhang * 2f)
                                var grabOffset = Float.NaN
                                try {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (change.isConsumed) break
                                        if (!change.pressed) {
                                            if (change.changedToUp()) {
                                                val releaseDelta = change.position.x - change.previousPosition.x
                                                selectionX += if (layoutDirection == LayoutDirection.Rtl) -releaseDelta else releaseDelta
                                                onSelectState(indexAt(selectionX))
                                                change.consume()
                                            }
                                            break
                                        }
                                        // The layout can still resize externally while a pointer is held.
                                        val width = size.width.toFloat()
                                        val chipW = (width - contentInset * 2f) / items.size + overhang * 2f
                                        val maxLeft = (width - chipW).coerceAtLeast(0f)
                                        val x = logicalX(change.position.x)
                                        // Both positions use the current local coordinates, so their
                                        // delta excludes movement caused by the capsule's placement.
                                        val pointerDelta = change.position.x - change.previousPosition.x
                                        val delta = if (layoutDirection == LayoutDirection.Rtl) -pointerDelta else pointerDelta
                                        selectionX += delta
                                        if (grabOffset.isNaN()) {
                                            if (abs(selectionX - downX) < viewConfiguration.touchSlop) {
                                                pressedIndexState.intValue = indexAt(selectionX)
                                                continue
                                            }
                                            // Preserve the press anchor, not the unfinished animation's lag.
                                            grabOffset = downX - pressedLeft
                                        }
                                        val chipLeft = (x - grabOffset).coerceIn(0f, maxLeft)
                                        val next = indexAt(selectionX)
                                        val target = navigationDragTarget(chipLeft, chipW, 0f, width, delta, next != current)
                                        animateIndicator(target.left, target.right, target.leftSpring, target.rightSpring)
                                        current = next
                                        pressedIndexState.intValue = next
                                        change.consume()
                                    }
                                } finally {
                                    pressedIndexState.intValue = -1
                                    dragging.value = false
                                    settleGeneration++
                                }
                            }
                        }
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            GlassNavigationIndicator(
                pressed = chipPressed,
                height = height - GlassNavigationBarDefaults.IndicatorPaddingVertical * 2,
                appearance = GlassIndicatorAppearance(
                    color = indicatorColor,
                    pressedColor = indicatorPressedColor,
                ),
                modifier = Modifier
                    .matchParentSize()
                    .padding(vertical = GlassNavigationBarDefaults.IndicatorPaddingVertical)
                    .layout { measurable, constraints ->
                        // Both edges are clamped on every frame, not just at the target. Keep the
                        // spring alive so the other edge still rebounds.
                        // Match upstream's first-frame selected slot before springs settle.
                        val bounds = if (right.value <= left.value && constraints.maxWidth > contentInset * 2f) {
                            val slotWidth = ((constraints.maxWidth - contentInset * 2f) / items.size).coerceAtLeast(0f)
                            navigationIndicatorBounds(
                                contentInset - overhang + index * slotWidth,
                                contentInset + overhang + (index + 1) * slotWidth,
                                constraints.maxWidth.toFloat(),
                                contentInset - overhang,
                            )
                        } else {
                            navigationIndicatorBounds(
                                left.value,
                                right.value,
                                constraints.maxWidth.toFloat(),
                                contentInset - overhang,
                            )
                        }
                        val start = bounds.x.fastRoundToInt()
                        val width = (bounds.y.fastRoundToInt() - start).coerceAtLeast(0)
                        val placeable = measurable.measure(
                            constraints.copy(minWidth = width, maxWidth = width),
                        )
                        layout(constraints.maxWidth, placeable.height) {
                            placeable.placeRelative(IntOffset(start, 0))
                        }
                    },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
                    .padding(horizontal = GlassNavigationBarDefaults.ContentPaddingHorizontal),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val pressedIndexProvider = remember { { pressedIndexState.intValue } }
                items.forEachIndexed { position, item ->
                    key(item) {
                        GlassNavigationBarItemContent(
                            item = item,
                            position = position,
                            selected = position == index,
                            pressedIndexProvider = pressedIndexProvider,
                            targetTint = if (position == index) selectedColor else unselectedColor,
                            labelSize = labelSize,
                            interactionEnabled = interactive,
                            onSelect = onSelectState,
                            previewLabel = previewLabel,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.GlassNavigationBarItemContent(
    item: GlassNavigationItem,
    position: Int,
    selected: Boolean,
    pressedIndexProvider: () -> Int,
    targetTint: Color,
    labelSize: androidx.compose.ui.unit.TextUnit,
    interactionEnabled: Boolean,
    onSelect: (Int) -> Unit,
    previewLabel: (@Composable (Boolean) -> Unit)?,
) {
    val tintState = animateColorAsState(
        targetValue = targetTint,
        animationSpec = GlassMotion.navContent(),
        label = "glassNavigationTint",
    )
    val tintProducer = remember { ColorProducer { tintState.value } }
    val isPressed by remember(position) {
        computedStateOf { pressedIndexProvider() == position }
    }
    val pressAlpha = animateFloatAsState(
        targetValue = if (isPressed) {
            GlassNavigationBarDefaults.PressedAlpha
        } else {
            1f
        },
        animationSpec = GlassMotion.navContentFloat(),
        label = "glassNavigationPress",
    )
    Box(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = GlassNavigationBarDefaults.Height)
            .graphicsLayer { this.alpha = pressAlpha.value }
            .padding(
                horizontal = GlassNavigationBarDefaults.IndicatorOverhang,
                vertical = GlassNavigationBarDefaults.ContentPaddingVertical,
            )
            .then(
                if (interactionEnabled) {
                    Modifier
                        .navigationTabActivation(
                            selected = selected,
                            description = item.contentDescription ?: item.label,
                            onActivate = { onSelect(position) },
                        )
                        .focusable()
                } else {
                    Modifier.clearAndSetSemantics { }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        GlassNavigationItemVisual(item, selected, tintProducer, labelSize, previewLabel)
    }
}

/** The rendered destination stays separate from the press/selection gesture owner. */
@Composable
private fun GlassNavigationItemVisual(
    item: GlassNavigationItem,
    selected: Boolean,
    tintProducer: ColorProducer,
    labelSize: androidx.compose.ui.unit.TextUnit,
    previewLabel: (@Composable (Boolean) -> Unit)?,
) {
    androidx.compose.foundation.layout.Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(GlassNavigationBarDefaults.LabelSpacing),
    ) {
        BadgedBox(badge = { item.badge?.invoke() }) {
            Icon(
                painter = rememberVectorPainter(item.icon),
                tint = tintProducer,
                contentDescription = null,
                modifier = Modifier.size(GlassNavigationBarDefaults.IconSize),
            )
        }
        item.label?.let { label ->
            if (previewLabel != null) {
                Box(
                    modifier = Modifier.heightIn(min = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    previewLabel(selected)
                }
            } else {
                GlassNavigationItemLabel(label, tintProducer, labelSize)
            }
        }
    }
}

/** Keep large-font caption composition independent of icon press and selection. */
@Composable
private fun GlassNavigationItemLabel(
    label: String,
    tintProducer: ColorProducer,
    labelSize: androidx.compose.ui.unit.TextUnit,
) {
    val baseLabelStyle = MiuixTheme.textStyles.footnote2
    val labelTextStyle = remember(baseLabelStyle, labelSize) {
        baseLabelStyle.copy(fontSize = labelSize)
    }
    Text(
        text = label,
        color = tintProducer,
        style = labelTextStyle,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}
