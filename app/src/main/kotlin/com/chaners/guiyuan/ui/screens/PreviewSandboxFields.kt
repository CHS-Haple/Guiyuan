package com.chaners.guiyuan.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.UiStyle
import com.chaners.guiyuan.ui.LocalUiStyle
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.glass.GlassSegmentedTabRow
import top.yukonga.miuix.kmp.glass.GlassSegmentedTabRowDefaults
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.TabRowDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal enum class SandboxField(
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
    @DrawableRes val detailRes: Int? = null,
) {
    Sim(R.string.home_preview_sim_title, R.drawable.ic_material_symbol_sim_card),
    BatteryMode(R.string.home_preview_battery_mode_title, R.drawable.ic_material_symbol_battery_5_bar, R.drawable.ic_material_symbol_tune),
    ChargingState(R.string.home_preview_charging_state_title, R.drawable.ic_material_symbol_bolt),
}

@Composable
internal fun SandboxSegmentedField(
    field: SandboxField,
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val title = stringResource(field.titleRes)
        val glass = LocalUiStyle.current == UiStyle.OS4 && isRuntimeShaderSupported()
        val requiredWidth = if (glass) sandboxSegmentedRequiredWidth(options) else 0

        if (glass && requiredWidth > constraints.maxWidth) {
            // Equal-width segments cannot fit these labels; show the full options in a popup.
            UiDropdownPreference(
                items = options,
                selectedIndex = selectedIndex,
                title = title,
                startAction = { SemanticLeadingIcon(field.iconRes, detailRes = field.detailRes) },
                onSelectedIndexChange = onSelected,
                modifier = Modifier.fillMaxWidth(),
                insideMargin = SandboxPreferenceInsideMargin,
            )
        } else {
            BasicComponent(
                title = title,
                startAction = { SemanticLeadingIcon(field.iconRes, detailRes = field.detailRes) },
                modifier = Modifier.fillMaxWidth(),
                insideMargin = SandboxPreferenceInsideMargin,
                bottomAction = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        UiTabRow(
                            tabs = options,
                            selectedTabIndex = selectedIndex,
                            onTabSelected = onSelected,
                            modifier = if (glass) Modifier.fillMaxWidth() else Modifier.widthIn(max = 300.dp).fillMaxWidth(),
                        )
                    }
                },
            )
        }
    }
}


@Composable
private fun sandboxSegmentedRequiredWidth(options: List<String>): Int {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val selectedStyle = MiuixTheme.textStyles.subtitle
    val idleStyle = MiuixTheme.textStyles.body2
    return remember(options, measurer, density, layoutDirection, selectedStyle, idleStyle) {
        val widest = options.maxOfOrNull { option ->
            val text = AnnotatedString(option)
            maxOf(
                measurer.measure(text, style = selectedStyle, softWrap = false, maxLines = 1).size.width,
                measurer.measure(text, style = idleStyle, softWrap = false, maxLines = 1).size.width,
            )
        } ?: 0
        val spacing = with(density) {
            (
                GlassSegmentedTabRowDefaults.TrackPadding * 2 +
                    GlassSegmentedTabRowDefaults.TabPaddingHorizontal * (options.size * 2) +
                    GlassSegmentedTabRowDefaults.TabGap * (options.size - 1).coerceAtLeast(0) +
                    SandboxPreferenceInsideMargin.calculateStartPadding(layoutDirection) +
                    SandboxPreferenceInsideMargin.calculateEndPadding(layoutDirection)
                ).roundToPx()
        }
        widest * options.size + spacing
    }
}

internal val SandboxPreferenceInsideMargin =
    PaddingValues(
        horizontal = 16.dp,
        vertical = 10.dp,
    )

@Composable
internal fun UiTabRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    inCard: Boolean = true,
) {
    if (LocalUiStyle.current == UiStyle.OS4 && isRuntimeShaderSupported()) {
        // A tab inside the scrolling page must not sample the page's own
        // backdrop: that recorder also contains this tab. Record only its base.
        val surface = if (inCard) MiuixTheme.colorScheme.surfaceContainer else MiuixTheme.colorScheme.background
        val backdrop = rememberLayerBackdrop { drawRect(surface) }
        Box(modifier = modifier) {
            Box(Modifier.matchParentSize().layerBackdrop(backdrop))
            GlassSegmentedTabRow(
                tabs = tabs,
                selectedIndex = selectedTabIndex,
                onSelect = onTabSelected,
                backdrop = backdrop,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        TabRowWithContour(
            tabs = tabs,
            selectedTabIndex = selectedTabIndex,
            onTabSelected = onTabSelected,
            modifier = modifier,
            colors = TabRowDefaults.tabRowColors(
                backgroundColor = if (inCard) MiuixTheme.colorScheme.surface else MiuixTheme.colorScheme.surfaceContainerHighest,
            ),
        )
    }
}
