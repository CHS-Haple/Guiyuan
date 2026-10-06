package com.chaners.guiyuan.xposed

import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import java.lang.reflect.Proxy

internal object BindableGeometryProbe {
    private const val MODERN_STATUS_BAR_VIEW =
        "com.android.systemui.statusbar.pipeline.shared.ui.view.ModernStatusBarView"
    private const val BINDING =
        "com.android.systemui.statusbar.pipeline.shared.ui.binder.ModernStatusBarViewBinding"
    private const val FUNCTION0 = "kotlin.jvm.functions.Function0"
    private const val SLOT = "combined_status_visual_geometry_probe"
    private const val BATTERY_CONTAINER =
        "com.android.systemui.statusbar.views.MiuiStatusBatteryContainer"
    private const val BATTERY_VIEW =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"

    fun inspect(host: Any): Snapshot {
        val hostView =
            host as? ViewGroup
                ?: return Snapshot.unavailable("host-not-view-group")
        val batteryContainer =
            hostView.directChild(BATTERY_CONTAINER) as? ViewGroup
                ?: return Snapshot.unavailable("battery-container-missing")
        val battery =
            batteryContainer.directChild(BATTERY_VIEW)
                ?: return Snapshot.unavailable("battery-view-missing")
        if (battery.width <= 0 || battery.height <= 0) {
            return Snapshot.unavailable("battery-geometry-not-ready")
        }

        val resolution = ParticipantAccess.resolve(host)
        val handles =
            when (resolution) {
                is ParticipantAccess.ResolveResult.Ready ->
                    resolution.handles

                is ParticipantAccess.ResolveResult.Failure ->
                    return Snapshot.unavailable(resolution.reason)
            }

        val classLoader = handles.classLoader
        val modernViewClass =
            classOrNull(MODERN_STATUS_BAR_VIEW, classLoader)
                ?: return Snapshot.unavailable("modern-view-class-missing")
        val bindingClass =
            classOrNull(BINDING, classLoader)
                ?: return Snapshot.unavailable("binding-class-missing")
        val function0Class =
            classOrNull(FUNCTION0, classLoader)
                ?: return Snapshot.unavailable("function0-class-missing")
        if (!bindingClass.isInterface || !function0Class.isInterface) {
            return Snapshot.unavailable("binding-proxy-contract-mismatch")
        }

        val reference =
            (0 until handles.group.childCount)
                .asSequence()
                .map { index -> handles.group.getChildAt(index) }
                .firstOrNull { child ->
                    modernViewClass.isInstance(child) &&
                        child.visibility == View.VISIBLE &&
                        child.width > 0 &&
                        child.height > 0
                }
                ?: return Snapshot.unavailable("visible-modern-reference-missing")

        val nativeHeight =
            reference.layoutParams
                ?.height
                ?.takeIf { height -> height > 0 }
                ?: reference.height

        val constructor =
            modernViewClass.declaredConstructors
                .firstOrNull { candidate ->
                    candidate.parameterTypes.map { type -> type.name } ==
                        listOf(
                            "android.content.Context",
                            "android.util.AttributeSet",
                        )
                }
                ?: return Snapshot.unavailable("modern-view-constructor-missing")
        constructor.isAccessible = true

        val initView =
            modernViewClass.declaredMethods
                .firstOrNull { method ->
                    method.name == "initView" &&
                        method.parameterTypes.map { type -> type.name } ==
                            listOf(
                                "java.lang.String",
                                FUNCTION0,
                            )
                }
                ?: return Snapshot.unavailable("modern-view-init-missing")
        initView.isAccessible = true

        return runCatching {
            val binding =
                Proxy.newProxyInstance(
                    classLoader,
                    arrayOf(bindingClass),
                ) { proxy, method, args ->
                    when (method.name) {
                        "getShouldIconBeVisible" -> false
                        "isCollecting" -> true
                        "toString" -> "CombinedStatusVisualGeometryBinding"
                        "hashCode" -> System.identityHashCode(proxy)
                        "equals" -> proxy === args?.firstOrNull()
                        else -> defaultValue(method.returnType)
                    }
                }
            val bindingFactory =
                Proxy.newProxyInstance(
                    classLoader,
                    arrayOf(function0Class),
                ) { proxy, method, args ->
                    when (method.name) {
                        "invoke" -> binding
                        "toString" -> "CombinedStatusVisualGeometryBindingFactory"
                        "hashCode" -> System.identityHashCode(proxy)
                        "equals" -> proxy === args?.firstOrNull()
                        else -> defaultValue(method.returnType)
                    }
                }

            val root =
                constructor.newInstance(
                    handles.group.context,
                    null as AttributeSet?,
                ) as? FrameLayout
                    ?: error("modern-view-not-frame-layout")
            root.clipChildren = false
            root.clipToPadding = false
            initView.invoke(
                root,
                SLOT,
                bindingFactory,
            )

            val renderView =
                RenderView(handles.group.context)
            root.addView(
                renderView,
                FrameLayout.LayoutParams(
                    battery.width,
                    battery.height,
                    Gravity.CENTER,
                ),
            )

            root.measure(
                View.MeasureSpec.makeMeasureSpec(
                    0,
                    View.MeasureSpec.UNSPECIFIED,
                ),
                View.MeasureSpec.makeMeasureSpec(
                    nativeHeight,
                    View.MeasureSpec.EXACTLY,
                ),
            )
            root.layout(
                0,
                0,
                root.measuredWidth,
                root.measuredHeight,
            )

            val projectedTop = reference.top + renderView.top
            val projectedBottom = reference.top + renderView.bottom
            val projectedFitsGroup =
                projectedTop >= 0 &&
                    projectedBottom <= handles.group.height

            Snapshot(
                available = true,
                reason = null,
                referenceClass = reference.javaClass.name,
                referenceBounds =
                    reference.left.toString() + "," +
                        reference.top + "-" +
                        reference.right + "," +
                        reference.bottom,
                referenceLayoutWidth =
                    reference.layoutParams?.width ?: Int.MIN_VALUE,
                referenceLayoutHeight =
                    reference.layoutParams?.height ?: Int.MIN_VALUE,
                groupHeight = handles.group.height,
                groupClipChildren = handles.group.clipChildren,
                groupClipToPadding = handles.group.clipToPadding,
                visualWidth = battery.width,
                visualHeight = battery.height,
                shellMeasuredWidth = root.measuredWidth,
                shellMeasuredHeight = root.measuredHeight,
                shellClipChildren = root.clipChildren,
                renderMeasuredWidth = renderView.measuredWidth,
                renderMeasuredHeight = renderView.measuredHeight,
                renderBounds =
                    renderView.left.toString() + "," +
                        renderView.top + "-" +
                        renderView.right + "," +
                        renderView.bottom,
                projectedTop = projectedTop,
                projectedBottom = projectedBottom,
                projectedFitsGroup = projectedFitsGroup,
                nativeGeometryWrites = 0,
            ).also {
                root.removeAllViews()
            }
        }.getOrElse { error ->
            Snapshot.unavailable(
                "visual-geometry-" +
                    (error.message ?: error.javaClass.simpleName),
            )
        }
    }

    private fun ViewGroup.directChild(className: String): View? {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.javaClass.name == className) {
                return child
            }
        }
        return null
    }

    private fun classOrNull(
        name: String,
        classLoader: ClassLoader,
    ): Class<*>? =
        runCatching {
            Class.forName(name, false, classLoader)
        }.getOrNull()

    private fun defaultValue(type: Class<*>): Any? =
        when (type) {
            java.lang.Boolean.TYPE -> false
            java.lang.Byte.TYPE -> 0.toByte()
            java.lang.Short.TYPE -> 0.toShort()
            java.lang.Integer.TYPE -> 0
            java.lang.Long.TYPE -> 0L
            java.lang.Float.TYPE -> 0f
            java.lang.Double.TYPE -> 0.0
            java.lang.Character.TYPE -> 0.toChar()
            java.lang.Void.TYPE -> null
            else -> null
        }

    internal data class Snapshot(
        val available: Boolean,
        val reason: String?,
        val referenceClass: String?,
        val referenceBounds: String?,
        val referenceLayoutWidth: Int,
        val referenceLayoutHeight: Int,
        val groupHeight: Int,
        val groupClipChildren: Boolean,
        val groupClipToPadding: Boolean,
        val visualWidth: Int,
        val visualHeight: Int,
        val shellMeasuredWidth: Int,
        val shellMeasuredHeight: Int,
        val shellClipChildren: Boolean,
        val renderMeasuredWidth: Int,
        val renderMeasuredHeight: Int,
        val renderBounds: String?,
        val projectedTop: Int,
        val projectedBottom: Int,
        val projectedFitsGroup: Boolean,
        val nativeGeometryWrites: Int,
    ) {
        val ready: Boolean
            get() =
                available &&
                    referenceLayoutHeight > 0 &&
                    shellMeasuredWidth == visualWidth &&
                    shellMeasuredHeight == referenceLayoutHeight &&
                    renderMeasuredWidth == visualWidth &&
                    renderMeasuredHeight == visualHeight &&
                    projectedFitsGroup &&
                    nativeGeometryWrites == 0

        val logLine: String
            get() =
                "nativeBindableVisualGeometry available=" + available +
                    " reason=" + (reason ?: "none") +
                    " reference=" + (referenceClass ?: "none") +
                    " referenceBounds=" + (referenceBounds ?: "none") +
                    " referenceLayout=" +
                    referenceLayoutWidth + "x" + referenceLayoutHeight +
                    " groupHeight=" + groupHeight +
                    " groupClipChildren=" + groupClipChildren +
                    " groupClipToPadding=" + groupClipToPadding +
                    " visual=" + visualWidth + "x" + visualHeight +
                    " shellMeasured=" +
                    shellMeasuredWidth + "x" + shellMeasuredHeight +
                    " shellClipChildren=" + shellClipChildren +
                    " renderMeasured=" +
                    renderMeasuredWidth + "x" + renderMeasuredHeight +
                    " renderBounds=" + (renderBounds ?: "none") +
                    " projected=" + projectedTop + "-" + projectedBottom +
                    " projectedFitsGroup=" + projectedFitsGroup +
                    " ready=" + ready +
                    " nativeGeometryWrites=" + nativeGeometryWrites

        companion object {
            fun unavailable(reason: String): Snapshot =
                Snapshot(
                    available = false,
                    reason = reason,
                    referenceClass = null,
                    referenceBounds = null,
                    referenceLayoutWidth = Int.MIN_VALUE,
                    referenceLayoutHeight = Int.MIN_VALUE,
                    groupHeight = -1,
                    groupClipChildren = true,
                    groupClipToPadding = true,
                    visualWidth = -1,
                    visualHeight = -1,
                    shellMeasuredWidth = -1,
                    shellMeasuredHeight = -1,
                    shellClipChildren = true,
                    renderMeasuredWidth = -1,
                    renderMeasuredHeight = -1,
                    renderBounds = null,
                    projectedTop = Int.MIN_VALUE,
                    projectedBottom = Int.MIN_VALUE,
                    projectedFitsGroup = false,
                    nativeGeometryWrites = 0,
                )
        }
    }
}
