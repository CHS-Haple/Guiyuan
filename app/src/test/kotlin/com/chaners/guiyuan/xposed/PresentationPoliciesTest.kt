package com.chaners.guiyuan.xposed

import java.lang.reflect.InvocationTargetException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PresentationPoliciesTest {

    @Test
    fun ownedEntriesPreserveExistingAndRestoreOnlyAddedSlots() {
        val slots = mutableListOf("alarm_clock", "wifi")
        val owned = OwnedEntries.add(slots, listOf("wifi", "mobile", "no_sim"))
        assertEquals(listOf("mobile", "no_sim"), owned)
        assertEquals(listOf("alarm_clock", "wifi", "mobile", "no_sim"), slots)
        OwnedEntries.restore(slots, owned)
        assertEquals(listOf("alarm_clock", "wifi"), slots)
    }

    @Test
    fun rollbackFailureKeepsOriginalAdditionError() {
        val addError = IllegalStateException("add-failed")
        val rollbackError = IllegalStateException("rollback-failed")
        val slots =
            object : ArrayList<String>() {
                override fun add(element: String): Boolean {
                    if (element == "airplane") throw addError
                    return super.add(element)
                }

                override fun remove(element: String): Boolean {
                    if (element == "mobile") throw rollbackError
                    return super.remove(element)
                }
            }
        slots.add("native")
        val error =
            runCatching {
                OwnedEntries.add(slots, listOf("mobile", "airplane"))
            }.exceptionOrNull()
        assertTrue(error === addError)
        assertEquals(listOf(rollbackError), error?.suppressed?.toList())
    }

    @Test
    fun reflectedFatalRestoreFailureStopsIndependentSlotRemovals() {
        val fatal = OutOfMemoryError("native-slot-restore")
        val slots =
            object : ArrayList<String>() {
                override fun remove(element: String): Boolean {
                    if (element == "mobile") throw InvocationTargetException(fatal)
                    return super.remove(element)
                }
            }
        slots.addAll(listOf("native", "mobile", "wifi"))

        val error = runCatching {
            OwnedEntries.restore(slots, listOf("wifi", "mobile"))
        }.exceptionOrNull()

        assertSame(fatal, error)
        // No later sibling writes after a VM-fatal restoration error.
        assertEquals(listOf("native", "mobile", "wifi"), slots)
    }

    @Test
    fun reflectedFatalRollbackFailureOutranksOrdinarySlotAdditionFailure() {
        val addError = IllegalStateException("add-failed")
        val fatal = OutOfMemoryError("slot-rollback-fatal")
        val slots =
            object : ArrayList<String>() {
                override fun add(element: String): Boolean {
                    if (element == "airplane") throw addError
                    return super.add(element)
                }

                override fun remove(element: String): Boolean {
                    if (element == "mobile") throw InvocationTargetException(fatal)
                    return super.remove(element)
                }
            }
        slots.add("native")

        val error = runCatching {
            OwnedEntries.add(slots, listOf("mobile", "airplane"))
        }.exceptionOrNull()

        assertSame(fatal, error)
        assertEquals(listOf("native", "mobile"), slots)
    }

    @Test
    fun restoreAttemptsEveryOwnedEntryAfterOneRemovalFails() {
        val removalError = IllegalStateException("no-sim-removal-failed")
        val slots =
            object : ArrayList<String>() {
                override fun remove(element: String): Boolean {
                    if (element == "no_sim") throw removalError
                    return super.remove(element)
                }
            }
        slots.addAll(listOf("native", "mobile", "no_sim", "airplane"))
        val error =
            runCatching {
                OwnedEntries.restore(slots, listOf("mobile", "no_sim", "airplane"))
            }.exceptionOrNull()
        assertTrue(error === removalError)
        assertEquals(listOf("native", "no_sim"), slots)
    }

    @Test
    fun persistentIgnoredSlotRestoreRemovesOnlySessionOwnedDelta() {
        val existing = listOf("alarm_clock", "wifi")
        val requested = listOf("wifi", "mobile", "no_sim")
        val owned =
            OwnedEntries.delta(
                existing = existing,
                requested = requested,
            )
        assertEquals(listOf("mobile", "no_sim"), owned)

        val live = listOf("alarm_clock", "wifi", "mobile", "no_sim", "vpn")
        assertEquals(
            listOf("alarm_clock", "wifi", "vpn"),
            OwnedEntries.restoreTarget(
                live = live,
                ownedEntries = owned,
            ),
        )
    }
}
