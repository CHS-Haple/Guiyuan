package com.chaners.guiyuan.ui

import androidx.compose.runtime.staticCompositionLocalOf
import top.yukonga.miuix.kmp.blur.Backdrop
import com.chaners.guiyuan.settings.UiStyle

internal val LocalUiStyle = staticCompositionLocalOf { UiStyle.OS3 }

// The page records one backdrop; nested OS4 surfaces reuse it without recording popup hosts.
internal val LocalGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }
