package com.chaners.guiyuan.ui.screens

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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.BuildConfig
import com.chaners.guiyuan.CombinedStatusApplication
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.CombinedStatusFeatureSettingsRepository
import com.chaners.guiyuan.settings.CombinedStatusVisualSettings
import com.chaners.guiyuan.settings.CombinedStatusVisualSettingsRepository
import com.chaners.guiyuan.system.XposedRuntimeStatus
import com.chaners.guiyuan.ui.components.CombinedStatusPreview
import com.chaners.guiyuan.ui.components.HotReloadAction
import com.chaners.guiyuan.ui.components.MiuixBlurredTopBar
import com.chaners.guiyuan.ui.components.rememberTopBarBackdrop
import com.chaners.guiyuan.ui.components.topBarBackdropSource
import com.chaners.guiyuan.ui.layout.pageContentPadding
import com.chaners.guiyuan.ui.theme.RuntimeSuccessAccent
import com.chaners.guiyuan.ui.theme.RuntimeWarningAccent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
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
}

private data class HomeRuntimeCardState(
    val titleRes: Int,
    val summaryRes: Int,
    val tone: RuntimeStatusTone,
    val mark: RuntimeStatusMarkKind,
)

@Composable
internal fun HomeScreen(
    bottomContentPadding: Dp,
    hotReloadInProgress: Boolean,
    previewState: PreviewSandboxUiState,
    onHotReload: () -> Unit,
    onOpenPreviewSandbox: () -> Unit,
) {
    val context = LocalContext.current
    val application =
        remember(context.applicationContext) {
            context.applicationContext as CombinedStatusApplication
        }
    val featureRepository =
        remember(context.applicationContext) {
            CombinedStatusFeatureSettingsRepository(context.applicationContext)
        }
    val featureSettings by
        featureRepository.settings.collectAsState(
            initial = featureRepository.current(),
        )
    val visualRepository =
        remember(context.applicationContext) {
            CombinedStatusVisualSettingsRepository(context.applicationContext)
        }
    val visualSettings by
        visualRepository.settings.collectAsState(
            initial = visualRepository.current(),
        )
    val xposedRuntimeStatus by
        application.xposedRuntimeStatus.collectAsState()
    val previewResources =
        remember(context.applicationContext) {
            PreviewSystemUiResourceResolver(context.applicationContext)
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
                        runtimeStatus = xposedRuntimeStatus,
                        hotReloadInProgress = hotReloadInProgress,
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
                        visualSettings = visualSettings,
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
    runtimeStatus: XposedRuntimeStatus,
    hotReloadInProgress: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state =
        resolveHomeRuntimeCardState(
            enabled = enabled,
            runtimeStatus = runtimeStatus,
            hotReloadInProgress = hotReloadInProgress,
        )
    val accentColor =
        when (state.tone) {
            RuntimeStatusTone.Success -> RuntimeSuccessAccent
            RuntimeStatusTone.Warning -> RuntimeWarningAccent
            RuntimeStatusTone.Error -> MiuixTheme.colorScheme.error
            RuntimeStatusTone.Neutral -> MiuixTheme.colorScheme.onSurfaceContainerVariant
        }
    val containerColor =
        when (state.tone) {
            RuntimeStatusTone.Neutral -> MiuixTheme.colorScheme.surfaceContainer
            else ->
                accentColor
                    .copy(alpha = 0.15f)
                    .compositeOver(MiuixTheme.colorScheme.surfaceContainer)
        }

    Card(
        modifier = modifier,
        colors =
            CardDefaults.defaultColors(
                color = containerColor,
                contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
            ),
        pressFeedbackType = PressFeedbackType.Tilt,
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
    resources: PreviewSystemUiResourceResolver,
    visualSettings: CombinedStatusVisualSettings,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            CombinedStatusPreview(
                model = state.toRenderModel(resources),
                visualSettings = visualSettings,
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
    Canvas(modifier = modifier) {
        val markColor = color.copy(alpha = 0.58f)
        val ringStrokeWidth = 6.4.dp.toPx()
        val symbolStrokeWidth = 7.2.dp.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.40f

        drawCircle(
            color = markColor,
            radius = radius,
            center = center,
            style =
                Stroke(
                    width = ringStrokeWidth,
                    cap = StrokeCap.Round,
                ),
        )

        when (kind) {
            RuntimeStatusMarkKind.Check -> {
                val checkPath =
                    Path().apply {
                        moveTo(size.width * 0.29f, size.height * 0.52f)
                        lineTo(size.width * 0.44f, size.height * 0.66f)
                        lineTo(size.width * 0.72f, size.height * 0.35f)
                    }
                drawPath(
                    path = checkPath,
                    color = markColor,
                    style =
                        Stroke(
                            width = symbolStrokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                )
            }

            RuntimeStatusMarkKind.Alert -> {
                drawLine(
                    color = markColor,
                    start = Offset(size.width * 0.50f, size.height * 0.29f),
                    end = Offset(size.width * 0.50f, size.height * 0.56f),
                    strokeWidth = symbolStrokeWidth,
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    color = markColor,
                    radius = symbolStrokeWidth * 0.58f,
                    center = Offset(size.width * 0.50f, size.height * 0.70f),
                )
            }

            RuntimeStatusMarkKind.Minus -> {
                drawLine(
                    color = markColor,
                    start = Offset(size.width * 0.31f, size.height * 0.50f),
                    end = Offset(size.width * 0.69f, size.height * 0.50f),
                    strokeWidth = symbolStrokeWidth,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

private fun resolveHomeRuntimeCardState(
    enabled: Boolean,
    runtimeStatus: XposedRuntimeStatus,
    hotReloadInProgress: Boolean,
): HomeRuntimeCardState {
    if (!enabled) {
        return HomeRuntimeCardState(
            titleRes = R.string.home_runtime_disabled,
            summaryRes = R.string.home_runtime_disabled_summary,
            tone = RuntimeStatusTone.Neutral,
            mark = RuntimeStatusMarkKind.Minus,
        )
    }

    if (hotReloadInProgress) {
        return HomeRuntimeCardState(
            titleRes = R.string.home_runtime_reloading,
            summaryRes = R.string.home_runtime_reloading_summary,
            tone = RuntimeStatusTone.Warning,
            mark = RuntimeStatusMarkKind.Alert,
        )
    }

    return when (runtimeStatus) {
        XposedRuntimeStatus.Checking ->
            HomeRuntimeCardState(
                titleRes = R.string.home_runtime_checking,
                summaryRes = R.string.home_runtime_checking_summary,
                tone = RuntimeStatusTone.Warning,
                mark = RuntimeStatusMarkKind.Alert,
            )

        XposedRuntimeStatus.FrameworkUnavailable ->
            HomeRuntimeCardState(
                titleRes = R.string.home_runtime_framework_unavailable,
                summaryRes = R.string.home_runtime_framework_unavailable_summary,
                tone = RuntimeStatusTone.Error,
                mark = RuntimeStatusMarkKind.Alert,
            )

        XposedRuntimeStatus.QueryUnavailable ->
            HomeRuntimeCardState(
                titleRes = R.string.home_runtime_unknown,
                summaryRes = R.string.home_runtime_unknown_summary,
                tone = RuntimeStatusTone.Warning,
                mark = RuntimeStatusMarkKind.Alert,
            )

        is XposedRuntimeStatus.Connected ->
            when {
                !runtimeStatus.systemUiInScope ->
                    HomeRuntimeCardState(
                        titleRes = R.string.home_runtime_unhooked,
                        summaryRes = R.string.home_runtime_unhooked_summary,
                        tone = RuntimeStatusTone.Error,
                        mark = RuntimeStatusMarkKind.Alert,
                    )

                runtimeStatus.systemUiRunning ->
                    HomeRuntimeCardState(
                        titleRes = R.string.home_runtime_running,
                        summaryRes = R.string.home_runtime_running_summary,
                        tone = RuntimeStatusTone.Success,
                        mark = RuntimeStatusMarkKind.Check,
                    )

                else ->
                    HomeRuntimeCardState(
                        titleRes = R.string.home_runtime_pending,
                        summaryRes = R.string.home_runtime_pending_summary,
                        tone = RuntimeStatusTone.Warning,
                        mark = RuntimeStatusMarkKind.Alert,
                    )
            }
    }
}

private val RuntimeCardHeight = 160.dp
private val RuntimeStatusMarkSize = 96.dp
private val HomePreviewStageHeight = 180.dp
private val HomePreviewIconSize = 112.dp
private val HomePreviewSurfaceHeight = 176.dp
