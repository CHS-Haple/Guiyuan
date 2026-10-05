package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusScenePolicyTest {
class CombinedStatusScenePolicyTest {
    @Test
    fun everySceneHasExactlyOneCapability() {
        val capabilities = CombinedStatusScenePolicy.all()

        assertEquals(CombinedStatusScene.entries.size, capabilities.size)
        assertEquals(
            CombinedStatusScene.entries.toSet(),
            capabilities.map { it.scene }.toSet(),
        )
    }

    @Test
    fun homeStableUsesProjectedOverlayWithoutNativeSlotMutation() {
        val home = CombinedStatusScenePolicy.capability(CombinedStatusScene.HOME_STABLE)

        assertEquals(CombinedStatusRenderMode.PROJECTED, home.renderMode)
        assertEquals(CombinedStatusMotionOwnership.NONE, home.motionOwnership)
        assertEquals(CombinedStatusSceneEvidence.RUNTIME_VERIFIED, home.evidence)
    }

    @Test
    fun systemUiOwnedTransitionsDoNotRequestCombinedSlotMutation() {
        val systemUiOwned =
            CombinedStatusScenePolicy.all()
                .filter { it.motionOwnership == CombinedStatusMotionOwnership.SYSTEM_UI }

        assertTrue(systemUiOwned.isNotEmpty())
        systemUiOwned.forEach { capability ->
            assertTrue(capability.renderMode in CombinedStatusRenderMode.entries)
        }
    }

    @Test
    fun chargingIsNotModeledAsAnIndependentScene() {
        assertTrue(
            CombinedStatusScene.entries.none {
                it.name.contains("CHARG", ignoreCase = true)
            },
        )
    }

    @Test
    fun retainedTransitionSourceWitnessSurvivesPresentationHandoff() {
        assertTrue(
            CombinedStatusScenePolicy.retainedTransitionSourceWitnessAvailable(
                widthPx = 105,
                heightPx = 169,
                hostAttached = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.retainedTransitionSourceWitnessAvailable(
                widthPx = 0,
                heightPx = 169,
                hostAttached = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.retainedTransitionSourceWitnessAvailable(
                widthPx = 105,
                heightPx = 169,
                hostAttached = false,
            ),
        )
    }
}
