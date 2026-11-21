package com.chaners.guiyuan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.BuildConfig
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.DiagLevel
import com.chaners.guiyuan.system.DiagReport
import com.chaners.guiyuan.system.DiagSnapshot
import com.chaners.guiyuan.system.LogCategory
import com.chaners.guiyuan.system.LogEntry

/**
 * Read-only log presentation values, derived from the current snapshot and UI filters.
 * Snapshot capture, navigation synchronization, filtering preferences and export actions
 * stay owned by DiagnosticsScreen.
 */
internal class DiagnosticsLogReadout(
    val entries: List<LogEntry>,
    val filteredEntries: List<LogEntry>,
    val filterActive: Boolean,
    val showRootGuide: Boolean,
    val captureTruncated: Boolean,
    val detailStatus: String?,
    val detailActive: Boolean,
    val integrityNotice: String,
)

@Composable
internal fun diagnosticsLogReadout(
    snapshot: DiagSnapshot?,
    selectedLevel: DiagLevel,
    loading: Boolean,
    levelFilter: Int,
    categoryFilter: Int,
): DiagnosticsLogReadout {
    val focusedCategory =
        when (categoryFilter) {
            CAT_PERF -> LogCategory.Performance
            CAT_DISPLAY -> LogCategory.Display
            else -> null
        }
    val entries = remember(snapshot, focusedCategory) {
        snapshot?.entries.orEmpty().asReversed().filter { isRuntimeLog(it, focusedCategory) }
    }
    val filterActive =
        levelFilter != LEVEL_ALL ||
            categoryFilter != CAT_ALL
    val filteredEntries = remember(entries, levelFilter, categoryFilter) {
        if (filterActive) {
            entries.filter { matchesFilter(it, levelFilter, categoryFilter) }
        } else {
            entries
        }
    }
    val collectionFailed = snapshot?.log?.result?.isSuccess == false
    val hasCapturedSessionLines = snapshot?.log?.sessionLines?.isNotEmpty() == true
    val captureTruncated = snapshot?.log?.result?.truncated == true && hasCapturedSessionLines
    val omittedEarlierLines = snapshot?.let(DiagReport::omittedEarlierLines) ?: 0
    val detailedConfirmed = remember(snapshot) { snapshot?.isRuntimeDetailConfirmed() == true }
    val showRootGuide = !hasCapturedSessionLines && collectionFailed
    val detailStatus =
        if (selectedLevel == DiagLevel.Detailed) {
            stringResource(
                when {
                    !BuildConfig.RUNTIME_DIAGNOSTICS -> R.string.diagnostics_detail_unavailable
                    loading -> R.string.diagnostics_detail_pending
                    snapshot?.level == DiagLevel.Detailed && detailedConfirmed ->
                        R.string.diagnostics_detail_confirmed
                    else -> R.string.diagnostics_detail_unconfirmed
                },
            )
        } else {
            null
        }
    val detailActive = detailStatus != null && !loading &&
        snapshot?.level == DiagLevel.Detailed && detailedConfirmed && BuildConfig.RUNTIME_DIAGNOSTICS
    val integrityNotice =
        listOfNotNull(
            if (collectionFailed && hasCapturedSessionLines) {
                stringResource(R.string.diagnostics_collection_incomplete)
            } else null,
            when {
                captureTruncated && omittedEarlierLines > 0 ->
                    stringResource(R.string.diagnostics_capture_and_report_limited, omittedEarlierLines)
                captureTruncated ->
                    stringResource(R.string.diagnostics_capture_truncated)
                omittedEarlierLines > 0 ->
                    stringResource(R.string.diagnostics_report_lines_omitted, omittedEarlierLines)
                else -> null
            },
        ).joinToString("\n")
    return DiagnosticsLogReadout(
        entries = entries,
        filteredEntries = filteredEntries,
        filterActive = filterActive,
        showRootGuide = showRootGuide,
        captureTruncated = captureTruncated,
        detailStatus = detailStatus,
        detailActive = detailActive,
        integrityNotice = integrityNotice,
    )
}
