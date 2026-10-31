package com.chaners.guiyuan.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.GyApplication
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.AppLang
import com.chaners.guiyuan.settings.SettingsEntry
import com.chaners.guiyuan.settings.SettingsEntryCfg
import com.chaners.guiyuan.settings.SettingsEntryIcon
import com.chaners.guiyuan.settings.SettingsEntryPrefs
import com.chaners.guiyuan.system.ScopeRestart
import com.chaners.guiyuan.ui.components.UiDialogButton
import com.chaners.guiyuan.ui.components.UiOverlayDialog
import com.chaners.guiyuan.ui.components.dialogActionGap
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import top.yukonga.miuix.kmp.preference.SwitchPreference

// Shared with the normal Settings page and first-run OOBE. SettingsEntryPrefs
// remains the only persistent entry writer; the framework owns scope grants.
internal class AppSettingsEntryState(
    val config: SettingsEntryCfg,
    val busy: Boolean,
    val pending: SettingsEntry?,
    val failed: Boolean,
    val selectEntry: (SettingsEntry) -> Unit,
    val selectIcon: (SettingsEntryIcon) -> Unit,
    val dismissPending: () -> Unit,
    val confirmPending: () -> Unit,
    val dismissFailed: () -> Unit,
)

@Composable
internal fun rememberAppSettingsEntryState(runtimeBusy: Boolean): AppSettingsEntryState {
    val context = LocalContext.current.applicationContext
    val app = remember(context) { context as GyApplication }
    val configFlow = remember(context) { SettingsEntryPrefs.observe(context) }
    val config by configFlow.collectAsState(
        initial = SettingsEntryCfg(
            entry = SettingsEntryPrefs.get(context),
            icon = SettingsEntryPrefs.icon(context),
        ),
    )
    val scopeBusy by app.scopeRequestInFlight.collectAsState(initial = app.scopeRequestActive)
    val reloadBusy by app.hotReloadInFlight.collectAsState(initial = app.hotReloadActive)
    val restartState by ScopeRestart.state.collectAsState()
    var pending by remember { mutableStateOf<SettingsEntry?>(null) }
    var applying by remember { mutableStateOf<SettingsEntry?>(null) }
    var failed by remember { mutableStateOf(false) }
    val busy = runtimeBusy || scopeBusy || reloadBusy ||
        restartState is ScopeRestart.State.Running || applying != null

    fun saveEntry(value: SettingsEntry) {
        if (!SettingsEntryPrefs.set(context, value)) failed = true
    }
    fun saveIcon(value: SettingsEntryIcon) {
        if (!SettingsEntryPrefs.setIcon(context, value)) failed = true
    }

    return AppSettingsEntryState(
        config = config,
        busy = busy,
        pending = pending,
        failed = failed,
        selectEntry = { selected ->
            if (!busy && selected != config.entry) {
                if (selected == SettingsEntry.Disabled || app.settingsInScope()) {
                    saveEntry(selected)
                } else {
                    pending = selected
                }
            }
        },
        selectIcon = { icon -> if (!busy) saveIcon(icon) },
        dismissPending = { pending = null },
        confirmPending = {
            val selected = pending
            pending = null
            if (selected != null && !busy) {
                val previous = SettingsEntryPrefs.get(context)
                applying = selected
                val accepted = app.requestSettingsScope { approved ->
                    if (applying == selected) {
                        applying = null
                        if (approved) {
                            // A newer setting takes precedence over a delayed scope grant.
                            if (SettingsEntryPrefs.get(context) == previous) saveEntry(selected)
                        } else {
                            failed = true
                        }
                    }
                }
                if (!accepted) {
                    applying = null
                    failed = true
                }
            }
        },
        dismissFailed = { failed = false },
    )
}

@Composable
internal fun AppSettingsRows(
    lang: AppLang,
    iconHidden: Boolean,
    onLangChange: (AppLang) -> Unit,
    onIconHiddenChange: (Boolean) -> Unit,
    entryState: AppSettingsEntryState,
) {
    UiDropdownPreference(
        items = listOf(
            stringResource(R.string.language_system),
            stringResource(R.string.language_english),
            stringResource(R.string.language_simplified_chinese),
        ),
        selectedIndex = lang.ordinal,
        title = stringResource(R.string.language_title),
        startAction = {
            SemanticLeadingIcon(iconRes = R.drawable.ic_material_symbol_language)
        },
        summary = stringResource(R.string.language_summary),
        showValue = true,
        onSelectedIndexChange = { index ->
            AppLang.entries.getOrNull(index)?.let(onLangChange)
        },
    )
    SwitchPreference(
        title = stringResource(R.string.hide_launcher_icon),
        startAction = {
            SemanticLeadingIcon(iconRes = R.drawable.ic_material_symbol_visibility_off)
        },
        summary = stringResource(R.string.hide_launcher_icon_summary),
        checked = iconHidden,
        onCheckedChange = onIconHiddenChange,
    )
    UiDropdownPreference(
        items = listOf(
            stringResource(R.string.settings_entry_disabled),
            stringResource(R.string.settings_entry_top),
            stringResource(R.string.settings_entry_middle),
            stringResource(R.string.settings_entry_bottom),
        ),
        selectedIndex = entryState.config.entry.ordinal,
        title = stringResource(R.string.settings_entry_title),
        startAction = {
            SemanticLeadingIcon(
                iconRes = R.drawable.ic_material_symbol_settings_applications,
                enabled = !entryState.busy,
            )
        },
        enabled = !entryState.busy,
        summary = stringResource(
            if (entryState.config.entry == SettingsEntry.Disabled) R.string.settings_entry_summary
            else R.string.settings_entry_restart_hint,
        ),
        showValue = true,
        onSelectedIndexChange = { index ->
            SettingsEntry.entries.getOrNull(index)?.let(entryState.selectEntry)
        },
    )
    if (entryState.config.entry != SettingsEntry.Disabled) {
        UiDropdownPreference(
            items = listOf(
                stringResource(R.string.settings_entry_icon_rounded),
                stringResource(R.string.settings_entry_icon_hollow),
            ),
            selectedIndex = entryState.config.icon.ordinal,
            title = stringResource(R.string.settings_entry_icon_title),
            startAction = {
                SemanticLeadingIcon(iconRes = R.drawable.ic_material_symbol_filter_none)
            },
            enabled = !entryState.busy,
            summary = stringResource(R.string.settings_entry_icon_summary),
            showValue = true,
            onSelectedIndexChange = { index ->
                SettingsEntryIcon.entries.getOrNull(index)?.let(entryState.selectIcon)
            },
        )
    }
}

@Composable
internal fun AppSettingsEntryDialogs(entryState: AppSettingsEntryState) {
    UiOverlayDialog(
        title = stringResource(R.string.settings_entry_scope_title),
        summary = stringResource(R.string.settings_entry_scope_summary),
        show = entryState.pending != null,
        onDismissRequest = entryState.dismissPending,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            UiDialogButton(
                text = stringResource(R.string.cancel),
                modifier = Modifier.weight(1f),
                onClick = entryState.dismissPending,
            )
            Spacer(Modifier.width(dialogActionGap()))
            UiDialogButton(
                text = stringResource(R.string.confirm),
                modifier = Modifier.weight(1f),
                role = GlassDialogButtonRole.Primary,
                enabled = !entryState.busy,
                onClick = entryState.confirmPending,
            )
        }
    }
    UiOverlayDialog(
        title = stringResource(R.string.settings_entry_failed_title),
        summary = stringResource(R.string.settings_entry_failed_summary),
        show = entryState.failed,
        onDismissRequest = entryState.dismissFailed,
    ) {
        UiDialogButton(
            text = stringResource(R.string.confirm),
            modifier = Modifier.fillMaxWidth(),
            onClick = entryState.dismissFailed,
        )
    }
}
