package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.BatteryColorSchemeLibrary
import com.chaners.guiyuan.settings.FeatureCfg
import com.chaners.guiyuan.settings.FeatureRepo
import com.chaners.guiyuan.settings.VisualCfg
import com.chaners.guiyuan.settings.VisualRepo
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal data class FeaturesSectionSettings(
    val search: PageSearch,
    val feature: FeatureCfg,
    val featureRepo: FeatureRepo,
    val visual: VisualCfg,
    val visualRepo: VisualRepo,
)

internal data class FeaturesSectionResources(
    val mobileWeight: FeatureWeightUi,
    val batteryWeight: FeatureWeightUi,
    val schemes: BatteryColorSchemeLibrary,
)

internal data class FeaturesSectionUi(
    val mobileWeightOpen: Boolean,
    val batteryWeightOpen: Boolean,
    val colorSheetOpen: Boolean,
    val ioBusy: Boolean,
)

internal data class FeaturesSectionActions(
    val openMobileWeight: () -> Unit,
    val openBatteryWeight: () -> Unit,
    val openColorSheet: () -> Unit,
    val export: () -> Unit,
    val import: () -> Unit,
    val reset: () -> Unit,
)

private data class FeatureSectionsMatching(
    val display: Boolean,
    val layout: Boolean,
    val network: Boolean,
    val battery: Boolean,
    val management: Boolean,
    val bothSidesCharging: Boolean,
) {
    val hasResults: Boolean get() = display || layout || network || battery || management
}

private fun PageSearch.matchFeatureSections(showChargingIcon: Boolean): FeatureSectionsMatching {
    val charging = filtering && any(
        R.string.both_sides_charging_title,
        R.string.both_sides_charging_vertical_offset,
    )
    return FeatureSectionsMatching(
        display = anyFrom(FeatureSearchIds.display),
        layout = anyFrom(FeatureSearchIds.layout),
        network = anyFrom(FeatureSearchIds.network),
        battery = anyFrom(FeatureSearchIds.battery) || (charging && !showChargingIcon),
        management = anyFrom(FeatureSearchIds.management),
        bothSidesCharging = charging,
    )
}

internal fun LazyListScope.featureScreenSections(
    settings: FeaturesSectionSettings,
    resources: FeaturesSectionResources,
    ui: FeaturesSectionUi,
    actions: FeaturesSectionActions,
) {
    val matches = settings.search.matchFeatureSections(settings.visual.showChargingIcon)
    featureScreenDisplayLayoutNetwork(settings, resources, ui, actions, matches)
    featureScreenBatteryManagement(settings, resources, ui, actions, matches)
    if (settings.search.filtering && !matches.hasResults) featureScreenEmptySearch()
}

private fun LazyListScope.featureScreenDisplayLayoutNetwork(
    settings: FeaturesSectionSettings,
    resources: FeaturesSectionResources,
    ui: FeaturesSectionUi,
    actions: FeaturesSectionActions,
    matches: FeatureSectionsMatching,
) {
    val (search, featureCfg, featureRepo, visual, visualRepo) = settings
    if (matches.display) {
        featureDisplaySection(search, featureCfg, featureRepo, visual, visualRepo)
    }
    if (matches.layout) {
        featureLayoutSection(
            search = search,
            visual = visual,
            visualRepo = visualRepo,
            enabled = featureCfg.enabled,
            bothSidesChargingSearch = matches.bothSidesCharging,
        )
    }
    if (matches.network) {
        featureNetworkSection(
            search = search,
            visual = visual,
            visualRepo = visualRepo,
            enabled = featureCfg.enabled,
            weight = resources.mobileWeight,
            weightDialogOpen = ui.mobileWeightOpen,
            onOpenWeightDialog = actions.openMobileWeight,
        )
    }
}

private fun LazyListScope.featureScreenBatteryManagement(
    settings: FeaturesSectionSettings,
    resources: FeaturesSectionResources,
    ui: FeaturesSectionUi,
    actions: FeaturesSectionActions,
    matches: FeatureSectionsMatching,
) {
    val (search, featureCfg, featureRepo, visual, visualRepo) = settings
    if (matches.battery) {
        featureBatterySection(
            search = search,
            state = BatteryFeatureSnapshot(visual, featureCfg, resources.schemes, resources.batteryWeight),
            repositories = BatteryFeatureRepositories(visualRepo, featureRepo),
            dialogs = BatteryFeatureDialogs(
                colorOpen = ui.colorSheetOpen,
                onOpenColor = actions.openColorSheet,
                weightOpen = ui.batteryWeightOpen,
                onOpenWeight = actions.openBatteryWeight,
            ),
            bothSidesChargingSearch = matches.bothSidesCharging,
        )
    }
    if (matches.management) {
        featureManagementSection(
            search = search,
            ioBusy = ui.ioBusy,
            onExport = actions.export,
            onImport = actions.import,
            onReset = actions.reset,
        )
    }
}

private fun LazyListScope.featureScreenEmptySearch() {
    item {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.feature_search_empty),
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )
        }
    }
}
