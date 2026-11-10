package com.chaners.guiyuan.ui.screens

import android.content.ClipData
import android.content.Context
import android.content.Intent
import com.chaners.guiyuan.BuildConfig
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.UiStyle
import com.chaners.guiyuan.ui.LocalUiStyle
import com.chaners.guiyuan.ui.icons.Os4GlassFilter
import com.chaners.guiyuan.ui.icons.Os4GlassMore
import com.chaners.guiyuan.ui.icons.Os4GlassShare
import com.chaners.guiyuan.settings.DiagLevel
import com.chaners.guiyuan.settings.DiagSettings
import com.chaners.guiyuan.settings.DiagRepo
import com.chaners.guiyuan.system.LogCategory
import com.chaners.guiyuan.system.LogEntry
import com.chaners.guiyuan.system.LogLevel
import com.chaners.guiyuan.system.DiagLogReader
import com.chaners.guiyuan.system.DiagSnapshot
import com.chaners.guiyuan.system.DiagReport
import com.chaners.guiyuan.system.DiagFiles
import com.chaners.guiyuan.ui.components.UiOverlayDialog
import com.chaners.guiyuan.ui.components.UiDialogButton
import com.chaners.guiyuan.ui.components.dialogActionGap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.anim.folmeSpring
import top.yukonga.miuix.kmp.basic.Badge
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.BadgedBox
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.IconButtonDefaults
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.menu.WindowIconCascadingDropdownMenu
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.nav.core.LocalNavTransitionScope
import top.yukonga.miuix.kmp.nav.transition.NavRole
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DiagnosticsScreen(
    onBack: () -> Unit,
    cachedSnapshot: DiagSnapshot?,
    onSnapshot: (DiagSnapshot) -> Unit,
) {
    val context = LocalContext.current
    val navScope = LocalNavTransitionScope.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val placementSpec = remember { folmeSpring<IntOffset>(damping = 1f, response = 0.3f) }
    val diagRepo =
        remember(context.applicationContext) {
            DiagRepo(context.applicationContext)
        }
    val diagSettings by
        diagRepo.settings.collectAsState(
            initial = DiagSettings(level = diagRepo.current()),
        )

    var snapshot by remember { mutableStateOf(cachedSnapshot) }
    val initialSnapshot = remember { cachedSnapshot }
    val rise = remember(snapshot) {
        Animatable(if (snapshot == null || snapshot === initialSnapshot) 1f else 0f)
    }
    LaunchedEffect(rise) {
        if (rise.value == 0f) {
            withFrameNanos { } // Paint the initial position before the spring starts.
            rise.animateTo(1f, folmeSpring(damping = 1f, response = 0.55f))
        }
    }
    val risePx = with(LocalDensity.current) { 28.dp.toPx() }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var clearConfirmOpen by rememberSaveable { mutableStateOf(false) }
    var clearingHistory by remember { mutableStateOf(false) }
    var expandedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var reportBusy by remember { mutableStateOf(false) }
    var exportOpen by rememberSaveable { mutableStateOf(false) }
    var levelFilter by rememberSaveable {
        mutableIntStateOf(LEVEL_ALL)
    }
    var categoryFilter by rememberSaveable {
        mutableIntStateOf(CAT_ALL)
    }

    val exportOkMsg = stringResource(R.string.diagnostic_report_exported)
    val exportFailMsg = stringResource(R.string.diagnostic_report_export_failed)
    val shareFailMsg = stringResource(R.string.diagnostic_report_share_failed)
    val shareTitle = stringResource(R.string.share_diagnostic_report)
    val clearedMsg = stringResource(R.string.diagnostics_history_cleared)
    val clearFailedMsg = stringResource(R.string.diagnostics_history_clear_failed)
    val refreshFailedMsg = stringResource(R.string.diagnostics_history_refresh_failed)
    val exportTitle = stringResource(R.string.export_diagnostic_report)
    val menuTitle = stringResource(R.string.diagnostics_more_actions)
    val filterTitle = stringResource(R.string.diagnostics_filter)
    val refreshTexts =
        listOf(
            stringResource(R.string.diagnostics_pull_to_refresh),
            stringResource(R.string.diagnostics_release_to_refresh),
            stringResource(R.string.diagnostics_refreshing),
            stringResource(R.string.diagnostics_refresh_complete),
        )

    suspend fun awaitNavIdle() {
        snapshotFlow { navScope.role == NavRole.Top && !navScope.isRunning }.first { it }
    }

    suspend fun captureSnapshot() {
        try {
            val captured = DiagSnapshot.capture(context.applicationContext)
            awaitNavIdle()
            snapshot = captured
            onSnapshot(captured)
            expandedKey = null
        } finally {
            loading = false
            refreshing = false
        }
    }

    fun reloadSnapshot() {
        if (loading) return
        loading = true
        scope.launch { captureSnapshot() }
    }

    fun refresh() {
        if (loading) return
        loading = true
        refreshing = true
        scope.launch { captureSnapshot() }
    }

    fun withReport(onReady: suspend (String) -> Unit) {
        val captured = snapshot ?: return
        if (reportBusy) return
        reportBusy = true
        scope.launch {
            try {
                val report = withContext(Dispatchers.Default) {
                    DiagReport.build(captured)
                }
                onReady(report)
            } finally {
                reportBusy = false
            }
        }
    }

    val exportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("text/plain"),
        ) { uri ->
            exportOpen = false
            if (uri != null) {
                withReport { report ->
                    val success =
                        DiagFiles.write(
                            context = context,
                            uri = uri,
                            report = report,
                        )
                    snackbarHostState.showSnackbar(
                        if (success) exportOkMsg else exportFailMsg,
                    )
                }
            }
        }

    LaunchedEffect(Unit) {
        // Wait for the native page transition before starting root log capture.
        awaitNavIdle()
        captureSnapshot()
    }

    val reportEnabled =
        snapshot != null &&
            !loading &&
            !reportBusy &&
            !exportOpen
    val focusedCategory =
        when (categoryFilter) {
            CAT_PERF -> LogCategory.Performance
            CAT_DISPLAY -> LogCategory.Display
            else -> null
        }
    val entries = remember(snapshot, focusedCategory) {
        snapshot?.entries.orEmpty().asReversed().filter { isRuntimeLog(it, focusedCategory) }
    }
    val filteredEntries = remember(entries, levelFilter, categoryFilter) {
        entries.filter { matchesFilter(it, levelFilter, categoryFilter) }
    }
    val filterActive =
        levelFilter != LEVEL_ALL ||
            categoryFilter != CAT_ALL
    // The preference write is synchronous; do not wait for the observer flow while switching.
    val selectedLevel = if (loading) diagRepo.current() else diagSettings.level
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
    SettingsPage(
        title = stringResource(R.string.diagnostics_title),
        onBack = onBack,
        keepCompactTitle = showRootGuide,
        titlePadding = if (LocalUiStyle.current == UiStyle.OS4) TopAppBarDefaults.TitlePadding else 0.dp,
        glassActions = true,
        snackbarHost = { SnackbarHost(state = snackbarHostState) },
        navigationActions = {
            LogFilterMenu(
                title = filterTitle,
                levelMask = levelFilter,
                categoryMask = categoryFilter,
                onLevelMaskChange = {
                    levelFilter = it
                    expandedKey = null
                },
                onCategoryMaskChange = {
                    categoryFilter = it
                    expandedKey = null
                },
                onReset = {
                    levelFilter = LEVEL_ALL
                    categoryFilter = CAT_ALL
                    expandedKey = null
                },
            )
        },
        actions = {
            DiagActionButton(
                onClick = {
                    exportOpen = true
                    exportLauncher.launch(DiagFiles.fileName())
                },
                onLongClick = {
                    withReport { report ->
                        val prepared =
                            DiagFiles.prepare(
                                context = context,
                                report = report,
                            )
                        if (prepared == null) {
                            snackbarHostState.showSnackbar(shareFailMsg)
                            return@withReport
                        }
                        val sendIntent =
                            Intent(Intent.ACTION_SEND).apply {
                                type = DiagFiles.MIME_TYPE
                                putExtra(Intent.EXTRA_STREAM, prepared.uri)
                                clipData =
                                    ClipData.newUri(
                                        context.contentResolver,
                                        shareTitle,
                                        prepared.uri,
                                    )
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                        DiagFiles.logIntent(sendIntent, prepared.uri)
                        val chooserIntent =
                            Intent.createChooser(sendIntent, shareTitle).apply {
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                        runCatching { context.startActivity(chooserIntent) }
                            .onSuccess { DiagFiles.logChooser() }
                            .onFailure { error ->
                                DiagFiles.logChooser(error)
                                DiagFiles.discard(context, prepared)
                                snackbarHostState.showSnackbar(shareFailMsg)
                            }
                    }
                },
                longClickLabel = shareTitle,
                enabled = reportEnabled,
            ) {
                Icon(
                    imageVector = if (LocalUiStyle.current == UiStyle.OS4) Os4GlassShare else MiuixIcons.Share,
                    contentDescription = exportTitle,
                    modifier = Modifier.size(if (LocalUiStyle.current == UiStyle.OS4) 28.dp else 24.dp),
                )
            }
            DiagMenu(
                title = menuTitle,
                diagnosticsLevel = selectedLevel,
                canChangeLevel = !loading && !clearingHistory,
                canScrollTop = !loading && listState.canScrollBackward,
                canScrollBottom = !loading && listState.canScrollForward,
                canClear = !loading && !clearingHistory && !reportBusy && !exportOpen &&
                    snapshot?.log?.sessionLines?.isNotEmpty() == true,
                onDiagLevelChange = { level ->
                    if (!loading && level != diagRepo.current()) {
                        diagRepo.setLevel(level)
                        reloadSnapshot()
                    }
                },
                onScrollTop = {
                    scope.launch { listState.animateScrollToItem(0) }
                },
                onScrollBottom = {
                    scope.launch {
                        val lastIndex = listState.layoutInfo.totalItemsCount - 1
                        if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
                    }
                },
                onClear = { clearConfirmOpen = true },
            )
        },
        listState = listState,
        userScrollEnabled = !showRootGuide,
        // The Root failure guide has a retry action; no pull gesture in this state.
        pullToRefresh =
            if (showRootGuide) {
                null
            } else {
                SettingsPullToRefresh(
                    refreshing = refreshing,
                    onRefresh = ::refresh,
                    texts = refreshTexts,
                )
            },
        overlay = {
            UiOverlayDialog(
                show = clearConfirmOpen,
                title = stringResource(R.string.diagnostics_clear_history),
                summary = stringResource(R.string.diagnostics_history_clear_confirm),
                onDismissRequest = { if (!clearingHistory) clearConfirmOpen = false },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dialogActionGap()),
                ) {
                    UiDialogButton(
                        text = stringResource(R.string.cancel),
                        modifier = Modifier.weight(1f),
                        enabled = !clearingHistory,
                        onClick = { clearConfirmOpen = false },
                    )
                    UiDialogButton(
                        text = stringResource(R.string.diagnostics_clear_history_action),
                        modifier = Modifier.weight(1f),
                        enabled = !clearingHistory,
                        role = GlassDialogButtonRole.Destructive,
                        onClick = {
                            if (!clearingHistory && !loading) {
                                scope.launch {
                                    clearingHistory = true
                                    val cleared = runCatching {
                                        DiagLogReader.clearHistory(context.applicationContext)
                                    }.getOrDefault(false)
                                    clearConfirmOpen = false
                                    val feedback =
                                        if (cleared) {
                                            loading = true
                                            val refreshed = runCatching { captureSnapshot() }.isSuccess
                                            listState.scrollToItem(0)
                                            if (refreshed) clearedMsg else refreshFailedMsg
                                        } else {
                                            clearFailedMsg
                                        }
                                    clearingHistory = false
                                    snackbarHostState.showSnackbar(feedback)
                                }
                            }
                        },
                    )
                }
            }
        },
    ) {
        if (!showRootGuide) {
            // A changing notice must not insert a new key before the summary and shift the title.
            item(key = "diagnostics-header") {
                Column(
                    modifier = Modifier.graphicsLayer {
                        val shown = logProgress(rise.value, 0)
                        translationY = risePx * (1f - shown)
                        alpha = shown
                    },
                ) {
                    if (filteredEntries.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.diagnostics_events_summary, filteredEntries.size),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .padding(top = 6.dp, bottom = 4.dp),
                            style = MiuixTheme.textStyles.subtitle,
                            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                        )
                    }
                    AnimatedContent(
                        targetState = detailStatus to detailActive,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "diagnostics-detail-status",
                    ) { (status, active) ->
                        if (status != null) {
                            Text(
                                text = status,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                                style = if (active) MiuixTheme.textStyles.footnote1 else MiuixTheme.textStyles.body2,
                                color = if (active) {
                                    MiuixTheme.colorScheme.onSurfaceVariantSummary
                                } else {
                                    MiuixTheme.colorScheme.onSurfaceSecondary
                                },
                            )
                        }
                    }
                    if (integrityNotice.isNotEmpty()) {
                        Text(
                            text = integrityNotice,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        )
                    }
                }
            }
        }
        when {
            snapshot == null -> {
                item(key = "diagnostics-loading") {
                    if (loading) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 72.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.diagnostics_loading),
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                            )
                        }
                    } else {
                        LogStateCard(text = stringResource(R.string.diagnostics_events_empty))
                    }
                }
            }
            filteredEntries.isNotEmpty() -> {
                itemsIndexed(
                    items = filteredEntries,
                    key = { index, entry -> logRowKey(entry, index) },
                ) { index, entry ->
                    val rowKey = logRowKey(entry, index)
                    LogCard(
                        context = context,
                        entry = entry,
                        expanded = expandedKey == rowKey,
                        onToggle = {
                            expandedKey =
                                if (expandedKey == rowKey) {
                                    null
                                } else {
                                    rowKey
                                }
                        },
                        modifier =
                            Modifier
                                .animateItem(
                                    fadeInSpec = null,
                                    placementSpec = placementSpec,
                                    fadeOutSpec = null,
                                )
                                .graphicsLayer {
                                    val shown = logProgress(rise.value, index)
                                    translationY = risePx * (1f - shown)
                                    alpha = shown
                                },
                    )
                }
                item(key = "diagnostics-history-end") {
                    Text(
                        text = stringResource(
                            when {
                                captureTruncated && filterActive -> R.string.diagnostics_events_end_truncated_filtered
                                captureTruncated -> R.string.diagnostics_events_end_truncated
                                filterActive -> R.string.diagnostics_events_end_filtered
                                else -> R.string.diagnostics_events_end
                            },
                        ),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            showRootGuide -> {
                item(key = "diagnostics-root-unavailable") {
                    LogEmptyGuide(
                        title = stringResource(R.string.diagnostics_root_unavailable_title),
                        description = stringResource(R.string.diagnostics_root_unavailable_message),
                        note = listOfNotNull(
                            stringResource(R.string.diagnostics_root_unavailable_export),
                            detailStatus,
                            integrityNotice.takeIf(String::isNotEmpty),
                        ).joinToString("\n"),
                        icon = MiuixIcons.Medium.Report,
                        busy = loading,
                        onRetry = ::refresh,
                        modifier = Modifier.fillParentMaxSize(),
                    )
                }
            }
            entries.isEmpty() && filterActive -> {
                item(key = "diagnostics-state-filter-empty") {
                    LogStateCard(text = stringResource(R.string.diagnostics_filter_empty))
                }
            }
            entries.isEmpty() -> {
                item(key = "diagnostics-no-events") {
                    LogEmptyGuide(
                        title = stringResource(R.string.diagnostics_events_empty),
                        description = stringResource(
                            if (snapshot?.log?.clearedAt != null) {
                                R.string.diagnostics_history_empty
                            } else {
                                R.string.diagnostics_no_events_hint
                            },
                        ),
                        note = null,
                        icon = MiuixIcons.Info,
                        busy = loading,
                        onRetry = ::refresh,
                        modifier = Modifier.fillParentMaxSize(),
                    )
                }
            }
            else -> {
                item(key = "diagnostics-state-empty") {
                    LogStateCard(text = stringResource(R.string.diagnostics_filter_empty))
                }
            }
        }
    }

}

private fun logProgress(progress: Float, index: Int): Float {
    // Off-screen rows share the last visible row's start.
    val start = index.coerceAtMost(7) * 0.07f
    return ((progress - start) / (1f - start)).coerceIn(0f, 1f)
}

internal const val LEVEL_INFO = 1 shl 0
internal const val LEVEL_WARN = 1 shl 1
internal const val LEVEL_ERROR = 1 shl 2
private const val LEVEL_ALL =
    LEVEL_INFO or LEVEL_WARN or LEVEL_ERROR

internal const val CAT_MODULE = 1 shl 0
internal const val CAT_NETWORK = 1 shl 1
internal const val CAT_DISPLAY = 1 shl 2
internal const val CAT_PERF = 1 shl 3
internal const val CAT_SETTINGS = 1 shl 4
internal const val CAT_OTHER = 1 shl 5
private const val CAT_ALL =
    CAT_MODULE or
        CAT_NETWORK or
        CAT_DISPLAY or
        CAT_PERF or
        CAT_SETTINGS or
        CAT_OTHER

private fun toggleMask(
    mask: Int,
    bit: Int,
    checked: Boolean,
): Int =
    if (checked) {
        mask or bit
    } else {
        mask and bit.inv()
    }

private data class FilterOption(
    @StringRes val titleRes: Int,
    val bit: Int,
)

private val LevelOptions =
    listOf(
        FilterOption(R.string.diagnostics_filter_info, LEVEL_INFO),
        FilterOption(R.string.diagnostics_filter_warning, LEVEL_WARN),
        FilterOption(R.string.diagnostics_filter_error, LEVEL_ERROR),
    )

private val CategoryOptions =
    listOf(
        FilterOption(
            R.string.diagnostics_filter_module_compatibility,
            CAT_MODULE,
        ),
        FilterOption(R.string.diagnostics_filter_network, CAT_NETWORK),
        FilterOption(
            R.string.diagnostics_filter_display_transition,
            CAT_DISPLAY,
        ),
        FilterOption(R.string.diagnostics_filter_performance, CAT_PERF),
        FilterOption(
            R.string.diagnostics_filter_settings_maintenance,
            CAT_SETTINGS,
        ),
        FilterOption(R.string.diagnostics_filter_other, CAT_OTHER),
    )

@Composable
private fun DiagActionButton(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    longClickLabel: String,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    if (LocalUiStyle.current == UiStyle.OS4) {
        GlassIconButton(
            onClick = onClick,
            onLongClick = onLongClick,
            onLongClickLabel = longClickLabel,
            enabled = enabled,
            content = content,
        )
    } else {
        // MIUIX's IconButton is click-only; this retains its surface and one gesture owner.
        Box(
            modifier = Modifier
                .defaultMinSize(
                    minWidth = IconButtonDefaults.MinWidth,
                    minHeight = IconButtonDefaults.MinHeight,
                )
                .squircleSurface(color = Color.Unspecified, cornerRadius = IconButtonDefaults.CornerRadius)
                .combinedClickable(
                    enabled = enabled,
                    role = Role.Button,
                    onLongClickLabel = longClickLabel,
                    onLongClick = onLongClick,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun filterOptionItems(
    options: List<FilterOption>,
    mask: Int,
    onMaskChange: (Int) -> Unit,
): List<DropdownItem> =
    options.map { option ->
        val selected = mask and option.bit != 0
        DropdownItem(
            text = stringResource(option.titleRes),
            selected = selected,
            onClick = {
                onMaskChange(
                    toggleMask(
                        mask = mask,
                        bit = option.bit,
                        checked = !selected,
                    ),
                )
            },
        )
    }

@Composable
private fun logFilterEntries(
    levelItems: List<DropdownItem>,
    categoryItems: List<DropdownItem>,
    filterActive: Boolean,
    onReset: () -> Unit,
): List<DropdownEntry> =
    listOf(
        DropdownEntry(
            items =
                listOf(
                    DropdownItem(
                        text = stringResource(R.string.diagnostics_filter_levels),
                        children = levelItems,
                    ),
                    DropdownItem(
                        text = stringResource(R.string.diagnostics_filter_categories),
                        children = categoryItems,
                    ),
                ),
        ),
        DropdownEntry(
            items =
                listOf(
                    DropdownItem(
                        text = stringResource(R.string.diagnostics_filter_reset),
                        enabled = filterActive,
                        onClick = onReset,
                    ),
                ),
        ),
    )

@Composable
private fun LogFilterMenu(
    title: String,
    levelMask: Int,
    categoryMask: Int,
    onLevelMaskChange: (Int) -> Unit,
    onCategoryMaskChange: (Int) -> Unit,
    onReset: () -> Unit,
) {
    val filterActive =
        levelMask != LEVEL_ALL ||
            categoryMask != CAT_ALL
    val levelItems = filterOptionItems(LevelOptions, levelMask, onLevelMaskChange)
    val categoryItems = filterOptionItems(CategoryOptions, categoryMask, onCategoryMaskChange)
    val entries = logFilterEntries(levelItems, categoryItems, filterActive, onReset)

    val icon: @Composable () -> Unit = {
        BadgedBox(
            badge = {
                AnimatedVisibility(
                    visible = filterActive,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Badge(containerColor = MiuixTheme.colorScheme.primary)
                }
            },
        ) {
            Icon(
                imageVector = if (LocalUiStyle.current == UiStyle.OS4) Os4GlassFilter else MiuixIcons.Filter,
                contentDescription = title,
                modifier = Modifier.size(if (LocalUiStyle.current == UiStyle.OS4) 28.dp else 24.dp),
            )
        }
    }

    TooltipBox(text = title) {
        if (LocalUiStyle.current == UiStyle.OS4 && isRuntimeShaderSupported()) {
            Os4CascadingMenu(entries = entries, collapseOnSelection = false, content = icon)
        } else {
            WindowIconCascadingDropdownMenu(
                entries = entries,
                collapseOnSelection = false,
                content = icon,
            )
        }
    }
}

@Composable
private fun diagLevelEntry(
    diagnosticsLevel: DiagLevel,
    canChangeLevel: Boolean,
    onDiagLevelChange: (DiagLevel) -> Unit,
): DropdownEntry {
    val currentLevelLabel =
        stringResource(
            if (diagnosticsLevel == DiagLevel.Detailed) {
                R.string.diagnostics_mode_detailed
            } else {
                R.string.diagnostics_mode_basic
            },
        )
    val levelItems =
        DiagLevel.entries.map { level ->
            DropdownItem(
                text =
                    stringResource(
                        if (level == DiagLevel.Detailed) {
                            R.string.diagnostics_mode_detailed
                        } else {
                            R.string.diagnostics_mode_basic
                        },
                    ),
                selected = diagnosticsLevel == level,
                enabled = canChangeLevel,
                onClick = { onDiagLevelChange(level) },
            )
        }
    return DropdownEntry(
        items =
            listOf(
                DropdownItem(
                    text = stringResource(R.string.diagnostics_mode_title),
                    summary = currentLevelLabel,
                    enabled = canChangeLevel,
                    children = levelItems,
                ),
            ),
    )
}

@Composable
private fun DiagMenu(
    title: String,
    diagnosticsLevel: DiagLevel,
    canChangeLevel: Boolean,
    canScrollTop: Boolean,
    canScrollBottom: Boolean,
    canClear: Boolean,
    onDiagLevelChange: (DiagLevel) -> Unit,
    onScrollTop: () -> Unit,
    onScrollBottom: () -> Unit,
    onClear: () -> Unit,
) {
    val entries =
        listOf(
            diagLevelEntry(diagnosticsLevel, canChangeLevel, onDiagLevelChange),
            DropdownEntry(
                items =
                    listOf(
                        DropdownItem(
                            text = stringResource(R.string.diagnostics_scroll_top),
                            enabled = canScrollTop,
                            onClick = onScrollTop,
                        ),
                        DropdownItem(
                            text = stringResource(R.string.diagnostics_scroll_bottom),
                            enabled = canScrollBottom,
                            onClick = onScrollBottom,
                        ),
                    ),
            ),
            DropdownEntry(
                items =
                    listOf(
                        DropdownItem(
                            text = stringResource(R.string.diagnostics_clear_history),
                            enabled = canClear,
                            onClick = onClear,
                        ),
                    ),
            ),
        )

    val icon: @Composable () -> Unit = {
        Icon(
            imageVector = if (LocalUiStyle.current == UiStyle.OS4) Os4GlassMore else MiuixIcons.More,
            contentDescription = title,
            modifier = Modifier.size(if (LocalUiStyle.current == UiStyle.OS4) 28.dp else 24.dp),
        )
    }
    TooltipBox(text = title) {
        if (LocalUiStyle.current == UiStyle.OS4 && isRuntimeShaderSupported()) {
            Os4CascadingMenu(entries = entries, content = icon)
        } else {
            WindowIconCascadingDropdownMenu(entries = entries, content = icon)
        }
    }
}

@Composable
private fun LogEmptyGuide(
    title: String,
    description: String,
    note: String?,
    icon: ImageVector,
    busy: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 340.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier.size(72.dp).background(
                        color = MiuixTheme.colorScheme.surfaceContainerHighest,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceContainerHighest,
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = title,
                style = MiuixTheme.textStyles.title3.copy(fontWeight = FontWeight.SemiBold),
                color = MiuixTheme.colorScheme.onSurfaceContainer,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = description,
                modifier = Modifier.fillMaxWidth(),
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                textAlign = TextAlign.Start,
            )
            note?.let {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = it,
                    modifier = Modifier.fillMaxWidth(),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Start,
                )
            }
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onRetry,
                enabled = !busy,
                modifier = Modifier.widthIn(min = 176.dp),
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(
                    text = stringResource(R.string.diagnostics_retry_logs),
                    style = MiuixTheme.textStyles.button,
                )
            }
        }
    }
}
