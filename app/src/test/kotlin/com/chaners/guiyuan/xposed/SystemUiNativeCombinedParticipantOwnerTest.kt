package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemUiNativeCombinedParticipantOwnerTest {
    @Test
    fun zeroSlotBridgeAcceptsPreservedBatteryGeometry() {
        assertTrue(
            SystemUiNativeCombinedParticipantOwner.isZeroSlotHandoffReady(
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
            SystemUiNativeCombinedParticipantOwner.isZeroSlotHandoffReady(
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
            SystemUiNativeCombinedParticipantOwner.isZeroSlotHandoffReady(
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
            SystemUiNativeCombinedParticipantOwner.isZeroSlotHandoffReady(
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
            SystemUiNativeCombinedParticipantOwner.isZeroSlotHandoffReady(
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
            SystemUiNativeCombinedParticipantOwner.HandoffMode.VISIBLE_HOME,
            SystemUiNativeCombinedParticipantOwner.resolveHandoffMode(
                surface = SysUiSceneSource.Surface.UNLOCKED_STATUS_BAR,
                rootShown = true,
            ),
        )
    }

    @Test
    fun handoffModePrearmsHiddenHomeParticipantOnKeyguard() {
        assertEquals(
            SystemUiNativeCombinedParticipantOwner.HandoffMode.PREARMED_KEYGUARD,
            SystemUiNativeCombinedParticipantOwner.resolveHandoffMode(
                surface = SysUiSceneSource.Surface.KEYGUARD,
                rootShown = false,
            ),
        )
    }

    @Test
    fun handoffModeDoesNotPrearmVisibleParticipantOnKeyguard() {
        assertEquals(
            SystemUiNativeCombinedParticipantOwner.HandoffMode.BLOCKED,
            SystemUiNativeCombinedParticipantOwner.resolveHandoffMode(
                surface = SysUiSceneSource.Surface.KEYGUARD,
                rootShown = true,
            ),
        )
    }

    @Test
    fun handoffModeFailsClosedForUnknownAndShadeLocked() {
        listOf(
            SysUiSceneSource.Surface.UNKNOWN,
            SysUiSceneSource.Surface.SHADE_LOCKED,
        ).forEach { surface ->
            assertEquals(
                SystemUiNativeCombinedParticipantOwner.HandoffMode.BLOCKED,
                SystemUiNativeCombinedParticipantOwner.resolveHandoffMode(
                    surface = surface,
                    rootShown = false,
                ),
            )
        }
    }


    @Test
    fun activeNativeSlotKeepsZeroWidthShellAlignedToBatterySlot() {
        assertTrue(
            SystemUiNativeCombinedParticipantOwner.isActiveSlotHandoffReady(
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
            SystemUiNativeCombinedParticipantOwner.isActiveSlotHandoffReady(
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
            SystemUiNativeCombinedParticipantOwner.isActiveSlotHandoffReady(
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
            SystemUiNativeCombinedParticipantOwner.isActiveSlotHandoffReady(
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
            SystemUiNativeCombinedParticipantOwner.resolvePostLayoutVisualWidth(
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
            SystemUiNativeCombinedParticipantOwner.resolveNativeSlotOccupancyWidth(
                nativeBatteryHidden = false,
                visualWidth = 105,
            ),
        )
        assertEquals(
            105,
            SystemUiNativeCombinedParticipantOwner.resolveNativeSlotOccupancyWidth(
                nativeBatteryHidden = true,
                visualWidth = 105,
            ),
        )
        assertEquals(
            null,
            SystemUiNativeCombinedParticipantOwner.resolveNativeSlotOccupancyWidth(
                nativeBatteryHidden = true,
                visualWidth = 0,
            ),
        )
    }

    @Test
    fun releasedBatterySlotAcceptsNativeMeasuredVisualWidth() {
        assertEquals(
            105,
            SystemUiNativeCombinedParticipantOwner.resolvePostLayoutVisualWidth(
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
            SystemUiNativeCombinedParticipantOwner.resolvePostLayoutVisualWidth(
                layoutWidth = 105,
                measuredWidth = 105,
                visualWidth = 105,
            ),
        )
        assertEquals(
            null,
            SystemUiNativeCombinedParticipantOwner.resolvePostLayoutVisualWidth(
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
            SystemUiNativeCombinedParticipantOwner.resolveNativeSlotTranslationX(
                statusIconsWidth = stableBoundary,
                rootLeft = 0,
            ),
        )
        assertEquals(
            448f,
            SystemUiNativeCombinedParticipantOwner.resolveNativeSlotTranslationX(
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
            SystemUiNativeCombinedParticipantOwner.resolveNativeSlotTranslationX(
                statusIconsWidth = 478,
                rootLeft = 0,
            ),
        )
        assertEquals(
            478f,
            SystemUiNativeCombinedParticipantOwner.resolveNativeSlotTranslationX(
                statusIconsWidth = 478,
                rootLeft = 0,
            ),
        )
        assertEquals(
            null,
            SystemUiNativeCombinedParticipantOwner.resolveNativeSlotTranslationX(
                statusIconsWidth = 0,
                rootLeft = 1,
            ),
        )
    }

    @Test
    fun activeHandoffUsesSlotAnchorInsteadOfEvictedBatteryContent() {
        assertTrue(
            SystemUiNativeCombinedParticipantOwner.isActiveSlotHandoffReady(
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
            SystemUiNativeCombinedParticipantOwner.isActiveSlotHandoffReady(
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
            SystemUiNativeCombinedParticipantOwner.mergeNativeParticipantTint(
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
            SystemUiNativeCombinedParticipantOwner.mergeNativeParticipantTint(
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
            SystemUiNativeCombinedParticipantOwner.mergeNativeParticipantTint(
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
            SystemUiNativeCombinedParticipantOwner.mergeNativeParticipantTint(
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
            SystemUiNativeCombinedParticipantOwner.resolveNativeContentVisibility(
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
            SystemUiNativeCombinedParticipantOwner.resolveNativeContentVisibility(
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
            SystemUiNativeCombinedParticipantOwner.resolveNativeContentVisibility(
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
            SystemUiNativeCombinedParticipantOwner.resolveNativeContentVisibility(
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
            SystemUiNativeCombinedParticipantOwner.resolveNativeFeatureRemoveFlag(
                featureEnabled = true,
                handoffValidated = true,
            ),
        )
        assertEquals(
            true,
            SystemUiNativeCombinedParticipantOwner.resolveNativeFeatureRemoveFlag(
                featureEnabled = false,
                handoffValidated = true,
            ),
        )
    }

    @Test
    fun unvalidatedMasterSwitchStaysOnBootstrapFallback() {
        assertEquals(
            null,
            SystemUiNativeCombinedParticipantOwner.resolveNativeFeatureRemoveFlag(
                featureEnabled = false,
                handoffValidated = false,
            ),
        )
    }

    @Test
    fun nativeVisibleStateNamesResolveWithoutAssumingNumericOrder() {
        val states =
            SystemUiNativeCombinedParticipantOwner.resolveNativeVisibilityStates { candidate ->
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
            SystemUiNativeCombinedParticipantOwner.resolveNativeVisibilityStates { candidate ->
                when (candidate) {
                    0 -> "ICON"
                    1 -> "DOT"
                    else -> "UNKNOWN"
                }
            },
        )
    }
}