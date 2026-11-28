package com.chaners.guiyuan.ui.glass

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.TopAppBarLayout
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults

@Composable
internal fun UiNativeTitleBar(
    title: String,
    largeTitle: String,
    modifier: Modifier,
    scrollBehavior: ScrollBehavior?,
    titleAlpha: () -> Float,
    compact: Boolean,
    expandable: Boolean,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    if (compact) {
        SmallTopAppBar(
            title = title,
            modifier = modifier,
            color = Color.Transparent,
            titleColor = MiuixTheme.colorScheme.onSurface.copy(alpha = titleAlpha()),
            scrollBehavior = scrollBehavior,
            defaultWindowInsetsPadding = true,
            titlePadding = TopAppBarDefaults.TitlePadding,
            navigationIconPadding = GlassTopAppBarDefaults.HorizontalPadding,
            actionIconPadding = GlassTopAppBarDefaults.HorizontalPadding,
            navigationIcon = navigationIcon,
            actions = actions,
            bottomContent = {},
        )
        return
    }
    TopAppBarLayout(
        title = title,
        expandable = expandable,
        largeTitle = largeTitle,
        subtitle = "",
        modifier = modifier,
        color = Color.Transparent,
        titleColor = MiuixTheme.colorScheme.onSurface,
        largeTitleColor = MiuixTheme.colorScheme.onSurface,
        subtitleColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        scrollBehavior = scrollBehavior,
        defaultWindowInsetsPadding = true,
        titlePadding = TopAppBarDefaults.TitlePadding,
        navigationIconPadding = GlassTopAppBarDefaults.HorizontalPadding,
        actionIconPadding = GlassTopAppBarDefaults.HorizontalPadding,
        navigationIcon = navigationIcon,
        actions = {
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        },
        bottomContent = {},
        titleBlurRadius = GlassTopAppBarDefaults.TitleBlurRadius,
        titleAlpha = titleAlpha,
    )
}
