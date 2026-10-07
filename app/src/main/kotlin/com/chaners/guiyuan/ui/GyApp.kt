package com.chaners.guiyuan.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.chaners.guiyuan.settings.AppLang
import com.chaners.guiyuan.settings.ThemeMode
import com.chaners.guiyuan.system.DiagSnapshot
import com.chaners.guiyuan.settings.Appearance
import com.chaners.guiyuan.settings.LiquidMode
import com.chaners.guiyuan.settings.NavContent
import com.chaners.guiyuan.settings.NavStyle
import com.chaners.guiyuan.ui.navigation.AppRoute
import com.chaners.guiyuan.ui.screens.AboutScreen
import com.chaners.guiyuan.ui.screens.AboutThirdPartyScreen
import com.chaners.guiyuan.ui.screens.AppearanceScreen
import com.chaners.guiyuan.ui.screens.DiagnosticsScreen
import com.chaners.guiyuan.ui.screens.PreviewBatteryMode
import com.chaners.guiyuan.ui.screens.PreviewChargingState
import com.chaners.guiyuan.ui.screens.PreviewMobileNetwork
import com.chaners.guiyuan.ui.screens.PreviewNetworkMode
import com.chaners.guiyuan.ui.screens.PreviewSandboxScreen
import com.chaners.guiyuan.ui.screens.PreviewSandboxUiState
import com.chaners.guiyuan.ui.screens.PreviewWifiState
import com.chaners.guiyuan.ui.theme.GyTheme
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GyApp(
    appearance: Appearance,
    dark: Boolean,
    lang: AppLang,
    iconHidden: Boolean,
    onHotReload: (() -> Unit) -> Boolean,
    onThemeChange: (ThemeMode) -> Unit,
    onDynamicChange: (Boolean) -> Unit,
    onNavEnabledChange: (Boolean) -> Unit,
    onNavStyleChange: (NavStyle) -> Unit,
    onNavContentChange: (NavContent) -> Unit,
    onLiquidModeChange: (LiquidMode) -> Unit,
    onSwipeBackChange: (Boolean) -> Unit,
    onLangChange: (AppLang) -> Unit,
    onIconHiddenChange: (Boolean) -> Unit,
) {
    GyTheme(
        themeMode = appearance.theme,
        dynamicColorEnabled = appearance.dynamicColor,
    ) {
        var lastDiagSnapshot by remember { mutableStateOf<DiagSnapshot?>(null) }
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
            !appearance.swipeBack -> NavSwipeDirection.None
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
                    appearance = appearance,
                    dark = dark,
                    lang = lang,
                    iconHidden = iconHidden,
                    onHotReload = onHotReload,
                    onLangChange = onLangChange,
                    onIconHiddenChange = onIconHiddenChange,
                    onSwipeBackChange = onSwipeBackChange,
                    previewState = previewState,
                    onNavigate = ::navigate,
                )
            }
            entry<AppRoute.Appearance>(swipeDismiss = swipeBackDirection) {
                AppearanceScreen(
                    appearance = appearance,
                    dark = dark,
                    onThemeChange = onThemeChange,
                    onDynamicChange = onDynamicChange,
                    onNavEnabledChange =
                        onNavEnabledChange,
                    onNavStyleChange =
                        onNavStyleChange,
                    onNavContentChange =
                        onNavContentChange,
                    onLiquidModeChange =
                        onLiquidModeChange,
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
                AboutScreen(
                    onBack = ::navigateBack,
                    onOpenThirdParty = { navigate(AppRoute.AboutThirdParty) },
                )
            }
            entry<AppRoute.AboutThirdParty>(swipeDismiss = swipeBackDirection) {
                AboutThirdPartyScreen(onBack = ::navigateBack)
            }
            entry<AppRoute.Diagnostics>(swipeDismiss = swipeBackDirection) {
                DiagnosticsScreen(
                    onBack = ::navigateBack,
                    cachedSnapshot = lastDiagSnapshot,
                    onSnapshot = { lastDiagSnapshot = it },
                )
            }
        }
    }
}
