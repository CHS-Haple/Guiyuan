package com.chaners.guiyuan.system

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SysUiScopeTest {
    @Test
    fun restartUsesExactPidAndSigterm() {
        val command = SysUiScope.restartCmd

        assertTrue(command.contains("pidof"))
        assertTrue(command.contains(SysUiScope.SYS_UI_PACKAGE))
        assertTrue(command.contains("kill -TERM"))
        assertTrue(command.contains("old_alive"))
        assertTrue(command.contains("new_pids"))
        assertFalse(command.contains("retry_pid"))
    }

    @Test
    fun restartDoesNotUseCrashForceStopSigkillOrBlindKillall() {
        val command = SysUiScope.restartCmd

        assertFalse(command.contains("am force-stop"))
        assertFalse(command.contains("am crash"))
        assertFalse(command.contains("kill -9"))
        assertFalse(command.contains("killall"))
        assertFalse(command.contains("pkill"))
    }

    @Test
    fun replacementProbeIsBounded() {
        assertTrue(SysUiScope.RESTART_TRIES in 1..100)
        assertTrue(SysUiScope.RESTART_PROBE_SEC == "0.1")
    }
}
