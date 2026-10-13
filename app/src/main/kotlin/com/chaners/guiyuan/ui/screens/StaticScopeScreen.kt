package com.chaners.guiyuan.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas as BitmapCanvas
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
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
import top.yukonga.miuix.kmp.basic.Surface
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
                status = scopeState("com.android.systemui", env.sysUiInstalled, xposedStatus),
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
                status = scopeState("com.android.settings", env.settingsInstalled, xposedStatus),
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
    status: ScopeState,
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
            val label =
                when (status) {
                    ScopeState.Authorized -> stringResource(R.string.runtime_scope_authorized)
                    ScopeState.Unauthorized -> stringResource(R.string.runtime_scope_disabled)
                    ScopeState.Missing -> stringResource(R.string.runtime_scope_not_installed)
                    ScopeState.Failed -> stringResource(R.string.runtime_scope_error)
                    ScopeState.Unknown -> "—"
                }
            val color =
                when (status) {
                    ScopeState.Authorized -> MiuixTheme.colorScheme.primary
                    ScopeState.Failed -> MiuixTheme.colorScheme.error
                    else -> MiuixTheme.colorScheme.onSurfaceVariantSummary
                }
            val backgroundAlpha =
                when (status) {
                    ScopeState.Authorized, ScopeState.Failed -> 0.12f
                    else -> 0.07f
                }
            Surface(
                modifier = Modifier.heightIn(min = 20.dp),
                shape = RoundedCornerShape(5.dp),
                color = color.copy(alpha = backgroundAlpha),
                contentColor = color,
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        fontSize = MiuixTheme.textStyles.footnote2.fontSize,
                        fontWeight = FontWeight.SemiBold,
                        color = color,
                        maxLines = 1,
                    )
                }
            }
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

internal fun scopeState(pkg: String, installed: Boolean?, status: XposedStatus): ScopeState {
    if (installed == false) return ScopeState.Missing
    val connected = status as? XposedStatus.Connected ?: return ScopeState.Unknown
    if (pkg !in connected.scopes) return ScopeState.Unauthorized

    val failed = connected.runningTargets.any { (name, state) ->
        (name == pkg || name.startsWith("$pkg:")) && state == HookedTarget.State.FAILED
    }
    // Reported targets are a snapshot, not proof that a host is running now.
    return if (failed) ScopeState.Failed else ScopeState.Authorized
}

internal enum class ScopeState {
    Authorized,
    Unauthorized,
    Missing,
    Failed,
    Unknown,
}
