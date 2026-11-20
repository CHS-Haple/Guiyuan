package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.GyApplication
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.FeatureCfg
import com.chaners.guiyuan.settings.FeatureRepo
import com.chaners.guiyuan.settings.VisualCfg
import com.chaners.guiyuan.settings.VisualRepo
import com.chaners.guiyuan.settings.UiStyle
import com.chaners.guiyuan.system.XposedStatus
import com.chaners.guiyuan.ui.LocalUiStyle
import com.chaners.guiyuan.ui.LocalTopBarProgressiveBlur
import com.chaners.guiyuan.ui.LocalGlassRailProgress
import com.chaners.guiyuan.ui.components.HotReloadAction
import com.chaners.guiyuan.ui.components.MiuixBlurredTopBar
import com.chaners.guiyuan.ui.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop

internal data class HomeScreenSources(
    val featureRepository: FeatureRepo,
    val featureSettings: State<FeatureCfg>,
    val visual: State<VisualCfg>,
    val xposedStatus: State<XposedStatus>,
    val previewResources: PreviewSysUiResources,
)

internal data class HomeScreenFrame(
    val listState: LazyListState,
    val scrollBehavior: MiuixScrollBehavior,
    val backdrop: LayerBackdrop?,
)

@Composable
internal fun rememberHomeScreenSources(): HomeScreenSources {
    val context = LocalContext.current.applicationContext
    val application = remember(context) { context as GyApplication }
    val featureRepo = remember(context) { FeatureRepo(context) }
    val featureSettings = featureRepo.settings.collectAsState(initial = featureRepo.current())
    val visualRepo = remember(context) { VisualRepo(context) }
    val visual = visualRepo.settings.collectAsState(initial = visualRepo.current())
    val xposedStatus = application.xposedStatus.collectAsState()
    val previewResources = remember(context) { PreviewSysUiResources(context) }
    return HomeScreenSources(featureRepo, featureSettings, visual, xposedStatus, previewResources)
}

@Composable
internal fun HomeScreenTopBar(
    runtime: HomeScreenRuntime,
    actions: HomeScreenActions,
    frame: HomeScreenFrame,
) {
    val buttons: @Composable RowScope.() -> Unit = {
        if (!runtime.hotReloadInNavigation) {
            HotReloadAction(
                inProgress = runtime.hotReloadInProgress,
                enabled = !runtime.restartInProgress && !runtime.scopeRequestInProgress,
                onClick = actions.onHotReload,
            )
        }
    }
    if (LocalUiStyle.current == UiStyle.OS4) {
        GlassTopAppBar(
            progressiveBlur = LocalTopBarProgressiveBlur.current,
            isContentScrolled = frame.listState.canScrollBackward,
            title = stringResource(R.string.home_title),
            titleAlpha = LocalGlassRailProgress.current,
            backdrop = frame.backdrop,
            scrollBehavior = frame.scrollBehavior,
            actions = buttons,
            unclippedActions = true,
        )
    } else {
        MiuixBlurredTopBar(
            backdrop = frame.backdrop,
            scrollBehavior = frame.scrollBehavior,
        ) { barColor ->
            TopAppBar(
                title = stringResource(R.string.home_title),
                color = barColor,
                actions = buttons,
                scrollBehavior = frame.scrollBehavior,
            )
        }
    }
}
