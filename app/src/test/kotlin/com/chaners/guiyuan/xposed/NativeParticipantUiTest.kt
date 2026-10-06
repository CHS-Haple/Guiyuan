package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeParticipantPresentationTest {
    @Test
    fun zeroSlotBridgeAcceptsPreservedBatteryGeometry() {
        assertTrue(
            NativeParticipantUi.isZeroSlotHandoffReady(
                rootMeasuredWidth = 0,
                rootMeasuredHeight = 108,
                renderMeasuredWidth = 105,
                renderMeasuredHeight = 108,
                expectedVisualWidth = 105,
                expectedVisualHeight = 108,
                parentClipsChildren = false,
                rootScreenX = 1242,
                slotAnchorScreenX = 1242,
                renderLeft = 0,
                renderRight = 105,
            ),
        )
    }

    @Test
    fun zeroSlotBridgeRejectsDuplicateLayoutWidth() {
        assertFalse(
            NativeParticipantUi.isZeroSlotHandoffReady(
                rootMeasuredWidth = 105,
                rootMeasuredHeight = 108,
                renderMeasuredWidth = 105,
                renderMeasuredHeight = 108,
                expectedVisualWidth = 105,
                expectedVisualHeight = 108,
                parentClipsChildren = false,
                rootScreenX = 1242,
                slotAnchorScreenX = 1242,
                renderLeft = 0,
                renderRight = 105,
            ),
        )
    }

    @Test
    fun zeroSlotBridgeRejectsCenteredChildOverflow() {
        assertFalse(
            NativeParticipantUi.isZeroSlotHandoffReady(
                rootMeasuredWidth = 0,
                rootMeasuredHeight = 108,
                renderMeasuredWidth = 105,
                renderMeasuredHeight = 108,
                expectedVisualWidth = 105,
                expectedVisualHeight = 108,
                parentClipsChildren = false,
                rootScreenX = 1242,
                slotAnchorScreenX = 1242,
                renderLeft = -52,
                renderRight = 53,
            ),
        )
    }

    @Test
    fun zeroSlotBridgeRejectsClippedOrMisalignedOverflow() {
        assertFalse(
            NativeParticipantUi.isZeroSlotHandoffReady(
                rootMeasuredWidth = 0,
                rootMeasuredHeight = 108,
                renderMeasuredWidth = 105,
                renderMeasuredHeight = 108,
                expectedVisualWidth = 105,
                expectedVisualHeight = 108,
                parentClipsChildren = true,
                rootScreenX = 1242,
                slotAnchorScreenX = 1242,
                renderLeft = 0,
                renderRight = 105,
            ),
        )
        assertFalse(
            NativeParticipantUi.isZeroSlotHandoffReady(
                rootMeasuredWidth = 0,
                rootMeasuredHeight = 108,
                renderMeasuredWidth = 105,
                renderMeasuredHeight = 108,
                expectedVisualWidth = 105,
                expectedVisualHeight = 108,
                parentClipsChildren = false,
                rootScreenX = 1238,
                slotAnchorScreenX = 1242,
                renderLeft = 0,
                renderRight = 105,
            ),
        )
    }
    @Test
    fun handoffModeAllowsVisibleHome() {
        assertEquals(
            NativeParticipantUi.HandoffMode.VISIBLE_HOME,
            NativeParticipantUi.resolveHandoffMode(
                surface = SceneSource.Surface.UNLOCKED_STATUS_BAR,
                rootShown = true,
            ),
        )
    }

    @Test
    fun handoffModePrearmsHiddenHomeParticipantOnKeyguard() {
        assertEquals(
            NativeParticipantUi.HandoffMode.PREARMED_KEYGUARD,
            NativeParticipantUi.resolveHandoffMode(
                surface = SceneSource.Surface.KEYGUARD,
                rootShown = false,
            ),
        )
    }

    @Test
    fun handoffModeDoesNotPrearmVisibleParticipantOnKeyguard() {
        assertEquals(
            NativeParticipantUi.HandoffMode.BLOCKED,
            NativeParticipantUi.resolveHandoffMode(
                surface = SceneSource.Surface.KEYGUARD,
                rootShown = true,
            ),
        )
    }

    @Test
    fun handoffModeFailsClosedForUnknownAndShadeLocked() {
        listOf(
            SceneSource.Surface.UNKNOWN,
            SceneSource.Surface.SHADE_LOCKED,
        ).forEach { surface ->
            assertEquals(
                NativeParticipantUi.HandoffMode.BLOCKED,
                NativeParticipantUi.resolveHandoffMode(
                    surface = surface,
                    rootShown = false,
                ),
            )
        }
    }


    @Test
    fun activeNativeSlotKeepsZeroWidthShellAlignedToBatterySlot() {
        assertTrue(
            NativeParticipantUi.isActiveSlotHandoffReady(
                rootLayoutWidth = 0,
                rootLayoutHeight = 108,
                renderMeasuredWidth = 105,
                renderMeasuredHeight = 108,
                expectedVisualWidth = 105,
                expectedVisualHeight = 108,
                parentClipsChildren = false,
                rootScreenX = 1242,
                slotAnchorScreenX = 1242,
                renderLeft = 0,
                renderRight = 105,
            ),
        )
    }

    @Test
    fun activeNativeSlotRejectsDuplicateShellOccupancy() {
        assertFalse(
            NativeParticipantUi.isActiveSlotHandoffReady(
                rootLayoutWidth = 105,
                rootLayoutHeight = 108,
                renderMeasuredWidth = 105,
                renderMeasuredHeight = 108,
                expectedVisualWidth = 105,
                expectedVisualHeight = 108,
                parentClipsChildren = false,
                rootScreenX = 1137,
                slotAnchorScreenX = 1242,
                renderLeft = 0,
                renderRight = 105,
            ),
        )
    }

    @Test
    fun activeNativeSlotRejectsAnchorOrVisualMismatch() {
        assertFalse(
            NativeParticipantUi.isActiveSlotHandoffReady(
                rootLayoutWidth = 0,
                rootLayoutHeight = 108,
                renderMeasuredWidth = 105,
                renderMeasuredHeight = 108,
                expectedVisualWidth = 105,
                expectedVisualHeight = 108,
                parentClipsChildren = false,
                rootScreenX = 1238,
                slotAnchorScreenX = 1242,
                renderLeft = 0,
                renderRight = 105,
            ),
        )
        assertFalse(
            NativeParticipantUi.isActiveSlotHandoffReady(
                rootLayoutWidth = 0,
                rootLayoutHeight = 108,
                renderMeasuredWidth = 135,
                renderMeasuredHeight = 108,
                expectedVisualWidth = 105,
                expectedVisualHeight = 108,
                parentClipsChildren = false,
                rootScreenX = 1242,
                slotAnchorScreenX = 1242,
                renderLeft = 0,
                renderRight = 135,
            ),
        )
    }

    @Test
    fun zeroOccupancyShellResolvesRealVisualBounds() {
        assertEquals(
            105,
            NativeParticipantUi.resolvePostLayoutVisualWidth(
                layoutWidth = 0,
                measuredWidth = 0,
                visualWidth = 105,
            ),
        )
    }

    @Test
    fun nativeBatteryHideClaimsOnlyReleasedSlotWidth() {
        assertEquals(
            0,
            NativeParticipantUi.resolveNativeSlotOccupancyWidth(
                nativeBatteryHidden = false,
                visualWidth = 105,
            ),
        )
        assertEquals(
            105,
            NativeParticipantUi.resolveNativeSlotOccupancyWidth(
                nativeBatteryHidden = true,
                visualWidth = 105,
            ),
        )
        assertEquals(
            null,
            NativeParticipantUi.resolveNativeSlotOccupancyWidth(
                nativeBatteryHidden = true,
                visualWidth = 0,
            ),
        )
    }

    @Test
    fun releasedBatterySlotAcceptsNativeMeasuredVisualWidth() {
        assertEquals(
            105,
            NativeParticipantUi.resolvePostLayoutVisualWidth(
                layoutWidth = 105,
                measuredWidth = 105,
                visualWidth = 105,
                nativeBatteryHidden = true,
            ),
        )
    }

    @Test
    fun visualBoundsRejectNonZeroOccupancyOrInvalidVisualWidth() {
        assertEquals(
            null,
            NativeParticipantUi.resolvePostLayoutVisualWidth(
                layoutWidth = 105,
                measuredWidth = 105,
                visualWidth = 105,
            ),
        )
        assertEquals(
            null,
            NativeParticipantUi.resolvePostLayoutVisualWidth(
                layoutWidth = 0,
                measuredWidth = 0,
                visualWidth = 0,
            ),
        )
    }

    @Test
    fun chargingPresentationDoesNotChangeStableSlotTranslation() {
        val stableBoundary = 478
        val chargingLiveBoundary = 448

        assertEquals(
            478f,
            NativeParticipantUi.resolveNativeSlotTranslationX(
                statusIconsWidth = stableBoundary,
                rootLeft = 0,
            ),
        )
        assertEquals(
            448f,
            NativeParticipantUi.resolveNativeSlotTranslationX(
                statusIconsWidth = chargingLiveBoundary,
                rootLeft = 0,
            ),
        )
        assertEquals(30, stableBoundary - chargingLiveBoundary)
    }

    @Test
    fun nativeSlotTranslationUsesStableStatusIconBoundary() {
        assertEquals(
            478f,
            NativeParticipantUi.resolveNativeSlotTranslationX(
                statusIconsWidth = 478,
                rootLeft = 0,
            ),
        )
        assertEquals(
            478f,
            NativeParticipantUi.resolveNativeSlotTranslationX(
                statusIconsWidth = 478,
                rootLeft = 0,
            ),
        )
        assertEquals(
            null,
            NativeParticipantUi.resolveNativeSlotTranslationX(
                statusIconsWidth = 0,
                rootLeft = 1,
            ),
        )
    }

    @Test
    fun activeHandoffUsesSlotAnchorInsteadOfEvictedBatteryContent() {
        assertTrue(
            NativeParticipantUi.isActiveSlotHandoffReady(
                rootLayoutWidth = 0,
                rootLayoutHeight = 108,
                renderMeasuredWidth = 105,
                renderMeasuredHeight = 108,
                expectedVisualWidth = 105,
                expectedVisualHeight = 108,
                parentClipsChildren = false,
                rootScreenX = 1257,
                slotAnchorScreenX = 1257,
                renderLeft = 0,
                renderRight = 105,
            ),
        )
        assertFalse(
            NativeParticipantUi.isActiveSlotHandoffReady(
                rootLayoutWidth = 0,
                rootLayoutHeight = 108,
                renderMeasuredWidth = 105,
                renderMeasuredHeight = 108,
                expectedVisualWidth = 105,
                expectedVisualHeight = 108,
                parentClipsChildren = false,
                rootScreenX = 1362,
                slotAnchorScreenX = 1257,
                renderLeft = 0,
                renderRight = 105,
            ),
        )
    }

    @Test
    fun nativeBindingTintBecomesSingleResolvedTintAuthority() {
        val merged =
            NativeParticipantUi.mergeNativeParticipantTint(
                batteryTint =
                    TintState(
                        appliedTint = 0xbf000000.toInt(),
                        statusIconTint = 0xff202020.toInt(),
                    ),
                nativeTint = 0xff303030.toInt(),
            )

        assertEquals(0xff303030.toInt(), merged.appliedTint)
        assertEquals(0xff303030.toInt(), merged.statusIconTint)
    }

    @Test
    fun nativeBindingTintDoesNotDependOnBatteryAnchor() {
        val merged =
            NativeParticipantUi.mergeNativeParticipantTint(
                batteryTint =
                    TintState(
                        appliedTint = 0xbf000000.toInt(),
                    ),
                nativeTint = 0xff303030.toInt(),
            )

        assertEquals(0xff303030.toInt(), merged.appliedTint)
        assertEquals(0xff303030.toInt(), merged.statusIconTint)
    }

    @Test
    fun missingNativeTintDropsLegacyStatusIconFallbackAndUsesBatteryAnchor() {
        val merged =
            NativeParticipantUi.mergeNativeParticipantTint(
                batteryTint =
                    TintState(
                        appliedTint = 0xbf101010.toInt(),
                        statusIconTint = 0xff202020.toInt(),
                    ),
                nativeTint = null,
            )

        assertEquals(0xbf101010.toInt(), merged.appliedTint)
        assertEquals(null, merged.statusIconTint)
    }

    @Test
    fun transparentNativeBindingTintFallsBackWithoutOverwritingAnchor() {
        val merged =
            NativeParticipantUi.mergeNativeParticipantTint(
                batteryTint =
                    TintState(
                        appliedTint = 0xbf000000.toInt(),
                    ),
                nativeTint = 0x00303030,
            )

        assertEquals(0xbf000000.toInt(), merged.appliedTint)
        assertEquals(null, merged.statusIconTint)
    }

    @Test
    fun masterSwitchBlocksHomeOverlayRegardlessOfControlCenterOrHandoffState() {
        assertFalse(
            HomeRenderSession.resolveOverlayVisible(
                featureEnabled = false,
                controlCenterAllowsHome = true,
                nativeHandoffActive = false,
            ),
        )
        assertFalse(
            HomeRenderSession.resolveOverlayVisible(
                featureEnabled = false,
                controlCenterAllowsHome = true,
                nativeHandoffActive = true,
            ),
        )
    }

    @Test
    fun controlCenterOwnershipBlocksHomeOverlay() {
        assertFalse(
            HomeRenderSession.resolveOverlayVisible(
                featureEnabled = true,
                controlCenterAllowsHome = false,
                nativeHandoffActive = false,
            ),
        )
    }

    @Test
    fun enabledMasterSwitchStillDefersToControlCenterAndNativeHandoff() {
        assertTrue(
            HomeRenderSession.resolveOverlayVisible(
                featureEnabled = true,
                controlCenterAllowsHome = true,
                nativeHandoffActive = false,
            ),
        )
        assertFalse(
            HomeRenderSession.resolveOverlayVisible(
                featureEnabled = true,
                controlCenterAllowsHome = true,
                nativeHandoffActive = true,
            ),
        )
    }


    @Test
    fun nativeIconStateShowsCombinedRendererAndHidesDot() {
        val visibility =
            NativeParticipantUi.resolveNativeContentVisibility(
                state = 7,
                iconState = 7,
                dotState = 8,
                hiddenState = 9,
            )

        assertEquals(android.view.View.VISIBLE, visibility?.renderVisibility)
        assertEquals(android.view.View.GONE, visibility?.dotVisibility)
    }

    @Test
    fun nativeDotStateUsesSystemDotWithoutCombinedRenderer() {
        val visibility =
            NativeParticipantUi.resolveNativeContentVisibility(
                state = 8,
                iconState = 7,
                dotState = 8,
                hiddenState = 9,
            )

        assertEquals(android.view.View.INVISIBLE, visibility?.renderVisibility)
        assertEquals(android.view.View.VISIBLE, visibility?.dotVisibility)
    }

    @Test
    fun nativeHiddenStateKeepsShellButDrawsNoCombinedContent() {
        val visibility =
            NativeParticipantUi.resolveNativeContentVisibility(
                state = 9,
                iconState = 7,
                dotState = 8,
                hiddenState = 9,
            )

        assertEquals(android.view.View.INVISIBLE, visibility?.renderVisibility)
        assertEquals(android.view.View.INVISIBLE, visibility?.dotVisibility)
    }

    @Test
    fun unknownNativeVisibleStateFailsClosed() {
        assertEquals(
            null,
            NativeParticipantUi.resolveNativeContentVisibility(
                state = 99,
                iconState = 7,
                dotState = 8,
                hiddenState = 9,
            ),
        )
    }


    @Test
    fun validatedMasterSwitchUsesNativeRemoveLifecycle() {
        assertEquals(
            false,
            NativeParticipantUi.resolveNativeFeatureRemoveFlag(
                featureEnabled = true,
                handoffValidated = true,
            ),
        )
        assertEquals(
            true,
            NativeParticipantUi.resolveNativeFeatureRemoveFlag(
                featureEnabled = false,
                handoffValidated = true,
            ),
        )
    }

    @Test
    fun unvalidatedMasterSwitchStaysOnBootstrapFallback() {
        assertEquals(
            null,
            NativeParticipantUi.resolveNativeFeatureRemoveFlag(
                featureEnabled = false,
                handoffValidated = false,
            ),
        )
    }

    @Test
    fun nativeVisibleStateNamesResolveWithoutAssumingNumericOrder() {
        val states =
            NativeParticipantUi.resolveNativeVisibilityStates { candidate ->
                when (candidate) {
                    2 -> "ICON"
                    4 -> "DOT"
                    7 -> "HIDDEN"
                    else -> "UNKNOWN"
                }
            }

        assertEquals(2, states?.icon)
        assertEquals(4, states?.dot)
        assertEquals(7, states?.hidden)
    }

    @Test
    fun missingNativeVisibleStateFailsClosed() {
        assertEquals(
            null,
            NativeParticipantUi.resolveNativeVisibilityStates { candidate ->
                when (candidate) {
                    0 -> "ICON"
                    1 -> "DOT"
                    else -> "UNKNOWN"
                }
            },
        )
    }
}