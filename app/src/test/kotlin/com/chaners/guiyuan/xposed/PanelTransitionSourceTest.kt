package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PanelTransitionSourceTest {
    @Test
    fun callbackFailureIsContained() {
        var reported: Throwable? = null
        val completed =
            PanelTransitionSource.dispatchRuntimeCallback(
                callback = { error("callback-failure") },
                onFailure = { reported = it },
            )

        assertFalse(completed)
        assertEquals("callback-failure", reported?.message)
    }

    @Test
    fun failureHandlerStaysInHook() {
        val completed =
            PanelTransitionSource.dispatchRuntimeCallback(
                callback = { error("callback-failure") },
                onFailure = { error("failure-handler-failure") },
            )

        assertFalse(completed)
    }

    @Test
    fun nativeFractionKeepsPayload() {
        assertEquals(-0.2f, PanelTransitionSource.nativeFraction(-0.2f))
        assertEquals(0.5f, PanelTransitionSource.nativeFraction(0.5f))
        assertEquals(1.4f, PanelTransitionSource.nativeFraction(1.4f))
        assertNull(PanelTransitionSource.nativeFraction(Float.NaN))
        assertNull(PanelTransitionSource.nativeFraction(Float.POSITIVE_INFINITY))
    }

    @Test
    fun ccHomeNeedsNativeInvisible() {
        assertEquals(true, PanelTransitionSource.controlCenterAllowsHome(false))
        assertEquals(false, PanelTransitionSource.controlCenterAllowsHome(true))
        assertEquals(false, PanelTransitionSource.controlCenterAllowsHome(null))
    }

    @Test
    fun ccEligibilitySeedsReload() {
        PanelTransitionSource.resetRuntimeState()
        assertNull(PanelTransitionSource.currentControlCenterHomeEligibility())

        PanelTransitionSource.restoreControlCenterHomeEligibility(false)
        assertEquals(false, PanelTransitionSource.currentControlCenterHomeEligibility())

        PanelTransitionSource.restoreControlCenterHomeEligibility(true)
        assertEquals(true, PanelTransitionSource.currentControlCenterHomeEligibility())

        // A v5 or older payload has no Control Center field; do not erase the
        // successfully installed generation's current/bootstrap eligibility.
        PanelTransitionSource.restoreControlCenterHomeEligibility(null)
        assertEquals(true, PanelTransitionSource.currentControlCenterHomeEligibility())
    }

    @Test
    fun hookCountCoversRuntimeHooks() {
        assertEquals(4, PanelTransitionSource.expectedHookCount(false))
        assertEquals(4, PanelTransitionSource.expectedHookCount(true))
    }

    @Test
    fun diagPolicyKeepsSemanticEdges() {
        assertTrue(
            PanelTransitionSource.DiagnosticPolicy.shouldReportPanelEvent(
                expandedChanged = false,
                trackingChanged = false,
                visibleChanged = true,
                sourceSceneChanged = false,
                batteryIslandChanged = false,
            ),
        )
        assertTrue(
            PanelTransitionSource.DiagnosticPolicy.shouldReportPanelEvent(
                expandedChanged = false,
                trackingChanged = false,
                visibleChanged = false,
                sourceSceneChanged = false,
                batteryIslandChanged = true,
            ),
        )
    }

    @Test
    fun matrixUsesNativeProgress() {
        assertEquals(0f, ControlCenterTransition.geometryProgress(0f))
        assertEquals(
            0.41f,
            ControlCenterTransition.geometryProgress(0.41f),
            0.0001f,
        )
        assertEquals(
            0.82f,
            ControlCenterTransition.geometryProgress(0.82f),
            0.0001f,
        )
        assertEquals(1f, ControlCenterTransition.geometryProgress(1f))
        assertEquals(0f, ControlCenterTransition.geometryProgress(-0.2f))
        assertEquals(1f, ControlCenterTransition.geometryProgress(1.4f))
    }

    @Test
    fun componentGeometryUsesLocalBounds() {
        val parent = floatArrayOf(100f, 200f, 120f, 0f, 0f, 120f)
        val bounds =
            StatusPainter.TransitionBounds(
                left = 30f,
                top = 40f,
                right = 90f,
                bottom = 80f,
            )
        val component =
            requireNotNull(
                ControlCenterTransition.componentGeometry(
                    parentGeometry = parent,
                    parentWidth = 120,
                    parentHeight = 120,
                    bounds = bounds,
                ),
            )
        assertEquals(100f, component[0], 0.0001f)
        assertEquals(200f, component[1], 0.0001f)
        assertEquals(60f, component[2], 0.0001f)
        assertEquals(0f, component[3], 0.0001f)
        assertEquals(0f, component[4], 0.0001f)
        assertEquals(40f, component[5], 0.0001f)
    }

    @Test
    fun motionUsesNativeExpansion() {
        assertEquals(0f, ControlCenterTransition.motionProgress(0f))
        assertEquals(
            0.41f,
            ControlCenterTransition.motionProgress(0.41f),
            0.0001f,
        )
        assertEquals(
            0.82f,
            ControlCenterTransition.motionProgress(0.82f),
            0.0001f,
        )
        assertEquals(1f, ControlCenterTransition.motionProgress(1f))

        assertEquals(
            0f,
            ControlCenterTransition.mobileSignalShapeProgress(0f),
            0.0001f,
        )
        assertEquals(
            0.25f,
            ControlCenterTransition.mobileSignalShapeProgress(0.5f),
            0.0001f,
        )
        assertEquals(
            1f,
            ControlCenterTransition.mobileSignalShapeProgress(1f),
            0.0001f,
        )
    }

    @Test
    fun reservationExpandsPastCompact() {
        val spans =
            listOf(
                ControlCenterTransition.ReservationSpan(
                    sourceLeft = -22f,
                    sourceRight = -12f,
                    targetLeft = -145f,
                    targetRight = -110f,
                ),
                ControlCenterTransition.ReservationSpan(
                    sourceLeft = -44f,
                    sourceRight = -32f,
                    targetLeft = -96f,
                    targetRight = -62f,
                ),
            )

        assertEquals(
            105,
            ControlCenterTransition.resolveReservationWidth(
                compactWidthPx = 105,
                spans = spans,
                progress = 0f,
            ),
        )
        assertEquals(
            105,
            ControlCenterTransition.resolveReservationWidth(
                compactWidthPx = 105,
                spans = spans,
                progress = 0.5f,
            ),
        )
        assertEquals(
            145,
            ControlCenterTransition.resolveReservationWidth(
                compactWidthPx = 105,
                spans = spans,
                progress = 1f,
            ),
        )
    }

    @Test
    fun mobileMorphRowsDotsFirst() {
        assertEquals(
            0f,
            StatusPainter.MobileSignalMorphPolicy.rowProgress(0f),
            0.0001f,
        )
        assertEquals(
            1f,
            StatusPainter.MobileSignalMorphPolicy.rowProgress(0.5f),
            0.0001f,
        )
        assertEquals(
            0f,
            StatusPainter.MobileSignalMorphPolicy.barProgress(0.5f),
            0.0001f,
        )
        assertEquals(
            1f,
            StatusPainter.MobileSignalMorphPolicy.barProgress(1f),
            0.0001f,
        )
    }

    @Test
    fun mobileMorphDelaysBarGrowth() {
        assertEquals(
            0f,
            StatusPainter.MobileSignalMorphPolicy.barProgress(0.25f),
            0.0001f,
        )
        assertEquals(
            0.5f,
            StatusPainter.MobileSignalMorphPolicy.rowProgress(0.25f),
            0.0001f,
        )
        assertEquals(
            0.5f,
            StatusPainter.MobileSignalMorphPolicy.barProgress(0.75f),
            0.0001f,
        )
    }

    @Test
    fun mobileMorphKeepsSharedBottom() {
        val maxBarHeight = 54f
        val diameter = 6f
        val half =
            StatusPainter.MobileSignalMorphPolicy.sharedBottomExpansion(
                maxBarHeight = maxBarHeight,
                diameter = diameter,
                barProgress = 0.5f,
            )
        val full =
            StatusPainter.MobileSignalMorphPolicy.sharedBottomExpansion(
                maxBarHeight = maxBarHeight,
                diameter = diameter,
                barProgress = 1f,
            )

        assertTrue(half > 0f)
        assertEquals((maxBarHeight - diameter) / 4f, half, 0.0001f)
        assertEquals((maxBarHeight - diameter) / 2f, full, 0.0001f)
    }

    @Test
    fun mobileMorphUsesNativeHeightCap() {
        val maxBarHeight =
            StatusPainter.MobileSignalMorphPolicy.targetMaxBarHeight(
                sourceBoundsHeight = 24f,
                diameter = 6f,
                targetHeightRatio = 3f,
            )
        val highest =
            StatusPainter.MobileSignalMorphPolicy.targetBarHeight(
                index = 3,
                maxBarHeight = maxBarHeight,
                diameter = 6f,
            )
        val lowest =
            StatusPainter.MobileSignalMorphPolicy.targetBarHeight(
                index = 0,
                maxBarHeight = maxBarHeight,
                diameter = 6f,
            )

        assertEquals(54f, maxBarHeight, 0.0001f)
        assertEquals(maxBarHeight, highest, 0.0001f)
        assertTrue(lowest < highest)
        assertTrue(highest < 24f * 3f)
    }

    @Test
    fun transitionDoesNotOwnReleaseTime() {
        assertEquals(
            0.92f,
            ControlCenterTransition.geometryProgress(0.92f),
            0.0001f,
        )
        assertEquals(
            1f,
            ControlCenterTransition.geometryProgress(1f),
            0.0001f,
        )
    }

    @Test
    fun similarityKeepsAspectRatio() {
        val source = floatArrayOf(10f, 20f, 60f, 0f, 0f, 30f)
        val target = floatArrayOf(110f, 220f, 100f, 0f, 0f, 100f)
        val end =
            ControlCenterTransition.interpolateSimilarityGeometry(
                source = source,
                target = target,
                progress = 1f,
                scalePolicy = StatusPainter.TransitionScalePolicy.TARGET,
            )

        assertEquals(110f, end[0], 0.0001f)
        assertEquals(220f, end[1], 0.0001f)
        assertEquals(100f, end[2], 0.0001f)
        assertEquals(0f, end[3], 0.0001f)
        assertEquals(0f, end[4], 0.0001f)
        assertEquals(50f, end[5], 0.0001f)
    }

    @Test
    fun matrixInterpolationIsDeterministic() {
        val source = floatArrayOf(0f, 0f, 10f, 0f, 0f, 10f)
        val target = floatArrayOf(20f, 40f, 20f, 0f, 0f, 20f)
        val mid =
            ControlCenterTransition.interpolateGeometry(
                source,
                target,
                0.5f,
            )
        assertEquals(10f, mid[0])
        assertEquals(20f, mid[1])
        assertEquals(15f, mid[2])
        assertEquals(0f, mid[3])
        assertEquals(0f, mid[4])
        assertEquals(15f, mid[5])
    }

    @Test
    fun ccSourcePrefersHomeIdentity() {
        assertEquals(
            SourceScene.HOME,
            PanelTransitionSource.classifyControlCenterSourceScene(
                homeIdentityMatches = true,
                structuralScene = SourceScene.UNKNOWN,
            ),
        )
        assertEquals(
            SourceScene.KEYGUARD,
            PanelTransitionSource.classifyControlCenterSourceScene(
                homeIdentityMatches = false,
                structuralScene = SourceScene.KEYGUARD,
            ),
        )
        assertEquals(
            SourceScene.UNKNOWN,
            PanelTransitionSource.classifyControlCenterSourceScene(
                homeIdentityMatches = false,
                structuralScene = SourceScene.UNKNOWN,
            ),
        )
    }

    @Test
    fun ccUpdateCarriesAppearance() {
        val update =
            PanelTransitionSource.Update(
                source = PanelTransitionSource.Source.CONTROL_CENTER,
                fraction = null,
                expanded = null,
                tracking = null,
                visible = null,
                controlCenterAppearance = true,
                controlCenterAppearanceAnimated = true,
            )
        assertEquals(true, update.controlCenterAppearance)
        assertEquals(true, update.controlCenterAppearanceAnimated)
    }


    @Test
    fun ccUpdateCarriesIslandState() {
        val active =
            PanelTransitionSource.Update(
                source = PanelTransitionSource.Source.CONTROL_CENTER,
                fraction = 0.5f,
                expanded = null,
                tracking = null,
                visible = null,
                controlCenterBatteryIslandActive = true,
            )
        val ordinary =
            active.copy(controlCenterBatteryIslandActive = false)

        assertEquals(true, active.controlCenterBatteryIslandActive)
        assertEquals(false, ordinary.controlCenterBatteryIslandActive)
    }

    @Test
    fun ccUpdateCarriesSourceScene() {
        val update =
            PanelTransitionSource.Update(
                source = PanelTransitionSource.Source.CONTROL_CENTER,
                fraction = null,
                expanded = null,
                tracking = null,
                visible = true,
                controlCenterSourceScene = SourceScene.KEYGUARD,
            )
        assertEquals(SourceScene.KEYGUARD, update.controlCenterSourceScene)
    }
}
