package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.system.RuntimeEnv
import com.chaners.guiyuan.system.XposedStatus
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.ArrowPreference

private const val ABOUT_VALUE_SEPARATOR = "｜"

@Composable
internal fun AboutLinksCard(
    onOpenProject: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenLicense: () -> Unit,
    onOpenThirdParty: () -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
    ) {
        ArrowPreference(
            title = stringResource(R.string.about_project_home_title),
            summary = stringResource(R.string.about_project_home_summary),
            startAction = {
                SemanticLeadingIcon(
                    iconRes = R.drawable.ic_material_symbol_folder_code,
                )
            },
            onClick = onOpenProject,
        )
        ArrowPreference(
            title = stringResource(R.string.terms_title),
            summary = stringResource(R.string.about_terms_summary),
            startAction = {
                SemanticLeadingIcon(iconRes = R.drawable.ic_material_symbol_contract)
            },
            onClick = onOpenTerms,
        )
        ArrowPreference(
            title = stringResource(R.string.privacy_title),
            summary = stringResource(R.string.about_privacy_summary),
            startAction = {
                SemanticLeadingIcon(
                    iconRes = R.drawable.ic_material_symbol_policy,
                )
            },
            onClick = onOpenPrivacy,
        )
        ArrowPreference(
            title = stringResource(R.string.about_open_source_license_title),
            summary = stringResource(R.string.about_open_source_license_summary),
            startAction = {
                SemanticLeadingIcon(
                    iconRes = R.drawable.ic_material_symbol_license,
                )
            },
            onClick = onOpenLicense,
        )
        ArrowPreference(
            title = stringResource(R.string.about_third_party_title),
            summary = stringResource(R.string.about_third_party_summary),
            startAction = {
                SemanticLeadingIcon(
                    iconRes = R.drawable.ic_material_symbol_account_tree,
                )
            },
            onClick = onOpenThirdParty,
        )
    }
}

@Composable
internal fun AboutEnvironmentCard(
    env: RuntimeEnv,
    xposedStatus: XposedStatus,
    onOpenScopes: () -> Unit,
) {
    SmallTitle(stringResource(R.string.section_device_system))
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
    ) {
        AboutDeviceDetails(env)
        AboutRuntimeDetails(xposedStatus, onOpenScopes)
    }
}

@Composable
private fun AboutDeviceDetails(env: RuntimeEnv) {
    val unavailable = stringResource(R.string.about_value_unavailable)
    val deviceSummary =
        listOf(
            env.device.trim(),
            env.model
                .trim()
                .takeIf { model ->
                    model.isNotBlank() &&
                        !model.equals(env.device.trim(), ignoreCase = true)
                },
        )
            .filterNotNull()
            .filter(String::isNotBlank)
            .ifEmpty { listOf(unavailable) }
            .joinToString(separator = ABOUT_VALUE_SEPARATOR)
    val androidSummary =
        if (env.androidVersion.isNotBlank()) {
            buildString {
                append("Android ")
                append(env.androidVersion)
                append(ABOUT_VALUE_SEPARATOR)
                append("API ")
                append(env.sdk)
            }
        } else {
            unavailable
        }

    BasicComponent(
        title = stringResource(R.string.device_name_label),
        summary = deviceSummary,
        startAction = {
            SemanticLeadingIcon(
                iconRes = R.drawable.ic_material_symbol_smartphone,
            )
        },
    )
    BasicComponent(
        title = stringResource(R.string.android_version_label),
        summary = androidSummary,
        startAction = {
            SemanticLeadingIcon(
                iconRes = R.drawable.ic_material_symbol_android,
            )
        },
    )
    BasicComponent(
        title = stringResource(R.string.os_version_label),
        summary = env.osVersion.ifBlank { unavailable },
        startAction = {
            SemanticLeadingIcon(
                iconRes = R.drawable.ic_material_symbol_layers,
            )
        },
    )
}

@Composable
private fun AboutRuntimeDetails(
    xposedStatus: XposedStatus,
    onOpenScopes: () -> Unit,
) {
    val serviceSummary = when (val status = xposedStatus) {
        is XposedStatus.Connected ->
            stringResource(R.string.runtime_service_connected, status.apiVersion)
        XposedStatus.Checking -> stringResource(R.string.runtime_service_checking)
        XposedStatus.FrameworkUnavailable -> stringResource(R.string.runtime_service_disconnected)
        XposedStatus.QueryUnavailable -> stringResource(R.string.runtime_service_query_unavailable)
    }
    BasicComponent(
        title = stringResource(R.string.runtime_service_title),
        summary = serviceSummary,
        startAction = {
            SemanticLeadingIcon(
                iconRes = R.drawable.ic_material_symbol_extension,
            )
        },
    )
    ArrowPreference(
        title = stringResource(R.string.runtime_scope_title),
        summary = stringResource(R.string.static_scope_entry_summary),
        startAction = {
            SemanticLeadingIcon(
                iconRes = R.drawable.ic_material_symbol_target,
            )
        },
        onClick = onOpenScopes,
    )
}
