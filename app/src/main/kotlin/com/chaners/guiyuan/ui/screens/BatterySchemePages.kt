package com.chaners.guiyuan.ui.screens

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.BatteryBuiltInColorScheme
import com.chaners.guiyuan.settings.BatteryColorSchemeLibrary
import com.chaners.guiyuan.settings.BatteryColorSchemeSource
import com.chaners.guiyuan.settings.BatteryCustomColorScheme
import com.chaners.guiyuan.settings.CombinedStatusBatteryColorSlot
import com.chaners.guiyuan.settings.batteryBuiltInColor
import com.chaners.guiyuan.settings.batterySchemeEntryColor
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.FloatingActionButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButtonDefaults
import top.yukonga.miuix.kmp.basic.SnackbarDefaults
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
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
    onOpenBuiltInSlot: (BatteryBuiltInColorScheme, CombinedStatusBatteryColorSlot) -> Unit,
    onOpenCustomSlot: (Int, CombinedStatusBatteryColorSlot) -> Unit,
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

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BatterySchemeIndicatorRail(
            pageCount = pages.size,
            currentPage = pagerState.currentPage,
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
    onSlotClick: (CombinedStatusBatteryColorSlot) -> Unit,
    onSettingsCardMeasured: (Int) -> Unit,
    onRename: (() -> Unit)?,
    onCopy: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
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
                CombinedStatusBatteryColorSlot.entries.forEach { slot ->
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
    slot: CombinedStatusBatteryColorSlot,
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
                                Modifier.heightIn(min = 144.dp)
                            },
                        ),
                pressFeedbackType =
                    if (enabled) {
                        PressFeedbackType.Sink
                    } else {
                        PressFeedbackType.None
                    },
                onClick = if (enabled) onClick else null,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier =
                            Modifier.size(
                                width = FloatingActionButtonDefaults.MinWidth,
                                height = FloatingActionButtonDefaults.MinHeight,
                            ),
                        shape = CircleShape,
                        color =
                            if (enabled) {
                                MiuixTheme.colorScheme.primary
                            } else {
                                MiuixTheme.colorScheme.disabledPrimaryButton
                            },
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
                                    if (enabled) {
                                        MiuixTheme.colorScheme.onPrimary
                                    } else {
                                        MiuixTheme.colorScheme.disabledOnPrimaryButton
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BatterySchemeIndicatorRail(
    pageCount: Int,
    currentPage: Int,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        BatteryPagerIndicator(
            pageCount = pageCount,
            currentPage = currentPage,
        )
    }
}

@Composable
private fun BatteryPagerIndicator(
    pageCount: Int,
    currentPage: Int,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val width by
                animateDpAsState(
                    targetValue = if (index == currentPage) 16.dp else 6.dp,
                    label = "battery-scheme-indicator",
                )
            Surface(
                modifier =
                    Modifier
                        .width(width)
                        .height(6.dp),
                shape = CircleShape,
                color =
                    if (index == currentPage) {
                        MiuixTheme.colorScheme.primary
                    } else {
                        MiuixTheme.colorScheme.disabledOnSecondaryVariant
                    },
            ) {}
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
    val entry =
        DropdownEntry(
            items =
                listOf(
                    DropdownItem(
                        text = stringResource(R.string.battery_custom_scheme_rename),
                        onClick = onRename,
                    ),
                    DropdownItem(
                        text = stringResource(R.string.battery_custom_scheme_copy),
                        enabled = canCopy,
                        onClick = onCopy,
                    ),
                    DropdownItem(
                        text = stringResource(R.string.battery_custom_scheme_delete),
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
                imageVector = MiuixIcons.More,
                contentDescription = stringResource(R.string.battery_custom_scheme_manage),
            )
        }
    }
}
