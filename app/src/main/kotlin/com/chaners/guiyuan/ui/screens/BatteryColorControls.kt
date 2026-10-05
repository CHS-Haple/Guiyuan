package com.chaners.guiyuan.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.BATTERY_COLOR_SCHEME_CUSTOM_MAX
import com.chaners.guiyuan.settings.BATTERY_COLOR_SCHEME_HYPEROS_KEY
import com.chaners.guiyuan.settings.BatteryBuiltInColorScheme
import com.chaners.guiyuan.settings.BatteryColorSchemeEntry
import com.chaners.guiyuan.settings.BatteryColorSchemeLibrary
import com.chaners.guiyuan.settings.BatteryColorSchemeLibraryRepository
import com.chaners.guiyuan.settings.BatteryColorSchemeSource
import com.chaners.guiyuan.settings.BatteryCustomColorScheme
import com.chaners.guiyuan.settings.CombinedStatusBatteryColorSlot
import com.chaners.guiyuan.settings.batteryBuiltInColor
import com.chaners.guiyuan.settings.batterySchemeEntryColor
import com.chaners.guiyuan.settings.customSchemeKey
import com.chaners.guiyuan.settings.limitBatteryCustomSchemeNameInput
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.FloatingActionButtonDefaults
import top.yukonga.miuix.kmp.basic.HsvHueSlider
import top.yukonga.miuix.kmp.basic.HsvSaturationSlider
import top.yukonga.miuix.kmp.basic.HsvValueSlider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.IconButtonDefaults
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SnackbarDefaults
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.basic.drawCheckerboard
import top.yukonga.miuix.kmp.color.api.toHsv
import top.yukonga.miuix.kmp.color.space.Hsv
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PagerGestureNestedScrollConnection
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.PagerNavigationSpringSpec
import top.yukonga.miuix.kmp.utils.pagerGestureOverride
import top.yukonga.miuix.kmp.utils.springAnimateToPage
import kotlin.math.roundToInt

private val COMMON_BATTERY_COLORS =
    listOf(
        0xFFFF3B30.toInt(),
        0xFFFF9500.toInt(),
        0xFFFFCC00.toInt(),
        0xFF34C759.toInt(),
        0xFF32ADE6.toInt(),
        0xFF007AFF.toInt(),
        0xFF5856D6.toInt(),
        0xFFAF52DE.toInt(),
        0xFFFF2D55.toInt(),
        0xFF8E8E93.toInt(),
    )

private val BATTERY_COLOR_PREVIEW_SLOTS = CombinedStatusBatteryColorSlot.entries

private const val BATTERY_COLOR_SHEET_HEIGHT_FRACTION = 0.84f
private val BATTERY_SCHEME_VERTICAL_GAP = 12.dp

private sealed interface BatterySchemePage {
    val key: String

    data class BuiltIn(
        val scheme: BatteryBuiltInColorScheme,
    ) : BatterySchemePage {
        override val key: String = scheme.key
    }

    data class Custom(
        val scheme: BatteryCustomColorScheme,
    ) : BatterySchemePage {
        override val key: String = scheme.key
    }

    data object Add : BatterySchemePage {
        override val key: String = "add"
    }
}

@Composable
internal fun BatteryColorPreference(
    library: BatteryColorSchemeLibrary,
    enabled: Boolean,
    holdDownState: Boolean,
    onClick: () -> Unit,
) {
    ArrowPreference(
        title = stringResource(R.string.battery_colors),
        summary =
            stringResource(
                R.string.battery_color_scheme_summary,
                batterySchemeDisplayName(library, library.activeSchemeKey),
            ),
        enabled = enabled,
        holdDownState = holdDownState,
        onClick = onClick,
        endActions = {
            BatterySchemePreviewStrip(
                page = schemePageForKey(library, library.activeSchemeKey),
            )
        },
    )
}

@Composable
internal fun BatteryColorBottomSheet(
    show: Boolean,
    library: BatteryColorSchemeLibrary,
    repository: BatteryColorSchemeLibraryRepository,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val navPager = rememberPagerState(initialPage = 0, pageCount = { 2 })
    var selectedCustomId by remember { mutableStateOf<Int?>(null) }
    var selectedSlot by remember { mutableStateOf<CombinedStatusBatteryColorSlot?>(null) }
    var requestedSchemeKey by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var pendingCreateSourceKey by remember {
        mutableStateOf(BATTERY_COLOR_SCHEME_HYPEROS_KEY)
    }
    var pendingCreateSlot by remember {
        mutableStateOf<CombinedStatusBatteryColorSlot?>(null)
    }
    var renameCustomId by remember { mutableStateOf<Int?>(null) }
    var deleteCustomId by remember { mutableStateOf<Int?>(null) }
    val nextCustomId = repository.nextAvailableCustomId()
    val nextCustomName =
        nextCustomId?.let {
            stringResource(R.string.battery_custom_scheme_default_name, it)
        }
    val inDetail = navPager.currentPage == 1
    val sheetTitle =
        if (inDetail && selectedSlot != null) {
            stringResource(
                R.string.battery_mode_editor_title,
                stringResource(batteryColorSlotLabel(requireNotNull(selectedSlot))),
            )
        } else {
            stringResource(R.string.battery_colors)
        }

    OverlayBottomSheet(
        show = show,
        modifier = Modifier.fillMaxHeight(BATTERY_COLOR_SHEET_HEIGHT_FRACTION),
        title = sheetTitle,
        backgroundColor =
            if (inDetail) {
                MiuixTheme.colorScheme.surface
            } else {
                MiuixTheme.colorScheme.background
            },
        insideMargin =
            if (inDetail) {
                BottomSheetDefaults.insideMargin
            } else {
                DpSize(0.dp, 0.dp)
            },
        startAction =
            if (inDetail) {
                {
                    TooltipBox(text = stringResource(R.string.back)) {
                        IconButton(
                            onClick = {
                                scope.launch { navPager.springAnimateToPage(0) }
                            },
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    }
                }
            } else {
                {
                    Spacer(
                        modifier =
                            Modifier.size(
                                width = IconButtonDefaults.MinWidth,
                                height = IconButtonDefaults.MinHeight,
                            ),
                    )
                }
            },
        onDismissRequest = {
            if (inDetail) {
                scope.launch { navPager.springAnimateToPage(0) }
            } else {
                onDismiss()
            }
        },
    ) {
        HorizontalPager(
            state = navPager,
            modifier =
                Modifier
                    .fillMaxSize()
                    .clipToBounds(),
            userScrollEnabled = false,
            verticalAlignment = Alignment.Top,
        ) { page ->
            if (page == 0) {
                BatterySchemeOverview(
                    library = library,
                    requestedSchemeKey = requestedSchemeKey,
                    onRequestedSchemeHandled = { requestedSchemeKey = null },
                    canCreateCustom = nextCustomId != null,
                    nextCustomName = nextCustomName,
                    onApplyScheme = repository::activateScheme,
                    onOpenBuiltInSlot = { scheme, slot ->
                        pendingCreateSourceKey = scheme.key
                        pendingCreateSlot = slot
                        showCreateDialog = true
                    },
                    onOpenCustomSlot = { id, slot ->
                        selectedCustomId = id
                        selectedSlot = slot
                        scope.launch { navPager.springAnimateToPage(1) }
                    },
                    onAdd = {
                        pendingCreateSourceKey = BATTERY_COLOR_SCHEME_HYPEROS_KEY
                        pendingCreateSlot = null
                        showCreateDialog = true
                    },
                    onRenameCustom = { renameCustomId = it },
                    onCopyCustom = { custom ->
                        val name = nextCustomName
                        if (name != null) {
                            repository.createCustom(name, custom.key)?.let { id ->
                                requestedSchemeKey = customSchemeKey(id)
                            }
                        }
                    },
                    onDeleteCustom = { deleteCustomId = it },
                )
            } else {
                val custom = selectedCustomId?.let(library::customById)
                val slot = selectedSlot
                if (custom != null && slot != null) {
                    BatteryCustomModeEditor(
                        custom = custom,
                        slot = slot,
                        onSourceChange = { source ->
                            repository.setCustomSource(custom.id, slot, source)
                        },
                        onColorChange = { color ->
                            repository.setCustomColor(custom.id, slot, color)
                        },
                    )
                }
            }
        }
    }

    BatteryCreateSchemeDialog(
        show = showCreateDialog,
        nextId = nextCustomId,
        onDismiss = {
            showCreateDialog = false
            pendingCreateSourceKey = BATTERY_COLOR_SCHEME_HYPEROS_KEY
            pendingCreateSlot = null
        },
        onCreate = { name ->
            val sourceKey = pendingCreateSourceKey
            val targetSlot = pendingCreateSlot
            showCreateDialog = false
            pendingCreateSourceKey = BATTERY_COLOR_SCHEME_HYPEROS_KEY
            pendingCreateSlot = null
            repository.createCustom(
                name = name,
                fromSchemeKey = sourceKey,
            )?.let { id ->
                requestedSchemeKey = customSchemeKey(id)
                if (targetSlot != null) {
                    selectedCustomId = id
                    selectedSlot = targetSlot
                    scope.launch { navPager.springAnimateToPage(1) }
                }
            }
        },
    )

    BatteryRenameSchemeDialog(
        scheme = renameCustomId?.let(library::customById),
        onDismiss = { renameCustomId = null },
        onRename = { id, name ->
            renameCustomId = null
            repository.renameCustom(id, name)
        },
    )

    val deleting = deleteCustomId?.let(library::customById)
    OverlayDialog(
        title = stringResource(R.string.battery_custom_scheme_delete),
        summary =
            deleting?.let {
                stringResource(
                    R.string.battery_custom_scheme_delete_summary,
                    customSchemeName(it),
                )
            },
        show = deleting != null,
        onDismissRequest = { deleteCustomId = null },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(
                text = stringResource(R.string.cancel),
                modifier = Modifier.weight(1f),
                onClick = { deleteCustomId = null },
            )
            Spacer(Modifier.width(20.dp))
            TextButton(
                text = stringResource(R.string.battery_custom_scheme_delete),
                modifier = Modifier.weight(1f),
                colors =
                    ButtonDefaults.textButtonColors(
                        textColor = MiuixTheme.colorScheme.error,
                    ),
                onClick = {
                    deleting?.let { repository.deleteCustom(it.id) }
                    deleteCustomId = null
                },
            )
        }
    }
}

@Composable
private fun BatterySchemeOverview(
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

@Composable
private fun BatteryCustomModeEditor(
    custom: BatteryCustomColorScheme,
    slot: CombinedStatusBatteryColorSlot,
    onSourceChange: (BatteryColorSchemeSource) -> Unit,
    onColorChange: (Int) -> Unit,
) {
    val entry = custom.entries.entryFor(slot)
    val resolved = batterySchemeEntryColor(entry, slot)
    val sourceOptions = BatteryColorSchemeSource.entries
    val sourceLabels =
        listOf(
            batterySourceLabel(BatteryColorSchemeSource.HYPEROS),
            batterySourceLabel(BatteryColorSchemeSource.IOS),
            batterySourceLabel(BatteryColorSchemeSource.LOW_SATURATION),
            batterySourceLabel(BatteryColorSchemeSource.FOLLOW_SYSTEM),
            batterySourceLabel(BatteryColorSchemeSource.CUSTOM),
        )
    val selectedSourceIndex = sourceOptions.indexOf(entry.source).coerceAtLeast(0)
    val seed = batteryColorEditorSeed(entry, slot)
    val visuallyInactive = entry.source == BatteryColorSchemeSource.FOLLOW_SYSTEM

    var editingColor by remember(custom.id, slot, entry.source, seed) {
        mutableStateOf(seed?.or(0xFF000000.toInt()))
    }
    var hexText by remember(custom.id, slot, editingColor) {
        mutableStateOf(
            editingColor
                ?.let(::batteryColorHex)
                ?.removePrefix("#")
                .orEmpty(),
        )
    }
    val rgb = editingColor?.let(::batteryColorRgb)
    var redText by remember(custom.id, slot, editingColor) {
        mutableStateOf(rgb?.first?.toString().orEmpty())
    }
    var greenText by remember(custom.id, slot, editingColor) {
        mutableStateOf(rgb?.second?.toString().orEmpty())
    }
    var blueText by remember(custom.id, slot, editingColor) {
        mutableStateOf(rgb?.third?.toString().orEmpty())
    }

    fun applyColor(color: Int) {
        val opaque = color or 0xFF000000.toInt()
        editingColor = opaque
        hexText = batteryColorHex(opaque).removePrefix("#")
        val value = batteryColorRgb(opaque)
        redText = value.first.toString()
        greenText = value.second.toString()
        blueText = value.third.toString()
        // setCustomColor is the single copy-on-write path: an actual edit promotes this slot
        // to CUSTOM while simply browsing FOLLOW_SYSTEM keeps the runtime source untouched.
        onColorChange(opaque)
    }

    val hsv = editingColor?.let { Color(it).toHsv() }
    val currentValueText =
        when {
            resolved != null -> batteryColorHex(resolved)
            entry.source == BatteryColorSchemeSource.CUSTOM ->
                stringResource(R.string.battery_color_custom_unset)
            else -> stringResource(R.string.battery_color_follow_inversion)
        }
    val inactiveTitleColor =
        if (visuallyInactive) {
            MiuixTheme.colorScheme.disabledOnSecondaryVariant
        } else {
            MiuixTheme.colorScheme.onBackgroundVariant
        }
    val inactiveFieldColors =
        if (visuallyInactive) {
            TextFieldDefaults.textFieldColors(
                backgroundColor = MiuixTheme.colorScheme.disabledSecondaryVariant,
                labelColor = MiuixTheme.colorScheme.disabledOnSecondaryVariant,
                borderColor = MiuixTheme.colorScheme.disabledPrimary,
            )
        } else {
            TextFieldDefaults.textFieldColors()
        }
    val inactiveFieldTextStyle =
        MiuixTheme.textStyles.main.copy(
            color =
                if (visuallyInactive) {
                    MiuixTheme.colorScheme.disabledOnSecondaryVariant
                } else {
                    MiuixTheme.colorScheme.onBackground
                },
        )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            OverlayDropdownPreference(
            items = sourceLabels,
            selectedIndex = selectedSourceIndex,
            title = stringResource(R.string.battery_color_source),
            summary = currentValueText,
            startAction = {
                if (resolved == null) {
                    BatteryColorMosaic(size = 24.dp)
                } else {
                    BatteryColorDot(
                        color = resolved,
                        size = 24.dp,
                    )
                }
            },
            onSelectedIndexChange = { index ->
                sourceOptions.getOrNull(index)?.let(onSourceChange)
            },
        )
        }

        BatterySheetSmallTitle(
            text = stringResource(R.string.battery_color_common),
            textColor = inactiveTitleColor,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            insideMargin = PaddingValues(16.dp),
        ) {
            COMMON_BATTERY_COLORS.chunked(5).forEachIndexed { index, colors ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (index == 0) 12.dp else 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    colors.forEach { color ->
                        BatteryCommonColorButton(
                            color = color,
                            selected =
                                entry.source == BatteryColorSchemeSource.CUSTOM &&
                                    editingColor == color,
                            visuallyInactive = visuallyInactive,
                            onClick = { applyColor(color) },
                        )
                    }
                }
            }
        }

        BatterySheetSmallTitle(
            text = stringResource(R.string.battery_color_full_adjustment),
            textColor = inactiveTitleColor,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (hsv != null) {
                BatteryHsvAdjustmentRow(
                    title = stringResource(R.string.battery_color_hue),
                    valueText = "${hsv.h.roundToInt()}°",
                    visuallyInactive = visuallyInactive,
                ) {
                    HsvHueSlider(
                        currentHue = hsv.h,
                        onHueChanged = { fraction ->
                            applyColor(
                                Hsv(fraction * 360f, hsv.s, hsv.v).toColor().toArgb(),
                            )
                        },
                    )
                }
                BatteryHsvAdjustmentRow(
                    title = stringResource(R.string.battery_color_saturation),
                    valueText = "${hsv.s.roundToInt()}%",
                    visuallyInactive = visuallyInactive,
                ) {
                    HsvSaturationSlider(
                        currentHue = hsv.h,
                        currentSaturation = hsv.s / 100f,
                        onSaturationChanged = { saturation ->
                            applyColor(
                                Hsv(hsv.h, saturation * 100f, hsv.v).toColor().toArgb(),
                            )
                        },
                    )
                }
                BatteryHsvAdjustmentRow(
                    title = stringResource(R.string.battery_color_brightness),
                    valueText = "${hsv.v.roundToInt()}%",
                    visuallyInactive = visuallyInactive,
                ) {
                    HsvValueSlider(
                        currentHue = hsv.h,
                        currentSaturation = hsv.s / 100f,
                        currentValue = hsv.v / 100f,
                        onValueChanged = { value ->
                            applyColor(
                                Hsv(hsv.h, hsv.s, value * 100f).toColor().toArgb(),
                            )
                        },
                    )
                }
            } else {
                BasicComponent(
                    title = stringResource(R.string.battery_color_no_fixed_color),
                    summary = stringResource(R.string.battery_color_no_fixed_color_summary),
                    enabled = false,
                )
            }
        }

        BatterySheetSmallTitle(
            text = stringResource(R.string.battery_color_precise_input),
            textColor = inactiveTitleColor,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            insideMargin = PaddingValues(16.dp),
        ) {
            TextField(
                value = hexText,
                onValueChange = { raw ->
                    val normalized =
                        raw.removePrefix("#")
                            .uppercase()
                            .filter { it.isDigit() || it in 'A'..'F' }
                    if (normalized.length <= 6) {
                        hexText = normalized
                        batteryColorFromHex(normalized)?.let(::applyColor)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.battery_color_hex),
                singleLine = true,
                colors = inactiveFieldColors,
                textStyle = inactiveFieldTextStyle,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextField(
                    value = redText,
                    onValueChange = { raw ->
                        if (raw.length <= 3 && raw.all(Char::isDigit)) {
                            redText = raw
                            batteryColorFromRgb(redText, greenText, blueText)?.let(::applyColor)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = "R",
                    singleLine = true,
                    colors = inactiveFieldColors,
                    textStyle = inactiveFieldTextStyle,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                TextField(
                    value = greenText,
                    onValueChange = { raw ->
                        if (raw.length <= 3 && raw.all(Char::isDigit)) {
                            greenText = raw
                            batteryColorFromRgb(redText, greenText, blueText)?.let(::applyColor)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = "G",
                    singleLine = true,
                    colors = inactiveFieldColors,
                    textStyle = inactiveFieldTextStyle,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                TextField(
                    value = blueText,
                    onValueChange = { raw ->
                        if (raw.length <= 3 && raw.all(Char::isDigit)) {
                            blueText = raw
                            batteryColorFromRgb(redText, greenText, blueText)?.let(::applyColor)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = "B",
                    singleLine = true,
                    colors = inactiveFieldColors,
                    textStyle = inactiveFieldTextStyle,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        }
    }
}

@Composable
private fun BatteryHsvAdjustmentRow(
    title: String,
    valueText: String,
    visuallyInactive: Boolean,
    slider: @Composable () -> Unit,
) {
    BasicComponent(
        title = title,
        enabled = !visuallyInactive,
        endActions = {
            Text(
                text = valueText,
                fontSize = MiuixTheme.textStyles.body2.fontSize,
                color =
                    if (visuallyInactive) {
                        MiuixTheme.colorScheme.disabledOnSecondaryVariant
                    } else {
                        MiuixTheme.colorScheme.onSurfaceVariantActions
                    },
                modifier = Modifier.padding(end = 8.dp),
            )
        },
        bottomAction = slider,
    )
}

@Composable
private fun BatteryCreateSchemeDialog(
    show: Boolean,
    nextId: Int?,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    val defaultName =
        nextId?.let {
            stringResource(R.string.battery_custom_scheme_default_name, it)
        }.orEmpty()
    var name by remember(show, nextId) { mutableStateOf(defaultName) }

    OverlayDialog(
        title = stringResource(R.string.battery_custom_scheme_new),
        show = show && nextId != null,
        onDismissRequest = onDismiss,
    ) {
        Column {
            TextField(
                value = name,
                onValueChange = { name = limitBatteryCustomSchemeNameInput(it) },
                label = stringResource(R.string.battery_custom_scheme_name),
                singleLine = true,
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    text = stringResource(R.string.cancel),
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss,
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = stringResource(R.string.battery_custom_scheme_create),
                    modifier = Modifier.weight(1f),
                    enabled = name.trim().isNotEmpty(),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = {
                        onCreate(name.trim())
                    },
                )
            }
        }
    }
}

@Composable
private fun BatteryRenameSchemeDialog(
    scheme: BatteryCustomColorScheme?,
    onDismiss: () -> Unit,
    onRename: (Int, String) -> Unit,
) {
    val currentName = scheme?.let { customSchemeName(it) }.orEmpty()
    var name by remember(scheme?.id, currentName) { mutableStateOf(currentName) }
    OverlayDialog(
        title = stringResource(R.string.battery_custom_scheme_rename),
        show = scheme != null,
        onDismissRequest = onDismiss,
    ) {
        if (scheme != null) {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = limitBatteryCustomSchemeNameInput(it) },
                    label = stringResource(R.string.battery_custom_scheme_name),
                    singleLine = true,
                )
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(
                        text = stringResource(R.string.cancel),
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss,
                    )
                    Spacer(Modifier.width(20.dp))
                    TextButton(
                        text = stringResource(R.string.confirm),
                        modifier = Modifier.weight(1f),
                        enabled =
                            name.trim().isNotEmpty() &&
                                name.trim() != currentName,
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = {
                            onRename(scheme.id, name.trim())
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BatterySheetSmallTitle(
    text: String,
    textColor: Color? = null,
) {
    SmallTitle(
        text = text,
        textColor = textColor ?: MiuixTheme.colorScheme.onBackgroundVariant,
    )
}

@Composable
private fun BatterySchemePreviewStrip(
    page: BatterySchemePage?,
    size: Dp = 12.dp,
    spacing: Dp = 6.dp,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BATTERY_COLOR_PREVIEW_SLOTS.forEach { slot ->
            val color =
                when (page) {
                    is BatterySchemePage.BuiltIn ->
                        batteryBuiltInColor(page.scheme, slot)
                    is BatterySchemePage.Custom ->
                        batterySchemeEntryColor(page.scheme.entries.entryFor(slot), slot)
                    else -> null
                }
            if (color == null) {
                BatteryColorMosaic(size = size)
            } else {
                BatteryColorDot(
                    color = color,
                    size = size,
                )
            }
        }
    }
}

@Composable
private fun batteryCommonColorName(color: Int): String =
    stringResource(
        when (color) {
            0xFFFF3B30.toInt() -> R.string.battery_common_color_red
            0xFFFF9500.toInt() -> R.string.battery_common_color_orange
            0xFFFFCC00.toInt() -> R.string.battery_common_color_yellow
            0xFF34C759.toInt() -> R.string.battery_common_color_green
            0xFF32ADE6.toInt() -> R.string.battery_common_color_light_blue
            0xFF007AFF.toInt() -> R.string.battery_common_color_blue
            0xFF5856D6.toInt() -> R.string.battery_common_color_indigo
            0xFFAF52DE.toInt() -> R.string.battery_common_color_purple
            0xFFFF2D55.toInt() -> R.string.battery_common_color_pink
            else -> R.string.battery_common_color_gray
        },
    )

@Composable
private fun BatteryCommonColorButton(
    color: Int,
    selected: Boolean,
    visuallyInactive: Boolean,
    onClick: () -> Unit,
) {
    TooltipBox(text = batteryCommonColorName(color)) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = Color.Transparent,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Surface(
                        modifier = Modifier.size(34.dp),
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = BorderStroke(3.dp, Color.White),
                        shadowElevation = 2.dp,
                    ) {}
                }
                BatteryColorDot(
                    color = color,
                    size = 28.dp,
                    visuallyInactive = visuallyInactive,
                )
            }
        }
    }
}

private const val BATTERY_SWATCH_BORDER_RATIO = 1f / 14f
private const val BATTERY_SWATCH_BORDER_ALPHA = 0.26f

@Composable
private fun batterySwatchBorder(size: Dp): BorderStroke =
    BorderStroke(
        width = (size.value * BATTERY_SWATCH_BORDER_RATIO).dp,
        color = MiuixTheme.colorScheme.onSurface.copy(alpha = BATTERY_SWATCH_BORDER_ALPHA),
    )

@Composable
private fun BatteryColorDot(
    color: Int,
    size: Dp = 20.dp,
    visuallyInactive: Boolean = false,
) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color =
            if (visuallyInactive) {
                Color(color).copy(alpha = 0.45f)
            } else {
                Color(color)
            },
        border =
            if (visuallyInactive) {
                BorderStroke(
                    width = (size.value * BATTERY_SWATCH_BORDER_RATIO).dp,
                    color = MiuixTheme.colorScheme.disabledOnSecondaryVariant,
                )
            } else {
                batterySwatchBorder(size)
            },
    ) {}
}

@Composable
private fun BatteryColorMosaic(
    size: Dp = 20.dp,
) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = Color.Transparent,
        border = batterySwatchBorder(size),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .drawCheckerboard(
                        cellSizeDp = 3.dp,
                        lightColor = MiuixTheme.colorScheme.surfaceContainer,
                        darkColor = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.28f),
                    ),
        )
    }
}

@Composable
private fun batterySchemeDisplayName(
    library: BatteryColorSchemeLibrary,
    key: String,
): String =
    BatteryBuiltInColorScheme.fromKey(key)?.let { batteryBuiltInName(it) }
        ?: library.customByKey(key)?.let { customSchemeName(it) }
        ?: batteryBuiltInName(BatteryBuiltInColorScheme.HYPEROS)

@Composable
private fun batteryBuiltInName(scheme: BatteryBuiltInColorScheme): String =
    when (scheme) {
        BatteryBuiltInColorScheme.HYPEROS ->
            stringResource(R.string.battery_color_preset_hyperos)
        BatteryBuiltInColorScheme.IOS ->
            stringResource(R.string.battery_color_preset_ios)
        BatteryBuiltInColorScheme.LOW_SATURATION ->
            stringResource(R.string.battery_color_preset_recommended)
    }

@Composable
private fun customSchemeName(scheme: BatteryCustomColorScheme): String =
    scheme.name.ifBlank {
        stringResource(R.string.battery_custom_scheme_default_name, scheme.id)
    }

@Composable
private fun batterySourceLabel(source: BatteryColorSchemeSource): String =
    when (source) {
        BatteryColorSchemeSource.HYPEROS ->
            stringResource(R.string.battery_color_preset_hyperos)
        BatteryColorSchemeSource.IOS ->
            stringResource(R.string.battery_color_preset_ios)
        BatteryColorSchemeSource.LOW_SATURATION ->
            stringResource(R.string.battery_color_preset_recommended)
        BatteryColorSchemeSource.FOLLOW_SYSTEM ->
            stringResource(R.string.battery_color_follow_inversion)
        BatteryColorSchemeSource.CUSTOM ->
            stringResource(R.string.battery_color_source_custom)
    }

@Composable
private fun batterySourceValue(
    entry: BatteryColorSchemeEntry,
    slot: CombinedStatusBatteryColorSlot,
): String =
    batterySchemeEntryColor(entry, slot)?.let(::batteryColorHex)
        ?: stringResource(R.string.battery_color_follow_inversion)

@StringRes
private fun batteryColorSlotLabel(slot: CombinedStatusBatteryColorSlot): Int =
    when (slot) {
        CombinedStatusBatteryColorSlot.NORMAL -> R.string.battery_mode_normal
        CombinedStatusBatteryColorSlot.POWER_SAVE -> R.string.battery_mode_power_save
        CombinedStatusBatteryColorSlot.PERFORMANCE -> R.string.battery_mode_performance
        CombinedStatusBatteryColorSlot.SUPER_POWER_SAVE -> R.string.battery_mode_super_power_save
        CombinedStatusBatteryColorSlot.CHARGING -> R.string.battery_mode_charging
        CombinedStatusBatteryColorSlot.LOW -> R.string.battery_mode_low
    }

private fun schemePageForKey(
    library: BatteryColorSchemeLibrary,
    key: String,
): BatterySchemePage? =
    BatteryBuiltInColorScheme.fromKey(key)?.let { BatterySchemePage.BuiltIn(it) }
        ?: library.customByKey(key)?.let { BatterySchemePage.Custom(it) }

internal fun batteryColorEditorSeed(
    entry: BatteryColorSchemeEntry,
    slot: CombinedStatusBatteryColorSlot,
): Int? =
    batterySchemeEntryColor(entry, slot)
        ?: entry.customColor

internal fun batteryColorHex(color: Int): String =
    "#%06X".format(color and 0x00FFFFFF)

internal fun batteryColorFromHex(value: String): Int? {
    val normalized = value.removePrefix("#")
    if (normalized.length != 6) return null
    return normalized.toLongOrNull(16)
        ?.toInt()
        ?.or(0xFF000000.toInt())
}

internal fun batteryColorRgb(color: Int): Triple<Int, Int, Int> =
    Triple(
        (color shr 16) and 0xFF,
        (color shr 8) and 0xFF,
        color and 0xFF,
    )

internal fun batteryColorFromRgb(
    red: String,
    green: String,
    blue: String,
): Int? {
    val r = red.toIntOrNull()?.takeIf { it in 0..255 } ?: return null
    val g = green.toIntOrNull()?.takeIf { it in 0..255 } ?: return null
    val b = blue.toIntOrNull()?.takeIf { it in 0..255 } ?: return null
    return 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
}
