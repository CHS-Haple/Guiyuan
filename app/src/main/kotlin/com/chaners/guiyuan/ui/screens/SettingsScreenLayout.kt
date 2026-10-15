package com.chaners.guiyuan.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as BitmapCanvas
import android.graphics.Paint
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.ui.components.MiuixBlurredTopBar
import com.chaners.guiyuan.ui.components.rememberTopBarBackdrop
import com.chaners.guiyuan.ui.components.topBarBackdropSource
import com.chaners.guiyuan.ui.layout.pageContentPadding
import kotlin.math.sqrt
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
internal fun SemanticLeadingIcon(
    @DrawableRes iconRes: Int,
    enabled: Boolean = true,
    @DrawableRes detailRes: Int? = null,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val tint = ColorFilter.tint(
        MiuixTheme.colorScheme.onSurfaceContainer.copy(
            alpha = if (enabled) 1f else 0.38f,
        ),
    )
    val main = remember(context, density.density, iconRes) {
        materialIcon(context, iconRes, with(density) { 22.dp.roundToPx() })
    }
    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (detailRes == null) {
            Image(
                bitmap = main,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                colorFilter = tint,
            )
        } else {
            val detail = remember(context, density.density, detailRes) {
                materialIcon(context, detailRes, with(density) { 22.dp.roundToPx() })
            }
            val badgeSize = remember(main, detail) {
                val mainArea = paintedIconArea(main)
                val detailArea = paintedIconArea(detail)
                if (mainArea > 0 && detailArea > 0) {
                    // Match a quarter of the main glyph's painted area, not its height alone.
                    (11f * sqrt(mainArea.toFloat() / detailArea.toFloat())).coerceIn(12f, 17f).dp
                } else {
                    12.dp
                }
            }
            val badge = remember(context, density.density, detailRes, badgeSize) {
                materialIcon(context, detailRes, with(density) { badgeSize.roundToPx() })
            }
            val gap = with(density) { 1.dp.roundToPx() }
            val cutout = remember(badge, gap) { badgeCutout(badge, gap) }
            Image(
                bitmap = main,
                contentDescription = null,
                modifier =
                    Modifier
                        .size(22.dp)
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .drawWithContent {
                            drawContent()
                            // Erase only the badge outline; keep the MIUIX surface untouched.
                            drawImage(
                                image = cutout,
                                topLeft = Offset(
                                    23.dp.toPx() - badgeSize.toPx() - gap,
                                    23.dp.toPx() - badgeSize.toPx() - gap,
                                ),
                                blendMode = BlendMode.DstOut,
                            )
                        },
                colorFilter = tint,
            )
            Image(
                bitmap = badge,
                contentDescription = null,
                modifier = Modifier.align(Alignment.BottomEnd).size(badgeSize),
                colorFilter = tint,
            )
        }
    }
}

// Material paths already include their optical spacing in the 24dp viewport.
private fun materialIcon(
    context: Context,
    @DrawableRes res: Int,
    side: Int,
): ImageBitmap {
    val drawable = requireNotNull(context.getDrawable(res)).mutate()
    val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
    drawable.setBounds(0, 0, side, side)
    drawable.draw(BitmapCanvas(bitmap))
    return bitmap.asImageBitmap()
}

// Count painted alpha so narrow detail glyphs are not treated like full-width ones.
private fun paintedIconArea(icon: ImageBitmap): Int {
    val bitmap = icon.asAndroidBitmap()
    var area = 0
    for (y in 0 until bitmap.height) {
        for (x in 0 until bitmap.width) {
            area += bitmap.getPixel(x, y) ushr 24
        }
    }
    return area
}

// A round alpha dilation leaves a small, contour-shaped gap around the badge.
private fun badgeCutout(badge: ImageBitmap, gap: Int): ImageBitmap {
    val source = badge.asAndroidBitmap()
    val side = source.width + gap * 2
    val result = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
    val canvas = BitmapCanvas(result)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    for (y in -gap..gap) {
        for (x in -gap..gap) {
            if (x * x + y * y <= gap * gap) {
                canvas.drawBitmap(source, (gap + x).toFloat(), (gap + y).toFloat(), paint)
            }
        }
    }
    return result.asImageBitmap()
}

internal data class SettingsPullToRefresh(
    val refreshing: Boolean,
    val onRefresh: () -> Unit,
    val texts: List<String>,
)

@Composable
internal fun SettingsPage(
    title: String,
    onBack: () -> Unit,
    snackbarHost: @Composable () -> Unit = {},
    navigationActions: @Composable RowScope.() -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    titlePadding: Dp = TopAppBarDefaults.TitlePadding,
    titleAlpha: Float = 1f,
    listState: LazyListState? = null,
    pullToRefresh: SettingsPullToRefresh? = null,
    background: @Composable BoxScope.(PaddingValues) -> Unit = {},
    foreground: @Composable BoxScope.(PaddingValues) -> Unit = {},
    overlay: @Composable () -> Unit = {},
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val resolvedListState = listState ?: rememberLazyListState()
    val topBarBackdrop = rememberTopBarBackdrop()
    val springScroll = pullToRefresh == null

    Scaffold(
        snackbarHost = snackbarHost,
        topBar = {
            MiuixBlurredTopBar(
                backdrop = topBarBackdrop,
                scrollBehavior = scrollBehavior,
            ) { barColor ->
                SmallTopAppBar(
                    title = title,
                    color = barColor,
                    titleColor = MiuixTheme.colorScheme.onSurface.copy(alpha = titleAlpha),
                    scrollBehavior = scrollBehavior,
                    titlePadding = titlePadding,
                    navigationIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TooltipBox(text = stringResource(R.string.back)) {
                                IconButton(onClick = onBack) {
                                    Icon(
                                        MiuixIcons.Back,
                                        contentDescription = stringResource(R.string.back),
                                    )
                                }
                            }
                            navigationActions()
                        }
                    },
                    actions = actions,
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .topBarBackdropSource(topBarBackdrop),
        ) {
            val contentPadding =
                pageContentPadding(
                    innerPadding = paddingValues,
                    extraBottom = 12.dp,
                )
            background(contentPadding)

            @Composable
            fun SettingsList() {
                LazyColumn(
                    state = resolvedListState,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .then(
                                if (springScroll) Modifier.scrollEndHaptic().overScrollVertical()
                                else Modifier,
                            )
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = contentPadding,
                    // Pull-to-refresh remains the gesture owner on Diagnostics.
                    overscrollEffect = if (springScroll) null else rememberOverscrollEffect(),
                    content = content,
                )
            }

            if (pullToRefresh != null) {
                PullToRefresh(
                    isRefreshing = pullToRefresh.refreshing,
                    onRefresh = pullToRefresh.onRefresh,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    topAppBarScrollBehavior = scrollBehavior,
                    refreshTexts = pullToRefresh.texts,
                ) {
                    SettingsList()
                }
            } else {
                SettingsList()
            }
            foreground(contentPadding)
        }

        overlay()
    }
}

internal fun androidx.compose.foundation.lazy.LazyListScope.Section(
    @StringRes titleRes: Int? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    item {
        titleRes?.let { SmallTitle(stringResource(it)) }
        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
            content = content,
        )
    }
}
