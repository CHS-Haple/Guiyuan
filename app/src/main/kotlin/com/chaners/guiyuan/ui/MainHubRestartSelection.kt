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
