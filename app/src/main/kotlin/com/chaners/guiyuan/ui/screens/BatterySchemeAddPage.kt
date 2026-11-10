package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.BATTERY_COLOR_SCHEME_CUSTOM_MAX
import com.chaners.guiyuan.settings.BatteryBuiltInColorScheme
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.FloatingActionButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SnackbarDefaults
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

@Composable
internal fun BatteryAddSchemePage(
    count: Int,
    enabled: Boolean,
    name: String,
    settingsCardHeightPx: Int,
    onClick: () -> Unit,
) {
    val density = LocalDensity.current
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            BatterySchemeHeader(
                name = name,
                previewPage = BatterySchemePage.BuiltIn(BatteryBuiltInColorScheme.HYPEROS),
            )
            BatterySchemeActionArea {
                TextButton(
                    text =
                        stringResource(
                            if (enabled) {
                                R.string.battery_custom_scheme_add_page_title
                            } else {
                                R.string.battery_custom_scheme_limit
                            },
                        ),
                    onClick = onClick,
                    enabled = enabled,
                    minWidth = 26.dp,
                    minHeight = 26.dp,
                    cornerRadius = SnackbarDefaults.ActionCornerRadius,
                    insideMargin = SnackbarDefaults.ActionInsideMargin,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    textStyle = TextStyle(fontSize = 15.sp),
                )
            }
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .then(
                            if (settingsCardHeightPx > 0) {
                                Modifier.height(with(density) { settingsCardHeightPx.toDp() })
                            } else {
                                Modifier.heightIn(min = 400.dp)
                            },
                        ),
                pressFeedbackType =
                    if (enabled) PressFeedbackType.Sink else PressFeedbackType.None,
                onClick = if (enabled) onClick else null,
            ) {
                BatteryAddSchemeCardBody(count, enabled)
            }
        }
    }
}

@Composable
private fun BatteryAddSchemeCardBody(count: Int, enabled: Boolean) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(FloatingActionButtonDefaults.MinWidth),
            shape = CircleShape,
            color =
                if (enabled) MiuixTheme.colorScheme.primary
                else MiuixTheme.colorScheme.disabledPrimaryButton,
            shadowElevation = 0.dp,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MiuixIcons.Add,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint =
                        if (enabled) MiuixTheme.colorScheme.onPrimary
                        else MiuixTheme.colorScheme.disabledOnPrimaryButton,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text =
                if (enabled) stringResource(R.string.battery_scheme_new_summary)
                else stringResource(R.string.battery_scheme_full_summary, BATTERY_COLOR_SCHEME_CUSTOM_MAX),
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        BatterySchemeCapacityStatus(count, enabled)
    }
}

@Composable
private fun BatterySchemeCapacityStatus(count: Int, enabled: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.battery_scheme_capacity),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Text(
                text = "$count / $BATTERY_COLOR_SCHEME_CUSTOM_MAX",
                style = MiuixTheme.textStyles.body2,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            repeat(BATTERY_COLOR_SCHEME_CUSTOM_MAX) { index ->
                Surface(
                    modifier = Modifier.weight(1f).height(6.dp),
                    shape = CircleShape,
                    color =
                        if (index < count) MiuixTheme.colorScheme.primary
                        else MiuixTheme.colorScheme.onSurface.copy(alpha = 0.14f),
                ) {}
            }
        }
        Text(
            text =
                if (enabled) stringResource(
                    R.string.battery_scheme_remaining,
                    BATTERY_COLOR_SCHEME_CUSTOM_MAX - count,
                ) else stringResource(R.string.battery_scheme_full_hint),
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}
