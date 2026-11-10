package com.chaners.guiyuan.ui.screens

import android.content.Context
import com.chaners.guiyuan.R
import com.chaners.guiyuan.system.LogEntry
import com.chaners.guiyuan.system.LogLevel
import java.util.Locale

internal const val LOG_FIELD_SOURCE = "source"
internal const val LOG_FIELD_REASON = "reason"
internal const val LOG_FIELD_TRANSPORT = "transport"
internal const val LOG_FIELD_TRANSFERRED_CHARGING = "transferredCharging"
internal const val LOG_FIELD_NATIVE_CHARGING = "nativeCharging"
internal const val LOG_FIELD_NATIVE_PERCENT = "nativePercent"
private const val LOG_FIELD_LEVEL = "level"
internal const val LOG_BOOLEAN_TRUE = "true"
internal const val LOG_BOOLEAN_FALSE = "false"
internal const val EVENT_CONNECTIVITY = "connectivity"
internal const val EVENT_CONNECTIVITY_DELIVERY_FAILED = "connectivity.delivery-failed"
internal const val EVENT_PIPELINE_LATENCY = "pipeline.latency"
internal const val EVENT_BATTERY_RECONCILE = "battery.reconcile"
private const val EVENT_MODULE_LOADED = "module.loaded"
private const val EVENT_MODULE_RELOADED = "module.reloaded"
private const val EVENT_COMPATIBILITY_PROBE = "compatibility.probe"
private const val EVENT_COMPATIBILITY_REVALIDATED = "compatibility.revalidated"
private const val EVENT_HOOK_INSTALL = "hook.install"
private const val EVENT_HOOK_REPLACE = "hook.replace"
private const val EVENT_SOURCE_INSTALL = "source.install"
private const val EVENT_SOURCE_ATTACH = "source.attach"
private const val EVENT_RUNTIME_ATTACH = "runtime.attach"
private const val EVENT_RUNTIME_TEARDOWN = "runtime.teardown"
private const val EVENT_RUNTIME_PREFERENCES_BIND = "runtimePreferences.bind"
private const val EVENT_DIAGNOSTICS_BIND = "diagnostics.bind"
private const val EVENT_HOST_CAPTURE = "host.capture"
private const val EVENT_HOST_RESTORE = "host.restore"
private const val EVENT_SESSION_ATTACH = "session.attach"
internal const val EVENT_PRESENTATION_RESOLVE = "presentation.resolve"
private const val EVENT_PRESENTATION_CUTOVER = "presentation.cutover"
private const val EVENT_PRESENTATION_FAIL_NATIVE = "presentation.failNative"
private const val EVENT_RENDERER_ATTACH = "renderer.attach"
private const val EVENT_DIAGNOSTICS_SNAPSHOT = "diagnostics.snapshot"
private const val EVENT_DIAGNOSTICS_LEVEL = "diagnostics.level"
private const val EVENT_VISUAL_SETTINGS_CHANGED = "visualSettings.changed"
private const val EVENT_FEATURE_SETTINGS_CHANGED = "featureSettings.changed"
private const val EVENT_HOT_RELOAD_PREPARE = "hotReload.prepare"
private const val EVENT_HOT_RELOAD_GENERATION_HANDOFF = "hotReload.generationHandoff"
private const val EVENT_HOT_RELOAD_MIGRATION = "hotReload.migration"
private const val EVENT_HOT_RELOAD_RESTORE = "hotReload.restore"
private const val EVENT_HOT_RELOAD_COMPLETE = "hotReload.complete"
private const val EVENT_TINT_COMMIT = "tintCommit"
private const val EVENT_HOME_RENDER_TINT = "homeRenderTint"
private const val EVENT_MOBILE_RECOVERY = "mobile.recovery"
private const val EVENT_SCENE_STABLE_FAMILY = "scene.stableFamily"
private const val EVENT_AOD_STATE = "aod.state"
private const val EVENT_AOD_TARGET = "aod.target"

internal fun logTitle(
    context: Context,
    entry: LogEntry,
): String {
    val res =
        when (entry.event) {
            EVENT_MODULE_LOADED -> R.string.diagnostics_log_event_module_loaded
            EVENT_MODULE_RELOADED -> R.string.diagnostics_log_event_module_reloaded
            EVENT_COMPATIBILITY_PROBE,
            EVENT_COMPATIBILITY_REVALIDATED,
            -> R.string.diagnostics_log_event_compatibility
            EVENT_HOOK_INSTALL,
            EVENT_HOOK_REPLACE,
            -> R.string.diagnostics_log_event_hook
            EVENT_SOURCE_INSTALL,
            EVENT_SOURCE_ATTACH,
            -> R.string.diagnostics_log_event_source
            EVENT_RUNTIME_ATTACH -> R.string.diagnostics_log_event_runtime_attach
            EVENT_RUNTIME_TEARDOWN -> R.string.diagnostics_log_event_runtime_teardown
            EVENT_RUNTIME_PREFERENCES_BIND -> R.string.diagnostics_log_event_runtime_preferences
            EVENT_DIAGNOSTICS_BIND -> R.string.diagnostics_log_event_diagnostics_bind
            EVENT_HOST_CAPTURE -> R.string.diagnostics_log_event_host_capture
            EVENT_HOST_RESTORE -> R.string.diagnostics_log_event_host_restore
            EVENT_SESSION_ATTACH -> R.string.diagnostics_log_event_session_attach
            EVENT_PRESENTATION_CUTOVER -> R.string.diagnostics_log_event_presentation_cutover
            EVENT_PRESENTATION_FAIL_NATIVE -> R.string.diagnostics_log_event_fail_native
            EVENT_PRESENTATION_RESOLVE -> R.string.diagnostics_log_event_presentation_resolve
            EVENT_RENDERER_ATTACH -> R.string.diagnostics_log_event_renderer_attach
            EVENT_DIAGNOSTICS_SNAPSHOT -> R.string.diagnostics_log_event_diagnostics_snapshot
            EVENT_DIAGNOSTICS_LEVEL -> R.string.diagnostics_log_event_diagnostics_level
            EVENT_PIPELINE_LATENCY -> R.string.diagnostics_log_event_pipeline_latency
            EVENT_VISUAL_SETTINGS_CHANGED -> R.string.diagnostics_log_event_visual_settings
            EVENT_FEATURE_SETTINGS_CHANGED -> R.string.diagnostics_log_event_feature_settings
            "hotReload.cleanup" -> R.string.diagnostics_log_event_hot_reload_cleanup
            EVENT_BATTERY_RECONCILE -> R.string.diagnostics_log_event_battery_reconcile
            EVENT_HOT_RELOAD_PREPARE -> R.string.diagnostics_log_event_hot_reload_prepare
            EVENT_HOT_RELOAD_GENERATION_HANDOFF -> R.string.diagnostics_log_event_hot_reload_handoff
            EVENT_HOT_RELOAD_MIGRATION -> R.string.diagnostics_log_event_hot_reload_migration
            EVENT_HOT_RELOAD_RESTORE -> R.string.diagnostics_log_event_hot_reload_restore
            EVENT_HOT_RELOAD_COMPLETE -> R.string.diagnostics_log_event_hot_reload
            EVENT_MOBILE_RECOVERY -> R.string.diagnostics_log_event_mobile_recovery
            EVENT_SCENE_STABLE_FAMILY -> R.string.diagnostics_log_event_scene_stable
            EVENT_AOD_STATE -> R.string.diagnostics_log_event_aod_state
            EVENT_AOD_TARGET -> R.string.diagnostics_log_event_aod_target
            EVENT_CONNECTIVITY -> R.string.diagnostics_log_event_connectivity
            EVENT_CONNECTIVITY_DELIVERY_FAILED -> R.string.diagnostics_log_event_connectivity_failure
            "networkPipeline.wifi.iconEvent" -> R.string.diagnostics_log_event_wifi_icon
            "statusIconPresentation" -> R.string.diagnostics_log_event_status_icon_presentation
            EVENT_TINT_COMMIT -> R.string.diagnostics_log_event_tint_commit
            EVENT_HOME_RENDER_TINT -> R.string.diagnostics_log_event_home_tint
            else -> null
        }
    return res?.let(context::getString)
        ?: entry.event
        ?: context.getString(R.string.diagnostics_log_event_generic)
}

internal fun logSummary(context: Context, entry: LogEntry): String {
    val fallback = if (entry.structured) "" else entry.message
    val separator = context.getString(R.string.diagnostics_log_summary_separator)
    val summary = when (entry.event) {
        EVENT_CONNECTIVITY_DELIVERY_FAILED, EVENT_CONNECTIVITY ->
            logConnectivitySummary(context, entry, separator).orEmpty()
        EVENT_BATTERY_RECONCILE, EVENT_PIPELINE_LATENCY,
        EVENT_TINT_COMMIT, EVENT_HOME_RENDER_TINT ->
            logRuntimeTelemetrySummary(context, entry, separator).orEmpty()
        else -> logLifecycleSummary(context, entry, separator)
    }
    return summary.ifBlank { fallback }
}

// The same nonblank part/join contract applies to every compact diagnostic event.
internal fun joinLogSummaryParts(separator: String, vararg parts: String?): String =
    parts.filterNotNull().filter(String::isNotBlank).joinToString(separator)

// Network observations encode transport and delivery, not session lifecycle.
private fun logConnectivitySummary(context: Context, entry: LogEntry, separator: String): String? {
    val fields = entry.fields
    if (entry.event == EVENT_CONNECTIVITY_DELIVERY_FAILED) {
        return joinLogSummaryParts(separator,
            fields[LOG_FIELD_SOURCE]?.let {
                context.getString(R.string.diagnostics_log_summary_source, sourceLabel(context, it))
            },
            fields["type"],
        )
    }

    if (entry.event == EVENT_CONNECTIVITY) {
        return buildList {
            fields[LOG_FIELD_TRANSPORT]?.let { add(transportLabel(context, it)) }
            if (fields["validated"] == LOG_BOOLEAN_TRUE) {
                add(context.getString(R.string.diagnostics_log_summary_validated))
            }
            if (fields["internetCapability"] == LOG_BOOLEAN_TRUE) {
                add(context.getString(R.string.diagnostics_log_summary_internet))
            }
            val mobileState =
                when (fields["mobileDataEnabled"]) {
                    LOG_BOOLEAN_TRUE -> R.string.diagnostics_log_summary_mobile_data_on
                    LOG_BOOLEAN_FALSE -> R.string.diagnostics_log_summary_mobile_data_off
                    else -> null
                }
            mobileState?.let { add(context.getString(it)) }
        }.joinToString(separator)
    }
    return null
}

// Battery reconciliation, pipeline latency and tint are direct runtime telemetry.
private fun logRuntimeTelemetrySummary(context: Context, entry: LogEntry, separator: String): String? {
    val fields = entry.fields
    if (entry.event == EVENT_BATTERY_RECONCILE) {
        val previous = chargingStateLabel(context, fields[LOG_FIELD_TRANSFERRED_CHARGING])
        val native = chargingStateLabel(context, fields[LOG_FIELD_NATIVE_CHARGING])
        val charging =
            when {
                previous != null && native != null ->
                    context.getString(R.string.diagnostics_log_battery_transition, previous, native)
                native != null -> context.getString(R.string.diagnostics_log_battery_native, native)
                else -> entry.state?.let { stateLabel(context, it) }
            }
        val percent = fields[LOG_FIELD_NATIVE_PERCENT]?.toIntOrNull()?.takeIf { it in 0..100 }
        return joinLogSummaryParts(separator,
            charging,
            percent?.let { context.getString(R.string.diagnostics_log_battery_percent, it) },
        )
    }

    if (entry.event == EVENT_PIPELINE_LATENCY) {
        return joinLogSummaryParts(separator,
            fields["sourceToDrawUs"]?.toLongOrNull()?.let { micros ->
                context.getString(R.string.diagnostics_log_summary_total_time, formatMicros(micros))
            },
            fields[LOG_FIELD_SOURCE]?.let {
                context.getString(R.string.diagnostics_log_summary_source, sourceLabel(context, it))
            },
        )
    }

    if (entry.event == EVENT_TINT_COMMIT || entry.event == EVENT_HOME_RENDER_TINT) {
        return joinLogSummaryParts(separator,
            fields["applied"] ?: fields["statusIcon"],
            fields[LOG_FIELD_SOURCE]?.let {
                context.getString(R.string.diagnostics_log_summary_source, sourceLabel(context, it))
            },
        )
    }
    return null
}

// Skip a repeated ready/released/preparing subtitle only for a normal unqualified event.
internal fun shouldElideLogSummaryState(entry: LogEntry, reason: String?, problem: Boolean): Boolean =
    !problem && reason == null &&
        when (entry.event) {
            EVENT_MODULE_LOADED, EVENT_MODULE_RELOADED, EVENT_RUNTIME_ATTACH,
            EVENT_RUNTIME_PREFERENCES_BIND, EVENT_DIAGNOSTICS_BIND,
            EVENT_HOST_RESTORE, EVENT_SESSION_ATTACH, EVENT_RENDERER_ATTACH,
            EVENT_HOT_RELOAD_RESTORE, EVENT_HOT_RELOAD_COMPLETE,
            EVENT_HOT_RELOAD_GENERATION_HANDOFF, EVENT_MOBILE_RECOVERY,
            -> entry.state.equals("ready", true)
            EVENT_RUNTIME_TEARDOWN -> entry.state.equals("released", true)
            EVENT_HOT_RELOAD_PREPARE -> entry.state.equals("preparing", true)
            else -> false
        }

// All other events describe module/session state or a fail-native reason.
private fun logLifecycleSummary(context: Context, entry: LogEntry, separator: String): String {
    val fields = entry.fields
    val state = entry.state?.let { stateLabel(context, it) }
    val reason = fields[LOG_FIELD_REASON]?.takeIf(String::isNotBlank)?.let { reasonLabel(context, it) }
    val problem = entry.level == LogLevel.Warning ||
        entry.level == LogLevel.Error || entry.level == LogLevel.Fatal
    val source = fields[LOG_FIELD_SOURCE]?.let { sourceLabel(context, it) }

    val summary =
        when (entry.event) {
            EVENT_FEATURE_SETTINGS_CHANGED -> joinLogSummaryParts(separator,
                switchLabel(context, R.string.diagnostics_log_setting_combined, fields["combinedStatusEnabled"]),
                switchLabel(context, R.string.diagnostics_log_setting_keyguard, fields["keyguardEnabled"]),
                switchLabel(context, R.string.diagnostics_log_setting_aod, fields["aodEnabled"]),
                reason,
            ).ifBlank { joinLogSummaryParts(separator, state, reason) }

            EVENT_VISUAL_SETTINGS_CHANGED -> joinLogSummaryParts(separator, state, reason)

            EVENT_DIAGNOSTICS_LEVEL -> joinLogSummaryParts(separator,
                when (fields[LOG_FIELD_LEVEL]) {
                    "detailed" -> context.getString(R.string.diagnostics_mode_detailed)
                    "general" -> context.getString(R.string.diagnostics_mode_basic)
                    else -> fields[LOG_FIELD_LEVEL]
                },
                reason,
            )

            EVENT_SCENE_STABLE_FAMILY -> {
                val previous = fields["previous"]?.takeUnless { it.equals("UNKNOWN", true) }
                val change =
                    if (previous != null && state != null) {
                        stateLabel(context, previous) + " → " + state
                    } else {
                        state
                    }
                joinLogSummaryParts(separator, change, reason)
            }

            EVENT_PRESENTATION_FAIL_NATIVE -> joinLogSummaryParts(separator, reason)

            EVENT_AOD_STATE, EVENT_AOD_TARGET, EVENT_PRESENTATION_CUTOVER,
            EVENT_COMPATIBILITY_PROBE, EVENT_COMPATIBILITY_REVALIDATED,
            EVENT_HOOK_INSTALL, EVENT_HOOK_REPLACE, EVENT_SOURCE_INSTALL, EVENT_SOURCE_ATTACH,
            -> joinLogSummaryParts(separator, state, reason)

            else -> {
                val alreadyTold = shouldElideLogSummaryState(entry, reason, problem)
                joinLogSummaryParts(separator,
                    if (alreadyTold) null else state,
                    reason,
                    if (problem && reason == null) {
                        source?.let { context.getString(R.string.diagnostics_log_summary_source, it) }
                    } else null,
                )
            }
        }
    return summary
}

private fun formatMicros(micros: Long): String =
    if (micros >= 1_000L || micros <= -1_000L) {
        String.format(Locale.US, "%.2f ms", micros / 1_000.0)
    } else {
        "$micros μs"
    }

internal val RUNTIME_EVENTS =
    setOf(
        EVENT_MODULE_LOADED,
        EVENT_MODULE_RELOADED,
        EVENT_DIAGNOSTICS_SNAPSHOT,
        EVENT_BATTERY_RECONCILE,
        EVENT_COMPATIBILITY_PROBE,
        EVENT_COMPATIBILITY_REVALIDATED,
        EVENT_HOOK_INSTALL,
        EVENT_HOOK_REPLACE,
        EVENT_SOURCE_INSTALL,
        EVENT_SOURCE_ATTACH,
        EVENT_RUNTIME_ATTACH,
        EVENT_RUNTIME_TEARDOWN,
        EVENT_RUNTIME_PREFERENCES_BIND,
        EVENT_DIAGNOSTICS_BIND,
        EVENT_DIAGNOSTICS_LEVEL,
        EVENT_HOST_CAPTURE,
        EVENT_HOST_RESTORE,
        EVENT_SESSION_ATTACH,
        EVENT_PRESENTATION_CUTOVER,
        EVENT_PRESENTATION_FAIL_NATIVE,
        EVENT_RENDERER_ATTACH,
        EVENT_FEATURE_SETTINGS_CHANGED,
        EVENT_VISUAL_SETTINGS_CHANGED,
        EVENT_HOT_RELOAD_PREPARE,
        EVENT_HOT_RELOAD_GENERATION_HANDOFF,
        EVENT_HOT_RELOAD_MIGRATION,
        EVENT_HOT_RELOAD_RESTORE,
        EVENT_HOT_RELOAD_COMPLETE,
        EVENT_MOBILE_RECOVERY,
        EVENT_SCENE_STABLE_FAMILY,
        EVENT_AOD_STATE,
        EVENT_AOD_TARGET,
    )
