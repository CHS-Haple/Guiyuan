package com.chaners.guiyuan.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.ui.icons.Os4GlassShare
import com.chaners.guiyuan.settings.DiagLevel
import com.chaners.guiyuan.settings.DiagSettings
import com.chaners.guiyuan.settings.DiagRepo
import com.chaners.guiyuan.system.DiagLogReader
import com.chaners.guiyuan.system.DiagSnapshot
import com.chaners.guiyuan.system.DiagReport
import com.chaners.guiyuan.system.DiagFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.anim.folmeSpring
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.nav.core.LocalNavTransitionScope
import top.yukonga.miuix.kmp.nav.transition.NavRole

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

    fun reloadSnapshot(showRefreshIndicator: Boolean = false) {
        if (loading) return
        loading = true
        if (showRefreshIndicator) refreshing = true
        scope.launch { captureSnapshot() }
    }

    fun withReport(onReady: suspend (String) -> Unit) {
        if (snapshot == null || reportBusy || loading) return
        reportBusy = true
        scope.launch {
            try {
                // Share/export must capture the current SystemUI session, not the
                // screen snapshot taken when Diagnostics was opened.
                val captured = DiagSnapshot.capture(context.applicationContext)
                snapshot = captured
                onSnapshot(captured)
                expandedKey = null
                val report = withContext(Dispatchers.Default) {
                    DiagReport.build(captured)
                }
                onReady(report)
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Exception) {
                snackbarHostState.showSnackbar(refreshFailedMsg)
            } finally {
                reportBusy = false
            }
        }
    }

    fun clearHistory() {
        if (clearingHistory || loading) return
        scope.launch {
            clearingHistory = true
            val feedback = try {
                val cleared = runCatchingUiOperation {
                    DiagLogReader.clearHistory(context.applicationContext)
                }.getOrDefault(false)
                clearConfirmOpen = false
                if (cleared) {
                    loading = true
                    val refreshed = runCatchingUiOperation { captureSnapshot() }.isSuccess
                    listState.scrollToItem(0)
                    if (refreshed) clearedMsg else refreshFailedMsg
                } else {
                    clearFailedMsg
                }
            } finally {
                // Always release the local operation owner, including on cancellation.
                clearingHistory = false
            }
            // Preserve the original order: dismiss the busy state before Snackbar suspends.
            snackbarHostState.showSnackbar(feedback)
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
    // The preference write is synchronous; do not wait for the observer flow while switching.
    val selectedLevel = if (loading) diagRepo.current() else diagSettings.level
    val filteredLog = diagnosticsFilteredLog(
        snapshot = snapshot,
        levelFilter = levelFilter,
        categoryFilter = categoryFilter,
    )
    val captureStatus = diagnosticsCaptureStatus(
        snapshot = snapshot,
        selectedLevel = selectedLevel,
        loading = loading,
    )
    SettingsPage(
        title = stringResource(R.string.diagnostics_title),
        onBack = onBack,
        keepCompactTitle = captureStatus.showRootGuide,
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
                    imageVector = Os4GlassShare,
                    contentDescription = exportTitle,
                    modifier = Modifier.size(28.dp),
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
        userScrollEnabled = !captureStatus.showRootGuide,
        // The Root failure guide has a retry action; no pull gesture in this state.
        pullToRefresh =
            if (captureStatus.showRootGuide) {
                null
            } else {
                SettingsPullToRefresh(
                    refreshing = refreshing,
                    onRefresh = { reloadSnapshot(showRefreshIndicator = true) },
                    texts = refreshTexts,
                )
            },
        overlay = {
            DiagnosticsClearHistoryDialog(
                show = clearConfirmOpen,
                busy = clearingHistory,
                onDismiss = { clearConfirmOpen = false },
                onClear = ::clearHistory,
            )
        },
    ) {
        diagnosticsLogItems(
            view = DiagnosticsLogPresentation(
                snapshot = snapshot,
                filteredLog = filteredLog,
                captureStatus = captureStatus,
                loading = loading,
                expandedKey = expandedKey,
                motion = DiagnosticsLogMotion(
                    rise = rise,
                    risePx = risePx,
                    placementSpec = placementSpec,
                ),
            ),
            context = context,
            onToggle = { rowKey -> expandedKey = if (expandedKey == rowKey) null else rowKey },
            onRetry = { reloadSnapshot(showRefreshIndicator = true) },
        )
    }

}
