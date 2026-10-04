package com.chaners.guiyuan.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.chaners.guiyuan.settings.AppLanguage
import com.chaners.guiyuan.settings.AppThemeMode
import com.chaners.guiyuan.settings.AppearanceSettings
import com.chaners.guiyuan.settings.FloatingNavigationContent
import com.chaners.guiyuan.settings.FloatingNavigationStyle
import com.chaners.guiyuan.ui.navigation.AppRoute
import com.chaners.guiyuan.ui.screens.AboutScreen
import com.chaners.guiyuan.ui.screens.AppearanceScreen
import com.chaners.guiyuan.ui.screens.DiagnosticsScreen
import com.chaners.guiyuan.ui.screens.PreviewBatteryMode
import com.chaners.guiyuan.ui.screens.PreviewChargingState
import com.chaners.guiyuan.ui.screens.PreviewMobileNetwork
import com.chaners.guiyuan.ui.screens.PreviewNetworkMode
import com.chaners.guiyuan.ui.screens.PreviewSandboxScreen
import com.chaners.guiyuan.ui.screens.PreviewSandboxUiState
import com.chaners.guiyuan.ui.screens.PreviewWifiState
import com.chaners.guiyuan.ui.theme.CombinedStatusTheme
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun CombinedStatusApp(
    settings: AppearanceSettings,
    darkMode: Boolean,
    appLanguage: AppLanguage,
    launcherIconHidden: Boolean,
    onHotReload: (() -> Unit) -> Boolean,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onDynamicColorEnabledChange: (Boolean) -> Unit,
    onFloatingNavigationBarEnabledChange: (Boolean) -> Unit,
    onFloatingNavigationStyleChange: (FloatingNavigationStyle) -> Unit,
    onFloatingNavigationContentChange: (FloatingNavigationContent) -> Unit,
    onSwipeBackEnabledChange: (Boolean) -> Unit,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onLauncherIconHiddenChange: (Boolean) -> Unit,
) {
    CombinedStatusTheme(
        themeMode = settings.themeMode,
        dynamicColorEnabled = settings.dynamicColorEnabled,
    ) {
        var previewSimPresent by rememberSaveable { mutableStateOf(true) }
        var previewAirplaneMode by rememberSaveable { mutableStateOf(false) }
        var previewNetworkModeIndex by rememberSaveable {
            mutableIntStateOf(PreviewNetworkMode.WIFI.ordinal)
        }
        var previewMobileNetworkIndex by rememberSaveable {
            mutableIntStateOf(PreviewMobileNetwork.FIVE_G.ordinal)
        }
        var previewMobileSignalLevel by rememberSaveable { mutableIntStateOf(4) }
        var previewWifiStateIndex by rememberSaveable {
            mutableIntStateOf(PreviewWifiState.CONNECTED.ordinal)
        }
        var previewWifiSignalLevel by rememberSaveable { mutableIntStateOf(3) }
        var previewBatteryPercent by rememberSaveable { mutableIntStateOf(87) }
        var previewBatteryModeIndex by rememberSaveable {
            mutableIntStateOf(PreviewBatteryMode.BALANCED.ordinal)
        }
        var previewChargingStateIndex by rememberSaveable {
            mutableIntStateOf(PreviewChargingState.NOT_CHARGING.ordinal)
        }
        val previewState =
            PreviewSandboxUiState(
                simPresent = previewSimPresent,
                airplaneMode = previewAirplaneMode,
                networkMode =
                    PreviewNetworkMode.entries[
                        previewNetworkModeIndex.coerceIn(
                            0,
                            PreviewNetworkMode.entries.lastIndex,
                        )
                    ],
                mobileNetwork =
                    PreviewMobileNetwork.entries[
                        previewMobileNetworkIndex.coerceIn(
                            0,
                            PreviewMobileNetwork.entries.lastIndex,
                        )
                    ],
                mobileSignalLevel = previewMobileSignalLevel,
                wifiState =
                    PreviewWifiState.entries[
                        previewWifiStateIndex.coerceIn(
                            0,
                            PreviewWifiState.entries.lastIndex,
                        )
                    ],
                wifiSignalLevel = previewWifiSignalLevel,
                batteryPercent = previewBatteryPercent,
                batteryMode =
                    PreviewBatteryMode.entries[
                        previewBatteryModeIndex.coerceIn(
                            0,
                            PreviewBatteryMode.entries.lastIndex,
                        )
                    ],
                chargingState =
                    PreviewChargingState.entries[
                        previewChargingStateIndex.coerceIn(
                            0,
                            PreviewChargingState.entries.lastIndex,
                        )
                    ],
            )

        val backStack = rememberNavBackStack<AppRoute>(AppRoute.Home)
        val swipeBackDirection = when {
            !settings.swipeBackEnabled -> NavSwipeDirection.None
            LocalLayoutDirection.current == LayoutDirection.Ltr -> NavSwipeDirection.LeftToRight
            else -> NavSwipeDirection.RightToLeft
        }

        fun navigate(route: AppRoute) {
            if (route !in backStack) {
                backStack.add(route)
            }
        }

        fun navigateBack() {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        }

        NavDisplay(
            backStack = backStack,
            onBack = ::navigateBack,
            transition = NavTransitions.MiuixDefault,
            effects = NavDisplayEffects(
                cornerClipRadius = rememberNavSystemCornerRadius(),
                backdropColor = MiuixTheme.colorScheme.surface,
            ),
        ) {
            entry<AppRoute.Home> {
                MainHub(
                    settings = settings,
                    darkMode = darkMode,
                    appLanguage = appLanguage,
                    launcherIconHidden = launcherIconHidden,
                    onHotReload = onHotReload,
                    onAppLanguageChange = onAppLanguageChange,
                    onLauncherIconHiddenChange = onLauncherIconHiddenChange,
                    onSwipeBackEnabledChange = onSwipeBackEnabledChange,
                    previewState = previewState,
                    onNavigate = ::navigate,
                )
            }
            entry<AppRoute.Appearance>(swipeDismiss = swipeBackDirection) {
                AppearanceScreen(
                    settings = settings,
                    darkMode = darkMode,
                    onThemeModeChange = onThemeModeChange,
                    onDynamicColorEnabledChange = onDynamicColorEnabledChange,
                    onFloatingNavigationBarEnabledChange =
                        onFloatingNavigationBarEnabledChange,
                    onFloatingNavigationStyleChange =
                        onFloatingNavigationStyleChange,
                    onFloatingNavigationContentChange =
                        onFloatingNavigationContentChange,
                    onBack = ::navigateBack,
                )
            }
            entry<AppRoute.PreviewSandbox>(swipeDismiss = swipeBackDirection) {
                PreviewSandboxScreen(
                    state = previewState,
                    onSimPresentChange = { previewSimPresent = it },
                    onAirplaneModeChange = { previewAirplaneMode = it },
                    onNetworkModeChange = {
                        previewNetworkModeIndex = it.ordinal
                    },
                    onMobileNetworkChange = {
                        previewMobileNetworkIndex = it.ordinal
                    },
                    onMobileSignalLevelChange = {
                        previewMobileSignalLevel = it
                    },
                    onWifiStateChange = {
                        previewWifiStateIndex = it.ordinal
                    },
                    onWifiSignalLevelChange = {
                        previewWifiSignalLevel = it
                    },
                    onBatteryPercentChange = {
                        previewBatteryPercent = it
                    },
                    onBatteryModeChange = {
                        previewBatteryModeIndex = it.ordinal
                    },
                    onChargingStateChange = {
                        previewChargingStateIndex = it.ordinal
                    },
                    onBack = ::navigateBack,
                )
            }
            entry<AppRoute.About>(swipeDismiss = swipeBackDirection) {
                AboutScreen(onBack = ::navigateBack)
            }
            entry<AppRoute.Diagnostics>(swipeDismiss = swipeBackDirection) {
                DiagnosticsScreen(onBack = ::navigateBack)
            }
        }
    }
}
