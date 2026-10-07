package com.chaners.guiyuan.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.chaners.guiyuan.settings.NavStyle
import top.yukonga.miuix.kmp.basic.FloatingToolbarDefaults
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal const val NAV_BLUR_RADIUS = 25f
internal const val NAV_BLEND_ALPHA = 0.6f

internal val NavStyle.requiresTextureBackdrop: Boolean
    get() = this == NavStyle.Blur || this == NavStyle.Glass

@Composable
internal fun Modifier.floatingNavMaterial(
    backdrop: Backdrop,
    dark: Boolean,
    style: NavStyle,
): Modifier {
    if (!style.requiresTextureBackdrop) return this

    return textureBlur(
        backdrop = backdrop,
        shape = RoundedCornerShape(FloatingToolbarDefaults.CornerRadius),
        blurRadius = NAV_BLUR_RADIUS,
        colors =
            BlurDefaults.blurColors(
                blendColors =
                    listOf(
                        BlendColorEntry(
                            color =
                                MiuixTheme.colorScheme.surfaceContainer.copy(
                                    alpha = NAV_BLEND_ALPHA,
                                ),
                        ),
                    ),
            ),
        highlight =
            if (style == NavStyle.Glass) {
                if (dark) Highlight.GlassStrokeMiddleDark else Highlight.GlassStrokeMiddleLight
            } else {
                null
            },
    )
}
