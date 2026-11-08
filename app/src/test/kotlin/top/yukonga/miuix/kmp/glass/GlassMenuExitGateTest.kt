package top.yukonga.miuix.kmp.glass

import org.junit.Assert.assertEquals
import org.junit.Test

/** Regression coverage for Guiyuan's root-host lifecycle added around upstream Glass menus. */
class GlassMenuExitGateTest {
    @Test
    fun primaryOnlyMenuCompletesAfterPrimaryExit() {
        val gate = GlassMenuExitGate()
        var completionCount = 0
        gate.reset()
        gate.primaryDismissed(secondaryPresent = false) { completionCount++ }
        assertEquals(1, completionCount)
        // A duplicate transition-end callback must not unmount the host twice.
        gate.primaryDismissed(secondaryPresent = false) { completionCount++ }
        gate.secondaryDismissed { completionCount++ }
        assertEquals(1, completionCount)
    }

    @Test
    fun secondaryOpenAndPrimaryExitsFirstWaitsForSecondary() {
        val gate = GlassMenuExitGate()
        var completionCount = 0
        gate.reset()
        gate.primaryDismissed(secondaryPresent = true) { completionCount++ }
        assertEquals(0, completionCount)
        gate.secondaryDismissed { completionCount++ }
        assertEquals(1, completionCount)
    }

    @Test
    fun secondaryExitsFirstAndPrimaryFinishesLast() {
        val gate = GlassMenuExitGate()
        var completionCount = 0
        gate.reset()
        gate.secondaryDismissed { completionCount++ }
        assertEquals(0, completionCount)
        gate.primaryDismissed(secondaryPresent = false) { completionCount++ }
        assertEquals(1, completionCount)
    }

    @Test
    fun reopeningClearsOldExitState() {
        val gate = GlassMenuExitGate()
        var completionCount = 0
        gate.reset()
        gate.primaryDismissed(secondaryPresent = false) { completionCount++ }
        gate.reset()
        gate.secondaryDismissed { completionCount++ }
        assertEquals(1, completionCount)
        gate.primaryDismissed(secondaryPresent = false) { completionCount++ }
        assertEquals(2, completionCount)
    }

    @Test
    fun initialSecondaryDismissWithoutPrimaryHasNoCompletion() {
        val gate = GlassMenuExitGate()
        var completionCount = 0
        gate.secondaryDismissed { completionCount++ }
        assertEquals(0, completionCount)
    }
}
