package com.chaners.guiyuan.xposed

internal object CompatibilityProbe {
    private val markerClasses = linkedMapOf(
        "statusHost" to "com.android.systemui.statusbar.views.MiuiNotificationStatusContainer",
        "controlCenterHeader" to "com.android.systemui.controlcenter.shade.ControlCenterHeaderView",
        "controlCenterIcons" to "com.android.systemui.controlcenter.phone.widget.ControlCenterFakeStatusIcons",
        "keyguardHeader" to "com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView",
        "batteryView" to "com.android.systemui.statusbar.views.MiuiBatteryMeterView",
    )

    fun inspect(classLoader: ClassLoader): Snapshot {
        val resolved = markerClasses.mapValues { (_, className) ->
            runCatching {
                Class.forName(className, false, classLoader)
            }.isSuccess
        }
        return Snapshot(resolved)
    }

    internal class Snapshot(
        private val resolved: Map<String, Boolean>,
    ) {
        fun isAvailable(marker: String): Boolean = resolved[marker] == true

        val summary: String
            get() {
                val matched = resolved.count { it.value }
                val missing = resolved
                    .filterValues { present -> !present }
                    .keys
                    .joinToString(",")

                return buildString {
                    append("SystemUI ready profile=hyperos-17.03.260226.r ")
                    append("compatibility=")
                    append(if (matched == resolved.size) "STRUCTURAL_MATCH" else "PARTIAL_MATCH")
                    append(" markers=")
                    append(matched)
                    append('/')
                    append(resolved.size)
                    if (missing.isNotEmpty()) {
                        append(" missing=")
                        append(missing)
                    }
                }
            }
    }
}
