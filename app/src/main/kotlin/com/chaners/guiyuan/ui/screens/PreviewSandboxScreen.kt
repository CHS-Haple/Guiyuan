package com.chaners.guiyuan.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.UiStyle
import com.chaners.guiyuan.ui.LocalTopBarProgressiveBlur
import com.chaners.guiyuan.ui.LocalUiStyle
import com.chaners.guiyuan.ui.icons.Os4GlassBack
import com.chaners.guiyuan.ui.LocalGlassBackdrop
import com.chaners.guiyuan.settings.VisualRepo
import com.chaners.guiyuan.settings.SignalDotArrangement
import com.chaners.guiyuan.settings.FeatureRepo
import com.chaners.guiyuan.ui.components.MiuixBlurredTopBar
import com.chaners.guiyuan.ui.components.rememberTopBarBackdrop
import com.chaners.guiyuan.ui.components.topBarBackdropSource
import com.chaners.guiyuan.ui.components.signalValueText
import com.chaners.guiyuan.ui.layout.pageContentPadding
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import com.chaners.guiyuan.ui.glass.GlassTopAppBar
import com.chaners.guiyuan.ui.glass.GlassTopAppBarDefaults
import com.chaners.guiyuan.ui.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassMotion
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.MiuixPopupHost
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
    val renderModel = remember(state, visual.dualSignal, feature.lowBatteryThreshold, resourceResolver, LocalConfiguration.current) {
        state.toRenderModel(
            resourceResolver,
            dualSignalEnabled = visual.dualSignal,
            lowBatteryThreshold = feature.lowBatteryThreshold,
        )
    }

    val choices = previewSandboxOptions()

    // The dock is an overlay; compare the preview against the list's native content inset.
    val listState = rememberLazyListState()
    val showDock by remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            val preview = info.visibleItemsInfo.firstOrNull { it.key == "fullPreview" }
            when {
                info.totalItemsCount == 0 -> false
                preview != null -> preview.offset + preview.size <= info.beforeContentPadding
                else -> listState.firstVisibleItemIndex > 0
            }
        }
    }
    val scope = rememberCoroutineScope()

    val canScroll = listState.canScrollBackward || listState.canScrollForward
    val scrollBehavior = MiuixScrollBehavior()
    val topBarBackdrop = rememberTopBarBackdrop()
    val scrollToTop: () -> Unit = {
        scope.launch {
            listState.animateScrollToItem(0)
            if (!listState.canScrollBackward) {
                // Clear stale Glass shadow while keeping MIUIX's current title collapse state.
                scrollBehavior.state.contentOffset = 0f
            }
        }
    }
    val os4 = LocalUiStyle.current == UiStyle.OS4
    val glassOverscroll = os4 && canScroll
    val onOverscrollChanged: (Float) -> Unit = remember(scrollBehavior) {
        { offset -> scrollBehavior.state.overscrollOffset = offset }
    }
    // Match MIUIX's 52dp compact title center plus its 4dp bottom title clearance.
    val dockPadding = if (os4) {
        val inset = (TopAppBarDefaults.CollapsedHeight - TopAppBarDefaults.SmallTopAppBarCenterHeight) / 2
        PaddingValues(top = inset, bottom = inset + TopAppBarDefaults.LargeTitleBottomPadding)
    } else {
        PaddingValues(vertical = 6.dp)
    }
    var dockHeightPx by remember { mutableIntStateOf(0) }
    var nativeBarHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val dockHeight = with(density) { dockHeightPx.toDp() }
    val backAction: @Composable () -> Unit = {
        TooltipBox(text = stringResource(R.string.back)) {
            if (os4) {
                GlassIconButton(onClick = onBack) {
                    Icon(Os4GlassBack, contentDescription = stringResource(R.string.back))
                }
            } else {
                IconButton(onClick = onBack) {
                    Icon(MiuixIcons.Back, contentDescription = stringResource(R.string.back))
                }
            }
        }
    }
    val barActions: @Composable RowScope.() -> Unit = {
        val title = stringResource(
            if (dark) R.string.home_preview_switch_light
            else R.string.home_preview_switch_dark,
        )
        TooltipBox(text = title) {
            if (os4) {
                GlassIconButton(onClick = onToggleTheme) {
                    Image(
                        painter = painterResource(if (dark) R.drawable.ic_material_symbol_light_mode else R.drawable.ic_material_symbol_dark_mode),
                        contentDescription = title,
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurface),
                    )
                }
            } else IconButton(onClick = onToggleTheme) {
                Image(
                    painter = painterResource(
                        if (dark) R.drawable.ic_material_symbol_light_mode
                        else R.drawable.ic_material_symbol_dark_mode,
                    ),
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurface),
                )
            }
        }
    }
    val dock: @Composable () -> Unit = {
        AnimatedVisibility(
            visible = showDock,
            modifier = Modifier.onSizeChanged { dockHeightPx = it.height },
            enter = if (os4) {
                expandVertically(
                    animationSpec = GlassMotion.barExpand(),
                    expandFrom = Alignment.Top,
                    clip = false,
                )
            } else fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = if (os4) {
                shrinkVertically(
                    animationSpec = GlassMotion.barCollapse(),
                    shrinkTowards = Alignment.Top,
                    clip = false,
                )
            } else fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        ) {
            // The OS4 button shadow extends outside the animated row; avoid offscreen alpha clipping.
            val opacityModifier = if (os4) {
                val opacity by transition.animateFloat(
                    transitionSpec = {
                        if (targetState == EnterExitState.Visible) GlassMotion.fadeIn()
                        else GlassMotion.fadeOut()
                    },
                    label = "dockOpacity",
                ) { state ->
                    if (state == EnterExitState.Visible) 1f else 0f
                }
                Modifier.graphicsLayer {
                    alpha = opacity
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                }
            } else Modifier
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(opacityModifier)
                    .padding(dockPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = TopAppBarDefaults.TitlePadding),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = stringResource(R.string.home_preview_live_title),
                        style = MiuixTheme.textStyles.title4,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                SandboxDockIcon(renderModel, visual)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = if (os4) GlassTopAppBarDefaults.HorizontalPadding else TopAppBarDefaults.ActionIconPadding),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    val backTop = stringResource(R.string.diagnostics_scroll_top)
                    TooltipBox(text = backTop) {
                        if (os4) {
                            GlassIconButton(
                                onClick = scrollToTop,
                                backdrop = topBarBackdrop,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_material_symbol_vertical_align_top),
                                    contentDescription = backTop,
                                )
                            }
                        } else IconButton(
                            onClick = scrollToTop,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_material_symbol_vertical_align_top),
                                contentDescription = backTop,
                            )
                        }
                    }
                }
            }
        }
    }

    CompositionLocalProvider(LocalGlassBackdrop provides topBarBackdrop) {
        Scaffold(
            topBar = {
              Box(Modifier.onSizeChanged { nativeBarHeightPx = it.height }) {
                if (os4) {
                    GlassTopAppBar(
                        progressiveBlur = LocalTopBarProgressiveBlur.current,
                        isContentScrolled = listState.canScrollBackward,
                        title = stringResource(R.string.home_preview_sandbox_title),
                        backdrop = topBarBackdrop,
                        scrollBehavior = scrollBehavior,
                        bandOverhang = GlassTopAppBarDefaults.BandOverhang + dockHeight,
                        navigationIcon = backAction,
                        actions = barActions,
                        unclippedNavigationIcon = true,
                        unclippedActions = true,
                    )
                } else {
                    MiuixBlurredTopBar(
                        backdrop = topBarBackdrop,
                        scrollBehavior = scrollBehavior,
                        fadeExtension = dockHeight,
                    ) { barColor ->
                        SmallTopAppBar(
                            title = stringResource(R.string.home_preview_sandbox_title),
                            color = barColor,
                            scrollBehavior = scrollBehavior,
                            navigationIcon = backAction,
                            actions = barActions,
                        )
                    }
                }
              }
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
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                overscrollEffect = if (glassOverscroll) null else rememberOverscrollEffect(),
                modifier =
                    Modifier
                        .fillMaxSize()
                        .topBarBackdropSource(topBarBackdrop)
                        .then(if (canScroll) Modifier.scrollEndHaptic() else Modifier)
                        .then(if (glassOverscroll) Modifier.overScrollVertical(onOffsetChanged = onOverscrollChanged) else Modifier)
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding =
                    pageContentPadding(
                        innerPadding = paddingValues,
                        extraBottom = 12.dp,
                    ),
            ) {
                previewSandboxRows(display, choices, renderModel, visual, actions)
            }
         }
    }
    }
}
