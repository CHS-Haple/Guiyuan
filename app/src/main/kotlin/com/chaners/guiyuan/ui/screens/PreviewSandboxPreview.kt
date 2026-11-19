package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.VisualCfg
import com.chaners.guiyuan.ui.components.StatusPreview
import com.chaners.guiyuan.xposed.RenderModel
import com.chaners.guiyuan.ui.components.PreviewSummary
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SandboxFullPreview(
    state: PreviewSandboxUiState,
    model: RenderModel,
    visual: VisualCfg,
    showDescription: Boolean = true,
) {
    if (showDescription) {
        Text(
            text = stringResource(R.string.home_preview_sandbox_summary),
            modifier = Modifier.padding(top = 2.dp),
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (showDescription) 8.dp else 0.dp)
            .height(192.dp),
        contentAlignment = Alignment.Center,
    ) {
        StatusPreview(
            model = model,
            visual = visual,
            modifier = Modifier
                .width(SandboxPreviewWidth)
                .height(SandboxPreviewHeight),
        )
    }
    PreviewSummary(state, modifier = Modifier.padding(top = 4.dp))
}

// Draw into the actual compact View rather than resampling a full-sized GPU layer.
@Composable
internal fun SandboxDockIcon(
    model: RenderModel,
    visual: VisualCfg,
) {
    val compactHeight = TopAppBarDefaults.SmallTopAppBarCenterHeight
    val compactWidth = compactHeight * (SandboxPreviewWidth.value / SandboxPreviewHeight.value)
    StatusPreview(
        model = model,
        visual = visual,
        modifier = Modifier.size(compactWidth, compactHeight),
        referenceSize = DpSize(SandboxPreviewWidth, SandboxPreviewHeight),
    )
}

private val SandboxPreviewWidth = 120.dp
private val SandboxPreviewHeight = 184.dp
