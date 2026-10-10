package com.chaners.guiyuan.xposed

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator
import com.chaners.guiyuan.settings.VisualCfg
import com.chaners.guiyuan.xposed.battery.BatteryColorPolicy
import com.chaners.guiyuan.xposed.network.CenterIndicator

internal class RenderView(
    context: Context,
    private val onDrawn: ((View) -> Unit)? = null,
    private val onStateRendered: (
        latencyMs: Long,
        committedOnMainThread: Boolean,
        sample: RuntimeRenderLatencySample?,
    ) -> Unit = { _, _, _ -> },
) : View(context) {
    private val painter = StatusPainter(context)
    private val centerEnterInterpolator: Interpolator =
        AnimationUtils.loadInterpolator(
            context,
            android.R.interpolator.linear_out_slow_in,
        )
    private val centerExitInterpolator: Interpolator =
        AnimationUtils.loadInterpolator(
            context,
            android.R.interpolator.fast_out_linear_in,
        )

    @Volatile
    private var model: RenderModel? = null

    private var previousCenterIndicator: CenterIndicator? = null
    private var centerTransitionFraction = 1f
    private var centerTransitionAnimator: ValueAnimator? = null

    @Volatile
    private var tintState: TintState? = null

    @Volatile
    private var visual = VisualCfg()

    // Color preferences depend on configuration, not on each animation frame.
    private var colorPrefsSource = visual
    private var colorPrefs = BatteryColorPolicy.preferencesFor(visual)
    private var colorCache: ColorCache? = null

    @Volatile
    private var logicalViewportWidthPx: Int = 0

    @Volatile
    private var logicalViewportHeightPx: Int = 0

    @Volatile
    private var logicalViewportTopInsetPx: Int = 0

    @Volatile
    private var scaleMobileTypeWithCanvas = false

    @Volatile
    private var pendingStateUptimeMs: Long = 0

    @Volatile
    private var pendingStateCommittedOnMainThread: Boolean = false

    @Volatile
    private var pendingTrace: RuntimeRenderTrace? = null

    @Volatile
    private var pendingModelCommittedNanos: Long = 0L

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        setWillNotDraw(false)
    }

    fun setModel(
        model: RenderModel?,
        trace: RuntimeRenderTrace? = null,
    ) {
        if (this.model == model) {
            return
        }

        val previousModel = this.model
        this.model = model
        pendingStateUptimeMs = SystemClock.uptimeMillis()
        pendingStateCommittedOnMainThread =
            Looper.myLooper() === Looper.getMainLooper()
        pendingTrace = trace
        pendingModelCommittedNanos =
            if (trace == null) {
                0L
            } else {
                SystemClock.elapsedRealtimeNanos()
            }

        val previousCenter = previousModel?.centerIndicator
        val nextCenter = model?.centerIndicator
        if (Looper.myLooper() === Looper.getMainLooper()) {
            applyCenterTransitionPolicy(
                previous = previousCenter,
                current = nextCenter,
            )
        } else {
            post {
                if (this.model?.centerIndicator == nextCenter) {
                    applyCenterTransitionPolicy(
                        previous = previousCenter,
                        current = nextCenter,
                    )
                }
            }
        }

        requestRedraw()
    }

    fun setTintState(state: TintState) {
        if (tintState == state) {
            return
        }
        tintState = state
        requestRedraw()
    }

    fun setVisualCfg(next: VisualCfg) {
        if (visual == next) {
            return
        }
        visual = next
        requestRedraw()
    }

    fun setScaleMobileTypeWithCanvas(enabled: Boolean) {
        if (scaleMobileTypeWithCanvas == enabled) {
            return
        }
        scaleMobileTypeWithCanvas = enabled
        requestRedraw()
    }

    fun setLogicalViewport(
        widthPx: Int,
        heightPx: Int,
        topInsetPx: Int,
    ) {
        val resolvedWidth = widthPx.coerceAtLeast(0)
        val resolvedHeight = heightPx.coerceAtLeast(0)
        val resolvedTopInset = topInsetPx.coerceAtLeast(0)
        if (
            logicalViewportWidthPx == resolvedWidth &&
            logicalViewportHeightPx == resolvedHeight &&
            logicalViewportTopInsetPx == resolvedTopInset
        ) {
            return
        }
        logicalViewportWidthPx = resolvedWidth
        logicalViewportHeightPx = resolvedHeight
        logicalViewportTopInsetPx = resolvedTopInset
        requestRedraw()
    }

    fun currentLogicalViewportWidthPx(): Int =
        logicalViewportWidthPx.takeIf { it > 0 } ?: width

    fun currentLogicalViewportHeightPx(): Int =
        logicalViewportHeightPx.takeIf { it > 0 } ?: height

    fun currentLogicalViewportTopInsetPx(): Int =
        logicalViewportTopInsetPx.coerceAtLeast(0)

    fun requiredTopOverflowPx(
        logicalWidthPx: Int,
        logicalHeightPx: Int,
    ): Int {
        val current = model ?: return 0
        return painter.requiredTopOverflowPx(
            width = logicalWidthPx,
            height = logicalHeightPx,
            model = current,
            visual = this.visual,
            previousCenterIndicator = previousCenterIndicator,
            // Reserve both full endpoints so the animated glyph is not clipped.
            centerExitAmount = 1f,
            centerEnterAmount = 1f,
            scaleMobileTypeWithCanvas = scaleMobileTypeWithCanvas,
        )
    }

    fun clearPendingLatency() {
        pendingStateUptimeMs = 0L
        pendingTrace = null
        pendingModelCommittedNanos = 0L
    }

    private fun applyCenterTransitionPolicy(
        previous: CenterIndicator?,
        current: CenterIndicator?,
    ) {
        when (
            CenterTransitionPolicy.decide(
                previous = previous,
                current = current,
                activeSource = previousCenterIndicator,
                transitionRunning = centerTransitionAnimator != null,
            )
        ) {
            CenterTransitionPolicy.Decision.START ->
                startCenterTransition(
                    previous = checkNotNull(previous),
                    current = checkNotNull(current),
                )

            CenterTransitionPolicy.Decision.KEEP ->
                invalidate()

            CenterTransitionPolicy.Decision.SNAP ->
                cancelCenterTransition()
        }
    }

    private fun startCenterTransition(
        previous: CenterIndicator,
        current: CenterIndicator,
    ) {
        centerTransitionAnimator?.cancel()
        previousCenterIndicator = previous
        centerTransitionFraction = 0f

        centerTransitionAnimator =
            ValueAnimator
                .ofFloat(0f, 1f)
                .apply {
                    duration = CENTER_TRANSITION_DURATION_MS
                    addUpdateListener { animator ->
                        centerTransitionFraction =
                            (animator.animatedValue as Float)
                                .coerceIn(0f, 1f)
                        invalidate()
                    }
                    addListener(
                        object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                if (centerTransitionAnimator === animation) {
                                    centerTransitionAnimator = null
                                    previousCenterIndicator = null
                                    centerTransitionFraction = 1f
                                    invalidate()
                                }
                            }
                        },
                    )
                    start()
                }
    }

    private fun cancelCenterTransition() {
        centerTransitionAnimator?.cancel()
        centerTransitionAnimator = null
        previousCenterIndicator = null
        centerTransitionFraction = 1f
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        painter.clearNativeResources()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        painter.clearNativeResources()
        requestRedraw()
    }

    override fun onDetachedFromWindow() {
        cancelCenterTransition()
        super.onDetachedFromWindow()
    }

    private fun requestRedraw() {
        if (Looper.myLooper() === Looper.getMainLooper()) {
            invalidate()
        } else {
            postInvalidateOnAnimation()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val current = model ?: return
        val tint = tintState ?: return
        val currentVisual = visual
        if (currentVisual !== colorPrefsSource) {
            colorPrefs = BatteryColorPolicy.preferencesFor(currentVisual)
            colorPrefsSource = currentVisual
        }
        val transitionFraction =
            centerTransitionFraction.coerceIn(0f, 1f)
        val logicalWidth = currentLogicalViewportWidthPx()
        val logicalHeight = currentLogicalViewportHeightPx()
        val logicalTopInset = currentLogicalViewportTopInsetPx()
        val viewportSave = canvas.save()
        if (logicalTopInset > 0) {
            canvas.translate(0f, logicalTopInset.toFloat())
        }
        val colors =
            colorCache
                ?.takeIf { cache ->
                    cache.model === current &&
                        cache.tint === tint &&
                        cache.visual === currentVisual
                }?.colors
                ?: ColorPolicy.resolve(
                    model = current,
                    tintState = tint,
                    visualSettings = currentVisual,
                    batteryColorPreferences = colorPrefs,
                ).also { resolved ->
                    colorCache = ColorCache(current, tint, currentVisual, resolved)
                }

        painter.draw(
            canvas = canvas,
            width = logicalWidth,
            height = logicalHeight,
            model = current,
            colors = colors,
            opacity = 1f,
            visual = currentVisual,
            previousCenterIndicator = previousCenterIndicator,
            centerExitAmount =
                1f -
                    centerExitInterpolator
                        .getInterpolation(transitionFraction),
            centerEnterAmount =
                centerEnterInterpolator
                    .getInterpolation(transitionFraction),
            scaleMobileTypeWithCanvas = scaleMobileTypeWithCanvas,
        )
        canvas.restoreToCount(viewportSave)
        onDrawn?.invoke(this)

        val committedAt = pendingStateUptimeMs
        if (committedAt != 0L) {
            pendingStateUptimeMs = 0L
            val trace = pendingTrace
            val modelCommittedNanos = pendingModelCommittedNanos
            pendingTrace = null
            pendingModelCommittedNanos = 0L
            val sample =
                if (trace != null && modelCommittedNanos != 0L) {
                    RuntimeRenderLatencySample.from(
                        trace = trace,
                        modelCommittedNanos = modelCommittedNanos,
                        drawNanos = SystemClock.elapsedRealtimeNanos(),
                        committedOnMainThread = pendingStateCommittedOnMainThread,
                    )
                } else {
                    null
                }
            onStateRendered(
                (SystemClock.uptimeMillis() - committedAt).coerceAtLeast(0L),
                pendingStateCommittedOnMainThread,
                sample,
            )
        }
    }

    // Only state inputs affect colors; motion interpolators do not.
    private class ColorCache(
        val model: RenderModel,
        val tint: TintState,
        val visual: VisualCfg,
        val colors: RenderColors,
    )

    private companion object {
        const val CENTER_TRANSITION_DURATION_MS = 100L
    }
}
