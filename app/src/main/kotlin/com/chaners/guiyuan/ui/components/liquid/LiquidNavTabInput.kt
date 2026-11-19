package com.chaners.guiyuan.ui.components.liquid

import androidx.compose.foundation.focusable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics

/**
 * A single accessibility/keyboard contract for horizontal and vertical Liquid tabs.
 * The parent Liquid surface alone owns pointer gestures.
 */
internal fun Modifier.liquidNavTabInput(
    selected: Boolean,
    interactive: Boolean,
    onActivate: () -> Unit,
): Modifier = then(
    if (!interactive) {
        Modifier.clearAndSetSemantics {}
    } else {
        Modifier.semantics(mergeDescendants = true) {
            role = Role.Tab
            this.selected = selected
            onClick {
                onActivate()
                true
            }
        }.onKeyEvent { event ->
            val activate = event.key == Key.Enter ||
                event.key == Key.NumPadEnter ||
                event.key == Key.Spacebar
            if (activate) {
                if (event.type == KeyEventType.KeyUp) onActivate()
                true
            } else {
                false
            }
        }.focusable()
    },
)
