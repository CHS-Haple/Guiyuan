package com.chaners.guiyuan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.chaners.guiyuan.settings.BATTERY_TOP_TEXT_WEIGHT_MAX
import com.chaners.guiyuan.settings.BATTERY_TOP_TEXT_WEIGHT_MIN
import com.chaners.guiyuan.settings.BatteryColorSchemeLibrary
import com.chaners.guiyuan.settings.BatterySchemeRepo
import com.chaners.guiyuan.settings.FeatureCfg
import com.chaners.guiyuan.settings.FeatureRepo
import com.chaners.guiyuan.settings.NativeFontAxis
import com.chaners.guiyuan.settings.VisualCfg
import com.chaners.guiyuan.settings.VisualRepo

internal data class FeaturesSettingsResources(
    val featureRepo: FeatureRepo,
    val feature: FeatureCfg,
    val visualRepo: VisualRepo,
    val visual: VisualCfg,
)

internal data class FeaturesBatteryResources(
    val repository: BatterySchemeRepo,
    val schemes: BatteryColorSchemeLibrary,
    val mobileWeight: FeatureWeightUi,
    val batteryWeight: FeatureWeightUi,
)

internal data class FeaturesScreenResources(
    val settings: FeaturesSettingsResources,
    val battery: FeaturesBatteryResources,
)

@Composable
internal fun rememberFeaturesScreenResources(): FeaturesScreenResources {
    val context = LocalContext.current.applicationContext
    val featureRepo = remember(context) { FeatureRepo(context) }
    val feature by featureRepo.settings.collectAsState(initial = featureRepo.current())
    val visualRepo = remember(context) { VisualRepo(context) }
    val visual by visualRepo.settings.collectAsState(initial = visualRepo.current())
    val fontAxis by remember(context) { NativeFontAxis.observe(context) }
        .collectAsState(initial = NativeFontAxis.State.Pending)
    val batteryAxis by remember(context) { NativeFontAxis.observeBattery(context) }
        .collectAsState(initial = NativeFontAxis.State.Pending)
    val mobileWeight = featureWeightUi(
        fontAxis, 200..1000, NativeWeightRow.MOBILE_TYPE, feature.enabled,
    )
    val batteryWeight = featureWeightUi(
        batteryAxis,
        BATTERY_TOP_TEXT_WEIGHT_MIN..BATTERY_TOP_TEXT_WEIGHT_MAX,
        NativeWeightRow.BATTERY,
        feature.enabled,
    )
    val schemeRepo = remember(context) { BatterySchemeRepo(context) }
    val schemes by schemeRepo.library.collectAsState(initial = schemeRepo.current())
    return FeaturesScreenResources(
        settings = FeaturesSettingsResources(featureRepo, feature, visualRepo, visual),
        battery = FeaturesBatteryResources(schemeRepo, schemes, mobileWeight, batteryWeight),
    )
}
