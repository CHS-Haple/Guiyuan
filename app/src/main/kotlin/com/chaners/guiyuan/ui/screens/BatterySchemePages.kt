package com.chaners.guiyuan.ui.screens

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.UiStyle
import com.chaners.guiyuan.ui.LocalUiStyle
import com.chaners.guiyuan.ui.LocalGlassBackdrop
import com.chaners.guiyuan.ui.icons.Os4GlassMore
import com.chaners.guiyuan.settings.BATTERY_COLOR_SCHEME_CUSTOM_MAX
import com.chaners.guiyuan.settings.BatteryBuiltInColorScheme
import com.chaners.guiyuan.settings.BatteryColorSchemeLibrary
import com.chaners.guiyuan.settings.BatteryColorSchemeSource
import com.chaners.guiyuan.settings.BatteryCustomColorScheme
import com.chaners.guiyuan.settings.BatteryColorSlot
import com.chaners.guiyuan.settings.batteryBuiltInColor
import com.chaners.guiyuan.settings.batterySchemeEntryColor
import kotlinx.coroutines.launch
import kotlin.math.abs
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.FloatingActionButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButtonDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.SnackbarDefaults
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassDropdownPopupContent
import top.yukonga.miuix.kmp.glass.GlassPopupDefaults
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.PopupLayout
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PagerGestureNestedScrollConnection
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.PagerNavigationSpringSpec
import top.yukonga.miuix.kmp.utils.pagerGestureOverride
import top.yukonga.miuix.kmp.utils.springAnimateToPage

@Composable
internal fun BatterySchemeOverview(
    library: BatteryColorSchemeLibrary,
    requestedSchemeKey: String?,
    onRequestedSchemeHandled: () -> Unit,
    canCreateCustom: Boolean,
    nextCustomName: String?,
    onApplyScheme: (String) -> Unit,
    onOpenBuiltInSlot: (BatteryBuiltInColorScheme, BatteryColorSlot) -> Unit,
    onOpenCustomSlot: (Int, BatteryColorSlot) -> Unit,
    onAdd: () -> Unit,
    onRenameCustom: (Int) -> Unit,
    onCopyCustom: (BatteryCustomColorScheme) -> Unit,
    onDeleteCustom: (Int) -> Unit,
) {
    val pages =
        buildList {
            add(BatterySchemePage.BuiltIn(BatteryBuiltInColorScheme.HYPEROS))
            add(BatterySchemePage.BuiltIn(BatteryBuiltInColorScheme.IOS))
            add(BatterySchemePage.BuiltIn(BatteryBuiltInColorScheme.LOW_SATURATION))
            library.customSchemes.forEach { add(BatterySchemePage.Custom(it)) }
            add(BatterySchemePage.Add)
        }
    val initial =
        pages.indexOfFirst { it.key == library.activeSchemeKey }
            .takeIf { it >= 0 }
            ?: 0
    val pagerState =
        rememberPagerState(
            initialPage = initial,
            pageCount = { pages.size },
        )
    val scope = rememberCoroutineScope()
    var dotTarget by remember { mutableIntStateOf(-1) }
    var settingsCardHeightPx by remember { mutableIntStateOf(0) }
    val flingBehavior =
        PagerDefaults.flingBehavior(
            state = pagerState,
            snapAnimationSpec = PagerNavigationSpringSpec,
        )

    LaunchedEffect(requestedSchemeKey, pages.map { it.key }) {
        val requested = requestedSchemeKey ?: return@LaunchedEffect
        val target = pages.indexOfFirst { it.key == requested }
        if (target >= 0) {
            if (target != pagerState.currentPage) {
                pagerState.springAnimateToPage(target)
            }
            onRequestedSchemeHandled()
        }
    }

    LaunchedEffect(dotTarget) {
        val target = dotTarget
        if (target < 0) return@LaunchedEffect
        try {
            pagerState.scrollToPage(target)
        } finally {
            if (dotTarget == target) dotTarget = -1
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BatterySchemeIndicatorRail(
            pages = pages,
            pagerState = pagerState,
            targetPage = dotTarget,
            onPageSelect = { target ->
                if (target != pagerState.currentPage || dotTarget >= 0 || pagerState.isScrollInProgress) {
                    dotTarget = target
                }
            },
        )
        HorizontalPager(
            state = pagerState,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
                    .pagerGestureOverride(
                        pagerState = pagerState,
                        flingBehavior = flingBehavior,
                    ),
            userScrollEnabled = false,
            flingBehavior = flingBehavior,
            pageNestedScrollConnection = PagerGestureNestedScrollConnection,
            contentPadding =
                PaddingValues(
                    horizontal = BottomSheetDefaults.insideMargin.width,
                ),
            pageSpacing = 12.dp,
            verticalAlignment = Alignment.Top,
        ) { index ->
            when (val page = pages[index]) {
                is BatterySchemePage.BuiltIn ->
                    BatterySchemePageContent(
                        name = batteryBuiltInName(page.scheme),
                        builtIn = page.scheme,
                        custom = null,
                        isActive = library.activeSchemeKey == page.key,
                        canCreateCustom = canCreateCustom,
                        onApply = { onApplyScheme(page.key) },
                        onSlotClick = { slot ->
                            scope.launch {
                                val addPage = pages.lastIndex
                                if (pagerState.currentPage != addPage) {
                                    pagerState.springAnimateToPage(addPage)
                                }
                                onOpenBuiltInSlot(page.scheme, slot)
                            }
                        },
                        onSettingsCardMeasured = { height ->
                            if (height > 0) settingsCardHeightPx = height
                        },
                        onRename = null,
                        onCopy = null,
                        onDelete = null,
                    )
                is BatterySchemePage.Custom ->
                    BatterySchemePageContent(
                        name = customSchemeName(page.scheme),
                        builtIn = null,
                        custom = page.scheme,
                        isActive = library.activeSchemeKey == page.key,
                        canCreateCustom = canCreateCustom,
                        onApply = { onApplyScheme(page.key) },
                        onSlotClick = { slot ->
                            onOpenCustomSlot(page.scheme.id, slot)
                        },
                        onSettingsCardMeasured = { height ->
                            if (height > 0) settingsCardHeightPx = height
                        },
                        onRename = { onRenameCustom(page.scheme.id) },
                        onCopy = { onCopyCustom(page.scheme) },
                        onDelete = { onDeleteCustom(page.scheme.id) },
                    )
                BatterySchemePage.Add ->
                    BatteryAddSchemePage(
                        count = library.customSchemes.size,
                        enabled = canCreateCustom,
                        name =
                            nextCustomName
                                ?: stringResource(R.string.battery_custom_scheme_add_page_title),
                        settingsCardHeightPx = settingsCardHeightPx,
                        onClick = onAdd,
                    )
            }
        }
    }
}

@Composable
private fun BatterySchemePageContent(
    name: String,
    builtIn: BatteryBuiltInColorScheme?,
    custom: BatteryCustomColorScheme?,
    isActive: Boolean,
    canCreateCustom: Boolean,
    onApply: () -> Unit,
    onSlotClick: (BatteryColorSlot) -> Unit,
    onSettingsCardMeasured: (Int) -> Unit,
    onRename: (() -> Unit)?,
    onCopy: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    val scrollState = rememberScrollState()
    val canScroll = scrollState.canScrollBackward || scrollState.canScrollForward
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (canScroll) Modifier.scrollEndHaptic() else Modifier)
                .verticalScroll(scrollState),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.surface,
                    contentColor = MiuixTheme.colorScheme.onSurface,
                ),
            insideMargin = PaddingValues(16.dp),
        ) {
            BatterySchemeHeader(
                name = name,
                previewPage =
                    if (builtIn != null) {
                        BatterySchemePage.BuiltIn(builtIn)
                    } else {
                        custom?.let(BatterySchemePage::Custom)
                    },
                endAction =
                    if (custom != null && onRename != null && onCopy != null && onDelete != null) {
                        {
                            BatterySchemeManageMenu(
                                canCopy = canCreateCustom,
                                onRename = onRename,
                                onCopy = onCopy,
                                onDelete = onDelete,
                            )
                        }
                    } else {
                        null
                    },
            )
            BatterySchemeActionArea {
                TextButton(
                    text =
                        stringResource(
                            if (isActive) {
                                R.string.battery_color_scheme_applied
                            } else {
                                R.string.battery_color_scheme_apply
                            },
                        ),
                    onClick = onApply,
                    enabled = !isActive,
                    minWidth = 26.dp,
                    minHeight = 26.dp,
                    cornerRadius = SnackbarDefaults.ActionCornerRadius,
                    insideMargin = SnackbarDefaults.ActionInsideMargin,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    textStyle = TextStyle(fontSize = 15.sp),
                )
            }
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .onSizeChanged { size ->
                            onSettingsCardMeasured(size.height)
                        },
            ) {
                BatteryColorSlot.entries.forEach { slot ->
                    val color =
                        when {
                            builtIn != null -> batteryBuiltInColor(builtIn, slot)
                            custom != null ->
                                batterySchemeEntryColor(custom.entries.entryFor(slot), slot)
                            else -> null
                        }
                    val followsSystem =
                        when {
                            builtIn != null -> color == null
                            custom != null ->
                                custom.entries.entryFor(slot).source ==
                                    BatteryColorSchemeSource.FOLLOW_SYSTEM ||
                                    color == null
                            else -> true
                        }
                    BatteryModeSettingItem(
                        slot = slot,
                        color = color,
                        followsSystem = followsSystem,
                        enabled = custom != null || canCreateCustom,
                        onClick = { onSlotClick(slot) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BatterySchemeHeader(
    name: String,
    previewPage: BatterySchemePage?,
    endAction: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = IconButtonDefaults.MinHeight),
    ) {
        Text(
            text = name,
            style = MiuixTheme.textStyles.title2,
            fontWeight = FontWeight.SemiBold,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = IconButtonDefaults.MinWidth)
                    .align(Alignment.Center),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        endAction?.let { action ->
            Box(
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                action()
            }
        }
    }
    Spacer(Modifier.height(BATTERY_SCHEME_VERTICAL_GAP))
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        BatterySchemePreviewStrip(
            page = previewPage,
            size = 28.dp,
            spacing = 10.dp,
        )
    }
}

@Composable
private fun BatterySchemeActionArea(
    content: @Composable () -> Unit,
) {
    Spacer(Modifier.height(BATTERY_SCHEME_VERTICAL_GAP))
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
    Spacer(Modifier.height(BATTERY_SCHEME_VERTICAL_GAP))
}

@Composable
private fun BatteryModeSettingItem(
    slot: BatteryColorSlot,
    color: Int?,
    followsSystem: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val summary =
        if (followsSystem || color == null) {
            stringResource(R.string.battery_color_follow_inversion)
        } else {
            batteryColorHex(color)
        }
    ArrowPreference(
        title = stringResource(batteryColorSlotLabel(slot)),
        summary = summary,
        enabled = enabled,
        onClick = onClick,
        endActions = {
            if (followsSystem || color == null) {
                BatteryColorMosaic(size = 24.dp)
            } else {
                BatteryColorDot(
                    color = color,
                    size = 24.dp,
                )
            }
        },
    )
}

@Composable
private fun BatteryAddSchemePage(
    count: Int,
    enabled: Boolean,
    name: String,
    settingsCardHeightPx: Int,
    onClick: () -> Unit,
) {
    val density = LocalDensity.current
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.surface,
                    contentColor = MiuixTheme.colorScheme.onSurface,
                ),
            insideMargin = PaddingValues(16.dp),
        ) {
            BatterySchemeHeader(
                name = name,
                previewPage = BatterySchemePage.BuiltIn(BatteryBuiltInColorScheme.HYPEROS),
            )
            BatterySchemeActionArea {
                TextButton(
                    text =
                        stringResource(
                            if (enabled) {
                                R.string.battery_custom_scheme_add_page_title
                            } else {
                                R.string.battery_custom_scheme_limit
                            },
                        ),
                    onClick = onClick,
                    enabled = enabled,
                    minWidth = 26.dp,
                    minHeight = 26.dp,
                    cornerRadius = SnackbarDefaults.ActionCornerRadius,
                    insideMargin = SnackbarDefaults.ActionInsideMargin,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    textStyle = TextStyle(fontSize = 15.sp),
                )
            }
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .then(
                            if (settingsCardHeightPx > 0) {
                                Modifier.height(with(density) { settingsCardHeightPx.toDp() })
                            } else {
                                Modifier.heightIn(min = 400.dp)
                            },
                        ),
                pressFeedbackType =
                    if (enabled) PressFeedbackType.Sink else PressFeedbackType.None,
                onClick = if (enabled) onClick else null,
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        modifier = Modifier.size(FloatingActionButtonDefaults.MinWidth),
                        shape = CircleShape,
                        color =
                            if (enabled) MiuixTheme.colorScheme.primary
                            else MiuixTheme.colorScheme.disabledPrimaryButton,
                        shadowElevation = 0.dp,
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Add,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint =
                                    if (enabled) MiuixTheme.colorScheme.onPrimary
                                    else MiuixTheme.colorScheme.disabledOnPrimaryButton,
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text =
                            if (enabled) stringResource(R.string.battery_scheme_new_summary)
                            else stringResource(R.string.battery_scheme_full_summary, BATTERY_COLOR_SCHEME_CUSTOM_MAX),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(24.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.battery_scheme_capacity),
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                            Text(
                                text = "$count / $BATTERY_COLOR_SCHEME_CUSTOM_MAX",
                                style = MiuixTheme.textStyles.body2,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            repeat(BATTERY_COLOR_SCHEME_CUSTOM_MAX) { index ->
                                Surface(
                                    modifier = Modifier.weight(1f).height(6.dp),
                                    shape = CircleShape,
                                    color =
                                        if (index < count) MiuixTheme.colorScheme.primary
                                        else MiuixTheme.colorScheme.onSurface.copy(alpha = 0.14f),
                                ) {}
                            }
                        }
                        Text(
                            text =
                                if (enabled) stringResource(
                                    R.string.battery_scheme_remaining,
                                    BATTERY_COLOR_SCHEME_CUSTOM_MAX - count,
                                ) else stringResource(R.string.battery_scheme_full_hint),
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BatterySchemeIndicatorRail(
    pages: List<BatterySchemePage>,
    pagerState: PagerState,
    targetPage: Int,
    onPageSelect: (Int) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(28.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        BatteryPagerIndicator(
            pages = pages,
            pagerState = pagerState,
            targetPage = targetPage,
            onPageSelect = onPageSelect,
        )
    }
}

@Composable
private fun BatteryPagerIndicator(
    pages: List<BatterySchemePage>,
    pagerState: PagerState,
    targetPage: Int,
    onPageSelect: (Int) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val navigate by rememberUpdatedState(onPageSelect)
    val centers = remember(pages.size) { FloatArray(pages.size) }
    var pressedPage by remember { mutableIntStateOf(-1) }
    val dotSpring = remember {
        spring<Dp>(stiffness = Spring.StiffnessMedium, dampingRatio = 0.85f)
    }
    val gap by animateDpAsState(
        targetValue = if (pressedPage >= 0) 10.dp else 8.dp,
        animationSpec = dotSpring,
        label = "battery-scheme-gap",
    )
    val switchMargin = with(LocalDensity.current) { 3.dp.toPx() }

    Row(
        modifier = Modifier.pointerInput(pages.size, haptic) {
            awaitEachGesture {
                fun nearest(x: Float): Int =
                    centers.indices.minByOrNull { abs(x - centers[it]) } ?: -1

                val down = awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Initial,
                )
                var selected = nearest(down.position.x)
                if (selected < 0) return@awaitEachGesture
                pressedPage = selected
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                down.consume()
                navigate(selected)
                try {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        val next = nearest(change.position.x)
                        if (next >= 0 && next != selected &&
                            abs(change.position.x - centers[next]) + switchMargin <
                            abs(change.position.x - centers[selected])
                        ) {
                            selected = next
                            pressedPage = next
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            navigate(next)
                        }
                        event.changes.forEach { it.consume() }
                        if (!change.pressed) break
                    }
                } finally {
                    pressedPage = -1
                }
            }
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val currentPage = pagerState.currentPage
        val pagePosition =
            (currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, pages.lastIndex.toFloat())
        val scrolling = pagerState.isScrollInProgress
        val highlightedPage = pressedPage.takeIf { it >= 0 } ?: targetPage.takeIf { it >= 0 }
        pages.forEachIndexed { index, page ->
            val name = when (page) {
                is BatterySchemePage.BuiltIn -> batteryBuiltInName(page.scheme)
                is BatterySchemePage.Custom -> customSchemeName(page.scheme)
                BatterySchemePage.Add -> stringResource(R.string.battery_custom_scheme_add_page_title)
            }
            val isCurrent = index == currentPage
            val progress =
                if (highlightedPage != null) {
                    if (index == highlightedPage) 1f else 0f
                } else {
                    (1f - abs(index - pagePosition)).coerceIn(0f, 1f)
                }
            val width by animateDpAsState(
                targetValue = 8.dp + (if (pressedPage == index) 16.dp else 13.dp) * progress,
                animationSpec =
                    if (scrolling && highlightedPage == null) snap() else dotSpring,
                label = "battery-scheme-indicator",
            )
            Box(
                modifier = Modifier
                    .width(width + gap).height(28.dp)
                    .onGloballyPositioned { centers[index] = it.positionInParent().x + it.size.width / 2f }
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        role = Role.Tab,
                        onClickLabel = stringResource(R.string.battery_scheme_go_to, name),
                        onClick = { onPageSelect(index) },
                    )
                    .semantics {
                        contentDescription = name
                        selected = isCurrent
                    },
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.width(width).height(8.dp),
                    shape = CircleShape,
                    color =
                        lerp(
                            MiuixTheme.colorScheme.disabledOnSecondaryVariant,
                            MiuixTheme.colorScheme.primary,
                            progress,
                        ),
                ) {}
            }
        }
    }
}

@Composable
private fun BatterySchemeManageMenu(
    modifier: Modifier = Modifier,
    canCopy: Boolean,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    if (LocalUiStyle.current == UiStyle.OS4 && isRuntimeShaderSupported()) {
        // Capture at the sheet caller: the root PopupLayout may not inherit this local.
        val backdrop = LocalGlassBackdrop.current
        val haptic = LocalHapticFeedback.current
        var expanded by remember { mutableStateOf(false) }
        val mounted = remember { mutableStateOf(false) }
        var anchorBounds by remember { mutableStateOf(Rect.Zero) }

        TooltipBox(text = stringResource(R.string.battery_custom_scheme_manage)) {
            GlassIconButton(
                onClick = {
                    expanded = !expanded
                    if (expanded) {
                        mounted.value = true
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    }
                },
                modifier = modifier.onGloballyPositioned { anchorBounds = it.boundsInRoot() },
            ) {
                Icon(
                    imageVector = Os4GlassMore,
                    contentDescription = stringResource(R.string.battery_custom_scheme_manage),
                )
            }
        }

        if (mounted.value) {
            // The sheet's pager clips children. Its root Scaffold owns the popup host.
            PopupLayout(
                visible = mounted,
                enableWindowDim = false,
                enableBackHandler = false,
                enterTransition = EnterTransition.None,
                exitTransition = ExitTransition.None,
                renderInRootScaffold = true,
            ) {
                var hostOrigin by remember { mutableStateOf(Offset.Zero) }
                Box(
                    modifier = Modifier.fillMaxSize().onGloballyPositioned {
                        hostOrigin = it.positionInRoot()
                    },
                ) {
                    GlassDropdownPopupContent(
                        show = expanded,
                        onDismissRequest = { expanded = false },
                        anchorBounds = anchorBounds,
                        coordinateOrigin = hostOrigin,
                        backdrop = backdrop,
                        onDismissFinished = { mounted.value = false },
                    ) {
                        GlassPopupItem(
                            text = stringResource(R.string.battery_custom_scheme_rename),
                            icon = MiuixIcons.Edit,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                expanded = false
                                onRename()
                            },
                        )
                        GlassPopupItem(
                            text = stringResource(R.string.battery_custom_scheme_copy),
                            icon = MiuixIcons.Copy,
                            enabled = canCopy,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                expanded = false
                                onCopy()
                            },
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(
                                horizontal = GlassPopupDefaults.DividerPaddingHorizontal,
                                vertical = GlassPopupDefaults.DividerPaddingVertical,
                            ),
                            thickness = GlassPopupDefaults.DividerThickness,
                        )
                        GlassPopupItem(
                            text = stringResource(R.string.battery_custom_scheme_delete),
                            icon = MiuixIcons.Delete,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                expanded = false
                                onDelete()
                            },
                        )
                    }
                }
            }
        }
    } else {
        val entry =
            DropdownEntry(
                items =
                    listOf(
                        DropdownItem(
                            text = stringResource(R.string.battery_custom_scheme_rename),
                            icon = { modifier ->
                                Icon(
                                    imageVector = MiuixIcons.Edit,
                                    contentDescription = null,
                                    modifier = modifier,
                                )
                            },
                            onClick = onRename,
                        ),
                        DropdownItem(
                            text = stringResource(R.string.battery_custom_scheme_copy),
                            icon = { modifier ->
                                Icon(
                                    imageVector = MiuixIcons.Copy,
                                    contentDescription = null,
                                    modifier = modifier,
                                )
                            },
                            enabled = canCopy,
                            onClick = onCopy,
                        ),
                        DropdownItem(
                            text = stringResource(R.string.battery_custom_scheme_delete),
                            icon = { modifier ->
                                Icon(
                                    imageVector = MiuixIcons.Delete,
                                    contentDescription = null,
                                    modifier = modifier,
                                )
                            },
                            onClick = onDelete,
                        ),
                    ),
            )

        TooltipBox(text = stringResource(R.string.battery_custom_scheme_manage)) {
            OverlayIconDropdownMenu(
                entry = entry,
                modifier = modifier,
            ) {
                Icon(
                    imageVector = if (LocalUiStyle.current == UiStyle.OS4) Os4GlassMore else MiuixIcons.More,
                    contentDescription = stringResource(R.string.battery_custom_scheme_manage),
                )
            }
        }
    }
}
