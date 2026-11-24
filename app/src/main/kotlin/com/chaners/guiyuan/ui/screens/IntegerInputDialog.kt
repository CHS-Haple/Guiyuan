package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import androidx.compose.ui.res.stringResource
import top.yukonga.miuix.kmp.basic.Text
import com.chaners.guiyuan.ui.components.UiDialogButton
import com.chaners.guiyuan.ui.components.dialogActionGap
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import top.yukonga.miuix.kmp.glass.GlassTextField
import com.chaners.guiyuan.ui.components.UiOverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal data class IntegerInputModel(
    val title: String,
    val summary: String,
    val value: Int,
    val range: IntRange,
    val suffix: String? = null,
)

internal data class IntegerInputCallbacks(
    val onDismiss: () -> Unit,
    val onConfirm: (Int) -> Unit,
)

@Composable
internal fun IntegerInputDialog(
    show: Boolean,
    model: IntegerInputModel,
    callbacks: IntegerInputCallbacks,
) {
    val (title, summary, value, range, suffix) = model
    val (onDismiss, onConfirm) = callbacks
    var editedInput by remember { mutableStateOf<String?>(null) }
    val input = editedInput ?: value.toString()
    UiOverlayDialog(
        title = title,
        summary = summary,
        show = show,
        onDismissRequest = onDismiss,
        onDismissFinished = { editedInput = null },
    ) {
        Column {
            GlassTextField(
                value = input,
                onValueChange = { text ->
                    if (text.length <= range.last.toString().length && text.all(Char::isDigit)) {
                        editedInput = text
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon =
                    if (suffix != null) {
                        {
                            Text(
                                text = suffix,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            )
                        }
                    } else {
                        null
                    },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dialogActionGap()),
            ) {
                UiDialogButton(
                    text = stringResource(R.string.cancel),
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss,
                )
                UiDialogButton(
                    text = stringResource(R.string.confirm),
                    modifier = Modifier.weight(1f),
                    role = GlassDialogButtonRole.Primary,
                    onClick = {
                        onConfirm((input.toIntOrNull() ?: value).coerceIn(range))
                    },
                )
            }
        }
    }
}
