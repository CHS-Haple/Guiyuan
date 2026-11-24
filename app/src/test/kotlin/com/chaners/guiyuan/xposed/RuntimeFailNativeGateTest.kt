package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeFailNativeGateTest {
    @Test
    fun globalFailureIsStickyUntilAnExplicitCompleteOffOnCycle() {
        val gate = RuntimeFailNativeGate()
        assertTrue(gate.allowManualEnable())
        assertTrue(gate.beginGlobalFailure())
        assertTrue(gate.suspended)
        assertFalse(gate.beginGlobalFailure())
        gate.globalCleanupCompleted(true)
        assertFalse(gate.allowManualEnable())
        gate.manualDisableCompleted(true)
        assertTrue(gate.allowManualEnable())
        assertFalse(gate.suspended)
    }

    @Test
    fun incompleteCleanupRequiresRestartRatherThanSilentReacquisition() {
        val gate = RuntimeFailNativeGate()
        assertTrue(gate.beginGlobalFailure())
        gate.globalCleanupCompleted(false)
        assertTrue(gate.restartRequired)
        gate.manualDisableCompleted(true)
        assertFalse(gate.allowManualEnable())
        assertFalse(gate.beginGlobalFailure())
    }

    @Test
    fun manualOffCannotArmResetIfItsOwnReleaseFails() {
        val gate = RuntimeFailNativeGate()
        assertTrue(gate.beginGlobalFailure())
        gate.globalCleanupCompleted(true)
        gate.manualDisableCompleted(false)
        assertTrue(gate.restartRequired)
        assertFalse(gate.allowManualEnable())
    }

    @Test
    fun newSevereFaultCanCancelArmedManualReset() {
        val gate = RuntimeFailNativeGate()
        assertTrue(gate.beginGlobalFailure())
        gate.globalCleanupCompleted(true)
        gate.manualDisableCompleted(true)
        assertTrue(gate.beginGlobalFailure())
        assertFalse(gate.allowManualEnable())
    }
}
