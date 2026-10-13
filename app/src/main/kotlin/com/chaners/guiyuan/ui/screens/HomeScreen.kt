package com.chaners.guiyuan.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.BuildConfig
import com.chaners.guiyuan.GyApplication
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.FeatureRepo
import com.chaners.guiyuan.settings.VisualCfg
import com.chaners.guiyuan.settings.VisualRepo
import com.chaners.guiyuan.system.XposedStatus
import com.chaners.guiyuan.ui.components.StatusPreview
import com.chaners.guiyuan.ui.components.HotReloadAction
import com.chaners.guiyuan.ui.components.MiuixBlurredTopBar
import com.chaners.guiyuan.ui.components.rememberTopBarBackdrop
import com.chaners.guiyuan.ui.components.topBarBackdropSource
import com.chaners.guiyuan.ui.layout.pageContentPadding
import com.chaners.guiyuan.ui.theme.RuntimeSuccessAccent
import com.chaners.guiyuan.ui.theme.RuntimeWarningAccent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.theme.MiuixTheme

private enum class RuntimeStatusTone {
    Success,
    Warning,
    Error,
    Neutral,
}

private enum class RuntimeStatusMarkKind {
    Check,
    Alert,
    Minus,
    Loading,
}

private enum class HomeRuntimeAction {
    None,
    Restart,
    RequestScope,
}

private data class HomeRuntimeCardState(
    val titleRes: Int,
    val summaryRes: Int,
    val tone: RuntimeStatusTone,
    val mark: RuntimeStatusMarkKind,
    val action: HomeRuntimeAction = HomeRuntimeAction.None,
)

@Composable
internal fun HomeScreen(
    bottomContentPadding: Dp,
    hotReloadInProgress: Boolean,
    restartInProgress: Boolean,
    scopeRequestInProgress: Boolean,
    onRestart: () -> Unit,
    onRequestScope: () -> Unit,
    previewState: PreviewSandboxUiState,
    onHotReload: () -> Unit,
    onOpenPreviewSandbox: () -> Unit,
) {
    val context = LocalContext.current
    val application =
        remember(context.applicationContext) {
            context.applicationContext as GyApplication
        }
    val featureRepository =
        remember(context.applicationContext) {
            FeatureRepo(context.applicationContext)
        }
    val featureSettings by
        featureRepository.settings.collectAsState(
            initial = featureRepository.current(),
        )
    val visualRepo =
        remember(context.applicationContext) {
            VisualRepo(context.applicationContext)
        }
    val visual by
        visualRepo.settings.collectAsState(
            initial = visualRepo.current(),
        )
    val xposedStatus by
        application.xposedStatus.collectAsState()
    val previewResources =
        remember(context.applicationContext) {
            PreviewSysUiResources(context.applicationContext)
        }

    val scrollBehavior = MiuixScrollBehavior()
    val topBarBackdrop = rememberTopBarBackdrop()

    Scaffold(
        topBar = {
            MiuixBlurredTopBar(
                backdrop = topBarBackdrop,
                scrollBehavior = scrollBehavior,
            ) { barColor ->
                TopAppBar(
                    title = stringResource(R.string.home_title),
                    color = barColor,
                    actions = {
                        HotReloadAction(
                            inProgress = hotReloadInProgress,
                            enabled = !restartInProgress && !scopeRequestInProgress,
                            onClick = onHotReload,
                        )
                    },
                    scrollBehavior = scrollBehavior,
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
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .scrollEndHaptic()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding =
                    pageContentPadding(
                        innerPadding = paddingValues,
                        outerBottomPadding = bottomContentPadding,
                        extraBottom = 12.dp,
                    ),
            ) {
                item {
                    SmallTitle(stringResource(R.string.section_home_runtime))
                    HomeRuntimeStatusCard(
                        enabled = featureSettings.enabled,
                        runtimeStatus = xposedStatus,
                        hotReloadInProgress = hotReloadInProgress,
                        restartInProgress = restartInProgress,
                        scopeRequestInProgress = scopeRequestInProgress,
                        onRestart = onRestart,
                        onRequestScope = onRequestScope,
                        onEnabledChange = featureRepository::setEnabled,
                        modifier =
                            Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp),
                    )
                }

                item {
                    SmallTitle(stringResource(R.string.section_home_preview_sandbox))
                    HomePreviewSandboxCard(
                        state = previewState,
                        resources = previewResources,
                        visual = visual,
                        onOpen = onOpenPreviewSandbox,
                        modifier =
                            Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeRuntimeStatusCard(
    enabled: Boolean,
    runtimeStatus: XposedStatus,
    hotReloadInProgress: Boolean,
    restartInProgress: Boolean,
    scopeRequestInProgress: Boolean,
    onRestart: () -> Unit,
    onRequestScope: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state =
        resolveHomeRuntimeCardState(
            enabled = enabled,
            runtimeStatus = runtimeStatus,
            hotReloadInProgress = hotReloadInProgress,
            restartInProgress = restartInProgress,
            scopeRequestInProgress = scopeRequestInProgress,
        )
    val targetAccentColor =
        when (state.tone) {
            RuntimeStatusTone.Success -> RuntimeSuccessAccent
            RuntimeStatusTone.Warning -> RuntimeWarningAccent
            RuntimeStatusTone.Error -> MiuixTheme.colorScheme.error
            RuntimeStatusTone.Neutral -> MiuixTheme.colorScheme.onSurfaceContainerVariant
        }
    val targetContainerColor =
        when (state.tone) {
            RuntimeStatusTone.Neutral -> MiuixTheme.colorScheme.surfaceContainer
            else ->
                targetAccentColor
                    .copy(alpha = 0.15f)
                    .compositeOver(MiuixTheme.colorScheme.surfaceContainer)
        }
    val accentColor by
        animateColorAsState(
            targetValue = targetAccentColor,
            animationSpec = tween(StatusColorAnimMillis),
            label = "runtimeAccent",
        )
    val containerColor by
        animateColorAsState(
            targetValue = targetContainerColor,
            animationSpec = tween(StatusColorAnimMillis),
            label = "runtimeContainer",
        )

    Card(
        modifier = modifier,
        colors =
            CardDefaults.defaultColors(
                color = containerColor,
                contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
            ),
        pressFeedbackType = PressFeedbackType.Tilt,
        onClick =
            when (state.action) {
                HomeRuntimeAction.Restart -> onRestart
                HomeRuntimeAction.RequestScope -> onRequestScope
                HomeRuntimeAction.None -> null
            },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(RuntimeCardHeight)
                    .padding(horizontal = 22.dp, vertical = 18.dp),
        ) {
            RuntimeStatusMark(
                kind = state.mark,
                color = accentColor,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(y = 6.dp)
                        .size(RuntimeStatusMarkSize),
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(end = 98.dp),
            ) {
                Text(
                    text = stringResource(state.titleRes),
                    style =
                        MiuixTheme.textStyles.title3.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                )
                Text(
                    text =
                        stringResource(
                            R.string.home_version_line,
                            BuildConfig.VERSION_NAME,
                        ),
                    modifier = Modifier.padding(top = 5.dp),
                    style =
                        MiuixTheme.textStyles.body1.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                )
                Text(
                    text =
                        stringResource(
                            R.string.home_build_line,
                            BuildConfig.BUILD_ID,
                        ),
                    modifier = Modifier.padding(top = 1.dp),
                    style =
                        MiuixTheme.textStyles.body1.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                )
                Text(
                    text = stringResource(state.summaryRes),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    maxLines = 2,
                )
            }

            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

@Composable
private fun HomePreviewSandboxCard(
    state: PreviewSandboxUiState,
    resources: PreviewSysUiResources,
    visual: VisualCfg,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val model = remember(state, resources, LocalConfiguration.current) {
        state.toRenderModel(resources)
    }

    Card(
        modifier = modifier,
        insideMargin = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        pressFeedbackType = PressFeedbackType.Tilt,
        showIndication = true,
        onClick = onOpen,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.home_preview_open_title),
                style =
                    MiuixTheme.textStyles.title3.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                color = MiuixTheme.colorScheme.onSurfaceContainer,
            )
            Icon(
                imageVector = MiuixIcons.Basic.ArrowRight,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .size(18.dp),
            )
        }
        Text(
            text = stringResource(R.string.home_preview_home_summary),
            modifier = Modifier.padding(top = 5.dp, end = 24.dp),
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(HomePreviewStageHeight)
                    .padding(top = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            StatusPreview(
                model = model,
                visual = visual,
                modifier =
                    Modifier
                        .width(HomePreviewIconSize)
                        .height(HomePreviewSurfaceHeight),
            )
        }
        Text(
            text = previewNetworkSummary(state),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurfaceContainer,
        )
        Text(
            text = previewBatterySummary(state),
            modifier =
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 1.dp),
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
        )
    }
}

@Composable
private fun RuntimeStatusMark(
    kind: RuntimeStatusMarkKind,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val transition = updateTransition(kind, label = "runtimeMark")
    val motion = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )
    val ring by transition.animateFloat(transitionSpec = { motion }, label = "ring") {
        if (it == RuntimeStatusMarkKind.Loading) 0f else 1f
    }
    val check by transition.animateFloat(transitionSpec = { motion }, label = "check") {
        if (it == RuntimeStatusMarkKind.Check) 1f else 0f
    }
    val alert by transition.animateFloat(transitionSpec = { motion }, label = "alert") {
        if (it == RuntimeStatusMarkKind.Alert) 1f else 0f
    }
    val minus by transition.animateFloat(transitionSpec = { motion }, label = "minus") {
        if (it == RuntimeStatusMarkKind.Minus) 1f else 0f
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val ringProgress = ring.coerceIn(0f, 1f)
        // Unmount the indefinite MIUIX spinner once the handoff is complete.
        if (ringProgress < 0.999f) {
            CircularProgressIndicator(
                modifier = Modifier.graphicsLayer { alpha = 1f - ringProgress },
                size = RuntimeStatusMarkSize * 0.8f + 6.4.dp,
                strokeWidth = 6.4.dp,
                colors = ProgressIndicatorDefaults.progressIndicatorColors(
                    foregroundColor = color.copy(alpha = 0.58f),
                    backgroundColor = color.copy(alpha = 0.13f),
                ),
            )
        }

        if (ringProgress > 0f) {
            CircularProgressIndicator(
                progress = ringProgress,
                size = RuntimeStatusMarkSize * 0.8f + 6.4.dp,
                strokeWidth = 6.4.dp,
                colors = ProgressIndicatorDefaults.progressIndicatorColors(
                    foregroundColor = color.copy(alpha = 0.58f),
                    backgroundColor = Color.Transparent,
                ),
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val markColor = color.copy(alpha = 0.58f)
            val symbolStroke = 7.2.dp.toPx()

            // The outer ring stays shared while center strokes retract or grow.
            val reveal = ((ringProgress - 0.15f) / 0.85f).coerceIn(0f, 1f)
            val checkProgress = (check * reveal).coerceIn(0f, 1f)
            if (checkProgress > 0f) {
                val path = Path().apply {
                    moveTo(size.width * 0.29f, size.height * 0.52f)
                    lineTo(size.width * 0.44f, size.height * 0.66f)
                    lineTo(size.width * 0.72f, size.height * 0.35f)
                }
                val measure = PathMeasure().apply { setPath(path, false) }
                val visible = Path()
                if (measure.getSegment(0f, measure.length * checkProgress, visible, true)) {
                    drawPath(
                        path = visible,
                        color = markColor.copy(alpha = markColor.alpha * checkProgress),
                        style = Stroke(
                            width = symbolStroke,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }
            }

            val alertProgress = (alert * reveal).coerceIn(0f, 1f)
            if (alertProgress > 0f) {
                val tint = markColor.copy(alpha = markColor.alpha * alertProgress)
                drawLine(
                    color = tint,
                    start = Offset(size.width * 0.50f, size.height * 0.29f),
                    end = Offset(size.width * 0.50f, size.height * (0.29f + 0.27f * alertProgress)),
                    strokeWidth = symbolStroke,
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    color = tint,
                    radius = symbolStroke * 0.58f * alertProgress,
                    center = Offset(size.width * 0.50f, size.height * 0.70f),
                )
            }

            val minusProgress = (minus * reveal).coerceIn(0f, 1f)
            if (minusProgress > 0f) {
                drawLine(
                    color = markColor.copy(alpha = markColor.alpha * minusProgress),
                    start = Offset(size.width * (0.50f - 0.19f * minusProgress), size.height * 0.50f),
                    end = Offset(size.width * (0.50f + 0.19f * minusProgress), size.height * 0.50f),
                    strokeWidth = symbolStroke,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

private fun resolveHomeRuntimeCardState(
    enabled: Boolean,
    runtimeStatus: XposedStatus,
    hotReloadInProgress: Boolean,
    restartInProgress: Boolean,
    scopeRequestInProgress: Boolean,
): HomeRuntimeCardState {
    if (!enabled) {
        return HomeRuntimeCardState(
            titleRes = R.string.home_runtime_disabled,
            summaryRes = R.string.home_runtime_disabled_summary,
            tone = RuntimeStatusTone.Neutral,
            mark = RuntimeStatusMarkKind.Minus,
        )
    }

    if (restartInProgress) {
        return HomeRuntimeCardState(
            titleRes = R.string.home_runtime_restarting,
            summaryRes = R.string.home_runtime_restarting_summary,
            tone = RuntimeStatusTone.Warning,
            mark = RuntimeStatusMarkKind.Loading,
        )
    }

    if (hotReloadInProgress) {
        return HomeRuntimeCardState(
            titleRes = R.string.home_runtime_reloading,
            summaryRes = R.string.home_runtime_reloading_summary,
            tone = RuntimeStatusTone.Warning,
            mark = RuntimeStatusMarkKind.Loading,
        )
    }

    if (scopeRequestInProgress) {
        return HomeRuntimeCardState(
            titleRes = R.string.home_runtime_requesting,
            summaryRes = R.string.home_runtime_requesting_summary,
            tone = RuntimeStatusTone.Warning,
            mark = RuntimeStatusMarkKind.Loading,
        )
    }

    return when (runtimeStatus) {
        XposedStatus.Checking ->
            HomeRuntimeCardState(
                titleRes = R.string.home_runtime_checking,
                summaryRes = R.string.home_runtime_checking_summary,
                tone = RuntimeStatusTone.Neutral,
                mark = RuntimeStatusMarkKind.Loading,
            )

        XposedStatus.FrameworkUnavailable ->
            HomeRuntimeCardState(
                titleRes = R.string.home_runtime_framework_unavailable,
                summaryRes = R.string.home_runtime_framework_unavailable_summary,
                tone = RuntimeStatusTone.Error,
                mark = RuntimeStatusMarkKind.Alert,
            )

        XposedStatus.QueryUnavailable ->
            HomeRuntimeCardState(
                titleRes = R.string.home_runtime_unknown,
                summaryRes = R.string.home_runtime_unknown_summary,
                tone = RuntimeStatusTone.Warning,
                mark = RuntimeStatusMarkKind.Alert,
            )

        is XposedStatus.Connected ->
            when {
                !runtimeStatus.sysUiInScope ->
                    HomeRuntimeCardState(
                        titleRes = R.string.home_runtime_unhooked,
                        summaryRes = R.string.home_runtime_unhooked_summary,
                        tone = RuntimeStatusTone.Error,
                        mark = RuntimeStatusMarkKind.Alert,
                        action = HomeRuntimeAction.RequestScope,
                    )

                runtimeStatus.sysUiRunning ->
                    HomeRuntimeCardState(
                        titleRes = R.string.home_runtime_running,
                        summaryRes = R.string.home_runtime_running_summary,
                        tone = RuntimeStatusTone.Success,
                        mark = RuntimeStatusMarkKind.Check,
                    )

                else ->
                    HomeRuntimeCardState(
                        titleRes = R.string.home_runtime_pending,
                        summaryRes = R.string.home_runtime_restart_summary,
                        tone = RuntimeStatusTone.Warning,
                        mark = RuntimeStatusMarkKind.Alert,
                        action = HomeRuntimeAction.Restart,
                    )
            }
    }
}

private const val StatusColorAnimMillis = 220
private val RuntimeCardHeight = 160.dp
private val RuntimeStatusMarkSize = 96.dp
private val HomePreviewStageHeight = 180.dp
private val HomePreviewIconSize = 112.dp
private val HomePreviewSurfaceHeight = 176.dp
