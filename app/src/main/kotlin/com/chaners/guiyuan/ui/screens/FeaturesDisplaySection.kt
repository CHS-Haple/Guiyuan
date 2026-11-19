package com.chaners.guiyuan.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.FeatureCfg
import com.chaners.guiyuan.settings.FeatureRepo
import com.chaners.guiyuan.settings.VisualCfg
import com.chaners.guiyuan.settings.VisualRepo
import top.yukonga.miuix.kmp.preference.SwitchPreference

private enum class FeatureDisplayToggle(
    @StringRes val titleRes: Int,
    @StringRes val summaryRes: Int,
    @DrawableRes val iconRes: Int,
) {
    KEYGUARD(
        R.string.keyguard_combined_status_title,
        R.string.keyguard_combined_status_summary,
        R.drawable.ic_material_symbol_lock,
    ),
    AOD(
        R.string.aod_combined_status_title,
        R.string.aod_combined_status_summary,
        R.drawable.ic_material_symbol_bedtime,
    ),
    CC_TINT(
        R.string.control_center_tint_transition,
        R.string.control_center_tint_transition_summary,
        R.drawable.ic_material_symbol_contrast,
    ),
    FILL_RETRACT(
        R.string.battery_fill_follow_retract,
        R.string.battery_fill_follow_retract_summary,
        R.drawable.ic_material_symbol_donut_large,
    ),
}

@Composable
private fun FeatureDisplaySwitch(
    row: FeatureDisplayToggle,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SwitchPreference(
        title = stringResource(row.titleRes),
        startAction = { SemanticLeadingIcon(row.iconRes, enabled) },
        summary = stringResource(row.summaryRes),
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange,
    )
}

internal fun LazyListScope.featureDisplaySection(
    search: PageSearch,
    featureCfg: FeatureCfg,
    featureRepo: FeatureRepo,
    visual: VisualCfg,
    visualRepo: VisualRepo,
) {
    Section(R.string.section_display_transition) {
        if (search.matches(R.string.keyguard_combined_status_title)) {
            FeatureDisplaySwitch(
                row = FeatureDisplayToggle.KEYGUARD,
                checked = featureCfg.keyguard,
                enabled = featureCfg.enabled,
                onCheckedChange = featureRepo::setKeyguard,
            )
        }
        if (search.matches(R.string.aod_combined_status_title)) {
            FeatureDisplaySwitch(
                row = FeatureDisplayToggle.AOD,
                checked = featureCfg.aod,
                enabled = featureCfg.enabled,
                onCheckedChange = featureRepo::setAod,
            )
        }
        if (search.matches(R.string.control_center_tint_transition)) {
            FeatureDisplaySwitch(
                row = FeatureDisplayToggle.CC_TINT,
                checked = visual.ccTintTransition,
                enabled = featureCfg.enabled,
                onCheckedChange = visualRepo::setCcTintTransition,
            )
        }
        if (search.matches(R.string.battery_fill_follow_retract)) {
            FeatureDisplaySwitch(
                row = FeatureDisplayToggle.FILL_RETRACT,
                checked = visual.fillFollowsRetract,
                enabled = featureCfg.enabled,
                onCheckedChange = visualRepo::setFillFollowsRetract,
            )
        }
    }
}
