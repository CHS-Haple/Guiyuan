package top.yukonga.miuix.kmp.glass

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.BlurTopAppBar

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
    largeTitleBlurRadius: Dp,
    titleAlpha: () -> Float,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    bottomContent: @Composable () -> Unit,
) {
    BlurTopAppBar(
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
        largeTitleBlurRadius = largeTitleBlurRadius,
        titleAlpha = titleAlpha,
    )
}
