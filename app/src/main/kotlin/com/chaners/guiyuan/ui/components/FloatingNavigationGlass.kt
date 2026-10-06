package com.chaners.guiyuan.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.chaners.guiyuan.settings.FloatingNavStyle
import top.yukonga.miuix.kmp.basic.FloatingToolbarDefaults
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal const val FloatingNavigationBlurRadius = 25f
internal const val FloatingNavigationBlendAlpha = 0.6f

internal val FloatingNavStyle.requiresTextureBackdrop: Boolean
    get() = this != FloatingNavStyle.Standard

@Composable
internal fun Modifier.floatingNavigationMaterial(
    backdrop: Backdrop,
    darkMode: Boolean,
    style: FloatingNavStyle,
): Modifier {
    if (style == FloatingNavStyle.Standard) return this

    return textureBlur(
        backdrop = backdrop,
        shape = RoundedCornerShape(FloatingToolbarDefaults.CornerRadius),
        blurRadius = FloatingNavigationBlurRadius,
        colors =
            BlurDefaults.blurColors(
                blendColors =
                    listOf(
                        BlendColorEntry(
                            color =
                                MiuixTheme.colorScheme.surfaceContainer.copy(
                                    alpha = FloatingNavigationBlendAlpha,
                                ),
                        ),
                    ),
            ),
        highlight =
            if (style == FloatingNavStyle.Glass) {
                if (darkMode) Highlight.GlassStrokeMiddleDark else Highlight.GlassStrokeMiddleLight
            } else {
                null
            },
    )
}
