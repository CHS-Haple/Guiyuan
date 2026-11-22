package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import com.chaners.guiyuan.ui.components.UiOverlayDialog
import com.chaners.guiyuan.ui.components.UiDialogButton
import com.chaners.guiyuan.ui.components.dialogActionGap
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.R
import com.chaners.guiyuan.system.XposedStatus
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal data class GuideFrameworkSnapshot(
    val framework: XposedStatus,
    val sysUiInScope: Boolean,
    val canRequestScope: Boolean,
    val scopeBusy: Boolean,
    val scopeRequestFailed: Boolean,
)

internal data class GuideRootSnapshot(
    val rootChecking: Boolean,
    val rootAccess: Boolean?,
)

internal data class GuidePermissionActions(
    val expandedPermission: Int,
    val onToggleExpanded: (Int) -> Unit,
    val onRequestScope: () -> Unit,
)

@Composable
internal fun ColumnScope.GuidePermissionStage(
    status: GuideFrameworkSnapshot,
    root: GuideRootSnapshot,
    actions: GuidePermissionActions,
) {
    GuideFrameworkPermission(status, actions)
    GuideScopePermission(status, actions)
    GuideRootPermission(status, root, actions)
}

@Composable
private fun ColumnScope.GuideFrameworkPermission(
    status: GuideFrameworkSnapshot,
    actions: GuidePermissionActions,
) {
    val framework = status.framework
    val connected = framework as? XposedStatus.Connected
    val expandedPermission = actions.expandedPermission
                            Card(modifier = Modifier.fillMaxWidth()) {
                                GuidePermissionItem(
                                    display = GuidePermissionDisplay(
                                        iconRes = R.drawable.ic_material_symbol_extension,
                                        title = stringResource(R.string.guide_framework),
                                        status = when (framework) {
                                            XposedStatus.Checking -> stringResource(R.string.guide_framework_checking)
                                            XposedStatus.FrameworkUnavailable -> stringResource(R.string.guide_framework_offline)
                                            XposedStatus.QueryUnavailable -> stringResource(R.string.guide_framework_read_failed)
                                            is XposedStatus.Connected -> stringResource(R.string.guide_framework_connected)
                                        },
                                        purpose = stringResource(R.string.guide_framework_purpose),
                                        checked = connected != null,
                                        guidance = when (framework) {
                                            XposedStatus.FrameworkUnavailable -> stringResource(R.string.guide_framework_help)
                                            XposedStatus.QueryUnavailable -> stringResource(R.string.guide_framework_read_help)
                                            else -> null
                                        },
                                    ),
                                    disclosure = GuidePermissionDisclosure(
                                        expanded = expandedPermission == 1,
                                        onToggleDetails = { actions.onToggleExpanded(1) },
                                    ),
                                )
                            }
                            Spacer(Modifier.height(12.dp))

}

@Composable
private fun ColumnScope.GuideScopePermission(
    status: GuideFrameworkSnapshot,
    actions: GuidePermissionActions,
) {
    val framework = status.framework
    val connected = framework as? XposedStatus.Connected
    val sysUiInScope = status.sysUiInScope
    val canRequestScope = status.canRequestScope
    val scopeBusy = status.scopeBusy
    val expandedPermission = actions.expandedPermission
                            Card(modifier = Modifier.fillMaxWidth()) {
                                GuidePermissionItem(
                                    display = GuidePermissionDisplay(
                                        iconRes = R.drawable.ic_material_symbol_target,
                                        title = stringResource(R.string.guide_sysui_scope),
                                        status = when {
                                            sysUiInScope -> stringResource(R.string.runtime_scope_authorized)
                                            framework == XposedStatus.Checking -> stringResource(R.string.guide_framework_checking)
                                            framework == XposedStatus.QueryUnavailable -> stringResource(R.string.guide_scope_read_failed)
                                            connected == null -> stringResource(R.string.guide_scope_wait_framework)
                                            scopeBusy -> stringResource(R.string.guide_scope_requesting)
                                            else -> stringResource(R.string.runtime_scope_disabled)
                                        },
                                        purpose = stringResource(R.string.guide_sysui_scope_purpose),
                                        checked = sysUiInScope,
                                        guidance = when {
                                            sysUiInScope -> null
                                            framework == XposedStatus.QueryUnavailable -> stringResource(R.string.guide_framework_read_help)
                                            connected == null -> stringResource(R.string.guide_scope_help_offline)
                                            connected.apiVersion < 102 -> stringResource(R.string.guide_scope_help_api)
                                            !canRequestScope -> stringResource(R.string.guide_scope_help_busy)
                                            else -> stringResource(R.string.guide_scope_help_request)
                                        },
                                    ),
                                    disclosure = GuidePermissionDisclosure(
                                        expanded = expandedPermission == 2,
                                        onToggleDetails = { actions.onToggleExpanded(2) },
                                    ),
                                )
                                if (!sysUiInScope) {
                                    TextButton(
                                        text = stringResource(R.string.guide_scope_request_action),
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                                        colors = ButtonDefaults.textButtonColorsPrimary(),
                                        enabled = canRequestScope,
                                        onClick = actions.onRequestScope,
                                    )
                                }
                            }

}

@Composable
private fun ColumnScope.GuideRootPermission(
    status: GuideFrameworkSnapshot,
    root: GuideRootSnapshot,
    actions: GuidePermissionActions,
) {
    val rootChecking = root.rootChecking
    val rootAccess = root.rootAccess
    val scopeBusy = status.scopeBusy
    val scopeRequestFailed = status.scopeRequestFailed
    val expandedPermission = actions.expandedPermission
                            SmallTitle(text = stringResource(R.string.guide_permission_later))
                            Card(modifier = Modifier.fillMaxWidth()) {
                                GuidePermissionItem(
                                    display = GuidePermissionDisplay(
                                        iconRes = R.drawable.ic_material_symbol_admin_panel_settings,
                                        title = stringResource(R.string.guide_root_title),
                                        status = when {
                                            rootChecking -> stringResource(R.string.guide_root_checking)
                                            rootAccess == true -> stringResource(R.string.guide_root_granted)
                                            rootAccess == false -> stringResource(R.string.guide_root_unavailable)
                                            else -> stringResource(R.string.guide_root_not_checked)
                                        },
                                        purpose = stringResource(R.string.guide_root_purpose),
                                        checked = rootAccess == true,
                                        guidance = if (rootAccess == false) stringResource(R.string.guide_root_help) else null,
                                    ),
                                    disclosure = GuidePermissionDisclosure(
                                        expanded = expandedPermission == 3,
                                        onToggleDetails = { actions.onToggleExpanded(3) },
                                    ),
                                )
                            }
                            if (scopeBusy || scopeRequestFailed) {
                                Text(
                                    text = stringResource(
                                        if (scopeBusy) R.string.guide_scope_requesting
                                        else R.string.guide_scope_request_failed,
                                    ),
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                    style = MiuixTheme.textStyles.footnote1,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            }

}

@Composable
internal fun GuidePermissionWarningDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit,
    onReturn: () -> Unit,
    onContinue: () -> Unit,
) {
    UiOverlayDialog(
        show = show,
        title = stringResource(R.string.guide_permission_warning_title),
        summary = stringResource(R.string.guide_permission_warning_summary),
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            UiDialogButton(
                text = stringResource(R.string.guide_permission_return),
                modifier = Modifier.weight(1f),
                role = GlassDialogButtonRole.Primary,
                onClick = onReturn,
            )
            Spacer(Modifier.width(dialogActionGap()))
            UiDialogButton(
                text = stringResource(R.string.guide_permission_continue),
                modifier = Modifier.weight(1f),
                onClick = onContinue,
            )
        }
    }
}
