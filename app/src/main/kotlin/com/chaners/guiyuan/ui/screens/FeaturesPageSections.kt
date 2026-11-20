package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.FeatureCfg
import com.chaners.guiyuan.settings.FeatureRepo
import com.chaners.guiyuan.settings.VisualCfg
import com.chaners.guiyuan.settings.VisualRepo
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.preference.SwitchPreference

internal fun LazyListScope.featureDisplaySection(
    search: PageSearch,
    visual: VisualCfg,
    visualRepo: VisualRepo,
    featureCfg: FeatureCfg,
    featureRepo: FeatureRepo,
) {
    Section(R.string.section_display_transition) {
        if (search.matches(R.string.keyguard_combined_status_title)) {
            SwitchPreference(
                title = stringResource(R.string.keyguard_combined_status_title),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_lock, featureCfg.enabled) },
                summary = stringResource(R.string.keyguard_combined_status_summary),
                checked = featureCfg.keyguard,
                enabled = featureCfg.enabled,
                onCheckedChange = featureRepo::setKeyguard,
            )
        }
        if (search.matches(R.string.aod_combined_status_title)) {
            SwitchPreference(
                title = stringResource(R.string.aod_combined_status_title),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_bedtime, featureCfg.enabled) },
                summary = stringResource(R.string.aod_combined_status_summary),
                checked = featureCfg.aod,
                enabled = featureCfg.enabled,
                onCheckedChange = featureRepo::setAod,
            )
        }
        if (search.matches(R.string.control_center_tint_transition)) {
            SwitchPreference(
                title = stringResource(R.string.control_center_tint_transition),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_contrast, featureCfg.enabled) },
                summary = stringResource(R.string.control_center_tint_transition_summary),
                checked = visual.ccTintTransition,
                enabled = featureCfg.enabled,
                onCheckedChange = visualRepo::setCcTintTransition,
            )
        }
        if (search.matches(R.string.battery_fill_follow_retract)) {
            SwitchPreference(
                title = stringResource(R.string.battery_fill_follow_retract),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_donut_large, featureCfg.enabled) },
                summary = stringResource(R.string.battery_fill_follow_retract_summary),
                checked = visual.fillFollowsRetract,
                enabled = featureCfg.enabled,
                onCheckedChange = visualRepo::setFillFollowsRetract,
            )
        }
    }
}

internal fun LazyListScope.featureManagementSection(
    search: PageSearch,
    ioBusy: Boolean,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onRequestReset: () -> Unit,
) {
    Section(R.string.section_management) {
        if (search.matches(R.string.config_backup_title)) {
            BasicComponent(
                title = stringResource(R.string.config_backup_title),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_save) },
                summary = stringResource(R.string.config_backup_summary),
                enabled = !ioBusy,
                onClick = onBackup,
            )
        }
        if (search.matches(R.string.config_restore_title)) {
            BasicComponent(
                title = stringResource(R.string.config_restore_title),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_file_open) },
                summary = stringResource(R.string.config_restore_summary),
                enabled = !ioBusy,
                onClick = onRestore,
            )
        }
        if (search.matches(R.string.restore_feature_defaults)) {
            BasicComponent(
                title = stringResource(R.string.restore_feature_defaults),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_settings_backup_restore) },
                summary = stringResource(R.string.restore_feature_defaults_summary),
                onClick = onRequestReset,
            )
        }
    }
}
