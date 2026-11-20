package com.chaners.guiyuan.ui.screens

import com.chaners.guiyuan.BuildConfig
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.UiStyle
import com.chaners.guiyuan.ui.LocalUiStyle
import com.chaners.guiyuan.ui.icons.Os4GlassShare
import com.chaners.guiyuan.settings.DiagLevel
import com.chaners.guiyuan.settings.DiagSettings
import com.chaners.guiyuan.settings.DiagRepo
import com.chaners.guiyuan.system.LogCategory
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
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.nav.core.LocalNavTransitionScope
import top.yukonga.miuix.kmp.nav.transition.NavRole

@OptIn(ExperimentalScrollBarApi::class)
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
                state = LogFilterState(levelFilter, categoryFilter),
                actions = LogFilterActions(
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
                ),
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
                        if (!shareDiagnosticReport(context, report, shareTitle)) {
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
                status = DiagMenuStatus(
                    diagnosticsLevel = selectedLevel,
                    canChangeLevel = !loading && !clearingHistory,
                    canScrollTop = !loading && listState.canScrollBackward,
                    canScrollBottom = !loading && listState.canScrollForward,
                    canClear = !loading && !clearingHistory && !reportBusy && !exportOpen &&
                        snapshot?.log?.sessionLines?.isNotEmpty() == true,
                ),
                actions = DiagMenuActions(
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
                ),
            )
        },
        listState = listState,
        // Use MIUIX's auto-fading scrollbar for the potentially long diagnostic log.
        // Keep the Root failure guide non-scrollable and free of decorative controls.
        foreground = { padding ->
            if (!showRootGuide) {
                VerticalScrollBar(
                    adapter = rememberScrollBarAdapter(listState),
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(
                        top = padding.calculateTopPadding(),
                        bottom = padding.calculateBottomPadding(),
                    ),
                )
            }
        },
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
        diagnosticsLogItems(
            view = DiagnosticsLogPresentation(
                snapshot = snapshot,
                entries = entries,
                filteredEntries = filteredEntries,
                showRootGuide = showRootGuide,
                loading = loading,
                filterActive = filterActive,
                captureTruncated = captureTruncated,
                detailStatus = detailStatus,
                detailActive = detailActive,
                integrityNotice = integrityNotice,
                expandedKey = expandedKey,
                rise = rise,
                risePx = risePx,
                placementSpec = placementSpec,
            ),
            context = context,
            onToggle = { rowKey -> expandedKey = if (expandedKey == rowKey) null else rowKey },
            onRetry = ::refresh,
        )
    }

}
