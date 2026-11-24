package com.chaners.guiyuan.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as BitmapCanvas
import android.graphics.Paint
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
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
    val main = painterResource(iconRes)
    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (detailRes == null) {
            Image(
                painter = main,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                colorFilter = tint,
            )
        } else {
            // 12×12 occupies one quarter of the shared 24×24 optical box.
            val badgeSize = 12.dp
            val badge = painterResource(detailRes)
            val gap = with(density) { 1.dp.roundToPx() }
            val cutout = remember(context, density.density, detailRes) {
                badgeCutout(
                    materialIcon(context, detailRes, with(density) { badgeSize.roundToPx() }),
                    gap,
                )
            }
            Image(
                painter = main,
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
                painter = badge,
                contentDescription = null,
                modifier = Modifier.align(Alignment.BottomEnd).size(badgeSize),
                colorFilter = tint,
            )
        }
    }
}

// Only the composite cutout needs pixels; the displayed glyphs stay vector.
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
