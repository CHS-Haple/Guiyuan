package top.yukonga.miuix.kmp.glass

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.TopAppBar

// Published TopAppBar clips its entire layout, including protruding glass actions.
internal const val NativeTitleLocalClip = false

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
    largeTitleBlurRadius: Dp,
    titleAlpha: () -> Float,
    compact: Boolean,
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
    TopAppBar(
        title = title,
        largeTitle = largeTitle,
        subtitle = subtitle,
        modifier = modifier,
        color = Color.Transparent,
        scrollBehavior = scrollBehavior,
        defaultWindowInsetsPadding = defaultWindowInsetsPadding,
        titlePadding = titlePadding,
        navigationIconPadding = navigationIconPadding,
        actionIconPadding = actionIconPadding,
        navigationIcon = navigationIcon,
        actions = actions,
        bottomContent = bottomContent,
        titleColor = MiuixTheme.colorScheme.onSurface.copy(alpha = titleAlpha()),
        largeTitleColor = MiuixTheme.colorScheme.onSurface.copy(alpha = titleAlpha()),
    )
}
