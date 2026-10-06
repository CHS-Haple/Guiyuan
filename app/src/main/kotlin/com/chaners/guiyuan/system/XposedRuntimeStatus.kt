package com.chaners.guiyuan.system

internal sealed interface XposedStatus {
    data object Checking : XposedStatus

    data object FrameworkUnavailable : XposedStatus

    data object QueryUnavailable : XposedStatus

    data class Connected(
        val systemUiInScope: Boolean,
        val systemUiRunning: Boolean,
    ) : XposedStatus
}
