package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.AppLang
import com.chaners.guiyuan.ui.navigation.AppRoute
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

internal data class SettingsHubSnapshot(
    val lang: AppLang,
    val iconHidden: Boolean,
    val swipeBackEnabled: Boolean,
    val runtimeBusy: Boolean,
)

internal data class SettingsHubActions(
    val onRestart: () -> Unit,
    val onLangChange: (AppLang) -> Unit,
    val onIconHiddenChange: (Boolean) -> Unit,
    val onSwipeBackChange: (Boolean) -> Unit,
    val onNavigate: (AppRoute) -> Unit,
)

@Composable
internal fun SettingsHubScreen(
    bottomContentPadding: Dp,
    snapshot: SettingsHubSnapshot,
    actions: SettingsHubActions,
) {
    val entryState = rememberAppSettingsEntryState(snapshot.runtimeBusy)
    HubPage(
        title = stringResource(R.string.settings_title),
        bottomContentPadding = bottomContentPadding,
        overlay = { AppSettingsEntryDialogs(entryState) },
    ) {
        settingsHubAppearanceSection(snapshot, actions)
        settingsHubAppSection(snapshot, actions, entryState)
        settingsHubMaintenanceSection(entryState, actions.onRestart, actions.onNavigate)
    }
}

private fun LazyListScope.settingsHubAppearanceSection(
    snapshot: SettingsHubSnapshot,
    actions: SettingsHubActions,
) {
    GroupedPreferenceSection(
        R.string.section_appearance_interaction,
        "settings-appearance",
        listOf(
            PreferenceCardItem("appearance") {
                ArrowPreference(
                    title = stringResource(R.string.appearance_title),
                    startAction = {
                        SemanticLeadingIcon(
                            iconRes = R.drawable.ic_material_symbol_display_settings,
                        )
                    },
                    summary = stringResource(R.string.appearance_summary),
                    onClick = { actions.onNavigate(AppRoute.Appearance) },
                )
            },
            PreferenceCardItem("swipe-back") {
                SwitchPreference(
                    title = stringResource(R.string.swipe_back),
                    startAction = {
                        SemanticLeadingIcon(
                            iconRes = R.drawable.ic_material_symbol_swipe,
                        )
                    },
                    summary = stringResource(R.string.swipe_back_summary),
                    checked = snapshot.swipeBackEnabled,
                    onCheckedChange = actions.onSwipeBackChange,
                )
            },
        ),
    )
}

private fun LazyListScope.settingsHubAppSection(
    snapshot: SettingsHubSnapshot,
    actions: SettingsHubActions,
    entryState: AppSettingsEntryState,
) {
    GroupedPreferenceSection(
        R.string.section_app,
        "settings-app",
        listOf(
            PreferenceCardItem("language") {
                AppLanguageRow(snapshot.lang, actions.onLangChange)
            },
            PreferenceCardItem("launcher-icon") {
                AppLauncherIconRow(snapshot.iconHidden, actions.onIconHiddenChange)
            },
            PreferenceCardItem("settings-entry") {
                AppSettingsEntryRows(entryState)
            },
        ),
    )
}
private fun LazyListScope.settingsHubMaintenanceSection(
    entryState: AppSettingsEntryState,
    onRestart: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
) {
    GroupedPreferenceSection(
        R.string.section_diagnostics_maintenance,
        "settings-maintenance",
        listOf(
            PreferenceCardItem("about") {
                ArrowPreference(
                    title = stringResource(R.string.about_title),
                    startAction = {
                        SemanticLeadingIcon(
                            iconRes = R.drawable.ic_material_symbol_info,
                        )
                    },
                    summary = stringResource(R.string.about_summary),
                    onClick = { onNavigate(AppRoute.About) },
                )
            },
            PreferenceCardItem("diagnostics") {
                ArrowPreference(
                    title = stringResource(R.string.diagnostics_title),
                    startAction = {
                        SemanticLeadingIcon(
                            iconRes = R.drawable.ic_material_symbol_troubleshoot_outlined,
                        )
                    },
                    summary = stringResource(R.string.diagnostics_summary),
                    onClick = { onNavigate(AppRoute.Diagnostics) },
                )
            },
            PreferenceCardItem("restart") {
                BasicComponent(
                    title = stringResource(R.string.restart_scope),
                    startAction = {
                        SemanticLeadingIcon(
                            iconRes = R.drawable.ic_material_symbol_restart_alt,
                            enabled = !entryState.busy,
                        )
                    },
                    summary = stringResource(R.string.restart_scope_summary),
                    enabled = !entryState.busy,
                    onClick = onRestart,
                )
            },
        ),
    )
}
