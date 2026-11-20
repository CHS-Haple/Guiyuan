package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.chaners.guiyuan.GyApplication
import com.chaners.guiyuan.R
import com.chaners.guiyuan.system.RuntimeEnv
import com.chaners.guiyuan.ui.LocalUiStyle
import com.chaners.guiyuan.settings.UiStyle
import com.chaners.guiyuan.ui.screens.about.GyBgEffect
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported

@Composable
internal fun AboutScreen(
    onBack: () -> Unit,
    onOpenThirdParty: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenScopes: () -> Unit,
) {
    val context = LocalContext.current
    val app = remember(context.applicationContext) {
        context.applicationContext as GyApplication
    }
    val xposedStatus by app.xposedStatus.collectAsState()
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val scroll = rememberAboutScrollState()
    val backdrop = rememberLayerBackdrop()
    val blur = remember { isRuntimeShaderSupported() }
    val logoBlend = rememberBrandLogoBlend()
    var showRepoUnavailable by rememberSaveable { mutableStateOf(false) }
    val env by produceState(initialValue = RuntimeEnv.basic(), key1 = context.applicationContext) {
        value = RuntimeEnv.resolve(context.applicationContext)
    }
    val actions = AboutPageActions(onOpenThirdParty, onOpenPrivacy, onOpenTerms, onOpenScopes)
    SettingsPage(
        title = stringResource(R.string.about_title),
        onBack = onBack,
        listState = scroll.list,
        compactTopBar = LocalUiStyle.current == UiStyle.OS4,
        titleAlpha = ((scroll.progress - 0.35f) / 0.65f).coerceIn(0f, 1f),
        background = {
            GyBgEffect(
                playing = lifecycleState.isAtLeast(Lifecycle.State.RESUMED) &&
                    scroll.visible && scroll.progress < 1f,
                bgModifier = if (blur) Modifier.layerBackdrop(backdrop) else Modifier,
                alpha = { 1f - scroll.progress },
            )
        },
        foreground = { padding -> AboutHeroForeground(scroll, padding, blur, backdrop, logoBlend) },
        overlay = {
            AboutRepositoryUnavailableDialog(showRepoUnavailable) { showRepoUnavailable = false }
        },
    ) {
        aboutPageItems(scroll) {
            AboutPageContent(env, xposedStatus, actions) { showRepoUnavailable = true }
        }
    }
}
