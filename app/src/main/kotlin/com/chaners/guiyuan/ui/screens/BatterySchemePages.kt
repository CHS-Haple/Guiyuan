package com.chaners.guiyuan.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.BatteryBuiltInColorScheme
import com.chaners.guiyuan.settings.BatteryColorSchemeLibrary
import com.chaners.guiyuan.settings.BatteryColorSchemeSource
import com.chaners.guiyuan.settings.BatteryCustomColorScheme
import com.chaners.guiyuan.settings.BatteryColorSlot
import com.chaners.guiyuan.settings.batteryBuiltInColor
import com.chaners.guiyuan.settings.batterySchemeEntryColor
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.TabRow
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
    val labels =
        listOf(
            batteryBuiltInName(BatteryBuiltInColorScheme.HYPEROS),
            batteryBuiltInName(BatteryBuiltInColorScheme.IOS),
            batteryBuiltInName(BatteryBuiltInColorScheme.LOW_SATURATION),
        ) + library.customSchemes.map { customSchemeName(it) } +
            stringResource(R.string.battery_custom_scheme_add_page_title)
    val initial =
        pages.indexOfFirst { it.key == library.activeSchemeKey }
            .takeIf { it >= 0 }
            ?: 0
    val pagerState = rememberPagerState(initialPage = initial, pageCount = { pages.size })
    val scope = rememberCoroutineScope()
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

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            tabs = labels,
            selectedTabIndex = pagerState.currentPage.coerceIn(0, labels.lastIndex),
            onTabSelected = { index ->
                scope.launch { pagerState.springAnimateToPage(index) }
            },
            modifier = Modifier.padding(horizontal = BottomSheetDefaults.insideMargin.width),
        )
        Spacer(Modifier.height(12.dp))
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
            contentPadding = PaddingValues(horizontal = BottomSheetDefaults.insideMargin.width),
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
                        onSlotClick = { slot -> onOpenBuiltInSlot(page.scheme, slot) },
                    )
                is BatterySchemePage.Custom ->
                    BatterySchemePageContent(
                        name = customSchemeName(page.scheme),
                        builtIn = null,
                        custom = page.scheme,
                        isActive = library.activeSchemeKey == page.key,
                        canCreateCustom = canCreateCustom,
                        onApply = { onApplyScheme(page.key) },
                        onSlotClick = { slot -> onOpenCustomSlot(page.scheme.id, slot) },
                        onRename = { onRenameCustom(page.scheme.id) },
                        onCopy = { onCopyCustom(page.scheme) },
                        onDelete = { onDeleteCustom(page.scheme.id) },
                    )
                BatterySchemePage.Add ->
                    BatteryAddSchemePage(
                        count = library.customSchemes.size,
                        enabled = canCreateCustom,
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
    onRename: (() -> Unit)? = null,
    onCopy: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surface),
            insideMargin = PaddingValues(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = name,
                    style = MiuixTheme.textStyles.title2,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isActive) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MiuixTheme.colorScheme.primary.copy(alpha = 0.13f),
                    ) {
                        Text(
                            text = stringResource(R.string.battery_color_scheme_applied),
                            color = MiuixTheme.colorScheme.primary,
                            style = MiuixTheme.textStyles.footnote2,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
                if (custom != null && onRename != null && onCopy != null && onDelete != null) {
                    BatterySchemeManageMenu(
                        canCopy = canCreateCustom,
                        onRename = onRename,
                        onCopy = onCopy,
                        onDelete = onDelete,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                BatterySchemePreviewStrip(
                    page =
                        if (builtIn != null) BatterySchemePage.BuiltIn(builtIn)
                        else custom?.let(BatterySchemePage::Custom),
                    size = 30.dp,
                    spacing = 10.dp,
                )
            }
            if (!isActive) {
                Spacer(Modifier.height(18.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Button(
                        onClick = onApply,
                        colors = ButtonDefaults.buttonColorsPrimary(),
                    ) {
                        Text(stringResource(R.string.battery_color_scheme_apply))
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SmallTitle(text = stringResource(R.string.battery_scheme_modes))
        Card(modifier = Modifier.fillMaxWidth()) {
            BatteryColorSlot.entries.forEach { slot ->
                val color =
                    when {
                        builtIn != null -> batteryBuiltInColor(builtIn, slot)
                        custom != null -> batterySchemeEntryColor(custom.entries.entryFor(slot), slot)
                        else -> null
                    }
                val followsSystem =
                    when {
                        builtIn != null -> color == null
                        custom != null ->
                            custom.entries.entryFor(slot).source ==
                                BatteryColorSchemeSource.FOLLOW_SYSTEM || color == null
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
        Spacer(Modifier.height(20.dp))
    }
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
    onClick: () -> Unit,
) {
    val primary = MiuixTheme.colorScheme.primary
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(min = 360.dp),
            colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surface),
            insideMargin = PaddingValues(20.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(
                        if (enabled) R.string.battery_custom_scheme_add_page_title
                        else R.string.battery_custom_scheme_limit
                    ),
                    style = MiuixTheme.textStyles.title2,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        if (enabled) R.string.battery_scheme_new_summary
                        else R.string.battery_scheme_full_summary
                    ),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))
                if (enabled) {
                    FloatingActionButton(
                        onClick = onClick,
                        containerColor = primary.copy(alpha = 0.13f),
                        shadowElevation = 0.dp,
                        minWidth = 96.dp,
                        minHeight = 96.dp,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Add,
                            contentDescription = stringResource(R.string.battery_custom_scheme_create),
                            modifier = Modifier.size(40.dp),
                            tint = primary,
                        )
                    }
                } else {
                    Surface(
                        modifier = Modifier.size(96.dp),
                        shape = CircleShape,
                        color = MiuixTheme.colorScheme.surfaceContainer,
                        shadowElevation = 0.dp,
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "$count / 5",
                                style = MiuixTheme.textStyles.title2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.battery_scheme_capacity),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Text(
                        text = "$count / 5",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    repeat(5) { i ->
                        Surface(
                            modifier = Modifier.weight(1f).height(7.dp),
                            shape = CircleShape,
                            color =
                                if (i < count) primary
                                else MiuixTheme.colorScheme.surfaceContainer,
                        ) {}
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text =
                        if (enabled) stringResource(R.string.battery_scheme_remaining, 5 - count)
                        else stringResource(R.string.battery_scheme_full_hint),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
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
