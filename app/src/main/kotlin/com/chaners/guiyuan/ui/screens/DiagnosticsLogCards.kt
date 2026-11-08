package com.chaners.guiyuan.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.system.LogEntry
import com.chaners.guiyuan.system.LogLevel
import com.chaners.guiyuan.ui.theme.RuntimeWarningAccent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
private fun LogCardMeta(entry: LogEntry, category: String) {
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
}

@Composable
private fun LogCardExpanded(entry: LogEntry, expanded: Boolean) {
    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
    ) {
        Column {
            Spacer(modifier = Modifier.height(10.dp))
            LogDetail(
                label = "event",
                value = entry.event ?: "—",
            )
            entry.component?.let { component ->
                LogDetail(
                    label = "component",
                    value = component,
                )
            }
            entry.state?.let { state ->
                LogDetail(
                    label = "state",
                    value = state,
                )
            }

            logDetailFields(entry).forEach { (key, value) ->
                LogDetail(
                    label = key,
                    value = value,
                )
            }
        }
    }
}

@Composable
internal fun LogCard(
    context: Context,
    entry: LogEntry,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = logTitle(context, entry)
    val summary = logSummary(context, entry)
    val category = categoryLabel(context, entry.category)
    val warning = entry.level == LogLevel.Warning ||
        entry.level == LogLevel.Error || entry.level == LogLevel.Fatal

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
        LogCardMeta(entry, category)
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
                maxLines = if (expanded) 3 else if (warning) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        LogCardExpanded(entry, expanded)
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
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
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
