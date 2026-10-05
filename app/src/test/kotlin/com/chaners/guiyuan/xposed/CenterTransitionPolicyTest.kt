package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CenterTransitionPolicyTest {
    @Test
    fun mobileTypeChangesStayInTheSamePresentationFamily() {
        val fourG =
            CenterIndicator.MobileType(
                label = "4G",
                enhanced = false,
                internet = InternetState.VALIDATED,
            )
        val fiveG =
            CenterIndicator.MobileType(
                label = "5G",
                enhanced = false,
                internet = InternetState.VALIDATED,
            )
        val fiveGa =
            CenterIndicator.MobileType(
                label = "5GA",
                enhanced = false,
                internet = InternetState.VALIDATED,
            )

        assertFalse(CenterTransitionPolicy.shouldAnimate(fourG, fiveG))
        assertFalse(CenterTransitionPolicy.shouldAnimate(fiveG, fiveGa))
    }

    @Test
    fun wifiDetailChangesStayInTheSamePresentationFamily() {
        val weak =
            CenterIndicator.Wifi(
                segments = 1,
                internet = InternetState.VALIDATED,
            )
        val strongNoInternet =
            CenterIndicator.Wifi(
                segments = 3,
                internet = InternetState.NO_INTERNET,
            )

        assertFalse(CenterTransitionPolicy.shouldAnimate(weak, strongNoInternet))
    }

    @Test
    fun crossFamilyPresentationChangesAnimate() {
        val mobile =
            CenterIndicator.MobileType(
                label = "5G",
                enhanced = false,
                internet = InternetState.VALIDATED,
            )
        val wifi =
            CenterIndicator.Wifi(
                segments = 3,
                internet = InternetState.VALIDATED,
            )

        assertTrue(CenterTransitionPolicy.shouldAnimate(mobile, wifi))
        assertTrue(CenterTransitionPolicy.shouldAnimate(wifi, CenterIndicator.Airplane))
        assertTrue(CenterTransitionPolicy.shouldAnimate(CenterIndicator.Airplane, CenterIndicator.Empty))
    }

    @Test
    fun sameTargetFamilyUpdateKeepsRunningTransition() {
        val source =
            CenterIndicator.MobileType(
                label = "5G",
                enhanced = false,
                internet = InternetState.VALIDATED,
            )
        val previousTarget =
            CenterIndicator.Wifi(
                segments = 1,
                internet = InternetState.VALIDATED,
            )
        val currentTarget =
            CenterIndicator.Wifi(
                segments = 3,
                internet = InternetState.NO_INTERNET,
            )

        assertEquals(
            CenterTransitionPolicy.Decision.KEEP,
            CenterTransitionPolicy.decide(
                previous = previousTarget,
                current = currentTarget,
                activeSource = source,
                transitionRunning = true,
            ),
        )
    }

    @Test
    fun reversalToActiveSourceSnapsInsteadOfStartingAnotherAnimation() {
        val source =
            CenterIndicator.MobileType(
                label = "5G",
                enhanced = false,
                internet = InternetState.VALIDATED,
            )
        val target =
            CenterIndicator.Wifi(
                segments = 3,
                internet = InternetState.VALIDATED,
            )

        assertEquals(
            CenterTransitionPolicy.Decision.SNAP,
            CenterTransitionPolicy.decide(
                previous = target,
                current = source,
                activeSource = source,
                transitionRunning = true,
            ),
        )
    }

    @Test
    fun thirdFamilyInterruptionSnapsToLatestPresentation() {
        val source =
            CenterIndicator.MobileType(
                label = "5G",
                enhanced = false,
                internet = InternetState.VALIDATED,
            )
        val target =
            CenterIndicator.Wifi(
                segments = 3,
                internet = InternetState.VALIDATED,
            )

        assertEquals(
            CenterTransitionPolicy.Decision.SNAP,
            CenterTransitionPolicy.decide(
                previous = target,
                current = CenterIndicator.Airplane,
                activeSource = source,
                transitionRunning = true,
            ),
        )
    }
    @Test
    fun noSimIsItsOwnNativeCenterFamily() {
        val noSim =
            CenterIndicator.NoSim(
                PresentationStore.NativeIconResource(
                    packageName = "com.android.systemui",
                    resourceId = 42,
                ),
            )

        assertEquals(
            CenterTransitionPolicy.Family.NO_SIM,
            CenterTransitionPolicy.family(noSim),
        )
        assertTrue(
            CenterTransitionPolicy.shouldAnimate(
                CenterIndicator.Airplane,
                noSim,
            ),
        )
    }

}
