package com.chaners.guiyuan.ui.screens

import com.chaners.guiyuan.R
import com.chaners.guiyuan.system.XposedStatus

// Home's display decision is distinct from live Xposed status collection and UI animation.
internal enum class RuntimeStatusTone {
    Success,
    Warning,
    Error,
    Neutral,
}

internal enum class RuntimeStatusMarkKind {
    Check,
    Alert,
    Minus,
    Loading,
}

internal enum class HomeRuntimeAction {
    None,
    Restart,
    RequestScope,
}

internal data class HomeRuntimeCardState(
    val titleRes: Int,
    val summaryRes: Int,
    val tone: RuntimeStatusTone,
    val mark: RuntimeStatusMarkKind,
    val action: HomeRuntimeAction = HomeRuntimeAction.None,
)

// Transient user commands take priority over the last observed Xposed state.
private fun transientHomeRuntimeCardState(
    enabled: Boolean,
    restartInProgress: Boolean,
    hotReloadInProgress: Boolean,
    scopeRequestInProgress: Boolean,
): HomeRuntimeCardState? {
    if (!enabled) {
        return HomeRuntimeCardState(
            titleRes = R.string.home_runtime_disabled,
            summaryRes = R.string.home_runtime_disabled_summary,
            tone = RuntimeStatusTone.Neutral,
            mark = RuntimeStatusMarkKind.Minus,
        )
    }

    if (restartInProgress) {
        return HomeRuntimeCardState(
            titleRes = R.string.home_runtime_restarting,
            summaryRes = R.string.home_runtime_restarting_summary,
            tone = RuntimeStatusTone.Warning,
            mark = RuntimeStatusMarkKind.Loading,
        )
    }

    if (hotReloadInProgress) {
        return HomeRuntimeCardState(
            titleRes = R.string.home_runtime_reloading,
            summaryRes = R.string.home_runtime_reloading_summary,
            tone = RuntimeStatusTone.Warning,
            mark = RuntimeStatusMarkKind.Loading,
        )
    }

    if (scopeRequestInProgress) {
        return HomeRuntimeCardState(
            titleRes = R.string.home_runtime_requesting,
            summaryRes = R.string.home_runtime_requesting_summary,
            tone = RuntimeStatusTone.Warning,
            mark = RuntimeStatusMarkKind.Loading,
        )
    }
    return null
}

// When there is no outstanding command, only the authoritative Xposed snapshot wins.
private fun stableHomeRuntimeCardState(runtimeStatus: XposedStatus): HomeRuntimeCardState {
    return when (runtimeStatus) {
        XposedStatus.Checking ->
            HomeRuntimeCardState(
                titleRes = R.string.home_runtime_checking,
                summaryRes = R.string.home_runtime_checking_summary,
                tone = RuntimeStatusTone.Neutral,
                mark = RuntimeStatusMarkKind.Loading,
            )

        XposedStatus.FrameworkUnavailable ->
            HomeRuntimeCardState(
                titleRes = R.string.home_runtime_framework_unavailable,
                summaryRes = R.string.home_runtime_framework_unavailable_summary,
                tone = RuntimeStatusTone.Error,
                mark = RuntimeStatusMarkKind.Alert,
            )

        XposedStatus.QueryUnavailable ->
            HomeRuntimeCardState(
                titleRes = R.string.home_runtime_unknown,
                summaryRes = R.string.home_runtime_unknown_summary,
                tone = RuntimeStatusTone.Warning,
                mark = RuntimeStatusMarkKind.Alert,
            )

        is XposedStatus.Connected ->
            when {
                !runtimeStatus.sysUiInScope ->
                    HomeRuntimeCardState(
                        titleRes = R.string.home_runtime_unhooked,
                        summaryRes = R.string.home_runtime_unhooked_summary,
                        tone = RuntimeStatusTone.Error,
                        mark = RuntimeStatusMarkKind.Alert,
                        action = HomeRuntimeAction.RequestScope,
                    )

                runtimeStatus.sysUiRunning ->
                    HomeRuntimeCardState(
                        titleRes = R.string.home_runtime_running,
                        summaryRes = R.string.home_runtime_running_summary,
                        tone = RuntimeStatusTone.Success,
                        mark = RuntimeStatusMarkKind.Check,
                    )

                else ->
                    HomeRuntimeCardState(
                        titleRes = R.string.home_runtime_pending,
                        summaryRes = R.string.home_runtime_restart_summary,
                        tone = RuntimeStatusTone.Warning,
                        mark = RuntimeStatusMarkKind.Alert,
                        action = HomeRuntimeAction.Restart,
                    )
            }
    }
}

// Preserve the existing priority: disabled > restart > hot reload > scope request > stable.
internal fun resolveHomeRuntimeCardState(
    enabled: Boolean,
    runtimeStatus: XposedStatus,
    hotReloadInProgress: Boolean,
    restartInProgress: Boolean,
    scopeRequestInProgress: Boolean,
): HomeRuntimeCardState =
    transientHomeRuntimeCardState(
        enabled = enabled,
        restartInProgress = restartInProgress,
        hotReloadInProgress = hotReloadInProgress,
        scopeRequestInProgress = scopeRequestInProgress,
    ) ?: stableHomeRuntimeCardState(runtimeStatus)
