package com.chaners.guiyuan.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.chaners.guiyuan.settings.CombinedStatusVisualSettings
import com.chaners.guiyuan.xposed.RenderModel
import com.chaners.guiyuan.xposed.RenderView
import com.chaners.guiyuan.xposed.CombinedStatusTintState
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun CombinedStatusPreview(
    model: RenderModel,
    visualSettings: CombinedStatusVisualSettings,
    modifier: Modifier = Modifier,
) {
    val tint = MiuixTheme.colorScheme.onSurfaceContainer.toArgb()

    AndroidView(
        factory = { context ->
            RenderView(context).apply {
                addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                    syncPreviewViewport()
                }
                setScaleMobileTypeWithCanvas(true)
                setTintState(
                    CombinedStatusTintState(
                        appliedTint = tint,
                        statusIconTint = tint,
                    ),
                )
                setVisualSettings(visualSettings)
                setModel(model)
            }
        },
        modifier = modifier,
        update = { view ->
            view.syncPreviewViewport()
            view.setScaleMobileTypeWithCanvas(true)
            view.setTintState(
                CombinedStatusTintState(
                    appliedTint = tint,
                    statusIconTint = tint,
                ),
            )
            view.setVisualSettings(visualSettings)
            view.setModel(model)
        },
    )
}


private fun RenderView.syncPreviewViewport() {
    if (width <= 0 || height <= 0) return
    val logicalSize = minOf(width, height)
    val extraHeight = (height - logicalSize).coerceAtLeast(0)
    setLogicalViewport(
        widthPx = width,
        heightPx = logicalSize,
        topInsetPx = extraHeight / 2,
    )
}
