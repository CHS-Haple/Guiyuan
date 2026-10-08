package com.chaners.guiyuan.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as BitmapCanvas
import android.graphics.Paint
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
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
        opticalIcon(
            context,
            iconRes,
            with(density) { 22.dp.roundToPx() },
            with(density) { 19.dp.roundToPx() },
        )
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
            val badge = remember(context, density.density, detailRes) {
                opticalIcon(
                    context,
                    detailRes,
                    with(density) { 11.dp.roundToPx() },
                    with(density) { 9.dp.roundToPx() },
                )
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
                                topLeft = Offset(12.dp.toPx() - gap, 12.dp.toPx() - gap),
                                blendMode = BlendMode.DstOut,
                            )
                        },
                colorFilter = tint,
            )
            Image(
                bitmap = badge,
                contentDescription = null,
                modifier = Modifier.align(Alignment.BottomEnd).size(11.dp),
                colorFilter = tint,
            )
        }
    }
}

// Center each glyph's visible ink, keeping the Material outline and proportions intact.
private fun opticalIcon(
    context: Context,
    @DrawableRes res: Int,
    side: Int,
    ink: Int,
): ImageBitmap {
    val drawable = requireNotNull(context.getDrawable(res)).mutate()
    val sample = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
    drawable.setBounds(0, 0, side, side)
    drawable.draw(BitmapCanvas(sample))

    val pixels = IntArray(side * side)
    sample.getPixels(pixels, 0, side, 0, 0, side, side)
    var left = side
    var top = side
    var right = -1
    var bottom = -1
    for (y in 0 until side) {
        for (x in 0 until side) {
            if ((pixels[y * side + x] ushr 24) >= 24) {
                left = minOf(left, x)
                top = minOf(top, y)
                right = maxOf(right, x)
                bottom = maxOf(bottom, y)
            }
        }
    }
    if (right < left) return sample.asImageBitmap()

    val width = right - left + 1
    val height = bottom - top + 1
    val scale = ink.toFloat() / maxOf(width, height)
    val result = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
    val canvas = BitmapCanvas(result)
    val saved = canvas.save()
    canvas.translate(
        (side - width * scale) / 2f - left * scale,
        (side - height * scale) / 2f - top * scale,
    )
    canvas.scale(scale, scale)
    drawable.draw(canvas)
    canvas.restoreToCount(saved)
    sample.recycle()
    return result.asImageBitmap()
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
    listState: LazyListState? = null,
    pullToRefresh: SettingsPullToRefresh? = null,
    overlay: @Composable () -> Unit = {},
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val resolvedListState = listState ?: rememberLazyListState()
    val topBarBackdrop = rememberTopBarBackdrop()

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

            @Composable
            fun SettingsList() {
                LazyColumn(
                    state = resolvedListState,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = contentPadding,
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
        }

        overlay()
    }
}

internal fun androidx.compose.foundation.lazy.LazyListScope.Section(
    @StringRes titleRes: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    item {
        SmallTitle(stringResource(titleRes))
        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
            content = content,
        )
    }
}
