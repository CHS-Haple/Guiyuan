// Adapted from MIUIX example component/GroupedCardItems.kt, revision a09d4f32.
// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.chaners.guiyuan.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.LocalContentColor

internal class PreferenceCardItem(
    val key: String,
    val content: @Composable ColumnScope.() -> Unit,
)

internal fun LazyListScope.GroupedPreferenceSection(
    @StringRes titleRes: Int,
    keyPrefix: String,
    items: List<PreferenceCardItem>,
) {
    if (items.isEmpty()) return
    item(key = "$keyPrefix:title", contentType = "preference-title") {
        SmallTitle(stringResource(titleRes))
    }
    items.forEachIndexed { index, row ->
        item(key = "$keyPrefix:${row.key}", contentType = "preference-segment") {
            PreferenceCardSegment(index == 0, index == items.lastIndex, row.content)
        }
    }
}

@Composable
private fun PreferenceCardSegment(
    first: Boolean,
    last: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.defaultColors()
    val top = if (first) CardDefaults.CornerRadius else 0.dp
    val bottom = if (last) CardDefaults.CornerRadius else 0.dp
    val seam = if (!last && colors.color.alpha == 1f) {
        Modifier.drawBehind {
            // Upstream opaque-edge bleed prevents rasterization seams.
            val bleedTop = (size.height - 2f).coerceAtLeast(0f)
            drawRect(colors.color, Offset(0f, bleedTop), Size(size.width, size.height - bleedTop + 2f))
        }
    } else Modifier
    val surface = if (top == 0.dp && bottom == 0.dp) {
        Modifier.background(colors.color)
    } else {
        Modifier.squircleSurface(colors.color, top, top, bottom, bottom)
    }
    CompositionLocalProvider(LocalContentColor provides colors.contentColor) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = if (last) 12.dp else 0.dp)
                .then(seam)
                .then(surface)
                .padding(CardDefaults.InsideMargin),
            content = content,
        )
    }
}
