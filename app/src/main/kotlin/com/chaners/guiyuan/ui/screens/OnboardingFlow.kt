package com.chaners.guiyuan.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.annotation.DrawableRes
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import com.chaners.guiyuan.GyApplication
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.AppLang
import com.chaners.guiyuan.settings.UiStyle
import com.chaners.guiyuan.ui.LocalUiStyle
import com.chaners.guiyuan.ui.LocalGlassBackdrop
import com.chaners.guiyuan.ui.icons.Os4GlassRefresh
import com.chaners.guiyuan.ui.components.MiuixBlurredTopBar
import com.chaners.guiyuan.ui.components.UiTopBarNavigationIcon
import com.chaners.guiyuan.ui.components.rememberTopBarBackdrop
import com.chaners.guiyuan.ui.components.topBarBackdropSource
import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.transition.navGraphicsTransition
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.glass.GlassIconButton
import com.chaners.guiyuan.system.RootShell
import com.chaners.guiyuan.system.ScopeRestart
import com.chaners.guiyuan.system.XposedStatus
import com.chaners.guiyuan.ui.components.UiOverlayBottomSheet
import com.chaners.guiyuan.ui.components.UiOverlayDialog
import com.chaners.guiyuan.ui.components.UiDialogButton
import com.chaners.guiyuan.ui.components.dialogActionGap
import com.chaners.guiyuan.ui.screens.about.BgEffectBackground
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.icon.extended.Help
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole

// Guide steps are peers, not push-overlay destinations. Share MIUIX's motion and
// gesture driver while both pages translate the entire width in opposite directions.
private val guideStepTransition = navGraphicsTransition { scope ->
    val offset = (-scope.relativeDepth).coerceIn(-1f, 1f)
    val direction = if (scope.layoutDirection == LayoutDirection.Rtl) -1f else 1f
    translationX = (offset * direction * scope.layoutSize.width.toFloat()).fastRoundToInt().toFloat()
}

@Serializable
private sealed interface GuidePage : NavKey {
    @Serializable data object Welcome : GuidePage
    @Serializable data class Stage(val number: Int) : GuidePage
    @Serializable data object Complete : GuidePage
}

private enum class PermissionWarningPhase {
    Hidden, Visible, Dismissing, Advancing,
}

// The guide owns only temporary navigation state; existing preferences and
// framework services remain the authoritative writers for settings and scope.
@Composable
internal fun OnboardingFlow(
    lang: AppLang,
    onLangChange: (AppLang) -> Unit,
    iconHidden: Boolean,
    onIconHiddenChange: (Boolean) -> Unit,
    swipeBackEnabled: Boolean,
    onComplete: () -> Unit,
    onExit: (() -> Unit)? = null,
) {
    val context = LocalContext.current.applicationContext
    val app = remember(context) { context as GyApplication }
    val framework by app.xposedStatus.collectAsState()
    val scopeBusy by app.scopeRequestInFlight.collectAsState(initial = app.scopeRequestActive)
    val reloadBusy by app.hotReloadInFlight.collectAsState(initial = app.hotReloadActive)
    val restartState by ScopeRestart.state.collectAsState()
    val restartBusy = restartState is ScopeRestart.State.Running
    val connected = framework as? XposedStatus.Connected
    val sysUiInScope = connected?.sysUiInScope == true
    val canRequestScope = connected != null && connected.apiVersion >= 102 &&
        !scopeBusy && !reloadBusy && !restartBusy
    val swipeBackDirection = when {
        !swipeBackEnabled -> NavSwipeDirection.None
        LocalLayoutDirection.current == LayoutDirection.Ltr -> NavSwipeDirection.LeftToRight
        else -> NavSwipeDirection.RightToLeft
    }

    val pages = rememberNavBackStack<GuidePage>(GuidePage.Welcome)
    val step = when (val current = pages.lastOrNull()) {
        GuidePage.Welcome -> 0
        is GuidePage.Stage -> current.number
        GuidePage.Complete -> 4
        else -> 0 // NavBackStack stores NavKey, not GuidePage.
    }
    var permissionWarning by rememberSaveable { mutableStateOf(PermissionWarningPhase.Hidden) }
    fun advance() {
        val next = when (step) {
            0, 1, 2 -> GuidePage.Stage(step + 1)
            3 -> GuidePage.Complete
            else -> {
                onComplete()
                return
            }
        }
        // MIUIX rejects duplicate content keys. Guard fast repeated taps before
        // the new page has had a chance to recompose.
        if (next !in pages) pages.add(next)
    }
    var openDocument by rememberSaveable { mutableStateOf<AgreementDocument?>(null) }
    var documentVisible by rememberSaveable { mutableStateOf(false) }
    var policyAccepted by rememberSaveable { mutableStateOf(false) }
    var scopeRequestFailed by remember { mutableStateOf(false) }
    var rootChecking by remember { mutableStateOf(false) }
    var rootAccess by remember { mutableStateOf<Boolean?>(null) }
    var rootRefresh by remember { mutableIntStateOf(0) }
    var expandedPermission by rememberSaveable { mutableIntStateOf(0) }
    fun back() {
        // Let the active MIUIX overlay own Back until its dismissal finishes.
        if (openDocument != null || permissionWarning != PermissionWarningPhase.Hidden) return
        if (pages.size > 1) pages.removeLastOrNull() else onExit?.invoke()
    }
    fun refreshPermissionStatus() {
        app.refreshXposedStatus()
        rootRefresh++
    }
    val entryState = rememberAppSettingsEntryState(runtimeBusy = false)

    val activityLifecycle = LocalLifecycleOwner.current.lifecycle
    // MainActivity refreshes framework status on resume. Returning from an
    // external manager also retries Root without adding a polling loop.
    DisposableEffect(activityLifecycle, step) {
        if (step == 1) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) rootRefresh++
            }
            activityLifecycle.addObserver(observer)
            onDispose { activityLifecycle.removeObserver(observer) }
        } else {
            onDispose {}
        }
    }
    LaunchedEffect(step) {
        if (step == 1) app.refreshXposedStatus()
    }
    // Leaving the step cancels RootShell.execute and its temporary su process.
    LaunchedEffect(step, rootRefresh) {
        if (step == 1) {
            rootChecking = true
            try {
                val result = RootShell.execute(context, "id -u", 15L)
                rootAccess = result.isSuccess && result.output.trim() == "0"
            } finally {
                rootChecking = false
            }
        }
    }
    // MIUIX's BottomSheetContentLayout owns Back/drag dismissal and predictive motion.

    NavDisplay(
        backStack = pages,
        onBack = ::back,
        transition = guideStepTransition,
        effects = NavDisplayEffects(
            enableCornerClip = false,
            dimAmount = 0f,
            blockInputDuringTransition = true,
            backdropColor = MiuixTheme.colorScheme.surface,
        ),
    ) {
        entry<GuidePage.Welcome> {
            GuiyuanBrandGuide(completed = false, onNext = ::advance)
        }
        entry<GuidePage.Complete>(swipeDismiss = swipeBackDirection) {
            GuiyuanBrandGuide(completed = true, onNext = onComplete)
        }
        entry<GuidePage.Stage>(swipeDismiss = if (openDocument == null) swipeBackDirection else NavSwipeDirection.None) { page ->
        val step = page.number
        val title = when (step) {
            1 -> stringResource(R.string.guide_permissions_title)
            2 -> stringResource(R.string.guide_privacy_title)
            else -> stringResource(R.string.guide_basic_title)
        }
        val illustration = when (step) {
            1 -> R.drawable.ic_material_symbol_security
            2 -> R.drawable.ic_material_symbol_balance
            else -> R.drawable.ic_material_symbol_settings
        }
        val scrollBehavior = MiuixScrollBehavior()
        val scroll = rememberScrollState()
        val importantScroll = rememberScrollState()
        val topBarBackdrop = rememberTopBarBackdrop()
        val os4 = LocalUiStyle.current == UiStyle.OS4
        val refreshAction: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
            if (step == 1) {
                val label = stringResource(R.string.guide_refresh_status)
                TooltipBox(text = label) {
                    if (os4) {
                        GlassIconButton(onClick = ::refreshPermissionStatus) {
                            Icon(
                                imageVector = Os4GlassRefresh,
                                contentDescription = label,
                            )
                        }
                    } else {
                        IconButton(onClick = ::refreshPermissionStatus) {
                            Icon(
                                imageVector = MiuixIcons.Regular.Refresh,
                                contentDescription = label,
                            )
                        }
                    }
                }
            }
        }
        // Match the in-app Settings top bar instead of mixing a glass back control
        // into the OS3 bar. The guide has its own content heading, hence compact.
        CompositionLocalProvider(LocalGlassBackdrop provides topBarBackdrop) {
            Scaffold(
                topBar = {
                    if (os4) {
                        GlassTopAppBar(
                            title = "",
                            isContentScrolled = step != 2 && scroll.value > 0,
                            backdrop = topBarBackdrop,
                            scrollBehavior = scrollBehavior,
                            compact = true,
                            navigationIcon = { UiTopBarNavigationIcon(onBack = ::back) },
                            actions = refreshAction,
                            unclippedNavigationIcon = true,
                            unclippedActions = step == 1,
                        )
                    } else {
                        MiuixBlurredTopBar(
                            backdrop = topBarBackdrop,
                            scrollBehavior = scrollBehavior,
                        ) { barColor ->
                            SmallTopAppBar(
                                title = "",
                                color = barColor,
                                scrollBehavior = scrollBehavior,
                                navigationIcon = { UiTopBarNavigationIcon(onBack = ::back) },
                                actions = refreshAction,
                            )
                        }
                    }
                },
            ) { scaffoldPadding ->
            Column(
                modifier = Modifier.fillMaxSize().topBarBackdropSource(topBarBackdrop)
                    .padding(scaffoldPadding).padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().then(
                        if (step == 2) Modifier
                        else Modifier.nestedScroll(scrollBehavior.nestedScrollConnection).verticalScroll(scroll),
                    ),
                ) {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().height(82.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(illustration),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MiuixTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = title,
                            modifier = Modifier.semantics { heading() },
                            style = MiuixTheme.textStyles.title1.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center,
                            color = MiuixTheme.colorScheme.onBackground,
                        )
                    }
                    Spacer(Modifier.height(30.dp))
                    when (step) {
                        1 -> {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                GuidePermissionItem(
                                    iconRes = R.drawable.ic_material_symbol_extension,
                                    title = stringResource(R.string.guide_framework),
                                    purpose = stringResource(R.string.guide_framework_purpose),
                                    status = when (framework) {
                                        XposedStatus.Checking -> stringResource(R.string.guide_framework_checking)
                                        XposedStatus.FrameworkUnavailable -> stringResource(R.string.guide_framework_offline)
                                        XposedStatus.QueryUnavailable -> stringResource(R.string.guide_framework_read_failed)
                                        is XposedStatus.Connected -> stringResource(R.string.guide_framework_connected)
                                    },
                                    checked = connected != null,
                                    expanded = expandedPermission == 1,
                                    onToggleDetails = {
                                        expandedPermission = if (expandedPermission == 1) 0 else 1
                                    },
                                    guidance = when (framework) {
                                        XposedStatus.FrameworkUnavailable -> stringResource(R.string.guide_framework_help)
                                        XposedStatus.QueryUnavailable -> stringResource(R.string.guide_framework_read_help)
                                        else -> null
                                    },
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Card(modifier = Modifier.fillMaxWidth()) {
                                GuidePermissionItem(
                                    iconRes = R.drawable.ic_material_symbol_target,
                                    title = stringResource(R.string.guide_sysui_scope),
                                    purpose = stringResource(R.string.guide_sysui_scope_purpose),
                                    status = when {
                                        sysUiInScope -> stringResource(R.string.runtime_scope_authorized)
                                        framework == XposedStatus.Checking -> stringResource(R.string.guide_framework_checking)
                                        framework == XposedStatus.QueryUnavailable -> stringResource(R.string.guide_scope_read_failed)
                                        connected == null -> stringResource(R.string.guide_scope_wait_framework)
                                        scopeBusy -> stringResource(R.string.guide_scope_requesting)
                                        else -> stringResource(R.string.runtime_scope_disabled)
                                    },
                                    checked = sysUiInScope,
                                    expanded = expandedPermission == 2,
                                    onToggleDetails = {
                                        expandedPermission = if (expandedPermission == 2) 0 else 2
                                    },
                                    guidance = when {
                                        sysUiInScope -> null
                                        framework == XposedStatus.QueryUnavailable -> stringResource(R.string.guide_framework_read_help)
                                        connected == null -> stringResource(R.string.guide_scope_help_offline)
                                        connected.apiVersion < 102 -> stringResource(R.string.guide_scope_help_api)
                                        !canRequestScope -> stringResource(R.string.guide_scope_help_busy)
                                        else -> stringResource(R.string.guide_scope_help_request)
                                    },
                                )
                                if (!sysUiInScope) {
                                    TextButton(
                                        text = stringResource(R.string.guide_scope_request_action),
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                                        colors = ButtonDefaults.textButtonColorsPrimary(),
                                        enabled = canRequestScope,
                                        onClick = {
                                            scopeRequestFailed = false
                                            if (!app.requestSysUiScope { approved ->
                                                    scopeRequestFailed = !approved
                                                }) scopeRequestFailed = true
                                        },
                                    )
                                }
                            }
                            SmallTitle(text = stringResource(R.string.guide_permission_later))
                            Card(modifier = Modifier.fillMaxWidth()) {
                                GuidePermissionItem(
                                    iconRes = R.drawable.ic_material_symbol_admin_panel_settings,
                                    title = stringResource(R.string.guide_root_title),
                                    purpose = stringResource(R.string.guide_root_purpose),
                                    status = when {
                                        rootChecking -> stringResource(R.string.guide_root_checking)
                                        rootAccess == true -> stringResource(R.string.guide_root_granted)
                                        rootAccess == false -> stringResource(R.string.guide_root_unavailable)
                                        else -> stringResource(R.string.guide_root_not_checked)
                                    },
                                    checked = rootAccess == true,
                                    expanded = expandedPermission == 3,
                                    onToggleDetails = {
                                        expandedPermission = if (expandedPermission == 3) 0 else 3
                                    },
                                    guidance = if (rootAccess == false) stringResource(R.string.guide_root_help) else null,
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
                        2 -> {
                            Column(
                                modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth()
                                    .weight(1f).padding(horizontal = 4.dp),
                            ) {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    BasicComponent(
                                        title = stringResource(R.string.terms_title),
                                        summary = stringResource(R.string.about_terms_summary),
                                        startAction = {
                                            SemanticLeadingIcon(iconRes = R.drawable.ic_material_symbol_contract)
                                        },
                                        endActions = {
                                            Icon(
                                                imageVector = MiuixIcons.Regular.ChevronForward,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            )
                                        },
                                        onClick = {
                                            openDocument = AgreementDocument.Terms
                                            documentVisible = true
                                        },
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    BasicComponent(
                                        title = stringResource(R.string.privacy_title),
                                        summary = stringResource(R.string.about_privacy_summary),
                                        startAction = {
                                            SemanticLeadingIcon(iconRes = R.drawable.ic_material_symbol_policy)
                                        },
                                        endActions = {
                                            Icon(
                                                imageVector = MiuixIcons.Regular.ChevronForward,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            )
                                        },
                                        onClick = {
                                            openDocument = AgreementDocument.Privacy
                                            documentVisible = true
                                        },
                                    )
                                }
                                Spacer(Modifier.height(24.dp))
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                                Spacer(Modifier.height(20.dp))
                                Text(
                                    text = stringResource(R.string.guide_important_note),
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                        .semantics { heading() },
                                    style = MiuixTheme.textStyles.headline1.copy(fontWeight = FontWeight.SemiBold),
                                    color = MiuixTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(16.dp))
                                Column(
                                    modifier = Modifier.weight(1f).fillMaxWidth()
                                        .verticalScroll(importantScroll)
                                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    listOf(
                                        R.string.guide_privacy_brief,
                                        R.string.guide_privacy_note_scope,
                                        R.string.guide_privacy_note_root,
                                        R.string.guide_privacy_note_diagnostics,
                                        R.string.guide_privacy_note_sharing,
                                    ).forEach { note ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Text(
                                                text = "•",
                                                style = MiuixTheme.textStyles.body2,
                                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            )
                                            Text(
                                                text = stringResource(note),
                                                modifier = Modifier.weight(1f),
                                                style = MiuixTheme.textStyles.body2.copy(lineHeight = 1.5f.em),
                                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        3 -> {
                            SmallTitle(text = stringResource(R.string.section_app))
                            Card(modifier = Modifier.fillMaxWidth()) {
                                AppSettingsRows(
                                    lang = lang,
                                    iconHidden = iconHidden,
                                    onLangChange = onLangChange,
                                    onIconHiddenChange = onIconHiddenChange,
                                    entryState = entryState,
                                )
                            }
                        }
                    }
                    if (step != 2) Spacer(Modifier.height(24.dp))
                }
                if (step == 2) {
                    Row(
                        modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth()
                            .toggleable(
                                value = policyAccepted,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Checkbox,
                                onValueChange = { policyAccepted = it },
                            )
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Checkbox(
                            state = ToggleableState(policyAccepted),
                            onClick = null,
                        )
                        val acknowledgment = stringResource(R.string.guide_privacy_ack)
                        val terms = stringResource(R.string.guide_terms_link_text)
                        val privacy = stringResource(R.string.guide_policy_link_text)
                        val linkedAcknowledgment = buildAnnotatedString {
                            append(acknowledgment)
                            listOf(
                                Triple(terms, AgreementDocument.Terms, "terms"),
                                Triple(privacy, AgreementDocument.Privacy, "privacy"),
                            ).forEach { (label, document, tag) ->
                                val start = acknowledgment.indexOf(label)
                                if (start >= 0) {
                                    addLink(
                                        LinkAnnotation.Clickable(
                                            tag = tag,
                                            linkInteractionListener = LinkInteractionListener {
                                                openDocument = document
                                                documentVisible = true
                                            },
                                        ),
                                        start,
                                        start + label.length,
                                    )
                                }
                            }
                        }
                        Text(
                            text = linkedAcknowledgment,
                            modifier = Modifier.weight(1f),
                            style = MiuixTheme.textStyles.body2.copy(lineHeight = 1.4f.em),
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                TextButton(
                    text = stringResource(R.string.guide_continue),
                    modifier = Modifier.widthIn(max = 336.dp).fillMaxWidth().heightIn(min = 50.dp),
                    cornerRadius = 16.dp,
                    textStyle = MiuixTheme.textStyles.button,
                    colors = if (step == 1 && !sysUiInScope) {
                        ButtonDefaults.textButtonColors()
                    } else {
                        ButtonDefaults.textButtonColorsPrimary()
                    },
                    enabled = step != 2 || policyAccepted,
                    onClick = {
                        if (step == 1 && !sysUiInScope) {
                            permissionWarning = PermissionWarningPhase.Visible
                        } else {
                            advance()
                        }
                    },
                )
                Spacer(Modifier.height(44.dp))
            }
            // Navigation keeps covered entries alive. Only the top stage hosts popups.
            if (pages.lastOrNull() == page) {
                when (step) {
                    1 -> UiOverlayDialog(
                        show = permissionWarning == PermissionWarningPhase.Visible,
                        title = stringResource(R.string.guide_permission_warning_title),
                        summary = stringResource(R.string.guide_permission_warning_summary),
                        onDismissRequest = {
                            permissionWarning = PermissionWarningPhase.Dismissing
                        },
                        onDismissFinished = {
                            val proceed = permissionWarning == PermissionWarningPhase.Advancing
                            permissionWarning = PermissionWarningPhase.Hidden
                            if (proceed) advance()
                        },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            UiDialogButton(
                                text = stringResource(R.string.guide_permission_return),
                                modifier = Modifier.weight(1f),
                                role = GlassDialogButtonRole.Primary,
                                onClick = {
                                    permissionWarning = PermissionWarningPhase.Dismissing
                                },
                            )
                            Spacer(Modifier.width(dialogActionGap()))
                            UiDialogButton(
                                text = stringResource(R.string.guide_permission_continue),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    permissionWarning = PermissionWarningPhase.Advancing
                                },
                            )
                        }
                    }
                    2 -> UiOverlayBottomSheet(
                        show = documentVisible,
                        modifier = Modifier.fillMaxHeight(0.85f),
                        title = stringResource((openDocument ?: AgreementDocument.Privacy).titleRes()),
                        backgroundColor = MiuixTheme.colorScheme.background,
                        insideMargin = DpSize(0.dp, 0.dp),
                        onDismissRequest = { documentVisible = false },
                        onDismissFinished = {
                            if (!documentVisible) openDocument = null
                        },
                    ) {
                        openDocument?.let { AgreementSheetBody(it) }
                    }
                    3 -> AppSettingsEntryDialogs(entryState)
                    else -> Unit
                }
            }
        }
        }
        }
    }
}

@Composable
private fun GuiyuanBrandGuide(completed: Boolean, onNext: () -> Unit) {
    // The MIUIX navigation entry lifecycle pauses covered pages below RESUMED.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> resumed = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> resumed = false
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val surface = MiuixTheme.colorScheme.surface
    val accent = MiuixTheme.colorScheme.primary
    val actionColors = guideActionColors()
    val arrowRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // The established About shader owns the mixed colors. Keep a bounded
    // theme-color fallback for devices without RuntimeShader support.
    val fallback = Brush.linearGradient(
        listOf(
            lerp(surface, accent, 0.25f),
            lerp(surface, accent, 0.06f),
            lerp(surface, accent, 0.19f),
        ),
    )
    val blurSupported = remember { isRuntimeShaderSupported() }
    val backdrop = rememberLayerBackdrop()
    val blend = rememberBrandLogoBlend()
    val identityMaterial = if (!blurSupported) Modifier else Modifier.textureBlur(
        backdrop = backdrop,
        shape = RoundedCornerShape(0.dp),
        blurRadius = 150f,
        colors = BlurColors(blendColors = blend),
        contentBlendMode = BlendMode.DstIn,
    )
    val wave = remember { Animatable(0f) }
    var wavePhase by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val diameter = with(density) { 100.dp.toPx() }
    val radius = diameter / 2f
    val glint = if (surface.luminance() < 0.5f) {
        lerp(accent, Color.Black, 0.35f)
    } else {
        lerp(accent, Color.White, 0.70f)
    }
    var titleDistance by remember { mutableFloatStateOf(0f) }
    var completionDistance by remember { mutableFloatStateOf(0f) }
    Box(modifier = Modifier.fillMaxSize().background(fallback)) {
        BgEffectBackground(
            dynamicBackground = resumed,
            modifier = Modifier.fillMaxSize(),
            bgModifier = if (blurSupported) Modifier.layerBackdrop(backdrop) else Modifier,
            isFullSize = true,
        ) {}
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GuiyuanAnimatedIdentityMark(
                        markSize = 100.dp,
                        wave = wave,
                        wavePhase = wavePhase,
                        onWaveRelease = { wavePhase = it },
                        interactive = true,
                        active = resumed,
                        effectModifier = identityMaterial,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.product_name),
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                val measured = coords.positionInParent().y +
                                    coords.size.height / 2f - radius
                                if (titleDistance != measured) titleDistance = measured
                            }
                            .graphicsLayer {
                                val lift = waveLift(wave.value, titleDistance, diameter, wavePhase)
                                compositingStrategy = if (wave.isRunning) {
                                    CompositingStrategy.Offscreen
                                } else {
                                    CompositingStrategy.Auto
                                }
                                scaleX = 1f + lift * 0.055f
                                scaleY = 1f + lift * 0.035f
                                translationY = -lift * 7.dp.toPx()
                            }
                            .waveGlint(wave, titleDistance, diameter, wavePhase, glint, accent)
                            .then(identityMaterial),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = MiuixTheme.colorScheme.onBackground,
                    )
                    if (completed) {
                        Spacer(Modifier.height(36.dp))
                        Text(
                            text = stringResource(R.string.guide_done_title),
                            modifier = Modifier
                                .onGloballyPositioned { coords ->
                                    val measured = coords.positionInParent().y +
                                        coords.size.height / 2f - radius
                                    if (completionDistance != measured) completionDistance = measured
                                }
                                .graphicsLayer {
                                    val lift = waveLift(wave.value, completionDistance, diameter, wavePhase)
                                    compositingStrategy = if (wave.isRunning) {
                                        CompositingStrategy.Offscreen
                                    } else {
                                        CompositingStrategy.Auto
                                    }
                                    scaleX = 1f + lift * 0.045f
                                    scaleY = 1f + lift * 0.025f
                                    translationY = -lift * 5.dp.toPx()
                                }
                                .waveGlint(wave, completionDistance, diameter, wavePhase, glint, accent),
                            fontSize = 24.sp,
                            textAlign = TextAlign.Center,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }
            if (completed) {
                Box(Modifier.widthIn(max = 336.dp).fillMaxWidth().heightIn(min = 50.dp)) {
                    GuideBrandedActionBackdrop(
                        modifier = Modifier.matchParentSize(),
                        cornerRadius = 16.dp,
                        container = actionColors.container,
                        material = identityMaterial,
                        materialAlpha = actionColors.materialAlpha,
                        enabled = blurSupported,
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                        cornerRadius = 16.dp,
                        colors = ButtonDefaults.buttonColors(
                            color = Color.Transparent,
                            contentColor = actionColors.content,
                        ),
                        onClick = onNext,
                    ) {
                        Text(
                            text = stringResource(R.string.guide_start),
                            style = MiuixTheme.textStyles.button,
                        )
                    }
                }
                Spacer(Modifier.height(44.dp))
            } else {
                val continueLabel = stringResource(R.string.guide_continue)
                TooltipBox(text = continueLabel) {
                    Box(Modifier.size(70.dp)) {
                        GuideBrandedActionBackdrop(
                            modifier = Modifier.matchParentSize(),
                            cornerRadius = 35.dp,
                            container = actionColors.container,
                            material = identityMaterial,
                            materialAlpha = actionColors.materialAlpha,
                            enabled = blurSupported,
                        )
                        IconButton(
                            onClick = onNext,
                            modifier = Modifier.size(70.dp).semantics { contentDescription = continueLabel },
                            backgroundColor = Color.Transparent,
                            cornerRadius = 35.dp,
                            minWidth = 70.dp,
                            minHeight = 70.dp,
                        ) {
                            Canvas(Modifier.width(31.dp).height(22.dp)) {
                                val middle = size.height / 2f
                                val tip = size.width * if (arrowRtl) 0.04f else 0.96f
                                val tail = size.width * if (arrowRtl) 0.96f else 0.04f
                                val wing = size.height * 0.42f
                                val base = tip + if (arrowRtl) wing else -wing
                                drawLine(actionColors.content, Offset(tail, middle), Offset(tip, middle), strokeWidth = 2.6.dp.toPx(), cap = StrokeCap.Round)
                                drawLine(actionColors.content, Offset(base, middle - wing), Offset(tip, middle), strokeWidth = 2.6.dp.toPx(), cap = StrokeCap.Round)
                                drawLine(actionColors.content, Offset(base, middle + wing), Offset(tip, middle), strokeWidth = 2.6.dp.toPx(), cap = StrokeCap.Round)
                            }
                        }
                    }
                }
                Spacer(Modifier.weight(0.28f))
            }
        }
    }
}

@Composable
private fun GuideBrandedActionBackdrop(
    modifier: Modifier,
    cornerRadius: Dp,
    container: Color,
    material: Modifier,
    materialAlpha: Float,
    enabled: Boolean,
) {
    Box(modifier) {
        Box(Modifier.matchParentSize().squircleSurface(color = container, cornerRadius = cornerRadius))
        if (enabled && materialAlpha > 0f) {
            Box(
                Modifier.matchParentSize().then(material)
                    .squircleSurface(color = Color.White.copy(alpha = materialAlpha), cornerRadius = cornerRadius),
            )
        }
    }
}

internal data class GuideActionColors(val container: Color, val content: Color, val materialAlpha: Float)

private fun guideContrastRatio(foreground: Color, background: Color): Float {
    val light = maxOf(foreground.luminance(), background.luminance())
    val dark = minOf(foreground.luminance(), background.luminance())
    return (light + 0.05f) / (dark + 0.05f)
}

internal fun resolveGuideActionColors(surface: Color, onSurface: Color, accent: Color): GuideActionColors {
    val tint = lerp(onSurface, accent, 0.10f)
    val baseOpacities = listOf(0.60f, 0.68f, 0.76f, 0.84f, 0.92f, 1f)
    // The blended backdrop is not a predictable solid color. Bound both possible
    // luminance extremes so the plain button text/arrow stays readable.
    for (materialAlpha in listOf(0.30f, 0.26f, 0.22f, 0.18f, 0.14f, 0.10f, 0f)) {
        for (baseOpacity in baseOpacities) {
            val container = tint.copy(alpha = baseOpacity).compositeOver(surface)
            val darkExtreme = Color.Black.copy(alpha = materialAlpha).compositeOver(container)
            val lightExtreme = Color.White.copy(alpha = materialAlpha).compositeOver(container)
            if (minOf(
                    guideContrastRatio(surface, darkExtreme),
                    guideContrastRatio(surface, lightExtreme),
                ) >= 4.5f
            ) {
                return GuideActionColors(container, surface, materialAlpha)
            }
        }
    }
    // Preserve the theme's solid foreground contrast if no blend is safe.
    return GuideActionColors(onSurface, surface, 0f)
}

@Composable
private fun guideActionColors(): GuideActionColors {
    val surface = MiuixTheme.colorScheme.surface
    val onSurface = MiuixTheme.colorScheme.onSurface
    val accent = MiuixTheme.colorScheme.primary
    return remember(surface, onSurface, accent) {
        resolveGuideActionColors(surface, onSurface, accent)
    }
}

@Composable
private fun GuidePermissionItem(
    @DrawableRes iconRes: Int,
    title: String,
    status: String,
    purpose: String,
    checked: Boolean,
    expanded: Boolean,
    onToggleDetails: () -> Unit,
    guidance: String? = null,
) {
    val detailsLabel = stringResource(R.string.guide_permission_details)
    BasicComponent(
        startAction = { SemanticLeadingIcon(iconRes = iconRes) },
        endActions = {
            if (checked) {
                Icon(
                    imageVector = MiuixIcons.Basic.Check,
                    contentDescription = status,
                    modifier = Modifier.size(26.dp),
                    tint = MiuixTheme.colorScheme.primary,
                )
            } else {
                Text(
                    text = status,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                modifier = Modifier.weight(1f, fill = false),
                style = MiuixTheme.textStyles.headline1.copy(fontWeight = FontWeight.Medium),
                color = MiuixTheme.colorScheme.onBackground,
            )
            TooltipBox(text = detailsLabel) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(
                        onClick = onToggleDetails,
                        modifier = Modifier.size(32.dp),
                        minWidth = 32.dp,
                        minHeight = 32.dp,
                        cornerRadius = 16.dp,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Regular.Help,
                            contentDescription = detailsLabel,
                            modifier = Modifier.size(20.dp),
                            tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        )
                    }
                }
            }
        }
    }
    if (expanded) {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    }
    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(expandFrom = Alignment.Top),
        exit = shrinkVertically(shrinkTowards = Alignment.Top),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
            ) {
                Text(
                    text = stringResource(R.string.guide_purpose_label),
                    style = MiuixTheme.textStyles.subtitle,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = purpose,
                    style = MiuixTheme.textStyles.body2.copy(lineHeight = 1.4f.em),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                if (guidance != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.guide_guidance_label),
                        style = MiuixTheme.textStyles.subtitle,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = guidance,
                        style = MiuixTheme.textStyles.body2.copy(lineHeight = 1.4f.em),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }
    }
}
