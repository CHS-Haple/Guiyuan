package com.chaners.guiyuan.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.chaners.guiyuan.settings.AppPlatform
import com.chaners.guiyuan.settings.AppLang
import com.chaners.guiyuan.settings.Appearance
import com.chaners.guiyuan.settings.LiquidMode
import com.chaners.guiyuan.settings.NavContent
import com.chaners.guiyuan.settings.NavStyle
import com.chaners.guiyuan.settings.UiStyle
import com.chaners.guiyuan.settings.ThemeMode
import com.chaners.guiyuan.system.DiagSnapshot
import com.chaners.guiyuan.ui.navigation.AppRoute
import com.chaners.guiyuan.ui.screens.AboutScreen
import com.chaners.guiyuan.ui.screens.AboutThirdPartyScreen
import com.chaners.guiyuan.ui.screens.OnboardingFlow
import com.chaners.guiyuan.ui.screens.PrivacyPolicyScreen
import com.chaners.guiyuan.ui.screens.TermsOfUseScreen
import com.chaners.guiyuan.ui.screens.StaticScopeScreen
import com.chaners.guiyuan.ui.screens.AppearanceScreen
import com.chaners.guiyuan.ui.screens.DiagnosticsScreen
import com.chaners.guiyuan.ui.screens.PreviewBatteryMode
import com.chaners.guiyuan.ui.screens.PreviewChargingState
import com.chaners.guiyuan.ui.screens.PreviewDataSim
import com.chaners.guiyuan.ui.screens.PreviewSimCards
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
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

@Composable
internal fun GyApp(
    appearance: Appearance,
    dark: Boolean,
    onPreviewThemeChange: (Boolean?) -> Unit,
    sandboxActive: Boolean,
    onSandboxToggle: (Boolean, PreviewSandboxUiState) -> Unit,
    onSandboxUpdate: (PreviewSandboxUiState) -> Unit,
    onSandboxStop: () -> Unit,
    lang: AppLang,
    iconHidden: Boolean,
    onHotReload: () -> Unit,
    onUiStyleChange: (UiStyle) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onDynamicChange: (Boolean) -> Unit,
    onColorSpecChange: (ThemeColorSpec?) -> Unit,
    onPaletteStyleChange: (ThemePaletteStyle) -> Unit,
    onNavEnabledChange: (Boolean) -> Unit,
    onNavStyleChange: (NavStyle) -> Unit,
    onNavTouchExtensionChange: (Boolean) -> Unit,
    onNavContentChange: (NavContent) -> Unit,
    onLiquidModeChange: (LiquidMode) -> Unit,
    onSwipeBackChange: (Boolean) -> Unit,
    onUiScaleChange: (Int) -> Unit,
    onLangChange: (AppLang) -> Unit,
    onIconHiddenChange: (Boolean) -> Unit,
) {
    GyTheme(
        themeMode = appearance.theme,
        dynamicColorEnabled = appearance.dynamicColor,
        colorSpec = appearance.colorSpec,
        paletteStyle = appearance.paletteStyle,
    ) {
        val context = LocalContext.current.applicationContext
        var guideDone by remember(context) { mutableStateOf(!AppPlatform.needsOnboarding(context)) }
        if (!guideDone) {
            OnboardingFlow(
                lang = lang,
                onLangChange = onLangChange,
                iconHidden = iconHidden,
                onIconHiddenChange = onIconHiddenChange,
                onComplete = {
                    AppPlatform.completeOnboarding(context)
                    guideDone = true
                },
            )
            return@GyTheme
        }
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
        var previewDualMobile by rememberSaveable { mutableStateOf(false) }
        var previewSim2SignalLevel by rememberSaveable { mutableIntStateOf(4) }
        var previewSim2NetworkIndex by rememberSaveable {
            mutableIntStateOf(PreviewMobileNetwork.FOUR_G.ordinal)
        }
        var previewDataSimIndex by rememberSaveable {
            mutableIntStateOf(PreviewDataSim.SIM1.ordinal)
        }
        var previewSimCardsIndex by rememberSaveable {
            mutableIntStateOf(PreviewSimCards.BOTH.ordinal)
        }
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
        val savedNetworkMode =
            PreviewNetworkMode.entries[
                previewNetworkModeIndex.coerceIn(0, PreviewNetworkMode.entries.lastIndex)
            ]
        val savedMobileNetwork =
            PreviewMobileNetwork.entries[
                previewMobileNetworkIndex.coerceIn(0, PreviewMobileNetwork.entries.lastIndex)
            ]
        val previewState =
            PreviewSandboxUiState(
                simPresent = previewSimPresent,
                airplaneMode = previewAirplaneMode,
                networkMode =
                    if (
                        savedNetworkMode == PreviewNetworkMode.MOBILE &&
                        !previewDualMobile &&
                        savedMobileNetwork == PreviewMobileNetwork.NONE
                    ) {
                        PreviewNetworkMode.NONE
                    } else {
                        savedNetworkMode
                    },
                mobileNetwork = savedMobileNetwork,
                mobileSignalLevel = previewMobileSignalLevel,
                dualMobile = previewDualMobile,
                sim2SignalLevel = previewSim2SignalLevel,
                sim2MobileNetwork =
                    PreviewMobileNetwork.entries[
                        previewSim2NetworkIndex.coerceIn(
                            0, PreviewMobileNetwork.entries.lastIndex,
                        )
                    ],
                dataSim =
                    PreviewDataSim.entries[
                        previewDataSimIndex.coerceIn(0, PreviewDataSim.entries.lastIndex)
                    ],
                simCards =
                    PreviewSimCards.entries[
                        previewSimCardsIndex.coerceIn(0, PreviewSimCards.entries.lastIndex)
                    ],
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

        // Only a changed preview updates an active lease; start() owns its first snapshot.
        LaunchedEffect(previewState) {
            if (sandboxActive) onSandboxUpdate(previewState)
        }

        val backStack = rememberNavBackStack<AppRoute>(AppRoute.Home)
        val currentRoute = backStack.lastOrNull()
        DisposableEffect(currentRoute) {
            onDispose {
                if (currentRoute == AppRoute.PreviewSandbox) onSandboxStop()
            }
        }
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
                    onUiStyleChange = onUiStyleChange,
                    onThemeChange = onThemeChange,
                    onDynamicChange = onDynamicChange,
                    onColorSpecChange = onColorSpecChange,
                    onPaletteStyleChange = onPaletteStyleChange,
                    onNavEnabledChange =
                        onNavEnabledChange,
                    onNavStyleChange =
                        onNavStyleChange,
                    onNavTouchExtensionChange = onNavTouchExtensionChange,
                    onNavContentChange =
                        onNavContentChange,
                    onLiquidModeChange =
                        onLiquidModeChange,
                    onUiScaleChange = onUiScaleChange,
                    onBack = ::navigateBack,
                )
            }
            entry<AppRoute.PreviewSandbox>(swipeDismiss = swipeBackDirection) {
                var sandboxDark by remember { mutableStateOf<Boolean?>(null) }
                val previewDark = sandboxDark ?: dark
                DisposableEffect(previewDark) {
                    onPreviewThemeChange(previewDark)
                    onDispose { onPreviewThemeChange(null) }
                }
                GyTheme(
                    themeMode = if (previewDark) ThemeMode.Dark else ThemeMode.Light,
                    dynamicColorEnabled = appearance.dynamicColor,
                    colorSpec = appearance.colorSpec,
                    paletteStyle = appearance.paletteStyle,
                ) {
                    PreviewSandboxScreen(
                        state = previewState,
                        parasiteActive = sandboxActive,
                        onParasiteChange = { enabled, snapshot -> onSandboxToggle(enabled, snapshot) },
                        onSimPresentChange = { previewSimPresent = it },
                        onAirplaneModeChange = { previewAirplaneMode = it },
                        onNetworkModeChange = {
                            if (
                                it == PreviewNetworkMode.MOBILE &&
                                previewMobileNetworkIndex == PreviewMobileNetwork.NONE.ordinal
                            ) {
                                previewMobileNetworkIndex = PreviewMobileNetwork.FIVE_G.ordinal
                            }
                            previewNetworkModeIndex = it.ordinal
                        },
                        onMobileNetworkChange = {
                            previewMobileNetworkIndex = it.ordinal
                        },
                        onMobileSignalLevelChange = {
                            previewMobileSignalLevel = it
                        },
                        onDualMobileChange = { previewDualMobile = it },
                        onSim2SignalLevelChange = { previewSim2SignalLevel = it },
                        onSim2NetworkChange = { previewSim2NetworkIndex = it.ordinal },
                        onDataSimChange = { previewDataSimIndex = it.ordinal },
                        onSimCardsChange = { previewSimCardsIndex = it.ordinal },
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
                        dark = previewDark,
                        onToggleTheme = { sandboxDark = !previewDark },
                        onBack = {
                            onSandboxStop()
                            navigateBack()
                        },
                    )
                }
            }
            entry<AppRoute.About>(swipeDismiss = swipeBackDirection) {
                AboutScreen(
                    onBack = ::navigateBack,
                    onOpenThirdParty = { navigate(AppRoute.AboutThirdParty) },
                    onOpenPrivacy = { navigate(AppRoute.PrivacyPolicy) },
                    onOpenTerms = { navigate(AppRoute.TermsOfUse) },
                    onOpenGuide = { navigate(AppRoute.Onboarding) },
                    onOpenScopes = { navigate(AppRoute.StaticScopes) },
                )
            }
            entry<AppRoute.Onboarding>(swipeDismiss = swipeBackDirection) {
                OnboardingFlow(
                    lang = lang,
                    onLangChange = onLangChange,
                    iconHidden = iconHidden,
                    onIconHiddenChange = onIconHiddenChange,
                    onComplete = ::navigateBack,
                    onExit = ::navigateBack,
                )
            }
            entry<AppRoute.PrivacyPolicy>(swipeDismiss = swipeBackDirection) {
                PrivacyPolicyScreen(onBack = ::navigateBack)
            }
            entry<AppRoute.TermsOfUse>(swipeDismiss = swipeBackDirection) {
                TermsOfUseScreen(onBack = ::navigateBack)
            }
            entry<AppRoute.AboutThirdParty>(swipeDismiss = swipeBackDirection) {
                AboutThirdPartyScreen(onBack = ::navigateBack)
            }
            entry<AppRoute.StaticScopes>(swipeDismiss = swipeBackDirection) {
                StaticScopeScreen(onBack = ::navigateBack)
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
