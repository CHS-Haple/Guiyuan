package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.BATTERY_COLOR_SCHEME_HYPEROS_KEY
import com.chaners.guiyuan.settings.BatteryBuiltInColorScheme
import com.chaners.guiyuan.settings.BatteryColorSchemeLibrary
import com.chaners.guiyuan.settings.BatteryColorSchemeLibraryRepository
import com.chaners.guiyuan.settings.BatteryCustomColorScheme
import com.chaners.guiyuan.settings.BatteryColorSlot
import com.chaners.guiyuan.settings.customSchemeKey
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.IconButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal val COMMON_BATTERY_COLORS =
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

internal val BATTERY_COLOR_PREVIEW_SLOTS = BatteryColorSlot.entries

private const val BATTERY_COLOR_SHEET_HEIGHT_FRACTION = 0.84f
internal val BATTERY_SCHEME_VERTICAL_GAP = 12.dp

internal sealed interface BatterySchemePage {
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
    var selectedCustomId by remember { mutableStateOf<Int?>(null) }
    var selectedSlot by remember { mutableStateOf<BatteryColorSlot?>(null) }
    var showDetail by remember { mutableStateOf(false) }
    var requestedSchemeKey by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var pendingCreateSourceKey by remember {
        mutableStateOf(BATTERY_COLOR_SCHEME_HYPEROS_KEY)
    }
    var pendingCreateSlot by remember {
        mutableStateOf<BatteryColorSlot?>(null)
    }
    var renameCustomId by remember { mutableStateOf<Int?>(null) }
    var deleteCustomId by remember { mutableStateOf<Int?>(null) }

    val nextCustomId = repository.nextAvailableCustomId()
    val nextCustomName =
        nextCustomId?.let {
            stringResource(R.string.battery_custom_scheme_default_name, it)
        }
    val detailCustom = selectedCustomId?.let(library::customById)
    val detailSlot = selectedSlot
    val detailVisible =
        show &&
            showDetail &&
            detailCustom != null &&
            detailSlot != null

    LaunchedEffect(show) {
        if (!show) {
            showDetail = false
            selectedCustomId = null
            selectedSlot = null
        }
    }

    OverlayBottomSheet(
        show = show,
        modifier = Modifier.fillMaxHeight(BATTERY_COLOR_SHEET_HEIGHT_FRACTION),
        title = stringResource(R.string.battery_colors),
        backgroundColor = MiuixTheme.colorScheme.background,
        insideMargin = DpSize(0.dp, 0.dp),
        startAction = {
            Spacer(
                modifier =
                    Modifier.size(
                        width = IconButtonDefaults.MinWidth,
                        height = IconButtonDefaults.MinHeight,
                    ),
            )
        },
        allowDismiss = !detailVisible,
        onDismissRequest = {
            if (!detailVisible) {
                onDismiss()
            }
        },
    ) {
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
                showDetail = true
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
    }

    OverlayBottomSheet(
        show = detailVisible,
        modifier = Modifier.fillMaxHeight(BATTERY_COLOR_SHEET_HEIGHT_FRACTION),
        title =
            detailSlot?.let { slot ->
                stringResource(
                    R.string.battery_mode_editor_title,
                    stringResource(batteryColorSlotLabel(slot)),
                )
            },
        backgroundColor = MiuixTheme.colorScheme.surface,
        insideMargin = BottomSheetDefaults.insideMargin,
        enableWindowDim = false,
        startAction = {
            TooltipBox(text = stringResource(R.string.back)) {
                IconButton(
                    onClick = { showDetail = false },
                ) {
                    Icon(
                        imageVector = MiuixIcons.Back,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            }
        },
        onDismissRequest = { showDetail = false },
        onDismissFinished = {
            if (!showDetail) {
                selectedCustomId = null
                selectedSlot = null
            }
        },
    ) {
        if (detailCustom != null && detailSlot != null) {
            BatteryCustomModeEditor(
                custom = detailCustom,
                slot = detailSlot,
                onSourceChange = { source ->
                    repository.setCustomSource(detailCustom.id, detailSlot, source)
                },
                onColorChange = { color ->
                    repository.setCustomColor(detailCustom.id, detailSlot, color)
                },
            )
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
                    showDetail = true
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
