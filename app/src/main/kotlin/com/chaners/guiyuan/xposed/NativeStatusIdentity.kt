package com.chaners.guiyuan.xposed

import android.view.View
import android.view.ViewGroup

internal object NativeStatusIdentity {
    internal fun roleFor(
        view: View,
        resourceId: String,
    ): String? {
        val className = view.javaClass.name
        return when (className) {
            NativeStatusInventory.MOBILE_NETWORK_VIEW_CLASS_NAME -> "mobileNetwork"
            NativeStatusInventory.WIFI_VIEW_CLASS_NAME -> "wifi"
            NativeStatusInventory.BATTERY_VIEW_CLASS_NAME -> "battery"
            NativeStatusInventory.MIUI_STATUS_ICON_CONTAINER_CLASS_NAME -> "miuiStatusIcons"
            NativeStatusInventory.STATUS_ICON_CONTAINER_CLASS_NAME -> "statusIcons"
            NativeStatusInventory.BATTERY_CONTAINER_CLASS_NAME -> "batteryContainer"
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

    internal fun pathFromRoot(
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

    internal fun ancestorChain(view: View): List<String> =
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

    internal fun resourceId(view: View): String {
        if (view.id == View.NO_ID) {
            return "none"
        }

        return SysUiPresentationRuntime.optionalNativeRead {
            view.resources.getResourceName(view.id)
        } ?: view.id.toString()
    }

    internal fun visibilityName(visibility: Int): String =
        when (visibility) {
            View.VISIBLE -> "VISIBLE"
            View.INVISIBLE -> "INVISIBLE"
            View.GONE -> "GONE"
            else -> visibility.toString()
        }
}
