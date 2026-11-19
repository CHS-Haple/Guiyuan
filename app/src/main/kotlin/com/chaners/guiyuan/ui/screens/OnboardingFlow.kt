package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import android.widget.Toast
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.GyApplication
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.AppLang
import com.chaners.guiyuan.settings.OnboardingPrefs
import com.chaners.guiyuan.settings.UiStyle
import com.chaners.guiyuan.ui.LocalUiStyle
import com.chaners.guiyuan.ui.LocalTopBarProgressiveBlur
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
import top.yukonga.miuix.kmp.nav.core.navBackStackOf
import top.yukonga.miuix.kmp.nav.transition.navGraphicsTransition
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import com.chaners.guiyuan.ui.glass.GlassTopAppBar
import com.chaners.guiyuan.ui.glass.GlassIconButton
import com.chaners.guiyuan.system.RootShell
import com.chaners.guiyuan.system.ScopeRestart
import com.chaners.guiyuan.system.XposedStatus
import com.chaners.guiyuan.ui.components.UiOverlayBottomSheet
import com.chaners.guiyuan.ui.components.UiOverlayDialog
import com.chaners.guiyuan.ui.components.UiDialogButton
import com.chaners.guiyuan.ui.components.dialogActionGap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole

// Guide steps are peers, not push-overlay destinations. Share MIUIX's motion and
// gesture driver while both pages translate the entire width in opposite directions.
private val guideStepTransition = navGraphicsTransition { scope ->
    val offset = (-scope.relativeDepth).coerceIn(-1f, 1f)
    val direction = if (scope.layoutDirection == LayoutDirection.Rtl) -1f else 1f
    translationX = (offset * direction * scope.layoutSize.width.toFloat()).fastRoundToInt().toFloat()
}

// Page identity is unchanged; only the onboarding visit order is adjusted.
internal fun nextGuideStage(step: Int): Int? = when (step) {
    0 -> 2 // Present privacy and terms before any optional Root status check.
    2 -> 1
    1 -> 3
    3 -> 4
    else -> null
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
    agreementReviewOnly: Boolean = false,
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

    // An unfinished guide begins at its entry page on each new app session.
    // Save settings and agreement completion, not the partial page stack.
    val pages = remember(agreementReviewOnly) {
        navBackStackOf(
            if (agreementReviewOnly) GuidePage.Stage(2) else GuidePage.Welcome,
        )
    }
    val step = when (val current = pages.lastOrNull()) {
        GuidePage.Welcome -> 0
        is GuidePage.Stage -> current.number
        GuidePage.Complete -> 4
        else -> 0 // NavBackStack stores NavKey, not GuidePage.
    }
    var permissionWarning by remember { mutableStateOf(PermissionWarningPhase.Hidden) }
    fun advanceFrom(source: GuidePage) {
        // A retained page can invoke an older callback after navigation changes.
        // Only the page currently on top may advance the guide.
        if (pages.lastOrNull() != source) return
        val sourceStep = when (source) {
            GuidePage.Welcome -> 0
            is GuidePage.Stage -> source.number
            GuidePage.Complete -> return
        }
        val next = when (val nextStep = nextGuideStage(sourceStep)) {
            1, 2, 3 -> GuidePage.Stage(nextStep)
            4 -> GuidePage.Complete
            else -> return
        }
        if (next !in pages) pages.add(next)
    }
    val requiredReading = remember(context) { OnboardingPrefs.readingRequirements(context) }
    var termsViewed by remember { mutableStateOf(false) }
    var privacyViewed by remember { mutableStateOf(false) }
    var openDocument by remember { mutableStateOf<AgreementDocument?>(null) }
    var documentVisible by remember { mutableStateOf(false) }
    var documentDismissing by remember { mutableStateOf(false) }
    var policyAccepted by remember { mutableStateOf(false) }

    fun showAgreement(document: AgreementDocument) {
        if (documentVisible || documentDismissing) return
        when (document) {
            AgreementDocument.Terms -> termsViewed = true
            AgreementDocument.Privacy -> privacyViewed = true
        }
        openDocument = document
        documentVisible = true
    }

    fun requestAcknowledgment(checked: Boolean) {
        if (!checked) {
            policyAccepted = false
            return
        }
        val termsPending = requiredReading.terms && !termsViewed
        val privacyPending = requiredReading.privacy && !privacyViewed
        if (requiredReading.canAcknowledge(termsViewed, privacyViewed)) {
            policyAccepted = true
            return
        }
        val message = when {
            termsPending && privacyPending -> R.string.guide_read_both_first
            termsPending -> R.string.guide_read_terms_first
            else -> R.string.guide_read_privacy_first
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // Returning to this page clears consent, not document views.
    // NavDisplay retains the outgoing page through its transition.
    LaunchedEffect(step) {
        if (step == 2) policyAccepted = false
    }
    var scopeRequestFailed by remember { mutableStateOf(false) }
    var rootChecking by remember { mutableStateOf(false) }
    var rootAccess by remember { mutableStateOf<Boolean?>(null) }
    var rootRefresh by remember { mutableIntStateOf(0) }
    var expandedPermission by remember { mutableIntStateOf(0) }
    fun back() {
        // Let the active MIUIX overlay own Back until its dismissal finishes.
        if (documentVisible || documentDismissing || permissionWarning != PermissionWarningPhase.Hidden) return
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
            GuiyuanBrandGuide(
                completed = false,
                onNext = { advanceFrom(GuidePage.Welcome) },
            )
        }
        entry<GuidePage.Complete>(swipeDismiss = swipeBackDirection) {
            GuiyuanBrandGuide(
                completed = true,
                onNext = {
                    if (pages.lastOrNull() == GuidePage.Complete) onComplete()
                },
            )
        }
        entry<GuidePage.Stage>(
            swipeDismiss = if (documentVisible || documentDismissing) NavSwipeDirection.None else swipeBackDirection,
        ) { page ->
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
                            progressiveBlur = LocalTopBarProgressiveBlur.current,
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
                                GuideAgreementCard(
                                    titleRes = R.string.terms_title,
                                    summaryRes = R.string.about_terms_summary,
                                    iconRes = R.drawable.ic_material_symbol_contract,
                                    required = requiredReading.terms && !termsViewed,
                                    onClick = { showAgreement(AgreementDocument.Terms) },
                                )
                                Spacer(Modifier.height(12.dp))
                                GuideAgreementCard(
                                    titleRes = R.string.privacy_title,
                                    summaryRes = R.string.about_privacy_summary,
                                    iconRes = R.drawable.ic_material_symbol_policy,
                                    required = requiredReading.privacy && !privacyViewed,
                                    onClick = { showAgreement(AgreementDocument.Privacy) },
                                )
                                GuidePrivacyNotes(importantScroll)
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
                    GuidePrivacyAcknowledgment(
                        accepted = policyAccepted,
                        onCheckedChange = ::requestAcknowledgment,
                        onOpenDocument = ::showAgreement,
                    )
                }
                TextButton(
                    text = stringResource(
                        if (step == 2) R.string.guide_agree_continue else R.string.guide_continue,
                    ),
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
                        // A covered page's retained click handler must not act on the new top.
                        if (pages.lastOrNull() == page) {
                            if (step == 1 && !sysUiInScope) {
                                permissionWarning = PermissionWarningPhase.Visible
                            } else if (agreementReviewOnly && step == 2) {
                                onComplete()
                            } else {
                                advanceFrom(page)
                            }
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
                            if (proceed) advanceFrom(page)
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
                        onDismissRequest = {
                            if (documentVisible) {
                                documentDismissing = true
                                documentVisible = false
                            }
                        },
                        onDismissFinished = {
                            // Keep the rendered document stable while MIUIX retires its popup host.
                            documentDismissing = false
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
