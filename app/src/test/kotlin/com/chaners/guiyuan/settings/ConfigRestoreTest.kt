package com.chaners.guiyuan.settings

import java.io.IOException
import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

private const val TEST_LAYOUT_KEY = "layout"
private const val TEST_OLD_VALUE = "old"
private const val TEST_FEATURE_ROLLBACK = "feature rollback"
private const val TEST_FEATURE_WRITE = "feature write"

class ConfigRestoreTest {
    @Test
    fun successDoesNotAttemptCompensation() {
        var rollbackCount = 0
        assertTrue(
            restoreBackupStores(
                restoreVisual = { true },
                restoreFeature = { true },
                rollbackVisual = { rollbackCount++; true },
                rollbackFeature = { rollbackCount++; true },
            ),
        )
        assertEquals(0, rollbackCount)
    }

    @Test
    fun visualFalseCompensatesPartialWriteWithoutTouchingFeatures() {
        val visuals = mutableMapOf(TEST_LAYOUT_KEY to TEST_OLD_VALUE)
        var featureCalls = 0
        var featureRollbacks = 0
        assertFalse(
            restoreBackupStores(
                restoreVisual = {
                    visuals[TEST_LAYOUT_KEY] = "new"
                    false
                },
                restoreFeature = { featureCalls++; true },
                rollbackVisual = {
                    visuals[TEST_LAYOUT_KEY] = TEST_OLD_VALUE
                    true
                },
                rollbackFeature = { featureRollbacks++; true },
            ),
        )
        assertEquals(mapOf(TEST_LAYOUT_KEY to TEST_OLD_VALUE), visuals)
        assertEquals(0, featureCalls)
        assertEquals(0, featureRollbacks)
    }

    @Test
    fun featureCommitThrowRestoresBothKeyMapsIncludingAbsentKeysAndTimestamp() {
        val visual = mutableMapOf<String, Any>(TEST_LAYOUT_KEY to TEST_OLD_VALUE)
        val feature = mutableMapOf<String, Any>(
            FEATURE_ENABLED_KEY to false,
            FEATURE_CHANGED_AT_NS_KEY to 17L,
        )
        val beforeVisual = visual.toMap()
        val beforeFeature = feature.toMap()
        val cause = IOException("feature commit failed")
        val thrown =
            assertThrows(IOException::class.java) {
                restoreBackupStores(
                    restoreVisual = { visual[TEST_LAYOUT_KEY] = "new"; true },
                    restoreFeature = {
                        feature[FEATURE_AOD_KEY] = true
                        feature[FEATURE_CHANGED_AT_NS_KEY] = 999L
                        throw cause
                    },
                    rollbackVisual = { visual.clear(); visual.putAll(beforeVisual); true },
                    rollbackFeature = { feature.clear(); feature.putAll(beforeFeature); true },
                )
            }
        assertTrue(thrown === cause)
        assertEquals(beforeVisual, visual)
        assertEquals(beforeFeature, feature)
        assertFalse(feature.containsKey(FEATURE_AOD_KEY))
    }

    @Test
    fun visualCommitThrowDoesNotAttemptFeatureButCompensatesVisual() {
        val entries = mutableMapOf<String, Any>("preset" to 3)
        val before = entries.toMap()
        var featuresTouched = false
        val cause = IOException("visual commit failed")
        val thrown =
            assertThrows(IOException::class.java) {
                restoreBackupStores(
                    restoreVisual = {
                        entries["preset"] = 7
                        throw cause
                    },
                    restoreFeature = { featuresTouched = true; true },
                    rollbackVisual = { entries.clear(); entries.putAll(before); true },
                    rollbackFeature = { featuresTouched = true; true },
                )
            }
        assertTrue(thrown === cause)
        assertEquals(before, entries)
        assertFalse(featuresTouched)
    }

    @Test
    fun featureFalseCompensatesBothStoresAndReturnsFalse() {
        val order = mutableListOf<String>()
        assertFalse(
            restoreBackupStores(
                restoreVisual = { order.add("visual write"); true },
                restoreFeature = { order.add(TEST_FEATURE_WRITE); false },
                rollbackVisual = { order.add("visual rollback"); true },
                rollbackFeature = { order.add(TEST_FEATURE_ROLLBACK); true },
            ),
        )
        assertEquals(
            listOf("visual write", TEST_FEATURE_WRITE, TEST_FEATURE_ROLLBACK, "visual rollback"),
            order,
        )
    }

    @Test
    fun failureInFeatureRollbackDoesNotPreventVisualRollbackOrHideOriginalCause() {
        val cause = IOException(TEST_FEATURE_WRITE)
        var visualRolledBack = false
        val thrown =
            assertThrows(IOException::class.java) {
                restoreBackupStores(
                    restoreVisual = { true },
                    restoreFeature = { throw cause },
                    rollbackVisual = { visualRolledBack = true; true },
                    rollbackFeature = { throw IllegalStateException(TEST_FEATURE_ROLLBACK) },
                )
            }
        assertTrue(thrown === cause)
        assertTrue(visualRolledBack)
        assertEquals(1, thrown.suppressed.size)
        assertTrue(thrown.suppressed[0].message!!.contains(TEST_FEATURE_ROLLBACK))
    }

    @Test
    fun unsuccessfulRollbackNeverReportsRestoreAsSuccessful() {
        val failure =
            assertThrows(IllegalStateException::class.java) {
                restoreBackupStores(
                    restoreVisual = { true },
                    restoreFeature = { false },
                    rollbackVisual = { false },
                    rollbackFeature = { true },
                )
            }
        assertTrue(failure.message!!.contains("visual"))
    }

    @Test
    fun bothRollbackFailuresRemainVisibleOnTheOriginalWriteException() {
        val cause = IOException("feature commit threw")
        val thrown =
            assertThrows(IOException::class.java) {
                restoreBackupStores(
                    restoreVisual = { true },
                    restoreFeature = { throw cause },
                    rollbackVisual = { false },
                    rollbackFeature = { throw IllegalStateException(TEST_FEATURE_ROLLBACK) },
                )
            }
        assertTrue(thrown === cause)
        assertEquals(1, thrown.suppressed.size)
        val rollbackFailure = thrown.suppressed[0]
        assertTrue(rollbackFailure.message!!.contains(TEST_FEATURE_ROLLBACK))
        assertEquals(1, rollbackFailure.suppressed.size)
        assertTrue(rollbackFailure.suppressed[0].message!!.contains("visual"))
    }

    @Test
    fun cancellationDuringRollbackTakesPriorityWithoutHidingTheWriteError() {
        val cause = IOException(TEST_FEATURE_WRITE)
        val cancellation = CancellationException("cancelled during visual rollback")
        val thrown =
            assertThrows(CancellationException::class.java) {
                restoreBackupStores(
                    restoreVisual = { true },
                    restoreFeature = { throw cause },
                    rollbackVisual = { throw cancellation },
                    rollbackFeature = { true },
                )
            }
        assertTrue(thrown === cancellation)
        assertEquals(1, thrown.suppressed.size)
        assertTrue(thrown.suppressed[0] === cause)
    }

    @Test
    fun cancellationIsPropagatedAfterCompensation() {
        val cancellation = CancellationException("cancelled")
        var restored = false
        val thrown =
            assertThrows(CancellationException::class.java) {
                restoreBackupStores(
                    restoreVisual = { true },
                    restoreFeature = { throw cancellation },
                    rollbackVisual = { restored = true; true },
                    rollbackFeature = { true },
                )
            }
        assertTrue(thrown === cancellation)
        assertTrue(restored)
    }
}
