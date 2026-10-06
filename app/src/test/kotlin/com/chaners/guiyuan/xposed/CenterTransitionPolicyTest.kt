package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CenterTransitionTest {
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

        assertFalse(CenterTransition.shouldAnimate(fourG, fiveG))
        assertFalse(CenterTransition.shouldAnimate(fiveG, fiveGa))
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

        assertFalse(CenterTransition.shouldAnimate(weak, strongNoInternet))
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

        assertTrue(CenterTransition.shouldAnimate(mobile, wifi))
        assertTrue(CenterTransition.shouldAnimate(wifi, CenterIndicator.Airplane))
        assertTrue(CenterTransition.shouldAnimate(CenterIndicator.Airplane, CenterIndicator.Empty))
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
            CenterTransition.Decision.KEEP,
            CenterTransition.decide(
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
            CenterTransition.Decision.SNAP,
            CenterTransition.decide(
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
            CenterTransition.Decision.SNAP,
            CenterTransition.decide(
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
            CenterTransition.Family.NO_SIM,
            CenterTransition.family(noSim),
        )
        assertTrue(
            CenterTransition.shouldAnimate(
                CenterIndicator.Airplane,
                noSim,
            ),
        )
    }

}
