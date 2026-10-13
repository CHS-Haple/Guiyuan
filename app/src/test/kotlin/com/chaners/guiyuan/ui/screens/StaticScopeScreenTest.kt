package com.chaners.guiyuan.ui.screens

import com.chaners.guiyuan.system.XposedStatus
import io.github.libxposed.service.HookedTarget
import org.junit.Assert.assertEquals
import org.junit.Test

class StaticScopeScreenTest {
    private val sysUi = "com.android.systemui"
    private val settings = "com.android.settings"

    @Test
    fun authorizedScopeWithoutReportedTargetIsNotConnected() {
        val state =
            XposedStatus.Connected(
                apiVersion = 102,
                scopes = setOf(sysUi),
                runningTargets = emptyMap(),
            )
        assertEquals(ScopeState.Authorized, scopeState(sysUi, true, state))
        assertEquals(ScopeState.Unauthorized, scopeState(settings, true, state))
    }

    @Test
    fun failedChildProcessAffectsOnlyItsOwnScope() {
        val state =
            XposedStatus.Connected(
                apiVersion = 102,
                scopes = setOf(sysUi, settings),
                runningTargets = mapOf("$sysUi:remote" to HookedTarget.State.FAILED),
            )
        assertEquals(ScopeState.Failed, scopeState(sysUi, true, state))
        assertEquals(ScopeState.Authorized, scopeState(settings, true, state))
    }

    @Test
    fun missingAndUnavailableFrameworkKeepSeparateStates() {
        val unavailable = XposedStatus.QueryUnavailable
        assertEquals(ScopeState.Missing, scopeState(settings, false, unavailable))
        assertEquals(ScopeState.Unknown, scopeState(settings, null, unavailable))
        assertEquals(ScopeState.Unknown, scopeState(sysUi, true, XposedStatus.Checking))
    }
}
