package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Content selected by the diagnostics state; retry behavior remains screen-owned. */
internal data class LogEmptyGuideContent(
    val title: String,
    val description: String,
    val note: String?,
    val icon: ImageVector,
)

@Composable
internal fun LogEmptyGuide(
    content: LogEmptyGuideContent,
    busy: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (title, description, note, icon) = content
    Box(
        modifier = modifier.fillMaxWidth().padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 340.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LogEmptyGuideHeader(icon, title)
            LogEmptyGuideBody(description, note)
            LogEmptyRetryAction(busy = busy, onRetry = onRetry)
        }
    }
}

@Composable
private fun LogEmptyGuideHeader(icon: ImageVector, title: String) {
    Box(
        modifier = Modifier.size(72.dp).background(
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
}

@Composable
private fun LogEmptyGuideBody(description: String, note: String?) {
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
}

@Composable
private fun LogEmptyRetryAction(
    busy: Boolean,
    onRetry: () -> Unit,
) {
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
