// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.chaners.guiyuan.ui.glass

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics

/** Retained but noninteractive glass surfaces remain visible without exposing input or semantics. */
internal fun Modifier.blockVisualOnlyInput(): Modifier = this
    .clearAndSetSemantics { }
    .pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
            }
        }
    }
