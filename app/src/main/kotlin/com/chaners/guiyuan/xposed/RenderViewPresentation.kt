package com.chaners.guiyuan.xposed

import android.graphics.Rect
import android.view.View
import java.lang.ref.WeakReference

internal object VerticalOverflowPolicy {
    internal data class Resolved(
        val physicalTopPx: Int,
        val physicalHeightPx: Int,
        val logicalTopInsetPx: Int,
    )

    fun resolve(
        logicalTopPx: Int,
        logicalHeightPx: Int,
        requestedTopOverflowPx: Int,
        requestedBottomOverflowPx: Int = 0,
    ): Resolved {
        val logicalHeight = logicalHeightPx.coerceAtLeast(0)
        val top = requestedTopOverflowPx.coerceAtLeast(0)
        val bottom = requestedBottomOverflowPx.coerceAtLeast(0)
        return Resolved(
            physicalTopPx = logicalTopPx - top,
            physicalHeightPx = logicalHeight + top + bottom,
            logicalTopInsetPx = top,
        )
    }
}

internal fun RenderView.layoutAtAnchor(bounds: Rect) {
    val width = bounds.width()
    val height = bounds.height()
    val topOverflowPx =
        requiredTopOverflowPx(logicalWidthPx = width, logicalHeightPx = height)
    val bottomOverflowPx =
        requiredBottomOverflowPx(logicalWidthPx = width, logicalHeightPx = height)
    layoutAtAnchor(bounds, topOverflowPx, bottomOverflowPx)
}

internal fun RenderView.layoutAtAnchor(
    bounds: Rect,
    topOverflowPx: Int,
    bottomOverflowPx: Int,
) {
    val physical =
        VerticalOverflowPolicy.resolve(
            logicalTopPx = bounds.top,
            logicalHeightPx = bounds.height(),
            requestedTopOverflowPx = topOverflowPx,
            requestedBottomOverflowPx = bottomOverflowPx,
        )
    setLogicalViewport(
        widthPx = bounds.width(),
        heightPx = bounds.height(),
        topInsetPx = physical.logicalTopInsetPx,
    )
    if (
        measuredWidth != bounds.width() ||
        measuredHeight != physical.physicalHeightPx
    ) {
        measure(
            View.MeasureSpec.makeMeasureSpec(
                bounds.width(),
                View.MeasureSpec.EXACTLY,
            ),
            View.MeasureSpec.makeMeasureSpec(
                physical.physicalHeightPx,
                View.MeasureSpec.EXACTLY,
            ),
        )
    }
    layout(
        bounds.left,
        physical.physicalTopPx,
        bounds.right,
        bounds.bottom + bottomOverflowPx,
    )
}

internal inline fun RenderView.captureTransitionWitness(
    motion: View,
    hostRef: WeakReference<out View>,
    representedSlots: () -> Set<String>,
): TransitionSourceWitness? {
    if (
        !SceneCcPolicy.retainedTransitionSourceWitnessAvailable(
            widthPx = width,
            heightPx = height,
            hostAttached =
                hostRef.get()?.isAttachedToWindow == true &&
                    motion.isAttachedToWindow,
        )
    ) return null
    if (motion.width <= 0 || motion.height <= 0) return null
    return TransitionSourceWitness(
        renderView = this,
        logicalLeftPx = 0,
        logicalTopPx = currentLogicalViewportTopInsetPx(),
        logicalWidthPx = currentLogicalViewportWidthPx(),
        logicalHeightPx = currentLogicalViewportHeightPx(),
        positionHost = hostRef.get() ?: return null,
        motionCarrier = motion,
        representedSlots = representedSlots(),
    )
}

internal fun RenderView.updateRenderVisibility(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.GONE
    if (visible) {
        invalidate()
    } else {
        clearPendingLatency()
    }
}
