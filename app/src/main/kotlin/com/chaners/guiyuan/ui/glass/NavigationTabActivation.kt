// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for Guiyuan's shared navigation bar and rail activation.

package com.chaners.guiyuan.ui.glass

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics

/** Retains matching MIUIX tab accessibility and keyboard behavior for bar and rail. */
internal fun Modifier.navigationTabActivation(
    selected: Boolean,
    description: String?,
    onActivate: () -> Unit,
): Modifier = this
    .semantics(mergeDescendants = true) {
        role = Role.Tab
        this.selected = selected
        description?.let { contentDescription = it }
        onClick {
            onActivate()
            true
        }
    }
    .onKeyEvent { event ->
        val activationKey = event.key == Key.Enter ||
            event.key == Key.NumPadEnter ||
            event.key == Key.Spacebar
        if (activationKey) {
            if (event.type == KeyEventType.KeyUp) onActivate()
            true
        } else {
            false
        }
    }
