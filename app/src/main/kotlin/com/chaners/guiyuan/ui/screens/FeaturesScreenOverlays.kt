package com.chaners.guiyuan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberSaveable
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.BATTERY_TOP_TEXT_WEIGHT_MAX
import com.chaners.guiyuan.settings.BATTERY_TOP_TEXT_WEIGHT_MIN

internal data class FeaturesOverlayToggles(
    val mobileWeight: MutableState<Boolean>,
    val batteryWeight: MutableState<Boolean>,
    val reset: MutableState<Boolean>,
    val colors: MutableState<Boolean>,
)

@Composable
internal fun rememberFeaturesOverlayToggles(): FeaturesOverlayToggles = FeaturesOverlayToggles(
    mobileWeight = rememberSaveable { mutableStateOf(false) },
    batteryWeight = rememberSaveable { mutableStateOf(false) },
    reset = rememberSaveable { mutableStateOf(false) },
    colors = rememberSaveable { mutableStateOf(false) },
)

@Composable
internal fun FeaturesScreenOverlays(
    resources: FeaturesScreenResources,
    backup: FeaturesBackupController,
    toggles: FeaturesOverlayToggles,
) {
    FeaturesConfirmationDialog(
        show = backup.state.pendingImport != null,
        titleRes = R.string.config_restore_title,
        summaryRes = R.string.config_restore_confirm,
        busy = backup.state.busy,
        onDismiss = { backup.state.pendingImport = null },
        onConfirm = backup.onRestore,
    )
    BatteryColorBottomSheet(
        show = toggles.colors.value,
        library = resources.battery.schemes,
        repository = resources.battery.repository,
        onDismiss = { toggles.colors.value = false },
    )
    FeaturesScreenWeightOverlays(resources, toggles)
    FeaturesConfirmationDialog(
        show = toggles.reset.value,
        titleRes = R.string.restore_feature_defaults,
        summaryRes = R.string.restore_feature_defaults_dialog_summary,
        busy = false,
        onDismiss = { toggles.reset.value = false },
        onConfirm = {
            toggles.reset.value = false
            resources.settings.featureRepo.reset()
            resources.settings.visualRepo.resetToDefaults()
        },
    )
}

@Composable
private fun FeaturesScreenWeightOverlays(
    resources: FeaturesScreenResources,
    toggles: FeaturesOverlayToggles,
) {
    FeaturesWeightDialog(
        show = toggles.mobileWeight.value,
        model = FeaturesWeightDialogModel(
            R.string.mobile_type_weight, resources.battery.mobileWeight,
            resources.settings.visual.mobileTypeWeight, 200..1000,
        ),
        onDismiss = { toggles.mobileWeight.value = false },
        onConfirmed = resources.settings.visualRepo::setMobileTypeWeight,
    )
    FeaturesWeightDialog(
        show = toggles.batteryWeight.value,
        model = FeaturesWeightDialogModel(
            R.string.battery_top_text_weight,
            resources.battery.batteryWeight,
            resources.settings.visual.topTextWeight,
            BATTERY_TOP_TEXT_WEIGHT_MIN..BATTERY_TOP_TEXT_WEIGHT_MAX,
        ),
        onDismiss = { toggles.batteryWeight.value = false },
        onConfirmed = resources.settings.visualRepo::setTopTextWeight,
    )
}
