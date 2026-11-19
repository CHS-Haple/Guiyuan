package com.chaners.guiyuan.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.UiStyle
import com.chaners.guiyuan.ui.LocalUiStyle
import com.chaners.guiyuan.ui.LocalGlassBackdrop
import com.chaners.guiyuan.ui.icons.Os4GlassRefresh
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.TooltipBox
import com.chaners.guiyuan.ui.glass.GlassIconButton
import com.chaners.guiyuan.ui.glass.LocalGlassTopAppBarContext
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh

@Composable
internal fun HotReloadAction(
    inProgress: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(R.string.hot_reload)

    TooltipBox(text = label) {
        if (LocalUiStyle.current == UiStyle.OS4) {
            // Home's hot-reload action needs a persistent glass surface. A top bar's
            // shared material progress intentionally fades out when not scrolled;
            // inherit its backdrop but not the bar's scrolling material clock.
            val resolvedBackdrop = LocalGlassTopAppBarContext.current?.buttonBackdrop
                ?: LocalGlassBackdrop.current
            CompositionLocalProvider(LocalGlassTopAppBarContext provides null) {
                GlassIconButton(
                    onClick = { if (enabled && !inProgress) onClick() },
                    backdrop = resolvedBackdrop,
                    enabled = enabled,
                    holdDownState = inProgress,
                ) {
                    Icon(
                        imageVector = Os4GlassRefresh,
                        contentDescription = label,
                        tint = if (enabled) MiuixTheme.colorScheme.onSurface
                            else MiuixTheme.colorScheme.disabledOnSecondaryVariant,
                    )
                }
            }
        } else {
            IconButton(
                onClick = { if (enabled && !inProgress) onClick() },
                enabled = enabled,
                holdDownState = inProgress,
            ) {
                Icon(
                    imageVector = MiuixIcons.Refresh,
                    contentDescription = label,
                )
            }
        }
    }
}
