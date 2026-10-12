package com.chaners.guiyuan.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas as BitmapCanvas
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.GyApplication
import com.chaners.guiyuan.R
import com.chaners.guiyuan.system.RuntimeEnv
import com.chaners.guiyuan.system.XposedStatus
import io.github.libxposed.service.HookedTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun StaticScopeScreen(onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val app = remember(context) { context as GyApplication }
    val xposedStatus by app.xposedStatus.collectAsState()
    val env by produceState(initialValue = RuntimeEnv.basic(), key1 = context) {
        value = RuntimeEnv.resolve(context)
    }

    val connected = xposedStatus as? XposedStatus.Connected
    val connectedText = stringResource(R.string.runtime_scope_connected)
    val disabledText = stringResource(R.string.runtime_scope_disabled)
    val missingText = stringResource(R.string.runtime_scope_not_installed)
    val errorText = stringResource(R.string.runtime_scope_error)

    fun status(pkg: String, installed: Boolean?): Pair<String, Boolean> {
        if (installed == false) return missingText to false
        if (connected == null) return "—" to false
        if (pkg !in connected.scopes) return disabledText to false
        val failed = connected.runningTargets.any { (name, state) ->
            (name == pkg || name.startsWith("$pkg:")) && state == HookedTarget.State.FAILED
        }
        return if (failed) errorText to true else connectedText to false
    }

    SettingsPage(
        title = stringResource(R.string.static_scope_title),
        onBack = onBack,
    ) {
        item {
            Text(
                text = stringResource(R.string.static_scope_description),
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
                fontSize = MiuixTheme.textStyles.body2.fontSize,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        Section(R.string.static_scope_primary_group) {
            StaticScopeItem(
                packageName = "com.android.systemui",
                name = stringResource(R.string.static_scope_sysui),
                version = env.sysUiVersion,
                installed = env.sysUiInstalled,
                status = status("com.android.systemui", env.sysUiInstalled),
                fallbackIcon = R.drawable.ic_material_symbol_display_settings,
                usage = stringResource(R.string.static_scope_sysui_usage),
                advice = stringResource(R.string.static_scope_sysui_advice),
            )
        }
        Section(R.string.static_scope_optional_group) {
            StaticScopeItem(
                packageName = "com.android.settings",
                name = stringResource(R.string.runtime_scope_settings),
                version = env.settingsVersion,
                installed = env.settingsInstalled,
                status = status("com.android.settings", env.settingsInstalled),
                fallbackIcon = R.drawable.ic_material_symbol_settings_applications,
                usage = stringResource(R.string.static_scope_settings_usage),
                advice = stringResource(R.string.static_scope_settings_advice),
            )
        }
    }
}

@Composable
private fun StaticScopeItem(
    packageName: String,
    name: String,
    version: String,
    installed: Boolean?,
    status: Pair<String, Boolean>,
    @DrawableRes fallbackIcon: Int,
    usage: String,
    advice: String,
) {
    val versionText = when {
        installed == false || version.isBlank() || version == "—" ->
            stringResource(R.string.static_scope_version_unavailable)
        else -> stringResource(R.string.static_scope_installed_version, version)
    }

    BasicComponent(
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        startAction = { StaticScopeIcon(packageName, fallbackIcon) },
        endActions = {
            Text(
                text = status.first,
                fontSize = MiuixTheme.textStyles.body2.fontSize,
                color = if (status.second) {
                    MiuixTheme.colorScheme.error
                } else {
                    MiuixTheme.colorScheme.onSurfaceVariantSummary
                },
            )
        },
    ) {
        Text(
            text = name,
            fontSize = MiuixTheme.textStyles.headline1.fontSize,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = packageName,
            fontSize = MiuixTheme.textStyles.body2.fontSize,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Text(
            text = versionText,
            fontSize = MiuixTheme.textStyles.body2.fontSize,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    Text(
        text = usage,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        fontSize = MiuixTheme.textStyles.body2.fontSize,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    )
    Text(
        text = advice,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
        fontSize = MiuixTheme.textStyles.body2.fontSize,
        fontWeight = FontWeight.Medium,
        color = MiuixTheme.colorScheme.onSurface,
    )
}

@Composable
private fun StaticScopeIcon(
    packageName: String,
    @DrawableRes fallbackIcon: Int,
) {
    val context = LocalContext.current.applicationContext
    val icon by produceState<ImageBitmap?>(initialValue = null, key1 = context, key2 = packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val drawable = context.packageManager.getApplicationIcon(packageName).mutate()
                val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
                drawable.setBounds(0, 0, bitmap.width, bitmap.height)
                drawable.draw(BitmapCanvas(bitmap))
                bitmap.asImageBitmap()
            }.getOrNull()
        }
    }
    Box(
        modifier = Modifier.size(36.dp),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = icon
        if (bitmap == null) {
            SemanticLeadingIcon(iconRes = fallbackIcon)
        } else {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)),
            )
        }
    }
}
