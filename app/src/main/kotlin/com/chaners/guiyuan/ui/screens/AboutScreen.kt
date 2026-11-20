package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.chaners.guiyuan.BuildConfig
import com.chaners.guiyuan.GyApplication
import com.chaners.guiyuan.R
import com.chaners.guiyuan.system.RuntimeEnv
import com.chaners.guiyuan.ui.LocalUiStyle
import com.chaners.guiyuan.settings.UiStyle
import com.chaners.guiyuan.ui.screens.about.GyBgEffect
import com.chaners.guiyuan.ui.components.UiDialogButton
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import com.chaners.guiyuan.ui.components.UiOverlayDialog

private const val ABOUT_PROJECT_URL = "https://github.com/CHS-Haple/Guiyuan"
private const val ABOUT_LICENSE_URL = "https://github.com/CHS-Haple/Guiyuan/blob/main/LICENSE"
private const val GPL_LICENSE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"
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
    val uriHandler = LocalUriHandler.current
    val listState = rememberLazyListState()
    val os4 = LocalUiStyle.current == UiStyle.OS4
    val density = LocalDensity.current
    val backdrop = rememberLayerBackdrop()
    val blur = remember { isRuntimeShaderSupported() }
    val logoBlend = rememberBrandLogoBlend()
    var heroHeight by remember { mutableStateOf(200.dp) }
    var spacerBottom by remember { mutableFloatStateOf(0f) }
    var iconBottom by remember { mutableFloatStateOf(0f) }
    var nameBottom by remember { mutableFloatStateOf(0f) }
    var versionBottom by remember { mutableFloatStateOf(0f) }
    val spacerHeight = heroHeight + 218.dp
    // The content may exceed one screen; use a minimum, not a fixed parent height.
    val contentMinHeight by remember(density, listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            val available = (
                info.viewportSize.height - info.beforeContentPadding - info.afterContentPadding
            ).coerceAtLeast(0)
            with(density) { available.toDp() }
        }
    }
    val scrollProgress by remember(density) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset /
                with(density) { (heroHeight + 218.dp).toPx() }).coerceIn(0f, 1f)
        }
    }
    val visible by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val collapse by remember {
        derivedStateOf {
            val offset = listState.firstVisibleItemScrollOffset.toFloat()
            if (listState.firstVisibleItemIndex > 0) {
                Triple(1f, 1f, 1f)
            } else if (spacerBottom <= versionBottom ||
                versionBottom <= nameBottom || nameBottom <= iconBottom
            ) {
                // The first layout may not have measured yet. A nonzero scroll
                // must not leave an opaque identity mark over a preference row.
                if (offset > 0f) Triple(1f, 1f, 1f) else Triple(0f, 0f, 0f)
            } else {
                val first = spacerBottom - versionBottom
                val second = versionBottom - nameBottom
                val third = nameBottom - iconBottom
                Triple(
                    ((offset - first * 0.5f) / (first * 0.5f)).coerceIn(0f, 1f),
                    ((offset - first) / second).coerceIn(0f, 1f),
                    ((offset - first - second) / third).coerceIn(0f, 1f),
                )
            }
        }
    }
    var showRepoUnavailable by rememberSaveable { mutableStateOf(false) }
    val env by
        produceState(
            initialValue = RuntimeEnv.basic(),
            key1 = context.applicationContext,
        ) {
            value = RuntimeEnv.resolve(context.applicationContext)
        }

    SettingsPage(
        title = stringResource(R.string.about_title),
        onBack = onBack,
        listState = listState,
        compactTopBar = os4,
        titleAlpha = ((scrollProgress - 0.35f) / 0.65f).coerceIn(0f, 1f),
        background = {
            GyBgEffect(
                playing = lifecycleState.isAtLeast(Lifecycle.State.RESUMED) &&
                    visible && scrollProgress < 1f,
                bgModifier = if (blur) Modifier.layerBackdrop(backdrop) else Modifier,
                alpha = { 1f - scrollProgress },
            )
        },
        foreground = { padding ->
            // Foreground keeps the interactive logo above the scroll hit layer,
            // but it must never draw over the first preference card.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        // Read list position in the draw phase so a fling cannot
                        // paint a stale foreground boundary between recompositions.
                        val firstCardTopPx =
                            if (listState.firstVisibleItemIndex == 0) {
                                (padding.calculateTopPadding() + spacerHeight).toPx() -
                                    listState.firstVisibleItemScrollOffset
                            } else {
                                0f
                            }
                        clipRect(bottom = firstCardTopPx.coerceIn(0f, size.height)) {
                            this@drawWithContent.drawContent()
                        }
                    },
            ) {
                AboutHero(
                    topPadding = padding.calculateTopPadding(),
                    backdrop = if (blur) backdrop else null,
                    logoBlend = logoBlend,
                    motion = AboutHeroMotion(
                        active = visible,
                        versionProgress = collapse.first,
                        nameProgress = collapse.second,
                        iconProgress = collapse.third,
                    ),
                    measurements = AboutHeroMeasurements(
                        onHeight = { if (heroHeight != it) heroHeight = it },
                        onIconBottom = {
                            if (listState.firstVisibleItemIndex == 0 &&
                                listState.firstVisibleItemScrollOffset == 0 && iconBottom != it
                            ) iconBottom = it
                        },
                        onNameBottom = {
                            if (listState.firstVisibleItemIndex == 0 &&
                                listState.firstVisibleItemScrollOffset == 0 && nameBottom != it
                            ) nameBottom = it
                        },
                        onVersionBottom = {
                            if (listState.firstVisibleItemIndex == 0 &&
                                listState.firstVisibleItemScrollOffset == 0 && versionBottom != it
                            ) versionBottom = it
                        },
                    ),
                )
            }
        },
        overlay = {
            UiOverlayDialog(
                title = stringResource(R.string.about_repo_unavailable_title),
                summary = stringResource(R.string.about_repo_unavailable_summary),
                show = showRepoUnavailable,
                onDismissRequest = { showRepoUnavailable = false },
            ) {
                UiDialogButton(
                    text = stringResource(R.string.confirm),
                    modifier = Modifier.fillMaxWidth(),
                    role = GlassDialogButtonRole.Primary,
                    onClick = { showRepoUnavailable = false },
                )
            }
        },
    ) {
        item(key = "logoSpacer") {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(spacerHeight)
                    .onGloballyPositioned { coords ->
                        if (listState.firstVisibleItemIndex == 0 &&
                            listState.firstVisibleItemScrollOffset == 0
                        ) {
                            val bottom = coords.positionInWindow().y + coords.size.height
                            if (spacerBottom != bottom) spacerBottom = bottom
                        }
                    },
            )
        }
        item(key = "aboutContent") {
            Column(Modifier.fillMaxWidth().heightIn(min = contentMinHeight)) {
                AboutLinksCard(
                    onOpenProject = {
                        if (BuildConfig.REPO_LINK_ENABLED) {
                            uriHandler.openUri(ABOUT_PROJECT_URL)
                        } else {
                            showRepoUnavailable = true
                        }
                    },
                    onOpenTerms = onOpenTerms,
                    onOpenPrivacy = onOpenPrivacy,
                    onOpenLicense = {
                        uriHandler.openUri(
                            if (BuildConfig.REPO_LINK_ENABLED) ABOUT_LICENSE_URL else GPL_LICENSE_URL,
                        )
                    },
                    onOpenThirdParty = onOpenThirdParty,
                )
                AboutEnvironmentCard(env, xposedStatus, onOpenScopes)
            }
        }
    }
}
