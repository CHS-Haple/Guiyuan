package com.chaners.guiyuan.xposed

import android.os.Bundle
import android.view.View
import android.view.ViewGroup

internal object HotReloadTransfer {
    private const val VERSION = 9
    private const val PRESENTATION_TRANSFER_VERSION = 8
    private const val TINT_TRANSFER_VERSION = 7
    private const val CC_TRANSFER_VERSION = 6
    private const val SHADE_TRANSFER_VERSION = 5
    private const val PREVIOUS_VERSION = 4
    private const val NATIVE_TRANSFER_VERSION = 3
    private const val VISUAL_TRANSFER_VERSION = 2
    private const val LEGACY_VERSION = 1
    private const val INDEX_VERSION = 0
    private const val INDEX_HOST = 1
    private const val INDEX_STATE = 2
    private const val INDEX_BINDINGS = 3
    private const val IDX_SHADE_HOME_ELIGIBLE = 4
    private const val IDX_CC_HOME_ELIGIBLE = 5
    private const val INDEX_APPLIED_TINT = 6
    private const val INDEX_STATUS_ICON_TINT = 7
    private const val IDX_CC_FAKE_HOST = 8
    private const val IDX_CC_COMPACT_READY = 9
    private const val INDEX_GENERATION_HANDOFF = 10
    private const val CURRENT_PAYLOAD_SIZE = 11
    private const val PRESENTATION_PAYLOAD_SIZE = 9
    private const val TINT_PAYLOAD_SIZE = 8
    private const val CONTROL_CENTER_PAYLOAD_SIZE = 6
    private const val SHADE_PAYLOAD_SIZE = 5
    private const val PREVIOUS_PAYLOAD_SIZE = 4
    private const val LEGACY_PAYLOAD_SIZE = 4
    private const val VISUAL_PAYLOAD_SIZE = 5
    private const val NATIVE_PAYLOAD_SIZE = 6

    private const val CC_FAKE_ROOT_CLASS =
        "com.android.systemui.controlcenter.phone.widget.ControlCenterFakeStatusIcons"

    fun capture(
        host: Any?,
        state: Bundle,
        bindings: Any,
        notificationShadeHomeEligible: Boolean?,
        controlCenterHomeEligible: Boolean?,
        appliedTint: Int?,
        statusIconTint: Int?,
        controlCenterFakeHost: ViewGroup?,
        controlCenterCompactReady: Boolean,
        generationHandoff: Runnable,
    ): Any? {
        val hostView = host as? View ?: return null
        if (hostView.javaClass.name != StatusBarHostCapture.HOST_CLASS_NAME) {
            return null
        }

        val fakeHost =
            controlCenterFakeHost
                ?.takeIf { candidate ->
                    candidate.isAttachedToWindow &&
                        candidate.javaClass.name == CC_FAKE_ROOT_CLASS
                }

        return arrayOf(
            VERSION,
            hostView,
            state,
            bindings,
            notificationShadeHomeEligible,
            controlCenterHomeEligible,
            appliedTint,
            statusIconTint,
            fakeHost,
            controlCenterCompactReady,
            generationHandoff,
        )
    }

    fun restore(raw: Any?): Restored? {
        val payload = raw as? Array<*> ?: return null
        val version = (payload.getOrNull(INDEX_VERSION) as? Number)?.toInt()
            ?: return null
        val expectedSize =
            when (version) {
                VERSION -> CURRENT_PAYLOAD_SIZE
                PRESENTATION_TRANSFER_VERSION -> PRESENTATION_PAYLOAD_SIZE
                TINT_TRANSFER_VERSION -> TINT_PAYLOAD_SIZE
                CC_TRANSFER_VERSION -> CONTROL_CENTER_PAYLOAD_SIZE
                SHADE_TRANSFER_VERSION -> SHADE_PAYLOAD_SIZE
                PREVIOUS_VERSION -> PREVIOUS_PAYLOAD_SIZE
                NATIVE_TRANSFER_VERSION -> NATIVE_PAYLOAD_SIZE
                VISUAL_TRANSFER_VERSION -> VISUAL_PAYLOAD_SIZE
                LEGACY_VERSION -> LEGACY_PAYLOAD_SIZE
                else -> return null
            }
        if (payload.size != expectedSize) {
            return null
        }

        val host = payload.getOrNull(INDEX_HOST) as? View ?: return null
        if (host.javaClass.name != StatusBarHostCapture.HOST_CLASS_NAME) {
            return null
        }

        val state = payload.getOrNull(INDEX_STATE) as? Bundle ?: return null
        val bindings = payload.getOrNull(INDEX_BINDINGS)
        val notificationShadeHomeEligible =
            if (
                version == VERSION ||
                version == PRESENTATION_TRANSFER_VERSION ||
                version == TINT_TRANSFER_VERSION ||
                version == CC_TRANSFER_VERSION ||
                version == SHADE_TRANSFER_VERSION
            ) {
                payload.getOrNull(IDX_SHADE_HOME_ELIGIBLE) as? Boolean
            } else {
                null
            }
        val controlCenterHomeEligible =
            if (
                version == VERSION ||
                version == PRESENTATION_TRANSFER_VERSION ||
                version == TINT_TRANSFER_VERSION ||
                version == CC_TRANSFER_VERSION
            ) {
                payload.getOrNull(IDX_CC_HOME_ELIGIBLE) as? Boolean
            } else {
                null
            }
        val appliedTint =
            if (
                version == VERSION ||
                version == PRESENTATION_TRANSFER_VERSION ||
                version == TINT_TRANSFER_VERSION
            ) {
                (payload.getOrNull(INDEX_APPLIED_TINT) as? Number)
                    ?.toInt()
                    ?.takeIf(::isOpaqueEnoughForPresentation)
            } else {
                null
            }
        val statusIconTint =
            if (
                (
                    version == VERSION ||
                        version == PRESENTATION_TRANSFER_VERSION ||
                        version == TINT_TRANSFER_VERSION
                ) &&
                    appliedTint != null
            ) {
                (payload.getOrNull(INDEX_STATUS_ICON_TINT) as? Number)
                    ?.toInt()
                    ?.takeIf(::isOpaqueEnoughForPresentation)
            } else {
                null
            }

        val controlCenterFakeHost =
            if (version == VERSION || version == PRESENTATION_TRANSFER_VERSION) {
                (payload.getOrNull(IDX_CC_FAKE_HOST) as? ViewGroup)
                    ?.takeIf { candidate ->
                        candidate.isAttachedToWindow &&
                            candidate.javaClass.name == CC_FAKE_ROOT_CLASS
                    }
            } else {
                null
            }

        val controlCenterCompactReady =
            if (version == VERSION) {
                payload.getOrNull(IDX_CC_COMPACT_READY) as? Boolean ?: false
            } else {
                false
            }
        val generationHandoff =
            if (version == VERSION) {
                payload.getOrNull(INDEX_GENERATION_HANDOFF) as? Runnable
            } else {
                null
            }

        return Restored(
            host = host,
            state = state,
            bindings = bindings,
            notificationShadeHomeEligible = notificationShadeHomeEligible,
            controlCenterHomeEligible = controlCenterHomeEligible,
            appliedTint = appliedTint,
            statusIconTint = statusIconTint,
            controlCenterFakeHost = controlCenterFakeHost,
            controlCenterCompactReady = controlCenterCompactReady,
            generationHandoff = generationHandoff,
        )
    }

    private fun isOpaqueEnoughForPresentation(color: Int): Boolean =
        color ushr 24 != 0

    internal data class Restored(
        val host: View,
        val state: Bundle,
        val bindings: Any?,
        val notificationShadeHomeEligible: Boolean?,
        val controlCenterHomeEligible: Boolean?,
        val appliedTint: Int?,
        val statusIconTint: Int?,
        val controlCenterFakeHost: ViewGroup?,
        val controlCenterCompactReady: Boolean,
        val generationHandoff: Runnable?,
    )
}
