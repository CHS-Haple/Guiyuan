package com.chaners.guiyuan.ui.screens

import android.content.Context
import com.chaners.guiyuan.R
import com.chaners.guiyuan.system.LogCategory
import com.chaners.guiyuan.system.LogEntry
import com.chaners.guiyuan.system.LogLevel

private const val MAX_DETAIL_FIELDS = 5

internal fun isRuntimeLog(entry: LogEntry, focusedCategory: LogCategory? = null): Boolean {
    if (
        entry.level == LogLevel.Warning ||
        entry.level == LogLevel.Error ||
        entry.level == LogLevel.Fatal
    ) {
        return true
    }
    // The connectivity source still emits a legacy INFO record.
    if (entry.event == EVENT_CONNECTIVITY && LOG_FIELD_TRANSPORT in entry.fields) return true
    if (entry.event == EVENT_CONNECTIVITY_DELIVERY_FAILED) return true
    if (!entry.structured) return false
    return when (entry.event) {
        EVENT_PIPELINE_LATENCY -> focusedCategory == LogCategory.Performance
        EVENT_PRESENTATION_RESOLVE -> focusedCategory == LogCategory.Display
        else -> entry.event in RUNTIME_EVENTS
    }
}

internal fun logDetailFields(entry: LogEntry): List<Pair<String, String>> {
    val priority =
        if (entry.event == EVENT_BATTERY_RECONCILE) BATTERY_DETAIL_PRIORITY else DETAIL_FIELD_PRIORITY
    return entry.fields
        .asSequence()
        .filter { (key, _) -> key !in LOG_META_FIELDS }
        .sortedBy { (key, _) ->
            priority.indexOf(key).let { if (it < 0) Int.MAX_VALUE else it }
        }
        .take(MAX_DETAIL_FIELDS)
        .map { (key, value) -> key to value }
        .toList()
}

internal fun levelLabel(level: LogLevel): String =
    when (level) {
        LogLevel.Verbose -> "VERBOSE"
        LogLevel.Debug -> "DEBUG"
        LogLevel.Info -> "INFO"
        LogLevel.Warning -> "WARN"
        LogLevel.Error -> "ERROR"
        LogLevel.Fatal -> "FATAL"
        LogLevel.Unknown -> "LOG"
    }

internal fun categoryLabel(
    context: Context,
    category: LogCategory,
): String =
    context.getString(
        when (category) {
            LogCategory.Module -> R.string.diagnostics_log_category_module
            LogCategory.Network -> R.string.diagnostics_log_category_network
            LogCategory.Display -> R.string.diagnostics_log_category_display
            LogCategory.Native -> R.string.diagnostics_log_category_native
            LogCategory.Transition -> R.string.diagnostics_log_category_transition
            LogCategory.Performance -> R.string.diagnostics_log_category_performance
            LogCategory.Settings -> R.string.diagnostics_log_category_settings
            LogCategory.Other -> R.string.diagnostics_log_category_other
        },
    )

// The same raw line can appear more than once in a capture.
internal fun logRowKey(entry: LogEntry, index: Int): String = "${entry.key}:$index"

internal fun matchesFilter(
    entry: LogEntry,
    levelMask: Int,
    categoryMask: Int,
): Boolean {
    val levelBit =
        when (entry.level) {
            LogLevel.Warning -> LEVEL_WARN
            LogLevel.Error,
            LogLevel.Fatal,
            -> LEVEL_ERROR
            else -> LEVEL_INFO
        }
    val categoryBit =
        when (entry.category) {
            LogCategory.Module,
            LogCategory.Native,
            -> CAT_MODULE
            LogCategory.Network -> CAT_NETWORK
            LogCategory.Display,
            LogCategory.Transition,
            -> CAT_DISPLAY
            LogCategory.Performance -> CAT_PERF
            LogCategory.Settings -> CAT_SETTINGS
            LogCategory.Other -> CAT_OTHER
        }
    return levelMask and levelBit != 0 && categoryMask and categoryBit != 0
}

private val LOG_META_FIELDS =
    setOf(
        "sequence",
        "sessionId",
        "uptimeMs",
        "healthSnapshot",
        "sampling",
    )

private val DETAIL_FIELD_PRIORITY = listOf(LOG_FIELD_REASON, LOG_FIELD_SOURCE, "traceId", "target", "owner")
private val BATTERY_DETAIL_PRIORITY = listOf(
    LOG_FIELD_TRANSFERRED_CHARGING, LOG_FIELD_NATIVE_CHARGING, LOG_FIELD_NATIVE_PERCENT, LOG_FIELD_SOURCE, LOG_FIELD_REASON,
)
