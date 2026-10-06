package com.chaners.guiyuan.system

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SysUiScopeTest {
    @Test
    fun restartUsesExactPidAndSigterm() {
        val command = SysUiScope.RestartCommand

        assertTrue(command.contains("pidof"))
        assertTrue(command.contains(SysUiScope.SystemUiPackage))
        assertTrue(command.contains("kill -TERM"))
        assertTrue(command.contains("old_alive"))
        assertTrue(command.contains("new_pids"))
        assertFalse(command.contains("retry_pid"))
    }

    @Test
    fun restartAvoidsDestructiveKill() {
        val command = SysUiScope.RestartCommand

        assertFalse(command.contains("am force-stop"))
        assertFalse(command.contains("am crash"))
        assertFalse(command.contains("kill -9"))
        assertFalse(command.contains("killall"))
        assertFalse(command.contains("pkill"))
    }

    @Test
    fun replacementProbeIsBounded() {
        assertTrue(SysUiScope.RestartProbeAttempts in 1..100)
        assertTrue(SysUiScope.RestartProbeIntervalSeconds == "0.1")
    }
}
