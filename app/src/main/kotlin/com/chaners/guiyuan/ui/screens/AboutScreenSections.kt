package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.BuildConfig
import com.chaners.guiyuan.R
import com.chaners.guiyuan.system.RuntimeEnv
import com.chaners.guiyuan.system.XposedStatus
import com.chaners.guiyuan.ui.components.UiDialogButton
import com.chaners.guiyuan.ui.components.UiOverlayDialog
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.LayerBackdrop

private const val ABOUT_PROJECT_URL = "https://github.com/CHS-Haple/Guiyuan"
private const val ABOUT_LICENSE_URL = "https://github.com/CHS-Haple/Guiyuan/blob/main/LICENSE"
private const val GPL_LICENSE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"

internal data class AboutPageActions(
    val onOpenThirdParty: () -> Unit,
    val onOpenPrivacy: () -> Unit,
    val onOpenTerms: () -> Unit,
    val onOpenScopes: () -> Unit,
)

@Composable
internal fun AboutHeroForeground(
    scroll: AboutScrollState,
    padding: PaddingValues,
    blur: Boolean,
    backdrop: LayerBackdrop,
    logoBlend: List<BlendColorEntry>,
) {
    val list = scroll.list
    val layout = scroll.layout
    Box(
        modifier = Modifier.fillMaxSize().drawWithContent {
            // Read the list position in the draw phase, including flings.
            val firstCardTopPx = if (list.firstVisibleItemIndex == 0) {
                (padding.calculateTopPadding() + layout.spacerHeight).toPx() -
                    list.firstVisibleItemScrollOffset
            } else 0f
            clipRect(bottom = firstCardTopPx.coerceIn(0f, size.height)) {
                this@drawWithContent.drawContent()
            }
        },
    ) {
        AboutHero(
            topPadding = padding.calculateTopPadding(),
            backdrop = backdrop.takeIf { blur },
            logoBlend = logoBlend,
            motion = AboutHeroMotion(
                active = scroll.visible,
                versionProgress = scroll.collapse.first,
                nameProgress = scroll.collapse.second,
                iconProgress = scroll.collapse.third,
            ),
            measurements = AboutHeroMeasurements(
                onHeight = { if (layout.height != it) layout.height = it },
                onIconBottom = {
                    if (layout.atRest(list) && layout.iconBottom != it) layout.iconBottom = it
                },
                onNameBottom = {
                    if (layout.atRest(list) && layout.nameBottom != it) layout.nameBottom = it
                },
                onVersionBottom = {
                    if (layout.atRest(list) && layout.versionBottom != it) layout.versionBottom = it
                },
            ),
        )
    }
}

internal fun LazyListScope.aboutPageItems(
    scroll: AboutScrollState,
    content: @Composable () -> Unit,
) {
    item(key = "logoSpacer") {
        Box(
            Modifier.fillMaxWidth().height(scroll.layout.spacerHeight)
                .onGloballyPositioned { coords ->
                    if (scroll.layout.atRest(scroll.list)) {
                        val bottom = coords.positionInWindow().y + coords.size.height
                        if (scroll.layout.spacerBottom != bottom) scroll.layout.spacerBottom = bottom
                    }
                },
        )
    }
    item(key = "aboutContent") {
        Column(Modifier.fillMaxWidth().heightIn(min = scroll.contentMinHeight)) {
            content()
        }
    }
}

@Composable
internal fun AboutPageContent(
    env: RuntimeEnv,
    xposedStatus: XposedStatus,
    actions: AboutPageActions,
    onRepoUnavailable: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    AboutLinksCard(
        onOpenProject = {
            if (BuildConfig.REPO_LINK_ENABLED) uriHandler.openUri(ABOUT_PROJECT_URL)
            else onRepoUnavailable()
        },
        onOpenTerms = actions.onOpenTerms,
        onOpenPrivacy = actions.onOpenPrivacy,
        onOpenLicense = {
            uriHandler.openUri(
                if (BuildConfig.REPO_LINK_ENABLED) ABOUT_LICENSE_URL else GPL_LICENSE_URL,
            )
        },
        onOpenThirdParty = actions.onOpenThirdParty,
    )
    AboutEnvironmentCard(env, xposedStatus, actions.onOpenScopes)
}

@Composable
internal fun AboutRepositoryUnavailableDialog(
    show: Boolean,
    onDismiss: () -> Unit,
) {
    UiOverlayDialog(
        title = stringResource(R.string.about_repo_unavailable_title),
        summary = stringResource(R.string.about_repo_unavailable_summary),
        show = show,
        onDismissRequest = onDismiss,
    ) {
        UiDialogButton(
            text = stringResource(R.string.confirm),
            modifier = Modifier.fillMaxWidth(),
            role = GlassDialogButtonRole.Primary,
            onClick = onDismiss,
        )
    }
}
