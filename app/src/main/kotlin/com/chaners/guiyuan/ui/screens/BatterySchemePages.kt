package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import top.yukonga.miuix.kmp.glass.GlassCard
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.basic.verticalScrollBar
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.ui.LocalGlassBackdrop
import com.chaners.guiyuan.ui.components.UiOverlayBottomSheet
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.basic.IconButtonDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.chaners.guiyuan.settings.BatteryBuiltInColorScheme
import com.chaners.guiyuan.settings.BatteryColorSchemeLibrary
import com.chaners.guiyuan.settings.BatteryCustomColorScheme
import com.chaners.guiyuan.settings.BatteryColorSlot
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.SnackbarDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import top.yukonga.miuix.kmp.utils.PagerGestureNestedScrollConnection
import top.yukonga.miuix.kmp.utils.PagerNavigationSpringSpec
import top.yukonga.miuix.kmp.utils.pagerGestureOverride
import top.yukonga.miuix.kmp.utils.springAnimateToPage

private data class SchemeDeckContext(
    val library: BatteryColorSchemeLibrary,
    val pages: List<BatterySchemePage>,
    val pager: PagerState,
    val scope: kotlinx.coroutines.CoroutineScope,
    val canCreateCustom: Boolean,
    val scrollStates: Map<String, ScrollState>,
)

internal data class SchemeNavigation(
    val onApply: (String) -> Unit,
    val onBuiltInSlot: (BatteryBuiltInColorScheme, BatteryColorSlot) -> Unit,
    val onCustomSlot: (Int, BatteryColorSlot) -> Unit,
)

internal data class SchemeCatalog(
    val onAdd: () -> Unit,
    val onRename: (Int) -> Unit,
    val onCopy: (BatteryCustomColorScheme) -> Unit,
    val onDelete: (Int) -> Unit,
)

internal data class BatterySchemeOverviewModel(
    val library: BatteryColorSchemeLibrary,
    val requestedSchemeKey: String?,
    val canCreateCustom: Boolean,
)

internal data class BatterySchemeOverviewActions(
    val onRequestedSchemeHandled: () -> Unit,
    val navigation: SchemeNavigation,
    val catalog: SchemeCatalog,
)

@Composable
internal fun BatterySchemeOverview(
    show: Boolean,
    sheetBackdrop: LayerBackdrop?,
    allowDismiss: Boolean,
    onDismissRequest: () -> Unit,
    model: BatterySchemeOverviewModel,
    actions: BatterySchemeOverviewActions,
) {
    // The header and pager share state outside the sheet body; retire it on native dismissal.
    var retained by remember { mutableStateOf(false) }
    if (!show && !retained) return
    SideEffect { if (show) retained = true }
    val (library, requestedSchemeKey, canCreateCustom) = model
    val onRequestedSchemeHandled = actions.onRequestedSchemeHandled
    val navigation = actions.navigation
    val catalog = actions.catalog
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
    SchemeRequestedPageEffect(requestedSchemeKey, pageKeys, pages, pagerState, onRequestedSchemeHandled)
    SchemeDotNavigationEffect(target.intValue, pagerState) {
        if (target.intValue == it) target.intValue = -1
    }
    val scrollStates = pages.associate { page ->
        page.key to key(page.key) { rememberScrollState() }
    }
    val currentScrollOffset: () -> Float = remember(pages, pagerState, scrollStates) {
        { pages.getOrNull(pagerState.currentPage)?.let { scrollStates[it.key]?.value }?.toFloat() ?: 0f }
    }
    val context = SchemeDeckContext(
        library, pages, pagerState, scope, canCreateCustom, scrollStates,
    )
    UiOverlayBottomSheet(
        show = show,
        modifier = Modifier.fillMaxHeight(BATTERY_COLOR_SHEET_HEIGHT_FRACTION)
            .then(if (sheetBackdrop != null) Modifier.layerBackdrop(sheetBackdrop) else Modifier),
        title = stringResource(R.string.battery_colors),
        backgroundColor = MiuixTheme.colorScheme.background,
        insideMargin = DpSize(0.dp, 0.dp),
        startAction = {
            Spacer(Modifier.size(width = IconButtonDefaults.MinWidth, height = IconButtonDefaults.MinHeight))
        },
        allowDismiss = allowDismiss,
        // The naming dialog, not the non-editable deck, owns IME avoidance.
        defaultWindowInsetsPadding = false,
        onDismissRequest = onDismissRequest,
        onDismissFinished = { if (!show) retained = false },
        headerBottomContent = {
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
        },
        scrollOffset = currentScrollOffset,
    ) { innerPadding ->
        CompositionLocalProvider(LocalGlassBackdrop provides sheetBackdrop) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BatterySchemeDeck(context, navigation, catalog, innerPadding)
            }
        }
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
    contentPadding: PaddingValues,
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
        // A scheme insertion changes pageCount; an old key lookup may still be measured
        // against its previous list. Let Pager use its index identity instead.
        // Per-scheme scroll states are already owned by page.key.
    ) { index ->
        BatterySchemeDeckPage(context.pages[index], context, navigation, catalog, contentPadding)
    }
}

@Composable
private fun BatterySchemeDeckPage(
    page: BatterySchemePage,
    context: SchemeDeckContext,
    navigation: SchemeNavigation,
    catalog: SchemeCatalog,
    contentPadding: PaddingValues,
) {
    val scrollState = context.scrollStates.getValue(page.key)
    when (page) {
        is BatterySchemePage.BuiltIn, is BatterySchemePage.Custom ->
            BatterySchemePageContent(
                state = BatterySchemePageState(
                    page = page,
                    isActive = context.library.activeSchemeKey == page.key,
                    canCreateCustom = context.canCreateCustom,
                    manageMenu = if (page is BatterySchemePage.Custom) {
                        {
                            BatterySchemeManageMenu(
                                canCopy = context.canCreateCustom,
                                onRename = { catalog.onRename(page.scheme.id) },
                                onCopy = { catalog.onCopy(page.scheme) },
                                onDelete = { catalog.onDelete(page.scheme.id) },
                            )
                        }
                    } else null,
                ),
                scrollState = scrollState,
                contentPadding = contentPadding,
                actions = BatterySchemePageActions(
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
                ),
            )
        BatterySchemePage.Add ->
            BatteryAddSchemePage(
                count = context.library.customSchemes.size,
                enabled = context.canCreateCustom,
                scrollState = scrollState,
                contentPadding = contentPadding,
                onClick = catalog.onAdd,
            )
    }
}

private data class BatterySchemePageState(
    val page: BatterySchemePage,
    val isActive: Boolean,
    val canCreateCustom: Boolean,
    val manageMenu: (@Composable () -> Unit)?,
)

private data class BatterySchemePageActions(
    val onApply: () -> Unit,
    val onSlotClick: (BatteryColorSlot) -> Unit,
)

@OptIn(ExperimentalScrollBarApi::class)
@Composable
private fun BatterySchemePageContent(
    state: BatterySchemePageState,
    scrollState: ScrollState,
    contentPadding: PaddingValues,
    actions: BatterySchemePageActions,
) {
    val (page, isActive, canCreateCustom, manageMenu) = state
    val (onApply, onSlotClick) = actions
    val builtIn = (page as? BatterySchemePage.BuiltIn)?.scheme
    val custom = (page as? BatterySchemePage.Custom)?.scheme
    val name = when (page) {
        is BatterySchemePage.BuiltIn -> batteryBuiltInName(page.scheme)
        is BatterySchemePage.Custom -> customSchemeName(page.scheme)
        BatterySchemePage.Add -> return
    }
    val canScroll = scrollState.canScrollBackward || scrollState.canScrollForward
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (canScroll) Modifier.scrollEndHaptic() else Modifier)
                .verticalScrollBar(scrollState, trackPadding = contentPadding)
                .verticalScroll(scrollState)
                .padding(contentPadding),
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
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
