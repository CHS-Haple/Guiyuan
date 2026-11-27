package com.chaners.guiyuan.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import com.chaners.guiyuan.settings.AppLang
import com.chaners.guiyuan.settings.Appearance
import com.chaners.guiyuan.settings.ThemeMode
import com.chaners.guiyuan.system.DiagSnapshot
import com.chaners.guiyuan.ui.navigation.AppRoute
import com.chaners.guiyuan.ui.navigation.appSwipeDismissDirection
import com.chaners.guiyuan.ui.screens.AboutScreen
import com.chaners.guiyuan.ui.screens.AboutThirdPartyScreen
import com.chaners.guiyuan.ui.screens.OnboardingFlow
import com.chaners.guiyuan.ui.screens.OnboardingConfig
import com.chaners.guiyuan.ui.screens.OnboardingHandlers
import com.chaners.guiyuan.ui.screens.PrivacyPolicyScreen
import com.chaners.guiyuan.ui.screens.TermsOfUseScreen
import com.chaners.guiyuan.ui.screens.StaticScopeScreen
import com.chaners.guiyuan.ui.screens.AppearanceScreen
import com.chaners.guiyuan.ui.screens.DiagnosticsScreen
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
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Owns the main navigation stack and preview selection only after onboarding completes. */
@Composable
internal fun GyAppNavigation(
    appearance: Appearance,
    dark: Boolean,
    lang: AppLang,
    iconHidden: Boolean,
    sandbox: AppSandbox,
    actions: AppSettingsActions,
) {
    val (onHotReload, themeActions, visualActions, onSwipeBackChange, onLangChange, onIconHiddenChange) = actions
    val hostActivity = LocalContext.current as? android.app.Activity
    var lastDiagSnapshot by remember { mutableStateOf<DiagSnapshot?>(null) }
    // A single writer owns the preview-only edit state; the display projection does not
    // overwrite the saved selection (notably SIM1-only with no selected network type).
    val previewSelection = rememberSaveable(stateSaver = PreviewSandboxUiStateSaver) {
        mutableStateOf(PreviewSandboxUiState())
    }
    val previewState = previewSelection.value.previewForPresentation()

    // Only a changed preview updates an active lease; start() owns its first snapshot.
    LaunchedEffect(previewState) {
        if (sandbox.sandboxActive) sandbox.onSandboxUpdate(previewState)
    }

    val backStack = rememberNavBackStack<AppRoute>(AppRoute.Home)
    val currentRoute = backStack.lastOrNull()
    DisposableEffect(currentRoute) {
        onDispose {
            if (currentRoute == AppRoute.PreviewSandbox) sandbox.onSandboxStop()
        }
    }
    val swipeBackDirection = appSwipeDismissDirection(appearance.swipeBack, LocalLayoutDirection.current)

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

    // Re-evaluate on configuration changes; only multi-window loses the outer screen corner.
    LocalConfiguration.current
    val navCornerRadius = if (hostActivity?.isInMultiWindowMode == true) 0.dp else rememberNavSystemCornerRadius()
    NavDisplay(
        backStack = backStack,
        onBack = ::navigateBack,
        transition = NavTransitions.MiuixDefault,
        effects = NavDisplayEffects(
            cornerClipRadius = navCornerRadius,
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
                themeActions = themeActions,
                visualActions = visualActions,
                onBack = ::navigateBack,
            )
        }
        entry<AppRoute.PreviewSandbox>(
            transition = NavTransitions.Expansion,
            swipeDismiss = swipeBackDirection,
        ) {
            PreviewSandboxRoute(
                appearance = appearance,
                display = PreviewSandboxDisplay(previewState, sandbox.sandboxActive, dark),
                sandbox = sandbox,
                selection = previewSelection,
                onBack = ::navigateBack,
            )
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


/** The navigation entry owns its temporary theme override; the activity owns the live lease. */
@Composable
private fun PreviewSandboxRoute(
    appearance: Appearance,
    display: PreviewSandboxDisplay,
    sandbox: AppSandbox,
    selection: MutableState<PreviewSandboxUiState>,
    onBack: () -> Unit,
) {
    var sandboxDark by remember { mutableStateOf<Boolean?>(null) }
    val previewDark = sandboxDark ?: display.dark
    DisposableEffect(previewDark) {
        sandbox.onPreviewThemeChange(previewDark)
        onDispose { sandbox.onPreviewThemeChange(null) }
    }
    GyTheme(
        appearance = appearance,
        themeMode = if (previewDark) ThemeMode.Dark else ThemeMode.Light,
    ) {
        PreviewSandboxScreen(
            display = display.copy(dark = previewDark),
            actions = previewSandboxActions(
                ui = PreviewSandboxUiActions(
                    onParasiteChange = sandbox.onSandboxToggle,
                    onToggleTheme = { sandboxDark = !previewDark },
                    onBack = {
                        sandbox.onSandboxStop()
                        onBack()
                    },
                ),
                selection = selection,
            ),
        )
    }
}

// Field actions edit the existing route-owned selection, never the projected state.
private fun previewSandboxActions(
    ui: PreviewSandboxUiActions,
    selection: MutableState<PreviewSandboxUiState>,
): PreviewSandboxActions = PreviewSandboxActions(
    ui = ui,
    network = PreviewSandboxNetworkActions(
        onAirplaneModeChange = { selection.value = selection.value.copy(airplaneMode = it) },
        onNetworkModeChange = { selection.value = selection.value.selectPreviewNetworkMode(it) },
        onWifiStateChange = { selection.value = selection.value.copy(wifiState = it) },
        onWifiSignalLevelChange = { selection.value = selection.value.copy(wifiSignalLevel = it) },
        onNetworkActivityChange = { selection.value = selection.value.copy(networkActivity = it) },
    ),
    sims = PreviewSandboxSimActions(
        first = PreviewSandboxSim1Actions(
            onMobileNetworkChange = { selection.value = selection.value.copy(mobileNetwork = it) },
            onMobileSignalLevelChange = { selection.value = selection.value.copy(mobileSignalLevel = it) },
        ),
        second = PreviewSandboxSim2Actions(
            onSim2SignalLevelChange = { selection.value = selection.value.copy(sim2SignalLevel = it) },
            onSim2NetworkChange = { selection.value = selection.value.copy(sim2MobileNetwork = it) },
        ),
        selection = PreviewSandboxSimSelectionActions(
            onDataSimChange = { selection.value = selection.value.copy(dataSim = it) },
            onSimCardsChange = { selection.value = selection.value.copy(simCards = it) },
        ),
    ),
    battery = PreviewSandboxBatteryActions(
        onBatteryPercentChange = { selection.value = selection.value.copy(batteryPercent = it) },
        onBatteryModeChange = { selection.value = selection.value.copy(batteryMode = it) },
        onChargingStateChange = { selection.value = selection.value.copy(chargingState = it) },
    ),
)
