package com.chaners.guiyuan.xposed

import android.view.View
import android.view.ViewGroup
import java.lang.ref.WeakReference
import java.lang.reflect.Field

internal object SystemUiKeyguardHostResolver {
    private const val KEYGUARD_HOST_CLASS =
        "com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView"
    private const val STATUS_ICON_CONTAINER_CLASS =
        "com.android.systemui.statusbar.views.MiuiStatusIconContainer"
    private const val BATTERY_CONTAINER_CLASS =
        "com.android.systemui.statusbar.views.MiuiStatusBatteryContainer"
    private const val BATTERY_VIEW_CLASS =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
    private const val TO_LOCK_SCREEN_FIELD = "mToLockScreen"

    private var lastSourceView: WeakReference<View>? = null
    private var lastSurface = SystemUiSceneStateSource.Surface.UNKNOWN
    private var lastRawState = Int.MIN_VALUE

    @Synchronized
    fun observe(
        update: SystemUiSceneStateSource.SceneUpdate,
    ): ResolveResult? {
        if (
            SystemUiSceneStateSource.steadySourceScene(update.sourceView) !=
                CombinedStatusSourceScene.KEYGUARD
        ) {
            return null
        }
        lastSourceView = WeakReference(update.sourceView)
        lastSurface = update.surface
        lastRawState = update.rawState
        return resolve(
            sourceView = update.sourceView,
            surface = update.surface,
            rawState = update.rawState,
        )
    }

    @Synchronized
    fun current(): ResolveResult? {
        val sourceView = lastSourceView?.get() ?: return null
        return resolve(
            sourceView = sourceView,
            surface = lastSurface,
            rawState = lastRawState,
        )
    }

    internal fun isSteadyKeyguardSurface(
        surface: SystemUiSceneStateSource.Surface,
    ): Boolean =
        surface == SystemUiSceneStateSource.Surface.KEYGUARD ||
            surface == SystemUiSceneStateSource.Surface.SHADE_LOCKED

    internal fun isKeyguardHostClassName(className: String): Boolean =
        className == KEYGUARD_HOST_CLASS

    internal fun nativeToLockScreenTarget(
        resolved: ResolvedHost,
    ): Boolean? =
        findField(resolved.host.javaClass, TO_LOCK_SCREEN_FIELD)
            ?.let { field ->
                runCatching { field.get(resolved.host) as? Boolean }.getOrNull()
            }

    internal fun statusIconsPresentationAlpha(
        resolved: ResolvedHost,
    ): Float? {
        val statusIcons = resolved.statusIcons
        if (!statusIcons.isAttachedToWindow) return null
        return resolveStatusIconsPresentationAlpha(
            visible = statusIcons.visibility == View.VISIBLE,
            alpha = statusIcons.alpha,
        )
    }

    internal fun resolveStatusIconsPresentationAlpha(
        visible: Boolean,
        alpha: Float,
    ): Float =
        if (visible) {
            alpha.coerceIn(0f, 1f)
        } else {
            0f
        }

    @Synchronized
    fun resetRuntimeState() {
        lastSourceView = null
        lastSurface = SystemUiSceneStateSource.Surface.UNKNOWN
        lastRawState = Int.MIN_VALUE
    }

    private fun resolve(
        sourceView: View,
        surface: SystemUiSceneStateSource.Surface,
        rawState: Int,
    ): ResolveResult {
        if (!isSteadyKeyguardSurface(surface)) {
            return ResolveResult.Inactive(surface)
        }

        val host =
            findAncestor(sourceView, KEYGUARD_HOST_CLASS) as? ViewGroup
                ?: return ResolveResult.Failure("keyguard-host-missing")
        if (!host.isAttachedToWindow) {
            return ResolveResult.Failure("keyguard-host-detached")
        }

        val systemIcons =
            readView(host, "mSystemIconsContainer") as? ViewGroup
                ?: return ResolveResult.Failure("keyguard-system-icons-missing")
        if (systemIcons.javaClass.name != BATTERY_CONTAINER_CLASS) {
            return ResolveResult.Failure("keyguard-system-icons-type-mismatch")
        }

        val statusIcons =
            readView(host, "mStatusIconContainer") as? ViewGroup
                ?: return ResolveResult.Failure("keyguard-status-icons-missing")
        if (statusIcons.javaClass.name != STATUS_ICON_CONTAINER_CLASS) {
            return ResolveResult.Failure("keyguard-status-icons-type-mismatch")
        }

        val battery =
            readView(host, "mBatteryView")
                ?: return ResolveResult.Failure("keyguard-battery-missing")
        if (battery.javaClass.name != BATTERY_VIEW_CLASS) {
            return ResolveResult.Failure("keyguard-battery-type-mismatch")
        }
        if (battery !== sourceView) {
            return ResolveResult.Failure("keyguard-battery-source-mismatch")
        }

        val batteryCarrier =
            SystemUiHomeCarrierMetrics.resolveCarrierView(battery)
                ?: return ResolveResult.Failure("keyguard-battery-core-carrier-missing")

        return ResolveResult.Ready(
            ResolvedHost(
                sourceView = sourceView,
                host = host,
                systemIcons = systemIcons,
                statusIcons = statusIcons,
                battery = battery,
                batteryCarrier = batteryCarrier,
                surface = surface,
                rawState = rawState,
            ),
        )
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
        findField(owner.javaClass, fieldName)
            ?.let { field -> runCatching { field.get(owner) as? View }.getOrNull() }

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

    internal data class ResolvedHost(
        val sourceView: View,
        val host: ViewGroup,
        val systemIcons: ViewGroup,
        val statusIcons: ViewGroup,
        val battery: View,
        val batteryCarrier: View,
        val surface: SystemUiSceneStateSource.Surface,
        val rawState: Int,
    )

    internal sealed interface ResolveResult {
        data class Ready(val host: ResolvedHost) : ResolveResult

        data class Inactive(
            val surface: SystemUiSceneStateSource.Surface,
        ) : ResolveResult

        data class Failure(val reason: String) : ResolveResult
    }
}
