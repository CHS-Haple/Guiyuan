package com.chaners.guiyuan.xposed

import android.view.View
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.ref.WeakReference
import java.lang.reflect.Field

internal object SystemUiIslandMotionSource {
    const val HOOK_COUNT = 1


    private const val INJECTOR_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.shared.ui.binder.HomeStatusBarViewBinderInjector"
    private const val LISTENER_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.shared.ui.binder.HomeStatusBarViewBinderInjector\$islandListener\$1"
    private const val STATUS_METHOD_NAME = "onIslandStatusChanged"
    private const val HOOK_ID = "combinedstatus.island.home.status"
    private const val BATTERY_VIEW_FIELD = "mBatteryView"

    private val diagnosticTrackedNames =
        listOf(
            "mStatusContainer",
            "mEndSideContent",
            "mStatusBarIcons",
            "mBatteryContainer",
            BATTERY_VIEW_FIELD,
        )

    private var injectorRef = WeakReference<Any>(null)
    private var diagnosticFields: List<Pair<String, Field>> = emptyList()
    @Volatile
    private var islandShowing: Boolean? = null
    private var lastDiagnosticShowing: Boolean? = null

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onEvent: ((String) -> Unit)? = null,
        isProbeEnabled: () -> Boolean = { true },
    ): List<HookHandle> {
        val injectorClass = Class.forName(INJECTOR_CLASS_NAME, false, classLoader)
        val listenerClass = Class.forName(LISTENER_CLASS_NAME, false, classLoader)
        val method =
            listenerClass.getDeclaredMethod(
                STATUS_METHOD_NAME,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
            ).apply { isAccessible = true }
        val diagnosticsEnabled = onEvent != null
        val outerField =
            if (diagnosticsEnabled) {
                listenerClass.declaredFields
                    .firstOrNull { it.type == injectorClass }
                    ?.apply { isAccessible = true }
            } else {
                null
            }
        val resolvedDiagnosticFields =
            if (diagnosticsEnabled) {
                diagnosticTrackedNames.mapNotNull { name ->
                    runCatching {
                        injectorClass.getDeclaredField(name).apply { isAccessible = true }
                    }.getOrNull()?.let { name to it }
                }
            } else {
                emptyList()
            }
        synchronized(this) {
            diagnosticFields = resolvedDiagnosticFields
        }

        val handle =
            module
                .hook(method)
                .setId(HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        val showing = chain.getArg(0) as? Boolean ?: false
                        val secondary = chain.getArg(1) as? Boolean ?: false
                        val animate = chain.getArg(2) as? Boolean ?: false
                        val result = chain.proceed()
                        val injector =
                            outerField?.let { field ->
                                runCatching { field.get(chain.thisObject) }.getOrNull()
                            }
                        synchronized(this) {
                            islandShowing = showing
                            if (injector != null) {
                                injectorRef = WeakReference(injector)
                            }
                        }
                        if (onEvent == null || !isProbeEnabled() || injector == null) {
                            return@Hooker result
                        }
                        val shouldReport =
                            synchronized(this) {
                                val changed = lastDiagnosticShowing != showing
                                if (changed) {
                                    lastDiagnosticShowing = showing
                                }
                                changed
                            }
                        if (shouldReport) {
                            onEvent(
                                "islandOwner event showing=" + showing +
                                    " secondary=" + secondary +
                                    " animate=" + animate +
                                    " fields=" +
                                    resolvedDiagnosticFields.joinToString(",") { (name, _) -> name } +
                                    " geometryWrites=0",
                            )
                        }
                        result
                    },
                )
        return listOf(handle)
    }

    fun matches(handle: HookHandle): Boolean = handle.id == HOOK_ID

    @Synchronized
    fun currentIslandShowing(): Boolean? = islandShowing

    @Synchronized
    fun currentOwnerSnapshot(): OwnerSnapshot? {
        val injector = injectorRef.get() ?: return null
        val views =
            diagnosticFields.mapNotNull { (name, field) ->
                (runCatching { field.get(injector) as? View }.getOrNull())
                    ?.let { name to viewSnapshot(it) }
            }.toMap()
        if (views.isEmpty()) {
            return null
        }
        return OwnerSnapshot(views)
    }

    fun resetRuntimeState() {
        synchronized(this) {
            injectorRef = WeakReference(null)
            diagnosticFields = emptyList()
            islandShowing = null
            lastDiagnosticShowing = null
        }
    }

    internal data class OwnerSnapshot(
        val views: Map<String, MotionViewSnapshot>,
    ) {
        val summary: String
            get() =
                "{" +
                    diagnosticTrackedNames
                        .mapNotNull { name ->
                            views[name]?.let { snapshot ->
                                name + "=" + snapshot.summary
                            }
                        }
                        .joinToString(",") +
                    "}"
    }

    internal data class MotionViewSnapshot(
        val className: String,
        val screenX: Int,
        val width: Int,
        val translationX: Float,
        val alpha: Float,
        val visibility: Int,
    ) {
        val summary: String
            get() =
                className +
                    "(x=" + screenX +
                    ",w=" + width +
                    ",tx=" + translationX +
                    ",a=" + alpha +
                    ",v=" + visibility +
                    ")"
    }

    private fun viewSnapshot(view: View): MotionViewSnapshot {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return MotionViewSnapshot(
            className = view.javaClass.simpleName,
            screenX = location[0],
            width = view.width,
            translationX = view.translationX,
            alpha = view.alpha,
            visibility = view.visibility,
        )
    }


}
