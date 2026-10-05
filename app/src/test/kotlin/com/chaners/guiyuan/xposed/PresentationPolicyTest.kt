package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PresentationPolicyTest {
    @Test
    fun incompleteCandidateKeepsLastStableModel() {
        val previous = model(centerIndicator = wifi(), mobileLevel = 4)

        val resolved =
            PresentationPolicy.resolveModel(
                previous = previous,
                candidate = null,
            )

        assertSame(previous, resolved)
    }

    @Test
    fun explicitHiddenOrUnavailableModelStillCommits() {
        val previous = model(centerIndicator = wifi(), mobileLevel = 4)
        val candidate = model(centerIndicator = CenterIndicator.Empty, mobileLevel = null)

        val resolved =
            PresentationPolicy.resolveModel(
                previous = previous,
                candidate = candidate,
            )

        assertEquals(candidate, resolved)
    }

    @Test
    fun initialIncompleteStateRemainsNotRenderable() {
        assertNull(
            PresentationPolicy.resolveModel(
                previous = null,
                candidate = null,
            ),
        )
    }

    @Test
    fun transparentTintDoesNotReplaceLastStableTint() {
        val previous = CombinedStatusTintState(0xe6ffffff.toInt())
        val transparent = CombinedStatusTintState(0x00000000)

        val resolved =
            PresentationPolicy.resolveTint(
                previous = previous,
                candidate = transparent,
            )

        assertSame(previous, resolved)
    }

    @Test
    fun initialTransparentTintIsRejected() {
        assertNull(
            PresentationPolicy.resolveTint(
                previous = null,
                candidate = CombinedStatusTintState(0x00000000),
            ),
        )
    }

    @Test
    fun nonTransparentTintCommitsImmediately() {
        val candidate = CombinedStatusTintState(0xbf000000.toInt())

        val resolved =
            PresentationPolicy.resolveTint(
                previous = CombinedStatusTintState(0xe6ffffff.toInt()),
                candidate = candidate,
            )

        assertEquals(candidate, resolved)
        assertTrue(PresentationPolicy.isValidTint(candidate))
    }

    private fun model(
        centerIndicator: CenterIndicator,
        mobileLevel: Int?,
    ) =
        RenderModel(
            batteryPercent = 83,
            charging = false,
            centerIndicator = centerIndicator,
            mobileLevel = mobileLevel,
            effectiveDataSubscriptionId = 4,
        )

    private fun wifi(): CenterIndicator =
        CenterIndicator.Wifi(
            segments = 3,
            internet = InternetState.VALIDATED,
        )
}
