package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenePolicyTest {
    @Test
    fun retainedSourceRequiresSizeAndAttachedHost() {
        assertTrue(
            SceneCcPolicy.retainedTransitionSourceWitnessAvailable(
                widthPx = 105,
                heightPx = 169,
                hostAttached = true,
            ),
        )
        assertFalse(
            SceneCcPolicy.retainedTransitionSourceWitnessAvailable(
                widthPx = 0,
                heightPx = 169,
                hostAttached = true,
            ),
        )
        assertFalse(
            SceneCcPolicy.retainedTransitionSourceWitnessAvailable(
                widthPx = 105,
                heightPx = 169,
                hostAttached = false,
            ),
        )
    }
}
