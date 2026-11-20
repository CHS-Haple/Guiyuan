package com.chaners.guiyuan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.chaners.guiyuan.R
import top.yukonga.miuix.kmp.basic.SnackbarHostState

@Composable
internal fun FeaturesScreen(
    bottomContentPadding: Dp,
    visible: Boolean,
    snackbarState: SnackbarHostState,
    search: PageSearch,
    searchInNavigationBar: Boolean,
) {
    val resources = rememberFeaturesScreenResources()
    val backup = rememberFeaturesBackupController(snackbarState)
    val toggles = rememberFeaturesOverlayToggles()
    LaunchedEffect(visible) {
        if (!visible) search.close()
    }
    HubPage(
        title = stringResource(R.string.features_title),
        bottomContentPadding = bottomContentPadding,
        search = search,
        searchInNavigationBar = searchInNavigationBar,
        overlay = { FeaturesScreenOverlays(resources, backup, toggles) },
    ) {
        featureScreenSections(
            settings = FeaturesSectionSettings(
                search, resources.settings.feature, resources.settings.featureRepo,
                resources.settings.visual, resources.settings.visualRepo,
            ),
            resources = FeaturesSectionResources(
                resources.battery.mobileWeight, resources.battery.batteryWeight, resources.battery.schemes,
            ),
            ui = FeaturesSectionUi(
                mobileWeightOpen = toggles.mobileWeight.value,
                batteryWeightOpen = toggles.batteryWeight.value,
                colorSheetOpen = toggles.colors.value,
                ioBusy = backup.state.busy,
            ),
            actions = FeaturesSectionActions(
                openMobileWeight = { toggles.mobileWeight.value = true },
                openBatteryWeight = { toggles.batteryWeight.value = true },
                openColorSheet = { toggles.colors.value = true },
                export = backup.onExport,
                import = backup.onImport,
                reset = { toggles.reset.value = true },
            ),
        )
    }
}
