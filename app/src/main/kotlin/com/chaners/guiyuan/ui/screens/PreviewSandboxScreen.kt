package com.chaners.guiyuan.ui.screens

import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.ui.LocalGlassBackdrop
import com.chaners.guiyuan.settings.VisualRepo
import com.chaners.guiyuan.settings.FeatureRepo
import com.chaners.guiyuan.ui.components.rememberTopBarBackdrop
import com.chaners.guiyuan.ui.components.topBarBackdropSource
import com.chaners.guiyuan.ui.layout.pageContentPadding
import com.chaners.guiyuan.ui.layout.pageScrollEffects
import com.chaners.guiyuan.ui.layout.rememberPageOverscrollCallback
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.MiuixPopupHost

@OptIn(ExperimentalScrollBarApi::class)
@Composable
internal fun PreviewSandboxScreen(
    display: PreviewSandboxDisplay,
    actions: PreviewSandboxActions,
) {
    val state = display.state
    val context = LocalContext.current.applicationContext
    val resourceResolver = remember(context) { PreviewSysUiResources(context) }
    val visualRepo = remember(context) { VisualRepo(context) }
    val visual by visualRepo.settings.collectAsState(initial = visualRepo.current())
    val featureRepo = remember(context) { FeatureRepo(context) }
    val feature by featureRepo.settings.collectAsState(initial = featureRepo.current())
    val renderModel = rememberPreviewRenderModel(
        state = state,
        visual = visual,
        resources = resourceResolver,
        lowBatteryThreshold = feature.lowBatteryThreshold,
    )

    val choices = previewSandboxOptions()

    val scrollState = rememberScrollState()
    var previewHeightPx by remember { mutableIntStateOf(0) }
    // The first preview and its title scroll together; use their measured height.
    val showDock by remember(scrollState) {
        derivedStateOf { previewHeightPx > 0 && scrollState.value >= previewHeightPx }
    }
    val scope = rememberCoroutineScope()

    val canScroll = scrollState.canScrollBackward || scrollState.canScrollForward
    val scrollBehavior = MiuixScrollBehavior()
    val topBarBackdrop = rememberTopBarBackdrop()
    val scrollToTop: () -> Unit = {
        scope.launch {
            scrollState.animateScrollTo(0)
            if (!scrollState.canScrollBackward) {
                // Clear stale Glass shadow while keeping MIUIX's current title collapse state.
                scrollBehavior.state.contentOffset = 0f
            }
        }
    }
    val onOverscrollChanged = rememberPageOverscrollCallback(scrollBehavior)
    var dockHeightPx by remember { mutableIntStateOf(0) }
    var nativeBarHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val dockHeight = with(density) { dockHeightPx.toDp() }
    val dock: @Composable () -> Unit = {
        SandboxFloatingDock(
            display = SandboxDockDisplay(showDock, renderModel, visual, topBarBackdrop),
            actions = SandboxDockActions(
                onScrollToTop = scrollToTop,
                onHeightChanged = { dockHeightPx = it },
            ),
        )
    }

    CompositionLocalProvider(LocalGlassBackdrop provides topBarBackdrop) {
        Scaffold(
            topBar = {
                PreviewSandboxTopBar(
                    frame = SandboxTopBarFrame(
                        topBarBackdrop, scrollBehavior, scrollState, dockHeight,
                    ),
                    actions = SandboxTopBarActions(
                        dark = display.dark,
                        onBack = actions.ui.onBack,
                        onToggleTheme = actions.ui.onToggleTheme,
                    ),
                    onHeightChanged = { nativeBarHeightPx = it },
                )
            },
            // Retain MIUIX's one native popup host; it draws above this dock.
            popupHost = {
                SandboxPopupHost(with(density) { nativeBarHeightPx.toDp() }, dock)
            },
    ) { paddingValues ->
        val contentPadding = pageContentPadding(
            innerPadding = paddingValues,
            extraBottom = 12.dp,
        )
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .topBarBackdropSource(topBarBackdrop)
                        .pageScrollEffects(scrollBehavior, canScroll, canScroll, onOverscrollChanged)
                        .verticalScroll(scrollState, overscrollEffect = if (canScroll) null else rememberOverscrollEffect())
                        .padding(contentPadding),
            ) {
                Box(Modifier.fillMaxWidth().onSizeChanged { previewHeightPx = it.height }) {
                    PreviewSandboxPrimarySection(state, renderModel, visual)
                }
                previewSandboxRows(display, choices, visual, actions)
            }
            VerticalScrollBar(
                adapter = rememberScrollBarAdapter(scrollState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                trackPadding = contentPadding,
                topAppBarScrollBehavior = scrollBehavior,
            )
         }
    }
    }
}

/** The dock is below the native popup layer, outside the scrolling body. */
@Composable
private fun SandboxPopupHost(top: Dp, dock: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().padding(top = top)) { dock() }
        MiuixPopupHost()
    }
}
