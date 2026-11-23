// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.chaners.guiyuan.ui.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassPopupVisuals
import top.yukonga.miuix.kmp.material.EdgeStroke
import top.yukonga.miuix.kmp.material.GlassMaterial
import top.yukonga.miuix.kmp.material.GlassStyle

/**
 * Anchor geometry and appearance for a [GlassTransformPopup] or [GlassDropdownPopup].
 *
 * Create with [rememberGlassPopupAnchor], attach [glassPopupAnchor] to the control and optionally
 * use [glassPopupAnchorContent] to identify the content copied during the transition.
 */
@Stable
class GlassPopupAnchor {

    internal var secondaryBackProgressState: State<Float>? by mutableStateOf(null)

    /**
     * Secondary predictive-back progress, including cancellation recovery and the retained exit
     * fraction. Read inside a draw/layer callback to make custom arrows follow the menu:
     * `arrowRotation = { rotation * (1f - anchor.secondaryBackProgress) }`.
     */
    val secondaryBackProgress: Float get() = secondaryBackProgressState?.value ?: 0f

    /** Material supplied by an attached glass button, including its resolved backdrop. */
    internal var surface: GlassAnchorSurface? by mutableStateOf(null)

    /** Expanded menu appearance shared with a secondary popup. */
    internal var popupVisuals: GlassPopupVisuals? by mutableStateOf(null)

    /** Button surface progress, separate from shadow opacity. */
    internal var surfaceProgress: State<Float>? by mutableStateOf(null)
    internal var surfaceMaterialAlpha: Float by mutableFloatStateOf(1f)
    internal val surfaceVisibility: Float get() = surfaceProgress?.value ?: 1f
    internal var surfaceFloating: Boolean by mutableStateOf(false)

    internal var containerCoordinates: LayoutCoordinates? = null
    internal var cachedContainerBounds: Rect? = null
    private var containerBoundsTick by mutableIntStateOf(0)

    /** The control's outer bounds in window coordinates. */
    internal val containerBounds: Rect
        get() {
            if (contentHidden) {
                @Suppress("UNUSED_EXPRESSION")
                containerBoundsTick
            }
            val cached = cachedContainerBounds
            if (cached != null) return cached
            val coordinates = containerCoordinates?.takeIf { it.isAttached } ?: return Rect.Zero
            return coordinates.boundsInWindow().also { cachedContainerBounds = it }
        }

    internal var contentCoordinates: LayoutCoordinates? = null
    internal var cachedContentBounds: Rect? = null
    private var contentBoundsTick by mutableIntStateOf(0)

    /** Control content bounds in window coordinates. */
    internal val contentBounds: Rect
        get() {
            if (contentHidden) {
                @Suppress("UNUSED_EXPRESSION")
                contentBoundsTick
            }
            val cached = cachedContentBounds
            if (cached != null) return cached
            val coordinates = contentCoordinates?.takeIf { it.isAttached } ?: return Rect.Zero
            return coordinates.boundsInWindow().also { cachedContentBounds = it }
        }

    /** Control corner radius used at the collapsed endpoint. */
    internal var cornerRadius: Dp = 0.dp

    /** Whether to keep the panel opaque during the transition instead of fading with the icon. */
    internal var floating: Boolean = false

    /** Hides the transform anchor throughout opening, expansion and exit. */
    internal var contentHidden: Boolean by mutableStateOf(false)

    internal fun updateContainerCoordinates(coordinates: LayoutCoordinates) {
        containerCoordinates = coordinates
        cachedContainerBounds = null
        if (contentHidden) containerBoundsTick++
    }

    internal fun clearContainerCoordinates(coordinates: LayoutCoordinates?) {
        if (containerCoordinates === coordinates) {
            containerCoordinates = null
            cachedContainerBounds = null
        }
    }

    internal fun updateContentCoordinates(coordinates: LayoutCoordinates) {
        contentCoordinates = coordinates
        cachedContentBounds = null
        if (contentHidden) contentBoundsTick++
    }

    internal fun clearContentCoordinates(coordinates: LayoutCoordinates?) {
        if (contentCoordinates === coordinates) {
            contentCoordinates = null
            cachedContentBounds = null
        }
    }


}

/** The button's sampling and colour treatment at the collapsed endpoint of its menu. */
internal data class GlassAnchorSurface(
    val backdrop: Backdrop?,
    val style: GlassStyle,
    val material: GlassMaterial,
    val underlayMaterial: GlassMaterial?,
    val stroke: EdgeStroke?,
    val fill: Color,
)

/** Marks the button's modifier so its renderer can publish the actual surface to this anchor. */
internal data class GlassPopupAnchorElement(
    val anchor: GlassPopupAnchor,
    val cornerRadius: Dp,
    val floating: Boolean,
) : ModifierNodeElement<GlassPopupAnchorNode>() {
    override fun create(): GlassPopupAnchorNode = GlassPopupAnchorNode(anchor, cornerRadius, floating)

    override fun update(node: GlassPopupAnchorNode) {
        node.update(anchor, cornerRadius, floating)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "glassPopupAnchor"
        properties["cornerRadius"] = cornerRadius
        properties["floating"] = floating
    }
}

internal class GlassPopupAnchorNode(
    private var anchor: GlassPopupAnchor,
    private var cornerRadius: Dp,
    private var floating: Boolean,
) : Modifier.Node(),
    LayoutModifierNode,
    GlobalPositionAwareModifierNode {
    private var lastCoordinates: LayoutCoordinates? = null

    override fun onAttach() {
        anchor.cornerRadius = cornerRadius
        anchor.floating = floating
    }

    fun update(anchor: GlassPopupAnchor, cornerRadius: Dp, floating: Boolean) {
        if (this.anchor !== anchor) {
            this.anchor.clearContainerCoordinates(lastCoordinates)
            this.anchor = anchor
            lastCoordinates?.takeIf { it.isAttached }?.let { anchor.updateContainerCoordinates(it) }
        }
        this.cornerRadius = cornerRadius
        this.floating = floating
        anchor.cornerRadius = cornerRadius
        anchor.floating = floating
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        lastCoordinates = coordinates
        anchor.updateContainerCoordinates(coordinates)
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                alpha = if (anchor.contentHidden) 0f else 1f
            }
        }
    }

    override fun onDetach() {
        anchor.clearContainerCoordinates(lastCoordinates)
        lastCoordinates = null
    }
}

private data class GlassPopupAnchorContentElement(
    val anchor: GlassPopupAnchor,
) : ModifierNodeElement<GlassPopupAnchorContentNode>() {
    override fun create(): GlassPopupAnchorContentNode = GlassPopupAnchorContentNode(anchor)

    override fun update(node: GlassPopupAnchorContentNode) {
        node.update(anchor)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "glassPopupAnchorContent"
    }
}

private class GlassPopupAnchorContentNode(
    private var anchor: GlassPopupAnchor,
) : Modifier.Node(),
    GlobalPositionAwareModifierNode {
    private var lastCoordinates: LayoutCoordinates? = null

    fun update(anchor: GlassPopupAnchor) {
        if (this.anchor !== anchor) {
            this.anchor.clearContentCoordinates(lastCoordinates)
            this.anchor = anchor
            lastCoordinates?.takeIf { it.isAttached }?.let { anchor.updateContentCoordinates(it) }
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        lastCoordinates = coordinates
        anchor.updateContentCoordinates(coordinates)
    }

    override fun onDetach() {
        anchor.clearContentCoordinates(lastCoordinates)
        lastCoordinates = null
    }
}

/** One anchor owner across the menu's opening and closing lifecycle. */
@Composable
fun rememberGlassPopupAnchor(): GlassPopupAnchor = remember { GlassPopupAnchor() }

/**
 * Reports a control's bounds to [anchor] and hides it while its menu is open.
 *
 * Apply to the control's outermost node, not its icon. On [GlassIconButton], this also supplies
 * the button's backdrop and surface appearance for the popup's collapsed endpoint.
 *
 * @param anchor Popup anchor.
 * @param cornerRadius Control corner radius; half its height for a pill shape.
 * @param floating Whether the control floats above the page.
 */
@Stable
fun Modifier.glassPopupAnchor(
    anchor: GlassPopupAnchor,
    cornerRadius: Dp,
    floating: Boolean = false,
): Modifier = this.then(GlassPopupAnchorElement(anchor, cornerRadius, floating))

/**
 * Identifies the content copied during a popup transition.
 *
 * Apply to the icon when the popup supplies the control's background to avoid duplicating it.
 * Without this modifier, the entire control is copied.
 *
 * @param anchor Popup anchor.
 */
@Stable
fun Modifier.glassPopupAnchorContent(anchor: GlassPopupAnchor): Modifier = this.then(GlassPopupAnchorContentElement(anchor))
