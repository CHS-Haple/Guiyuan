package com.chaners.guiyuan.system

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemUiScopeTest {
    @Test
    fun restartUsesExactPidAndSigterm() {
        val command = SystemUiScope.RestartCommand

        assertTrue(command.contains("pidof"))
        assertTrue(command.contains(SystemUiScope.SystemUiPackage))
        assertTrue(command.contains("kill -TERM"))
        assertTrue(command.contains("old_alive"))
        assertTrue(command.contains("new_pids"))
        assertFalse(command.contains("retry_pid"))
    }

    @Test
    fun restartDoesNotUseCrashForceStopSigkillOrBlindKillall() {
        val command = SystemUiScope.RestartCommand

        assertFalse(command.contains("am force-stop"))
        assertFalse(command.contains("am crash"))
        assertFalse(command.contains("kill -9"))
        assertFalse(command.contains("killall"))
        assertFalse(command.contains("pkill"))
    }

    @Test
    fun replacementProbeIsBounded() {
        assertTrue(SystemUiScope.RestartProbeAttempts in 1..100)
        assertTrue(SystemUiScope.RestartProbeIntervalSeconds == "0.1")
    }
}
