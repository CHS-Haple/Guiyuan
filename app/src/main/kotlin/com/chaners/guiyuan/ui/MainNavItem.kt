package com.chaners.guiyuan.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.R
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Tune

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
            MainNavItem(homeLabel, MiuixIcons.Normal.Home),
            MainNavItem(featuresLabel, MiuixIcons.Normal.Tune),
            MainNavItem(settingsLabel, MiuixIcons.Normal.Settings),
        )
    }
}
