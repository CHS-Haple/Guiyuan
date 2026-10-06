package com.chaners.guiyuan.xposed

import android.view.View
import android.view.ViewGroup

internal object NativeStatusInventory {
    const val MOBILE_NETWORK_VIEW_CLASS =
        "com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView"
    const val WIFI_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.wifi.ui.view.ModernStatusBarWifiView"
    const val BATTERY_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"

    const val MIUI_STATUS_CONTAINER_CLASS =
        "com.android.systemui.statusbar.views.MiuiStatusIconContainer"
    const val STATUS_ICON_CONTAINER_CLASS =
        "com.android.systemui.statusbar.phone.StatusIconContainer"
    const val BATTERY_CONTAINER_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiStatusBatteryContainer"

    private const val MAX_SCANNED_VIEWS = 1024
    private const val STATUS_ICON_SUBTREE_MAX_DEPTH = 3
    private const val STATUS_ICON_SUBTREE_MAX_NODES = 64

    fun schedule(
        host: Any,
        onSnapshot: (Snapshot) -> Unit,
    ) {
        val hostView = host as? View ?: return

        if (hostView.isLaidOut) {
            onSnapshot(inspect(hostView))
            return
        }

        hostView.addOnLayoutChangeListener(
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
                    view.removeOnLayoutChangeListener(this)
                    onSnapshot(inspect(hostView))
                }
            },
        )
    }

    private fun inspect(host: View): Snapshot {
        val scanRoot = host.rootView
        val entries = mutableListOf<Entry>()
        val scanState = ScanState()

        collect(
            view = scanRoot,
            depth = 0,
            path = "root",
            destination = entries,
            scanState = scanState,
        )

        val directChildren = (host as? ViewGroup)
            ?.let { group ->
                buildList {
                    for (index in 0 until group.childCount) {
                        add(group.getChildAt(index).javaClass.simpleName)
                    }
                }
            }
            .orEmpty()

        return Snapshot(
            hostClassName = host.javaClass.name,
            hostWidth = host.width,
            hostHeight = host.height,
            hostMeasuredWidth = host.measuredWidth,
            hostMeasuredHeight = host.measuredHeight,
            directChildren = directChildren,
            scanRootClassName = scanRoot.javaClass.name,
            hostPath = pathFromRoot(host, scanRoot),
            ancestorChain = ancestorChain(host),
            scannedViews = scanState.scannedViews,
            truncated = scanState.truncated,
            entries = entries,
            statusIconSubtree = scanState.miuiStatusIconContainer?.let(::inspectStatusIconSubtree),
        )
    }

    private fun collect(
        view: View,
        depth: Int,
        path: String,
        destination: MutableList<Entry>,
        scanState: ScanState,
    ) {
        if (scanState.scannedViews >= MAX_SCANNED_VIEWS) {
            scanState.truncated = true
            return
        }
        scanState.scannedViews += 1

        val viewResourceId = resourceId(view)
        val className = view.javaClass.name
        if (
            className == MIUI_STATUS_CONTAINER_CLASS &&
            scanState.miuiStatusIconContainer == null
        ) {
            scanState.miuiStatusIconContainer = view as? ViewGroup
        }

        roleFor(view, viewResourceId)?.let { role ->
            val parent = view.parent as? ViewGroup
            destination += Entry(
                role = role,
                className = className,
                resourceId = viewResourceId,
                parentClassName = parent?.javaClass?.name ?: "none",
                parentIndex = parent?.indexOfChild(view) ?: -1,
                depth = depth,
                path = path,
                visibility = visibilityName(view.visibility),
                width = view.width,
                height = view.height,
                measuredWidth = view.measuredWidth,
                measuredHeight = view.measuredHeight,
                left = view.left,
                top = view.top,
                right = view.right,
                bottom = view.bottom,
                translationX = view.translationX,
                translationY = view.translationY,
            )
        }

        val group = view as? ViewGroup ?: return
        for (index in 0 until group.childCount) {
            if (scanState.truncated) {
                return
            }
            collect(
                view = group.getChildAt(index),
                depth = depth + 1,
                path = "$path/$index",
                destination = destination,
                scanState = scanState,
            )
        }
    }

    private fun inspectStatusIconSubtree(container: ViewGroup): SubtreeSnapshot {
        val entries = mutableListOf<SubtreeEntry>()
        val state = SubtreeScanState()

        collectStatusIconChildren(
            parent = container,
            parentPath = "icons",
            parentDepth = 0,
            destination = entries,
            state = state,
        )

        return SubtreeSnapshot(
            rootClassName = container.javaClass.name,
            rootResourceId = resourceId(container),
            rootChildCount = container.childCount,
            maxDepth = STATUS_ICON_SUBTREE_MAX_DEPTH,
            maxNodes = STATUS_ICON_SUBTREE_MAX_NODES,
            truncated = state.truncated,
            entries = entries,
        )
    }

    private fun collectStatusIconChildren(
        parent: ViewGroup,
        parentPath: String,
        parentDepth: Int,
        destination: MutableList<SubtreeEntry>,
        state: SubtreeScanState,
    ) {
        if (parentDepth >= STATUS_ICON_SUBTREE_MAX_DEPTH) {
            if (parent.childCount > 0) {
                state.truncated = true
            }
            return
        }

        for (index in 0 until parent.childCount) {
            if (state.nodes >= STATUS_ICON_SUBTREE_MAX_NODES) {
                state.truncated = true
                return
            }

            val child = parent.getChildAt(index)
            val depth = parentDepth + 1
            val path = "$parentPath/$index"
            val childGroup = child as? ViewGroup

            destination += SubtreeEntry(
                className = child.javaClass.name,
                resourceId = resourceId(child),
                parentClassName = parent.javaClass.name,
                parentIndex = index,
                depth = depth,
                path = path,
                childCount = childGroup?.childCount ?: 0,
                visibility = visibilityName(child.visibility),
                width = child.width,
                height = child.height,
                measuredWidth = child.measuredWidth,
                measuredHeight = child.measuredHeight,
                left = child.left,
                top = child.top,
                right = child.right,
                bottom = child.bottom,
                translationX = child.translationX,
                translationY = child.translationY,
            )
            state.nodes += 1

            if (childGroup != null) {
                collectStatusIconChildren(
                    parent = childGroup,
                    parentPath = path,
                    parentDepth = depth,
                    destination = destination,
                    state = state,
                )
                if (state.nodes >= STATUS_ICON_SUBTREE_MAX_NODES) {
                    return
                }
            }
        }
    }

    private fun roleFor(
        view: View,
        resourceId: String,
    ): String? {
        val className = view.javaClass.name
        return when (className) {
            MOBILE_NETWORK_VIEW_CLASS -> "mobileNetwork"
            WIFI_VIEW_CLASS_NAME -> "wifi"
            BATTERY_VIEW_CLASS_NAME -> "battery"
            MIUI_STATUS_CONTAINER_CLASS -> "miuiStatusIcons"
            STATUS_ICON_CONTAINER_CLASS -> "statusIcons"
            BATTERY_CONTAINER_CLASS_NAME -> "batteryContainer"
            else -> candidateRole(className, resourceId)
        }
    }

    private fun candidateRole(
        className: String,
        resourceId: String,
    ): String? {
        val identity = (className + ' ' + resourceId).lowercase()
        return when {
            "wifi" in identity -> "candidateWifi"
            "mobile" in identity || "signal" in identity -> "candidateMobile"
            else -> null
        }
    }

    private fun pathFromRoot(
        view: View,
        root: View,
    ): String {
        if (view === root) {
            return "root"
        }

        val indices = mutableListOf<Int>()
        var current: View = view

        while (current !== root) {
            val parent = current.parent as? ViewGroup ?: return "unresolved"
            indices += parent.indexOfChild(current)
            current = parent
        }

        return buildString {
            append("root")
            indices.asReversed().forEach { index ->
                append('/')
                append(index)
            }
        }
    }

    private fun ancestorChain(view: View): List<String> =
        buildList {
            var current: View? = view
            while (current != null) {
                add(
                    buildString {
                        append(current.javaClass.simpleName)
                        append('[')
                        append(resourceId(current))
                        append(']')
                    },
                )
                current = current.parent as? View
            }
        }

    private fun resourceId(view: View): String {
        if (view.id == View.NO_ID) {
            return "none"
        }

        return runCatching {
            view.resources.getResourceName(view.id)
        }.getOrElse {
            view.id.toString()
        }
    }

    private fun visibilityName(visibility: Int): String =
        when (visibility) {
            View.VISIBLE -> "VISIBLE"
            View.INVISIBLE -> "INVISIBLE"
            View.GONE -> "GONE"
            else -> visibility.toString()
        }

    private class ScanState(
        var scannedViews: Int = 0,
        var truncated: Boolean = false,
        var miuiStatusIconContainer: ViewGroup? = null,
    )

    private class SubtreeScanState(
        var nodes: Int = 0,
        var truncated: Boolean = false,
    )

    internal data class Snapshot(
        val hostClassName: String,
        val hostWidth: Int,
        val hostHeight: Int,
        val hostMeasuredWidth: Int,
        val hostMeasuredHeight: Int,
        val directChildren: List<String>,
        val scanRootClassName: String,
        val hostPath: String,
        val ancestorChain: List<String>,
        val scannedViews: Int,
        val truncated: Boolean,
        val entries: List<Entry>,
        val statusIconSubtree: SubtreeSnapshot?,
    ) {
        val summary: String
            get() {
                val counts = entries.groupingBy { it.role }.eachCount()

                return buildString {
                    append("nativeStatus topology ")
                    append("root=")
                    append(scanRootClassName.substringAfterLast('.'))
                    append(" host=")
                    append(hostClassName.substringAfterLast('.'))
                    append(" hostPath=")
                    append(hostPath)
                    append(" scanned=")
                    append(scannedViews)
                    append(" truncated=")
                    append(truncated)
                    append(" mobileNetwork=")
                    append(counts["mobileNetwork"] ?: 0)
                    append(" wifi=")
                    append(counts["wifi"] ?: 0)
                    append(" battery=")
                    append(counts["battery"] ?: 0)
                    append(" miuiStatusIcons=")
                    append(counts["miuiStatusIcons"] ?: 0)
                    append(" statusIcons=")
                    append(counts["statusIcons"] ?: 0)
                    append(" batteryContainer=")
                    append(counts["batteryContainer"] ?: 0)
                    append(" candidateMobile=")
                    append(counts["candidateMobile"] ?: 0)
                    append(" candidateWifi=")
                    append(counts["candidateWifi"] ?: 0)
                }
            }

        val hostLine: String
            get() =
                "nativeStatus host " +
                    "size=${hostWidth}x$hostHeight measured=${hostMeasuredWidth}x$hostMeasuredHeight " +
                    "directChildren=[${directChildren.joinToString(",")}] " +
                    "ancestors=[${ancestorChain.joinToString(" <- ")}]"
    }

    internal data class Entry(
        val role: String,
        val className: String,
        val resourceId: String,
        val parentClassName: String,
        val parentIndex: Int,
        val depth: Int,
        val path: String,
        val visibility: String,
        val width: Int,
        val height: Int,
        val measuredWidth: Int,
        val measuredHeight: Int,
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
        val translationX: Float,
        val translationY: Float,
    ) {
        val logLine: String
            get() =
                "nativeStatus role=$role " +
                    "class=${className.substringAfterLast('.')} " +
                    "id=$resourceId parent=${parentClassName.substringAfterLast('.')} " +
                    "index=$parentIndex depth=$depth path=$path " +
                    "visibility=$visibility " +
                    "bounds=$left,$top-$right,$bottom " +
                    "size=${width}x$height measured=${measuredWidth}x$measuredHeight " +
                    "translation=$translationX,$translationY"
    }

    internal data class SubtreeSnapshot(
        val rootClassName: String,
        val rootResourceId: String,
        val rootChildCount: Int,
        val maxDepth: Int,
        val maxNodes: Int,
        val truncated: Boolean,
        val entries: List<SubtreeEntry>,
    ) {
        val summary: String
            get() =
                "nativeStatus topology iconSubtree " +
                    "root=${rootClassName.substringAfterLast('.')} " +
                    "id=$rootResourceId children=$rootChildCount nodes=${entries.size} " +
                    "maxDepth=$maxDepth maxNodes=$maxNodes truncated=$truncated"
    }

    internal data class SubtreeEntry(
        val className: String,
        val resourceId: String,
        val parentClassName: String,
        val parentIndex: Int,
        val depth: Int,
        val path: String,
        val childCount: Int,
        val visibility: String,
        val width: Int,
        val height: Int,
        val measuredWidth: Int,
        val measuredHeight: Int,
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
        val translationX: Float,
        val translationY: Float,
    ) {
        val logLine: String
            get() =
                "nativeStatus topology iconNode " +
                    "class=${className.substringAfterLast('.')} " +
                    "id=$resourceId parent=${parentClassName.substringAfterLast('.')} " +
                    "index=$parentIndex depth=$depth path=$path children=$childCount " +
                    "visibility=$visibility bounds=$left,$top-$right,$bottom " +
                    "size=${width}x$height measured=${measuredWidth}x$measuredHeight " +
                    "translation=$translationX,$translationY"
    }
}
