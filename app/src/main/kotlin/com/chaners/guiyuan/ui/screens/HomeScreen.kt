package com.chaners.guiyuan.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.BuildConfig
import com.chaners.guiyuan.GyApplication
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.UiStyle
import com.chaners.guiyuan.ui.LocalUiStyle
import com.chaners.guiyuan.ui.LocalGlassBackdrop
import com.chaners.guiyuan.settings.FeatureCfg
import com.chaners.guiyuan.settings.FeatureRepo
import com.chaners.guiyuan.settings.VisualCfg
import com.chaners.guiyuan.settings.VisualRepo
import com.chaners.guiyuan.system.XposedStatus
import com.chaners.guiyuan.ui.components.StatusPreview
import com.chaners.guiyuan.ui.components.PreviewSummary
import com.chaners.guiyuan.ui.components.HotReloadAction
import com.chaners.guiyuan.ui.components.MiuixBlurredTopBar
import com.chaners.guiyuan.ui.components.rememberTopBarBackdrop
import com.chaners.guiyuan.ui.components.topBarBackdropSource
import com.chaners.guiyuan.ui.layout.pageContentPadding
import com.chaners.guiyuan.ui.theme.RuntimeSuccessAccent
import com.chaners.guiyuan.ui.theme.RuntimeWarningAccent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
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

// Keep these in sync with resolveHomeRuntimeCardState.
private val RuntimeTextVariants = listOf(
    R.string.home_runtime_disabled to R.string.home_runtime_disabled_summary,
    R.string.home_runtime_restarting to R.string.home_runtime_restarting_summary,
    R.string.home_runtime_reloading to R.string.home_runtime_reloading_summary,
    R.string.home_runtime_requesting to R.string.home_runtime_requesting_summary,
    R.string.home_runtime_checking to R.string.home_runtime_checking_summary,
    R.string.home_runtime_framework_unavailable to R.string.home_runtime_framework_unavailable_summary,
    R.string.home_runtime_unknown to R.string.home_runtime_unknown_summary,
    R.string.home_runtime_unhooked to R.string.home_runtime_unhooked_summary,
    R.string.home_runtime_running to R.string.home_runtime_running_summary,
    R.string.home_runtime_pending to R.string.home_runtime_restart_summary,
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
    val featureSettings =
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
    val xposedStatus =
        application.xposedStatus.collectAsState()
    val previewResources =
        remember(context.applicationContext) {
            PreviewSysUiResources(context.applicationContext)
        }

    val listState = rememberLazyListState()
    val canScroll = listState.canScrollBackward || listState.canScrollForward
    val scrollBehavior = MiuixScrollBehavior()
    val topBarBackdrop = rememberTopBarBackdrop()

    val os4 = LocalUiStyle.current == UiStyle.OS4
    val actions: @Composable RowScope.() -> Unit = {
        HotReloadAction(
            inProgress = hotReloadInProgress,
            enabled = !restartInProgress && !scopeRequestInProgress,
            onClick = onHotReload,
        )
    }

    CompositionLocalProvider(LocalGlassBackdrop provides topBarBackdrop) {
    Scaffold(
        topBar = {
            if (os4) {
                GlassTopAppBar(
                    isContentScrolled = listState.canScrollBackward,
                    title = stringResource(R.string.home_title),
                    backdrop = topBarBackdrop,
                    scrollBehavior = scrollBehavior,
                    actions = actions,
                    unclippedActions = true,
                )
            } else {
                MiuixBlurredTopBar(
                    backdrop = topBarBackdrop,
                    scrollBehavior = scrollBehavior,
                ) { barColor ->
                    TopAppBar(
                        title = stringResource(R.string.home_title),
                        color = barColor,
                        actions = actions,
                        scrollBehavior = scrollBehavior,
                    )
                }
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
                state = listState,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .then(if (canScroll) Modifier.scrollEndHaptic() else Modifier)
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding =
                    pageContentPadding(
                        innerPadding = paddingValues,
                        outerBottomPadding = bottomContentPadding,
                        extraBottom = 12.dp,
                    ),
            ) {
                item {
                    HomeRuntimeStatusCard(
                        featureSettings = featureSettings,
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
                                .padding(top = 8.dp, bottom = 24.dp),
                    )
                }

                item {
                    HomePreviewSandboxCard(
                        state = previewState,
                        resources = previewResources,
                        visual = visual,
                        featureSettings = featureSettings,
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
}

private data class CardPaint(val accent: Color, val gradient: Brush)

@Composable
private fun runtimeCardPaint(state: HomeRuntimeCardState, enabled: Boolean): CardPaint {
    val targetAccentColor =
        when (state.tone) {
            RuntimeStatusTone.Success -> RuntimeSuccessAccent
            RuntimeStatusTone.Warning -> RuntimeWarningAccent
            RuntimeStatusTone.Error -> MiuixTheme.colorScheme.error
            RuntimeStatusTone.Neutral ->
                if (enabled) {
                    MiuixTheme.colorScheme.onSurfaceContainerVariant
                } else {
                    lerp(
                        MiuixTheme.colorScheme.onSurfaceContainerVariant,
                        MiuixTheme.colorScheme.primary,
                        0.35f,
                    )
                }
        }
    val surfaceColor = MiuixTheme.colorScheme.surfaceContainer
    val targetContainerColor =
        when (state.tone) {
            RuntimeStatusTone.Neutral ->
                if (enabled) {
                    surfaceColor
                } else {
                    MiuixTheme.colorScheme.primary
                        .copy(alpha = 0.14f)
                        .compositeOver(surfaceColor)
                }
            else ->
                targetAccentColor
                    .copy(alpha = 0.15f)
                    .compositeOver(surfaceColor)
        }
    val accentColor by
        animateColorAsState(
            targetValue = targetAccentColor,
            animationSpec = tween(StatusColorAnimMillis),
            label = "runtimeAccent",
        )
    // Keep gradient stops fixed while the enabled state changes.
    val topTarget =
        if (enabled) {
            surfaceColor.copy(alpha = 0.24f).compositeOver(targetContainerColor)
        } else {
            targetContainerColor
        }
    val midTarget =
        if (enabled) {
            targetContainerColor
        } else {
            surfaceColor.copy(alpha = 0.24f).compositeOver(targetContainerColor)
        }
    val lowerTarget =
        surfaceColor
            .copy(alpha = if (enabled) 0.44f else 0.34f)
            .compositeOver(targetContainerColor)
    val topColor by animateColorAsState(
        targetValue = topTarget,
        animationSpec = tween(StatusColorAnimMillis),
        label = "runtimeGradientTop",
    )
    val midColor by animateColorAsState(
        targetValue = midTarget,
        animationSpec = tween(StatusColorAnimMillis),
        label = "runtimeGradientMid",
    )
    val lowerColor by animateColorAsState(
        targetValue = lowerTarget,
        animationSpec = tween(StatusColorAnimMillis),
        label = "runtimeGradientLower",
    )
    val cardGradient =
        Brush.verticalGradient(
            0f to topColor,
            0.35f to midColor,
            0.6f to lowerColor,
            1f to surfaceColor,
        )

    return CardPaint(accentColor, cardGradient)
}

@Composable
private fun HomeRuntimeStatusCard(
    featureSettings: State<FeatureCfg>,
    runtimeStatus: State<XposedStatus>,
    hotReloadInProgress: Boolean,
    restartInProgress: Boolean,
    scopeRequestInProgress: Boolean,
    onRestart: () -> Unit,
    onRequestScope: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The native Tilt node belongs to the Card, not the changing runtime content.
    Card(
        modifier = modifier,
        pressFeedbackType = PressFeedbackType.Tilt,
    ) {
        HomeRuntimeCardBody(
            featureSettings = featureSettings,
            runtimeStatus = runtimeStatus,
            hotReloadInProgress = hotReloadInProgress,
            restartInProgress = restartInProgress,
            scopeRequestInProgress = scopeRequestInProgress,
            onRestart = onRestart,
            onRequestScope = onRequestScope,
            onEnabledChange = onEnabledChange,
        )
    }
}

@Composable
private fun HomeRuntimeCardBody(
    featureSettings: State<FeatureCfg>,
    runtimeStatus: State<XposedStatus>,
    hotReloadInProgress: Boolean,
    restartInProgress: Boolean,
    scopeRequestInProgress: Boolean,
    onRestart: () -> Unit,
    onRequestScope: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
) {
    val enabled = featureSettings.value.enabled
    val state = resolveHomeRuntimeCardState(
        enabled = enabled,
        runtimeStatus = runtimeStatus.value,
        hotReloadInProgress = hotReloadInProgress,
        restartInProgress = restartInProgress,
        scopeRequestInProgress = scopeRequestInProgress,
    )
    val (accentColor, cardGradient) = runtimeCardPaint(state, enabled)
    val onCardClick = when (state.action) {
        HomeRuntimeAction.Restart -> onRestart
        HomeRuntimeAction.RequestScope -> onRequestScope
        HomeRuntimeAction.None -> null
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardGradient)
            .then(if (onCardClick == null) Modifier else Modifier.clickable(onClick = onCardClick)),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 18.dp),
        ) {
            val density = LocalDensity.current
            val textMeasurer = rememberTextMeasurer()
            val configuration = LocalConfiguration.current
            val context = LocalContext.current
            val candidates = remember(context, configuration) {
                RuntimeTextVariants.map { (title, summary) ->
                    context.getString(title) to context.getString(summary)
                }
            }
            val titleStyle = MiuixTheme.textStyles.title3.copy(fontWeight = FontWeight.Medium)
            val summaryStyle = MiuixTheme.textStyles.body2
            val textWidth = with(density) {
                (maxWidth - RuntimeStatusMarkSize - 2.dp).roundToPx().coerceAtLeast(1)
            }
            val textConstraints = Constraints(maxWidth = textWidth)
            val slotHeights = remember(candidates, textConstraints, titleStyle, summaryStyle, textMeasurer) {
                val titleHeight = candidates.maxOf { (title, _) ->
                    textMeasurer.measure(title, style = titleStyle, constraints = textConstraints).size.height
                }
                val summaryHeight = candidates.maxOf { (_, summary) ->
                    textMeasurer.measure(summary, style = summaryStyle, constraints = textConstraints).size.height
                }
                titleHeight to summaryHeight
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(state.titleRes),
                        modifier = Modifier.heightIn(min = with(density) { slotHeights.first.toDp() }),
                        style = titleStyle,
                    )
                    Text(
                        text = stringResource(
                            R.string.home_version_line,
                            BuildConfig.VERSION_NAME,
                        ),
                        modifier = Modifier.padding(top = 5.dp),
                        style = MiuixTheme.textStyles.body1.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                        color = MiuixTheme.colorScheme.onSurfaceContainer,
                    )
                    Text(
                        text = stringResource(
                            R.string.home_build_line,
                            BuildConfig.BUILD_ID,
                        ),
                        modifier = Modifier.padding(top = 1.dp),
                        style = MiuixTheme.textStyles.body1.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                        color = MiuixTheme.colorScheme.onSurfaceContainer,
                    )
                    Text(
                        text = stringResource(state.summaryRes),
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .heightIn(min = with(density) { slotHeights.second.toDp() }),
                        style = summaryStyle,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    )
                }
                RuntimeStatusMark(
                    kind = state.mark,
                    color = accentColor,
                    modifier = Modifier.size(RuntimeStatusMarkSize),
                )
            }
        }
        val switchTitle = stringResource(R.string.home_runtime_enable_title)
        BasicComponent(
            modifier = Modifier.toggleable(
                value = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch,
                onValueChange = onEnabledChange,
            ),
            title = switchTitle,
            startAction = { SemanticLeadingIcon(R.drawable.ic_guiyuan_mark) },
            endActions = {
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    modifier = Modifier.semantics { contentDescription = switchTitle },
                )
            },
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        )
    }
}

@Composable
private fun HomePreviewSandboxCard(
    state: PreviewSandboxUiState,
    resources: PreviewSysUiResources,
    visual: VisualCfg,
    featureSettings: State<FeatureCfg>,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lowBatteryThreshold = featureSettings.value.lowBatteryThreshold
    val model = remember(state, resources, lowBatteryThreshold, LocalConfiguration.current) {
        state.toRenderModel(resources, lowBatteryThreshold = lowBatteryThreshold)
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
        PreviewSummary(state, modifier = Modifier.padding(top = 4.dp))
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

    // The ring fill is decorative, not a measured framework-check percentage.
    Box(modifier = modifier.clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        val ringProgress = ring.coerceIn(0f, 1f)
        // MIUIX keeps its rotating arc until the ring starts growing or resumes it after retraction.
        CircularProgressIndicator(
            progress = if (ringProgress == 0f) null else MinSpinnerSweep + ringProgress * (1f - MinSpinnerSweep),
            size = RuntimeStatusMarkSize * 0.8f + 6.4.dp,
            strokeWidth = 6.4.dp,
            colors = ProgressIndicatorDefaults.progressIndicatorColors(
                foregroundColor = color.copy(alpha = 0.58f),
                backgroundColor = color.copy(alpha = 0.13f),
            ),
        )

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
// MIUIX's indeterminate arc starts with a 30-degree sweep.
private const val MinSpinnerSweep = 30f / 360f
private val RuntimeStatusMarkSize = 96.dp
private val HomePreviewStageHeight = 180.dp
private val HomePreviewIconSize = 112.dp
private val HomePreviewSurfaceHeight = 176.dp
