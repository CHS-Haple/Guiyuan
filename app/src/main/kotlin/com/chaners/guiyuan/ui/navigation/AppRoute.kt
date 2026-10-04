package com.chaners.guiyuan.ui.navigation

import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavKey

@Serializable
internal sealed interface AppRoute : NavKey {
    @Serializable
    data object Home : AppRoute

    @Serializable
    data object Appearance : AppRoute

    @Serializable
    data object PreviewSandbox : AppRoute

    @Serializable
    data object About : AppRoute

    @Serializable
    data object AboutLicense : AppRoute

    @Serializable
    data object AboutThirdParty : AppRoute

    @Serializable
    data object Diagnostics : AppRoute
}
