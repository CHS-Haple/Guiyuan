package com.chaners.guiyuan.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.chaners.guiyuan.settings.OnboardingPrefs
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
import com.chaners.guiyuan.ui.screens.OnboardingConfig
import com.chaners.guiyuan.ui.screens.OnboardingHandlers
import com.chaners.guiyuan.ui.screens.PrivacyPolicyScreen
import com.chaners.guiyuan.ui.screens.TermsOfUseScreen
import com.chaners.guiyuan.ui.screens.StaticScopeScreen
import com.chaners.guiyuan.ui.screens.AppearanceScreen
import com.chaners.guiyuan.ui.screens.AppearanceThemeActions
import com.chaners.guiyuan.ui.screens.AppearanceStyleActions
import com.chaners.guiyuan.ui.screens.AppearanceNavigationActions
import com.chaners.guiyuan.ui.screens.AppearanceVisualActions
import com.chaners.guiyuan.ui.screens.DiagnosticsScreen
import com.chaners.guiyuan.ui.screens.PreviewMobileNetwork
import com.chaners.guiyuan.ui.screens.PreviewNetworkMode
import com.chaners.guiyuan.ui.screens.PreviewSandboxScreen
import com.chaners.guiyuan.ui.screens.PreviewSandboxBatteryActions
import com.chaners.guiyuan.ui.screens.PreviewSandboxSimSelectionActions
import com.chaners.guiyuan.ui.screens.PreviewSandboxSim2Actions
import com.chaners.guiyuan.ui.screens.PreviewSandboxSim1Actions
import com.chaners.guiyuan.ui.screens.PreviewSandboxSimActions
import com.chaners.guiyuan.ui.screens.PreviewSandboxNetworkActions
import com.chaners.guiyuan.ui.screens.PreviewSandboxUiActions
import com.chaners.guiyuan.ui.screens.PreviewSandboxActions
import com.chaners.guiyuan.ui.screens.PreviewSandboxDisplay
import com.chaners.guiyuan.ui.screens.PreviewSandboxUiState
import com.chaners.guiyuan.ui.screens.PreviewSandboxUiStateSaver
import com.chaners.guiyuan.ui.screens.previewForPresentation
import com.chaners.guiyuan.ui.screens.selectPreviewNetworkMode
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
    onTopBarProgressiveBlurChange: (Boolean) -> Unit,
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
        val hostActivity = LocalContext.current as? android.app.Activity
        val context = LocalContext.current.applicationContext
        val agreementReviewOnly = remember(context) { OnboardingPrefs.needsAgreementReview(context) }
        var guideDone by remember(context) { mutableStateOf(!OnboardingPrefs.needsOnboarding(context)) }
        if (!guideDone) {
            OnboardingFlow(
                config = OnboardingConfig(lang, iconHidden, appearance.swipeBack, agreementReviewOnly),
                handlers = OnboardingHandlers(
                    onLangChange = onLangChange,
                    onIconHiddenChange = onIconHiddenChange,
                    onComplete = {
                        OnboardingPrefs.completeOnboarding(context)
                        guideDone = true
                    },
                    // Declining acknowledgment exits without writing agreement versions.
                    onExit = { hostActivity?.finish() },
                ),
            )
            return@GyTheme
        }
        var lastDiagSnapshot by remember { mutableStateOf<DiagSnapshot?>(null) }
        // A single writer owns the preview-only edit state; the display projection does not
        // overwrite the saved selection (notably SIM1-only with no selected network type).
        var previewSelection by rememberSaveable(stateSaver = PreviewSandboxUiStateSaver) {
            mutableStateOf(PreviewSandboxUiState())
        }
        val previewState = previewSelection.previewForPresentation()

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
                    inputs = MainHubInputs(appearance, dark, lang, iconHidden, previewState),
                    callbacks = MainHubCallbacks(
                        onHotReload = onHotReload,
                        onLangChange = onLangChange,
                        onIconHiddenChange = onIconHiddenChange,
                        onSwipeBackChange = onSwipeBackChange,
                        onNavigate = ::navigate,
                    ),
                )
            }
            entry<AppRoute.Appearance>(swipeDismiss = swipeBackDirection) {
                AppearanceScreen(
                    appearance = appearance,
                    dark = dark,
                    themeActions = AppearanceThemeActions(
                        onThemeChange = onThemeChange,
                        onDynamicChange = onDynamicChange,
                        onColorSpecChange = onColorSpecChange,
                        onPaletteStyleChange = onPaletteStyleChange,
                    ),
                    visualActions = AppearanceVisualActions(
                        style = AppearanceStyleActions(
                            onUiStyleChange = onUiStyleChange,
                            onTopBarProgressiveBlurChange = onTopBarProgressiveBlurChange,
                            onUiScaleChange = onUiScaleChange,
                        ),
                        navigation = AppearanceNavigationActions(
                            onNavEnabledChange = onNavEnabledChange,
                            onNavStyleChange = onNavStyleChange,
                            onNavTouchExtensionChange = onNavTouchExtensionChange,
                            onNavContentChange = onNavContentChange,
                            onLiquidModeChange = onLiquidModeChange,
                        ),
                    ),
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
                        display = PreviewSandboxDisplay(previewState, sandboxActive, previewDark),
                        actions = PreviewSandboxActions(
                            ui = PreviewSandboxUiActions(
                                onParasiteChange = { enabled, snapshot ->
                                    onSandboxToggle(enabled, snapshot)
                                },
                                onToggleTheme = { sandboxDark = !previewDark },
                                onBack = {
                                    onSandboxStop()
                                    navigateBack()
                                },
                            ),
                            network = PreviewSandboxNetworkActions(
                                onAirplaneModeChange = { previewSelection = previewSelection.copy(airplaneMode = it) },
                                onNetworkModeChange = { previewSelection = previewSelection.selectPreviewNetworkMode(it) },
                                onWifiStateChange = { previewSelection = previewSelection.copy(wifiState = it) },
                                onWifiSignalLevelChange = { previewSelection = previewSelection.copy(wifiSignalLevel = it) },
                                onNetworkActivityChange = { previewSelection = previewSelection.copy(networkActivity = it) },
                            ),
                            sims = PreviewSandboxSimActions(
                                first = PreviewSandboxSim1Actions(
                                    onMobileNetworkChange = { previewSelection = previewSelection.copy(mobileNetwork = it) },
                                    onMobileSignalLevelChange = { previewSelection = previewSelection.copy(mobileSignalLevel = it) },
                                ),
                                second = PreviewSandboxSim2Actions(
                                    onSim2SignalLevelChange = { previewSelection = previewSelection.copy(sim2SignalLevel = it) },
                                    onSim2NetworkChange = { previewSelection = previewSelection.copy(sim2MobileNetwork = it) },
                                ),
                                selection = PreviewSandboxSimSelectionActions(
                                    onDataSimChange = { previewSelection = previewSelection.copy(dataSim = it) },
                                    onSimCardsChange = { previewSelection = previewSelection.copy(simCards = it) },
                                ),
                            ),
                            battery = PreviewSandboxBatteryActions(
                                onBatteryPercentChange = { previewSelection = previewSelection.copy(batteryPercent = it) },
                                onBatteryModeChange = { previewSelection = previewSelection.copy(batteryMode = it) },
                                onChargingStateChange = { previewSelection = previewSelection.copy(chargingState = it) },
                            ),
                        ),
                    )
                }
            }
            entry<AppRoute.About>(swipeDismiss = swipeBackDirection) {
                AboutScreen(
                    onBack = ::navigateBack,
                    onOpenThirdParty = { navigate(AppRoute.AboutThirdParty) },
                    onOpenPrivacy = { navigate(AppRoute.PrivacyPolicy) },
                    onOpenTerms = { navigate(AppRoute.TermsOfUse) },
                    onOpenScopes = { navigate(AppRoute.StaticScopes) },
                )
            }
            entry<AppRoute.Onboarding>(swipeDismiss = swipeBackDirection) {
                OnboardingFlow(
                    config = OnboardingConfig(lang, iconHidden, appearance.swipeBack),
                    handlers = OnboardingHandlers(
                        onLangChange = onLangChange,
                        onIconHiddenChange = onIconHiddenChange,
                        onComplete = ::navigateBack,
                        onExit = ::navigateBack,
                    ),
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
