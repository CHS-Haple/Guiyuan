// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
// App-only port of MIUIX GlassFloatingActionButton at 3177a684; avoid a second glass module.

package top.yukonga.miuix.kmp.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.material.GlassColorBlendMode
import top.yukonga.miuix.kmp.material.GlassColorLayer
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GlassFloatingActionButton(
    onClick: () -> Unit,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = GlassMotion.springOf(damping = 1f, response = if (pressed) 0.15f else 0.3f),
        label = "glassFabPressScale",
    )
    val primary = MiuixTheme.colorScheme.primary
    val baseMaterial = GlassTopAppBarDefaults.buttonMaterial()
    val material = remember(baseMaterial, primary) {
        baseMaterial.copy(third = GlassColorLayer(primary.copy(alpha = 0.92f), GlassColorBlendMode.SrcOver))
    }
    val shadowAlpha = remember { { 1f } }

    GlassButtonSurface(
        backdrop = backdrop,
        floating = true,
        surfaceAlpha = 1f,
        shadowAlpha = shadowAlpha,
        style = GlassDefaults.Style,
        material = material,
        underlayMaterial = null,
        fill = primary,
        size = 56.dp,
        shape = GlassShape(28.dp),
        stroke = GlassTopAppBarDefaults.buttonStroke(),
        shadow = GlassShadows.Regular,
        pressed = pressed,
        modifier = modifier
            .semantics { role = Role.Button }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            },
        content = content,
    )
}
