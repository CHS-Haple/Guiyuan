package com.chaners.guiyuan.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.chaners.guiyuan.GyApplication
import com.chaners.guiyuan.system.ScopeRestart
import com.chaners.guiyuan.ui.components.UiOverlayDialog
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.system.RestartTarget
import com.chaners.guiyuan.ui.components.UiDialogButton
import com.chaners.guiyuan.ui.components.dialogActionGap
import com.chaners.guiyuan.ui.screens.SemanticLeadingIcon
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import top.yukonga.miuix.kmp.preference.CheckboxLocation
import top.yukonga.miuix.kmp.preference.CheckboxPreference

/** Owns pending restart selection until the native dialog exit animation completes. */
internal class RestartScopeDialogState {
    var pickerOpen by mutableStateOf(false)
        private set
    var availableTargets by mutableStateOf(emptySet<RestartTarget>())
        private set
    var selectedTargets by mutableStateOf(emptySet<RestartTarget>())
        private set
    private var queuedRestart by mutableStateOf(emptySet<RestartTarget>())

    fun openIfAllowed(app: GyApplication, blocked: Boolean) {
        if (blocked || !app.canRestartScopes()) return
        val scopes = app.enabledScopes()
        availableTargets = RestartTarget.entries.filterTo(mutableSetOf()) {
            scopes != null && it.pkg in scopes
        }
        selectedTargets =
            if (RestartTarget.SYS_UI in availableTargets) setOf(RestartTarget.SYS_UI)
            else availableTargets
        queuedRestart = emptySet()
        pickerOpen = true
    }

    fun setSelection(targets: Set<RestartTarget>) {
        selectedTargets = targets
    }

    fun dismiss() {
        queuedRestart = emptySet()
        pickerOpen = false
    }

    fun close() {
        pickerOpen = false
    }

    fun queueRestart() {
        queuedRestart = selectedTargets intersect availableTargets
        pickerOpen = false
    }

    fun finishDismiss(app: GyApplication, restartInProgress: Boolean) {
        if (queuedRestart.isEmpty() || restartInProgress) return
        val targets = queuedRestart
        queuedRestart = emptySet()
        // The system's active scope may change while the dialog is visible.
        val liveScopes = app.enabledScopes().orEmpty()
        if (targets.any { it.pkg !in liveScopes }) {
            availableTargets = RestartTarget.entries.filterTo(mutableSetOf()) {
                it.pkg in liveScopes
            }
            selectedTargets = targets intersect availableTargets
            pickerOpen = true
        } else if (!app.restartScopes(targets)) {
            pickerOpen = true
        }
    }
}

@Composable
internal fun RestartScopeDialog(
    state: RestartScopeDialogState,
    app: GyApplication,
    restartInProgress: Boolean,
) {
    UiOverlayDialog(
        title = stringResource(R.string.restart_scope),
        summary = stringResource(
            if (state.availableTargets.isEmpty()) R.string.restart_scope_no_targets
            else R.string.restart_scope_dialog_summary,
        ),
        show = state.pickerOpen,
        onDismissRequest = state::dismiss,
        onDismissFinished = { state.finishDismiss(app, restartInProgress) },
    ) {
        RestartScopeSelection(
            availableTargets = state.availableTargets,
            selectedTargets = state.selectedTargets,
            onSelectionChange = state::setSelection,
            onClose = state::close,
            onRestart = state::queueRestart,
        )
    }
}

@Composable
internal fun RestartFailureDialog(restartState: ScopeRestart.State) {
    val context = LocalContext.current
    val failedTargets = (restartState as? ScopeRestart.State.Failed)?.targets
    var failedTargetsForExit by remember { mutableStateOf(emptySet<RestartTarget>()) }
    if (failedTargets != null) {
        SideEffect { failedTargetsForExit = failedTargets }
    }
    UiOverlayDialog(
        title = stringResource(R.string.restart_scope_failed),
        summary = stringResource(
            R.string.restart_scope_failed_summary,
            (failedTargets ?: failedTargetsForExit)
                .map {
                    when (it) {
                        RestartTarget.SYS_UI -> context.getString(R.string.restart_scope_sysui)
                        RestartTarget.SETTINGS -> context.getString(R.string.restart_scope_settings)
                    }
                }
                .joinToString(", "),
        ),
        show = restartState is ScopeRestart.State.Failed,
        onDismissRequest = ScopeRestart::clearFailure,
        onDismissFinished = { failedTargetsForExit = emptySet() },
    ) {
        UiDialogButton(
            text = stringResource(R.string.confirm),
            modifier = Modifier.fillMaxWidth(),
            role = GlassDialogButtonRole.Primary,
            onClick = ScopeRestart::clearFailure,
        )
    }
}

// Use the same installed-package artwork the scope manager displays.
@Composable
private fun RestartAppIcon(pkg: String, fallback: Int) {
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val icon = remember(ctx, pkg, density.density, config) {
        runCatching {
            val side = with(density) { 48.dp.roundToPx() }
            val drawable = ctx.packageManager.getApplicationIcon(pkg)
            val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, side, side)
            drawable.draw(Canvas(bitmap))
            bitmap.asImageBitmap()
        }.getOrNull()
    }
    if (icon != null) {
        Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(24.dp))
    } else {
        SemanticLeadingIcon(fallback)
    }
}

@Composable
internal fun RestartScopeSelection(
    availableTargets: Set<RestartTarget>,
    selectedTargets: Set<RestartTarget>,
    onSelectionChange: (Set<RestartTarget>) -> Unit,
    onClose: () -> Unit,
    onRestart: () -> Unit,
) {
    Column {
        RestartScopeChoices(availableTargets, selectedTargets, onSelectionChange)
        RestartScopeActions(availableTargets, selectedTargets, onSelectionChange, onClose, onRestart)
    }
}

@Composable
private fun ColumnScope.RestartScopeChoices(
    availableTargets: Set<RestartTarget>,
    selectedTargets: Set<RestartTarget>,
    onSelectionChange: (Set<RestartTarget>) -> Unit,
) {
    for (target in RestartTarget.entries) {
        if (target !in availableTargets) continue
        val titleRes = when (target) {
            RestartTarget.SYS_UI -> R.string.restart_scope_sysui
            RestartTarget.SETTINGS -> R.string.restart_scope_settings
        }
        val iconRes = when (target) {
            RestartTarget.SYS_UI -> R.drawable.ic_material_symbol_dashboard
            RestartTarget.SETTINGS -> R.drawable.ic_material_symbol_settings_applications
        }
        CheckboxPreference(
            title = stringResource(titleRes),
            summary = target.pkg,
            checked = target in selectedTargets,
            onCheckedChange = { checked ->
                onSelectionChange(if (checked) selectedTargets + target else selectedTargets - target)
            },
            checkboxLocation = CheckboxLocation.End,
            startAction = { RestartAppIcon(target.pkg, iconRes) },
        )
    }
}

@Composable
private fun ColumnScope.RestartScopeActions(
    availableTargets: Set<RestartTarget>,
    selectedTargets: Set<RestartTarget>,
    onSelectionChange: (Set<RestartTarget>) -> Unit,
    onClose: () -> Unit,
    onRestart: () -> Unit,
) {
    if (availableTargets.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
    }
    if (availableTargets.isEmpty()) {
        UiDialogButton(
            text = stringResource(R.string.confirm),
            modifier = Modifier.fillMaxWidth(),
            onClick = onClose,
        )
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            UiDialogButton(
                text = stringResource(
                    if (selectedTargets == availableTargets) {
                        R.string.restart_scope_clear_all
                    } else {
                        R.string.restart_scope_select_all
                    },
                ),
                modifier = Modifier.weight(1f),
                onClick = {
                    onSelectionChange(
                        if (selectedTargets == availableTargets) emptySet()
                        else availableTargets,
                    )
                },
            )
            Spacer(Modifier.width(dialogActionGap()))
            UiDialogButton(
                text = stringResource(R.string.restart),
                modifier = Modifier.weight(1f),
                enabled = selectedTargets.isNotEmpty(),
                role = GlassDialogButtonRole.Primary,
                onClick = onRestart,
            )
        }
    }
}
