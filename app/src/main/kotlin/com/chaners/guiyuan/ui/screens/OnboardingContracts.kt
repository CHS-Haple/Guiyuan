package com.chaners.guiyuan.ui.screens

import com.chaners.guiyuan.settings.AppLang

/** Preferences remain parent-owned; temporary page stack is private to OnboardingFlow. */
internal data class OnboardingConfig(
    val lang: AppLang,
    val iconHidden: Boolean,
    val swipeBackEnabled: Boolean,
    val agreementReviewOnly: Boolean = false,
)

internal data class OnboardingHandlers(
    val onLangChange: (AppLang) -> Unit,
    val onIconHiddenChange: (Boolean) -> Unit,
    val onComplete: () -> Unit,
    val onExit: (() -> Unit)? = null,
)
