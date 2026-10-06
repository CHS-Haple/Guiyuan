package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenePolicyTest {
    @Test
    fun everySceneHasExactlyOneCapability() {
        val capabilities = ScenePolicy.all()

        assertEquals(StatusScene.entries.size, capabilities.size)
        assertEquals(
            StatusScene.entries.toSet(),
            capabilities.map { it.scene }.toSet(),
        )
    }

    @Test
    fun homeStableUsesProjectedOverlayWithoutNativeSlotMutation() {
        val home = ScenePolicy.capability(StatusScene.HOME_STABLE)

        assertEquals(RenderMode.PROJECTED, home.renderMode)
        assertEquals(MotionOwnership.NONE, home.motionOwnership)
        assertEquals(SceneEvidence.RUNTIME_VERIFIED, home.evidence)
    }

    @Test
    fun systemUiOwnedTransitionsDoNotRequestCombinedSlotMutation() {
        val systemUiOwned =
            ScenePolicy.all()
                .filter { it.motionOwnership == MotionOwnership.SYSTEM_UI }

        assertTrue(systemUiOwned.isNotEmpty())
        systemUiOwned.forEach { capability ->
            assertTrue(capability.renderMode in RenderMode.entries)
        }
    }

    @Test
    fun chargingIsNotModeledAsAnIndependentScene() {
        assertTrue(
            StatusScene.entries.none {
                it.name.contains("CHARG", ignoreCase = true)
            },
        )
    }

    @Test
    fun retainedTransitionSourceWitnessSurvivesPresentationHandoff() {
        assertTrue(
            ScenePolicy.hasRetainedSourceWitness(
                widthPx = 105,
                heightPx = 169,
                hostAttached = true,
            ),
        )
        assertFalse(
            ScenePolicy.hasRetainedSourceWitness(
                widthPx = 0,
                heightPx = 169,
                hostAttached = true,
            ),
        )
        assertFalse(
            ScenePolicy.hasRetainedSourceWitness(
                widthPx = 105,
                heightPx = 169,
                hostAttached = false,
            ),
        )
    }
}
