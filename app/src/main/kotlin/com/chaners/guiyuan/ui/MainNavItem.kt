package com.chaners.guiyuan.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.R
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.symbol.AppSettings
import top.yukonga.miuix.kmp.icon.symbol.HomeScreen
import top.yukonga.miuix.kmp.icon.symbol.Tune
import top.yukonga.miuix.kmp.symbol.toImageVector

internal data class MainNavItem(
    val label: String,
    val icon: ImageVector,
)

/** One destination list for the live hub and its noninteractive appearance preview. */
@Composable
internal fun rememberMainNavItems(): List<MainNavItem> {
    val homeLabel = stringResource(R.string.nav_home)
    val featuresLabel = stringResource(R.string.nav_features)
    val settingsLabel = stringResource(R.string.nav_settings)
    return remember(homeLabel, featuresLabel, settingsLabel) {
        listOf(
            MainNavItem(homeLabel, MiuixIcons.Symbol.HomeScreen.toImageVector()),
            MainNavItem(featuresLabel, MiuixIcons.Symbol.Tune.toImageVector()),
            MainNavItem(settingsLabel, MiuixIcons.Symbol.AppSettings.toImageVector()),
        )
    }
}
