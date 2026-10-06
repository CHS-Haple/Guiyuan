package com.chaners.guiyuan.xposed

import android.view.View

// Pure handoff/visual decisions live here; the owner keeps hooks, bindings and view lifecycle.
internal object NativeCombinedPolicy {
    private const val ZERO_SLOT_WIDTH = 0
    private const val MAX_NATIVE_VISIBLE_STATE_PROBE = 8

    internal fun contentVisibility(
        state: Int,
        iconState: Int?,
        dotState: Int?,
        hiddenState: Int?,
    ): ContentVisibility? {
        if (iconState != null && state == iconState) {
            return ContentVisibility(
                renderVisibility = View.VISIBLE,
                dotVisibility = View.GONE,
            )
        }
        if (dotState != null && state == dotState) {
            return ContentVisibility(
                renderVisibility = View.INVISIBLE,
                dotVisibility = View.VISIBLE,
            )
        }
        if (hiddenState != null && state == hiddenState) {
            return ContentVisibility(
                renderVisibility = View.INVISIBLE,
                dotVisibility = View.INVISIBLE,
            )
        }
        return null
    }

    internal fun visibilityStates(
        stateName: (Int) -> String?,
    ): VisibilityStates? {
        var icon: Int? = null
        var dot: Int? = null
        var hidden: Int? = null

        for (candidate in 0..MAX_NATIVE_VISIBLE_STATE_PROBE) {
            when (stateName(candidate)?.trim()?.uppercase()) {
                "ICON" -> icon = candidate
                "DOT" -> dot = candidate
                "HIDDEN" -> hidden = candidate
            }
            if (icon != null && dot != null && hidden != null) {
                break
            }
        }

        val resolvedIcon = icon ?: return null
        val resolvedDot = dot ?: return null
        val resolvedHidden = hidden ?: return null
        if (setOf(resolvedIcon, resolvedDot, resolvedHidden).size != 3) {
            return null
        }
        return VisibilityStates(
            icon = resolvedIcon,
            dot = resolvedDot,
            hidden = resolvedHidden,
        )
    }

    internal fun mergeTint(
        batteryTint: TintState,
        nativeTint: Int?,
    ): TintState {
        val resolvedNativeTint =
            nativeTint
                ?.takeIf { color -> (color ushr 24) != 0 }
        return if (resolvedNativeTint != null) {
            TintState(
                appliedTint = resolvedNativeTint,
                statusIconTint = resolvedNativeTint,
            )
        } else {
            TintState(
                appliedTint = batteryTint.appliedTint,
            )
        }
    }

    internal fun slotTranslationX(
        statusIconsWidth: Int,
        rootLeft: Int,
    ): Float? =
        (statusIconsWidth - rootLeft)
            .takeIf { translation -> translation >= 0 }
            ?.toFloat()

    internal fun zeroSlotReady(
        rootMeasuredWidth: Int,
        rootMeasuredHeight: Int,
        nativeBatteryHidden: Boolean = false,
        renderMeasuredWidth: Int,
        renderMeasuredHeight: Int,
        expectedVisualWidth: Int,
        expectedVisualHeight: Int,
        parentClipsChildren: Boolean,
        rootScreenX: Int,
        slotAnchorScreenX: Int,
        renderLeft: Int,
        renderRight: Int,
    ): Boolean =
        rootMeasuredWidth ==
            slotOccupancyWidth(
                nativeBatteryHidden = nativeBatteryHidden,
                visualWidth = expectedVisualWidth,
            ) &&
            rootMeasuredHeight > 0 &&
            renderMeasuredWidth == expectedVisualWidth &&
            renderMeasuredHeight == expectedVisualHeight &&
            expectedVisualWidth > 0 &&
            expectedVisualHeight > 0 &&
            !parentClipsChildren &&
            rootScreenX == slotAnchorScreenX &&
            renderLeft == 0 &&
            renderRight == expectedVisualWidth

    internal fun activeSlotReady(
        rootLayoutWidth: Int,
        rootLayoutHeight: Int,
        nativeBatteryHidden: Boolean = false,
        renderMeasuredWidth: Int,
        renderMeasuredHeight: Int,
        expectedVisualWidth: Int,
        expectedVisualHeight: Int,
        parentClipsChildren: Boolean,
        rootScreenX: Int,
        slotAnchorScreenX: Int,
        renderLeft: Int,
        renderRight: Int,
    ): Boolean =
        rootLayoutWidth ==
            slotOccupancyWidth(
                nativeBatteryHidden = nativeBatteryHidden,
                visualWidth = expectedVisualWidth,
            ) &&
            rootLayoutHeight == expectedVisualHeight &&
            renderMeasuredWidth == expectedVisualWidth &&
            renderMeasuredHeight == expectedVisualHeight &&
            expectedVisualWidth > 0 &&
            expectedVisualHeight > 0 &&
            !parentClipsChildren &&
            rootScreenX == slotAnchorScreenX &&
            renderLeft == 0 &&
            renderRight == expectedVisualWidth

    internal enum class HandoffMode {
        BLOCKED,
        VISIBLE_HOME,
        PREARMED_KEYGUARD,
    }

    internal fun handoffMode(
        surface: SysUiSceneSource.Surface,
        rootShown: Boolean,
    ): HandoffMode =
        when {
            surface == SysUiSceneSource.Surface.UNLOCKED_STATUS_BAR ->
                HandoffMode.VISIBLE_HOME
            surface == SysUiSceneSource.Surface.KEYGUARD && !rootShown ->
                HandoffMode.PREARMED_KEYGUARD
            else ->
                HandoffMode.BLOCKED
        }

    internal fun slotOccupancyWidth(
        nativeBatteryHidden: Boolean,
        visualWidth: Int,
    ): Int? =
        visualWidth
            .takeIf { width -> width > 0 }
            ?.let { width ->
                if (nativeBatteryHidden) {
                    width
                } else {
                    ZERO_SLOT_WIDTH
                }
            }

    internal fun postLayoutVisualWidth(
        layoutWidth: Int,
        measuredWidth: Int,
        visualWidth: Int,
        nativeBatteryHidden: Boolean = false,
    ): Int? {
        val occupancyWidth =
            slotOccupancyWidth(
                nativeBatteryHidden = nativeBatteryHidden,
                visualWidth = visualWidth,
            ) ?: return null
        return visualWidth.takeIf {
            layoutWidth == occupancyWidth &&
                measuredWidth == occupancyWidth
        }
    }

    internal fun featureRemoveFlag(
        featureEnabled: Boolean,
        handoffValidated: Boolean,
    ): Boolean? =
        if (handoffValidated) {
            !featureEnabled
        } else {
            null
        }

    internal data class ContentVisibility(
        val renderVisibility: Int,
        val dotVisibility: Int,
    )

    internal data class VisibilityStates(
        val icon: Int,
        val dot: Int,
        val hidden: Int,
    )
}
