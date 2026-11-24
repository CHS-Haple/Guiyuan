package com.chaners.guiyuan.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.glass.GlassTextField

/** Glass inputs inside dialog surfaces. */
@Composable
internal fun UiDialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    GlassTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        trailingIcon = trailingIcon,
    )
}
