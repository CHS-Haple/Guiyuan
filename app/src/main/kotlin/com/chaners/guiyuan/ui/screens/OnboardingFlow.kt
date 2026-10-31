package com.chaners.guiyuan.ui.screens

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import com.chaners.guiyuan.GyApplication
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.AppLang
import com.chaners.guiyuan.system.RootShell
import com.chaners.guiyuan.system.ScopeRestart
import com.chaners.guiyuan.system.XposedStatus
import com.chaners.guiyuan.ui.components.UiOverlayBottomSheet
import com.chaners.guiyuan.ui.screens.about.BgEffectBackground
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.icon.extended.Tasks
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

// The guide owns only temporary navigation state; existing preferences and
// framework services remain the authoritative writers for settings and scope.
@Composable
internal fun OnboardingFlow(
    lang: AppLang,
    onLangChange: (AppLang) -> Unit,
    iconHidden: Boolean,
    onIconHiddenChange: (Boolean) -> Unit,
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

    // 0 Welcome, 1 permissions, 2 privacy, 3 basic settings, 4 complete.
    var step by rememberSaveable { mutableIntStateOf(0) }
    var openDocument by rememberSaveable { mutableStateOf<AgreementDocument?>(null) }
    var policyAccepted by rememberSaveable { mutableStateOf(false) }
    var scopeRequestFailed by remember { mutableStateOf(false) }
    var rootChecking by remember { mutableStateOf(false) }
    var rootAccess by remember { mutableStateOf<Boolean?>(null) }
    val entryState = rememberAppSettingsEntryState(runtimeBusy = false)
    val coroutineScope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    LaunchedEffect(step) { scroll.scrollTo(0) }
    BackHandler(enabled = openDocument != null || step > 0 || onExit != null) {
        when {
            openDocument != null -> openDocument = null
            step > 0 -> step--
            else -> onExit?.invoke()
        }
    }

    if (step == 0 || step == 4) {
        GuiyuanBrandGuide(
            completed = step == 4,
            onNext = { if (step == 4) onComplete() else step++ },
        )
    } else {
        val title = when (step) {
            1 -> stringResource(R.string.guide_permissions_title)
            2 -> stringResource(R.string.guide_privacy_title)
            else -> stringResource(R.string.guide_basic_title)
        }
        val illustration = when (step) {
            1 -> MiuixIcons.Tasks
            2 -> MiuixIcons.Lock
            else -> MiuixIcons.Settings
        }
        Scaffold { scaffoldPadding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(scaffoldPadding).padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(50.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { step-- }) {
                        Icon(MiuixIcons.Back, contentDescription = stringResource(R.string.back))
                    }
                }
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll),
                ) {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().height(70.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = illustration,
                            contentDescription = null,
                            modifier = Modifier.size(70.dp),
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
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            color = MiuixTheme.colorScheme.onBackground,
                        )
                    }
                    Spacer(Modifier.height(30.dp))
                    when (step) {
                        1 -> {
                            GuidePermissionItem(
                                title = stringResource(R.string.guide_sysui_scope),
                                status = when {
                                    sysUiInScope -> stringResource(R.string.guide_scope_ready)
                                    connected == null -> stringResource(R.string.guide_status_unknown)
                                    connected.apiVersion < 102 -> stringResource(R.string.guide_api_unsupported)
                                    else -> stringResource(R.string.guide_scope_not_ready)
                                },
                                checked = sysUiInScope,
                                onClick = if (!sysUiInScope && canRequestScope) {
                                    {
                                        scopeRequestFailed = false
                                        if (!app.requestSysUiScope { approved ->
                                                scopeRequestFailed = !approved
                                            }) scopeRequestFailed = true
                                    }
                                } else null,
                            )
                            Text(
                                text = stringResource(R.string.guide_permission_later),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                                style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp),
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                            GuidePermissionItem(
                                title = stringResource(R.string.guide_root_title),
                                status = when {
                                    rootChecking -> stringResource(R.string.guide_root_checking)
                                    rootAccess == true -> stringResource(R.string.guide_root_granted)
                                    rootAccess == false -> stringResource(R.string.guide_root_unavailable)
                                    else -> stringResource(R.string.guide_root_not_checked)
                                },
                                checked = rootAccess == true,
                                onClick = if (rootChecking) null else {
                                    {
                                        coroutineScope.launch {
                                            rootChecking = true
                                            try {
                                                val result = RootShell.execute(
                                                    context = context,
                                                    command = "id -u",
                                                    timeoutSeconds = 15L,
                                                )
                                                rootAccess = result.isSuccess && result.output.trim() == "0"
                                            } finally {
                                                rootChecking = false
                                            }
                                        }
                                    }
                                },
                            )
                            Spacer(Modifier.height(10.dp))
                            GuidePermissionItem(
                                title = stringResource(R.string.guide_framework),
                                status = when (framework) {
                                    XposedStatus.Checking -> stringResource(R.string.guide_framework_checking)
                                    XposedStatus.FrameworkUnavailable -> stringResource(R.string.guide_framework_offline)
                                    XposedStatus.QueryUnavailable -> stringResource(R.string.guide_status_unknown)
                                    is XposedStatus.Connected -> stringResource(R.string.guide_framework_connected)
                                },
                                checked = connected != null,
                            )
                            Text(
                                text = stringResource(R.string.guide_permission_hint),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                                style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp),
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                            if (scopeBusy || scopeRequestFailed || rootChecking || rootAccess == false) {
                                val hint = when {
                                    scopeBusy -> R.string.guide_scope_requesting
                                    scopeRequestFailed -> R.string.guide_scope_request_failed
                                    rootChecking -> R.string.guide_root_checking
                                    else -> R.string.guide_root_unavailable
                                }
                                Text(
                                    text = stringResource(hint),
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                                    style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp),
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            }
                        }
                        2 -> {
                            val termsLabel = stringResource(R.string.guide_terms_link_text)
                            val privacyLabel = stringResource(R.string.guide_policy_link_text)
                            val line = stringResource(R.string.guide_agreement_line, termsLabel, privacyLabel)
                            val linkedText = buildAnnotatedString {
                                append(line)
                                val termsStart = line.indexOf(termsLabel)
                                val policyStart = line.indexOf(privacyLabel, termsStart + termsLabel.length)
                                if (termsStart >= 0) {
                                    addLink(
                                        LinkAnnotation.Clickable(
                                            tag = "terms",
                                            linkInteractionListener = LinkInteractionListener {
                                                openDocument = AgreementDocument.Terms
                                            },
                                        ),
                                        termsStart,
                                        termsStart + termsLabel.length,
                                    )
                                }
                                if (policyStart >= 0) {
                                    addLink(
                                        LinkAnnotation.Clickable(
                                            tag = "privacy",
                                            linkInteractionListener = LinkInteractionListener {
                                                openDocument = AgreementDocument.Privacy
                                            },
                                        ),
                                        policyStart,
                                        policyStart + privacyLabel.length,
                                    )
                                }
                            }
                            Text(
                                text = linkedText,
                                modifier = Modifier.fillMaxWidth(),
                                style = MiuixTheme.textStyles.body2.copy(lineHeight = 26.sp),
                                color = MiuixTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(24.dp))
                            Text(
                                text = stringResource(R.string.guide_privacy_brief),
                                style = MiuixTheme.textStyles.body2.copy(lineHeight = 26.sp),
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                            Spacer(Modifier.height(20.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .toggleable(
                                        value = policyAccepted,
                                        role = Role.Checkbox,
                                        onValueChange = { policyAccepted = it },
                                    )
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Checkbox(
                                    state = if (policyAccepted) ToggleableState.On else ToggleableState.Off,
                                    onClick = null,
                                )
                                Text(
                                    text = stringResource(R.string.guide_privacy_ack),
                                    style = MiuixTheme.textStyles.body2,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        3 -> {
                            SmallTitle(
                                text = stringResource(R.string.section_app),
                                insideMargin = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            )
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
                    Spacer(Modifier.height(24.dp))
                }
                TextButton(
                    text = stringResource(
                        if (step == 2) R.string.guide_agree_continue else R.string.guide_continue,
                    ),
                    modifier = Modifier.fillMaxWidth().widthIn(max = 336.dp).height(50.dp),
                    cornerRadius = 16.dp,
                    textStyle = MiuixTheme.textStyles.button.copy(fontSize = 17.sp),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    enabled = step != 2 || policyAccepted,
                    onClick = { step++ },
                )
                Spacer(Modifier.height(44.dp))
            }
            // Both overlays require the composition locals owned by this Scaffold.
            AppSettingsEntryDialogs(entryState)
            UiOverlayBottomSheet(
                show = openDocument != null,
                modifier = Modifier.fillMaxHeight(0.85f),
                title = stringResource((openDocument ?: AgreementDocument.Privacy).titleRes()),
                backgroundColor = MiuixTheme.colorScheme.background,
                insideMargin = DpSize(0.dp, 0.dp),
                onDismissRequest = { openDocument = null },
            ) {
                openDocument?.let { AgreementSheetBody(it) }
            }
        }
    }
}

@Composable
private fun GuiyuanBrandGuide(completed: Boolean, onNext: () -> Unit) {
    val lifecycle = (LocalActivity.current as? ComponentActivity)?.lifecycle
    var resumed by remember(lifecycle) {
        mutableStateOf(lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true)
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> resumed = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> resumed = false
                else -> Unit
            }
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }
    val surface = MiuixTheme.colorScheme.surface
    val accent = MiuixTheme.colorScheme.primary
    val actionColors = guideActionColors()
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
                        fontSize = 35.sp,
                        fontWeight = FontWeight.Bold,
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
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }
            if (completed) {
                TextButton(
                    text = stringResource(R.string.guide_start),
                    modifier = Modifier.fillMaxWidth().widthIn(max = 336.dp).height(50.dp),
                    cornerRadius = 16.dp,
                    textStyle = MiuixTheme.textStyles.button.copy(fontSize = 17.sp),
                    colors = ButtonDefaults.textButtonColors(
                        color = actionColors.container,
                        textColor = actionColors.content,
                    ),
                    onClick = onNext,
                )
                Spacer(Modifier.height(44.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .squircleSurface(
                            color = actionColors.container,
                            cornerRadius = 35.dp,
                            extension = 1f,
                        )
                        .clickable(
                            onClickLabel = stringResource(R.string.guide_continue),
                            role = Role.Button,
                            onClick = onNext,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.width(31.dp).height(22.dp)) {
                        val middle = size.height / 2f
                        val end = size.width * 0.96f
                        val start = size.width * 0.04f
                        val wing = size.height * 0.42f
                        drawLine(actionColors.content, Offset(start, middle), Offset(end, middle), strokeWidth = 2.6.dp.toPx(), cap = StrokeCap.Round)
                        drawLine(actionColors.content, Offset(end - wing, middle - wing), Offset(end, middle), strokeWidth = 2.6.dp.toPx(), cap = StrokeCap.Round)
                        drawLine(actionColors.content, Offset(end - wing, middle + wing), Offset(end, middle), strokeWidth = 2.6.dp.toPx(), cap = StrokeCap.Round)
                    }
                }
                Spacer(Modifier.weight(0.28f))
            }
        }
    }
}

private data class GuideActionColors(val container: Color, val content: Color)

// The branded actions retain the reference geometry, while their material follows
// the same MIUIX theme tokens used by the About surface and animated identity.
@Composable
private fun guideActionColors(): GuideActionColors {
    val surface = MiuixTheme.colorScheme.surface
    val onSurface = MiuixTheme.colorScheme.onSurface
    val accent = MiuixTheme.colorScheme.primary
    val tint = lerp(onSurface, accent, 0.10f)
    // Respect the MIUIX theme palette; preserve the translucent style where
    // the chosen foreground has sufficient contrast over the theme surface.
    val opacities = listOf(0.60f, 0.68f, 0.76f, 0.84f, 0.92f, 1f)
    val opacity = opacities.firstOrNull { opacity ->
        val composite = tint.copy(alpha = opacity).compositeOver(surface)
        val foreground = surface.luminance()
        val background = composite.luminance()
        (maxOf(foreground, background) + 0.05f) /
            (minOf(foreground, background) + 0.05f) >= 4.5f
    } ?: 1f
    return GuideActionColors(
        container = tint.copy(alpha = opacity),
        content = surface,
    )
}

@Composable
private fun GuidePermissionItem(
    title: String,
    status: String,
    checked: Boolean,
    onClick: (() -> Unit)? = null,
) {
    val checkColor = MiuixTheme.colorScheme.primary
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(0.dp),
        pressFeedbackType = if (onClick != null) PressFeedbackType.Sink else PressFeedbackType.None,
        showIndication = onClick != null,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .semantics { stateDescription = status }
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MiuixTheme.textStyles.body1.copy(fontSize = 18.sp),
                color = MiuixTheme.colorScheme.onSurfaceContainer,
            )
            if (checked) {
                Icon(
                    imageVector = MiuixIcons.Basic.Check,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = checkColor,
                )
            }
        }
    }
}
