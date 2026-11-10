package com.chaners.guiyuan.ui.screens

import android.content.Context
import com.chaners.guiyuan.R
import java.util.Locale

internal fun switchLabel(
    context: Context,
    name: Int,
    value: String?,
): String? {
    val state =
        when (value) {
            LOG_BOOLEAN_TRUE -> R.string.diagnostics_log_switch_on
            LOG_BOOLEAN_FALSE -> R.string.diagnostics_log_switch_off
            else -> return null
        }
    return context.getString(
        R.string.diagnostics_log_switch_value,
        context.getString(name),
        context.getString(state),
    )
}

internal fun stateLabel(context: Context, state: String): String =
    stateRes(state)?.let(context::getString) ?: state

internal fun stateRes(state: String): Int? =
    when (state.lowercase(Locale.ROOT).replace('_', '-')) {
        "unknown" -> R.string.diagnostics_log_state_unknown
        "ready" -> R.string.diagnostics_log_state_ready
        "observed" -> R.string.diagnostics_log_state_observed
        "enabled" -> R.string.diagnostics_log_state_enabled
        "disabled" -> R.string.diagnostics_log_state_disabled
        "unavailable" -> R.string.diagnostics_log_state_unavailable
        "partial" -> R.string.diagnostics_log_state_partial
        "error" -> R.string.diagnostics_log_state_error
        "scheduled" -> R.string.diagnostics_log_state_scheduled
        "restart-required" -> R.string.diagnostics_log_state_restart_required
        "pending" -> R.string.diagnostics_log_state_pending
        "prepared" -> R.string.diagnostics_log_state_prepared
        "released" -> R.string.diagnostics_log_state_released
        "native" -> R.string.diagnostics_log_state_native
        "combined" -> R.string.diagnostics_log_state_combined
        "keyguard" -> R.string.diagnostics_log_state_keyguard
        "aod" -> R.string.diagnostics_log_state_aod
        "home" -> R.string.diagnostics_log_state_home
        "aod-stable" -> R.string.diagnostics_log_state_aod_stable
        "native-transition" -> R.string.diagnostics_log_state_native_transition
        "keyguard-eligible" -> R.string.diagnostics_log_state_keyguard_eligible
        "preparing" -> R.string.diagnostics_log_state_preparing
        "active" -> R.string.diagnostics_log_state_active
        "cleared" -> R.string.diagnostics_log_state_cleared
        "eligible" -> R.string.diagnostics_log_state_eligible
        "acquired" -> R.string.diagnostics_log_state_acquired
        "armed" -> R.string.diagnostics_log_state_armed
        "fallback" -> R.string.diagnostics_log_state_fallback
        "held" -> R.string.diagnostics_log_state_held
        "retained" -> R.string.diagnostics_log_state_retained
        "revealed" -> R.string.diagnostics_log_state_revealed
        "sampled" -> R.string.diagnostics_log_state_sampled
        "visual-only" -> R.string.diagnostics_log_state_visual_only
        "panel-hidden" -> R.string.diagnostics_log_state_panel_hidden
        "after-panel-hidden" -> R.string.diagnostics_log_state_after_panel_hidden
        "before-stable-commit" -> R.string.diagnostics_log_state_before_stable_commit
        "prelayout-pending" -> R.string.diagnostics_log_state_prelayout_pending
        "waiting-prelayout" -> R.string.diagnostics_log_state_waiting_prelayout
        "visual-handoff-layout-pending" -> R.string.diagnostics_log_state_visual_handoff_layout_pending
        else -> null
    }

internal fun sourceRes(value: String): Int? =
    when (value.lowercase(Locale.ROOT)) {
        "mobile" -> R.string.diagnostics_log_source_mobile
        "capabilities" -> R.string.diagnostics_log_source_capabilities
        "lost" -> R.string.diagnostics_log_source_lost
        "mobile-data" -> R.string.diagnostics_log_source_mobile_data
        "initial-none" -> R.string.diagnostics_log_source_initial_none
        "callback" -> R.string.diagnostics_log_source_callback
        "hotreloadnativeicon" -> R.string.diagnostics_log_source_hot_reload_native_icon
        "native" -> R.string.diagnostics_log_source_native
        "coldstart" -> R.string.diagnostics_log_source_cold_start
        "hotreload" -> R.string.diagnostics_log_source_hot_reload
        "hotreloadrestore" -> R.string.diagnostics_log_source_hot_reload_restore
        "hotreloadtransfer" -> R.string.diagnostics_log_source_hot_reload_transfer
        else -> null
    }

internal fun sourceLabel(context: Context, value: String): String =
    if (value.equals("wifi", ignoreCase = true)) "Wi-Fi"
    else sourceRes(value)?.let(context::getString) ?: value

internal fun transportRes(value: String): Int? =
    when (value.lowercase(Locale.ROOT)) {
        "cellular" -> R.string.diagnostics_log_source_mobile
        "none" -> R.string.diagnostics_log_source_none
        "other" -> R.string.diagnostics_log_source_other
        else -> null
    }

internal fun transportLabel(context: Context, value: String): String =
    if (value.equals("wifi", ignoreCase = true)) "Wi-Fi"
    else transportRes(value)?.let(context::getString) ?: value

internal fun chargingStateRes(value: String): Int? =
    when (value.lowercase(Locale.ROOT)) {
        LOG_BOOLEAN_TRUE -> R.string.diagnostics_log_charging_on
        LOG_BOOLEAN_FALSE -> R.string.diagnostics_log_charging_off
        else -> null
    }

internal fun chargingStateLabel(context: Context, value: String?): String? =
    value?.let { chargingStateRes(it)?.let(context::getString) }

internal fun reasonLabel(
    context: Context,
    value: String,
): String =
    when (value) {
        "saved-state-missing-or-unsupported-generation" ->
            context.getString(R.string.diagnostics_log_reason_saved_state)
        "main-thread-dispatch-failed" ->
            context.getString(R.string.diagnostics_log_reason_dispatch)
        else -> value
    }
