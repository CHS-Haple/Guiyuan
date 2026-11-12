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
    private val retirement = RetirementGate<Session>()

    private fun stop(session: Session) {
        retirement.release(session) {
            session.stop()
            0
        }
    }

    @Synchronized
    fun attach(
        host: Any,
        onEvent: (String) -> Unit,
    ): String? {
        if (retirement.pending != null) return "stable-status-release-incomplete"
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

        existing?.let { stop(it) }

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
    fun detach() {
        val session = current ?: retirement.pending ?: return
        stop(session)
        if (current === session) current = null
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
            releaseAll(
                { host.get()?.removeOnAttachStateChangeListener(this) },
                { clearAnchorLayoutListener() },
            )
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

            SysUiPresentationRuntime.emitPassiveDiagnostic {
                onEvent(
                    "stableStatus anchor source=$source " +
                        "hostId=${System.identityHashCode(hostView)} " +
                        "containerIndex=${hostView.indexOfChild(container)} " +
                        "batteryIndex=${container.indexOfChild(view)} " +
                        "batterySize=${view.width}x${view.height} " +
                        "batteryMeasured=${view.measuredWidth}x${view.measuredHeight} ",
                )
            }

            SysUiPresentationRuntime.emitPassiveDiagnostic {
                reportSlotMetrics(container, view)
            }
            return true
        }

        private fun reportSlotMetrics(container: ViewGroup, view: ViewGroup) {
            val statusIcons = container.directChild(STATUS_ICON_CONTAINER_CLASS_NAME)
            val layoutParams = view.layoutParams
            val margins = layoutParams as? ViewGroup.MarginLayoutParams
            val adjacentGap = statusIcons?.let { icons -> view.left - icons.right } ?: -1
            onEvent(
                "stableStatus slotMetrics " +
                    "batteryPadding=${view.paddingStart},${view.paddingEnd}," +
                    "${view.paddingTop},${view.paddingBottom} " +
                    "batteryMinWidth=${view.minimumWidth} " +
                    "layout=${layoutParams?.width ?: Int.MIN_VALUE}x" +
                    "${layoutParams?.height ?: Int.MIN_VALUE} " +
                    "margins=${margins?.marginStart ?: 0},${margins?.marginEnd ?: 0} " +
                    "containerPadding=${container.paddingStart},${container.paddingEnd} " +
                    "statusIconsWidth=${statusIcons?.width ?: -1} " +
                    "statusIconsRight=${statusIcons?.right ?: -1} " +
                    "batteryBounds=${view.left}-${view.right} adjacentGap=$adjacentGap " +
                    "clipChildren=${view.clipChildren},${container.clipChildren} " +
                    "rtl=${view.layoutDirection == View.LAYOUT_DIRECTION_RTL} " +
                    "translationX=${view.translationX} ",
            )
        }

        private fun clearAnchorLayoutListener() {
            val listener = anchorLayoutListener ?: return
            batteryView.get()?.removeOnLayoutChangeListener(listener)
            anchorLayoutListener = null
        }

    }

}
