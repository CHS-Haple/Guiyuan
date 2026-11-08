// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.glass

import androidx.compose.runtime.mutableFloatStateOf
import org.junit.Assert.assertEquals
import org.junit.Test

class GlassTransformPopupTest {
    @Test
    fun sharedAnchorVisibilityIgnoresMaterialAlpha() {
        val progress = mutableFloatStateOf(1f)
        val anchor = GlassPopupAnchor().apply {
            surfaceProgress = progress
            surfaceMaterialAlpha = 0.25f
        }

        assertEquals(1f, anchor.surfaceVisibility, 0f)
        progress.floatValue = 0.4f
        assertEquals(0.4f, anchor.surfaceVisibility, 0f)
        assertEquals(
            0.4f,
            transformPanelAlpha(
                floating = true,
                anchorAlpha = anchor.surfaceVisibility,
                geometryProgress = 0f,
                iconMaterial = 0f,
            ),
            0f,
        )
        progress.floatValue = 0f
        assertEquals(0f, anchor.surfaceVisibility, 0f)
    }

    @Test
    fun popupMotionVisibilityDoesNotMultiplyMaterialAlpha() {
        assertEquals(
            0.5f,
            transformPanelAlpha(
                floating = false,
                anchorAlpha = 0.25f,
                geometryProgress = 0.5f,
                iconMaterial = 0.5f,
            ),
            0f,
        )
        assertEquals(
            0.75f,
            transformPanelAlpha(
                floating = true,
                anchorAlpha = 0.5f,
                geometryProgress = 0.5f,
                iconMaterial = 0f,
            ),
            0f,
        )
    }
}
