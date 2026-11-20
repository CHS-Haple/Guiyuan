package com.chaners.guiyuan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.Appearance
import com.chaners.guiyuan.settings.LiquidMode
import com.chaners.guiyuan.settings.NavContent
import com.chaners.guiyuan.settings.NavStyle
import com.chaners.guiyuan.settings.ThemeMode
import com.chaners.guiyuan.settings.UiStyle
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

internal data class AppearanceThemeActions(
    val onThemeChange: (ThemeMode) -> Unit,
    val onDynamicChange: (Boolean) -> Unit,
    val onColorSpecChange: (ThemeColorSpec?) -> Unit,
    val onPaletteStyleChange: (ThemePaletteStyle) -> Unit,
)

internal data class AppearanceStyleActions(
    val onUiStyleChange: (UiStyle) -> Unit,
    val onTopBarProgressiveBlurChange: (Boolean) -> Unit,
    val onUiScaleChange: (Int) -> Unit,
)

internal data class AppearanceNavigationActions(
    val onNavEnabledChange: (Boolean) -> Unit,
    val onNavStyleChange: (NavStyle) -> Unit,
    val onNavTouchExtensionChange: (Boolean) -> Unit,
    val onNavContentChange: (NavContent) -> Unit,
    val onLiquidModeChange: (LiquidMode) -> Unit,
)

internal data class AppearanceVisualActions(
    val style: AppearanceStyleActions,
    val navigation: AppearanceNavigationActions,
)

internal data class AppearanceScaleUiState(
    val slider: MutableFloatState,
    val dialogOpen: MutableState<Boolean>,
)

@Composable
internal fun AppearanceScreen(
    appearance: Appearance,
    dark: Boolean,
    themeActions: AppearanceThemeActions,
    visualActions: AppearanceVisualActions,
    onBack: () -> Unit,
) {
    val scaleState = AppearanceScaleUiState(
        slider = remember(appearance.uiScalePercent) {
            mutableFloatStateOf(appearance.uiScalePercent.toFloat())
        },
        dialogOpen = remember { mutableStateOf(false) },
    )
    SettingsPage(
        title = stringResource(R.string.appearance_title),
        onBack = onBack,
        overlay = {
            IntegerInputDialog(
                show = scaleState.dialogOpen.value,
                title = stringResource(R.string.ui_scale_title),
                summary = "80% - 110%",
                value = appearance.uiScalePercent,
                range = 80..110,
                suffix = "%",
                onDismiss = { scaleState.dialogOpen.value = false },
                onConfirm = { percent ->
                    scaleState.dialogOpen.value = false
                    if (percent != appearance.uiScalePercent) visualActions.style.onUiScaleChange(percent)
                },
            )
        },
    ) {
        item { AppearanceThemePreview(appearance = appearance, dark = dark) }
        appearanceThemeSection(appearance, themeActions)
        appearanceVisualSection(appearance, visualActions, scaleState)
    }
}
