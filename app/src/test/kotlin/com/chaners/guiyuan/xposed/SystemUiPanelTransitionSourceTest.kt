package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemUiPanelTransitionSourceTest {
    @Test
    fun runtimeCallbackFailureIsContainedAndReported() {
        var reported: Throwable? = null
        val completed =
            SystemUiPanelTransitionSource.dispatchRuntimeCallback(
                callback = { error("callback-failure") },
                onFailure = { reported = it },
            )

        assertFalse(completed)
        assertEquals("callback-failure", reported?.message)
    }

    @Test
    fun runtimeCallbackFailureHandlerCannotEscapeTheHookBoundary() {
        val completed =
            SystemUiPanelTransitionSource.dispatchRuntimeCallback(
                callback = { error("callback-failure") },
                onFailure = { error("failure-handler-failure") },
            )

        assertFalse(completed)
    }

    @Test
    fun nativeFractionPreservesFiniteHyperOsPayload() {
        assertEquals(-0.2f, SystemUiPanelTransitionSource.nativeFraction(-0.2f))
        assertEquals(0.5f, SystemUiPanelTransitionSource.nativeFraction(0.5f))
        assertEquals(1.4f, SystemUiPanelTransitionSource.nativeFraction(1.4f))
        assertNull(SystemUiPanelTransitionSource.nativeFraction(Float.NaN))
        assertNull(SystemUiPanelTransitionSource.nativeFraction(Float.POSITIVE_INFINITY))
    }

    @Test
    fun controlCenterHomeEligibilityRequiresNativeInvisibleSemantics() {
        assertEquals(true, SystemUiPanelTransitionSource.controlCenterAllowsHome(false))
        assertEquals(false, SystemUiPanelTransitionSource.controlCenterAllowsHome(true))
        assertEquals(false, SystemUiPanelTransitionSource.controlCenterAllowsHome(null))
    }

    @Test
    fun controlCenterEligibilitySnapshotCanSeedHotReloadGeneration() {
        SystemUiPanelTransitionSource.resetRuntimeState()
        assertNull(SystemUiPanelTransitionSource.currentControlCenterHomeEligibility())

        SystemUiPanelTransitionSource.restoreControlCenterHomeEligibility(false)
        assertEquals(false, SystemUiPanelTransitionSource.currentControlCenterHomeEligibility())

        SystemUiPanelTransitionSource.restoreControlCenterHomeEligibility(true)
        assertEquals(true, SystemUiPanelTransitionSource.currentControlCenterHomeEligibility())

        // A v5 or older payload has no Control Center field; do not erase the
        // successfully installed generation's current/bootstrap eligibility.
        SystemUiPanelTransitionSource.restoreControlCenterHomeEligibility(null)
        assertEquals(true, SystemUiPanelTransitionSource.currentControlCenterHomeEligibility())
    }

    @Test
    fun runtimeHookCountIncludesOnlyNativeReadAndFakeLifecycleHooks() {
        assertEquals(4, SystemUiPanelTransitionSource.expectedHookCount(false))
        assertEquals(4, SystemUiPanelTransitionSource.expectedHookCount(true))
    }

    @Test
    fun appearanceDiagnosticPreservesNativeBooleanPayloadWithoutInterpretation() {
        assertEquals(
            "controlCenterAppearance first=true second=false expanding=unknown " +
                "addBatteryIsland=unknown batteryWidthDiff=unknown " +
                "fakePresentation=unknown readOnly=true nativeGeometryWrites=0",
            SystemUiPanelTransitionSource.appearanceDiagnostic(
                first = true,
                second = false,
                snapshot = null,
            ),
        )
    }

    @Test
    fun appearanceDiagnosticIncludesTopLevelFakePresentationSnapshot() {
        val fake =
            SystemUiPanelTransitionSource.ControlCenterFakePresentationSnapshot(
                rootClassName =
                    "com.android.systemui.controlcenter.phone.widget.ControlCenterFakeStatusIcons",
                rootVisibility = 0,
                rootAlpha = 1f,
                rootWidth = 1440,
                rootHeight = 108,
                statusBarAreaClassName =
                    "com.android.systemui.statusbar.views.MiuiStatusBatteryContainer",
                statusBarAreaVisibility = 0,
                statusBarAreaAlpha = 1f,
                statusBarAreaWidth = 587,
                statusBarAreaHeight = 108,
            )
        assertEquals(
            "controlCenterAppearance first=true second=true expanding=unknown " +
                "addBatteryIsland=unknown batteryWidthDiff=unknown " +
                "fakePresentation={root=" +
                "com.android.systemui.controlcenter.phone.widget.ControlCenterFakeStatusIcons" +
                "(v=0,a=1.0,w=1440,h=108),statusBarArea=" +
                "com.android.systemui.statusbar.views.MiuiStatusBatteryContainer" +
                "(v=0,a=1.0,w=587,h=108)} readOnly=true nativeGeometryWrites=0",
            SystemUiPanelTransitionSource.appearanceDiagnostic(
                first = true,
                second = true,
                snapshot = null,
                fakePresentation = fake,
            ),
        )
    }

    @Test
    fun controlAnchorProbeOnlyUsesTransitionBoundaryBuckets() {
        assertEquals(true, SystemUiPanelTransitionSource.isBoundaryDiagnosticBucket(0))
        assertEquals(true, SystemUiPanelTransitionSource.isBoundaryDiagnosticBucket(1))
        assertEquals(false, SystemUiPanelTransitionSource.isBoundaryDiagnosticBucket(2))
        assertEquals(false, SystemUiPanelTransitionSource.isBoundaryDiagnosticBucket(6))
        assertEquals(true, SystemUiPanelTransitionSource.isBoundaryDiagnosticBucket(7))
        assertEquals(true, SystemUiPanelTransitionSource.isBoundaryDiagnosticBucket(8))
        assertEquals(false, SystemUiPanelTransitionSource.isBoundaryDiagnosticBucket(null))
    }

    @Test
    fun homeMotionSnapshotKeepsBatteryWrapperAndBatteryDistinct() {
        val snapshot =
            SystemUiIslandMotionSource.OwnerSnapshot(
                views =
                    mapOf(
                        "mBatteryContainer" to
                            SystemUiIslandMotionSource.MotionViewSnapshot(
                                className = "FrameLayout",
                                screenX = 1242,
                                width = 105,
                                translationX = 0f,
                                alpha = 1f,
                                visibility = 0,
                            ),
                        "mBatteryView" to
                            SystemUiIslandMotionSource.MotionViewSnapshot(
                                className = "MiuiBatteryMeterView",
                                screenX = 1242,
                                width = 105,
                                translationX = 0f,
                                alpha = 1f,
                                visibility = 0,
                            ),
                    ),
            )

        assertEquals(
            "{mBatteryContainer=FrameLayout(x=1242,w=105,tx=0.0,a=1.0,v=0)," +
                "mBatteryView=MiuiBatteryMeterView(x=1242,w=105,tx=0.0,a=1.0,v=0)}",
            snapshot.summary,
        )
    }

    @Test
    fun diagnosticsUseBoundedExpansionBuckets() {
        assertEquals(0, SystemUiPanelTransitionSource.diagnosticBucket(0f))
        assertEquals(1, SystemUiPanelTransitionSource.diagnosticBucket(0.125f))
        assertEquals(4, SystemUiPanelTransitionSource.diagnosticBucket(0.5f))
        assertEquals(7, SystemUiPanelTransitionSource.diagnosticBucket(0.99f))
        assertEquals(8, SystemUiPanelTransitionSource.diagnosticBucket(1f))
        assertEquals(0, SystemUiPanelTransitionSource.diagnosticBucket(-0.2f))
        assertEquals(8, SystemUiPanelTransitionSource.diagnosticBucket(1.4f))
        assertNull(SystemUiPanelTransitionSource.diagnosticBucket(null))
    }

    @Test
    fun transitionMatrixUsesRawNativeExpansionProgress() {
        assertEquals(0f, CombinedStatusControlCenterTransitionOwner.Policy.geometryProgress(0f))
        assertEquals(
            0.41f,
            CombinedStatusControlCenterTransitionOwner.Policy.geometryProgress(0.41f),
            0.0001f,
        )
        assertEquals(
            0.82f,
            CombinedStatusControlCenterTransitionOwner.Policy.geometryProgress(0.82f),
            0.0001f,
        )
        assertEquals(1f, CombinedStatusControlCenterTransitionOwner.Policy.geometryProgress(1f))
        assertEquals(0f, CombinedStatusControlCenterTransitionOwner.Policy.geometryProgress(-0.2f))
        assertEquals(1f, CombinedStatusControlCenterTransitionOwner.Policy.geometryProgress(1.4f))
    }

    @Test
    fun transitionComponentGeometryFollowsLocalBoundsWithoutAnimationCoordinateConstants() {
        val parent = floatArrayOf(100f, 200f, 120f, 0f, 0f, 120f)
        val bounds =
            CombinedStatusPainter.TransitionBounds(
                left = 30f,
                top = 40f,
                right = 90f,
                bottom = 80f,
            )
        val component =
            requireNotNull(
                CombinedStatusControlCenterTransitionOwner.Policy.componentGeometry(
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
    fun transitionMotionAndMobileMorphUseNativeExpansion() {
        assertEquals(0f, CombinedStatusControlCenterTransitionOwner.Policy.motionProgress(0f))
        assertEquals(
            0.41f,
            CombinedStatusControlCenterTransitionOwner.Policy.motionProgress(0.41f),
            0.0001f,
        )
        assertEquals(
            0.82f,
            CombinedStatusControlCenterTransitionOwner.Policy.motionProgress(0.82f),
            0.0001f,
        )
        assertEquals(1f, CombinedStatusControlCenterTransitionOwner.Policy.motionProgress(1f))

        assertEquals(
            0f,
            CombinedStatusControlCenterTransitionOwner.Policy.mobileSignalShapeProgress(0f),
            0.0001f,
        )
        assertEquals(
            0.25f,
            CombinedStatusControlCenterTransitionOwner.Policy.mobileSignalShapeProgress(0.5f),
            0.0001f,
        )
        assertEquals(
            1f,
            CombinedStatusControlCenterTransitionOwner.Policy.mobileSignalShapeProgress(1f),
            0.0001f,
        )
    }

    @Test
    fun transitionReservationExpandsOnlyWhenSemanticSpanLeavesCompactBoundary() {
        val spans =
            listOf(
                CombinedStatusControlCenterTransitionOwner.Policy.ReservationSpan(
                    sourceLeft = -22f,
                    sourceRight = -12f,
                    targetLeft = -145f,
                    targetRight = -110f,
                ),
                CombinedStatusControlCenterTransitionOwner.Policy.ReservationSpan(
                    sourceLeft = -44f,
                    sourceRight = -32f,
                    targetLeft = -96f,
                    targetRight = -62f,
                ),
            )

        assertEquals(
            105,
            CombinedStatusControlCenterTransitionOwner.Policy.resolveReservationWidth(
                compactWidthPx = 105,
                spans = spans,
                progress = 0f,
            ),
        )
        assertEquals(
            105,
            CombinedStatusControlCenterTransitionOwner.Policy.resolveReservationWidth(
                compactWidthPx = 105,
                spans = spans,
                progress = 0.5f,
            ),
        )
        assertEquals(
            145,
            CombinedStatusControlCenterTransitionOwner.Policy.resolveReservationWidth(
                compactWidthPx = 105,
                spans = spans,
                progress = 1f,
            ),
        )
    }

    @Test
    fun mobileSignalMorphRowsDotsBeforeGrowingBars() {
        assertEquals(
            0f,
            CombinedStatusPainter.MobileSignalMorphPolicy.rowProgress(0f),
            0.0001f,
        )
        assertEquals(
            1f,
            CombinedStatusPainter.MobileSignalMorphPolicy.rowProgress(0.5f),
            0.0001f,
        )
        assertEquals(
            0f,
            CombinedStatusPainter.MobileSignalMorphPolicy.barProgress(0.5f),
            0.0001f,
        )
        assertEquals(
            1f,
            CombinedStatusPainter.MobileSignalMorphPolicy.barProgress(1f),
            0.0001f,
        )
    }

    @Test
    fun mobileSignalMorphKeepsBarGrowthOutUntilRowPhaseCompletes() {
        assertEquals(
            0f,
            CombinedStatusPainter.MobileSignalMorphPolicy.barProgress(0.25f),
            0.0001f,
        )
        assertEquals(
            0.5f,
            CombinedStatusPainter.MobileSignalMorphPolicy.rowProgress(0.25f),
            0.0001f,
        )
        assertEquals(
            0.5f,
            CombinedStatusPainter.MobileSignalMorphPolicy.barProgress(0.75f),
            0.0001f,
        )
    }

    @Test
    fun mobileSignalMorphExpandsBothWaysWhileKeepingOneSharedBottom() {
        val maxBarHeight = 54f
        val diameter = 6f
        val half =
            CombinedStatusPainter.MobileSignalMorphPolicy.sharedBottomExpansion(
                maxBarHeight = maxBarHeight,
                diameter = diameter,
                barProgress = 0.5f,
            )
        val full =
            CombinedStatusPainter.MobileSignalMorphPolicy.sharedBottomExpansion(
                maxBarHeight = maxBarHeight,
                diameter = diameter,
                barProgress = 1f,
            )

        assertTrue(half > 0f)
        assertEquals((maxBarHeight - diameter) / 4f, half, 0.0001f)
        assertEquals((maxBarHeight - diameter) / 2f, full, 0.0001f)
    }

    @Test
    fun mobileSignalMorphUsesNativeHeightOnlyAsACap() {
        val maxBarHeight =
            CombinedStatusPainter.MobileSignalMorphPolicy.targetMaxBarHeight(
                sourceBoundsHeight = 24f,
                diameter = 6f,
                targetHeightRatio = 3f,
            )
        val highest =
            CombinedStatusPainter.MobileSignalMorphPolicy.targetBarHeight(
                index = 3,
                maxBarHeight = maxBarHeight,
                diameter = 6f,
            )
        val lowest =
            CombinedStatusPainter.MobileSignalMorphPolicy.targetBarHeight(
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
    fun transitionDoesNotOwnANativeReleaseTimeline() {
        assertEquals(
            0.92f,
            CombinedStatusControlCenterTransitionOwner.Policy.geometryProgress(0.92f),
            0.0001f,
        )
        assertEquals(
            1f,
            CombinedStatusControlCenterTransitionOwner.Policy.geometryProgress(1f),
            0.0001f,
        )
    }

    @Test
    fun transitionSimilarityGeometryPreservesSourceAspectRatio() {
        val source = floatArrayOf(10f, 20f, 60f, 0f, 0f, 30f)
        val target = floatArrayOf(110f, 220f, 100f, 0f, 0f, 100f)
        val end =
            CombinedStatusControlCenterTransitionOwner.Policy.interpolateSimilarityGeometry(
                source = source,
                target = target,
                progress = 1f,
                scalePolicy = CombinedStatusPainter.TransitionScalePolicy.TARGET,
            )

        assertEquals(110f, end[0], 0.0001f)
        assertEquals(220f, end[1], 0.0001f)
        assertEquals(100f, end[2], 0.0001f)
        assertEquals(0f, end[3], 0.0001f)
        assertEquals(0f, end[4], 0.0001f)
        assertEquals(50f, end[5], 0.0001f)
    }

    @Test
    fun transitionMatrixInterpolatesAffineGeometryDeterministically() {
        val source = floatArrayOf(0f, 0f, 10f, 0f, 0f, 10f)
        val target = floatArrayOf(20f, 40f, 20f, 0f, 0f, 20f)
        val mid =
            CombinedStatusControlCenterTransitionOwner.Policy.interpolateGeometry(
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
    fun controlCenterSourceUsesHomeCarrierIdentityBeforeStructuralFallback() {
        assertEquals(
            CombinedStatusSourceScene.HOME,
            SystemUiPanelTransitionSource.classifyControlCenterSourceScene(
                homeIdentityMatches = true,
                structuralScene = CombinedStatusSourceScene.UNKNOWN,
            ),
        )
        assertEquals(
            CombinedStatusSourceScene.KEYGUARD,
            SystemUiPanelTransitionSource.classifyControlCenterSourceScene(
                homeIdentityMatches = false,
                structuralScene = CombinedStatusSourceScene.KEYGUARD,
            ),
        )
        assertEquals(
            CombinedStatusSourceScene.UNKNOWN,
            SystemUiPanelTransitionSource.classifyControlCenterSourceScene(
                homeIdentityMatches = false,
                structuralScene = CombinedStatusSourceScene.UNKNOWN,
            ),
        )
    }

    @Test
    fun controlCenterUpdateCarriesNativeAppearanceState() {
        val update =
            SystemUiPanelTransitionSource.Update(
                source = SystemUiPanelTransitionSource.Source.CONTROL_CENTER,
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
    fun controlCenterUpdateCarriesExactNativeBatteryIslandState() {
        val active =
            SystemUiPanelTransitionSource.Update(
                source = SystemUiPanelTransitionSource.Source.CONTROL_CENTER,
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
    fun controlCenterUpdateCarriesNativeSelectedSourceScene() {
        val update =
            SystemUiPanelTransitionSource.Update(
                source = SystemUiPanelTransitionSource.Source.CONTROL_CENTER,
                fraction = null,
                expanded = null,
                tracking = null,
                visible = true,
                controlCenterSourceScene = CombinedStatusSourceScene.KEYGUARD,
            )
        assertEquals(CombinedStatusSourceScene.KEYGUARD, update.controlCenterSourceScene)
    }
}
