package com.chaners.guiyuan.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.BuildConfig
import com.chaners.guiyuan.R
import com.chaners.guiyuan.ui.theme.RuntimeSuccessAccent
import com.chaners.guiyuan.ui.theme.RuntimeWarningAccent
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

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


internal data class CardPaint(val accent: Color, val gradient: Brush)

private data class CardTargetColors(
    val accent: Color,
    val surface: Color,
    val container: Color,
)

// Resolve palette targets separately; keep the existing animation and gradient geometry unchanged.
@Composable
private fun runtimeCardTargetColors(
    tone: RuntimeStatusTone,
    enabled: Boolean,
): CardTargetColors {
    val targetAccentColor =
        when (tone) {
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
        when (tone) {
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
    return CardTargetColors(
        accent = targetAccentColor,
        surface = surfaceColor,
        container = targetContainerColor,
    )
}

@Composable
internal fun runtimeCardPaint(state: HomeRuntimeCardState, enabled: Boolean): CardPaint {
    val targets = runtimeCardTargetColors(state.tone, enabled)
    val targetAccentColor = targets.accent
    val surfaceColor = targets.surface
    val targetContainerColor = targets.container
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
internal fun HomeRuntimeStatusDetails(
    state: HomeRuntimeCardState,
    accentColor: Color,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 18.dp),
    ) {
        val titleStyle = MiuixTheme.textStyles.title3.copy(fontWeight = FontWeight.SemiBold)
        val bodyStyle = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Medium)
        val summaryStyle = MiuixTheme.textStyles.body2
        val versionText = stringResource(R.string.home_version_line, BuildConfig.VERSION_NAME)
        val buildText = stringResource(R.string.home_build_line, BuildConfig.BUILD_ID)
        val textHeight = rememberRuntimeTextHeight(
            titleStyle = titleStyle,
            bodyStyle = bodyStyle,
            summaryStyle = summaryStyle,
            versionText = versionText,
            buildText = buildText,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).heightIn(min = textHeight)) {
                Text(
                    text = stringResource(state.titleRes),
                    style = titleStyle,
                )
                Text(
                    text = versionText,
                    modifier = Modifier.padding(top = 5.dp),
                    style = bodyStyle,
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                )
                Text(
                    text = buildText,
                    modifier = Modifier.padding(top = 1.dp),
                    style = bodyStyle,
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                )
                Text(
                    text = stringResource(state.summaryRes),
                    modifier = Modifier.padding(top = 12.dp),
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
}

@Composable
private fun BoxWithConstraintsScope.rememberRuntimeTextHeight(
    titleStyle: TextStyle,
    bodyStyle: TextStyle,
    summaryStyle: TextStyle,
    versionText: String,
    buildText: String,
): Dp {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val candidates = remember(context, configuration) {
        RuntimeTextVariants.map { (title, summary) ->
            context.getString(title) to context.getString(summary)
        }
    }
    val textWidth = with(density) {
        (maxWidth - RuntimeStatusMarkSize - 2.dp).roundToPx().coerceAtLeast(1)
    }
    val textConstraints = Constraints(maxWidth = textWidth)
    return remember(candidates, textConstraints, titleStyle, bodyStyle, summaryStyle, versionText, buildText, textMeasurer, density) {
        val fixedHeight =
            textMeasurer.measure(versionText, style = bodyStyle, constraints = textConstraints).size.height +
                textMeasurer.measure(buildText, style = bodyStyle, constraints = textConstraints).size.height
        val changingHeight = candidates.maxOf { (title, summary) ->
            textMeasurer.measure(title, style = titleStyle, constraints = textConstraints).size.height +
                textMeasurer.measure(summary, style = summaryStyle, constraints = textConstraints).size.height
        }
        val spacing = with(density) { 5.dp.roundToPx() + 1.dp.roundToPx() + 12.dp.roundToPx() }
        with(density) { (fixedHeight + changingHeight + spacing).toDp() }
    }
}

private const val StatusColorAnimMillis = 220
internal val RuntimeStatusMarkSize = 96.dp
