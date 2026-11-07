package com.chaners.guiyuan.ui.screens

import com.chaners.guiyuan.R
import com.chaners.guiyuan.system.XposedStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeRuntimeCardPolicyTest {
    @Test
    fun disabledAlwaysOverridesPendingWorkAndFrameworkStatus() {
        val state = resolveHomeRuntimeCardState(
            enabled = false,
            runtimeStatus = XposedStatus.FrameworkUnavailable,
            hotReloadInProgress = true,
            restartInProgress = true,
            scopeRequestInProgress = true,
        )
        assertEquals(R.string.home_runtime_disabled, state.titleRes)
        assertEquals(RuntimeStatusTone.Neutral, state.tone)
        assertEquals(RuntimeStatusMarkKind.Minus, state.mark)
        assertEquals(HomeRuntimeAction.None, state.action)
    }

    @Test
    fun activeTransientWorkUsesRestartThenReloadThenScopePriority() {
        fun state(restart: Boolean, reload: Boolean, scope: Boolean) =
            resolveHomeRuntimeCardState(
                enabled = true,
                runtimeStatus = XposedStatus.FrameworkUnavailable,
                hotReloadInProgress = reload,
                restartInProgress = restart,
                scopeRequestInProgress = scope,
            )
        assertEquals(R.string.home_runtime_restarting, state(true, true, true).titleRes)
        assertEquals(R.string.home_runtime_reloading, state(false, true, true).titleRes)
        assertEquals(R.string.home_runtime_requesting, state(false, false, true).titleRes)
        assertEquals(RuntimeStatusMarkKind.Loading, state(false, false, true).mark)
        assertEquals(HomeRuntimeAction.None, state(false, false, true).action)
    }

    @Test
    fun stableFrameworkStatusesRetainSeverityAndAction() {
        fun stable(snapshot: XposedStatus) =
            resolveHomeRuntimeCardState(
                enabled = true,
                runtimeStatus = snapshot,
                hotReloadInProgress = false,
                restartInProgress = false,
                scopeRequestInProgress = false,
            )
        assertEquals(R.string.home_runtime_checking, stable(XposedStatus.Checking).titleRes)
        assertEquals(
            RuntimeStatusTone.Error,
            stable(XposedStatus.FrameworkUnavailable).tone,
        )
        assertEquals(R.string.home_runtime_unknown, stable(XposedStatus.QueryUnavailable).titleRes)
        assertEquals(
            HomeRuntimeAction.None,
            stable(XposedStatus.FrameworkUnavailable).action,
        )
    }

    @Test
    fun connectedScopeAndPendingConditionsKeepDistinctActions() {
        fun connected(scopes: Set<String>) = XposedStatus.Connected(
            apiVersion = 102,
            scopes = scopes,
            runningTargets = emptyMap(),
        )
        val missing = resolveHomeRuntimeCardState(true, connected(emptySet()), false, false, false)
        assertEquals(R.string.home_runtime_unhooked, missing.titleRes)
        assertEquals(HomeRuntimeAction.RequestScope, missing.action)
        val pending = resolveHomeRuntimeCardState(
            true,
            connected(setOf("com.android.systemui")),
            false,
            false,
            false,
        )
        assertEquals(R.string.home_runtime_pending, pending.titleRes)
        assertEquals(HomeRuntimeAction.Restart, pending.action)
        assertEquals(RuntimeStatusTone.Warning, pending.tone)
    }
}
