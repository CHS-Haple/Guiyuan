package com.chaners.guiyuan.system

internal sealed interface XposedStatus {
    data object Checking : XposedStatus

    data object FrameworkUnavailable : XposedStatus

    data object QueryUnavailable : XposedStatus

    data class Connected(
        val apiVersion: Int,
        val scopes: Set<String>,
        val runningTargets: Set<String>,
    ) : XposedStatus {
        val sysUiInScope get() = "com.android.systemui" in scopes
        val sysUiRunning get() = "com.android.systemui" in runningTargets
    }
}
