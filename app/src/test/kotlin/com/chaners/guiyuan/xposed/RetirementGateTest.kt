package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class RetirementGateTest {
    @Test
    fun successfulReleaseDropsOldOwner() {
        val gate = RetirementGate<Any>()
        val first = Any()
        assertNull(gate.pending)
        assertEquals(3, gate.release(first) { 3 })
        assertNull(gate.pending)
        assertEquals(0, gate.release(Any()) { 0 })
        assertNull(gate.pending)
    }

    @Test
    fun failedReleaseRetainsOwnerAndBlocksForeignAcquisition() {
        val gate = RetirementGate<Any>()
        val first = Any()
        val failure = IllegalStateException("native-padding-restore-incomplete")
        assertSame(failure, runCatching {
            gate.release(first) { throw failure }
        }.exceptionOrNull())
        assertSame(first, gate.pending)

        var foreignWrites = 0
        assertEquals("release-incomplete", runCatching {
            gate.release(Any()) { foreignWrites++ }
        }.exceptionOrNull()?.message)
        assertEquals(0, foreignWrites)
        assertSame(first, gate.pending)

        assertEquals(2, gate.release(first) { 2 })
        assertNull(gate.pending)
    }

    @Test
    fun nestedReleaseCannotMutateSameOwnerTwice() {
        val gate = RetirementGate<Any>()
        val first = Any()
        var writes = 0
        assertEquals(1, gate.release(first) {
            writes++
            assertSame(first, gate.pending)
            assertEquals("release-reentered", runCatching {
                gate.release(first) { writes++ }
            }.exceptionOrNull()?.message)
            1
        })
        assertEquals(1, writes)
        assertNull(gate.pending)
    }

    @Test
    fun nestedFailureLeavesReceiptForLaterExplicitRelease() {
        val gate = RetirementGate<Any>()
        val first = Any()
        assertEquals("release-reentered", runCatching {
            gate.release(first) {
                gate.release(first) { 0 }
            }
        }.exceptionOrNull()?.message)
        assertSame(first, gate.pending)
        assertEquals(0, gate.release(first) { 0 })
        assertNull(gate.pending)
    }
}
