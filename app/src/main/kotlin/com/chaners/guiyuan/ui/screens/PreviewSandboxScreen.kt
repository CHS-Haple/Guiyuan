package com.chaners.guiyuan.ui.screens

import androidx.compose.animation.AnimatedVisibility
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.ui.LocalTopBarProgressiveBlur
import com.chaners.guiyuan.ui.LocalGlassBackdrop
import com.chaners.guiyuan.settings.VisualRepo
import com.chaners.guiyuan.settings.SignalDotArrangement
import com.chaners.guiyuan.settings.FeatureRepo
import com.chaners.guiyuan.ui.components.rememberTopBarBackdrop
import com.chaners.guiyuan.ui.components.topBarBackdropSource
import com.chaners.guiyuan.ui.components.signalValueText
import com.chaners.guiyuan.ui.layout.pageContentPadding
import com.chaners.guiyuan.ui.layout.pageScrollBarTrackPadding
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.MiuixPopupHost
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalScrollBarApi::class)
@Composable
internal fun PreviewSandboxScreen(
    display: PreviewSandboxDisplay,
    actions: PreviewSandboxActions,
) {
    val state = display.state
    val dark = display.dark
    val onToggleTheme = actions.ui.onToggleTheme
    val onBack = actions.ui.onBack
    val context = LocalContext.current
    val resourceResolver =
        remember(context.applicationContext) {
            PreviewSysUiResources(context.applicationContext)
        }
    val visualRepo =
        remember(context.applicationContext) {
            VisualRepo(context.applicationContext)
        }
    val visual by
        visualRepo.settings.collectAsState(
            initial = visualRepo.current(),
        )
    val featureRepo = remember(context.applicationContext) {
        FeatureRepo(context.applicationContext)
    }
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
    val glassOverscroll = canScroll
    val onOverscrollChanged: (Float) -> Unit = remember(scrollBehavior) {
        { offset -> scrollBehavior.state.overscrollOffset = offset }
    }
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
                        dark = dark,
                        onBack = onBack,
                        onToggleTheme = onToggleTheme,
                    ),
                    onHeightChanged = { nativeBarHeightPx = it },
                )
            },
            // Retain MIUIX's one native popup host; it draws above this dock.
            popupHost = {
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier.fillMaxWidth().padding(
                            top = with(density) { nativeBarHeightPx.toDp() },
                        ),
                    ) {
                        dock()
                    }
                    MiuixPopupHost()
                }
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
                        .then(if (canScroll) Modifier.scrollEndHaptic() else Modifier)
                        .then(if (glassOverscroll) Modifier.overScrollVertical(onOffsetChanged = onOverscrollChanged) else Modifier)
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .verticalScroll(scrollState, overscrollEffect = if (glassOverscroll) null else rememberOverscrollEffect())
                        .padding(contentPadding),
            ) {
                previewSandboxRows(display, choices, renderModel, visual, actions) { previewHeightPx = it }
            }
            VerticalScrollBar(
                adapter = rememberScrollBarAdapter(scrollState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                trackPadding = pageScrollBarTrackPadding(contentPadding, scrollBehavior),
                topAppBarScrollBehavior = scrollBehavior,
            )
         }
    }
    }
}
