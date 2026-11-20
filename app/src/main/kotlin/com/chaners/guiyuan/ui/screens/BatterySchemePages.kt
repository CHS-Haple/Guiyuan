package com.chaners.guiyuan.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.BatteryBuiltInColorScheme
import com.chaners.guiyuan.settings.BatteryColorSchemeLibrary
import com.chaners.guiyuan.settings.BatteryCustomColorScheme
import com.chaners.guiyuan.settings.BatteryColorSlot
import kotlinx.coroutines.launch
import kotlin.math.abs
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.SnackbarDefaults
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PagerGestureNestedScrollConnection
import top.yukonga.miuix.kmp.utils.PagerNavigationSpringSpec
import top.yukonga.miuix.kmp.utils.pagerGestureOverride
import top.yukonga.miuix.kmp.utils.springAnimateToPage

private data class SchemeDeckCreation(
    val enabled: Boolean,
    val nextName: String?,
    val settingsCardHeight: androidx.compose.runtime.MutableIntState,
)

private data class SchemeDeckContext(
    val library: BatteryColorSchemeLibrary,
    val pages: List<BatterySchemePage>,
    val pager: PagerState,
    val scope: kotlinx.coroutines.CoroutineScope,
    val creation: SchemeDeckCreation,
)

private data class SchemeNavigation(
    val onApply: (String) -> Unit,
    val onBuiltInSlot: (BatteryBuiltInColorScheme, BatteryColorSlot) -> Unit,
    val onCustomSlot: (Int, BatteryColorSlot) -> Unit,
)

private data class SchemeCatalog(
    val onAdd: () -> Unit,
    val onRename: (Int) -> Unit,
    val onCopy: (BatteryCustomColorScheme) -> Unit,
    val onDelete: (Int) -> Unit,
)

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
    val pages = remember(library.customSchemes) {
        buildList {
            add(BatterySchemePage.BuiltIn(BatteryBuiltInColorScheme.HYPEROS))
            add(BatterySchemePage.BuiltIn(BatteryBuiltInColorScheme.IOS))
            add(BatterySchemePage.BuiltIn(BatteryBuiltInColorScheme.LOW_SATURATION))
            library.customSchemes.forEach { add(BatterySchemePage.Custom(it)) }
            add(BatterySchemePage.Add)
        }
    }
    val pageKeys = remember(pages) { pages.map { it.key } }
    val initial = pages.indexOfFirst { it.key == library.activeSchemeKey }.takeIf { it >= 0 } ?: 0
    val pagerState = rememberPagerState(initialPage = initial, pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val target = remember { mutableIntStateOf(-1) }
    val settingsHeight = remember { mutableIntStateOf(0) }
    SchemeRequestedPageEffect(requestedSchemeKey, pageKeys, pages, pagerState, onRequestedSchemeHandled)
    SchemeDotNavigationEffect(target.intValue, pagerState) {
        if (target.intValue == it) target.intValue = -1
    }
    val context = SchemeDeckContext(
        library, pages, pagerState, scope, SchemeDeckCreation(canCreateCustom, nextCustomName, settingsHeight),
    )
    val navigation = SchemeNavigation(onApplyScheme, onOpenBuiltInSlot, onOpenCustomSlot)
    val catalog = SchemeCatalog(onAdd, onRenameCustom, onCopyCustom, onDeleteCustom)
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BatterySchemeIndicatorRail(
            pages = pages,
            pagerState = pagerState,
            targetPage = target.intValue,
            onPageSelect = { next ->
                if (next != pagerState.currentPage || target.intValue >= 0 || pagerState.isScrollInProgress) {
                    target.intValue = next
                }
            },
        )
        BatterySchemeDeck(context, navigation, catalog)
    }
}

@Composable
private fun SchemeRequestedPageEffect(
    requestedSchemeKey: String?,
    pageKeys: List<String>,
    pages: List<BatterySchemePage>,
    pager: PagerState,
    onHandled: () -> Unit,
) {
    LaunchedEffect(requestedSchemeKey, pageKeys) {
        val requested = requestedSchemeKey
        if (requested != null) {
            val target = pages.indexOfFirst { it.key == requested }
            if (target >= 0) {
                if (target != pager.currentPage) pager.springAnimateToPage(target)
                onHandled()
            }
        }
    }
}

@Composable
private fun SchemeDotNavigationEffect(
    target: Int,
    pager: PagerState,
    onSettled: (Int) -> Unit,
) {
    LaunchedEffect(target) {
        if (target >= 0) {
            try {
                pager.springAnimateToPage(target)
            } finally {
                onSettled(target)
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.BatterySchemeDeck(
    context: SchemeDeckContext,
    navigation: SchemeNavigation,
    catalog: SchemeCatalog,
) {
    val pager = context.pager
    val flingBehavior = PagerDefaults.flingBehavior(
        state = pager,
        snapAnimationSpec = PagerNavigationSpringSpec,
    )
    HorizontalPager(
        state = pager,
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .clipToBounds()
            .pagerGestureOverride(pagerState = pager, flingBehavior = flingBehavior),
        userScrollEnabled = false,
        flingBehavior = flingBehavior,
        pageNestedScrollConnection = PagerGestureNestedScrollConnection,
        contentPadding = PaddingValues(horizontal = BottomSheetDefaults.insideMargin.width),
        pageSpacing = 12.dp,
        verticalAlignment = Alignment.Top,
    ) { index ->
        BatterySchemeDeckPage(context.pages[index], context, navigation, catalog)
    }
}

@Composable
private fun BatterySchemeDeckPage(
    page: BatterySchemePage,
    context: SchemeDeckContext,
    navigation: SchemeNavigation,
    catalog: SchemeCatalog,
) {
    when (page) {
        is BatterySchemePage.BuiltIn, is BatterySchemePage.Custom ->
            BatterySchemePageContent(
                page = page,
                isActive = context.library.activeSchemeKey == page.key,
                canCreateCustom = context.creation.enabled,
                onApply = { navigation.onApply(page.key) },
                onSlotClick = { slot ->
                    when (page) {
                        is BatterySchemePage.BuiltIn ->
                            context.scope.launch {
                                val addPage = context.pages.lastIndex
                                if (context.pager.currentPage != addPage) {
                                    context.pager.springAnimateToPage(addPage)
                                }
                                navigation.onBuiltInSlot(page.scheme, slot)
                            }
                        is BatterySchemePage.Custom ->
                            navigation.onCustomSlot(page.scheme.id, slot)
                        BatterySchemePage.Add -> Unit
                    }
                },
                onSettingsCardMeasured = { height ->
                    if (height > 0) context.creation.settingsCardHeight.intValue = height
                },
                manageMenu = if (page is BatterySchemePage.Custom) {
                    {
                        BatterySchemeManageMenu(
                            canCopy = context.creation.enabled,
                            onRename = { catalog.onRename(page.scheme.id) },
                            onCopy = { catalog.onCopy(page.scheme) },
                            onDelete = { catalog.onDelete(page.scheme.id) },
                        )
                    }
                } else null,
            )
        BatterySchemePage.Add ->
            BatteryAddSchemePage(
                count = context.library.customSchemes.size,
                enabled = context.creation.enabled,
                name = context.creation.nextName ?: stringResource(R.string.battery_custom_scheme_add_page_title),
                settingsCardHeightPx = context.creation.settingsCardHeight.intValue,
                onClick = catalog.onAdd,
            )
    }
}

@Composable
private fun BatterySchemePageContent(
    page: BatterySchemePage,
    isActive: Boolean,
    canCreateCustom: Boolean,
    onApply: () -> Unit,
    onSlotClick: (BatteryColorSlot) -> Unit,
    onSettingsCardMeasured: (Int) -> Unit,
    manageMenu: (@Composable () -> Unit)?,
) {
    val builtIn = (page as? BatterySchemePage.BuiltIn)?.scheme
    val custom = (page as? BatterySchemePage.Custom)?.scheme
    val name = when (page) {
        is BatterySchemePage.BuiltIn -> batteryBuiltInName(page.scheme)
        is BatterySchemePage.Custom -> customSchemeName(page.scheme)
        BatterySchemePage.Add -> return
    }
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
                previewPage = page,
                endAction = manageMenu,
            )
            BatterySchemeApplyAction(isActive, onApply)
            BatterySchemeSettingsCard(
                builtIn = builtIn,
                custom = custom,
                canCreateCustom = canCreateCustom,
                onSlotClick = onSlotClick,
                onSettingsCardMeasured = onSettingsCardMeasured,
            )
        }
    }
}

@Composable
private fun BatterySchemeApplyAction(isActive: Boolean, onApply: () -> Unit) {
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

private data class BatteryIndicatorGesture(
    val centers: FloatArray,
    val pressed: androidx.compose.runtime.MutableIntState,
    val navigate: androidx.compose.runtime.State<(Int) -> Unit>,
    val switchMargin: Float,
)

private data class BatteryIndicatorLayout(
    val pager: PagerState,
    val targetPage: Int,
    val gap: Dp,
    val spring: androidx.compose.animation.core.SpringSpec<Dp>,
)

@Composable
private fun BatteryPagerIndicator(
    pages: List<BatterySchemePage>,
    pagerState: PagerState,
    targetPage: Int,
    onPageSelect: (Int) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val navigate = rememberUpdatedState(onPageSelect)
    val centers = remember(pages.size) { FloatArray(pages.size) }
    val pressedPage = remember { mutableIntStateOf(-1) }
    val dotSpring = remember {
        spring<Dp>(stiffness = Spring.StiffnessMedium, dampingRatio = 0.85f)
    }
    val gap by animateDpAsState(
        targetValue = if (pressedPage.intValue >= 0) 10.dp else 8.dp,
        animationSpec = dotSpring,
        label = "battery-scheme-gap",
    )
    val gesture = BatteryIndicatorGesture(
        centers = centers,
        pressed = pressedPage,
        navigate = navigate,
        switchMargin = with(LocalDensity.current) { 3.dp.toPx() },
    )
    val layout = BatteryIndicatorLayout(pagerState, targetPage, gap, dotSpring)
    Row(
        modifier = Modifier.batteryIndicatorGestures(gesture, haptic),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BatteryIndicatorDots(pages, layout, gesture)
    }
}

private fun Modifier.batteryIndicatorGestures(
    gesture: BatteryIndicatorGesture,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
): Modifier = pointerInput(gesture.centers.size, haptic) {
    awaitEachGesture {
        fun nearest(x: Float): Int =
            gesture.centers.indices.minByOrNull { abs(x - gesture.centers[it]) } ?: -1

        val down = awaitFirstDown(
            requireUnconsumed = false,
            pass = PointerEventPass.Initial,
        )
        var selected = nearest(down.position.x)
        if (selected >= 0) {
            gesture.pressed.intValue = selected
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            down.consume()
            gesture.navigate.value(selected)
            try {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    val next = nearest(change.position.x)
                    if (next >= 0 && next != selected &&
                        abs(change.position.x - gesture.centers[next]) + gesture.switchMargin <
                        abs(change.position.x - gesture.centers[selected])
                    ) {
                        selected = next
                        gesture.pressed.intValue = next
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        gesture.navigate.value(next)
                    }
                    event.changes.forEach { it.consume() }
                    if (!change.pressed) break
                }
            } finally {
                gesture.pressed.intValue = -1
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BatteryIndicatorDots(
    pages: List<BatterySchemePage>,
    layout: BatteryIndicatorLayout,
    gesture: BatteryIndicatorGesture,
) {
    val currentPage = layout.pager.currentPage
    val pagePosition =
        (currentPage + layout.pager.currentPageOffsetFraction).coerceIn(0f, pages.lastIndex.toFloat())
    val scrolling = layout.pager.isScrollInProgress
    val pressed = gesture.pressed.intValue
    val highlightedPage = pressed.takeIf { it >= 0 } ?: layout.targetPage.takeIf { it >= 0 }
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
            targetValue = 8.dp + (if (pressed == index) 16.dp else 13.dp) * progress,
            animationSpec = if (scrolling && highlightedPage == null) snap() else layout.spring,
            label = "battery-scheme-indicator",
        )
        Box(
            modifier = Modifier
                .width(width + layout.gap).height(28.dp)
                .onGloballyPositioned {
                    gesture.centers[index] = it.positionInParent().x + it.size.width / 2f
                }
                .clickable(
                    interactionSource = null,
                    indication = null,
                    role = Role.Tab,
                    onClickLabel = stringResource(R.string.battery_scheme_go_to, name),
                    onClick = { gesture.navigate.value(index) },
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
                color = lerp(
                    MiuixTheme.colorScheme.disabledOnSecondaryVariant,
                    MiuixTheme.colorScheme.primary,
                    progress,
                ),
            ) {}
        }
    }
}
