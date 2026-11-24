package com.chaners.guiyuan.xposed

import java.lang.reflect.InvocationTargetException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

private const val ADD_FAILURE_MESSAGE = "add-failed"
private const val UNOWNED_SLOT_VPN = "vpn"
private const val SLOT_MOBILE = "mobile"
private const val SLOT_WIFI = "wifi"
private const val SLOT_NO_SIM = "no_sim"
private const val SLOT_NATIVE = "native"
private const val SLOT_ALARM_CLOCK = "alarm_clock"
private const val SLOT_AIRPLANE = "airplane"

class PresentationPoliciesTest {

    @Test
    fun ownedEntriesPreserveExistingAndRestoreOnlyAddedSlots() {
        val slots = mutableListOf(SLOT_ALARM_CLOCK, SLOT_WIFI)
        val owned = OwnedEntries.add(slots, listOf(SLOT_WIFI, SLOT_MOBILE, SLOT_NO_SIM))
        assertEquals(listOf(SLOT_MOBILE, SLOT_NO_SIM), owned)
        assertEquals(listOf(SLOT_ALARM_CLOCK, SLOT_WIFI, SLOT_MOBILE, SLOT_NO_SIM), slots)
        OwnedEntries.restore(slots, owned)
        assertEquals(listOf(SLOT_ALARM_CLOCK, SLOT_WIFI), slots)
    }

    @Test
    fun rollbackFailureKeepsOriginalAdditionError() {
        val addError = IllegalStateException(ADD_FAILURE_MESSAGE)
        val rollbackError = IllegalStateException("rollback-failed")
        val slots =
            object : ArrayList<String>() {
                override fun add(element: String): Boolean {
                    if (element == SLOT_AIRPLANE) throw addError
                    return super.add(element)
                }

                override fun remove(element: String): Boolean {
                    if (element == SLOT_MOBILE) throw rollbackError
                    return super.remove(element)
                }
            }
        slots.add(SLOT_NATIVE)
        val error =
            runCatching {
                OwnedEntries.add(slots, listOf(SLOT_MOBILE, SLOT_AIRPLANE))
            }.exceptionOrNull()
        assertTrue(error === addError)
        assertEquals(listOf(rollbackError), error?.suppressed?.toList())
    }

    @Test
    fun repeatedSameRemovalExceptionPreservesOriginalAndContinues() {
        val repeated = IllegalStateException("same-removal-failure")
        val attempted = mutableListOf<String>()
        val slots =
            object : ArrayList<String>() {
                override fun remove(element: String): Boolean {
                    attempted += element
                    if (element == SLOT_MOBILE || element == SLOT_NO_SIM) throw repeated
                    return super.remove(element)
                }
            }
        slots.addAll(listOf(SLOT_NATIVE, SLOT_MOBILE, SLOT_NO_SIM, SLOT_AIRPLANE))

        val error = runCatching {
            OwnedEntries.restore(slots, listOf(SLOT_MOBILE, SLOT_NO_SIM, SLOT_AIRPLANE))
        }.exceptionOrNull()

        assertSame(repeated, error)
        assertEquals(0, repeated.suppressed.size)
        assertEquals(listOf(SLOT_AIRPLANE, SLOT_NO_SIM, SLOT_MOBILE), attempted)
        assertEquals(listOf(SLOT_NATIVE, SLOT_MOBILE, SLOT_NO_SIM), slots)
    }

    @Test
    fun reflectedFatalRestoreFailureStopsIndependentSlotRemovals() {
        val fatal = OutOfMemoryError("native-slot-restore")
        val slots =
            object : ArrayList<String>() {
                override fun remove(element: String): Boolean {
                    if (element == SLOT_MOBILE) throw InvocationTargetException(fatal)
                    return super.remove(element)
                }
            }
        slots.addAll(listOf(SLOT_NATIVE, SLOT_MOBILE, SLOT_WIFI))

        val error = runCatching {
            OwnedEntries.restore(slots, listOf(SLOT_WIFI, SLOT_MOBILE))
        }.exceptionOrNull()

        assertSame(fatal, error)
        // No later sibling writes after a VM-fatal restoration error.
        assertEquals(listOf(SLOT_NATIVE, SLOT_MOBILE, SLOT_WIFI), slots)
    }

    @Test
    fun reflectedFatalRollbackFailureOutranksOrdinarySlotAdditionFailure() {
        val addError = IllegalStateException(ADD_FAILURE_MESSAGE)
        val fatal = OutOfMemoryError("slot-rollback-fatal")
        val slots =
            object : ArrayList<String>() {
                override fun add(element: String): Boolean {
                    if (element == SLOT_AIRPLANE) throw addError
                    return super.add(element)
                }

                override fun remove(element: String): Boolean {
                    if (element == SLOT_MOBILE) throw InvocationTargetException(fatal)
                    return super.remove(element)
                }
            }
        slots.add(SLOT_NATIVE)

        val error = runCatching {
            OwnedEntries.add(slots, listOf(SLOT_MOBILE, SLOT_AIRPLANE))
        }.exceptionOrNull()

        assertSame(fatal, error)
        assertEquals(listOf(SLOT_NATIVE, SLOT_MOBILE), slots)
    }

    @Test
    fun restoreAttemptsEveryOwnedEntryAfterOneRemovalFails() {
        val removalError = IllegalStateException("no-sim-removal-failed")
        val slots =
            object : ArrayList<String>() {
                override fun remove(element: String): Boolean {
                    if (element == SLOT_NO_SIM) throw removalError
                    return super.remove(element)
                }
            }
        slots.addAll(listOf(SLOT_NATIVE, SLOT_MOBILE, SLOT_NO_SIM, SLOT_AIRPLANE))
        val error =
            runCatching {
                OwnedEntries.restore(slots, listOf(SLOT_MOBILE, SLOT_NO_SIM, SLOT_AIRPLANE))
            }.exceptionOrNull()
        assertTrue(error === removalError)
        assertEquals(listOf(SLOT_NATIVE, SLOT_NO_SIM), slots)
    }

    @Test
    fun persistentIgnoredSlotRestoreRemovesOnlySessionOwnedDelta() {
        val existing = listOf(SLOT_ALARM_CLOCK, SLOT_WIFI)
        val requested = listOf(SLOT_WIFI, SLOT_MOBILE, SLOT_NO_SIM)
        val owned =
            OwnedEntries.delta(
                existing = existing,
                requested = requested,
            )
        assertEquals(listOf(SLOT_MOBILE, SLOT_NO_SIM), owned)

        val live = listOf(SLOT_ALARM_CLOCK, SLOT_WIFI, SLOT_MOBILE, SLOT_NO_SIM, UNOWNED_SLOT_VPN)
        assertEquals(
            listOf(SLOT_ALARM_CLOCK, SLOT_WIFI, UNOWNED_SLOT_VPN),
            OwnedEntries.restoreTarget(
                live = live,
                ownedEntries = owned,
            ),
        )
    }
}
