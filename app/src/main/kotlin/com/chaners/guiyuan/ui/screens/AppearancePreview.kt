package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.Appearance
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun AppearanceThemePreview(
    appearance: Appearance,
    dark: Boolean,
) {
    Card(
        modifier =
            Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.theme_preview),
                style = MiuixTheme.textStyles.headline1.copy(fontWeight = FontWeight.Medium),
                color = MiuixTheme.colorScheme.onSurfaceContainer,
            )
            Text(
                text = stringResource(R.string.theme_preview_summary),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )

            AppearanceMiniPreview(
                appearance = appearance,
                dark = dark,
            )
        }
    }
}

@Composable
private fun AppearanceMiniPreview(
    appearance: Appearance,
    dark: Boolean,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        shape = RoundedCornerShape(18.dp),
        color = MiuixTheme.colorScheme.surface,
        border =
            BorderStroke(
                width = 1.dp,
                color = MiuixTheme.colorScheme.outline.copy(alpha = 0.18f),
            ),
    ) {
        ScaledPreviewContent(
            scale = MiniPreviewScale,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, top = 13.dp, end = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MiniPreviewHeader()
                MiniSwitchSettingPreview()
                MiniSliderSettingPreview()
                MiniNavigationPreview(
                    uiStyle = appearance.uiStyle,
                    floating = appearance.navEnabled,
                    style = appearance.navStyle,
                    content = appearance.activeNavContent,
                    liquidMode = appearance.liquidMode,
                    dark = dark,
                )
            }
        }
    }
}

@Composable
private fun MiniPreviewHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MiniTextBar(width = 88.dp, role = MiniTextRole.Title)
            MiniTextBar(width = 54.dp, role = MiniTextRole.Body2)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            MiniThemeSwatch(MiuixTheme.colorScheme.primary)
            MiniThemeSwatch(MiuixTheme.colorScheme.secondary)
            MiniThemeSwatch(MiuixTheme.colorScheme.surfaceContainerHigh)
        }
    }
}

@Composable
private fun MiniSwitchSettingPreview() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MiniTextBar(width = 82.dp, role = MiniTextRole.Body1)
                MiniTextBar(width = 56.dp, role = MiniTextRole.Body2)
            }
            Switch(
                checked = true,
                onCheckedChange = null,
            )
        }
    }
}

@Composable
private fun MiniSliderSettingPreview() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Surface(
                    modifier = Modifier.size(14.dp),
                    shape = RoundedCornerShape(7.dp),
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant.copy(alpha = 0.20f),
                ) {}
                MiniTextBar(width = 72.dp, role = MiniTextRole.Body1)
            }
            Slider(
                value = 0.43f,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                colors =
                    SliderDefaults.sliderColors(
                        disabledForegroundColor = MiuixTheme.colorScheme.primary,
                        disabledBackgroundColor = MiuixTheme.colorScheme.sliderBackground,
                        disabledThumbColor = MiuixTheme.colorScheme.onPrimary,
                    ),
            )
        }
    }
}

internal enum class MiniTextRole {
    Title,
    Body1,
    Body2,
}

@Composable
internal fun MiniTextBar(
    width: androidx.compose.ui.unit.Dp,
    role: MiniTextRole,
    modifier: Modifier = Modifier,
) {
    val fontSize =
        when (role) {
            MiniTextRole.Title -> MiuixTheme.textStyles.title3.fontSize
            MiniTextRole.Body1 -> MiuixTheme.textStyles.body1.fontSize
            MiniTextRole.Body2 -> MiuixTheme.textStyles.body2.fontSize
        }
    val height = (fontSize.value * 0.35f).dp
    val alpha =
        when (role) {
            MiniTextRole.Title -> 0.34f
            MiniTextRole.Body1 -> 0.27f
            MiniTextRole.Body2 -> 0.17f
        }

    Surface(
        modifier =
            modifier
                .width(width)
                .height(height),
        shape = RoundedCornerShape(height / 2f),
        color = MiuixTheme.colorScheme.onSurfaceContainerVariant.copy(alpha = alpha),
    ) {}
}

@Composable
private fun MiniThemeSwatch(color: Color) {
    Surface(
        modifier = Modifier.size(14.dp),
        shape = CircleShape,
        color = color,
        border =
            BorderStroke(
                width = 1.dp,
                color = MiuixTheme.colorScheme.outline.copy(alpha = 0.26f),
            ),
    ) {}
}

private const val MiniPreviewScale = 0.82f

@Composable
private fun ScaledPreviewContent(
    scale: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier,
        content = content,
    ) { measurables, constraints ->
        val maxWidth =
            if (constraints.hasBoundedWidth) {
                (constraints.maxWidth / scale).roundToInt()
            } else {
                constraints.maxWidth
            }
        val placeable =
            measurables.single().measure(
                Constraints(
                    minWidth = 0,
                    maxWidth = maxWidth,
                    minHeight = 0,
                    maxHeight = Constraints.Infinity,
                ),
            )
        val scaledHeight = (placeable.height * scale).roundToInt()
        val layoutHeight =
            scaledHeight.coerceIn(
                constraints.minHeight,
                if (constraints.hasBoundedHeight) constraints.maxHeight else scaledHeight,
            )

        layout(constraints.maxWidth, layoutHeight) {
            placeable.placeRelativeWithLayer(0, 0) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
}
