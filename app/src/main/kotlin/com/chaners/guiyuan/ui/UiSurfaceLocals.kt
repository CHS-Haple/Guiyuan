package com.chaners.guiyuan.ui

import androidx.compose.runtime.staticCompositionLocalOf
import top.yukonga.miuix.kmp.blur.Backdrop

internal val LocalTopBarProgressiveBlur = staticCompositionLocalOf { true }

// The page records one backdrop; nested Glass surfaces reuse it without recording popup hosts.
internal val LocalGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }
