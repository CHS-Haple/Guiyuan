package com.chaners.guiyuan.xposed

import android.view.View
import android.view.ViewGroup
import java.lang.ref.WeakReference

internal object StatusBarStableSession {
    private const val BATTERY_CONTAINER_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiStatusBatteryContainer"
    private const val BATTERY_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
    private const val STATUS_ICON_CONTAINER_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiStatusIconContainer"

    private var current: Session? = null

    @Synchronized
    fun attach(
        host: Any,
        onEvent: (String) -> Unit,
    ): String? {
        val hostView = host as? ViewGroup
            ?: return "host-not-view-group"

        val batteryContainer = hostView.directChild(BATTERY_CONTAINER_CLASS_NAME)
            ?: return "battery-container-missing"
        val batteryView = batteryContainer.directChild(BATTERY_VIEW_CLASS_NAME)
            ?: return "battery-view-missing"

        val existing = current
        if (existing?.matches(hostView, batteryContainer, batteryView) == true) {
            return null
        }

        existing?.stop()

        val session = Session(
            host = hostView,
            batteryContainer = batteryContainer,
            batteryView = batteryView,
            onEvent = onEvent,
        )
        current = session
        session.start()

        return null
    }

    @Synchronized
    fun currentSlotMetrics(host: Any): SlotMetrics? =
        current?.slotMetricsFor(host)

    @Synchronized
    fun detach() {
        current?.stop()
        current = null
    }

    private fun ViewGroup.directChild(className: String): ViewGroup? {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.javaClass.name == className) {
                return child as? ViewGroup
            }
        }
        return null
    }

    private class Session(
        host: ViewGroup,
        batteryContainer: ViewGroup,
        batteryView: ViewGroup,
        private val onEvent: (String) -> Unit,
    ) : View.OnAttachStateChangeListener {
        private val host = WeakReference(host)
        private val batteryContainer = WeakReference(batteryContainer)
        private val batteryView = WeakReference(batteryView)
        private var anchorCaptured = false
        private var anchorLayoutListener: View.OnLayoutChangeListener? = null
        private var capturedSlotMetrics: SlotMetrics? = null

        fun slotMetricsFor(host: Any): SlotMetrics? =
            capturedSlotMetrics.takeIf {
                this.host.get() === host
            }

        fun matches(
            host: ViewGroup,
            batteryContainer: ViewGroup,
            batteryView: ViewGroup,
        ): Boolean =
            this.host.get() === host &&
                this.batteryContainer.get() === batteryContainer &&
                this.batteryView.get() === batteryView

        fun start() {
            val view = host.get() ?: return
            view.addOnAttachStateChangeListener(this)
            scheduleAnchorCapture()
        }

        fun stop() {
            host.get()?.removeOnAttachStateChangeListener(this)
            clearAnchorLayoutListener()
        }

        override fun onViewAttachedToWindow(view: View) {
            scheduleAnchorCapture()
        }

        override fun onViewDetachedFromWindow(view: View) {
            clearAnchorLayoutListener()
        }

        private fun scheduleAnchorCapture() {
            if (anchorCaptured || captureAnchorIfReady("ready")) {
                return
            }

            val view = batteryView.get() ?: return
            if (anchorLayoutListener != null) {
                return
            }

            val listener =
                object : View.OnLayoutChangeListener {
                    override fun onLayoutChange(
                        view: View,
                        left: Int,
                        top: Int,
                        right: Int,
                        bottom: Int,
                        oldLeft: Int,
                        oldTop: Int,
                        oldRight: Int,
                        oldBottom: Int,
                    ) {
                        captureAnchorIfReady("layout")
                    }
                }

            anchorLayoutListener = listener
            view.addOnLayoutChangeListener(listener)
            captureAnchorIfReady("ready")
        }

        private fun captureAnchorIfReady(source: String): Boolean {
            if (anchorCaptured) {
                return true
            }

            val hostView = host.get() ?: return false
            val container = batteryContainer.get() ?: return false
            val view = batteryView.get() ?: return false

            if (
                !view.isLaidOut ||
                view.width <= 0 ||
                view.height <= 0 ||
                view.measuredWidth <= 0 ||
                view.measuredHeight <= 0
            ) {
                return false
            }

            anchorCaptured = true
            clearAnchorLayoutListener()

            onEvent(
                Anchor(
                    source = source,
                    hostIdentity = System.identityHashCode(hostView),
                    batteryContainerIndex = hostView.indexOfChild(container),
                    batteryIndex = container.indexOfChild(view),
                    batteryWidth = view.width,
                    batteryHeight = view.height,
                    batteryMeasuredWidth = view.measuredWidth,
                    batteryMeasuredHeight = view.measuredHeight,
                ).logLine,
            )

            val statusIcons = container.directChild(STATUS_ICON_CONTAINER_CLASS_NAME)
            val layoutParams = view.layoutParams
            val margins = layoutParams as? ViewGroup.MarginLayoutParams
            val slotMetrics =
                SlotMetrics(
                    batteryPaddingStart = view.paddingStart,
                    batteryPaddingEnd = view.paddingEnd,
                    batteryPaddingTop = view.paddingTop,
                    batteryPaddingBottom = view.paddingBottom,
                    batteryMinimumWidth = view.minimumWidth,
                    batteryLayoutWidth = layoutParams?.width ?: Int.MIN_VALUE,
                    batteryLayoutHeight = layoutParams?.height ?: Int.MIN_VALUE,
                    batteryMarginStart = margins?.marginStart ?: 0,
                    batteryMarginEnd = margins?.marginEnd ?: 0,
                    containerPaddingStart = container.paddingStart,
                    containerPaddingEnd = container.paddingEnd,
                    statusIconsWidth = statusIcons?.width ?: -1,
                    statusIconsRight = statusIcons?.right ?: -1,
                    batteryLeft = view.left,
                    batteryRight = view.right,
                    adjacentGap =
                        statusIcons?.let { icons -> view.left - icons.right } ?: -1,
                    batteryClipChildren = view.clipChildren,
                    containerClipChildren = container.clipChildren,
                    layoutRtl = view.layoutDirection == View.LAYOUT_DIRECTION_RTL,
                    batteryTranslationX = view.translationX,
                )
            capturedSlotMetrics = slotMetrics
            onEvent(slotMetrics.logLine)
            return true
        }

        private fun clearAnchorLayoutListener() {
            val listener = anchorLayoutListener ?: return
            batteryView.get()?.removeOnLayoutChangeListener(listener)
            anchorLayoutListener = null
        }

    }

    internal data class Anchor(
        val source: String,
        val hostIdentity: Int,
        val batteryContainerIndex: Int,
        val batteryIndex: Int,
        val batteryWidth: Int,
        val batteryHeight: Int,
        val batteryMeasuredWidth: Int,
        val batteryMeasuredHeight: Int,
    ) {
        val logLine: String
            get() =
                "stableStatus anchor source=$source hostId=$hostIdentity " +
                    "containerIndex=$batteryContainerIndex batteryIndex=$batteryIndex " +
                    "batterySize=${batteryWidth}x$batteryHeight " +
                    "batteryMeasured=${batteryMeasuredWidth}x$batteryMeasuredHeight "
    }

    internal data class SlotMetrics(
        val batteryPaddingStart: Int,
        val batteryPaddingEnd: Int,
        val batteryPaddingTop: Int,
        val batteryPaddingBottom: Int,
        val batteryMinimumWidth: Int,
        val batteryLayoutWidth: Int,
        val batteryLayoutHeight: Int,
        val batteryMarginStart: Int,
        val batteryMarginEnd: Int,
        val containerPaddingStart: Int,
        val containerPaddingEnd: Int,
        val statusIconsWidth: Int,
        val statusIconsRight: Int,
        val batteryLeft: Int,
        val batteryRight: Int,
        val adjacentGap: Int,
        val batteryClipChildren: Boolean,
        val containerClipChildren: Boolean,
        val layoutRtl: Boolean,
        val batteryTranslationX: Float,
    ) {
        val logLine: String
            get() =
                "stableStatus slotMetrics " +
                    "batteryPadding=$batteryPaddingStart,$batteryPaddingEnd," +
                    "$batteryPaddingTop,$batteryPaddingBottom " +
                    "batteryMinWidth=$batteryMinimumWidth " +
                    "layout=${batteryLayoutWidth}x$batteryLayoutHeight " +
                    "margins=$batteryMarginStart,$batteryMarginEnd " +
                    "containerPadding=$containerPaddingStart,$containerPaddingEnd " +
                    "statusIconsWidth=$statusIconsWidth statusIconsRight=$statusIconsRight " +
                    "batteryBounds=$batteryLeft-$batteryRight adjacentGap=$adjacentGap " +
                    "clipChildren=$batteryClipChildren,$containerClipChildren " +
                    "rtl=$layoutRtl translationX=$batteryTranslationX "
    }

}
