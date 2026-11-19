package com.chaners.guiyuan.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.system.LogEntry
import com.chaners.guiyuan.system.DiagSnapshot
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.AnimationVector1D

internal class DiagnosticsLogPresentation(
    val snapshot: DiagSnapshot?,
    val entries: List<LogEntry>,
    val filteredEntries: List<LogEntry>,
    val showRootGuide: Boolean,
    val loading: Boolean,
    val filterActive: Boolean,
    val captureTruncated: Boolean,
    val detailStatus: String?,
    val detailActive: Boolean,
    val integrityNotice: String,
    val expandedKey: String?,
    val rise: Animatable<Float, AnimationVector1D>,
    val risePx: Float,
    val placementSpec: FiniteAnimationSpec<IntOffset>,
)

internal fun LazyListScope.diagnosticsLogItems(
    view: DiagnosticsLogPresentation,
    context: Context,
    onToggle: (String) -> Unit,
    onRetry: () -> Unit,
) {
        if (!view.showRootGuide) {
            // A changing notice must not insert a new key before the summary and shift the title.
            item(key = "diagnostics-header") {
                Column(
                    modifier = Modifier.graphicsLayer {
                        val shown = logProgress(view.rise.value, 0)
                        translationY = view.risePx * (1f - shown)
                        alpha = shown
                    },
                ) {
                    if (view.filteredEntries.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.diagnostics_events_summary, view.filteredEntries.size),
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
                        targetState = view.detailStatus to view.detailActive,
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
                    if (view.integrityNotice.isNotEmpty()) {
                        Text(
                            text = view.integrityNotice,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        )
                    }
                }
            }
        }
        when {
            view.snapshot == null -> {
                item(key = "diagnostics-loading") {
                    if (view.loading) {
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
            view.filteredEntries.isNotEmpty() -> {
                itemsIndexed(
                    items = view.filteredEntries,
                    key = { index, entry -> logRowKey(entry, index) },
                ) { index, entry ->
                    val rowKey = logRowKey(entry, index)
                    LogCard(
                        context = context,
                        entry = entry,
                        expanded = view.expandedKey == rowKey,
                        onToggle = { onToggle(rowKey) },
                        modifier =
                            Modifier
                                .animateItem(
                                    fadeInSpec = null,
                                    view.placementSpec = view.placementSpec,
                                    fadeOutSpec = null,
                                )
                                .graphicsLayer {
                                    val shown = logProgress(view.rise.value, index)
                                    translationY = view.risePx * (1f - shown)
                                    alpha = shown
                                },
                    )
                }
                item(key = "diagnostics-history-end") {
                    Text(
                        text = stringResource(
                            when {
                                view.captureTruncated && view.filterActive -> R.string.diagnostics_events_end_truncated_filtered
                                view.captureTruncated -> R.string.diagnostics_events_end_truncated
                                view.filterActive -> R.string.diagnostics_events_end_filtered
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
            view.showRootGuide -> {
                item(key = "diagnostics-root-unavailable") {
                    LogEmptyGuide(
                        title = stringResource(R.string.diagnostics_root_unavailable_title),
                        description = stringResource(R.string.diagnostics_root_unavailable_message),
                        note = listOfNotNull(
                            stringResource(R.string.diagnostics_root_unavailable_export),
                            view.integrityNotice.takeIf(String::isNotEmpty),
                        ).joinToString("\n"),
                        icon = MiuixIcons.Medium.Report,
                        busy = view.loading,
                        onRetry = onRetry,
                        modifier = Modifier.fillParentMaxSize(),
                    )
                }
            }
            view.entries.isEmpty() && view.filterActive -> {
                item(key = "diagnostics-state-filter-empty") {
                    LogStateCard(text = stringResource(R.string.diagnostics_filter_empty))
                }
            }
            view.entries.isEmpty() -> {
                item(key = "diagnostics-no-events") {
                    LogEmptyGuide(
                        title = stringResource(R.string.diagnostics_events_empty),
                        description = stringResource(
                            if (view.snapshot?.log?.clearedAt != null) {
                                R.string.diagnostics_history_empty
                            } else {
                                R.string.diagnostics_no_events_hint
                            },
                        ),
                        note = null,
                        icon = MiuixIcons.Info,
                        busy = view.loading,
                        onRetry = onRetry,
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

private fun logProgress(progress: Float, index: Int): Float {
    // Off-screen rows share the last visible row's start.
    val start = index.coerceAtMost(7) * 0.07f
    return ((progress - start) / (1f - start)).coerceIn(0f, 1f)
}
