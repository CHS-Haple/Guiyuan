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
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import com.chaners.guiyuan.ui.components.navTouchExtension
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BadgedBox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.material.EdgeStroke
import top.yukonga.miuix.kmp.material.GlassEdgeStrokes
import top.yukonga.miuix.kmp.material.GlassMaterial
import top.yukonga.miuix.kmp.material.GlassMaterials
import top.yukonga.miuix.kmp.material.GlassShape
import top.yukonga.miuix.kmp.material.GlassStyle
import top.yukonga.miuix.kmp.material.GlassDefaults
import top.yukonga.miuix.kmp.material.GlassShadow
import top.yukonga.miuix.kmp.material.GlassShadows

/**
 * A floating bottom bar on glass.
 *
 * The indicator follows the pointer while dragging and settles from its current position and
 * velocity on release, without ever overshooting past the outermost destinations.
 *
 * @param items The destinations, in order.
 * @param selectedIndex The index of the current destination.
 * @param onSelect Commits on normal pointer release; keyboard/accessibility activates immediately.
 * @param backdrop The [Backdrop] supplying the content behind the glass.
 * @param modifier The modifier applied to the bar.
 * @param defaultWindowInsetsPadding Whether to apply the bottom floating margin and Android
 *   navigation/caption-bar insets. Disable for caller-managed layout and miniature previews.
 * @param style The glass material.
 * @param shape The bar's silhouette. Defaults to a capsule of [height].
 * @param alpha Opacity multiplier for the material.
 * @param visible Whether the bar is on screen. Turning it off does not simply hide it: the bar
 *   shrinks, fades and blurs itself away together, and comes back after a short delay.
 * @param stroke Optional bloom stroke along the rim.
 * @param shadow The shadow the floating capsule casts. `null` removes it.
 * @param height Minimum bar height. Content can grow for two-line or large-font captions.
 * @param material The bar's own body — the blur radius and the colour layers over it. `null`
 *   leaves the bar transparent.
 * @param indicatorPressedColor Fill of the capsule while a finger is on it.
 * @param indicatorColor Fill of the capsule behind the selected destination.
 * @param selectedColor Tint of the selected icon and label.
 * @param unselectedColor Tint of the others. They are not dimmed; the indicator alone says which
 *   one is current.
 * @param touchExtension Whether safe bottom whitespace also accepts destination taps.
 * @param trailingActionTouchExclusion Reserved action width plus gap, supplied by a caller
 *   that knows its action size. Without it, action-bearing bars keep the hit strip disabled.
 * @param trailingActionTouchSize Actual action button width, excluding its gap.
 * @param onTrailingActionTouch The original action callback for short taps in its safe bottom strip.
 * @param interactive Whether destination gestures, keyboard actions and accessibility actions are enabled.
 *   Noninteractive is for a visual preview, not for the real bottom navigation.
 * @param trailingAction Optional independent action outside the glass capsule. It owns its size
 *   and material, and shares the bar's show/hide transition. Adding or removing it animates its
 *   size, opacity and reserved space. Use [GlassIconButton] for a round action.
 */
@Composable
fun GlassNavigationBar(
    items: List<GlassNavigationItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    defaultWindowInsetsPadding: Boolean = true,
    style: GlassStyle = GlassDefaults.Style,
    shape: GlassShape = GlassShape(CornerSize(50), CornerSize(50), CornerSize(50), CornerSize(50)),
    alpha: Float = 1f,
    visible: Boolean = true,
    stroke: EdgeStroke? = GlassNavigationBarDefaults.stroke(),
    shadow: GlassShadow? = GlassShadows.Float,
    height: Dp = GlassNavigationBarDefaults.Height,
    material: GlassMaterial? = GlassNavigationBarDefaults.material(),
    indicatorColor: Color = GlassNavigationBarDefaults.indicatorColor(),
    indicatorPressedColor: Color = GlassNavigationBarDefaults.indicatorPressedColor(),
    selectedColor: Color = if (MiuixTheme.isDynamicColor) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onBackground,
    unselectedColor: Color = MiuixTheme.colorScheme.onBackground,
    interactive: Boolean = true,
    touchExtension: Boolean = false,
    trailingAction: (@Composable () -> Unit)? = null,
    trailingActionTouchExclusion: Dp = 0.dp,
    trailingActionTouchSize: Dp = 0.dp,
    onTrailingActionTouch: (() -> Unit)? = null,
) {
    if (items.isEmpty()) return
    val index = selectedIndex.coerceIn(0, items.lastIndex)
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val labelSize = with(density) {
        if (fontScale >= 1.6f) GlassNavigationBarDefaults.LargeLabelSize.toSp() else GlassNavigationBarDefaults.LabelSize.toSp()
    }
    val contentInset = with(density) { GlassNavigationBarDefaults.ContentPaddingHorizontal.toPx() }
    val overhang = with(density) { GlassNavigationBarDefaults.IndicatorOverhang.toPx() }

    var barWidth by remember { mutableIntStateOf(0) }
    var pressedIndex by remember { mutableIntStateOf(-1) }
    val dragging = remember { mutableStateOf(false) }
    var settleGeneration by remember { mutableIntStateOf(0) }

    val showScale = remember { Animatable(if (visible) 1f else GlassMotion.NAV_HIDE_SCALE) }
    var interactionEnabled by remember { mutableStateOf(visible) }
    LaunchedEffect(visible) {
        if (visible) {
            interactionEnabled = true
            delay(GlassMotion.NAV_SHOW_DELAY_MS)
            showScale.animateTo(1f, GlassMotion.navShowHide())
        } else {
            showScale.animateTo(GlassMotion.NAV_HIDE_SCALE, GlassMotion.navShowHide())
            interactionEnabled = false
        }
    }

    val inputEnabled = interactionEnabled && interactive
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
    val slotWidth = ((barWidth - contentInset * 2f) / items.size).coerceAtLeast(0f)
    val targetLeft = contentInset - overhang + index * slotWidth
    val targetRight = contentInset + overhang + (index + 1) * slotWidth
    val hasSlots = barWidth > contentInset * 2f

    LaunchedEffect(index, settleGeneration, targetLeft, targetRight, hasSlots) {
        if (!dragging.value && hasSlots) {
            if (right.value <= left.value) {
                left.snapTo(targetLeft)
                right.snapTo(targetRight)
            } else {
                animateIndicator(targetLeft, targetRight)
            }
        }
    }

    val chipPressed = pressedIndex >= 0

    val onSelectState by rememberUpdatedState(onSelect)
    val bottomPadding = if (defaultWindowInsetsPadding) {
        val navigationBarBottom = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
            .asPaddingValues().calculateBottomPadding()
        val captionBarBottom = WindowInsets.captionBar.only(WindowInsetsSides.Bottom)
            .asPaddingValues().calculateBottomPadding()
        val animatedCaptionBarHeight by animateDpAsState(
            targetValue = captionBarBottom.coerceAtLeast(0.dp),
            animationSpec = tween(durationMillis = 300),
            label = "glassNavigationCaptionBarHeight",
        )
        // Use upstream's maximum effective system inset, not the old additive
        // inset that doubled bottom whitespace when caption and navigation coexist.
        val systemBottom = maxOf(navigationBarBottom, animatedCaptionBarHeight)
        when {
            systemBottom <= 16.dp -> 20.dp
            systemBottom >= 28.dp -> systemBottom
            else -> 12.dp + systemBottom
        }
    } else {
        0.dp
    }

    GlassNavigationBarLayout(
        modifier = Modifier
            .then(
                if (defaultWindowInsetsPadding) {
                    Modifier.windowInsetsPadding(
                        WindowInsets.displayCutout.union(WindowInsets.navigationBars)
                            .only(WindowInsetsSides.Horizontal),
                    )
                } else {
                    Modifier
                },
            )
            .then(modifier)
            .fillMaxWidth()
            .then(
                if (touchExtension && inputEnabled && defaultWindowInsetsPadding &&
                    (trailingAction == null || trailingActionTouchExclusion > 0.dp)
                ) {
                    Modifier.navTouchExtension(
                        bottomPadding = bottomPadding,
                        horizontalPadding = 0.dp,
                        contentPadding = GlassNavigationBarDefaults.ContentPaddingHorizontal,
                        itemCount = items.size,
                        rtl = layoutDirection == LayoutDirection.Rtl,
                        onSelect = onSelect,
                        trailingActionWidth = if (trailingAction != null) {
                            trailingActionTouchExclusion
                        } else 0.dp,
                        trailingActionSize = if (trailingAction != null) {
                            trailingActionTouchSize
                        } else 0.dp,
                        onTrailingAction = if (trailingAction != null) {
                            onTrailingActionTouch
                        } else null,
                    )
                } else {
                    Modifier
                },
            )
            .padding(bottom = bottomPadding)
            .graphicsLayer {
                val show = showScale.value
                val visibility = ((show - GlassMotion.NAV_HIDE_SCALE) / (1f - GlassMotion.NAV_HIDE_SCALE))
                    .coerceIn(0f, 1f)
                scaleX = show
                scaleY = show
                this.alpha = visibility
                val blurPx = ((1f - visibility) * GlassMotion.NAV_HIDE_BLUR_DP).dp.toPx()
                renderEffect = if (blurPx > 0.5f) BlurEffect(blurPx, blurPx, TileMode.Decal) else null
            },
        trailingAction = trailingAction?.let { action ->
            {
                Box(
                    modifier = Modifier
                        .then(if (inputEnabled) Modifier else Modifier.clearAndSetSemantics { })
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) {
                                if (inputEnabled) placeable.placeRelative(0, 0)
                            }
                        },
                ) { action() }
            }
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = height)
                .glassPanel(
                    backdrop = backdrop,
                    shape = shape,
                    style = style,
                    alpha = alpha,
                    material = material,
                    stroke = stroke,
                    shadow = shadow,
                    shading = false,
                )
                .onSizeChanged { barWidth = it.width }
                .then(
                    if (inputEnabled) {
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
                                pressedIndex = current
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
                                                pressedIndex = indexAt(selectionX)
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
                                        pressedIndex = next
                                        change.consume()
                                    }
                                } finally {
                                    pressedIndex = -1
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
                        val bounds = navigationIndicatorBounds(
                            left.value,
                            right.value,
                            constraints.maxWidth.toFloat(),
                            contentInset - overhang,
                        )
                        val start = bounds.x.roundToInt()
                        val width = (bounds.y.roundToInt() - start).coerceAtLeast(0)
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
                items.forEachIndexed { position, item ->
                    key(item) {
                        val selected = position == index
                        val tint by animateColorAsState(
                            targetValue = if (selected) selectedColor else unselectedColor,
                            animationSpec = GlassMotion.navContent(),
                            label = "glassNavigationTint",
                        )
                        val pressAlpha = animateFloatAsState(
                            targetValue = if (position == pressedIndex) {
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
                                .heightIn(min = height)
                                .graphicsLayer { this.alpha = pressAlpha.value }
                                .padding(
                                    horizontal = GlassNavigationBarDefaults.IndicatorOverhang,
                                    vertical = GlassNavigationBarDefaults.ContentPaddingVertical,
                                )
                                .then(
                                    if (inputEnabled) {
                                        Modifier
                                            .semantics(mergeDescendants = true) {
                                                this.role = Role.Tab
                                                this.selected = selected
                                                (item.contentDescription ?: item.label)?.let {
                                                    this.contentDescription = it
                                                }
                                                onClick {
                                                    onSelectState(position)
                                                    true
                                                }
                                            }
                                            .onKeyEvent { event ->
                                                val activationKey = event.key == Key.Enter ||
                                                    event.key == Key.NumPadEnter ||
                                                    event.key == Key.Spacebar
                                                if (activationKey) {
                                                    if (event.type == KeyEventType.KeyUp) onSelectState(position)
                                                    true
                                                } else {
                                                    false
                                                }
                                            }
                                            .focusable()
                                    } else {
                                        Modifier.clearAndSetSemantics { }
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.foundation.layout.Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(GlassNavigationBarDefaults.LabelSpacing),
                            ) {
                                BadgedBox(badge = { item.badge?.invoke() }) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(GlassNavigationBarDefaults.IconSize),
                                        tint = tint,
                                    )
                                }
                                item.label?.let { label ->
                                    Text(
                                        text = label,
                                        style = MiuixTheme.textStyles.footnote2.copy(fontSize = labelSize),
                                        color = tint,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
