package com.chaners.guiyuan.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.chaners.guiyuan.settings.OnboardingPrefs
import com.chaners.guiyuan.settings.AppLang
import com.chaners.guiyuan.settings.Appearance
import com.chaners.guiyuan.ui.screens.OnboardingFlow
import com.chaners.guiyuan.ui.screens.OnboardingConfig
import com.chaners.guiyuan.ui.screens.OnboardingHandlers
import com.chaners.guiyuan.ui.screens.AppearanceThemeActions
import com.chaners.guiyuan.ui.screens.AppearanceVisualActions
import com.chaners.guiyuan.ui.screens.PreviewSandboxUiState
import com.chaners.guiyuan.ui.theme.GyTheme

// Activity-owned lease and window theme callbacks; route code owns only preview selection.
internal data class AppSandbox(
    val sandboxActive: Boolean,
    val onPreviewThemeChange: (Boolean?) -> Unit,
    val onSandboxToggle: (Boolean, PreviewSandboxUiState) -> Unit,
    val onSandboxUpdate: (PreviewSandboxUiState) -> Unit,
    val onSandboxStop: () -> Unit,
)

internal data class AppSettingsActions(
    val onHotReload: () -> Unit,
    val themeActions: AppearanceThemeActions,
    val visualActions: AppearanceVisualActions,
    val onSwipeBackChange: (Boolean) -> Unit,
    val onLangChange: (AppLang) -> Unit,
    val onIconHiddenChange: (Boolean) -> Unit,
)

@Composable
internal fun GyApp(
    appearance: Appearance,
    dark: Boolean,
    lang: AppLang,
    iconHidden: Boolean,
    sandbox: AppSandbox,
    actions: AppSettingsActions,
) {
    GyTheme(appearance) {
        val hostActivity = LocalContext.current as? android.app.Activity
        val context = LocalContext.current.applicationContext
        val agreementReviewOnly = remember(context) { OnboardingPrefs.needsAgreementReview(context) }
        var guideDone by remember(context) { mutableStateOf(!OnboardingPrefs.needsOnboarding(context)) }
        if (!guideDone) {
            OnboardingFlow(
                config = OnboardingConfig(lang, iconHidden, appearance.swipeBack, agreementReviewOnly),
                handlers = OnboardingHandlers(
                    onLangChange = actions.onLangChange,
                    onIconHiddenChange = actions.onIconHiddenChange,
                    onComplete = {
                        OnboardingPrefs.completeOnboarding(context)
                        guideDone = true
                    },
                    // Declining acknowledgment exits without writing agreement versions.
                    onExit = { hostActivity?.finish() },
                ),
            )
        } else {
            GyAppNavigation(appearance, dark, lang, iconHidden, sandbox, actions)
        }
    }
}
