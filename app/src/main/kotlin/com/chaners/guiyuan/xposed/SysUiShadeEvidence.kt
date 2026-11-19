package com.chaners.guiyuan.xposed

import android.content.Context
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.util.ArrayDeque
import java.lang.ref.WeakReference

/**
 * Diagnostic-only observation of the native Notification Shade header callback.
 * No native View property, animation, or expansion progress is written.
 */
internal object SysUiShadeEvidence {
    private const val CALLBACK_CLASS =
        "com.android.systemui.controlcenter.shade.NotificationHeaderExpandController\$notificationCallback\$1"
    private const val HEADER_CLASS =
        "com.android.systemui.controlcenter.shade.NotificationHeaderExpandController"
    private const val COMBINED_CLASS =
        "com.android.systemui.controlcenter.shade.CombinedHeaderController"
    private val samples = ShadeEvidenceGate()

    fun resetRuntimeState() {
        samples.reset()
    }

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        isDetailedDiagnosticsEnabled: () -> Boolean,
        onEvent: (String) -> Unit,
    ): HookHandle {
        val callback = Class.forName(CALLBACK_CLASS, false, classLoader)
        val callbackMethod =
            callback.getDeclaredMethod("onExpansionChanged", Float::class.javaPrimitiveType)
                .apply { isAccessible = true }
        val outer = callback.getDeclaredField("this\$0").apply { isAccessible = true }
        val headerClass = Class.forName(HEADER_CLASS, false, classLoader)
        val headerLazy = headerClass.getDeclaredField("headerController").apply { isAccessible = true }
        val context = headerClass.getDeclaredField("context").apply { isAccessible = true }
        val lazyGet =
            Class.forName("dagger.Lazy", false, classLoader).getDeclaredMethod("get")
                .apply { isAccessible = true }
        val combined = Class.forName(COMBINED_CLASS, false, classLoader)
        val notificationHeader =
            combined.getDeclaredField("notificationHeaderView").apply { isAccessible = true }
        val headerRoot = combined.getDeclaredField("headerView").apply { isAccessible = true }
        val carrier =
            combined.getDeclaredField("notificationCarrierViewContainer")
                .apply { isAccessible = true }
        val verticalMode =
            Class.forName("com.miui.utils.configs.MiuiConfigs", false, classLoader)
                .getDeclaredMethod("isVerticalMode", Context::class.java)
                .apply { isAccessible = true }

        return module.hook(callbackMethod)
            .setId("combinedstatus.notification-shade.evidence")
            .intercept(
                Hooker { chain ->
                    val result = chain.proceed()
                    if (isDetailedDiagnosticsEnabled()) {
                        val fraction = (chain.getArg(0) as? Number)?.toFloat()
                        val phase =
                            fraction?.let { samples.observe(chain.thisObject, it) }
                        if (phase != null) {
                            SysUiCcSource.dispatchRuntimeCallback(
                                callback = {
                                    val controller = outer.get(chain.thisObject)
                                    val selectedHeader = headerLazy.get(controller)?.let { lazyGet.invoke(it) }
                                    val appContext = context.get(controller) as? Context
                                    val nativeVertical =
                                        appContext?.let { verticalMode.invoke(null, it) as? Boolean }
                                    val notification =
                                        selectedHeader?.let { notificationHeader.get(it) as? View }
                                    val root = selectedHeader?.let { headerRoot.get(it) as? View }
                                    val carrierView = selectedHeader?.let { carrier.get(it) as? View }
                                    val home = SysUiHostRegistry.current() as? View
                                    if (Looper.myLooper() !== Looper.getMainLooper()) {
                                        onEvent(
                                            "shadeEvidence phase=" + phase +
                                                " state=unavailable reason=non-main-native-callback",
                                        )
                                        return@dispatchRuntimeCallback
                                    }
                                    onEvent(
                                        "shadeEvidence phase=" + phase +
                                            " fraction=" + fraction +
                                            " verticalMode=" + (nativeVertical ?: "unavailable") +
                                            " orientation=" +
                                            (appContext?.resources?.configuration?.orientation ?: "unavailable") +
                                            " home={" + viewState(home) + "}" +
                                            " homeOverlayShown=" + HomeRenderSession.isOverlayShown() +
                                            " notification={" + viewState(notification) + "}" +
                                            " notificationRoot={" + viewState(root) + "}" +
                                            " carrier={" + viewState(carrierView) + "}" +
                                            " notificationIconNodes=[" + findIconNodes(notification) + "]" +
                                            " headerIconNodes=[" + findIconNodes(root) + "]" +
                                            " ccFake=" + (CcRenderSession.visualState() ?: "none"),
                                    )
                                },
                                onFailure = { error ->
                                    SysUiCcSource.dispatchRuntimeCallback(
                                        callback = {
                                            onEvent(
                                                "shadeEvidence state=unavailable" +
                                                    " reason=" + error.javaClass.simpleName,
                                            )
                                        },
                                    )
                                },
                            )
                        }
                    }
                    result
                },
            )
    }

    private fun viewState(view: View?): String {
        if (view == null) return "unavailable"
        val location = IntArray(2)
        view.getLocationInWindow(location)
        return view.javaClass.simpleName +
            "@" + System.identityHashCode(view).toString(16) +
            ":attached=" + view.isAttachedToWindow +
            ":shown=" + view.isShown +
            ":visible=" + view.visibility +
            ":alpha=" + view.alpha +
            ":bounds=" + location[0] + "," + location[1] + "," + view.width + "," + view.height +
            ":parent=" + (view.parent?.javaClass?.simpleName ?: "none")
    }

    // A bounded snapshot is taken only at progress-stage changes, never every frame.
    private fun findIconNodes(root: View?): String {
        root ?: return "unavailable"
        val queue = ArrayDeque<Pair<View, Int>>()
        val nodes = ArrayList<String>(4)
        queue.add(root to 0)
        var visited = 0
        while (queue.isNotEmpty() && visited++ < 64 && nodes.size < 5) {
            val (view, depth) = queue.removeFirst()
            val name = view.javaClass.simpleName
            if (name.contains("StatusIcon") || name.contains("BatteryMeter")) {
                nodes.add(viewState(view))
            }
            if (depth < 5 && view is ViewGroup) {
                for (index in 0 until view.childCount) {
                    queue.add(view.getChildAt(index) to depth + 1)
                }
            }
        }
        return nodes.joinToString("|").ifEmpty { "none-in-bounded-header" }
    }
}

internal class ShadeEvidenceGate(private val maxReports: Int = 24) {
    private var ownerRef: WeakReference<Any>? = null
    private var previousStage = -1
    private var previousFraction = Float.NaN
    private var reports = 0

    @Synchronized
    fun reset() {
        ownerRef = null
        previousStage = -1
        previousFraction = Float.NaN
        reports = 0
    }

    @Synchronized
    fun observe(candidate: Any?, fraction: Float): String? {
        if (!fraction.isFinite() || fraction < 0f || fraction > 1f) return null
        if (ownerRef?.get() !== candidate) {
            ownerRef = candidate?.let(::WeakReference)
            previousStage = -1
            previousFraction = Float.NaN
        }
        val stage =
            when {
                fraction == 0f -> 0
                fraction < 0.5f -> 1
                fraction < 1f -> 2
                else -> 3
            }
        val direction =
            when {
                previousFraction.isNaN() -> "initial"
                fraction > previousFraction -> "opening"
                fraction < previousFraction -> "closing"
                else -> "steady"
            }
        previousFraction = fraction
        if (stage == previousStage || reports >= maxReports) return null
        previousStage = stage
        reports += 1
        return direction + ":" + when (stage) {
            0 -> "collapsed"
            1 -> "partial-low"
            2 -> "partial-high"
            else -> "expanded"
        }
    }
}
