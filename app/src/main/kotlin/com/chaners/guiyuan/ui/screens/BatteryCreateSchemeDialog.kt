package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.limitCustomSchemeName
import com.chaners.guiyuan.ui.components.UiDialogButton
import com.chaners.guiyuan.ui.components.dialogActionGap
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import top.yukonga.miuix.kmp.basic.TextField
import com.chaners.guiyuan.ui.components.UiOverlayDialog

@Composable
internal fun BatteryCreateSchemeDialog(
    show: Boolean,
    nextId: Int?,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit,
    onCreate: (String) -> Unit,
) {
    val defaultName =
        nextId?.let {
            stringResource(R.string.battery_custom_scheme_default_name, it)
        }.orEmpty()
    var name by remember(nextId) { mutableStateOf(defaultName) }

    UiOverlayDialog(
        title = stringResource(R.string.battery_custom_scheme_new),
        show = show && nextId != null,
        onDismissRequest = onDismiss,
        onDismissFinished = onDismissFinished,
    ) {
        Column {
            TextField(
                value = name,
                onValueChange = { name = limitCustomSchemeName(it) },
                label = stringResource(R.string.battery_custom_scheme_name),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    val next = name.trim()
                    if (show && next.isNotEmpty()) onCreate(next)
                }),
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                UiDialogButton(
                    text = stringResource(R.string.cancel),
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss,
                )
                Spacer(Modifier.width(dialogActionGap()))
                UiDialogButton(
                    text = stringResource(R.string.battery_custom_scheme_create),
                    modifier = Modifier.weight(1f),
                    enabled = show && name.trim().isNotEmpty(),
                    role = GlassDialogButtonRole.Primary,
                    onClick = {
                        if (show) onCreate(name.trim())
                    },
                )
            }
        }
    }
}
