package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.BATTERY_COLOR_SCHEME_CUSTOM_MAX
import com.chaners.guiyuan.settings.BatteryBuiltInColorScheme
import top.yukonga.miuix.kmp.basic.FloatingActionButtonDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.glass.GlassCard
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
internal fun BatteryAddSchemePage(
    count: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val canScroll = scrollState.canScrollBackward || scrollState.canScrollForward
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (canScroll) Modifier.scrollEndHaptic() else Modifier)
            .verticalScroll(scrollState),
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            insideMargin = PaddingValues(16.dp),
        ) {
            BatterySchemeHeader(
                name = stringResource(R.string.battery_custom_scheme_new),
                previewPage = BatterySchemePage.BuiltIn(BatteryBuiltInColorScheme.HYPEROS),
            )
            Text(
                text = stringResource(R.string.battery_scheme_initial_palette),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.fillMaxWidth().padding(top = BATTERY_SCHEME_VERTICAL_GAP),
                textAlign = TextAlign.Center,
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(BATTERY_SCHEME_VERTICAL_GAP),
            ) {
                IconButton(
                    onClick = onClick,
                    enabled = enabled,
                    minWidth = FloatingActionButtonDefaults.MinWidth,
                    minHeight = FloatingActionButtonDefaults.MinHeight,
                    backgroundColor =
                        if (enabled) MiuixTheme.colorScheme.primary
                        else MiuixTheme.colorScheme.disabledPrimaryButton,
                ) {
                    Icon(
                        imageVector = MiuixIcons.Add,
                        contentDescription = stringResource(R.string.battery_custom_scheme_new),
                        modifier = Modifier.size(32.dp),
                        tint =
                            if (enabled) MiuixTheme.colorScheme.onPrimary
                            else MiuixTheme.colorScheme.disabledOnPrimaryButton,
                    )
                }
                Text(
                    text = stringResource(
                        if (enabled) R.string.battery_scheme_new_summary
                        else R.string.battery_custom_scheme_limit,
                    ),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center,
                )
            }
            HorizontalDivider()
            BatterySchemeCapacityStatus(count, enabled)
        }
    }
}

@Composable
private fun BatterySchemeCapacityStatus(count: Int, enabled: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
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
        if (!enabled) {
            Text(
                text = stringResource(R.string.battery_scheme_full_hint),
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}
