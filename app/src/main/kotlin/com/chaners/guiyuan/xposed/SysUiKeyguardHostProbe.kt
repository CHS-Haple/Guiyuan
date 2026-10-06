package com.chaners.guiyuan.xposed

import android.view.View
import android.view.ViewGroup
import java.lang.ref.WeakReference
import java.lang.reflect.Field

/**
 * Diagnostic only. Reuses the existing scene callback and never installs its own hook.
 * Once a complete Keyguard host is seen, keep it until runtime reset.
 * CC `realSystemIcons` is context only; it does not gate steady Keyguard readiness.
 */
internal object SysUiKeyguardHostProbe {
    private const val KEYGUARD_HOST_CLASS =
        "com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView"

    private var confirmedHost = WeakReference<ViewGroup>(null)

    @Synchronized
    fun capture(update: SysUiSceneSource.SceneUpdate): Snapshot? {
        if (!shouldProbe(update.surface)) return null

        val host =
            findAncestor(update.sourceView, KEYGUARD_HOST_CLASS) as? ViewGroup
                ?: return null
        if (confirmedHost.get() === host) return null

        val systemIcons = readView(host, "mSystemIconsContainer")
        val statusIcons = readView(host, "mStatusIconContainer")
        val battery = readView(host, "mBatteryView")
        val batteryCarrier =
            battery?.let(SysUiCarrierMetrics::resolveCarrierView)

        val dependency = readValue(host, "mDep")
        val ccFake = dependency?.let { readValue(it, "ccFake") }
        val selectedRealSystemIcons =
            ccFake?.let { readView(it, "realSystemIcons") }
        val batteryMatchesSceneSource =
            battery?.let { candidate -> candidate === update.sourceView }
        val selectedAsRealSystemIcons =
            if (systemIcons == null || selectedRealSystemIcons == null) {
                null
            } else {
                selectedRealSystemIcons === systemIcons
            }
        val batteryCarrierWidthPx =
            batteryCarrier?.let(SysUiCarrierMetrics::resolveCarrierWidthPx)
        val complete =
            shouldFreezeSample(
                hostAttached = host.isAttachedToWindow,
                systemIconsAttached = systemIcons?.isAttachedToWindow == true,
                systemIconsWidth = systemIcons?.width ?: 0,
                batteryMatchesSceneSource = batteryMatchesSceneSource,
                batteryCarrierWidthPx = batteryCarrierWidthPx,
            )

        if (complete) {
            confirmedHost = WeakReference(host)
        }

        return Snapshot(
            rawState = update.rawState,
            host = ViewSnapshot.from(host),
            systemIcons = systemIcons?.let(ViewSnapshot::from),
            statusIcons = statusIcons?.let(ViewSnapshot::from),
            battery = battery?.let(ViewSnapshot::from),
            batteryCarrier = batteryCarrier?.let(ViewSnapshot::from),
            batteryCarrierWidthPx = batteryCarrierWidthPx,
            batteryMatchesSceneSource = batteryMatchesSceneSource,
            selectedRealSystemIcons =
                selectedRealSystemIcons?.let(ViewSnapshot::from),
            selectedAsRealSystemIcons = selectedAsRealSystemIcons,
            complete = complete,
        )
    }

    internal fun shouldProbe(surface: SysUiSceneSource.Surface): Boolean =
        surface == SysUiSceneSource.Surface.KEYGUARD

    internal fun isKeyguardHostClassName(className: String): Boolean =
        className == KEYGUARD_HOST_CLASS

    internal fun shouldFreezeSample(
        hostAttached: Boolean,
        systemIconsAttached: Boolean,
        systemIconsWidth: Int,
        batteryMatchesSceneSource: Boolean?,
        batteryCarrierWidthPx: Int?,
    ): Boolean =
        hostAttached &&
            systemIconsAttached &&
            systemIconsWidth > 0 &&
            batteryMatchesSceneSource == true &&
            (batteryCarrierWidthPx ?: 0) > 0

    @Synchronized
    fun resetRuntimeState() {
        confirmedHost = WeakReference(null)
    }

    internal data class Snapshot(
        val rawState: Int,
        val host: ViewSnapshot,
        val systemIcons: ViewSnapshot?,
        val statusIcons: ViewSnapshot?,
        val battery: ViewSnapshot?,
        val batteryCarrier: ViewSnapshot?,
        val batteryCarrierWidthPx: Int?,
        val batteryMatchesSceneSource: Boolean?,
        val selectedRealSystemIcons: ViewSnapshot?,
        val selectedAsRealSystemIcons: Boolean?,
        val complete: Boolean,
    )

    internal data class ViewSnapshot(
        val className: String,
        val identity: Int,
        val attached: Boolean,
        val visibility: Int,
        val alpha: Float,
        val width: Int,
        val height: Int,
        val measuredWidth: Int,
        val measuredHeight: Int,
        val paddingStart: Int,
        val paddingEnd: Int,
        val translationX: Float,
        val translationY: Float,
    ) {
        val summary: String
            get() =
                className.substringAfterLast('.') +
                    "{id=" + identity +
                    ",attached=" + attached +
                    ",v=" + visibility +
                    ",a=" + alpha +
                    ",w=" + width +
                    ",h=" + height +
                    ",mw=" + measuredWidth +
                    ",mh=" + measuredHeight +
                    ",ps=" + paddingStart +
                    ",pe=" + paddingEnd +
                    ",tx=" + translationX +
                    ",ty=" + translationY +
                    "}"

        companion object {
            fun from(view: View): ViewSnapshot =
                ViewSnapshot(
                    className = view.javaClass.name,
                    identity = System.identityHashCode(view),
                    attached = view.isAttachedToWindow,
                    visibility = view.visibility,
                    alpha = view.alpha,
                    width = view.width,
                    height = view.height,
                    measuredWidth = view.measuredWidth,
                    measuredHeight = view.measuredHeight,
                    paddingStart = view.paddingStart,
                    paddingEnd = view.paddingEnd,
                    translationX = view.translationX,
                    translationY = view.translationY,
                )
        }
    }

    private fun findAncestor(
        start: View,
        className: String,
    ): View? {
        var current: View? = start
        while (current != null) {
            if (current.javaClass.name == className) {
                return current
            }
            current = current.parent as? View
        }
        return null
    }

    private fun readView(
        owner: Any,
        fieldName: String,
    ): View? =
        readValue(owner, fieldName) as? View

    private fun readValue(
        owner: Any,
        fieldName: String,
    ): Any? =
        findField(owner.javaClass, fieldName)
            ?.let { field -> runCatching { field.get(owner) }.getOrNull() }

    private fun findField(
        type: Class<*>,
        name: String,
    ): Field? {
        var current: Class<*>? = type
        while (current != null) {
            val candidate = current
            val field =
                runCatching {
                    candidate
                        .getDeclaredField(name)
                        .apply { isAccessible = true }
                }.getOrNull()
            if (field != null) {
                return field
            }
            current = candidate.superclass
        }
        return null
    }
}
