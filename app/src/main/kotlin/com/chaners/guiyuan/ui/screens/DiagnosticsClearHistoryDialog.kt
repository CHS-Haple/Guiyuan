package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.R
import com.chaners.guiyuan.ui.components.UiOverlayDialog
import com.chaners.guiyuan.ui.components.UiDialogButton
import com.chaners.guiyuan.ui.components.dialogActionGap
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole

@Composable
internal fun DiagnosticsClearHistoryDialog(
    show: Boolean,
    busy: Boolean,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
) {
    UiOverlayDialog(
        show = show,
        title = stringResource(R.string.diagnostics_clear_history),
        summary = stringResource(R.string.diagnostics_history_clear_confirm),
        onDismissRequest = { if (!busy) onDismiss() },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dialogActionGap()),
        ) {
            UiDialogButton(
                text = stringResource(R.string.cancel),
                modifier = Modifier.weight(1f),
                enabled = !busy,
                onClick = onDismiss,
            )
            UiDialogButton(
                text = stringResource(R.string.diagnostics_clear_history_action),
                modifier = Modifier.weight(1f),
                enabled = !busy,
                role = GlassDialogButtonRole.Destructive,
                onClick = onClear,
            )
        }
    }
}
