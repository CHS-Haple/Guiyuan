package com.chaners.guiyuan.ui.components

import android.util.Size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.chaners.guiyuan.settings.VisualCfg
import com.chaners.guiyuan.xposed.RenderModel
import com.chaners.guiyuan.xposed.RenderView
import com.chaners.guiyuan.xposed.TintState
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun StatusPreview(
    model: RenderModel,
    visual: VisualCfg,
    modifier: Modifier = Modifier,
    referenceSize: DpSize? = null,
) {
    val tint = MiuixTheme.colorScheme.onSurfaceContainer.toArgb()
    val density = LocalDensity.current
    val reference = referenceSize?.let {
        Size(
            with(density) { it.width.roundToPx() },
            with(density) { it.height.roundToPx() },
        )
    }

    AndroidView(
        factory = { context ->
            RenderView(context, preview = true).apply {
                addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                    syncPreviewViewport()
                }
                setPreviewSourceSize(reference)
                setScaleMobileTypeWithCanvas(true)
                setTintState(
                    TintState(
                        appliedTint = tint,
                        statusIconTint = tint,
                    ),
                )
                setVisualCfg(visual)
                setModel(model)
            }
        },
        modifier = modifier,
        update = { view ->
            view.setPreviewSourceSize(reference)
            view.syncPreviewViewport()
            view.setScaleMobileTypeWithCanvas(true)
            view.setTintState(
                TintState(
                    appliedTint = tint,
                    statusIconTint = tint,
                ),
            )
            view.setVisualCfg(visual)
            view.setModel(model)
        },
    )
}

