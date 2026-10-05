package com.chaners.guiyuan.ui.screens

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.DiagnosticsLevel
import com.chaners.guiyuan.settings.DiagnosticsSettings
import com.chaners.guiyuan.settings.DiagnosticsRepo
import com.chaners.guiyuan.system.DiagnosticLogCategory
import com.chaners.guiyuan.system.DiagnosticLogEntry
import com.chaners.guiyuan.system.DiagnosticLogLevel
import com.chaners.guiyuan.system.DiagnosticsSnapshot
import com.chaners.guiyuan.system.DiagnosticsSnapshotProvider
import com.chaners.guiyuan.system.DiagnosticsReportBuilder
import com.chaners.guiyuan.system.DiagnosticsReportFiles
import com.chaners.guiyuan.ui.theme.RuntimeWarningAccent
import kotlinx.coroutines.launch
import java.util.Locale
import top.yukonga.miuix.kmp.basic.Badge
import top.yukonga.miuix.kmp.basic.BadgedBox
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.menu.WindowIconCascadingDropdownMenu
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DiagnosticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val diagnosticsRepository =
        remember(context.applicationContext) {
            DiagnosticsRepo(context.applicationContext)
        }
    val diagnosticsSettings by
        diagnosticsRepository.settings.collectAsState(
            initial = DiagnosticsSettings(level = diagnosticsRepository.currentLevel()),
        )

    var snapshot by remember { mutableStateOf<DiagnosticsSnapshot?>(null) }
    var loading by remember { mutableStateOf(true) }
    var pullRefreshing by remember { mutableStateOf(false) }
    var viewCleared by rememberSaveable { mutableStateOf(false) }
    var expandedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var refreshGeneration by rememberSaveable { mutableIntStateOf(0) }
    var reportInProgress by rememberSaveable { mutableStateOf(false) }
    var exportPickerOpen by rememberSaveable { mutableStateOf(false) }
    var levelFilter by rememberSaveable {
        mutableIntStateOf(DiagnosticsFilterLevelAll)
    }
    var categoryFilter by rememberSaveable {
        mutableIntStateOf(DiagnosticsFilterCategoryAll)
    }

    val exportSucceededMessage = stringResource(R.string.diagnostic_report_exported)
    val exportFailedMessage = stringResource(R.string.diagnostic_report_export_failed)
    val shareFailedMessage = stringResource(R.string.diagnostic_report_share_failed)
    val reportShareTitle = stringResource(R.string.share_diagnostic_report)
    val reportExportTitle = stringResource(R.string.export_diagnostic_report)
    val moreActionsTitle = stringResource(R.string.diagnostics_more_actions)
    val filterTitle = stringResource(R.string.diagnostics_filter)
    val pullRefreshTexts =
        listOf(
            stringResource(R.string.diagnostics_pull_to_refresh),
            stringResource(R.string.diagnostics_release_to_refresh),
            stringResource(R.string.diagnostics_refreshing),
            stringResource(R.string.diagnostics_refresh_complete),
        )

    fun requestRefresh() {
        if (loading) return
        loading = true
        pullRefreshing = true
        refreshGeneration += 1
    }

    fun withCurrentReport(onReady: suspend (String) -> Unit) {
        val captured = snapshot ?: return
        if (reportInProgress) return
        reportInProgress = true
        scope.launch {
            try {
                onReady(DiagnosticsReportBuilder.build(captured))
            } finally {
                reportInProgress = false
            }
        }
    }

    val exportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("text/plain"),
        ) { uri ->
            exportPickerOpen = false
            if (uri != null) {
                withCurrentReport { report ->
                    val success =
                        DiagnosticsReportFiles.writeExport(
                            context = context,
                            uri = uri,
                            report = report,
                        )
                    snackbarHostState.showSnackbar(
                        if (success) exportSucceededMessage else exportFailedMessage,
                    )
                }
            }
        }

    LaunchedEffect(refreshGeneration) {
        loading = true
        try {
            snapshot = DiagnosticsSnapshotProvider.capture(context.applicationContext)
            expandedKey = null
            viewCleared = false
        } finally {
            loading = false
            pullRefreshing = false
        }
    }

    val reportActionsEnabled =
        snapshot != null &&
            !loading &&
            !reportInProgress &&
            !exportPickerOpen
    val runtimeEntries =
        if (viewCleared) {
            emptyList()
        } else {
            snapshot
                ?.sessionEntries
                .orEmpty()
                .asSequence()
                .filter(::diagnosticLogIsRuntimeEntry)
                .toList()
                .asReversed()
        }
    val usefulEntries =
        runtimeEntries
            .asSequence()
            .filter { entry ->
                diagnosticsFilterMatches(
                    entry = entry,
                    levelMask = levelFilter,
                    categoryMask = categoryFilter,
                )
            }
            .take(MaxDiagnosticsUsefulEvents)
            .toList()
    val filterActive =
        levelFilter != DiagnosticsFilterLevelAll ||
            categoryFilter != DiagnosticsFilterCategoryAll
    SettingsPage(
        title = stringResource(R.string.diagnostics_title),
        onBack = onBack,
        titlePadding = 0.dp,
        snackbarHost = { SnackbarHost(state = snackbarHostState) },
        navigationActions = {
            DiagnosticsFilterMenu(
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
                    levelFilter = DiagnosticsFilterLevelAll
                    categoryFilter = DiagnosticsFilterCategoryAll
                    expandedKey = null
                },
            )
        },
        actions = {
            TooltipBox(text = reportShareTitle) {
                IconButton(
                    onClick = {
                        withCurrentReport { report ->
                            val prepared =
                                DiagnosticsReportFiles.prepareShare(
                                    context = context,
                                    report = report,
                                )
                            if (prepared == null) {
                                snackbarHostState.showSnackbar(shareFailedMessage)
                                return@withCurrentReport
                            }
                            val sendIntent =
                                Intent(Intent.ACTION_SEND).apply {
                                    type = DiagnosticsReportFiles.ShareMimeType
                                    putExtra(Intent.EXTRA_STREAM, prepared.uri)
                                    clipData =
                                        ClipData.newUri(
                                            context.contentResolver,
                                            reportShareTitle,
                                            prepared.uri,
                                        )
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                            DiagnosticsReportFiles.logShareIntent(context, sendIntent, prepared.uri)
                            val chooserIntent =
                                Intent.createChooser(sendIntent, reportShareTitle).apply {
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                            runCatching { context.startActivity(chooserIntent) }
                                .onSuccess { DiagnosticsReportFiles.logChooserLaunch(context) }
                                .onFailure { error ->
                                    DiagnosticsReportFiles.logChooserLaunch(context, error)
                                    DiagnosticsReportFiles.discardShare(context, prepared)
                                    snackbarHostState.showSnackbar(shareFailedMessage)
                                }
                        }
                    },
                    enabled = reportActionsEnabled,
                ) {
                    Icon(MiuixIcons.Share, contentDescription = reportShareTitle)
                }
            }
            TooltipBox(text = reportExportTitle) {
                IconButton(
                    onClick = {
                        exportPickerOpen = true
                        exportLauncher.launch(DiagnosticsReportFiles.suggestedFileName())
                    },
                    enabled = reportActionsEnabled,
                ) {
                    Icon(MiuixIcons.Download, contentDescription = reportExportTitle)
                }
            }
            DiagnosticsMoreMenu(
                title = moreActionsTitle,
                diagnosticsLevel = diagnosticsSettings.level,
                refreshEnabled = !loading && !exportPickerOpen,
                canScrollTop = !loading && !viewCleared && listState.canScrollBackward,
                canScrollBottom = !loading && !viewCleared && listState.canScrollForward,
                canClear = snapshot != null && !viewCleared,
                onDiagnosticsLevelChange = { level ->
                    if (level != diagnosticsSettings.level) {
                        diagnosticsRepository.setLevel(level)
                        requestRefresh()
                    }
                },
                onRefresh = ::requestRefresh,
                onScrollTop = {
                    scope.launch { listState.animateScrollToItem(0) }
                },
                onScrollBottom = {
                    scope.launch {
                        val lastIndex = listState.layoutInfo.totalItemsCount - 1
                        if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
                    }
                },
                onClear = {
                    viewCleared = true
                    expandedKey = null
                    scope.launch { listState.scrollToItem(0) }
                },
            )
        },
        listState = listState,
        pullToRefresh =
            snapshot?.let {
                SettingsPullToRefresh(
                    refreshing = pullRefreshing,
                    onRefresh = ::requestRefresh,
                    texts = pullRefreshTexts,
                )
            },
    ) {
        when {
            viewCleared -> {
                item(key = "diagnostics-state-cleared") {
                    DiagnosticsLogStateCard(
                        text = stringResource(R.string.diagnostics_view_cleared),
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            loading && snapshot == null -> {
                item(key = "diagnostics-state-loading") {
                    DiagnosticsLogStateCard(
                        text = stringResource(R.string.diagnostics_log_loading),
                        loading = true,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            usefulEntries.isEmpty() -> {
                item(key = "diagnostics-state-empty") {
                    DiagnosticsLogStateCard(
                        text =
                            stringResource(
                                if (runtimeEntries.isNotEmpty() && filterActive) {
                                    R.string.diagnostics_filter_empty
                                } else {
                                    R.string.diagnostics_events_empty
                                },
                            ),
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            else -> {
                item(key = "diagnostics-summary") {
                    Text(
                        text =
                            stringResource(
                                R.string.diagnostics_events_summary,
                                usefulEntries.size,
                            ),
                        modifier =
                            Modifier
                                .animateItem()
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(top = 6.dp, bottom = 8.dp),
                        style = MiuixTheme.textStyles.subtitle,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    )
                }
                itemsIndexed(
                    items = usefulEntries,
                    key = { _, entry -> entry.stableKey },
                ) { _, entry ->
                    DiagnosticsUsefulEventCard(
                        context = context,
                        entry = entry,
                        expanded = expandedKey == entry.stableKey,
                        onToggle = {
                            expandedKey =
                                if (expandedKey == entry.stableKey) {
                                    null
                                } else {
                                    entry.stableKey
                                }
                        },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

}

private const val MaxDiagnosticsUsefulEvents = 40

private const val DiagnosticsFilterLevelInfo = 1 shl 0
private const val DiagnosticsFilterLevelWarning = 1 shl 1
private const val DiagnosticsFilterLevelError = 1 shl 2
private const val DiagnosticsFilterLevelAll =
    DiagnosticsFilterLevelInfo or DiagnosticsFilterLevelWarning or DiagnosticsFilterLevelError

private const val DiagnosticsFilterCategoryModuleCompatibility = 1 shl 0
private const val DiagnosticsFilterCategoryNetwork = 1 shl 1
private const val DiagnosticsFilterCategoryDisplayTransition = 1 shl 2
private const val DiagnosticsFilterCategoryPerformance = 1 shl 3
private const val DiagnosticsFilterCategorySettingsMaintenance = 1 shl 4
private const val DiagnosticsFilterCategoryOther = 1 shl 5
private const val DiagnosticsFilterCategoryAll =
    DiagnosticsFilterCategoryModuleCompatibility or
        DiagnosticsFilterCategoryNetwork or
        DiagnosticsFilterCategoryDisplayTransition or
        DiagnosticsFilterCategoryPerformance or
        DiagnosticsFilterCategorySettingsMaintenance or
        DiagnosticsFilterCategoryOther

private fun diagnosticsFilterMatches(
    entry: DiagnosticLogEntry,
    levelMask: Int,
    categoryMask: Int,
): Boolean {
    val levelBit =
        when (entry.level) {
            DiagnosticLogLevel.Warning -> DiagnosticsFilterLevelWarning
            DiagnosticLogLevel.Error,
            DiagnosticLogLevel.Fatal,
            -> DiagnosticsFilterLevelError
            else -> DiagnosticsFilterLevelInfo
        }
    val categoryBit =
        when (entry.category) {
            DiagnosticLogCategory.Module,
            DiagnosticLogCategory.Native,
            -> DiagnosticsFilterCategoryModuleCompatibility
            DiagnosticLogCategory.Network -> DiagnosticsFilterCategoryNetwork
            DiagnosticLogCategory.Display,
            DiagnosticLogCategory.Transition,
            -> DiagnosticsFilterCategoryDisplayTransition
            DiagnosticLogCategory.Performance -> DiagnosticsFilterCategoryPerformance
            DiagnosticLogCategory.Settings -> DiagnosticsFilterCategorySettingsMaintenance
            DiagnosticLogCategory.Other -> DiagnosticsFilterCategoryOther
        }
    return levelMask and levelBit != 0 && categoryMask and categoryBit != 0
}

private fun diagnosticsToggleMask(
    mask: Int,
    bit: Int,
    checked: Boolean,
): Int =
    if (checked) {
        mask or bit
    } else {
        mask and bit.inv()
    }

private data class DiagnosticsFilterOption(
    @StringRes val titleRes: Int,
    val bit: Int,
)

private val DiagnosticsLevelFilterOptions =
    listOf(
        DiagnosticsFilterOption(R.string.diagnostics_filter_info, DiagnosticsFilterLevelInfo),
        DiagnosticsFilterOption(R.string.diagnostics_filter_warning, DiagnosticsFilterLevelWarning),
        DiagnosticsFilterOption(R.string.diagnostics_filter_error, DiagnosticsFilterLevelError),
    )

private val DiagnosticsCategoryFilterOptions =
    listOf(
        DiagnosticsFilterOption(
            R.string.diagnostics_filter_module_compatibility,
            DiagnosticsFilterCategoryModuleCompatibility,
        ),
        DiagnosticsFilterOption(R.string.diagnostics_filter_network, DiagnosticsFilterCategoryNetwork),
        DiagnosticsFilterOption(
            R.string.diagnostics_filter_display_transition,
            DiagnosticsFilterCategoryDisplayTransition,
        ),
        DiagnosticsFilterOption(R.string.diagnostics_filter_performance, DiagnosticsFilterCategoryPerformance),
        DiagnosticsFilterOption(
            R.string.diagnostics_filter_settings_maintenance,
            DiagnosticsFilterCategorySettingsMaintenance,
        ),
        DiagnosticsFilterOption(R.string.diagnostics_filter_other, DiagnosticsFilterCategoryOther),
    )

@Composable
private fun DiagnosticsFilterMenu(
    title: String,
    levelMask: Int,
    categoryMask: Int,
    onLevelMaskChange: (Int) -> Unit,
    onCategoryMaskChange: (Int) -> Unit,
    onReset: () -> Unit,
) {
    val filterActive =
        levelMask != DiagnosticsFilterLevelAll ||
            categoryMask != DiagnosticsFilterCategoryAll
    val levelItems =
        DiagnosticsLevelFilterOptions.map { option ->
            val selected = levelMask and option.bit != 0
            DropdownItem(
                text = stringResource(option.titleRes),
                selected = selected,
                onClick = {
                    onLevelMaskChange(
                        diagnosticsToggleMask(
                            mask = levelMask,
                            bit = option.bit,
                            checked = !selected,
                        ),
                    )
                },
            )
        }
    val categoryItems =
        DiagnosticsCategoryFilterOptions.map { option ->
            val selected = categoryMask and option.bit != 0
            DropdownItem(
                text = stringResource(option.titleRes),
                selected = selected,
                onClick = {
                    onCategoryMaskChange(
                        diagnosticsToggleMask(
                            mask = categoryMask,
                            bit = option.bit,
                            checked = !selected,
                        ),
                    )
                },
            )
        }
    val entries =
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

    TooltipBox(text = title) {
        WindowIconCascadingDropdownMenu(
            entries = entries,
            collapseOnSelection = false,
        ) {
            BadgedBox(
                badge = {
                    AnimatedVisibility(
                        visible = filterActive,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Badge(
                            containerColor = MiuixTheme.colorScheme.primary,
                        )
                    }
                },
            ) {
                Icon(
                    MiuixIcons.Filter,
                    contentDescription = title,
                )
            }
        }
    }
}

@Composable
private fun DiagnosticsMoreMenu(
    title: String,
    diagnosticsLevel: DiagnosticsLevel,
    refreshEnabled: Boolean,
    canScrollTop: Boolean,
    canScrollBottom: Boolean,
    canClear: Boolean,
    onDiagnosticsLevelChange: (DiagnosticsLevel) -> Unit,
    onRefresh: () -> Unit,
    onScrollTop: () -> Unit,
    onScrollBottom: () -> Unit,
    onClear: () -> Unit,
) {
    val currentLevelLabel =
        stringResource(
            if (diagnosticsLevel == DiagnosticsLevel.Detailed) {
                R.string.diagnostics_mode_detailed
            } else {
                R.string.diagnostics_mode_basic
            },
        )
    val levelItems =
        DiagnosticsLevel.entries.map { level ->
            DropdownItem(
                text =
                    stringResource(
                        if (level == DiagnosticsLevel.Detailed) {
                            R.string.diagnostics_mode_detailed
                        } else {
                            R.string.diagnostics_mode_basic
                        },
                    ),
                selected = diagnosticsLevel == level,
                onClick = { onDiagnosticsLevelChange(level) },
            )
        }
    val entries =
        listOf(
            DropdownEntry(
                items =
                    listOf(
                        DropdownItem(
                            text = stringResource(R.string.diagnostics_mode_title),
                            summary = currentLevelLabel,
                            children = levelItems,
                        ),
                        DropdownItem(
                            text = stringResource(R.string.diagnostics_refresh),
                            enabled = refreshEnabled,
                            onClick = onRefresh,
                        ),
                    ),
            ),
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
                            text = stringResource(R.string.diagnostics_clear_view),
                            enabled = canClear,
                            onClick = onClear,
                        ),
                    ),
            ),
        )

    TooltipBox(text = title) {
        WindowIconCascadingDropdownMenu(entries = entries) {
            Icon(MiuixIcons.More, contentDescription = title)
        }
    }
}

@Composable
private fun DiagnosticsUsefulEventCard(
    context: Context,
    entry: DiagnosticLogEntry,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = diagnosticLogTitle(context, entry)
    val summary = diagnosticLogSummary(context, entry)
    val category = diagnosticLogCategoryLabel(context, entry.category)

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 6.dp)
                .heightIn(min = 86.dp),
        insideMargin = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
        showIndication = true,
        onClick = onToggle,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DiagnosticsLogLevelTag(entry.level)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = category,
                modifier = Modifier.weight(1f),
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            entry.timeText?.let { time ->
                Text(
                    text = time,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style =
                MiuixTheme.textStyles.headline1.copy(
                    fontWeight = FontWeight.Medium,
                ),
            color = MiuixTheme.colorScheme.onSurfaceContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = summary,
            modifier = Modifier.animateContentSize(),
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            maxLines = if (expanded) 2 else 1,
            overflow = TextOverflow.Ellipsis,
        )

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        ) {
            Column {
                Spacer(modifier = Modifier.height(10.dp))
                DiagnosticLogDetailRow(
                    label = "Event",
                    value = entry.event ?: "—",
                )
                entry.component?.let { component ->
                    DiagnosticLogDetailRow(
                        label = "Component",
                        value = component,
                    )
                }
                entry.state?.let { state ->
                    DiagnosticLogDetailRow(
                        label = "State",
                        value = state,
                    )
                }

                entry.fields
                    .filterKeys { key -> key !in DiagnosticLogMetadataFields }
                    .forEach { (key, value) ->
                        DiagnosticLogDetailRow(label = key, value = value)
                    }
            }
        }
    }
}

@Composable
private fun DiagnosticsLogStateCard(
    text: String,
    loading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
        insideMargin = PaddingValues(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                InfiniteProgressIndicator(
                    color = MiuixTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = text,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )
        }
    }
}

@Composable
private fun DiagnosticsLogLevelTag(level: DiagnosticLogLevel) {
    val colors = MiuixTheme.colorScheme
    val warningColor = RuntimeWarningAccent
    val containerColor =
        when (level) {
            DiagnosticLogLevel.Error,
            DiagnosticLogLevel.Fatal,
            -> colors.errorContainer
            DiagnosticLogLevel.Warning -> warningColor.copy(alpha = 0.14f)
            DiagnosticLogLevel.Info -> colors.tertiaryContainer
            else -> colors.secondaryContainerVariant
        }
    val contentColor =
        when (level) {
            DiagnosticLogLevel.Error,
            DiagnosticLogLevel.Fatal,
            -> colors.error
            DiagnosticLogLevel.Warning -> warningColor
            DiagnosticLogLevel.Info -> colors.onTertiaryContainer
            else -> colors.onSurfaceContainerVariant
        }

    Surface(
        modifier = Modifier.heightIn(min = 20.dp),
        shape = RoundedCornerShape(5.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = diagnosticLogLevelLabel(level),
                style =
                    MiuixTheme.textStyles.footnote2.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DiagnosticLogDetailRow(
    label: String,
    value: String,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
    ) {
        Text(
            text = label,
            style =
                MiuixTheme.textStyles.footnote2.copy(
                    fontWeight = FontWeight.Medium,
                ),
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = value,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceContainer,
        )
    }
}

private fun diagnosticLogIsRuntimeEntry(entry: DiagnosticLogEntry): Boolean {
    if (
        entry.level == DiagnosticLogLevel.Warning ||
        entry.level == DiagnosticLogLevel.Error ||
        entry.level == DiagnosticLogLevel.Fatal
    ) {
        return true
    }
    if (!entry.structured) {
        return false
    }
    return entry.event in DiagnosticRuntimeEvents
}

private fun diagnosticLogLevelLabel(level: DiagnosticLogLevel): String =
    when (level) {
        DiagnosticLogLevel.Verbose -> "VERBOSE"
        DiagnosticLogLevel.Debug -> "DEBUG"
        DiagnosticLogLevel.Info -> "INFO"
        DiagnosticLogLevel.Warning -> "WARN"
        DiagnosticLogLevel.Error -> "ERROR"
        DiagnosticLogLevel.Fatal -> "FATAL"
        DiagnosticLogLevel.Unknown -> "LOG"
    }

private fun diagnosticLogCategoryLabel(
    context: Context,
    category: DiagnosticLogCategory,
): String =
    context.getString(
        when (category) {
            DiagnosticLogCategory.Module -> R.string.diagnostics_log_category_module
            DiagnosticLogCategory.Network -> R.string.diagnostics_log_category_network
            DiagnosticLogCategory.Display -> R.string.diagnostics_log_category_display
            DiagnosticLogCategory.Native -> R.string.diagnostics_log_category_native
            DiagnosticLogCategory.Transition -> R.string.diagnostics_log_category_transition
            DiagnosticLogCategory.Performance -> R.string.diagnostics_log_category_performance
            DiagnosticLogCategory.Settings -> R.string.diagnostics_log_category_settings
            DiagnosticLogCategory.Other -> R.string.diagnostics_log_category_other
        },
    )

private fun diagnosticLogTitle(
    context: Context,
    entry: DiagnosticLogEntry,
): String {
    val res =
        when (entry.event) {
            "module.loaded" -> R.string.diagnostics_log_event_module_loaded
            "module.reloaded" -> R.string.diagnostics_log_event_module_reloaded
            "compatibility.probe",
            "compatibility.revalidated",
            -> R.string.diagnostics_log_event_compatibility
            "hook.install",
            "hook.replace",
            -> R.string.diagnostics_log_event_hook
            "source.install",
            "source.attach",
            -> R.string.diagnostics_log_event_source
            "runtime.attach" -> R.string.diagnostics_log_event_runtime_attach
            "runtime.teardown" -> R.string.diagnostics_log_event_runtime_teardown
            "runtimePreferences.bind" -> R.string.diagnostics_log_event_runtime_preferences
            "diagnostics.bind" -> R.string.diagnostics_log_event_diagnostics_bind
            "host.capture" -> R.string.diagnostics_log_event_host_capture
            "host.restore" -> R.string.diagnostics_log_event_host_restore
            "session.attach" -> R.string.diagnostics_log_event_session_attach
            "presentation.cutover" -> R.string.diagnostics_log_event_presentation_cutover
            "presentation.failNative" -> R.string.diagnostics_log_event_fail_native
            "presentation.resolve" -> R.string.diagnostics_log_event_presentation_resolve
            "renderer.attach" -> R.string.diagnostics_log_event_renderer_attach
            "diagnostics.snapshot" -> R.string.diagnostics_log_event_diagnostics_snapshot
            "diagnostics.level" -> R.string.diagnostics_log_event_diagnostics_level
            "pipeline.latency" -> R.string.diagnostics_log_event_pipeline_latency
            "visualSettings.changed" -> R.string.diagnostics_log_event_visual_settings
            "featureSettings.changed" -> R.string.diagnostics_log_event_feature_settings
            "hotReload.prepare" -> R.string.diagnostics_log_event_hot_reload_prepare
            "hotReload.generationHandoff" -> R.string.diagnostics_log_event_hot_reload_handoff
            "hotReload.migration" -> R.string.diagnostics_log_event_hot_reload_migration
            "hotReload.restore" -> R.string.diagnostics_log_event_hot_reload_restore
            "hotReload.complete" -> R.string.diagnostics_log_event_hot_reload
            "mobile.recovery" -> R.string.diagnostics_log_event_mobile_recovery
            "scene.stableFamily" -> R.string.diagnostics_log_event_scene_stable
            "aod.state" -> R.string.diagnostics_log_event_aod_state
            "aod.target" -> R.string.diagnostics_log_event_aod_target
            "connectivity" -> R.string.diagnostics_log_event_connectivity
            "networkPipeline.wifi.iconEvent" -> R.string.diagnostics_log_event_wifi_icon
            "statusIconPresentation" -> R.string.diagnostics_log_event_status_icon_presentation
            "tintCommit" -> R.string.diagnostics_log_event_tint_commit
            "homeRenderTint" -> R.string.diagnostics_log_event_home_tint
            else -> null
        }
    return res?.let(context::getString)
        ?: entry.event
        ?: context.getString(R.string.diagnostics_log_event_generic)
}

private fun diagnosticLogSummary(
    context: Context,
    entry: DiagnosticLogEntry,
): String {
    if (entry.event == "connectivity") {
        return buildList {
            entry.fields["transport"]?.let { add(diagnosticTransportLabel(it)) }
            if (entry.fields["validated"] == "true") {
                add(context.getString(R.string.diagnostics_log_summary_validated))
            }
            if (entry.fields["internetCapability"] == "true") {
                add(context.getString(R.string.diagnostics_log_summary_internet))
            }
            entry.fields["mobileDataEnabled"]?.let { enabled ->
                add(
                    context.getString(
                        if (enabled == "true") {
                            R.string.diagnostics_log_summary_mobile_data_on
                        } else {
                            R.string.diagnostics_log_summary_mobile_data_off
                        },
                    ),
                )
            }
        }.joinToString(context.getString(R.string.diagnostics_log_summary_separator)).ifBlank { entry.message }
    }

    if (entry.event == "pipeline.latency") {
        return buildList {
            entry.fields["sourceToDrawUs"]?.toLongOrNull()?.let { micros ->
                add(
                    context.getString(
                        R.string.diagnostics_log_summary_total_time,
                        formatDiagnosticMicros(micros),
                    ),
                )
            }
            entry.fields["source"]?.let { source ->
                add(
                    context.getString(
                        R.string.diagnostics_log_summary_source,
                        diagnosticTransportLabel(source),
                    ),
                )
            }
        }.joinToString(context.getString(R.string.diagnostics_log_summary_separator)).ifBlank { entry.message }
    }

    if (entry.event in setOf("tintCommit", "homeRenderTint")) {
        return buildList {
            (entry.fields["applied"] ?: entry.fields["statusIcon"])?.let(::add)
            entry.fields["source"]?.let { source ->
                add(
                    context.getString(
                        R.string.diagnostics_log_summary_source,
                        source,
                    ),
                )
            }
        }.joinToString(context.getString(R.string.diagnostics_log_summary_separator)).ifBlank { entry.message }
    }

    val summary =
        buildList {
            entry.state?.let { add(diagnosticStateLabel(context, it)) }
            entry.fields["source"]?.let { source ->
                add(
                    context.getString(
                        R.string.diagnostics_log_summary_source,
                        diagnosticTransportLabel(source),
                    ),
                )
            }
            entry.fields["reason"]?.let { add(it) }
        }.take(3)
            .joinToString(context.getString(R.string.diagnostics_log_summary_separator))

    if (summary.isNotBlank()) {
        return summary
    }
    return if (entry.structured || entry.event != null) {
        context.getString(R.string.diagnostics_log_summary_recorded)
    } else {
        entry.message
    }
}

private fun diagnosticStateLabel(
    context: Context,
    state: String,
): String =
    when (state.lowercase()) {
        "ready" -> context.getString(R.string.diagnostics_log_state_ready)
        "observed" -> context.getString(R.string.diagnostics_log_state_observed)
        "disabled" -> context.getString(R.string.diagnostics_log_state_disabled)
        "unavailable" -> context.getString(R.string.diagnostics_log_state_unavailable)
        "partial" -> context.getString(R.string.diagnostics_log_state_partial)
        "error" -> context.getString(R.string.diagnostics_log_state_error)
        "scheduled" -> context.getString(R.string.diagnostics_log_state_scheduled)
        "restart-required" -> context.getString(R.string.diagnostics_log_state_restart_required)
        else -> state
    }

private fun diagnosticTransportLabel(value: String): String =
    when (value.lowercase()) {
        "wifi" -> "Wi-Fi"
        "mobile" -> "Mobile"
        "hotreload",
        "hotreloadrestore",
        -> "Hot Reload"
        else -> value
    }

private fun formatDiagnosticMicros(micros: Long): String =
    if (micros >= 1_000L) {
        String.format(Locale.US, "%.2f ms", micros / 1_000.0)
    } else {
        "$micros μs"
    }

private val DiagnosticRuntimeEvents =
    setOf(
        "module.loaded",
        "module.reloaded",
        "compatibility.probe",
        "compatibility.revalidated",
        "hook.install",
        "hook.replace",
        "source.install",
        "source.attach",
        "runtime.attach",
        "runtime.teardown",
        "runtimePreferences.bind",
        "diagnostics.bind",
        "diagnostics.level",
        "host.capture",
        "host.restore",
        "session.attach",
        "presentation.cutover",
        "presentation.failNative",
        "renderer.attach",
        "featureSettings.changed",
        "visualSettings.changed",
        "hotReload.prepare",
        "hotReload.generationHandoff",
        "hotReload.migration",
        "hotReload.restore",
        "hotReload.complete",
        "mobile.recovery",
        "scene.stableFamily",
        "aod.state",
        "aod.target",
    )

private val DiagnosticLogMetadataFields =
    setOf(
        "sequence",
        "sessionId",
        "uptimeMs",
        "traceId",
        "healthSnapshot",
        "sampling",
    )
