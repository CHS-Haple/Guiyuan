package com.chaners.guiyuan.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.DiagLevel
import com.chaners.guiyuan.settings.DiagSettings
import com.chaners.guiyuan.settings.DiagRepo
import com.chaners.guiyuan.system.LogCategory
import com.chaners.guiyuan.system.LogEntry
import com.chaners.guiyuan.system.LogLevel
import com.chaners.guiyuan.system.DiagSnapshot
import com.chaners.guiyuan.system.DiagReport
import com.chaners.guiyuan.system.DiagFiles
import com.chaners.guiyuan.ui.theme.RuntimeWarningAccent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import top.yukonga.miuix.kmp.anim.folmeSpring
import top.yukonga.miuix.kmp.basic.Badge
import top.yukonga.miuix.kmp.basic.BadgedBox
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
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
    var viewCleared by rememberSaveable { mutableStateOf(false) }
    var expandedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var reportBusy by rememberSaveable { mutableStateOf(false) }
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
            viewCleared = false
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
                onReady(DiagReport.build(captured))
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
    val entries = remember(snapshot, viewCleared) {
        if (viewCleared) emptyList()
        else snapshot?.entries.orEmpty().asReversed().filter(::isRuntimeLog)
    }
    val visibleEntries = remember(entries, levelFilter, categoryFilter) {
        entries
            .asSequence()
            .filter { matchesFilter(it, levelFilter, categoryFilter) }
            .take(MAX_EVENTS)
            .toList()
    }
    val filterActive =
        levelFilter != LEVEL_ALL ||
            categoryFilter != CAT_ALL
    SettingsPage(
        title = stringResource(R.string.diagnostics_title),
        onBack = onBack,
        titlePadding = 0.dp,
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
            TooltipBox(text = shareTitle) {
                IconButton(
                    onClick = {
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
                            DiagFiles.logIntent(context, sendIntent, prepared.uri)
                            val chooserIntent =
                                Intent.createChooser(sendIntent, shareTitle).apply {
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                            runCatching { context.startActivity(chooserIntent) }
                                .onSuccess { DiagFiles.logChooser(context) }
                                .onFailure { error ->
                                    DiagFiles.logChooser(context, error)
                                    DiagFiles.discard(context, prepared)
                                    snackbarHostState.showSnackbar(shareFailMsg)
                                }
                        }
                    },
                    enabled = reportEnabled,
                ) {
                    Icon(MiuixIcons.Share, contentDescription = shareTitle)
                }
            }
            TooltipBox(text = exportTitle) {
                IconButton(
                    onClick = {
                        exportOpen = true
                        exportLauncher.launch(DiagFiles.fileName())
                    },
                    enabled = reportEnabled,
                ) {
                    Icon(MiuixIcons.Download, contentDescription = exportTitle)
                }
            }
            DiagMenu(
                title = menuTitle,
                diagnosticsLevel = diagSettings.level,
                canScrollTop = !loading && !viewCleared && listState.canScrollBackward,
                canScrollBottom = !loading && !viewCleared && listState.canScrollForward,
                canClear = snapshot != null && !viewCleared,
                onDiagLevelChange = { level ->
                    if (level != diagSettings.level) {
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
                onClear = {
                    viewCleared = true
                    expandedKey = null
                    scope.launch { listState.scrollToItem(0) }
                },
            )
        },
        listState = listState,
        // Keep one MIUIX refresh host across entry and refresh.
        pullToRefresh =
            SettingsPullToRefresh(
                refreshing = refreshing,
                onRefresh = ::refresh,
                texts = refreshTexts,
            ),
    ) {
        when {
            viewCleared -> {
                item(key = "diagnostics-state-cleared") {
                    LogStateCard(
                        text = stringResource(R.string.diagnostics_view_cleared),
                        modifier = Modifier,
                    )
                }
            }
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
                                style = MiuixTheme.textStyles.subtitle,
                                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                            )
                        }
                    } else {
                        LogStateCard(text = stringResource(R.string.diagnostics_events_empty))
                    }
                }
            }
            visibleEntries.isNotEmpty() -> {
                item(key = "diagnostics-summary") {
                    Text(
                        text = stringResource(
                            R.string.diagnostics_events_summary,
                            visibleEntries.size,
                        ),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(top = 6.dp, bottom = 8.dp)
                                .graphicsLayer {
                                    val shown = logProgress(rise.value, 0)
                                    translationY = risePx * (1f - shown)
                                    alpha = shown
                                },
                        style = MiuixTheme.textStyles.subtitle,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    )
                }
                itemsIndexed(
                    items = visibleEntries,
                    key = { _, entry -> entry.key },
                ) { index, entry ->
                    LogCard(
                        context = context,
                        entry = entry,
                        expanded = expandedKey == entry.key,
                        onToggle = {
                            expandedKey =
                                if (expandedKey == entry.key) {
                                    null
                                } else {
                                    entry.key
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
            }
            else -> {
                item(key = "diagnostics-state-empty") {
                    LogStateCard(
                        text =
                            stringResource(
                                if (entries.isNotEmpty() && filterActive) {
                                    R.string.diagnostics_filter_empty
                                } else {
                                    R.string.diagnostics_events_empty
                                },
                            ),
                        modifier = Modifier,
                    )
                }
            }
        }
    }

}

private const val MAX_EVENTS = 40

private fun logProgress(progress: Float, index: Int): Float {
    // Off-screen rows share the last visible row's start.
    val start = index.coerceAtMost(7) * 0.07f
    return ((progress - start) / (1f - start)).coerceIn(0f, 1f)
}

private const val LEVEL_INFO = 1 shl 0
private const val LEVEL_WARN = 1 shl 1
private const val LEVEL_ERROR = 1 shl 2
private const val LEVEL_ALL =
    LEVEL_INFO or LEVEL_WARN or LEVEL_ERROR

private const val CAT_MODULE = 1 shl 0
private const val CAT_NETWORK = 1 shl 1
private const val CAT_DISPLAY = 1 shl 2
private const val CAT_PERF = 1 shl 3
private const val CAT_SETTINGS = 1 shl 4
private const val CAT_OTHER = 1 shl 5
private const val CAT_ALL =
    CAT_MODULE or
        CAT_NETWORK or
        CAT_DISPLAY or
        CAT_PERF or
        CAT_SETTINGS or
        CAT_OTHER

private fun matchesFilter(
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
    val levelItems =
        LevelOptions.map { option ->
            val selected = levelMask and option.bit != 0
            DropdownItem(
                text = stringResource(option.titleRes),
                selected = selected,
                onClick = {
                    onLevelMaskChange(
                        toggleMask(
                            mask = levelMask,
                            bit = option.bit,
                            checked = !selected,
                        ),
                    )
                },
            )
        }
    val categoryItems =
        CategoryOptions.map { option ->
            val selected = categoryMask and option.bit != 0
            DropdownItem(
                text = stringResource(option.titleRes),
                selected = selected,
                onClick = {
                    onCategoryMaskChange(
                        toggleMask(
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
private fun DiagMenu(
    title: String,
    diagnosticsLevel: DiagLevel,
    canScrollTop: Boolean,
    canScrollBottom: Boolean,
    canClear: Boolean,
    onDiagLevelChange: (DiagLevel) -> Unit,
    onScrollTop: () -> Unit,
    onScrollBottom: () -> Unit,
    onClear: () -> Unit,
) {
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
                onClick = { onDiagLevelChange(level) },
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
private fun LogCard(
    context: Context,
    entry: LogEntry,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = logTitle(context, entry)
    val summary = logSummary(context, entry)
    val category = categoryLabel(context, entry.category)

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 6.dp),
        insideMargin = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
        showIndication = true,
        onClick = onToggle,
        onLongPress = {
            copyLog(
                context = context,
                entry = entry,
            )
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LogLevelTag(entry.level)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = category,
                modifier = Modifier.weight(1f),
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            entry.time?.let { time ->
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
        if (summary.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = summary,
                modifier = Modifier.animateContentSize(),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                maxLines = if (expanded) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        ) {
            Column {
                Spacer(modifier = Modifier.height(10.dp))
                LogDetail(
                    label = "Event",
                    value = entry.event ?: "—",
                )
                entry.component?.let { component ->
                    LogDetail(
                        label = "Component",
                        value = component,
                    )
                }
                entry.state?.let { state ->
                    LogDetail(
                        label = "State",
                        value = state,
                    )
                }

                entry.fields
                    .filterKeys { key -> key !in LOG_META_FIELDS }
                    .forEach { (key, value) ->
                        LogDetail(label = key, value = value)
                    }
            }
        }
    }
}

@Composable
private fun LogStateCard(
    text: String,
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
            Text(
                text = text,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )
        }
    }
}

@Composable
private fun LogLevelTag(level: LogLevel) {
    val colors = MiuixTheme.colorScheme
    val warningColor = RuntimeWarningAccent
    val containerColor =
        when (level) {
            LogLevel.Error,
            LogLevel.Fatal,
            -> colors.errorContainer
            LogLevel.Warning -> warningColor.copy(alpha = 0.14f)
            LogLevel.Info -> colors.tertiaryContainer
            else -> colors.secondaryContainerVariant
        }
    val contentColor =
        when (level) {
            LogLevel.Error,
            LogLevel.Fatal,
            -> colors.error
            LogLevel.Warning -> warningColor
            LogLevel.Info -> colors.onTertiaryContainer
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
                text = levelLabel(level),
                style =
                    MiuixTheme.textStyles.footnote2.copy(
                        fontWeight = FontWeight.ExtraBold,
                    ),
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LogDetail(
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

private fun copyLog(
    context: Context,
    entry: LogEntry,
) {
    val clipboard =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return
    clipboard.setPrimaryClip(
        ClipData.newPlainText(
            context.getString(R.string.diagnostics_log_clipboard_label),
            entry.raw,
        ),
    )
    Toast.makeText(
        context,
        R.string.diagnostics_log_copied,
        Toast.LENGTH_SHORT,
    ).show()
}

private fun isRuntimeLog(entry: LogEntry): Boolean {
    if (
        entry.level == LogLevel.Warning ||
        entry.level == LogLevel.Error ||
        entry.level == LogLevel.Fatal
    ) {
        return true
    }
    if (!entry.structured) {
        return false
    }
    return entry.event in RUNTIME_EVENTS
}

private fun levelLabel(level: LogLevel): String =
    when (level) {
        LogLevel.Verbose -> "VERBOSE"
        LogLevel.Debug -> "DEBUG"
        LogLevel.Info -> "INFO"
        LogLevel.Warning -> "WARN"
        LogLevel.Error -> "ERROR"
        LogLevel.Fatal -> "FATAL"
        LogLevel.Unknown -> "LOG"
    }

private fun categoryLabel(
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

private fun logTitle(
    context: Context,
    entry: LogEntry,
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

private fun logSummary(
    context: Context,
    entry: LogEntry,
): String {
    // Structured events already have a title; their raw protocol is not a summary.
    val fallback = if (entry.structured) "" else entry.message
    if (entry.event == "connectivity") {
        return buildList {
            entry.fields["transport"]?.let { add(transportLabel(it)) }
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
        }.joinToString(context.getString(R.string.diagnostics_log_summary_separator)).ifBlank { fallback }
    }

    if (entry.event == "pipeline.latency") {
        return buildList {
            entry.fields["sourceToDrawUs"]?.toLongOrNull()?.let { micros ->
                add(
                    context.getString(
                        R.string.diagnostics_log_summary_total_time,
                        formatMicros(micros),
                    ),
                )
            }
            entry.fields["source"]?.let { source ->
                add(
                    context.getString(
                        R.string.diagnostics_log_summary_source,
                        transportLabel(source),
                    ),
                )
            }
        }.joinToString(context.getString(R.string.diagnostics_log_summary_separator)).ifBlank { fallback }
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
        }.joinToString(context.getString(R.string.diagnostics_log_summary_separator)).ifBlank { fallback }
    }

    val summary =
        buildList {
            entry.state?.let { add(stateLabel(context, it)) }
            entry.fields["source"]?.let { source ->
                add(
                    context.getString(
                        R.string.diagnostics_log_summary_source,
                        transportLabel(source),
                    ),
                )
            }
            entry.fields["reason"]?.let { add(it) }
        }.take(3)
            .joinToString(context.getString(R.string.diagnostics_log_summary_separator))

    return summary.ifBlank { fallback }
}

private fun stateLabel(
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

private fun transportLabel(value: String): String =
    when (value.lowercase()) {
        "wifi" -> "Wi-Fi"
        "mobile" -> "Mobile"
        "hotreload",
        "hotreloadrestore",
        -> "Hot Reload"
        else -> value
    }

private fun formatMicros(micros: Long): String =
    if (micros >= 1_000L) {
        String.format(Locale.US, "%.2f ms", micros / 1_000.0)
    } else {
        "$micros μs"
    }

private val RUNTIME_EVENTS =
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

private val LOG_META_FIELDS =
    setOf(
        "sequence",
        "sessionId",
        "uptimeMs",
        "traceId",
        "healthSnapshot",
        "sampling",
    )
