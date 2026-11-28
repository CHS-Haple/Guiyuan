package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.BATTERY_COLOR_SCHEME_CUSTOM_MAX
import com.chaners.guiyuan.settings.BatteryBuiltInColorScheme
import top.yukonga.miuix.kmp.basic.FloatingActionButtonDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.glass.GlassCard
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.icon.symbol.Add
import top.yukonga.miuix.kmp.symbol.rememberSymbolPainter

@Composable
internal fun BatteryAddSchemePage(
    count: Int,
    enabled: Boolean,
    scrollState: ScrollState,
    contentPadding: PaddingValues,
    onClick: () -> Unit,
) {
    val canScroll = scrollState.canScrollBackward || scrollState.canScrollForward
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (canScroll) Modifier.scrollEndHaptic() else Modifier)

            .verticalScroll(scrollState)
            .padding(contentPadding),
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            insideMargin = PaddingValues(16.dp),
        ) {
            BatterySchemeHeader(
                name = stringResource(R.string.battery_custom_scheme_new),
                previewPage = BatterySchemePage.BuiltIn(BatteryBuiltInColorScheme.HYPEROS),
            )
            BatterySchemeActionArea {
                Text(
                    text = stringResource(R.string.battery_scheme_initial_palette),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center,
                )
            }
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                pressFeedbackType = if (enabled) PressFeedbackType.Sink else PressFeedbackType.None,
                onClick = if (enabled) onClick else null,
            ) {
                BatteryAddSchemeCardBody(count, enabled)
            }
        }
    }
}

@Composable
private fun BatteryAddSchemeCardBody(count: Int, enabled: Boolean) {
    // Measure the same native rows as the existing scheme cards in this layout pass.
    // The reference is never placed: no off-screen hit targets or cross-page height cache.
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                BatterySchemeSettingRows(
                    builtIn = BatteryBuiltInColorScheme.HYPEROS,
                    custom = null,
                    canCreateCustom = false,
                    onSlotClick = {},
                )
            }
            BatteryAddSchemeCreationGlyph(enabled)
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp),
            ) {
                HorizontalDivider()
                BatterySchemeCapacityStatus(count, enabled)
            }
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minHeight = 0)
        val reference = measurables[0].measure(loose)
        val footer = measurables[2].measure(loose)
        val creationConstraints =
            if (loose.hasBoundedHeight) {
                loose.copy(maxHeight = (loose.maxHeight - footer.height).coerceAtLeast(0))
            } else {
                loose
            }
        val creation = measurables[1].measure(creationConstraints)
        val height = maxOf(reference.height, creation.height + footer.height)
            .coerceIn(constraints.minHeight, constraints.maxHeight)
        val footerTop = height - footer.height
        layout(reference.width, height) {
            creation.placeRelative(0, (footerTop - creation.height) / 2)
            footer.placeRelative(0, footerTop)
        }
    }
}

@Composable
private fun BatteryAddSchemeCreationGlyph(enabled: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(FloatingActionButtonDefaults.MinWidth),
            shape = CircleShape,
            color =
                if (enabled) MiuixTheme.colorScheme.primary
                else MiuixTheme.colorScheme.disabledPrimaryButton,
            shadowElevation = 0.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = rememberSymbolPainter(MiuixIcons.Symbol.Add),
                    contentDescription = stringResource(
                        if (enabled) R.string.battery_custom_scheme_new
                        else R.string.battery_custom_scheme_limit,
                    ),
                    modifier = Modifier.size(32.dp),
                    tint =
                        if (enabled) MiuixTheme.colorScheme.onPrimary
                        else MiuixTheme.colorScheme.disabledOnPrimaryButton,
                )
            }
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
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = count.toString(),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    text = "/ $BATTERY_COLOR_SCHEME_CUSTOM_MAX",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.alignByBaseline(),
                )
            }
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
