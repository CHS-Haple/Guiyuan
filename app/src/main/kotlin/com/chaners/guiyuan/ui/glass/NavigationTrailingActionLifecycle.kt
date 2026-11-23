// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
// Guiyuan extension for a shared navigation bar and rail action lifecycle.

package com.chaners.guiyuan.ui.glass

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.computedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics

/**
 * Retain an exiting navigation action through the final exit frame.
 *
 * Each caller retains its own composition-scoped copy. Clearing happens only after
 * the existing MIUIX motion clock reaches zero, so no additional owner/timer is needed.
 */
@Composable
internal fun rememberNavigationTrailingAction(
    action: (@Composable () -> Unit)?,
    progress: State<Float>,
): (@Composable () -> Unit)? {
    val retainedAction = remember { mutableStateOf(action) }
    SideEffect {
        if (action != null || progress.value == 0f) {
            retainedAction.value = action
        }
    }
    val isExiting by remember(progress) { computedStateOf { progress.value > 0f } }
    return action ?: retainedAction.value.takeIf { isExiting }
}

/** Exiting controls may paint their last frame but must not receive input or semantics. */
internal fun Modifier.blockRetiringNavigationActionInput(): Modifier = this
    .clearAndSetSemantics { }
    .pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
            }
        }
    }
