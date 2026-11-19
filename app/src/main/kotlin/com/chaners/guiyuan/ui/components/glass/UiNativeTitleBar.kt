package com.chaners.guiyuan.ui.components.glass

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.TopAppBarLayout

// PR423 clips title layers locally; action controls stay in their native measured slots.
internal const val NativeTitleLocalClip = true

@Composable
internal fun UiNativeTitleBar(
    title: String,
    largeTitle: String,
    subtitle: String,
    modifier: Modifier,
    scrollBehavior: ScrollBehavior?,
    titlePadding: Dp,
    navigationIconPadding: Dp,
    actionIconPadding: Dp,
    defaultWindowInsetsPadding: Boolean,
    titleBlurRadius: Dp,
    titleAlpha: () -> Float,
    compact: Boolean,
    expandable: Boolean,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    bottomContent: @Composable () -> Unit,
) {
    if (compact) {
        SmallTopAppBar(
            title = title,
            modifier = modifier,
            color = Color.Transparent,
            titleColor = MiuixTheme.colorScheme.onSurface.copy(alpha = titleAlpha()),
            scrollBehavior = scrollBehavior,
            defaultWindowInsetsPadding = defaultWindowInsetsPadding,
            titlePadding = titlePadding,
            navigationIconPadding = navigationIconPadding,
            actionIconPadding = actionIconPadding,
            navigationIcon = navigationIcon,
            actions = actions,
            bottomContent = bottomContent,
        )
        return
    }
    TopAppBarLayout(
        title = title,
        expandable = expandable,
        largeTitle = largeTitle,
        subtitle = subtitle,
        modifier = modifier,
        color = Color.Transparent,
        titleColor = MiuixTheme.colorScheme.onSurface,
        largeTitleColor = MiuixTheme.colorScheme.onSurface,
        subtitleColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        scrollBehavior = scrollBehavior,
        defaultWindowInsetsPadding = defaultWindowInsetsPadding,
        titlePadding = titlePadding,
        navigationIconPadding = navigationIconPadding,
        actionIconPadding = actionIconPadding,
        navigationIcon = navigationIcon,
        actions = {
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        },
        bottomContent = bottomContent,
        titleBlurRadius = titleBlurRadius,
        titleAlpha = titleAlpha,
    )
}
